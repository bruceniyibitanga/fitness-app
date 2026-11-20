import type { MetadataRoute } from "next";
import { site } from "@/content/site";
import { guides } from "@/content/guides";
import { GUIDES_UPDATED } from "@/lib/guide-schema";

/** Every public page. The printable plan sheets are left out on purpose: they're for the PDFs. */
export default function sitemap(): MetadataRoute.Sitemap {
  return [
    { url: site.url, changeFrequency: "weekly", priority: 1 },
    { url: `${site.url}/guides`, lastModified: GUIDES_UPDATED, changeFrequency: "monthly", priority: 0.8 },
    ...guides.map((guide) => ({
      url: `${site.url}/guides/${guide.slug}`,
      lastModified: GUIDES_UPDATED,
      changeFrequency: "monthly" as const,
      priority: 0.8,
    })),
    { url: `${site.url}/privacy`, changeFrequency: "yearly", priority: 0.3 },
  ];
}
