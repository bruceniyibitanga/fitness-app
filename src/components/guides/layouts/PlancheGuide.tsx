import type { Guide } from "@/content/guides";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { FaqList } from "@/components/ui/FaqList";
import { PlanOffer } from "../PlanOffer";
import { BeforeList, Breadcrumbs, GuideTitle, Mistakes, MoreGuides, Pass, WeekPlan } from "../parts";

/**
 * The staggered grid. The planche is the longest road of the four, so the stages sit as
 * cards that step down the page, with the wrist warning set large before any of them.
 */
export function PlancheGuide({ guide }: { guide: Guide }) {
  return (
    <article>
      <header className="gutter mx-auto grid max-w-[1200px] items-end gap-x-6 gap-y-12 pt-36 md:grid-cols-12">
        <div className="md:col-span-6">
          <Breadcrumbs skill={guide.skill} />
          <p className="mono intro mt-12 text-[11px] text-accent">{guide.eyebrow}</p>
          <GuideTitle guide={guide} className="intro intro-2 mt-6 text-[clamp(56px,7.4vw,120px)] leading-[0.92]" />
          <p className="intro intro-3 mt-8 max-w-[480px] text-lg leading-relaxed text-soft">{guide.answer}</p>
          <a href="#plan" className="mono intro intro-3 mt-8 inline-flex min-h-11 items-center text-[11px] underline-offset-4 hover:underline">
            Free printable plan <span aria-hidden="true" className="ml-2">↓</span>
          </a>
        </div>
        <MaskedPhoto
          photo={guide.photo}
          sizes="(min-width: 768px) 42vw, 100vw"
          onLoad
          priority
          position={guide.photoPosition}
          frame="aspect-square"
          className="md:col-span-5 md:col-start-8"
        />
      </header>

      {guide.callout ? (
        <section aria-labelledby="callout-title" className="gutter mx-auto mt-[clamp(80px,11vw,152px)] max-w-[1200px]">
          <div className="grid gap-8 border-y border-line py-[clamp(48px,7vw,88px)] md:grid-cols-12">
            <h2 id="callout-title" className="display reveal text-[clamp(40px,5vw,72px)] leading-[0.98] md:col-span-6">
              {guide.callout.title}
            </h2>
            <p className="reveal text-[17px] leading-relaxed text-soft md:col-span-5 md:col-start-8 md:self-end">{guide.callout.body}</p>
          </div>
        </section>
      ) : null}

      <section aria-labelledby="stages-title" className="gutter section-pad mx-auto max-w-[1200px]">
        <div className="grid gap-y-10 md:grid-cols-12">
          <SectionTitle id="stages-title" eyebrow="The progression" title="From a lean to a full planche." className="md:col-span-7" />
          <BeforeList items={guide.before} className="reveal md:col-span-4 md:col-start-9 md:self-end" />
        </div>
        <ol className="mt-16 grid gap-6 md:grid-cols-2 md:pb-32">
          {guide.stages.map((stage, i) => (
            <li key={stage.name} className={`reveal border border-line bg-surface p-[clamp(24px,3vw,40px)] ${i % 2 === 1 ? "md:translate-y-32" : ""}`}>
              <span className="display block text-[clamp(64px,7vw,96px)] leading-[0.8] text-tick">{i + 1}</span>
              <h3 className="display mt-6 text-[clamp(30px,3vw,40px)] leading-none">{stage.name}</h3>
              <Pass text={stage.pass} className="mt-5" />
              <p className="mt-2 text-[15px] leading-relaxed text-muted">{stage.cue}</p>
            </li>
          ))}
        </ol>
      </section>

      <section aria-labelledby="mistakes-title" className="gutter mx-auto max-w-[1200px]">
        <SectionTitle id="mistakes-title" eyebrow="Common mistakes" title="Four things that hold people back." className="max-w-[680px]" />
        <div className="mt-12">
          <Mistakes items={guide.mistakes} variant="numbered" />
        </div>
      </section>

      <section id="plan" aria-labelledby="plan-title" className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
        <div className="grid overflow-hidden border border-line bg-surface md:grid-cols-2">
          <MaskedPhoto photo={guide.photo} sizes="(min-width: 768px) 50vw, 100vw" position="62% 40%" frame="aspect-[4/3] md:aspect-auto md:h-full md:min-h-[440px]" />
          <div className="p-[clamp(28px,4vw,56px)]">
            <p className="mono reveal text-[11px] text-accent">Free download</p>
            <h2 id="plan-title" className="h2 reveal mt-6">The planche plan.</h2>
            <p className="reveal mt-6 max-w-[400px] text-[15px] leading-relaxed text-muted">{guide.plan.pitch}</p>
            <PlanOffer guide={guide} className="reveal mt-10" />
          </div>
        </div>
      </section>

      <section aria-labelledby="week-title" className="gutter mx-auto max-w-[1200px] pb-[clamp(96px,13vw,184px)]">
        <SectionTitle id="week-title" eyebrow="A sample week" title="Lean, hold, press." className="max-w-[640px]" />
        <div className="mt-12">
          <WeekPlan week={guide.week} note={guide.weekNote} variant="rows" />
        </div>
      </section>

      <section aria-labelledby="faq-title" className="gutter mx-auto grid max-w-[1200px] gap-14 md:grid-cols-[0.8fr_1.2fr]">
        <SectionTitle id="faq-title" eyebrow="Questions" title="Planche questions." className="md:sticky md:top-24 md:self-start" />
        <FaqList items={guide.faq} name="guide-faq" />
      </section>

      <MoreGuides current={guide.slug} />
    </article>
  );
}
