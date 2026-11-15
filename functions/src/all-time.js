/** Completion and historical backfill share this idempotent accounting operation. */
export async function creditCompletedResult(db, transaction, resultRef, result, create = false) {
  if (result.simulated === true || result.countedInAllTime === true) return () => {};
  const totalRef = db.doc(`challengerTotals/${result.uid}`);
  const previous = await transaction.get(totalRef);
  const total = previous.exists ? previous.data() : {};
  return () => {
    transaction.set(totalRef, {
      uid: result.uid, displayName: result.displayName,
      correctCount: (total.correctCount ?? 0) + result.correctCount,
      shotCount: (total.shotCount ?? 0) + result.shotCount,
      challenges: (total.challenges ?? 0) + 1,
    });
    const marked = { ...result, simulated: false, countedInAllTime: true };
    if (create) transaction.create(resultRef, marked); else transaction.set(resultRef, marked);
  };
}

export async function backfillAllTime(db) {
  // Run explicitly during rollout, never as part of a user's request.
  const results = await db.collectionGroup("results").get();
  let credited = 0;
  for (const snapshot of results.docs) {
    if (!/^dailyChallenges\/[^/]+\/results\/[^/]+$/.test(snapshot.ref.path)) continue;
    credited += await db.runTransaction(async transaction => {
      const current = await transaction.get(snapshot.ref);
      const result = current.data();
      if (!result || result.simulated === true || result.countedInAllTime === true) return 0;
      const credit = await creditCompletedResult(db, transaction, snapshot.ref, result);
      credit();
      return 1;
    });
  }
  return credited;
}
