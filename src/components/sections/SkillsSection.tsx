import { skills } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/** Every other card sits a little lower, so the row has a rhythm rather than a ruler edge. */
const offset = ["", "lg:mt-20", "", "lg:mt-20"];

/** Where to centre each photo so the athlete stays in frame at a tall crop. */
const position = ["50% 30%", "50% 40%", "45% 50%", "86% 50%"];

/** The four big skills as tall photos, a name and a range each. */
export function SkillsSection() {
  const titleId = `${skills.id}-title`;
  return (
    <section id={skills.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={skills.eyebrow} title={skills.title} className="max-w-[640px]" />
      <ul className="mt-[clamp(56px,8vw,104px)] grid grid-cols-2 gap-x-4 gap-y-12 lg:grid-cols-4 lg:gap-x-6">
        {skills.items.map((skill, i) => (
          <li key={skill.name} className={offset[i]}>
            <MaskedPhoto photo={skill.photo} sizes="(min-width: 1024px) 280px, 50vw" frame="aspect-[3/4]" position={position[i]} />
            <h3 className="reveal display mt-5 text-[clamp(26px,2.6vw,34px)] leading-none">{skill.name}</h3>
            <p className="reveal mono mt-2.5 text-[10px] text-muted">{skill.range}</p>
          </li>
        ))}
      </ul>
    </section>
  );
}
