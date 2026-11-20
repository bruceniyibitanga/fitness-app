import Link from "next/link";
import { privacy } from "@/content/site";

/** One large statement and a link. No box, no photo: the white space is the point. */
export function PrivacySection() {
  return (
    <section aria-labelledby="privacy-title" className="gutter section-pad mx-auto max-w-[1200px]">
      <div className="md:ml-[25%]">
        <p className="mono reveal text-[11px] text-accent">{privacy.eyebrow}</p>
        <h2 id="privacy-title" className="display reveal mt-6 max-w-[860px] text-[clamp(48px,6.6vw,100px)] leading-[0.95] text-balance">
          {privacy.title}
        </h2>
        <div className="reveal mt-10 flex flex-wrap items-center gap-x-10 gap-y-4">
          <p className="text-lg text-soft">{privacy.body}</p>
          <Link href={privacy.link.href} className="mono inline-flex min-h-11 items-center text-[11px] text-muted underline-offset-4 hover:text-text hover:underline">
            {privacy.link.label} <span aria-hidden="true" className="ml-2">→</span>
          </Link>
        </div>
      </div>
    </section>
  );
}
