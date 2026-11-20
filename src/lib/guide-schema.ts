import type { Guide } from "@/content/guides";
import { guideHub } from "@/content/guides";
import { photos } from "@/content/photos";
import { site } from "@/content/site";

/** When the guides were last reviewed. Bump this when the advice changes. */
export const GUIDES_UPDATED = "2026-11-20";

const publisher = { "@id": `${site.url}/#publisher` };

function breadcrumbs(items: { name: string; url: string }[]) {
  return {
    "@type": "BreadcrumbList",
    itemListElement: items.map((item, i) => ({ "@type": "ListItem", position: i + 1, name: item.name, item: item.url })),
  };
}

/**
 * JSON-LD for one guide: the article itself, where it sits on the site, and its FAQ
 * word for word. The publisher node is defined on the home page and linked by @id.
 */
export function guideStructuredData(guide: Guide) {
  const url = `${site.url}/guides/${guide.slug}`;
  return {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "Article",
        "@id": `${url}#article`,
        headline: guide.h1,
        description: guide.description,
        image: photos[guide.photo].src,
        inLanguage: "en-AU",
        datePublished: GUIDES_UPDATED,
        dateModified: GUIDES_UPDATED,
        author: publisher,
        publisher,
        mainEntityOfPage: url,
        about: { "@type": "Thing", name: guide.skill },
      },
      breadcrumbs([
        { name: "Home", url: site.url },
        { name: "Guides", url: `${site.url}/guides` },
        { name: guide.skill, url },
      ]),
      {
        "@type": "FAQPage",
        "@id": `${url}#faq`,
        mainEntity: guide.faq.map((item) => ({
          "@type": "Question",
          name: item.q,
          acceptedAnswer: { "@type": "Answer", text: item.a },
        })),
      },
    ],
  };
}

/** JSON-LD for the guide hub: a collection page listing the guides. */
export function hubStructuredData(all: Guide[]) {
  const url = `${site.url}/guides`;
  return {
    "@context": "https://schema.org",
    "@graph": [
      {
        "@type": "CollectionPage",
        "@id": `${url}#page`,
        name: guideHub.title,
        description: guideHub.description,
        url,
        inLanguage: "en-AU",
        publisher,
        hasPart: all.map((g) => ({ "@type": "Article", headline: g.h1, url: `${site.url}/guides/${g.slug}` })),
      },
      breadcrumbs([
        { name: "Home", url: site.url },
        { name: "Guides", url },
      ]),
    ],
  };
}
