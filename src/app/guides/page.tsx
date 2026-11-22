import type { Metadata } from "next";
import { guideHub, guides } from "@/content/guides";
import { hubStructuredData } from "@/lib/guide-schema";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Breadcrumbs, GuideCard } from "@/components/guides/parts";

export const metadata: Metadata = {
  title: guideHub.title,
  description: guideHub.description,
  alternates: { canonical: "/guides" },
  openGraph: { type: "website", url: "/guides", title: guideHub.title, description: guideHub.description },
};

/**
 * Where each card sits and how tall it is, the same rhythm as the home page's skills:
 * rows alternate wide-narrow, narrow-wide, so the long, flat skills get the wide cells.
 */
const CELL: Record<string, string> = {
  "muscle-up": "md:col-span-5",
  "front-lever": "md:col-span-7",
  planche: "md:col-span-7",
  handstand: "md:col-span-5",
};
const ORDER = ["front-lever", "muscle-up", "handstand", "planche"];

/** The guide hub: a ruled header, then the four guides as photo cards on the grid. */
export default function GuidesPage() {
  const ordered = ORDER.map((slug) => guides.find((g) => g.slug === slug)).filter((g) => g !== undefined);
  return (
    <>
      <JsonLd data={hubStructuredData(guides)} />
      <Header />
      <main>
        <header className="border-b border-line">
          <div className="rail cell pt-36 md:pt-40">
            <Breadcrumbs />
            <h1 className="figure intro intro-2 mt-10 text-[clamp(56px,8vw,128px)] leading-[0.88]">
              {guideHub.h1[0]}
              <br />
              <span className="text-accent">{guideHub.h1[1]}</span>
            </h1>
            <p className="intro intro-3 mt-8 max-w-[560px] text-lg leading-relaxed text-soft">{guideHub.lede}</p>
          </div>
        </header>
        <section aria-label="The guides" className="border-b border-line">
          <ul className="rail grid gap-px bg-line md:grid-cols-12">
            {ordered.map((guide, i) => (
              <li key={guide.slug} className={CELL[guide.slug]}>
                <GuideCard
                  guide={guide}
                  n={String(i + 1).padStart(2, "0")}
                  sizes="(min-width: 768px) 58vw, 100vw"
                  className="h-[420px] md:h-[560px]"
                />
              </li>
            ))}
          </ul>
        </section>
      </main>
      <Footer />
    </>
  );
}
