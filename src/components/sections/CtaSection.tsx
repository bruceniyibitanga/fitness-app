import { cta } from "@/content/site";
import { Curtain } from "@/components/ui/Curtain";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

/** The one form on the page. Headline and form on the left, a tall photo running off the right edge. */
export function CtaSection() {
  return (
    <section id={cta.id} aria-labelledby="cta-title" className="section-pad scroll-mt-8">
      <div className="grid items-center gap-y-16 md:grid-cols-12">
        <div className="gutter md:col-span-6 md:pr-0 lg:pl-[max(clamp(20px,4vw,48px),calc((100vw-1200px)/2+48px))]">
          <h2 id="cta-title" className="display slide text-[clamp(56px,7.6vw,120px)] leading-[0.88] text-balance">
            {cta.title}
          </h2>
          <WaitlistForm className="reveal mt-12 max-w-[460px]" />
        </div>
        <Curtain photo={cta.photo} sizes="(min-width: 768px) 45vw, 100vw" from="right" className="aspect-[4/5] md:col-span-5 md:col-start-8" />
      </div>
    </section>
  );
}
