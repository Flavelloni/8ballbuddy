import assert from "node:assert/strict";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { automaticNames, nextAutomaticName, aliasKey } from "../src/player-names.js";
import { readShotGroups } from "../scripts/shot-groups.mjs";
import { validateAlias, createDailyChallenge } from "../src/challenge.js";

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

test("catalog tags are explicit editorial choices and never reserve breaks", async () => {
  const groups = await readShotGroups(fileURLToPath(new URL("../../site/src/jsMain/resources/public/8ball/", import.meta.url)));
  let challengeCount = 0;
  for (const group of groups) for (const shot of group) {
    assert.equal(typeof shot.meta.challengeOnly, "boolean", shot.id);
    if (shot.meta.isBreak === true) assert.equal(shot.meta.challengeOnly, false, shot.id);
    if (shot.meta.challengeOnly) challengeCount++;
  }
  assert.ok(challengeCount >= 3);
});

test("daily selection honors a tagged later shot without tagging the group starter", () => {
  const catalog = [
    { id: 'QymvyRiXJ9A/030251', group: 2, correctBall: 9, challengeOnly: false },
    { id: 'QymvyRiXJ9A/030318', group: 2, correctBall: 14, challengeOnly: true },
  ];
  assert.deepEqual(createDailyChallenge('2026-11-16', catalog).shotIds, ['QymvyRiXJ9A/030318']);
  assert.equal(catalog[0].challengeOnly, false);
});
