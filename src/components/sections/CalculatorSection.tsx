import { calculator } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { PushUpCalculator } from "@/components/calculator/PushUpCalculator";

/** The question, its answer in one short paragraph beside it, then the tool. */
export function CalculatorSection({ n }: { n: string }) {
  const titleId = `${calculator.id}-title`;
  return (
    <section id={calculator.id} aria-labelledby={titleId} className="scroll-mt-8 border-b border-line">
      <div className="rail cell">
        <div className="flex flex-wrap items-end justify-between gap-x-12 gap-y-4">
          <NumberedTitle id={titleId} n={n} label={calculator.eyebrow} title={calculator.title} className="flex-[1_1_520px]" />
          <p className="reveal flex-[0_1_380px] text-[17px] leading-normal text-muted">{calculator.answer}</p>
        </div>
        <div className="reveal mt-10">
          <PushUpCalculator />
        </div>
      </div>
    </section>
  );
}
