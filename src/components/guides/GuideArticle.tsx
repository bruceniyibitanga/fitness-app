import type { Guide } from "@/content/guides";
import { guides } from "@/content/guides";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { FaqList } from "@/components/ui/FaqList";
import { PlanOffer } from "./PlanOffer";
import { Breadcrumbs, GuideCard, GuideTitle, StageMeter } from "./parts";

/** A two-digit section number, like "01". */
const num = (n: number) => String(n).padStart(2, "0");

/**
 * One guide, on the same ruled grid as the home page: a 1320px rail with hairlines down
 * both sides and a rule across under every section. All four guides share this layout,
 * so they read as one set; each keeps its own photo, section titles and safety note.
 */
export function GuideArticle({ guide }: { guide: Guide }) {
  // The safety note, where there is one, isn't numbered, so the numbers stay in step.
  return (
    <article>
      <GuideHeader guide={guide} />
      {guide.callout ? <Callout title={guide.callout.title} body={guide.callout.body} /> : null}
      <Stages guide={guide} n={num(1)} />
      <Plan guide={guide} n={num(2)} />
      <Mistakes guide={guide} n={num(3)} />
      <Week guide={guide} n={num(4)} />
      <Questions guide={guide} n={num(5)} />
      <MoreGuides current={guide.slug} n={num(6)} />
    </article>
  );
}

/**
 * The opener: the title and answer on the left, one tall photo running to the right edge,
 * starting under the navigation like the home page hero.
 */
function GuideHeader({ guide }: { guide: Guide }) {
  return (
    <header className="grid border-b border-line md:min-h-[88svh] md:grid-cols-[minmax(0,1fr)_minmax(0,44vw)]">
      <div className="inset-left flex flex-col justify-end pt-32 pr-[clamp(20px,4vw,48px)] pb-14 md:pb-[clamp(56px,8vh,104px)]">
        <Breadcrumbs skill={guide.skill} />
        <p className="mono intro mt-10 text-[11px] text-muted">{guide.eyebrow}</p>
        <GuideTitle guide={guide} className="intro intro-2 mt-6 text-[clamp(52px,7vw,116px)] leading-[0.88]" />
        <p className="intro intro-3 mt-8 max-w-[540px] text-lg leading-relaxed text-soft">{guide.answer}</p>
        <div className="intro intro-3 mt-10 flex flex-wrap items-center gap-x-8 gap-y-4">
          <a href="#plan" className="button-accent inline-flex min-h-13 items-center gap-3 px-7 text-[15px]">
            Free printable plan <span aria-hidden="true">↓</span>
          </a>
          <ul aria-label="Before you start" className="mono flex flex-wrap gap-x-5 gap-y-2 text-[10px] text-muted">
            {guide.before.map((item) => (
              <li key={item} className="flex items-center gap-2">
                <span aria-hidden="true" className="size-1.5 border border-text" />
                {item}
              </li>
            ))}
          </ul>
        </div>
      </div>
      <MaskedPhoto
        photo={guide.photo}
        sizes="(min-width: 768px) 44vw, 100vw"
        onLoad
        priority
        position={guide.photoPosition}
        frame="aspect-[4/5] md:aspect-auto md:min-h-[480px] md:flex-1"
        className="md:flex md:flex-col md:pt-[104px]"
      />
    </header>
  );
}

/** A safety note set large in its own ruled band, with an accent rule on the left. */
function Callout({ title, body }: { title: string; body: string }) {
  return (
    <section aria-labelledby="callout-title" className="border-b border-line bg-surface">
      <div className="rail cell grid gap-x-12 gap-y-6 md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] md:items-end">
        <div className="border-l-2 border-accent pl-6">
          <p className="mono reveal text-[11px] text-accent">Read this first</p>
          <h2 id="callout-title" className="headline reveal mt-4 text-[clamp(34px,4.2vw,60px)] text-balance">
            {title}
          </h2>
        </div>
        <p className="reveal max-w-[520px] text-[17px] leading-relaxed text-soft">{body}</p>
      </div>
    </section>
  );
}

/**
 * The stages, like the home page's "how it works": the title pinned on the left, the stages
 * scrolling past on the right, each lighting up as it crosses the middle of the screen.
 */
function Stages({ guide, n }: { guide: Guide; n: string }) {
  const count = guide.stages.length;
  return (
    <section aria-labelledby="stages-title" className="border-b border-line">
      <div className="rail grid md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <div className="cell border-b border-line md:border-r md:border-b-0">
          <NumberedTitle id="stages-title" n={n} label="The progression" title={guide.headings.stages} className="md:sticky md:top-10" />
        </div>
        <ol className="cell" aria-label={`${guide.skill} stages, from first to last`}>
          {guide.stages.map((stage, i) => (
            <li
              key={stage.name}
              className="step-on grid grid-cols-[56px_1fr] gap-4 border-t border-line py-9 first:border-t-0 first:pt-0 md:grid-cols-[72px_1fr] md:first:border-t md:first:pt-9"
            >
              <span className="mono pt-2 text-[12px] text-accent">{num(i + 1)}</span>
              <div>
                <StageMeter at={i} of={count} />
                <h3 className="mt-4 text-[clamp(28px,3vw,40px)] leading-none font-bold tracking-[-0.025em]">{stage.name}</h3>
                <p className="mt-4 flex items-baseline gap-3">
                  <span className="mono shrink-0 text-[10px] text-accent">Pass</span>
                  <span className="text-[16px] font-medium">{stage.pass}</span>
                </p>
                <p className="mt-2 max-w-[460px] text-[15px] leading-relaxed text-muted">{stage.cue}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

/** The free plan: the pitch on the left, the form on the right, on the surface colour. */
function Plan({ guide, n }: { guide: Guide; n: string }) {
  return (
    <section id="plan" aria-labelledby="plan-title" className="scroll-mt-8 border-b border-line bg-surface">
      <div className="rail cell grid items-end gap-x-12 gap-y-10 md:grid-cols-2">
        <div>
          <NumberedTitle id="plan-title" n={n} label="Free download" title={guide.headings.plan} />
          <p className="reveal mt-6 max-w-[440px] text-[16px] leading-relaxed text-muted">{guide.plan.pitch}</p>
        </div>
        <PlanOffer guide={guide} className="reveal" />
      </div>
    </section>
  );
}

/** Common mistakes as ruled cells, two across. */
function Mistakes({ guide, n }: { guide: Guide; n: string }) {
  return (
    <section aria-labelledby="mistakes-title" className="border-b border-line">
      <div className="rail">
        <div className="cell pb-10 md:pb-10">
          <NumberedTitle id="mistakes-title" n={n} label="Common mistakes" title={guide.headings.mistakes} />
        </div>
        <ul className="grid gap-px border-t border-line bg-line sm:grid-cols-2">
          {guide.mistakes.map((m, i) => (
            <li key={m.title} className="reveal bg-bg p-[clamp(24px,3vw,40px)]">
              <p className="mono text-[10px] text-accent">{num(i + 1)}</p>
              <h3 className="mt-3 text-[22px] leading-tight font-bold tracking-[-0.015em]">{m.title}</h3>
              <p className="mt-3 max-w-[460px] text-[15px] leading-relaxed text-muted">{m.fix}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

/** A sample week as ruled day columns, each exercise with its dose set in mono. */
function Week({ guide, n }: { guide: Guide; n: string }) {
  return (
    <section aria-labelledby="week-title" className="border-b border-line">
      <div className="rail">
        <div className="cell pb-10 md:pb-10">
          <NumberedTitle id="week-title" n={n} label="A sample week" title={guide.headings.week} />
        </div>
        <div className="grid gap-px border-y border-line bg-line md:grid-cols-3">
          {guide.week.map((day) => (
            <section key={day.label} aria-label={`${day.label}: ${day.focus}`} className="reveal bg-bg p-[clamp(24px,3vw,40px)]">
              <p className="mono text-[10px] text-muted">{day.label}</p>
              <h3 className="figure mt-2 text-[40px] leading-none">{day.focus}</h3>
              <ul className="mt-6 border-t border-line">
                {day.items.map((item) => (
                  <li key={item.name} className="flex items-baseline justify-between gap-4 border-b border-line py-3.5 text-[15px]">
                    <span>{item.name}</span>
                    <span className="mono shrink-0 text-[10px] text-muted">{item.dose}</span>
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
        <p className="cell max-w-[720px] py-8 text-[14px] leading-relaxed text-muted md:py-8">{guide.weekNote}</p>
      </div>
    </section>
  );
}

/** The guide's questions, laid out like the home page FAQ. */
function Questions({ guide, n }: { guide: Guide; n: string }) {
  return (
    <section aria-labelledby="faq-title" className="border-b border-line">
      <div className="rail cell grid gap-12 md:grid-cols-[minmax(0,0.8fr)_minmax(0,1.2fr)]">
        <NumberedTitle id="faq-title" n={n} label="Questions" title={`${guide.skill} questions.`} className="md:sticky md:top-24 md:self-start" />
        <FaqList items={guide.faq} name="guide-faq" />
      </div>
    </section>
  );
}

/** The other three guides as photo cards, so every page leads somewhere useful. */
function MoreGuides({ current, n }: { current: Guide["slug"]; n: string }) {
  const others = guides.filter((g) => g.slug !== current);
  return (
    <section aria-labelledby="more-guides" className="border-b border-line">
      <div className="rail">
        <div className="cell pb-10 md:pb-10">
          <NumberedTitle id="more-guides" n={n} label="More guides" title="Pick your next skill." />
        </div>
        <ul className="grid gap-px border-t border-line bg-line sm:grid-cols-3">
          {others.map((g) => (
            <li key={g.slug}>
              <GuideCard guide={g} heading="h3" sizes="(min-width: 640px) 33vw, 100vw" className="h-[380px] md:h-[440px]" />
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
