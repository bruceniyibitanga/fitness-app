import { faq } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";

/** Native details/summary: keyboard and screen-reader friendly with no JavaScript. */
export function FaqSection() {
  return (
    <section aria-labelledby="faq-title" className="border-b border-line">
      <div className="rail gutter section-pad grid gap-12 md:grid-cols-[0.8fr_1.2fr]">
        <SectionHeading id="faq-title" number={faq.number} label={faq.label} title={faq.title} />
        <div className="border-t border-line">
          {faq.items.map((item, i) => (
            <details key={item.q} name="faq" open={i === 0} className="group border-b border-line">
              <summary className="flex min-h-[68px] cursor-pointer list-none items-center justify-between gap-4 text-[19px] font-semibold [&::-webkit-details-marker]:hidden">
                {item.q}
                <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" className="shrink-0 text-muted">
                  <path d="M3 8h10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="square" />
                  <path d="M8 3v10" stroke="currentColor" strokeWidth="1.8" strokeLinecap="square" className="group-open:hidden" />
                </svg>
              </summary>
              <p className="pr-10 pb-5 text-base leading-relaxed text-muted">{item.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  );
}
