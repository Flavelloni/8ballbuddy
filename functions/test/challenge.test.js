import assert from "node:assert/strict";
import { test } from "node:test";
import {
  completedResult, createAttempt, createDailyBenchmarks, createDailyChallenge, pickerBalls,
  publicAttempt, startShot, submitAnswer, utcDate, validateAlias,
} from "../src/challenge.js";

const date = "2026-10-16";
const catalog = Array.from({ length: 30 }, (_, i) => ({ id: `shot-${i + 1}`, group: 1, correctBall: 6 }));

test("aliases are normalized and allow international letters and duplicate display names", () => {
  assert.equal(validateAlias("  Pool   Fan_42-  "), "Pool Fan_42-");
  assert.equal(validateAlias("\u00c5se"), "\u00c5se");
  assert.equal(validateAlias("\uff30\uff4f\uff4f\uff4c"), "Pool");
  const daily = createDailyChallenge(date, [catalog[0]]);
  const alice = createAttempt(daily, "alice", validateAlias("PoolFan42"), 0);
  const bob = createAttempt(daily, "bob", validateAlias("PoolFan42"), 0);
  assert.equal(alice.displayName, bob.displayName);
  assert.notEqual(alice.uid, bob.uid);
});

test("invalid aliases cannot enter the leaderboard", () => {
  for (const value of [undefined, null, 42, "", "   ", "ab", "a".repeat(21), "<script>", "name\nnext", "name\u202enext", "pool\ud83c\udfb1"]) {
    assert.throws(() => validateAlias(value), error => error.code === "invalid-argument");
  }
});

test("every player gets the same ten distinct shots regardless of catalog order", () => {
  const challenge = createDailyChallenge(date, catalog);
  assert.equal(challenge.shotIds.length, 10);
  assert.equal(new Set(challenge.shotIds).size, 10);
  assert.deepEqual(challenge, createDailyChallenge(date, [...catalog].reverse()));
  assert.notDeepEqual(challenge.shotIds, createDailyChallenge("2026-10-17", catalog).shotIds);
});

test("daily simulated scores have distinct names and take at least a minute per shot", () => {
  const benchmarks = createDailyBenchmarks(date, 10);
  assert.equal(benchmarks.length, 3);
  assert.deepEqual(benchmarks, createDailyBenchmarks(date, 10));
  assert.notDeepEqual(benchmarks, createDailyBenchmarks("2026-10-17", 10));
  assert.equal(new Set(benchmarks.map(entry => entry.displayName)).size, 3);
  assert.ok(benchmarks.every(entry => entry.simulated && entry.correctCount < 10));
  assert.ok(benchmarks.every(entry => entry.shotElapsedMs.length === 10 &&
    entry.shotElapsedMs.every(elapsed => elapsed >= 60_000)));
  assert.ok(benchmarks.every(entry => entry.totalElapsedMs ===
    entry.shotElapsedMs.reduce((total, elapsed) => total + elapsed, 0)));
});

test("the day changes at midnight UTC", () => {
  assert.equal(utcDate(Date.parse("2026-10-16T23:59:59Z")), date);
  assert.equal(utcDate(Date.parse("2026-10-17T00:00:00Z")), "2026-10-17");
});

test("a small catalog excludes invalid answers and duplicates", () => {
  const challenge = createDailyChallenge(date, [catalog[0], catalog[0], { id: "bad", group: 1, correctBall: 14 }]);
  assert.deepEqual(challenge.shotIds, ["shot-1"]);
  assert.deepEqual(pickerBalls(0), [1, 2, 3, 4, 5, 6, 7, 9, 10, 11, 12, 13, 14, 15]);
});

test("server time counts once and cannot be reset by starting again", () => {
  const challenge = createDailyChallenge(date, [catalog[0]]);
  let attempt = createAttempt(challenge, "uid", "Player", 0);
  assert.equal(completedResult(challenge, attempt), null);
  assert.throws(() => submitAnswer(challenge, attempt, "shot-1", 6, 1000), /clock/);
  attempt = startShot(challenge, attempt, "shot-1", 1000);
  attempt = startShot(challenge, attempt, "shot-1", 9000);
  assert.equal(attempt.currentShotStartedAtMs, 1000);
  assert.equal(publicAttempt(challenge, attempt, 10000).currentShotElapsedMs, 9000);
  attempt = submitAnswer(challenge, attempt, "shot-1", 6, 10500);
  assert.equal(attempt.answers[0].elapsedMs, 9500);
  assert.deepEqual(completedResult(challenge, attempt), {
    uid: "uid", displayName: "Player", correctCount: 1,
    totalElapsedMs: 9500, shotCount: 1, finishedAtMs: 10500,
  });
  assert.equal(submitAnswer(challenge, attempt, "shot-1", 1, 20000), attempt);
});

test("cannot skip shots, answer for the wrong group, or publish an incomplete result", () => {
  const challenge = createDailyChallenge(date, catalog.slice(0, 2));
  let attempt = createAttempt(challenge, "uid", "Player", 0);
  const [first, second] = challenge.shotIds;
  assert.throws(() => startShot(challenge, attempt, second, 10), /current shot/);
  attempt = startShot(challenge, attempt, first, 10);
  assert.throws(() => submitAnswer(challenge, attempt, first, 14, 20), /group/);
  assert.throws(() => submitAnswer(challenge, attempt, first, "6", 20), /valid ball/);
  assert.throws(() => submitAnswer(challenge, attempt, second, 6, 20), /current shot/);
  attempt = submitAnswer(challenge, attempt, first, 1, 20);
  assert.equal(completedResult(challenge, attempt), null);
  assert.equal(attempt.answers[0].correctBall, 6);
});

test("the browser response contains only answers already submitted", () => {
  const challenge = createDailyChallenge(date, catalog.slice(0, 2));
  const attempt = createAttempt(challenge, "uid", "Player", 0);
  assert.deepEqual(publicAttempt(challenge, attempt, 0), {
    displayName: "Player", date, shotIds: challenge.shotIds, answers: [], currentShotElapsedMs: 0, clockStarted: false,
  });
});
