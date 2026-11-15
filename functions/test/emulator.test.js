import assert from "node:assert/strict";
import { test } from "node:test";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { readFile } from "node:fs/promises";
import { answerChoices } from "../src/challenge.js";
import { automaticNames, backfillPlayerNames } from "../src/player-names.js";
import { backfillAllTime } from "../src/all-time.js";

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

test("automatic unique names, renaming, reserved shots, retry-safe totals, historical backfill and real ranks", async () => {
  await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/emulator/v1/projects/${projectId}/databases/(default)/documents`, { method: "DELETE" });
  const alice = await signIn(), bob = await signIn();
  await call("startDailyChallenge", null, {}, 401);
  const summary = await call("getDailyChallengeStatus", null);
  assert.equal(summary.shotCount, 3);
  assert.equal(summary.alias, null);
  assert.equal(summary.attempt, null);
  const [first, second] = await Promise.all([
    call("startDailyChallenge", alice), call("startDailyChallenge", bob),
  ]);
  assert.notEqual(first.displayName, second.displayName);
  assert.ok(automaticNames.includes(first.displayName));
  assert.ok(automaticNames.includes(second.displayName));
  assert.deepEqual(first.shotIds, second.shotIds);
  assert.deepEqual(await call("startDailyChallenge", alice, { alias: "CannotResetMe" }), first);
  assert.equal((await call("getDailyChallengeStatus", alice)).alias, first.displayName);
  const catalog = JSON.parse(await readFile(new URL("../generated/catalog.json", import.meta.url), "utf8"));
  assert.ok(first.shotIds.every(id => {
    const shot=catalog.find(s=>s.id===id);
    return shot.challengeOnly===true && !shot.isBreak;
  }));
  await call("renamePlayer", null, {alias:"Forbidden"}, 401);
  for (const alias of ["ab", "a".repeat(21), "<script>", "bad\u202ename"]) await call("renamePlayer", alice, {alias}, 400);
  assert.equal((await call("renamePlayer", alice, {alias:"  Pool   Hero  "})).alias, "Pool Hero");
  await call("renamePlayer", bob, {alias:"pool hero"}, 409);
  assert.equal((await call("startDailyChallenge", alice)).displayName, "Pool Hero");
  assert.equal((await call("getDailyChallengeStatus", alice)).attempt.displayName, "Pool Hero");
  await call("startChallengeShot", alice, {date:first.date,shotId:first.shotIds[1]},400);
  await call("submitChallengeAnswer", alice, {date:first.date,shotId:first.shotIds[0],selectedBall:6},400);
  const results=db.collection(`dailyChallenges/${first.date}/results`);
  for(const [account,wrongFirst] of [[alice,true],[bob,false]]) {
    for(const [index,shotId] of first.shotIds.entries()) {
      const shot=catalog.find(s=>s.id===shotId), input={date:first.date,shotId};
      await call("startChallengeShot",account,input);
      const attemptRef=db.doc(`dailyChallenges/${first.date}/attempts/${account.uid}`);
      const startedAt=(await attemptRef.get()).data().currentShotStartedAtMs;
      await call("startChallengeShot",account,input);
      assert.equal((await attemptRef.get()).data().currentShotStartedAtMs,startedAt);
      const selectedBallId=wrongFirst&&index===0 ? shot.ballSelection.ids.find(id=>id!==shot.ballSelection.correctBallId) : shot.ballSelection.correctBallId;
      const request={...input,selectedBallId,correctCount:100,uid:"forged",elapsedMs:0};
      const [accepted,retry]=await Promise.all([call("submitChallengeAnswer",account,request),call("submitChallengeAnswer",account,request)]);
      assert.deepEqual(accepted,retry);
      assert.equal(accepted.answers.length,index+1);
      if(index<2)assert.equal((await results.doc(account.uid).get()).exists,false);
    }
    const result=(await results.doc(account.uid).get()).data();
    assert.equal(result.correctCount,wrongFirst?2:3);
    assert.equal(result.countedInAllTime,true);
    assert.equal(result.simulated,false);
    const total=(await db.doc(`challengerTotals/${account.uid}`).get()).data();
    assert.equal(total.correctCount,result.correctCount);
    assert.equal(total.challenges,1);
  }
  let board=await call("getDailyLeaderboard",alice,{date:first.date});
  assert.equal(board.total,2);
  assert.equal(board.simulatedCount,0);
  assert.equal(board.ownRank,2);
  assert.equal(board.ownEntry.displayName,"Pool Hero");
  assert.deepEqual(board.entries.map(e=>e.uid),[bob.uid,alice.uid]);
  assert.equal((await call("getAllTimeLeaderboard",alice)).ownEntry.correctCount,2);
  await call("renamePlayer",alice,{alias:"Silver Champion"});
  assert.equal((await call("getDailyLeaderboard",alice,{date:first.date})).ownEntry.displayName,"Silver Champion");
  assert.equal((await call("getAllTimeLeaderboard",alice)).ownEntry.displayName,"Silver Champion");
  const past="2025-01-01";
  await db.doc(`dailyChallenges/${past}/results/${alice.uid}`).set({uid:alice.uid,displayName:"Old Name",correctCount:2,shotCount:3,totalElapsedMs:5000});
  await db.doc(`dailyChallenges/${past}/results/benchmark-old`).set({uid:"benchmark-old",displayName:"Simulated",simulated:true,correctCount:999,shotCount:999,totalElapsedMs:1});
  await results.doc("benchmark-now").set({uid:"benchmark-now",displayName:"Simulated",simulated:true,correctCount:999,shotCount:999,totalElapsedMs:1});
  assert.equal(await backfillAllTime(db),1);
  assert.equal(await backfillAllTime(db),0);
  const lifetime=await call("getAllTimeLeaderboard",alice);
  assert.equal(lifetime.total,2);
  assert.equal(lifetime.ownRank,1);
  assert.equal(lifetime.ownEntry.correctCount,4);
  assert.equal((await call("getDailyLeaderboard",alice,{date:first.date})).total,2);
  await call("getDailyLeaderboard",alice,{date:first.date,afterUid:"../attempts"},400);
  const batch=db.batch();
  for(let i=0;i<60;i++){
    const uid=`tie-${String(i).padStart(3,"0")}`;
    batch.set(results.doc(uid),{uid,displayName:`Tied player ${i}`,simulated:false,correctCount:3,totalElapsedMs:0,shotCount:3,finishedAtMs:Date.now()});
  }
  await batch.commit();
  board=await call("getDailyLeaderboard",alice,{date:first.date});
  assert.equal(board.total,62);
  assert.equal(board.entries.length,50);
  assert.equal(board.ownRank,62);
  assert.ok(board.entries.every(e=>e.rank===1));
  const next=await call("getDailyLeaderboard",alice,{date:first.date,afterUid:board.nextAfterUid});
  assert.equal(next.entries.length,12);
  assert.ok(next.entries.slice(0,10).every(e=>e.rank===1));
  assert.equal(next.entries[10].rank,61);
  assert.equal(next.entries[11].rank,62);
  for(const uid of ["tie-a","tie-b"])await db.doc(`challengerTotals/${uid}`).set({uid,displayName:uid,correctCount:10,shotCount:12,challenges:4});
  assert.deepEqual((await call("getAllTimeLeaderboard",alice)).entries.slice(0,2).map(e=>e.rank),[1,1]);
  const denied=await fetch(`http://${process.env.FIRESTORE_EMULATOR_HOST}/v1/projects/${projectId}/databases/(default)/documents/players/${alice.uid}`,{
    method:"PATCH",headers:{Authorization:`Bearer ${bob.token}`,"Content-Type":"application/json"},
    body:JSON.stringify({fields:{alias:{stringValue:"Stolen"}}}),
  });
  assert.equal(denied.status,403);
  await db.doc("players/legacy-a").set({alias:"Legacy Champion"});
  await db.doc("players/legacy-b").set({alias:"Legacy Champion"});
  await backfillPlayerNames(db);
  const legacyNames=await Promise.all(["legacy-a","legacy-b"].map(async uid=>(await db.doc(`players/${uid}`).get()).data().alias));
  assert.notEqual(legacyNames[0],legacyNames[1]);
  await backfillPlayerNames(db);
  assert.deepEqual(await Promise.all(["legacy-a","legacy-b"].map(async uid=>(await db.doc(`players/${uid}`).get()).data().alias)),legacyNames);
  await db.doc("namePools/default").set({available:[],nextNumber:2});
  const newcomer=await signIn(), numbered=await call("startDailyChallenge",newcomer);
  assert.equal(numbered.displayName,"Red Tiger 2");
  const another=await signIn();
  assert.equal((await call("startDailyChallenge",another)).displayName,"Red Tiger 3");
  assert.equal((await call("startDailyChallenge",newcomer)).displayName,"Red Tiger 2");
  console.log("PASS unique names, rename persistence, 12-shot eligibility, idempotent scoring/backfill, real daily/all-time ranks, ties, pagination and rules");
});
