import { existsSync } from "node:fs";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { GUIDE_SLUGS, getGuide, guideHub, guides, isGuideSlug } from "./guides";
import { photos } from "./photos";

const words = (text: string) => text.trim().split(/\s+/).length;
const allText = (value: unknown): string[] =>
  typeof value === "string" ? [value] : Array.isArray(value) ? value.flatMap(allText) : value && typeof value === "object" ? Object.values(value).flatMap(allText) : [];

describe("guides", () => {
  it("has exactly one guide per slug, in order", () => {
    expect(guides.map((g) => g.slug)).toEqual([...GUIDE_SLUGS]);
  });

  it.each(guides)("$slug has search-friendly title and description lengths", (guide) => {
    expect(guide.title.length).toBeLessThanOrEqual(60);
    expect(guide.description.length).toBeGreaterThanOrEqual(120);
    expect(guide.description.length).toBeLessThanOrEqual(160);
  });

  it.each(guides)("$slug answers the question in 35 to 70 words", (guide) => {
    expect(words(guide.answer)).toBeGreaterThanOrEqual(35);
    expect(words(guide.answer)).toBeLessThanOrEqual(70);
  });

  it.each(guides)("$slug keeps every FAQ answer under 60 words", (guide) => {
    for (const item of guide.faq) expect(words(item.a), item.q).toBeLessThanOrEqual(60);
    expect(guide.faq.length).toBeGreaterThanOrEqual(3);
  });

  it.each(guides)("$slug has enough stages, mistakes and sessions to be useful", (guide) => {
    expect(guide.stages.length).toBeGreaterThanOrEqual(5);
    expect(guide.mistakes.length).toBeGreaterThanOrEqual(3);
    expect(guide.week).toHaveLength(3);
    for (const stage of guide.stages) expect(stage.pass.length).toBeLessThanOrEqual(48);
  });

  it.each(guides)("$slug points at a real photo and its own plan file", (guide) => {
    expect(photos[guide.photo]).toBeDefined();
    expect(guide.plan.file).toBe(`/plans/levitx-${guide.slug}-plan.pdf`);
    expect(existsSync(join(process.cwd(), "public", guide.plan.file))).toBe(true);
  });

  it("uses no em dashes anywhere", () => {
    for (const text of [...allText(guides), ...allText(guideHub)]) expect(text).not.toContain("—");
  });

  it("keeps the hub title and description in range", () => {
    expect(guideHub.title.length).toBeLessThanOrEqual(60);
    expect(guideHub.description.length).toBeGreaterThanOrEqual(120);
    expect(guideHub.description.length).toBeLessThanOrEqual(160);
  });
});

describe("isGuideSlug and getGuide", () => {
  it("accepts only known slugs", () => {
    expect(isGuideSlug("planche")).toBe(true);
    for (const value of ["", "Planche", "../planche", "planche/", null, undefined, 3]) expect(isGuideSlug(value)).toBe(false);
  });

  it("finds a guide by slug", () => {
    expect(getGuide("handstand")?.skill).toBe("Handstand");
    expect(getGuide("nope")).toBeUndefined();
  });
});
