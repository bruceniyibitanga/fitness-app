import Link from "next/link";
import { privacy, watch } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";

/** Two short promises side by side: the watch, and where the data lives. */
export function WatchPrivacySection() {
  return (
    <section aria-label="Watch and privacy" className="gutter section-pad mx-auto grid max-w-[1200px] gap-3 md:grid-cols-2">
      <article id={watch.id} aria-labelledby="watch-title" className="reveal relative flex min-h-[560px] flex-col justify-end overflow-hidden p-[clamp(28px,4vw,48px)]">
        <PhotoFill photo={watch.photo} sizes="(min-width: 768px) 50vw, 100vw" />
        <div className="shade-bottom absolute inset-0" aria-hidden="true" />
        <div className="relative">
          <p className="mono text-[11px] text-accent">{watch.eyebrow}</p>
          <h2 id="watch-title" className="mt-5 text-[clamp(32px,3.6vw,48px)] leading-[1] font-semibold tracking-[-0.025em] text-balance">
            {watch.title}
          </h2>
          <p className="mt-4 text-base text-soft">{watch.body}</p>
        </div>
      </article>
      <article aria-labelledby="privacy-title" className="reveal flex min-h-[560px] flex-col justify-end bg-surface p-[clamp(28px,4vw,48px)]">
        <LockMark />
        <p className="mono mt-auto text-[11px] text-accent">{privacy.eyebrow}</p>
        <h2 id="privacy-title" className="mt-5 text-[clamp(32px,3.6vw,48px)] leading-[1] font-semibold tracking-[-0.025em] text-balance">
          {privacy.title}
        </h2>
        <p className="mt-4 text-base text-soft">{privacy.body}</p>
        <Link href={privacy.link.href} className="mono mt-8 inline-flex min-h-11 items-center self-start text-[11px] text-muted underline-offset-4 hover:text-text hover:underline">
          {privacy.link.label} <span aria-hidden="true" className="ml-2">→</span>
        </Link>
      </article>
    </section>
  );
}

/** A plain padlock, drawn with the same square strokes as the logo. */
function LockMark() {
  return (
    <svg width="44" height="44" viewBox="0 0 44 44" aria-hidden="true" className="text-tick">
      <path d="M13 20v-6a9 9 0 0 1 18 0v6" fill="none" stroke="currentColor" strokeWidth="3" />
      <rect x="9" y="20" width="26" height="19" fill="none" stroke="currentColor" strokeWidth="3" />
    </svg>
  );
}
