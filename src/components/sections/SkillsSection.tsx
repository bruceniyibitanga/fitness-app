import { skills } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PhotoFill } from "@/components/ui/PhotoFill";

type Skill = (typeof skills.items)[number];

export function SkillsSection() {
  const titleId = `${skills.id}-title`;
  return (
    <section id={skills.id} aria-labelledby={titleId} className="border-b border-line">
      <div className="rail">
        <div className="gutter flex flex-wrap items-end justify-between gap-x-12 gap-y-4 pt-[clamp(48px,6vw,88px)] pb-10">
          <SectionHeading id={titleId} number={skills.number} label={skills.label} title={skills.title} className="flex-[1_1_520px]" />
          <p className="reveal max-w-[360px] flex-[0_1_360px] text-[17px] leading-normal text-muted">{skills.lede}</p>
        </div>
        <ul className="grid grid-cols-2 border-t border-line lg:grid-cols-4">
          {skills.items.map((skill, i) => (
            <li key={skill.name} className={`border-line ${i % 2 === 0 ? "border-r" : ""} ${i < 2 ? "border-b lg:border-b-0" : ""} lg:border-r lg:last:border-r-0`}>
              <SkillTile skill={skill} />
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function SkillTile({ skill }: { skill: Skill }) {
  const body = (
    <>
      <div className="absolute inset-0 transition-transform duration-[900ms] ease-(--ease-out-soft) group-hover:scale-105">
        <PhotoFill photo={skill.photo} sizes="(min-width: 1024px) 25vw, 50vw" />
      </div>
      <div className="shade-bottom absolute inset-0" aria-hidden="true" />
      <div className="absolute inset-x-0 bottom-0 p-6">
        <p className="mono text-[10px] text-muted">{skill.range}</p>
        <h3 className="mt-2 text-[32px] leading-none font-[750] tracking-[-0.02em] [font-stretch:90%]">{skill.name}</h3>
        <p className="mono mt-3.5 text-[11px] text-accent">
          {skill.href ? "Progression guide" : "Guide coming soon"}{" "}
          <span className="inline-block transition-transform duration-500 group-hover:translate-x-1.5" aria-hidden="true">→</span>
        </p>
      </div>
    </>
  );

  const className = "group relative block aspect-[3/4.4] overflow-hidden";
  return skill.href ? (
    <a href={skill.href} className={className}>
      {body}
    </a>
  ) : (
    <div className={className}>{body}</div>
  );
}
