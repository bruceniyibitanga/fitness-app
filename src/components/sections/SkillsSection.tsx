import { skills } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

/** The four big skills as tall photos, a name and a range each. */
export function SkillsSection() {
  const titleId = `${skills.id}-title`;
  return (
    <section id={skills.id} aria-labelledby={titleId} className="section-pad scroll-mt-8">
      <div className="gutter mx-auto max-w-[1200px]">
        <SectionTitle id={titleId} eyebrow={skills.eyebrow} title={skills.title} />
      </div>
      <ul className="gutter mx-auto mt-[clamp(48px,7vw,96px)] grid max-w-[1200px] grid-cols-2 gap-3 lg:grid-cols-4">
        {skills.items.map((skill) => (
          <li key={skill.name} className="reveal group relative aspect-[3/4.2] overflow-hidden">
            <div className="absolute inset-0 transition-transform duration-[1200ms] ease-(--ease-out-soft) group-hover:scale-105">
              <PhotoFill photo={skill.photo} sizes="(min-width: 1024px) 300px, 50vw" />
            </div>
            <div className="shade-bottom absolute inset-0" aria-hidden="true" />
            <div className="absolute inset-x-0 bottom-0 p-[clamp(16px,2vw,24px)]">
              <h3 className="text-[clamp(22px,2.4vw,30px)] leading-none font-semibold tracking-[-0.02em]">{skill.name}</h3>
              <p className="mono mt-3 text-[10px] text-soft">{skill.range}</p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
