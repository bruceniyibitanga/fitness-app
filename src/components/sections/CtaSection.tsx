import { cta } from "@/content/site";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

/** The one form on the page. Headline and form on the left, a photo running off the right edge. */
export function CtaSection() {
  return (
    <section id={cta.id} aria-labelledby="cta-title" className="section-pad scroll-mt-8">
      <div className="grid items-center gap-y-16 md:grid-cols-[minmax(0,1fr)_minmax(0,46vw)] md:gap-x-[clamp(40px,6vw,96px)]">
        <div className="inset-left pr-[clamp(20px,4vw,48px)] md:pr-0">
          <h2 id="cta-title" className="display reveal text-[clamp(56px,7vw,112px)] leading-[0.92] text-balance">
            {cta.title}
          </h2>
          <WaitlistForm className="reveal mt-12 max-w-[460px]" />
        </div>
        <MaskedPhoto photo={cta.photo} sizes="(min-width: 768px) 46vw, 100vw" frame="aspect-[4/5]" position="30% center" />
      </div>
    </section>
  );
}
