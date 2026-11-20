import type { Guide } from "@/content/guides";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { FaqList } from "@/components/ui/FaqList";
import { PlanOffer } from "../PlanOffer";
import { Breadcrumbs, GuideTitle, Mistakes, MoreGuides, Pass, WeekPlan } from "../parts";

/** Photos set between stages, keyed by the stage they follow. */
const BETWEEN: Record<number, { photo: "join" | "hero"; caption: string; position: string }> = {
  2: { photo: "join", caption: "Kicking up beside the wall", position: "30% center" },
  4: { photo: "hero", caption: "Freestanding, one arm", position: "55% 45%" },
};

/**
 * The centred one. A handstand is about balance, so everything sits on one centre line:
 * the title, a narrow photo, and the stages stacked straight down with photos between.
 */
export function HandstandGuide({ guide }: { guide: Guide }) {
  return (
    <article>
      <header className="gutter mx-auto max-w-[920px] pt-36 text-center">
        <div className="flex justify-center">
          <Breadcrumbs skill={guide.skill} />
        </div>
        <p className="mono intro mt-12 text-[11px] text-accent">{guide.eyebrow}</p>
        <GuideTitle guide={guide} className="intro intro-2 mt-6 text-[clamp(56px,8vw,128px)] leading-[0.92]" />
        <p className="intro intro-3 mx-auto mt-8 max-w-[600px] text-lg leading-relaxed text-soft">{guide.answer}</p>
        <a href="#plan" className="mono intro intro-3 mt-8 inline-flex min-h-11 items-center text-[11px] underline-offset-4 hover:underline">
          Free printable plan <span aria-hidden="true" className="ml-2">↓</span>
        </a>
      </header>

      <MaskedPhoto
        photo={guide.photo}
        sizes="(min-width: 640px) 520px, 100vw"
        onLoad
        priority
        position={guide.photoPosition}
        frame="aspect-[4/5]"
        className="gutter mx-auto mt-14 max-w-[600px]"
      />

      <section aria-label="Before you start" className="gutter mx-auto mt-16 max-w-[920px]">
        <ul className="flex flex-wrap justify-center gap-x-8 gap-y-3 text-[15px]">
          {guide.before.map((item) => (
            <li key={item} className="flex items-baseline gap-3">
              <span aria-hidden="true" className="inline-block size-2 border border-text" />
              {item}
            </li>
          ))}
        </ul>
      </section>

      {guide.callout ? (
        <section aria-labelledby="callout-title" className="gutter mx-auto mt-[clamp(80px,11vw,152px)] max-w-[760px] text-center">
          <h2 id="callout-title" className="display reveal text-[clamp(40px,5vw,72px)] leading-[0.98]">{guide.callout.title}</h2>
          <p className="reveal mx-auto mt-6 max-w-[560px] text-[17px] leading-relaxed text-soft">{guide.callout.body}</p>
        </section>
      ) : null}

      <section aria-labelledby="stages-title" className="gutter section-pad mx-auto max-w-[760px] text-center">
        <SectionTitle id="stages-title" eyebrow="The progression" title="From the wall to the middle of the room." />
        <ol className="mt-16">
          {guide.stages.map((stage, i) => (
            <li key={stage.name}>
              <div className="reveal">
                <span aria-hidden="true" className="mx-auto block h-14 w-px bg-line" />
                <p className="mono mt-6 text-[10px] text-muted">Stage {i + 1}</p>
                <h3 className="display mt-3 text-[clamp(34px,4vw,52px)] leading-none">{stage.name}</h3>
                <Pass text={stage.pass} className="mt-5 justify-center" />
                <p className="mx-auto mt-2 max-w-[460px] text-[15px] leading-relaxed text-muted">{stage.cue}</p>
              </div>
              {BETWEEN[i] ? (
                <MaskedPhoto
                  photo={BETWEEN[i].photo}
                  sizes="(min-width: 640px) 440px, 100vw"
                  position={BETWEEN[i].position}
                  caption={BETWEEN[i].caption}
                  frame="aspect-[4/5]"
                  className="mx-auto mt-14 max-w-[440px]"
                />
              ) : null}
            </li>
          ))}
        </ol>
      </section>

      <section id="plan" aria-labelledby="plan-title" className="scroll-mt-8 bg-surface">
        <div className="gutter mx-auto max-w-[620px] py-[clamp(72px,10vw,128px)] text-center">
          <p className="mono reveal text-[11px] text-accent">Free download</p>
          <h2 id="plan-title" className="h2 reveal mt-6">The handstand plan.</h2>
          <p className="reveal mx-auto mt-6 max-w-[440px] text-[15px] leading-relaxed text-muted">{guide.plan.pitch}</p>
          <PlanOffer guide={guide} className="reveal mx-auto mt-10 max-w-[460px] text-left" />
        </div>
      </section>

      <section aria-labelledby="mistakes-title" className="gutter section-pad mx-auto max-w-[920px]">
        <SectionTitle id="mistakes-title" eyebrow="Common mistakes" title="Why it keeps tipping." className="text-center" />
        <div className="mt-12">
          <Mistakes items={guide.mistakes} />
        </div>
      </section>

      <section aria-labelledby="week-title" className="gutter mx-auto max-w-[1200px] pb-[clamp(96px,13vw,184px)]">
        <SectionTitle id="week-title" eyebrow="A sample week" title="Short and often." className="text-center" />
        <div className="mt-12">
          <WeekPlan week={guide.week} note={guide.weekNote} />
        </div>
      </section>

      <section aria-labelledby="faq-title" className="gutter mx-auto max-w-[760px]">
        <SectionTitle id="faq-title" eyebrow="Questions" title="Handstand questions." className="text-center" />
        <FaqList items={guide.faq} name="guide-faq" className="mt-12" />
      </section>

      <MoreGuides current={guide.slug} />
    </article>
  );
}
