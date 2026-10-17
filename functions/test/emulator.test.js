import assert from "node:assert/strict";
import { test } from "node:test";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFile } from "node:fs/promises";
import { pickerBalls } from "../src/challenge.js";

const projectId = "demo-ballbuddy";
assert.ok(process.env.FIRESTORE_EMULATOR_HOST, "Run this test using Firebase emulators:exec.");
assert.ok(process.env.FIREBASE_AUTH_EMULATOR_HOST, "The Auth emulator is required.");
const db = getFirestore(initializeApp({ projectId }, "emulator-tests"));
const functionsUrl = `http://127.0.0.1:5001/${projectId}/europe-west1/`;

async function signIn() {
  const response = await fetch(`http://${process.env.FIREBASE_AUTH_EMULATOR_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake`, {
    method: "POST", headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ returnSecureToken: true }),
  });
  const account = await response.json();
  assert.equal(response.status, 200, JSON.stringify(account));
  return { uid: account.localId, token: account.idToken };
}

async function call(name, account, data = {}, status = 200) {
  const response = await fetch(functionsUrl + name, {
    method: "POST", headers: { "Content-Type": "application/json",
      ...(account ? { Authorization: `Bearer ${account.token}` } : {}) },
    body: JSON.stringify({ data }),
  });
  const result = await response.json();
  assert.equal(response.status, status, JSON.stringify(result));
  return result.result ?? result.error;
}

test("anonymous aliases, duplicate names, immutable daily attempts, server scores, rules, and paginated ranks", async () => {
  await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${projectId}/databases/(default)/documents`, { method: "DELETE" });
  const alice = await signIn();
  const bob = await signIn();
  assert.notEqual(alice.uid, bob.uid);
  await call("startDailyChallenge", null, {}, 401);
  await call("getDailyChallengeStatus", null, {}, 401);
  assert.equal((await call("getDailyChallengeStatus", alice)).alias, null);
  for (const alias of ["ab", "a".repeat(21), "<script>", "bad\u202ename"]) {
    await call("startDailyChallenge", alice, { alias }, 400);
  }
  assert.equal((await db.doc(`players/${alice.uid}`).get()).exists, false);
  const [first, second] = await Promise.all([
    call("startDailyChallenge", alice, { alias: "  PoolFan42  " }),
    call("startDailyChallenge", bob, { alias: "PoolFan42", uid: alice.uid }),
  ]);
  assert.equal(first.displayName, "PoolFan42");
  assert.equal(second.displayName, "PoolFan42");
  assert.equal((await db.doc(`players/${alice.uid}`).get()).data().alias, "PoolFan42");
  const returning = await signIn();
  await db.doc(`players/${returning.uid}`).set({ alias: "RememberedName", createdAtMs: 1, updatedAtMs: 1 });
  const returningStatus = await call("getDailyChallengeStatus", returning);
  assert.equal(returningStatus.alias, "RememberedName");
  assert.equal(returningStatus.attempt, null);
  assert.equal((await call("startDailyChallenge", returning)).displayName, "RememberedName");
  assert.equal((await db.doc(`players/${bob.uid}`).get()).data().alias, "PoolFan42");
  const status = await call("getDailyChallengeStatus", alice);
  assert.equal(status.alias, "PoolFan42");
  assert.deepEqual(status.attempt, first);
  assert.equal((await call("startDailyChallenge", alice, { alias: "AnotherName" })).displayName, "PoolFan42");
  assert.equal((await db.doc(`players/${alice.uid}`).get()).data().alias, "PoolFan42");
  assert.deepEqual(first.shotIds, second.shotIds);
  assert.equal(first.date, new Date().toISOString().slice(0, 10));
  assert.equal(first.answers.length, 0);
  assert.equal(first.clockStarted, false);
  assert.equal(first.shots, undefined);
  const results = db.collection(`dailyChallenges/${first.date}/results`);
  assert.equal((await results.get()).size, 0);
  await call("submitChallengeAnswer", alice, { date: first.date, shotId: first.shotIds[0], selectedBall: 6 }, 400);
  await call("startChallengeShot", alice, { date: first.date, shotId: first.shotIds[1] }, 400);

  const catalog = JSON.parse(await readFile(new URL("../generated/catalog.json", import.meta.url), "utf8"));
  for (const [account, wrongFirst] of [[alice, true], [bob, false]]) {
    let attempt;
    for (const [index, shotId] of first.shotIds.entries()) {
      const shot = catalog.find(shot => shot.id === shotId);
      const input = { date: first.date, shotId };
      await call("startChallengeShot", account, input);
      const attemptRef = db.doc(`dailyChallenges/${first.date}/attempts/${account.uid}`);
      await attemptRef.update({ currentShotStartedAtMs: Date.now() - 2500 });
      const startedAt = (await attemptRef.get()).data().currentShotStartedAtMs;
      await call("startChallengeShot", account, input);
      assert.equal((await attemptRef.get()).data().currentShotStartedAtMs, startedAt);
      const selectedBall = wrongFirst && index === 0 ? pickerBalls(shot.group).find(ball => ball !== shot.correctBall) : shot.correctBall;
      const request = { ...input, selectedBall, elapsedMs: 0, correctCount: 100, uid: "forged" };
      const [accepted, retry] = await Promise.all([
        call("submitChallengeAnswer", account, request), call("submitChallengeAnswer", account, request),
      ]);
      assert.deepEqual(accepted, retry);
      assert.equal(accepted.answers.length, index + 1);
      assert.ok(accepted.answers[index].elapsedMs >= 2500);
      assert.equal(accepted.answers[index].correctBall, shot.correctBall);
      attempt = accepted;
      if (index < first.shotIds.length - 1) assert.equal((await results.doc(account.uid).get()).exists, false);
    }
    const result = (await results.doc(account.uid).get()).data();
    assert.equal(result.correctCount, first.shotIds.length - (wrongFirst ? 1 : 0));
    assert.equal(result.totalElapsedMs, attempt.answers.reduce((sum, answer) => sum + answer.elapsedMs, 0));
    assert.equal(result.email, undefined);
    assert.deepEqual((await call("startDailyChallenge", account)).answers, attempt.answers);
  }

  let board = await call("getDailyLeaderboard", alice, { date: first.date });
  assert.equal(board.total, 5);
  assert.equal(board.simulatedCount, 3);
  assert.equal(board.ownRank, 2);
  assert.deepEqual(board.entries.slice(0, 2).map(entry => entry.displayName), ["PoolFan42", "PoolFan42"]);
  assert.deepEqual(board.entries.slice(0, 2).map(entry => entry.uid), [bob.uid, alice.uid]);
  assert.ok(board.entries.slice(2).every(entry => entry.simulated));
  const benchmarks = (await results.where("simulated", "==", true).get()).docs.map(doc => doc.data());
  assert.equal(benchmarks.length, 3);
  assert.ok(benchmarks.every(entry => entry.correctCount < first.shotIds.length &&
    entry.shotElapsedMs.every(elapsed => elapsed >= 60_000)));
  assert.equal((await call("getDailyLeaderboard", alice, { date: first.date })).total, 5);
  const stranger = await signIn();
  const strangerAttempt = await call("startDailyChallenge", stranger, { alias: "PoolFan42" });
  assert.equal(strangerAttempt.answers.length, 0);
  assert.equal((await call("getDailyLeaderboard", stranger, { date: first.date })).ownRank, null);
  assert.equal((await call("getDailyChallengeStatus", alice)).attempt.answers.length, first.shotIds.length);
  await call("getDailyLeaderboard", alice, { date: first.date, afterUid: "../attempts" }, 400);

  const batch = db.batch();
  for (let i = 0; i < 60; i++) {
    const uid = `tie-${String(i).padStart(3, "0")}`;
    batch.set(results.doc(uid), { uid, displayName: `Tied player ${i}`, correctCount: first.shotIds.length,
      totalElapsedMs: 1000, shotCount: first.shotIds.length, finishedAtMs: Date.now() });
  }
  await batch.commit();
  board = await call("getDailyLeaderboard", alice, { date: first.date });
  assert.equal(board.total, 65);
  assert.equal(board.ownRank, 62);
  assert.equal(board.entries.length, 50);
  assert.ok(board.entries.every(entry => entry.rank === 1));
  const next = await call("getDailyLeaderboard", alice, { date: first.date, afterUid: board.nextAfterUid });
  assert.equal(next.entries.length, 15);
  assert.equal(next.nextAfterUid, null);
  assert.ok(next.entries.slice(0, 10).every(entry => entry.rank === 1));
  assert.equal(next.entries[10].rank, 61);
  assert.equal(next.entries[11].rank, 62);
  assert.ok(next.entries.slice(12).every(entry => entry.simulated));
  assert.equal(new Set([...board.entries, ...next.entries].map(entry => entry.uid)).size, 65);

  const denied = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/${projectId}/databases/(default)/documents/dailyChallenges/${first.date}`, {
    headers: { Authorization: `Bearer ${alice.token}` },
  });
  assert.equal(denied.status, 403);
  const forged = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/${projectId}/databases/(default)/documents/dailyChallenges/${first.date}/results/${alice.uid}`, {
    method: "PATCH", headers: { Authorization: `Bearer ${alice.token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: { correctCount: { integerValue: "100" } } }),
  });
  assert.equal(forged.status, 403);
  const renamed = await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/${projectId}/databases/(default)/documents/players/${alice.uid}`, {
    method: "PATCH", headers: { Authorization: `Bearer ${bob.token}`, "Content-Type": "application/json" },
    body: JSON.stringify({ fields: { alias: { stringValue: "StolenName" } } }),
  });
  assert.equal(renamed.status, 403);
});
