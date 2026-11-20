import { hero } from "@/content/site";
import { Curtain } from "@/components/ui/Curtain";

/**
 * A tall photo pinned to the right edge and a two-line headline that overlaps it.
 * One short line and one button; the form itself waits at the end of the page.
 */
export function Hero() {
  return (
    <section aria-labelledby="hero-title" className="relative isolate min-h-[100svh] overflow-hidden">
      <Curtain
        photo={hero.photo}
        sizes="(min-width: 768px) 56vw, 100vw"
        onLoad
        priority
        position="50% 40%"
        className="h-[76svh] md:absolute md:inset-y-0 md:right-0 md:h-auto md:w-[56vw]"
      >
        <div aria-hidden="true" className="absolute inset-0 bg-[linear-gradient(0deg,#0b0b0a_0%,rgb(11_11_10/0)_40%)] md:bg-[linear-gradient(90deg,rgb(11_11_10/0.55)_0%,rgb(11_11_10/0)_35%)]" />
      </Curtain>

      <div className="gutter fade-back relative z-10 mx-auto -mt-[22svh] flex max-w-[1200px] flex-col pb-20 md:mt-0 md:min-h-[100svh] md:justify-end md:pb-[clamp(72px,10vh,128px)]">
        <p className="mono intro text-[11px] text-soft">{hero.eyebrow}</p>
        <h1 id="hero-title" className="display mt-6 text-[clamp(64px,10.5vw,176px)] leading-[0.86]">
          {hero.title.map((line) => (
            <span key={line} className="line block overflow-hidden pb-[0.04em]">
              <span>{line}</span>
            </span>
          ))}
        </h1>
        <div className="intro intro-3 mt-10 flex flex-wrap items-center gap-x-10 gap-y-6">
          <a href="#join" className="inline-flex min-h-12 items-center bg-accent px-7 text-[15px] font-semibold text-bg transition-colors hover:bg-accent-hover">
            {hero.cta}
          </a>
          <p className="max-w-[300px] text-base leading-snug text-soft">{hero.body}</p>
        </div>
      </div>
    </section>
  );
}
