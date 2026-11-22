import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { GUIDE_SLUGS, getGuide } from "@/content/guides";
import { photos } from "@/content/photos";
import { guideStructuredData } from "@/lib/guide-schema";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { GuideArticle } from "@/components/guides/GuideArticle";

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
  return (
    <>
      <JsonLd data={guideStructuredData(guide)} />
      <Header />
      <main>
        <GuideArticle guide={guide} />
      </main>
      <Footer />
    </>
  );
}
