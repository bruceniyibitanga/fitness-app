import Link from "next/link";
import type { Guide, Mistake, Session } from "@/content/guides";
import { guides } from "@/content/guides";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/** Home / Guides / Skill. Plain links; the structured data carries the same trail. */
export function Breadcrumbs({ skill }: { skill?: string }) {
  return (
    <nav aria-label="Breadcrumb" className="mono text-[10px] text-muted">
      <ol className="flex flex-wrap items-center gap-x-2.5">
        <li>
          <Link href="/" className="hover:text-text">Home</Link>
        </li>
        <li aria-hidden="true">/</li>
        <li>
          {skill ? <Link href="/guides" className="hover:text-text">Guides</Link> : <span aria-current="page">Guides</span>}
        </li>
        {skill ? (
          <>
            <li aria-hidden="true">/</li>
            <li aria-current="page" className="text-text">{skill}</li>
          </>
        ) : null}
      </ol>
    </nav>
  );
}

/** The "pass" test for a stage, set as a small label and a line of text. */
export function Pass({ text, className = "" }: { text: string; className?: string }) {
  return (
    <p className={`flex items-baseline gap-3 ${className}`}>
      <span className="mono shrink-0 text-[10px] text-accent">Pass</span>
      <span className="text-[15px] font-medium">{text}</span>
    </p>
  );
}

/** The short "before you start" list, as ticks. */
export function BeforeList({ items, className = "" }: { items: string[]; className?: string }) {
  return (
    <div className={className}>
      <p className="mono text-[10px] text-muted">Before you start</p>
      <ul className="mt-4 space-y-2.5">
        {items.map((item) => (
          <li key={item} className="flex items-baseline gap-3 text-[15px]">
            <span aria-hidden="true" className="inline-block size-2 shrink-0 translate-y-[-1px] border border-text" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}

/**
 * Common mistakes. "columns" sits two across, "numbered" is a single list with large
 * numerals, and "stack" is a plain list for narrow columns.
 */
export function Mistakes({ items, variant = "columns" }: { items: Mistake[]; variant?: "columns" | "numbered" | "stack" }) {
  if (variant === "stack") {
    return (
      <ul className="border-t border-line">
        {items.map((m) => (
          <li key={m.title} className="reveal border-b border-line py-6">
            <h3 className="text-lg font-medium">{m.title}</h3>
            <p className="mt-2 text-[15px] leading-relaxed text-muted">{m.fix}</p>
          </li>
        ))}
      </ul>
    );
  }
  if (variant === "numbered") {
    return (
      <ol className="divide-y divide-line border-y border-line">
        {items.map((m, i) => (
          <li key={m.title} className="reveal grid gap-x-8 gap-y-2 py-8 md:grid-cols-[80px_1fr_1.4fr]">
            <span className="display text-[40px] leading-none text-accent">{i + 1}</span>
            <h3 className="text-lg font-medium">{m.title}</h3>
            <p className="text-[15px] leading-relaxed text-muted">{m.fix}</p>
          </li>
        ))}
      </ol>
    );
  }
  return (
    <ul className="grid gap-x-10 gap-y-10 sm:grid-cols-2">
      {items.map((m) => (
        <li key={m.title} className="reveal border-t border-line pt-6">
          <h3 className="text-lg font-medium">{m.title}</h3>
          <p className="mt-2 text-[15px] leading-relaxed text-muted">{m.fix}</p>
        </li>
      ))}
    </ul>
  );
}

/** A sample week. "cards" puts the days side by side; "rows" stacks them like a timetable. */
export function WeekPlan({ week, note, variant = "cards" }: { week: Session[]; note: string; variant?: "cards" | "rows" }) {
  return (
    <div>
      {variant === "cards" ? (
        <div className="grid gap-4 md:grid-cols-3">
          {week.map((day) => (
            <section key={day.label} aria-label={`${day.label}: ${day.focus}`} className="reveal border border-line bg-surface p-6">
              <p className="mono text-[10px] text-muted">{day.label}</p>
              <h3 className="display mt-3 text-[30px] leading-none">{day.focus}</h3>
              <SessionItems items={day.items} className="mt-5" />
            </section>
          ))}
        </div>
      ) : (
        <div className="border-t border-line">
          {week.map((day) => (
            <section key={day.label} aria-label={`${day.label}: ${day.focus}`} className="reveal grid gap-x-8 gap-y-3 border-b border-line py-7 md:grid-cols-[160px_1fr]">
              <div>
                <p className="mono text-[10px] text-muted">{day.label}</p>
                <h3 className="display mt-2 text-[28px] leading-none">{day.focus}</h3>
              </div>
              <SessionItems items={day.items} />
            </section>
          ))}
        </div>
      )}
      <p className="mt-6 max-w-[560px] text-[14px] leading-relaxed text-muted">{note}</p>
    </div>
  );
}

function SessionItems({ items, className = "" }: { items: Session["items"]; className?: string }) {
  return (
    <ul className={`space-y-3 ${className}`}>
      {items.map((item) => (
        <li key={item.name} className="border-b border-line pb-3 text-[15px] last:border-b-0">
          <p>{item.name}</p>
          <p className="mono mt-1 text-[10px] text-muted">{item.dose}</p>
        </li>
      ))}
    </ul>
  );
}

/** "How to do a muscle-up" with the skill set in italics. */
export function GuideTitle({ guide, className = "" }: { guide: Guide; className?: string }) {
  const lead = "How to do a ";
  const rest = guide.h1.startsWith(lead) ? guide.h1.slice(lead.length) : null;
  return (
    <h1 className={`display ${className}`}>
      {rest ? (
        <>
          {lead}
          <em className="italic">{rest}</em>
        </>
      ) : (
        guide.h1
      )}
    </h1>
  );
}

/** Links to the other guides, so every page leads somewhere useful. */
export function MoreGuides({ current }: { current: Guide["slug"] }) {
  const others = guides.filter((g) => g.slug !== current);
  return (
    <section aria-labelledby="more-guides" className="gutter section-pad mx-auto max-w-[1200px]">
      <p id="more-guides" className="mono text-[11px] text-accent">More guides</p>
      <ul className="mt-10 grid gap-6 sm:grid-cols-3">
        {others.map((g) => (
          <li key={g.slug}>
            <Link href={`/guides/${g.slug}`} className="group block">
              <MaskedPhoto photo={g.photo} sizes="(min-width: 640px) 33vw, 100vw" frame="aspect-[4/3]" position={g.photoPosition} />
              <p className="display mt-4 flex items-baseline justify-between text-[28px] leading-none">
                {g.skill}
                <span aria-hidden="true" className="text-[18px] text-muted transition-transform duration-300 group-hover:translate-x-1">→</span>
              </p>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
