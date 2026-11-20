import { faq } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";

/**
 * Native details/summary: keyboard and screen-reader friendly with no JavaScript. Every
 * answer is in the HTML even when closed, so search engines and AI answers can read it.
 */
export function FaqSection() {
  const titleId = `${faq.id}-title`;
  return (
    <section id={faq.id} aria-labelledby={titleId} className="gutter section-pad mx-auto grid max-w-[1200px] scroll-mt-8 gap-14 md:grid-cols-[0.8fr_1.2fr]">
      <SectionTitle id={titleId} eyebrow={faq.eyebrow} title={faq.title} className="md:sticky md:top-24 md:self-start" />
      <div className="border-t border-line">
        {faq.items.map((item) => (
          <details key={item.q} name="faq" className="group border-b border-line">
            <summary className="flex min-h-[76px] cursor-pointer list-none items-center justify-between gap-6 text-lg font-medium [&::-webkit-details-marker]:hidden">
              <h3>{item.q}</h3>
              <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" className="shrink-0 text-muted transition-transform duration-300 group-open:rotate-45">
                <path d="M3 8h10M8 3v10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="square" />
              </svg>
            </summary>
            <p className="max-w-[560px] pr-10 pb-7 text-base leading-relaxed text-muted">{item.a}</p>
          </details>
        ))}
      </div>
    </section>
  );
}
