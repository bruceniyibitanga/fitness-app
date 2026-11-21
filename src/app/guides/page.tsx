import type { Metadata } from "next";
import Link from "next/link";
import { guideHub, guides } from "@/content/guides";
import { hubStructuredData } from "@/lib/guide-schema";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { Breadcrumbs } from "@/components/guides/parts";

export const metadata: Metadata = {
  title: guideHub.title,
  description: guideHub.description,
  alternates: { canonical: "/guides" },
  openGraph: { type: "website", url: "/guides", title: guideHub.title, description: guideHub.description },
};

/** The first sentence of the answer, for the index. */
function firstSentence(text: string) {
  return text.split(/(?<=\.)\s/)[0];
}

/** The guide hub: four large rows, photos alternating sides like a contents spread. */
export default function GuidesPage() {
  return (
    <>
      <JsonLd data={hubStructuredData(guides)} />
      <Header />
      <main>
        <header className="gutter mx-auto max-w-[1200px] pt-36">
          <Breadcrumbs />
          <p className="mono intro mt-12 text-[11px] text-accent">{guideHub.eyebrow}</p>
          <h1 className="display intro intro-2 mt-6 text-[clamp(56px,8vw,128px)] leading-[0.92]">
            {guideHub.h1[0]}
            <br />
            <em className="italic">{guideHub.h1[1]}</em>
          </h1>
          <p className="intro intro-3 mt-8 max-w-[560px] text-lg leading-relaxed text-soft">{guideHub.lede}</p>
        </header>

        <ol className="gutter section-pad mx-auto max-w-[1200px]">
          {guides.map((guide, i) => {
            const flip = i % 2 === 1;
            return (
              <li key={guide.slug} className="border-t border-line py-[clamp(40px,6vw,80px)] first:border-t-0 first:pt-0">
                <Link href={`/guides/${guide.slug}`} className="group grid items-center gap-x-6 gap-y-8 md:grid-cols-12">
                  <MaskedPhoto
                    photo={guide.photo}
                    sizes="(min-width: 768px) 50vw, 100vw"
                    position={guide.photoPosition}
                    frame="aspect-[4/3]"
                    className={`md:col-span-6 ${flip ? "md:order-2 md:col-start-7" : ""}`}
                  />
                  <div className={`md:col-span-5 ${flip ? "md:order-1 md:col-start-1" : "md:col-start-8"}`}>
                    <p className="mono reveal text-[10px] text-muted">
                      {String(i + 1).padStart(2, "0")} · {guide.stages.length} stages · Free plan
                    </p>
                    <h2 className="display reveal mt-4 text-[clamp(44px,5.6vw,84px)] leading-[0.95]">{guide.skill}</h2>
                    <p className="reveal mt-5 max-w-[420px] text-[15px] leading-relaxed text-muted">{firstSentence(guide.answer)}</p>
                    <p className="mono reveal mt-8 inline-flex items-center gap-3 text-[11px]">
                      Read the guide
                      <span aria-hidden="true" className="transition-transform duration-300 group-hover:translate-x-1">→</span>
                    </p>
                  </div>
                </Link>
              </li>
            );
          })}
        </ol>
      </main>
      <Footer />
    </>
  );
}
