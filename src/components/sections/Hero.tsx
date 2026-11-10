import { hero } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

export function Hero() {
  // The first goal is repeated at the end so the loop has no visible jump.
  const goals = [...hero.goals, hero.goals[0]];

  return (
    <section aria-labelledby="hero-title" className="relative flex min-h-[760px] items-end border-b border-line md:min-h-[860px]">
      <PhotoFill photo="hero" sizes="100vw" priority position="70% center" motion="drift" />
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(90deg,rgb(11_11_10/0.94)_0%,rgb(11_11_10/0.72)_42%,rgb(11_11_10/0.08)_100%),linear-gradient(0deg,#0b0b0a_0%,transparent_38%)]"
      />

      <div className="relative w-full">
        <div className="gutter fade-back mx-auto grid max-w-[1320px] items-end gap-12 pt-40 pb-14 md:grid-cols-[1.25fr_0.75fr]">
          <div>
            <p className="mono intro text-xs text-muted">{hero.eyebrow}</p>
            <h1 id="hero-title" className="display intro intro-2 mt-5 max-w-[880px] text-[clamp(54px,7.4vw,112px)] leading-[0.92]">
              {hero.lead} <span className="sr-only">{hero.goals[0]}</span>
              <span className="rotator" aria-hidden="true">
                <span>
                  {goals.map((goal, i) => (
                    <span key={`${goal}-${i}`}>{goal}.</span>
                  ))}
                </span>
              </span>
            </h1>
            <p className="intro intro-3 mt-6 max-w-[500px] text-lg leading-normal text-soft">{hero.body}</p>
            <div id="join" className="intro intro-4 mt-8 max-w-[500px] scroll-mt-24">
              <WaitlistForm />
            </div>
          </div>

          <AdjustedCard />
        </div>

        <ul className="relative grid border-t border-line md:grid-cols-3">
          {hero.stats.map((stat) => (
            <li key={stat.value} className="gutter border-b border-line py-5 md:border-r md:border-b-0 md:last:border-r-0">
              <p className="text-[30px] font-[750] [font-stretch:90%]">{stat.value}</p>
              <p className="mono mt-1 text-[10px] text-muted">{stat.label}</p>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function AdjustedCard() {
  const { card } = hero;
  return (
    <div className="intro intro-4 hidden w-[280px] justify-self-end border border-tick bg-bg/80 backdrop-blur-md md:block">
      <div className="flex justify-between border-b border-line px-4 py-3.5">
        <span className="mono text-[10px] text-muted">{card.title}</span>
        <span className="mono text-[10px] text-accent">{card.badge}</span>
      </div>
      <div className="px-4 pt-[18px] pb-5">
        <p className="flex items-baseline gap-3">
          <s className="text-[28px] font-bold text-faint [font-stretch:85%]">{card.from}</s>
          <span className="display text-[64px] leading-[0.9]">{card.to}</span>
        </p>
        <dl className="mt-4 border-t border-line">
          {card.reasons.map((r) => (
            <div key={r.label} className="flex justify-between border-b border-line py-2.5 text-[13px]">
              <dt className="text-muted">{r.label}</dt>
              <dd>{r.value}</dd>
            </div>
          ))}
        </dl>
        <p className="mono mt-3 text-[10px] text-ice">{card.note}</p>
      </div>
    </div>
  );
}
