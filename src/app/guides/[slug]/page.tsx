import type { Metadata } from "next";
import { notFound } from "next/navigation";
import type { ComponentType } from "react";
import { GUIDE_SLUGS, getGuide, type Guide, type GuideSlug } from "@/content/guides";
import { photos } from "@/content/photos";
import { guideStructuredData } from "@/lib/guide-schema";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { MuscleUpGuide } from "@/components/guides/layouts/MuscleUpGuide";
import { FrontLeverGuide } from "@/components/guides/layouts/FrontLeverGuide";
import { PlancheGuide } from "@/components/guides/layouts/PlancheGuide";
import { HandstandGuide } from "@/components/guides/layouts/HandstandGuide";

/** Each guide has its own layout. The content is shared; the composition isn't. */
const LAYOUTS: Record<GuideSlug, ComponentType<{ guide: Guide }>> = {
  "muscle-up": MuscleUpGuide,
  "front-lever": FrontLeverGuide,
  planche: PlancheGuide,
  handstand: HandstandGuide,
};

// Only the four guides exist; anything else is a 404 rather than a render attempt.
export const dynamicParams = false;

export function generateStaticParams() {
  return GUIDE_SLUGS.map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: PageProps<"/guides/[slug]">): Promise<Metadata> {
  const guide = getGuide((await params).slug);
  if (!guide) return {};
  const url = `/guides/${guide.slug}`;
  return {
    title: guide.title,
    description: guide.description,
    alternates: { canonical: url },
    openGraph: {
      type: "article",
      url,
      title: guide.title,
      description: guide.description,
      images: [{ url: photos[guide.photo].src, alt: photos[guide.photo].alt }],
    },
    twitter: { card: "summary_large_image", title: guide.title, description: guide.description },
  };
}

export default async function GuidePage({ params }: PageProps<"/guides/[slug]">) {
  const guide = getGuide((await params).slug);
  if (!guide) notFound();
  const Layout = LAYOUTS[guide.slug];
  return (
    <>
      <JsonLd data={guideStructuredData(guide)} />
      <Header />
      <main>
        <Layout guide={guide} />
      </main>
      <Footer />
    </>
  );
}
