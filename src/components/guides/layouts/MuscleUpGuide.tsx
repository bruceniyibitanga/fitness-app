import type { Guide } from "@/content/guides";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { FaqList } from "@/components/ui/FaqList";
import { PlanOffer } from "../PlanOffer";
import { BeforeList, Breadcrumbs, GuideTitle, Mistakes, MoreGuides, Pass, WeekPlan } from "../parts";

/**
 * The ladder. A muscle-up is built rung by rung, so the stages climb a single vertical
 * line with the "before you start" list pinned beside them.
 */
export function MuscleUpGuide({ guide }: { guide: Guide }) {
  return (
    <article>
      <header className="grid md:min-h-[92svh] md:grid-cols-[minmax(0,1fr)_minmax(0,44vw)]">
        <div className="inset-left flex flex-col justify-end pt-36 pr-[clamp(20px,4vw,48px)] pb-14 md:pb-[clamp(56px,8vh,104px)]">
          <Breadcrumbs skill={guide.skill} />
          <p className="mono intro mt-12 text-[11px] text-accent">{guide.eyebrow}</p>
          <GuideTitle guide={guide} className="intro intro-2 mt-6 text-[clamp(56px,7.4vw,120px)] leading-[0.92]" />
          <p className="intro intro-3 mt-8 max-w-[520px] text-lg leading-relaxed text-soft">{guide.answer}</p>
          <a href="#plan" className="mono intro intro-3 mt-10 inline-flex min-h-11 items-center self-start text-[11px] underline-offset-4 hover:underline">
            Free printable plan <span aria-hidden="true" className="ml-2">↓</span>
          </a>
        </div>
        <MaskedPhoto
          photo={guide.photo}
          sizes="(min-width: 768px) 44vw, 100vw"
          onLoad
          priority
          position={guide.photoPosition}
          frame="aspect-[4/5] md:aspect-auto md:h-[calc(92svh-104px)]"
          className="md:pt-[104px]"
        />
      </header>

      <section aria-labelledby="ladder-title" className="gutter section-pad mx-auto grid max-w-[1200px] gap-y-14 md:grid-cols-12">
        <div className="md:sticky md:top-24 md:col-span-4 md:self-start">
          <SectionTitle id="ladder-title" eyebrow="The progression" title={`${guide.stages.length} rungs to your first rep.`} />
          <BeforeList items={guide.before} className="reveal mt-12" />
        </div>
        <ol className="relative md:col-span-7 md:col-start-6">
          <span aria-hidden="true" className="absolute top-5 bottom-5 left-[19px] w-px bg-line" />
          {guide.stages.map((stage, i) => (
            <li key={stage.name} className="reveal relative grid grid-cols-[40px_1fr] gap-x-6 pb-14 last:pb-0">
              <span className="mono relative flex size-10 items-center justify-center rounded-full border border-tick bg-bg text-[11px]">
                {String(i + 1).padStart(2, "0")}
              </span>
              <div className="pt-1">
                <h3 className="display text-[clamp(30px,3vw,40px)] leading-none">{stage.name}</h3>
                <Pass text={stage.pass} className="mt-4" />
                <p className="mt-2 max-w-[480px] text-[15px] leading-relaxed text-muted">{stage.cue}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section id="plan" aria-labelledby="plan-title" className="scroll-mt-8 bg-surface">
        <div className="gutter mx-auto grid max-w-[1200px] items-center gap-12 py-[clamp(72px,10vw,128px)] md:grid-cols-12">
          <div className="md:col-span-5">
            <p className="mono reveal text-[11px] text-accent">Free download</p>
            <h2 id="plan-title" className="h2 reveal mt-6">Take the ladder with you.</h2>
            <p className="reveal mt-6 max-w-[400px] text-[15px] leading-relaxed text-muted">{guide.plan.pitch}</p>
          </div>
          <PlanOffer guide={guide} className="reveal md:col-span-6 md:col-start-7" />
        </div>
      </section>

      <section aria-labelledby="mistakes-title" className="gutter section-pad mx-auto max-w-[1200px]">
        <SectionTitle id="mistakes-title" eyebrow="Common mistakes" title="What usually stalls it." className="max-w-[640px]" />
        <div className="mt-14">
          <Mistakes items={guide.mistakes} />
        </div>
      </section>

      <section aria-labelledby="week-title" className="gutter mx-auto max-w-[1200px] pb-[clamp(96px,13vw,184px)]">
        <SectionTitle id="week-title" eyebrow="A sample week" title="Three days, with rest between." className="max-w-[640px]" />
        <div className="mt-14">
          <WeekPlan week={guide.week} note={guide.weekNote} />
        </div>
      </section>

      <section aria-labelledby="faq-title" className="gutter mx-auto grid max-w-[1200px] gap-14 border-t border-line pt-[clamp(72px,10vw,128px)] md:grid-cols-[0.8fr_1.2fr]">
        <SectionTitle id="faq-title" eyebrow="Questions" title="Muscle-up questions." className="md:sticky md:top-24 md:self-start" />
        <FaqList items={guide.faq} name="guide-faq" />
      </section>

      <MoreGuides current={guide.slug} />
    </article>
  );
}
