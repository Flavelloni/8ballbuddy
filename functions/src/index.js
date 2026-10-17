import { readFileSync } from "node:fs";
import { initializeApp } from "firebase-admin/app";
import { FieldPath, getFirestore } from "firebase-admin/firestore";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  ChallengeError, completedResult, createAttempt, createDailyBenchmarks, createDailyChallenge,
  publicAttempt, startShot, submitAnswer, utcDate, validateAlias,
} from "./challenge.js";

initializeApp();
setGlobalOptions({ region: "europe-west1", maxInstances: 3, memory: "256MiB", timeoutSeconds: 30 });
const db = getFirestore();
const catalog = JSON.parse(readFileSync(new URL("../generated/catalog.json", import.meta.url), "utf8"));

function player(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Could not identify your player. Please try again.");
  }
  return { uid: request.auth.uid };
}

function dateArgument(value) {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value) ||
      Number.isNaN(Date.parse(`${value}T00:00:00Z`)) || utcDate(Date.parse(`${value}T00:00:00Z`)) !== value) {
    throw new HttpsError("invalid-argument", "Choose a valid challenge date.");
  }
  return value;
}

function callable(handler) {
  return onCall(async request => {
    const identity = player(request);
    try {
      return await handler(request.data ?? {}, identity);
    } catch (error) {
      if (error instanceof ChallengeError) throw new HttpsError(error.code, error.message);
      throw error;
    }
  });
}

export const getDailyChallengeStatus = onCall(async request => {
  const date = utcDate(Date.now());
  const dailyRef = db.doc(`dailyChallenges/${date}`);
  const refs = [dailyRef];
  if (request.auth) refs.push(db.doc(`players/${request.auth.uid}`), dailyRef.collection("attempts").doc(request.auth.uid));
  const [daily, profile, attempt] = await db.getAll(...refs);
  const challenge = daily.exists ? daily.data() : createDailyChallenge(date, catalog);
  return {
    date, shotCount: challenge.shotIds.length,
    alias: attempt?.exists ? attempt.data().displayName : profile?.exists ? profile.data().alias : null,
    attempt: daily.exists && attempt?.exists ? publicAttempt(challenge, attempt.data(), Date.now()) : null,
  };
});

export const startDailyChallenge = callable(async (data, identity) => {
  const date = utcDate(Date.now());
  const dailyRef = db.doc(`dailyChallenges/${date}`);
  const attemptRef = dailyRef.collection("attempts").doc(identity.uid);
  const profileRef = db.doc(`players/${identity.uid}`);
  return db.runTransaction(async transaction => {
    const [dailySnapshot, attemptSnapshot, profileSnapshot] = await transaction.getAll(dailyRef, attemptRef, profileRef);
    const now = Date.now();
    if (date !== utcDate(now)) throw new HttpsError("failed-precondition", "The daily challenge has changed. Please try again.");
    const alias = attemptSnapshot.exists ? attemptSnapshot.data().displayName
      : validateAlias(data.alias ?? (profileSnapshot.exists ? profileSnapshot.data().alias : undefined));
    const challenge = dailySnapshot.exists ? dailySnapshot.data() : createDailyChallenge(date, catalog);
    const attempt = attemptSnapshot.exists ? attemptSnapshot.data() : createAttempt(challenge, identity.uid, alias, now);
    if (!dailySnapshot.exists) transaction.create(dailyRef, challenge);
    if (!attemptSnapshot.exists) {
      transaction.create(attemptRef, attempt);
      transaction.set(profileRef, {
        alias, createdAtMs: profileSnapshot.exists ? profileSnapshot.data().createdAtMs : now, updatedAtMs: now,
      });
    }
    return publicAttempt(challenge, attempt, now);
  });
});

function updateAttempt(data, identity, update) {
  const date = dateArgument(data.date);
  if (date !== utcDate(Date.now())) {
    throw new HttpsError("failed-precondition", "This daily challenge has ended. Return to today's challenge.");
  }
  if (typeof data.shotId !== "string" || data.shotId.length > 200) {
    throw new HttpsError("invalid-argument", "Choose a valid shot.");
  }
  const dailyRef = db.doc(`dailyChallenges/${date}`);
  const attemptRef = dailyRef.collection("attempts").doc(identity.uid);
  const resultRef = dailyRef.collection("results").doc(identity.uid);
  return db.runTransaction(async transaction => {
    const [dailySnapshot, attemptSnapshot] = await transaction.getAll(dailyRef, attemptRef);
    if (!dailySnapshot.exists || !attemptSnapshot.exists) {
      throw new HttpsError("failed-precondition", "Start today's challenge first.");
    }
    const now = Date.now();
    if (date !== utcDate(now)) throw new HttpsError("failed-precondition", "This daily challenge has ended.");
    const challenge = dailySnapshot.data();
    const original = attemptSnapshot.data();
    const attempt = update(challenge, original, data, now);
    if (attempt !== original) {
      transaction.set(attemptRef, attempt);
      const result = completedResult(challenge, attempt);
      if (result) transaction.create(resultRef, result);
    }
    return publicAttempt(challenge, attempt, now);
  });
}

export const startChallengeShot = callable((data, identity) => updateAttempt(data, identity,
  (challenge, attempt, input, now) => startShot(challenge, attempt, input.shotId, now)));

export const submitChallengeAnswer = callable((data, identity) => updateAttempt(data, identity,
  (challenge, attempt, input, now) => submitAnswer(challenge, attempt, input.shotId, input.selectedBall, now)));

async function count(query) {
  return (await query.count().get()).data().count;
}

async function rankOf(results, entry) {
  const [moreCorrect, faster] = await Promise.all([
    count(results.where("correctCount", ">", entry.correctCount)),
    count(results.where("correctCount", "==", entry.correctCount).where("totalElapsedMs", "<", entry.totalElapsedMs)),
  ]);
  return moreCorrect + faster + 1;
}

async function ensureDailyBenchmarks(date) {
  const dailyRef = db.doc(`dailyChallenges/${date}`);
  const markerRef = dailyRef.collection("meta").doc("benchmarks");
  return db.runTransaction(async transaction => {
    const marker = await transaction.get(markerRef);
    if (marker.exists) return marker.data().count;
    const daily = await transaction.get(dailyRef);
    if (!daily.exists) return 0;
    const benchmarks = createDailyBenchmarks(date, daily.data().shotIds.length);
    for (const entry of benchmarks) transaction.create(dailyRef.collection("results").doc(entry.uid), entry);
    transaction.create(markerRef, { count: benchmarks.length });
    return benchmarks.length;
  });
}

export const getDailyLeaderboard = callable(async (data, identity) => {
  const date = dateArgument(data.date);
  const simulatedCount = await ensureDailyBenchmarks(date);
  const results = db.collection(`dailyChallenges/${date}/results`);
  let query = results.orderBy("correctCount", "desc").orderBy("totalElapsedMs", "asc").orderBy(FieldPath.documentId());
  if (data.afterUid != null) {
    if (typeof data.afterUid !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(data.afterUid)) {
      throw new HttpsError("invalid-argument", "Invalid leaderboard page.");
    }
    const cursor = await results.doc(data.afterUid).get();
    if (!cursor.exists) throw new HttpsError("invalid-argument", "Leaderboard page no longer exists.");
    query = query.startAfter(cursor);
  }
  const [snapshot, total, own] = await Promise.all([
    query.limit(51).get(), count(results), results.doc(identity.uid).get(),
  ]);
  const rows = snapshot.docs.slice(0, 50).map(document => document.data());
  // Competition ranks share a place when both score and time tie, including across pages.
  let previous = null;
  let currentRank = 0;
  let precedingTies = 0;
  if (rows.length) {
    currentRank = await rankOf(results, rows[0]);
    precedingTies = await count(results.where("correctCount", "==", rows[0].correctCount)
      .where("totalElapsedMs", "==", rows[0].totalElapsedMs).where(FieldPath.documentId(), "<", rows[0].uid));
  }
  const firstRank = currentRank;
  const entries = rows.map((entry, index) => {
    if (previous && (entry.correctCount !== previous.correctCount || entry.totalElapsedMs !== previous.totalElapsedMs)) {
      currentRank = firstRank + precedingTies + index;
    }
    previous = entry;
    return { ...entry, rank: currentRank };
  });
  return {
    entries, total, simulatedCount,
    ownRank: own.exists ? await rankOf(results, own.data()) : null,
    nextAfterUid: snapshot.docs.length > 50 ? rows.at(-1).uid : null,
  };
});
