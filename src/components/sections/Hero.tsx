import { hero } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

/** One photo, one promise, one line, one form. */
export function Hero() {
  return (
    <section aria-labelledby="hero-title" className="relative flex min-h-[100svh] items-end">
      <PhotoFill photo="hero" sizes="100vw" priority position="70% center" motion="drift" />
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(90deg,rgb(11_11_10/0.9)_0%,rgb(11_11_10/0.55)_45%,rgb(11_11_10/0)_100%),linear-gradient(0deg,#0b0b0a_0%,transparent_45%)]"
      />
      <div className="gutter fade-back relative mx-auto w-full max-w-[1200px] pt-40 pb-[clamp(64px,10vw,128px)]">
        <p className="mono intro text-[11px] text-soft">{hero.eyebrow}</p>
        <h1 id="hero-title" className="display intro intro-2 mt-7 max-w-[820px] text-[clamp(52px,7.6vw,108px)] leading-[0.92] text-balance">
          {hero.title}
        </h1>
        <p className="intro intro-3 mt-8 max-w-[440px] text-lg leading-relaxed text-soft">{hero.body}</p>
        <div id="join" className="intro intro-4 mt-10 max-w-[460px] scroll-mt-24">
          <WaitlistForm />
        </div>
      </div>
    </section>
  );
}
