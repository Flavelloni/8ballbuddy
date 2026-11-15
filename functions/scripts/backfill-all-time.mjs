import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { backfillAllTime } from "../src/all-time.js";
import { backfillPlayerNames } from "../src/player-names.js";

const projectId = process.argv[2];
if (!projectId) throw new Error("Supply the explicit Firebase project ID. No default production target is used.");
initializeApp({ projectId });
console.log(`Reserved names for ${await backfillPlayerNames(getFirestore())} existing profiles in ${projectId}.`);
console.log(`Credited ${await backfillAllTime(getFirestore())} historical completed rounds in ${projectId}.`);
