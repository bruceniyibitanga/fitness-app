import type { Guide } from "@/content/guides";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { FaqList } from "@/components/ui/FaqList";
import { PlanOffer } from "../PlanOffer";
import { BeforeList, Breadcrumbs, GuideTitle, Mistakes, MoreGuides, Pass, WeekPlan } from "../parts";

/** How long the lever is at each stage, as a share of a full lever. Tuck is short; full is everything. */
const LEVER = [0.3, 0.48, 0.66, 0.82, 1];

/**
 * The horizontal one. A front lever is about length, so the page reads left to right:
 * a wide photo, then the stages side by side with a line that grows as the lever does.
 */
export function FrontLeverGuide({ guide }: { guide: Guide }) {
  return (
    <article>
      <header className="gutter mx-auto max-w-[1200px] pt-36">
        <Breadcrumbs skill={guide.skill} />
        <div className="mt-12 grid items-end gap-x-6 gap-y-8 md:grid-cols-12">
          <div className="md:col-span-7">
            <p className="mono intro text-[11px] text-accent">{guide.eyebrow}</p>
            <GuideTitle guide={guide} className="intro intro-2 mt-6 text-[clamp(56px,7.6vw,124px)] leading-[0.92]" />
          </div>
          <div className="intro intro-3 md:col-span-4 md:col-start-9">
            <p className="text-[17px] leading-relaxed text-soft">{guide.answer}</p>
            <a href="#plan" className="mono mt-6 inline-flex min-h-11 items-center text-[11px] underline-offset-4 hover:underline">
              Free printable plan <span aria-hidden="true" className="ml-2">↓</span>
            </a>
          </div>
        </div>
      </header>

      <MaskedPhoto
        photo={guide.photo}
        sizes="100vw"
        onLoad
        priority
        position={guide.photoPosition}
        frame="aspect-[4/3] sm:aspect-[21/9]"
        className="mt-14"
      />

      <section aria-labelledby="stages-title" className="section-pad">
        <div className="gutter mx-auto grid max-w-[1200px] gap-y-10 md:grid-cols-12">
          <SectionTitle id="stages-title" eyebrow="The progression" title="Make the lever longer, one step at a time." className="md:col-span-7" />
          <BeforeList items={guide.before} className="reveal md:col-span-4 md:col-start-9 md:self-end" />
        </div>
        <ol
          aria-label="Front lever stages, from easiest to hardest"
          tabIndex={0}
          className="gutter mx-auto mt-16 flex max-w-[1200px] scroll-px-[clamp(20px,4vw,48px)] snap-x snap-mandatory gap-5 overflow-x-auto pb-4 md:grid md:grid-cols-5 md:overflow-visible md:pb-0"
        >
          {guide.stages.map((stage, i) => (
            <li key={stage.name} className="reveal min-w-[78%] snap-start border-t border-text pt-6 sm:min-w-[44%] md:min-w-0">
              <LeverLine share={LEVER[i] ?? 1} />
              <p className="mono mt-6 text-[10px] text-muted">Stage {i + 1}</p>
              <h3 className="display mt-2 text-[30px] leading-none">{stage.name}</h3>
              <Pass text={stage.pass} className="mt-4" />
              <p className="mt-2 text-[14px] leading-relaxed text-muted">{stage.cue}</p>
            </li>
          ))}
        </ol>
      </section>

      <section id="plan" aria-labelledby="plan-title" className="gutter mx-auto max-w-[1200px] scroll-mt-8">
        <div className="grid items-center gap-12 border-y border-line py-[clamp(56px,8vw,96px)] md:grid-cols-12">
          <div className="md:col-span-5">
            <p className="mono reveal text-[11px] text-accent">Free download</p>
            <h2 id="plan-title" className="h2 reveal mt-6">Your front lever plan, ready to print.</h2>
          </div>
          <div className="md:col-span-6 md:col-start-7">
            <p className="reveal mb-8 max-w-[420px] text-[15px] leading-relaxed text-muted">{guide.plan.pitch}</p>
            <PlanOffer guide={guide} className="reveal" />
          </div>
        </div>
      </section>

      <div className="gutter section-pad mx-auto grid max-w-[1200px] gap-x-16 gap-y-20 md:grid-cols-2">
        <section aria-labelledby="mistakes-title">
          <SectionTitle id="mistakes-title" eyebrow="Common mistakes" title="What breaks the line." />
          <div className="mt-12">
            <Mistakes items={guide.mistakes} variant="stack" />
          </div>
        </section>
        <section aria-labelledby="week-title">
          <SectionTitle id="week-title" eyebrow="A sample week" title="Hold, pull, hold." />
          <div className="mt-12">
            <WeekPlan week={guide.week} note={guide.weekNote} variant="rows" />
          </div>
        </section>
      </div>

      <section aria-labelledby="faq-title" className="gutter mx-auto max-w-[760px]">
        <SectionTitle id="faq-title" eyebrow="Questions" title="Front lever questions." className="text-center [&_p]:text-center" />
        <FaqList items={guide.faq} name="guide-faq" className="mt-12" />
      </section>

      <MoreGuides current={guide.slug} />
    </article>
  );
}

/** A bar with a line hanging level from it. The line grows with the stage. Decorative. */
function LeverLine({ share }: { share: number }) {
  return (
    <svg viewBox="0 0 200 24" className="w-full" aria-hidden="true">
      <line x1="0" y1="4" x2="200" y2="4" stroke="currentColor" strokeWidth="1" className="text-tick" />
      <circle cx="8" cy="12" r="4" className="fill-text" />
      <line x1="8" y1="12" x2={8 + 188 * share} y2="12" stroke="currentColor" strokeWidth="3" strokeLinecap="round" className="text-accent" />
    </svg>
  );
}
