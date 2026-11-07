import test from 'node:test';
import assert from 'node:assert/strict';
import { shotIdForPath, ignoredShotDirectory } from '../scripts/shot-paths.mjs';

test('adding broadcaster and matchup preserves existing challenge IDs', () => {
  assert.equal(shotIdForPath('MagicCitySlate/AMDgls4nJhQ/robhall_vs_lanceireland/051958'), 'AMDgls4nJhQ/051958');
  assert.equal(shotIdForPath('AMDgls4nJhQ/051958'), 'AMDgls4nJhQ/051958');
  assert.equal(shotIdForPath('OBK/QymvyRiXJ9A/albert_vs_emil/000203'), 'QymvyRiXJ9A/000203');
});

test('bad folders, hidden working folders and malformed IDs cannot enter the catalog', () => {
  assert.ok(ignoredShotDirectory('000203_bad'));
  assert.ok(ignoredShotDirectory('.layout-crop-history'));
  assert.equal(ignoredShotDirectory('000203'), false);
  assert.throws(() => shotIdForPath('OBK/bad/match/000203'));
  assert.throws(() => shotIdForPath('OBK/QymvyRiXJ9A/match/000203', '../outside'));
});
