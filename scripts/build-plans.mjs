#!/usr/bin/env node
/**
 * Prints each guide's plan sheet (/guides/<slug>/plan) to a PDF in public/plans/, using
 * headless Chrome. Run it against a running copy of the site whenever guide content changes:
 *
 *   npm run dev                       # in one terminal
 *   npm run plans                     # in another (defaults to http://localhost:3000)
 *   npm run plans -- http://localhost:3100
 *
 * Set CHROME_PATH if Chrome isn't in a usual place. The slugs are read from
 * src/content/guides.ts so this never needs its own list.
 */
import { spawnSync } from "node:child_process";
import { existsSync, readFileSync, statSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const root = join(dirname(fileURLToPath(import.meta.url)), "..");
const base = (process.argv[2] ?? "http://localhost:3000").replace(/\/$/, "");

if (!/^https?:\/\/[\w.-]+(:\d+)?$/.test(base)) {
  console.error(`Not a plain base URL: ${base}`);
  process.exit(1);
}

const source = readFileSync(join(root, "src/content/guides.ts"), "utf8");
const list = source.match(/GUIDE_SLUGS\s*=\s*\[([^\]]+)\]/)?.[1];
const slugs = list ? [...list.matchAll(/"([a-z-]+)"/g)].map((m) => m[1]) : [];
if (slugs.length === 0) {
  console.error("Couldn't find GUIDE_SLUGS in src/content/guides.ts");
  process.exit(1);
}

const candidates = [
  process.env.CHROME_PATH,
  "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
  "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
  "/usr/bin/google-chrome",
  "/usr/bin/chromium",
].filter(Boolean);
const chrome = candidates.find((path) => existsSync(path));
if (!chrome) {
  console.error("Chrome not found. Set CHROME_PATH.");
  process.exit(1);
}

let failed = false;
for (const slug of slugs) {
  const out = join(root, "public/plans", `levitx-${slug}-plan.pdf`);
  const result = spawnSync(
    chrome,
    [
      "--headless=new",
      "--disable-gpu",
      "--no-pdf-header-footer",
      "--virtual-time-budget=8000",
      `--print-to-pdf=${out}`,
      `${base}/guides/${slug}/plan`,
    ],
    { encoding: "utf8" },
  );
  const size = existsSync(out) ? statSync(out).size : 0;
  if (result.status !== 0 || size < 10_000) {
    failed = true;
    console.error(`✗ ${slug}: Chrome exited ${result.status}, file ${size} bytes`);
  } else {
    console.log(`✓ ${slug} → public/plans/levitx-${slug}-plan.pdf (${Math.round(size / 1024)} KB)`);
  }
}
process.exit(failed ? 1 : 0);
