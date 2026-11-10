import { cta } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

export function CtaSection() {
  return (
    <section aria-labelledby="cta-title" className="relative min-h-[620px] overflow-hidden">
      <PhotoFill photo={cta.photo} sizes="100vw" />
      <div className="absolute inset-0 bg-bg/60" aria-hidden="true" />
      <div className="gutter relative mx-auto flex min-h-[620px] max-w-[1320px] flex-col items-center justify-center text-center">
        <h2 id="cta-title" className="display reveal max-w-[900px] text-[clamp(48px,7vw,104px)] leading-[0.9]">
          {cta.title}
        </h2>
        <WaitlistForm className="reveal mt-9 w-full max-w-[500px] text-left" />
      </div>
    </section>
  );
}
