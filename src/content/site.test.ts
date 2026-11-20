import { describe, expect, it } from "vitest";
import { faq, site } from "./site";

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
