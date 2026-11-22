import { hero } from "@/content/site";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/**
 * The opener: the headline on the left in heavy condensed Archivo, the promise in accent,
 * and one tall photo running to the right edge of the screen. One line, one button.
 */
export function Hero() {
  return (
    <section aria-labelledby="hero-title" className="grid min-h-[100svh] border-b border-line md:grid-cols-[minmax(0,1fr)_minmax(0,48vw)]">
      <div className="inset-left flex flex-col justify-end pt-36 pr-[clamp(20px,4vw,48px)] pb-14 md:pb-[clamp(56px,9vh,112px)]">
        <p className="mono intro text-[11px] text-muted">{hero.eyebrow}</p>
        <h1 id="hero-title" className="figure intro intro-2 mt-8 text-[clamp(60px,8vw,132px)] leading-[0.88]">
          {hero.title[0]}
          <br />
          <span className="text-accent">{hero.title[1]}</span>
        </h1>
        <div className="intro intro-3 mt-12 flex flex-wrap items-center gap-x-10 gap-y-6">
          <a href="#join" className="button-accent inline-flex min-h-13 items-center px-7 text-[15px]">
            {hero.cta}
          </a>
          <p className="max-w-[280px] text-[15px] leading-snug text-muted">{hero.body}</p>
        </div>
      </div>
      <MaskedPhoto
        photo={hero.photo}
        sizes="(min-width: 768px) 48vw, 100vw"
        onLoad
        priority
        position="55% 45%"
        frame="aspect-[4/5] md:aspect-auto md:h-[calc(100svh-104px)]"
        className="md:pt-[104px]"
      />
    </section>
  );
}
