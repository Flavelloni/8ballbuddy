import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const { indexes } = JSON.parse(readFileSync(new URL("../../firestore.indexes.json", import.meta.url), "utf8"));

test("all-time standings declare their non-default ascending document-ID tie-break index", () => {
  assert.ok(indexes.some(index => index.collectionGroup === "challengerTotals" &&
    index.queryScope === "COLLECTION" && JSON.stringify(index.fields) === JSON.stringify([
      { fieldPath: "correctCount", order: "DESCENDING" },
      { fieldPath: "__name__", order: "ASCENDING" },
    ])));
});
