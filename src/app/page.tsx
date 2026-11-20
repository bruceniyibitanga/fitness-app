import { faq, site } from "@/content/site";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Hero } from "@/components/sections/Hero";
import { HowSection } from "@/components/sections/HowSection";
import { AdaptsSection } from "@/components/sections/AdaptsSection";
import { SkillsSection } from "@/components/sections/SkillsSection";
import { CalculatorSection } from "@/components/sections/CalculatorSection";
import { WatchPrivacySection } from "@/components/sections/WatchPrivacySection";
import { FaqSection } from "@/components/sections/FaqSection";
import { CtaSection } from "@/components/sections/CtaSection";

/**
 * What the page says about itself to search engines and AI assistants: who makes Levitx,
 * what it is and runs on, and the FAQ word for word. Each node has an @id so they link up.
 * No ratings or prices are claimed, because there are none yet.
 */
const structuredData = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "Organization",
      "@id": `${site.url}/#publisher`,
      name: site.publisher.name,
      url: site.publisher.url,
      address: {
        "@type": "PostalAddress",
        addressLocality: site.publisher.city,
        addressRegion: site.publisher.region,
        addressCountry: site.publisher.country,
      },
    },
    {
      "@type": "WebSite",
      "@id": `${site.url}/#website`,
      url: site.url,
      name: site.name,
      inLanguage: "en-AU",
      publisher: { "@id": `${site.url}/#publisher` },
    },
    {
      "@type": "MobileApplication",
      "@id": `${site.url}/#app`,
      name: site.name,
      url: site.url,
      description: site.description,
      applicationCategory: "HealthApplication",
      operatingSystem: site.platforms.join(", "),
      image: `${site.url}/opengraph-image.png`,
      publisher: { "@id": `${site.url}/#publisher` },
      author: { "@id": `${site.url}/#publisher` },
    },
    {
      "@type": "FAQPage",
      "@id": `${site.url}/#faq`,
      mainEntity: faq.items.map((item) => ({
        "@type": "Question",
        name: item.q,
        acceptedAnswer: { "@type": "Answer", text: item.a },
      })),
    },
  ],
};

export default function Home() {
  return (
    <>
      <JsonLd data={structuredData} />
      <Header />
      <main>
        <Hero />
        <HowSection />
        <AdaptsSection />
        <SkillsSection />
        <CalculatorSection />
        <WatchPrivacySection />
        <FaqSection />
        <CtaSection />
      </main>
      <Footer />
    </>
  );
}
