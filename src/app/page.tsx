import { faq, site } from "@/content/site";
import { JsonLd } from "@/components/JsonLd";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { Hero } from "@/components/sections/Hero";
import { Ticker } from "@/components/sections/Ticker";
import { PathSection } from "@/components/sections/PathSection";
import { AdaptsSection } from "@/components/sections/AdaptsSection";
import { SkillsSection } from "@/components/sections/SkillsSection";
import { CalculatorSection } from "@/components/sections/CalculatorSection";
import { WatchSection } from "@/components/sections/WatchSection";
import { KeepGoingSection } from "@/components/sections/KeepGoingSection";
import { ParksSection } from "@/components/sections/ParksSection";
import { FaqSection } from "@/components/sections/FaqSection";
import { CtaSection } from "@/components/sections/CtaSection";

const structuredData = {
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "SoftwareApplication",
      name: site.name,
      applicationCategory: "HealthApplication",
      operatingSystem: "iOS, watchOS",
      description: site.description,
      url: site.url,
    },
    {
      "@type": "FAQPage",
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
        <Ticker />
        <PathSection />
        <AdaptsSection />
        <SkillsSection />
        <CalculatorSection />
        <WatchSection />
        <KeepGoingSection />
        <ParksSection />
        <FaqSection />
        <CtaSection />
      </main>
      <Footer />
    </>
  );
}
