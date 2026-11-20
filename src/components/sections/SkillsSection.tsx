import { skills } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/**
 * Where to centre each photo so the athlete stays in frame. Levers and planches are long,
 * flat shapes, so the cards are landscape and every athlete fits whole.
 */
const position = ["50% 22%", "62% 45%", "55% 40%", "86% 50%"];

/** The four big skills, two by two, with the right-hand column set lower for rhythm. */
export function SkillsSection() {
  const titleId = `${skills.id}-title`;
  return (
    <section id={skills.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={skills.eyebrow} title={skills.title} className="max-w-[640px]" />
      <ul className="mt-[clamp(56px,8vw,104px)] grid gap-x-[clamp(16px,3vw,40px)] gap-y-14 sm:grid-cols-2 sm:pb-[clamp(48px,8vw,120px)]">
        {skills.items.map((skill, i) => (
          <li key={skill.name} className={i % 2 === 1 ? "sm:translate-y-[clamp(48px,8vw,120px)]" : ""}>
            <MaskedPhoto photo={skill.photo} sizes="(min-width: 640px) 50vw, 100vw" frame="aspect-[5/4]" position={position[i]} />
            <div className="mt-5 flex items-baseline justify-between gap-6">
              <h3 className="reveal display text-[clamp(28px,3vw,40px)] leading-none">{skill.name}</h3>
              <p className="reveal mono text-[10px] text-muted">{skill.range}</p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
