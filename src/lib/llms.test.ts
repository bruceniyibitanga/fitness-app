import { describe, expect, it } from "vitest";
import { llmsText } from "./llms";
import { faq, site } from "@/content/site";

describe("llmsText", () => {
  const text = llmsText();

  it("starts with the name and the one-line description, as the llms.txt format asks", () => {
    const [title, , summary] = text.split("\n");
    expect(title).toBe(`# ${site.name}`);
    expect(summary).toBe(`> ${site.description}`);
  });

  it("carries every FAQ answer word for word", () => {
    for (const item of faq.items) {
      expect(text).toContain(`### ${item.q}\n${item.a}`);
    }
  });

  it("links only to the canonical domain and the publisher", () => {
    const links = [...text.matchAll(/\]\((https?:\/\/[^)]+)\)/g)].map((m) => new URL(m[1]).origin);
    for (const origin of links) {
      expect([new URL(site.url).origin, new URL(site.publisher.url).origin]).toContain(origin);
    }
  });
});
