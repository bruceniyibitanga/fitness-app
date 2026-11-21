import { Fragment } from "react";
import { coach, type CoachCard, type CoachMoment } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/**
 * Meet Levi. Sells the experience, not the feature list: a few months of training told as a
 * timeline, each moment paired with the screen the app shows at that point. Moments alternate
 * sides, with wide photos between them so the story has room to breathe. Ends with the three
 * things the person never has to plan themselves.
 *
 * Server component with no state: everything comes from `coach` in site.ts.
 */
export function CoachSection() {
  const titleId = `${coach.id}-title`;
  const timelineId = `${coach.id}-timeline`;
  return (
    <section id={coach.id} aria-labelledby={titleId} className="section-pad">
      <div className="grid items-center gap-y-14 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-x-[clamp(40px,6vw,96px)]">
        <div className="inset-left pr-[clamp(20px,4vw,48px)] md:pr-0">
          <SectionTitle id={titleId} eyebrow={coach.eyebrow} title={coach.title} />
          <p className="reveal mt-7 max-w-[380px] text-lg leading-relaxed text-muted">{coach.body}</p>
        </div>
        <MaskedPhoto photo={coach.photo} sizes="(min-width: 768px) 58vw, 100vw" frame="aspect-[4/3] md:aspect-[6/5]" position="40% center" />
      </div>

      <div className="gutter mx-auto mt-[clamp(88px,12vw,168px)] max-w-[1200px]">
        <p id={timelineId} className="mono reveal text-[11px] text-muted">
          {coach.timelineLabel}
        </p>
        <ol aria-labelledby={timelineId} className="mt-10">
          {coach.moments.map((moment, index) => {
            const photoAfter = coach.breaks.find((b) => b.after === index);
            return (
              <Fragment key={moment.when}>
                <Moment moment={moment} flip={index % 2 === 1} />
                {photoAfter ? (
                  <li aria-hidden="true" className="py-[clamp(40px,6vw,80px)]">
                    <MaskedPhoto
                      photo={photoAfter.photo}
                      sizes="(min-width: 768px) 75vw, 100vw"
                      frame="aspect-[3/2] md:aspect-[21/9]"
                      className={index % 2 === 0 ? "md:mr-[16%]" : "md:ml-[16%]"}
                    />
                  </li>
                ) : null}
              </Fragment>
            );
          })}
        </ol>

        <BuiltIn />
        <p className="reveal mt-12 max-w-[560px] text-sm leading-relaxed text-faint">{coach.note}</p>
      </div>
    </section>
  );
}

type MomentProps = { moment: CoachMoment; flip: boolean };

/** One point in the timeline: words on one side, the app's screen on the other. */
function Moment({ moment, flip }: MomentProps) {
  return (
    <li className="grid gap-y-10 border-t border-line py-[clamp(48px,7vw,96px)] md:grid-cols-12 md:gap-x-6">
      <div className={`reveal md:col-span-5 ${flip ? "md:col-start-8" : "md:col-start-1"} md:row-start-1`}>
        <p className="mono text-[10px] text-accent">
          {moment.when} <span className="text-faint">·</span> <span className="text-muted">{moment.what}</span>
        </p>
        <h3 className="display mt-4 text-balance text-[clamp(30px,3.4vw,46px)] leading-[1.05]">{moment.title}</h3>
        <p className="mt-5 max-w-[400px] leading-relaxed text-muted">{moment.body}</p>
      </div>
      <div className={`reveal md:col-span-4 ${flip ? "md:col-start-2" : "md:col-start-8"} md:row-start-1 md:self-center`}>
        <AppCard card={moment.card} />
      </div>
    </li>
  );
}

/**
 * A small copy of what the app shows. Built from parts so each moment can show a different
 * screen without a component per screen. It's a picture of the app, so the choices aren't
 * buttons: nothing here can be pressed.
 */
function AppCard({ card }: { card: CoachCard }) {
  return (
    <figure className="border border-line bg-surface p-6 shadow-[0_24px_48px_-32px_rgba(23,22,19,0.35)]">
      <figcaption className="sr-only">What Levi shows in the app</figcaption>
      <p className="mono text-[10px] text-muted">{card.kicker}</p>
      {card.title ? <p className="display mt-3 text-[28px] leading-tight">{card.title}</p> : null}

      {card.change ? (
        <div className="mt-5">
          {card.change.from !== card.change.exercise ? <p className="text-sm text-soft">{card.change.exercise}</p> : null}
          <p className="mt-1 flex flex-wrap items-baseline gap-x-3">
            <s className="text-[20px] text-faint">{card.change.from}</s>
            <span aria-hidden="true" className="text-faint">
              →
            </span>
            <span className="display text-[44px] leading-[0.95]">{card.change.to}</span>
          </p>
        </div>
      ) : null}

      {card.noticed ? (
        <div className="mt-5">
          <p className="mono text-[10px] text-faint">What we noticed</p>
          <ul className="mt-2 space-y-1.5 text-sm text-soft">
            {card.noticed.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {card.reason ? (
        <p className="mt-5 border-l-2 border-accent pl-3 text-sm leading-relaxed text-text">{card.reason}</p>
      ) : null}

      {card.rows ? (
        <ul className="mt-5 divide-y divide-line border-y border-line">
          {card.rows.map((row) => (
            <li key={row.name} className="flex items-baseline justify-between gap-4 py-2.5 text-sm">
              <span className="text-soft">
                {row.name}
                {row.tag ? <span className="mono ml-2 text-[9px] text-ice">{row.tag}</span> : null}
              </span>
              <span className="mono text-[11px] text-muted">{row.dose}</span>
            </li>
          ))}
        </ul>
      ) : null}

      {card.choices ? (
        <ul className="mt-5 flex flex-wrap gap-2">
          {card.choices.map((choice, index) => (
            <li
              key={choice}
              className={`mono px-3 py-2 text-[10px] ${index === 0 ? "bg-text text-bg" : "border border-line text-soft"}`}
            >
              {choice}
            </li>
          ))}
        </ul>
      ) : null}

      {card.note ? <p className="mt-4 text-xs text-faint">{card.note}</p> : null}
    </figure>
  );
}

/** The three things built into every plan, in a row under a hairline. */
function BuiltIn() {
  const titleId = `${coach.id}-built-in`;
  return (
    <div aria-labelledby={titleId} role="group" className="mt-[clamp(64px,9vw,128px)] border-t border-text pt-10">
      <h3 id={titleId} className="display reveal max-w-[520px] text-balance text-[clamp(30px,3.4vw,46px)] leading-[1.05]">
        {coach.builtIn.title}
      </h3>
      <ul className="mt-12 grid gap-10 md:grid-cols-3 md:gap-6">
        {coach.builtIn.items.map((item, index) => (
          <li key={item.title} className="reveal">
            <p className="mono text-[10px] text-accent">{String(index + 1).padStart(2, "0")}</p>
            <p className="mt-3 text-lg text-text">{item.title}</p>
            <p className="mt-2 max-w-[320px] leading-relaxed text-muted">{item.body}</p>
          </li>
        ))}
      </ul>
    </div>
  );
}
