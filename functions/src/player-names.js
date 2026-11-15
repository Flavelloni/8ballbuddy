import { randomInt } from "node:crypto";
import { validateAlias } from "./challenge.js";

export const adjectives = ["Red", "Blue", "Golden", "Strong", "Slow", "Swift", "Brave", "Silent", "Wild", "Mighty", "Cool", "Silver"];
export const nouns = ["Warrior", "Cheetah", "Fighter", "Hero", "Tiger", "Panther", "Falcon", "Dragon", "Wolf", "Shark", "Lynx", "Eagle"];
export const automaticNames = adjectives.flatMap(adjective => nouns.map(noun => `${adjective} ${noun}`));
export const aliasKey = alias => encodeURIComponent(validateAlias(alias).toLowerCase());

/** A bounded, fixed pool is consumed before numbered names. Claims are never recycled. */
export function nextAutomaticName(state = {}, choose = randomInt) {
  const available = [...(state.available ?? automaticNames.map((_, index) => index))];
  if (available.length) {
    const [index] = available.splice(choose(available.length), 1);
    return { alias: automaticNames[index], state: { available, nextNumber: state.nextNumber ?? 2 } };
  }
  const nextNumber = state.nextNumber ?? 2;
  return { alias: `Red Tiger ${nextNumber}`, state: { available, nextNumber: nextNumber + 1 } };
}

/** All reads precede writes, and Firestore retries concurrent claims atomically. */
export async function reservePlayerName(db, transaction, uid, existingAlias) {
  if (existingAlias) {
    const alias = validateAlias(existingAlias);
    const ref = db.doc(`playerNames/${aliasKey(alias)}`);
    const claim = await transaction.get(ref);
    if (!claim.exists || claim.data().uid === uid) return { alias, write: () => transaction.set(ref, { uid }) };
  }
  const poolRef = db.doc("namePools/default");
  const pool = await transaction.get(poolRef);
  let state = pool.exists ? pool.data() : {};
  while (true) {
    const next = nextAutomaticName(state);
    state = next.state;
    const ref = db.doc(`playerNames/${aliasKey(next.alias)}`);
    const claim = await transaction.get(ref);
    if (!claim.exists) return {
      alias: next.alias,
      write: () => { transaction.set(poolRef, state); transaction.create(ref, { uid }); },
    };
  }
}

export async function backfillPlayerNames(db) {
  const profiles = await db.collection("players").get();
  for (const profile of profiles.docs) await db.runTransaction(async transaction => {
    const current = await transaction.get(profile.ref);
    const reserved = await reservePlayerName(db, transaction, profile.id, current.data()?.alias);
    reserved.write();
    transaction.set(profile.ref, { alias: reserved.alias }, { merge: true });
  });
  return profiles.size;
}
