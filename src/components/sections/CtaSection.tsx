import { cta } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";
import { WaitlistForm } from "@/components/waitlist/WaitlistForm";

export function CtaSection() {
  return (
    <section aria-labelledby="cta-title" className="relative overflow-hidden">
      <PhotoFill photo={cta.photo} sizes="100vw" />
      <div className="absolute inset-0 bg-bg/65" aria-hidden="true" />
      <div className="gutter relative mx-auto flex min-h-[720px] max-w-[1200px] flex-col items-center justify-center py-32 text-center">
        <h2 id="cta-title" className="display reveal text-[clamp(52px,8vw,120px)] leading-[0.9] text-balance">
          {cta.title}
        </h2>
        <WaitlistForm className="reveal mt-12 w-full max-w-[460px] text-left" />
      </div>
    </section>
  );
}
