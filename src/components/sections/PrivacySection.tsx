import Link from "next/link";
import { privacy } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";

/** The promise on the left; on the right, four short facts in a ruled grid and the detail. */
export function PrivacySection({ n }: { n: string }) {
  return (
    <section aria-labelledby="privacy-title" className="border-b border-line">
      <div className="rail grid md:grid-cols-2">
        <div className="cell border-b border-line md:border-r md:border-b-0">
          <NumberedTitle id="privacy-title" n={n} label={privacy.eyebrow} title={privacy.title} />
        </div>
        <div className="cell flex flex-col justify-end">
          <ul className="reveal grid grid-cols-2 gap-px border border-line bg-line">
            {privacy.facts.map((fact) => (
              <li key={fact} className="figure bg-bg px-5 py-6 text-[clamp(24px,2.4vw,32px)] leading-none">
                {fact}
              </li>
            ))}
          </ul>
          <p className="reveal mt-6 max-w-[460px] text-[17px] leading-normal text-muted">{privacy.body}</p>
          <Link href={privacy.link.href} className="mono reveal mt-3 inline-flex min-h-11 items-center self-start text-[11px] text-accent hover:text-accent-hover">
            {privacy.link.label} <span aria-hidden="true" className="ml-2">→</span>
          </Link>
        </div>
      </div>
    </section>
  );
}
