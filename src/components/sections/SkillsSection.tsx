import { skills } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { Curtain } from "@/components/ui/Curtain";

/** Every other card is set lower, so the row reads as a rhythm rather than a grid. */
const offset = ["", "lg:mt-24", "lg:mt-8", "lg:mt-32"];
const stagger = ["", "later", "", "later"];

/** The four big skills as tall photos, a name and a range each. */
export function SkillsSection() {
  const titleId = `${skills.id}-title`;
  return (
    <section id={skills.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={skills.eyebrow} title={skills.title} className="max-w-[640px]" />
      <ul className="mt-[clamp(56px,8vw,112px)] grid grid-cols-2 gap-x-3 gap-y-10 lg:grid-cols-4 lg:gap-x-5">
        {skills.items.map((skill, i) => (
          <li key={skill.name} className={`group ${offset[i]} ${i % 2 === 1 ? "mt-12" : ""}`}>
            <div className="overflow-hidden">
              <Curtain
                photo={skill.photo}
                sizes="(min-width: 1024px) 280px, 50vw"
                className="aspect-[3/4.3] transition-transform duration-[1200ms] ease-(--ease-out-soft) group-hover:scale-[1.03]"
              />
            </div>
            <div className={`reveal ${stagger[i]} mt-5`}>
              <h3 className="text-[clamp(20px,2vw,26px)] leading-none font-semibold tracking-[-0.02em]">{skill.name}</h3>
              <p className="mono mt-2.5 text-[10px] text-muted">{skill.range}</p>
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}
