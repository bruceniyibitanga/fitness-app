import { calculator } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { PushUpCalculator } from "@/components/calculator/PushUpCalculator";

/** The question, its answer in one short paragraph, then the tool. */
export function CalculatorSection() {
  const titleId = `${calculator.id}-title`;
  return (
    <section id={calculator.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <div className="grid gap-10 md:grid-cols-[1.1fr_0.9fr] md:items-end">
        <SectionTitle id={titleId} eyebrow={calculator.eyebrow} title={calculator.title} />
        <p className="reveal max-w-[440px] text-lg leading-relaxed text-soft">{calculator.answer}</p>
      </div>
      <div className="reveal mt-[clamp(48px,6vw,80px)]">
        <PushUpCalculator />
      </div>
    </section>
  );
}
