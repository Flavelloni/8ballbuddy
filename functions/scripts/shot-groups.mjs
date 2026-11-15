import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { shotIdForPath, ignoredShotDirectory } from "./shot-paths.mjs";

/** Mirrors BrowseGroups.kt. Determine boundaries on the complete active catalog first. */
export async function readShotGroups(root) {
  const shots = [];
  async function visit(directory) {
    const entries = await readdir(directory, { withFileTypes: true });
    if (entries.some(entry => entry.name === "meta.json")) {
      const filename = path.join(directory, "meta.json");
      const meta = JSON.parse(await readFile(filename, "utf8"));
      if (meta.archived === true) return;
      const relative = path.relative(root, directory).split(path.sep).join("/");
      const id = shotIdForPath(relative, meta.id);
      const stamp = id.split("/").at(-1).split("_")[0];
      const time = /^\d{6}$/.test(stamp) && +stamp.slice(2, 4) < 60 && +stamp.slice(4) < 60
        ? +stamp.slice(0, 2) * 3600 + +stamp.slice(2, 4) * 60 + +stamp.slice(4) : null;
      shots.push({ filename, meta, id, time, match: path.dirname(relative), player: (meta.player ?? "").trim().replace(/\s+/g, " ").toLowerCase(), group: meta.group ?? 0 });
      return;
    }
    for (const entry of entries.filter(entry => entry.isDirectory() && !ignoredShotDirectory(entry.name))) await visit(path.join(directory, entry.name));
  }
  await visit(root);
  const compare = (a, b) => a < b ? -1 : a > b ? 1 : 0;
  shots.sort((a, b) => compare(a.match, b.match) || (a.time ?? Infinity) - (b.time ?? Infinity) || compare(a.id, b.id));
  const groups = [];
  for (const shot of shots) {
    const previous = groups.at(-1)?.at(-1);
    if (!previous || shot.meta.isBreak || shot.match !== previous.match || shot.id.split("/")[0] !== previous.id.split("/")[0] ||
        shot.player !== previous.player || ([1, 2].includes(previous.group) && [0, 1, 2].includes(shot.group) && shot.group !== previous.group) ||
        shot.time === null || previous.time === null || shot.time - previous.time > 180) groups.push([shot]);
    else groups.at(-1).push(shot);
  }
  return groups;
}
