import { faq } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { FaqList } from "@/components/ui/FaqList";

/** The home page FAQ. The same questions are in the JSON-LD on the page. */
export function FaqSection() {
  const titleId = `${faq.id}-title`;
  return (
    <section id={faq.id} aria-labelledby={titleId} className="gutter section-pad mx-auto grid max-w-[1200px] scroll-mt-8 gap-14 md:grid-cols-[0.8fr_1.2fr]">
      <SectionTitle id={titleId} eyebrow={faq.eyebrow} title={faq.title} className="md:sticky md:top-24 md:self-start" />
      <FaqList items={faq.items} name="faq" />
    </section>
  );
}
