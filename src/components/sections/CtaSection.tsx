import { cta } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

/** The one form on the page: a full-width photo band, the line and the form centred over it. */
export function CtaSection() {
  return (
    <section id={cta.id} aria-labelledby="cta-title" className="zoom on-photo relative min-h-[620px] scroll-mt-8 overflow-hidden bg-surface">
      <PhotoFill photo={cta.photo} sizes="100vw" position="30% center" />
      <div aria-hidden="true" className="absolute inset-0 bg-[rgb(11_11_10/0.62)]" />
      <div className="wrap relative flex min-h-[620px] flex-col items-center justify-center py-20 text-center">
        <h2 id="cta-title" className="figure reveal max-w-[900px] text-[clamp(48px,7vw,104px)] leading-[0.9] text-balance">
          {cta.title}
        </h2>
        <WaitlistForm className="reveal mt-9 w-full max-w-[500px] text-left" />
      </div>
    </section>
  );
}
