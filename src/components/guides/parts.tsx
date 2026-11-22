import Link from "next/link";
import type { Guide } from "@/content/guides";
import { PhotoFill } from "@/components/ui/PhotoFill";

/** Home / Guides / Skill. Plain links; the structured data carries the same trail. */
export function Breadcrumbs({ skill }: { skill?: string }) {
  return (
    <nav aria-label="Breadcrumb" className="mono text-[10px] text-muted">
      <ol className="flex flex-wrap items-center gap-x-2.5">
        <li>
          <Link href="/" className="inline-flex min-h-11 items-center hover:text-text">
            Home
          </Link>
        </li>
        <li aria-hidden="true">/</li>
        <li>
          {skill ? (
            <Link href="/guides" className="inline-flex min-h-11 items-center hover:text-text">
              Guides
            </Link>
          ) : (
            <span aria-current="page">Guides</span>
          )}
        </li>
        {skill ? (
          <>
            <li aria-hidden="true">/</li>
            <li aria-current="page" className="text-text">
              {skill}
            </li>
          </>
        ) : null}
      </ol>
    </nav>
  );
}

/** "How to do a" on one line, the skill in accent on the next, like the home page hero. */
export function GuideTitle({ guide, className = "" }: { guide: Guide; className?: string }) {
  const lead = "How to do a ";
  const rest = guide.h1.startsWith(lead) ? guide.h1.slice(lead.length) : null;
  return (
    <h1 className={`figure ${className}`}>
      {rest ? (
        <>
          {lead.trim()}
          <br />
          <span className="text-accent">{rest}</span>
        </>
      ) : (
        guide.h1
      )}
    </h1>
  );
}

/**
 * Where a stage sits in the progression: one square per stage, filled up to this one.
 * Decorative, because the stage number beside it already says it in words.
 */
export function StageMeter({ at, of }: { at: number; of: number }) {
  return (
    <span aria-hidden="true" className="flex gap-1">
      {Array.from({ length: of }, (_, i) => (
        <span key={i} className={`size-2 ${i <= at ? "bg-accent" : "border border-tick"}`} />
      ))}
    </span>
  );
}

type GuideCardProps = {
  guide: Guide;
  /** The guide's number in a list, like "01". */
  n?: string;
  /** Tells the browser how wide the photo shows, so it downloads the right size. */
  sizes: string;
  /** The skill name's heading level, so it sits right in the page's outline. */
  heading?: "h2" | "h3";
  className?: string;
};

/**
 * A guide as a photo card that opens it. The card fills its grid cell, so the grid around
 * it draws the rules. Used by the hub and by "More guides".
 */
export function GuideCard({ guide, n, sizes, heading = "h2", className = "" }: GuideCardProps) {
  const Heading = heading;
  return (
    <Link href={`/guides/${guide.slug}`} className={`group on-photo zoom relative block overflow-hidden bg-surface ${className}`}>
      <div className="absolute inset-0 transition-transform duration-[900ms] ease-[cubic-bezier(.2,.7,.1,1)] group-hover:scale-[1.04]">
        <PhotoFill photo={guide.photo} sizes={sizes} position={guide.photoPosition} />
      </div>
      <div aria-hidden="true" className="shade-up absolute inset-0" />
      <div className="absolute inset-x-0 bottom-0 p-6">
        <p className="mono text-[10px] text-muted">
          {n ? <b className="font-medium text-accent">{n} · </b> : null}
          {guide.stages.length} stages · Free plan
        </p>
        <Heading className="mt-2 text-[32px] leading-none font-[750] tracking-[-0.02em] [font-stretch:90%]">{guide.skill}</Heading>
        <p className="mono mt-3.5 text-[11px] text-accent">
          Read the guide{" "}
          <span aria-hidden="true" className="inline-block transition-transform duration-300 group-hover:translate-x-1.5">
            →
          </span>
        </p>
      </div>
    </Link>
  );
}
