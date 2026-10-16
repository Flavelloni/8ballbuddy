import { readdir, readFile, mkdir, writeFile, access } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";
import { pickerBalls } from "../src/challenge.js";

const root = fileURLToPath(new URL("../../site/src/jsMain/resources/public/8ball/", import.meta.url));
const shots = [];

async function visit(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  if (entries.some(entry => entry.name === "meta.json")) {
    await Promise.all(["layout.jpg", "clip.mp4"].map(name => access(path.join(directory, name))));
    const meta = JSON.parse(await readFile(path.join(directory, "meta.json"), "utf8"));
    if (!Number.isInteger(meta.ball) || !pickerBalls(meta.group).includes(meta.ball)) {
      throw new Error(`Invalid answer or group: ${directory}`);
    }
    shots.push({
      id: path.relative(root, directory).split(path.sep).join("/"),
      group: meta.group,
      correctBall: meta.ball,
    });
    return;
  }
  for (const entry of entries.filter(entry => entry.isDirectory())) {
    await visit(path.join(directory, entry.name));
  }
}

await visit(root);
if (!shots.length) throw new Error("The challenge catalog is empty.");
shots.sort((a, b) => a.id.localeCompare(b.id, "en"));
const output = new URL("../generated/", import.meta.url);
await mkdir(output, { recursive: true });
await writeFile(new URL("catalog.json", output), JSON.stringify(shots, null, 2) + "\n");
console.log(`Built private answer catalog: ${shots.length} shots.`);
