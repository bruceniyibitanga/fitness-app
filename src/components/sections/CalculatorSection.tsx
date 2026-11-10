import { calculator } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PushUpCalculator } from "@/components/calculator/PushUpCalculator";

export function CalculatorSection() {
  const titleId = `${calculator.id}-title`;
  return (
    <section id={calculator.id} aria-labelledby={titleId} className="border-b border-line">
      <div className="rail gutter section-pad">
        <div className="flex flex-wrap items-end justify-between gap-x-12 gap-y-4">
          <SectionHeading id={titleId} number={calculator.number} label={calculator.label} title={calculator.title} className="flex-[1_1_520px]" />
          <p className="reveal max-w-[360px] flex-[0_1_360px] text-[17px] leading-normal text-muted">{calculator.lede}</p>
        </div>
        <div className="reveal mt-10">
          <PushUpCalculator />
        </div>
      </div>
    </section>
  );
}
