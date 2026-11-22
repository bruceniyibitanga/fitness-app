import { faq } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { FaqList } from "@/components/ui/FaqList";

/** The home page FAQ. The same questions are in the JSON-LD on the page. */
export function FaqSection({ n }: { n: string }) {
  const titleId = `${faq.id}-title`;
  return (
    <section id={faq.id} aria-labelledby={titleId} className="scroll-mt-8 border-b border-line">
      <div className="rail cell grid gap-12 md:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]">
        <NumberedTitle id={titleId} n={n} label={faq.eyebrow} title={faq.title} className="md:sticky md:top-24 md:self-start" />
        <FaqList items={faq.items} name="faq" />
      </div>
    </section>
  );
}
