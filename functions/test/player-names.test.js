import assert from "node:assert/strict";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { automaticNames, nextAutomaticName, aliasKey } from "../src/player-names.js";
import { readShotGroups } from "../scripts/shot-groups.mjs";
import { validateAlias } from "../src/challenge.js";

test("all adjective/noun combinations are unique and exhausted before adding numbers", () => {
  let state = {};
  const chosen = [];
  for (let index = 0; index < automaticNames.length; index++) {
    const next = nextAutomaticName(state, length => length - 1);
    state = next.state;
    chosen.push(validateAlias(next.alias));
  }
  assert.equal(new Set(chosen).size, automaticNames.length);
  assert.deepEqual(new Set(chosen), new Set(automaticNames));
  const numbered = nextAutomaticName(state);
  assert.equal(numbered.alias, "Red Tiger 2");
  assert.equal(nextAutomaticName(numbered.state).alias, "Red Tiger 3");
  assert.equal(aliasKey("  RED   Tiger "), aliasKey("Red Tiger"));
});

test("catalog tags reserve exactly non-break group starters without consuming breaks", async () => {
  const groups = await readShotGroups(fileURLToPath(new URL("../../site/src/jsMain/resources/public/8ball/", import.meta.url)));
  let challengeCount = 0;
  for (const group of groups) for (const [index, shot] of group.entries()) {
    assert.equal(shot.meta.challengeOnly, index === 0 && shot.meta.isBreak !== true, shot.id);
    if (shot.meta.challengeOnly) challengeCount++;
  }
  assert.ok(challengeCount >= 3);
});
