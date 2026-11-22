import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";
import { coach, faq, site } from "./site";

/** Guard rails for the things search engines read first. */
describe("site metadata", () => {
  it("keeps the title inside what search results show", () => {
    expect(site.title.length).toBeLessThanOrEqual(60);
  });

  it("keeps the description inside what search results show", () => {
    expect(site.description.length).toBeGreaterThanOrEqual(120);
    expect(site.description.length).toBeLessThanOrEqual(160);
  });

  it("has a canonical address with no trailing slash", () => {
    expect(site.url.endsWith("/")).toBe(false);
    expect(new URL(site.url).protocol).toBe("https:");
  });

  it("names the product as it is today: Levitx on Android", () => {
    const everything = JSON.stringify({ site, faq });
    expect(everything).not.toMatch(/\bLEVER\b|iPhone|Apple Watch|watchOS/);
    expect(site.title).toContain("Levitx");
  });

  it("keeps FAQ answers short enough to be read out as one answer", () => {
    for (const item of faq.items) {
      expect(item.a.split(/\s+/).length).toBeLessThanOrEqual(60);
    }
  });
});

/**
 * The Meet Levi cards claim to be what the app shows, so they must use the app's own words.
 * These read the app's phrase book and phone strings straight from the repo, turn each into a
 * pattern (placeholders match anything), and check every line a card shows against them. If
 * the app's wording changes, this fails until the website catches up.
 */
describe("Meet Levi", () => {
  const repo = (path: string) => readFileSync(fileURLToPath(new URL(`../../${path}`, import.meta.url)), "utf8");
  const escape = (text: string) => text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

  /** Each quoted Kotlin string in CoachPhrases, with `${...}` templates as wildcards. */
  const phrases = repo("mobile/core/src/commonMain/kotlin/app/levitx/core/coach/CoachPhrases.kt")
    .split("\n")
    .map((line) => line.match(/"(.*)"/)?.[1])
    .filter((literal): literal is string => Boolean(literal))
    .map((literal) => literal.split(/\$\{.*?\}/));

  /** Each phone string, with `%1$s` style placeholders as wildcards and `\'` unescaped. */
  const strings = [...repo("mobile/app-phone/src/main/res/values/strings.xml").matchAll(/<(?:string|item)[^>]*>([^<]*)</g)]
    .map((m) => m[1].replace(/\\'/g, "'"))
    .map((text) => text.split(/%\d\$[sd]/));

  // A string that's all placeholder ("%1$s") would match anything, so it can't vouch for a line.
  const patterns = [...phrases, ...strings]
    .filter((parts) => parts.join("").trim().length >= 6)
    .map((parts) => new RegExp(`^${parts.map(escape).join(".+?")}$`));
  /** Plain app strings, for replies that quote one whole sentence of a longer message. */
  const plain = strings.filter((parts) => parts.length === 1).map(([text]) => text);

  const isAppWording = (text: string) =>
    patterns.some((pattern) => pattern.test(text)) ||
    (/[.!?]$/.test(text) && plain.some((full) => full.startsWith(text) || full.includes(` ${text}`)));

  it("uses the app's own wording in every reply", () => {
    for (const { reply } of coach.stories) {
      const shown = [reply.title, reply.line, ...(reply.choices ?? [])];
      for (const text of shown.filter((t): t is string => Boolean(t))) {
        expect(isAppWording(text), text).toBe(true);
      }
    }
  });

  it("catches wording the app doesn't use", () => {
    expect(isAppWording("Knee push-up: target up. You hit every set last time.")).toBe(true);
    expect(isAppWording("You're not starting over: every exercise picks up where you left it.")).toBe(true);
    expect(isAppWording("Great job! You're crushing it.")).toBe(false);
    expect(isAppWording("You're not starting over")).toBe(false);
  });

  it("gives every story a reply, a unique key and a short thought", () => {
    const keys = coach.stories.map((s) => s.when);
    expect(new Set(keys).size).toBe(keys.length);
    for (const { thought, reply } of coach.stories) {
      expect(thought.split(/\s+/).length).toBeLessThanOrEqual(8);
      expect(reply.title ?? reply.line).toBeTruthy();
    }
  });

  it("writes without em dashes", () => {
    expect(JSON.stringify(coach)).not.toContain("\u2014");
  });
});
