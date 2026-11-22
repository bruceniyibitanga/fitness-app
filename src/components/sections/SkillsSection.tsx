import Link from "next/link";
import { skills } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

/**
 * Where each card sits on the grid and where its photo is centred. The two long, flat skills
 * (front lever, planche) get the wide cells so the athlete stays whole; the upright ones get
 * the narrow cells. Rows alternate wide-narrow, narrow-wide.
 */
const LAYOUT: Record<string, { cell: string; position: string }> = {
  "front-lever": { cell: "md:col-span-7", position: "62% 45%" },
  "muscle-up": { cell: "md:col-span-5", position: "50% 22%" },
  handstand: { cell: "md:col-span-5", position: "86% 50%" },
  planche: { cell: "md:col-span-7", position: "55% 40%" },
};
const ORDER = ["front-lever", "muscle-up", "handstand", "planche"];

/** The four skills as photo cards on a ruled grid, each opening its guide. */
export function SkillsSection({ n }: { n: string }) {
  const titleId = `${skills.id}-title`;
  const items = ORDER.map((slug) => skills.items.find((s) => s.guide === slug)).filter((s) => s !== undefined);
  return (
    <section id={skills.id} aria-labelledby={titleId} className="scroll-mt-8 border-b border-line">
      <div className="rail">
        <div className="cell flex flex-wrap items-end justify-between gap-x-12 gap-y-4 pb-10 md:pb-10">
          <NumberedTitle id={titleId} n={n} label={skills.eyebrow} title={skills.title} className="flex-[1_1_520px]" />
          <Link href="/guides" className="mono reveal inline-flex min-h-11 items-center gap-3 text-[11px] text-accent hover:text-accent-hover">
            All guides <span aria-hidden="true">→</span>
          </Link>
        </div>
        <ul className="grid gap-px border-t border-line bg-line md:grid-cols-12">
          {items.map((skill) => {
            const layout = LAYOUT[skill.guide];
            return (
              <li key={skill.guide} className={layout.cell}>
                <Link href={`/guides/${skill.guide}`} className="group on-photo zoom relative block h-[420px] overflow-hidden bg-surface md:h-[520px]">
                  <div className="absolute inset-0 transition-transform duration-[900ms] ease-[cubic-bezier(.2,.7,.1,1)] group-hover:scale-[1.04]">
                    <PhotoFill photo={skill.photo} sizes="(min-width: 768px) 58vw, 100vw" position={layout.position} />
                  </div>
                  <div aria-hidden="true" className="shade-up absolute inset-0" />
                  <div className="absolute inset-x-0 bottom-0 p-6">
                    <p className="mono text-[10px] text-muted">{skill.range}</p>
                    <h3 className="mt-2 text-[32px] leading-none font-[750] tracking-[-0.02em] [font-stretch:90%]">{skill.name}</h3>
                    <p className="mono mt-3.5 text-[11px] text-accent">
                      Progression guide{" "}
                      <span aria-hidden="true" className="inline-block transition-transform duration-300 group-hover:translate-x-1.5">
                        →
                      </span>
                    </p>
                  </div>
                </Link>
              </li>
            );
          })}
        </ul>
      </div>
    </section>
  );
}
