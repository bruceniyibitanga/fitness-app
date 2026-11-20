import { describe, expect, it } from "vitest";
import { guides } from "@/content/guides";
import { site } from "@/content/site";
import { guideStructuredData, hubStructuredData } from "./guide-schema";

describe("guideStructuredData", () => {
  it.each(guides)("$slug has an article, breadcrumbs and the FAQ word for word", (guide) => {
    const graph = guideStructuredData(guide)["@graph"] as Record<string, unknown>[];
    const types = graph.map((node) => node["@type"]);
    expect(types).toEqual(["Article", "BreadcrumbList", "FAQPage"]);

    const faq = graph[2] as { mainEntity: { name: string; acceptedAnswer: { text: string } }[] };
    expect(faq.mainEntity.map((q) => q.name)).toEqual(guide.faq.map((q) => q.q));
    expect(faq.mainEntity.map((q) => q.acceptedAnswer.text)).toEqual(guide.faq.map((q) => q.a));

    const crumbs = graph[1] as { itemListElement: { item: string }[] };
    expect(crumbs.itemListElement.at(-1)?.item).toBe(`${site.url}/guides/${guide.slug}`);
  });
});

describe("hubStructuredData", () => {
  it("lists every guide", () => {
    const page = hubStructuredData(guides)["@graph"][0] as { hasPart: { url: string }[] };
    expect(page.hasPart.map((p) => p.url)).toEqual(guides.map((g) => `${site.url}/guides/${g.slug}`));
  });
});
