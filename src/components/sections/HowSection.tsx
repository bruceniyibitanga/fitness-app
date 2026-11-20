import { how } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";

/** Three steps, a number and a line each. */
export function HowSection() {
  const titleId = `${how.id}-title`;
  return (
    <section id={how.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={how.eyebrow} title={how.title} />
      <ol className="mt-[clamp(56px,8vw,112px)] grid gap-14 md:grid-cols-3 md:gap-10">
        {how.steps.map((step, i) => (
          <li key={step.title} className="reveal border-t border-line pt-8">
            <p className="display text-[64px] leading-none text-accent" aria-hidden="true">
              {i + 1}
            </p>
            <h3 className="mt-8 text-2xl font-semibold tracking-[-0.01em]">{step.title}</h3>
            <p className="mt-3 max-w-[300px] text-base leading-relaxed text-muted">{step.body}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
