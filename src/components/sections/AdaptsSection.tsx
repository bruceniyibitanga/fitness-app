import { adapts } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

/** One example of the coach changing a session, shown rather than explained. */
export function AdaptsSection() {
  const titleId = `${adapts.id}-title`;
  return (
    <section id={adapts.id} aria-labelledby={titleId} className="gutter section-pad mx-auto grid max-w-[1200px] items-center gap-[clamp(48px,8vw,120px)] md:grid-cols-2">
      <div>
        <SectionTitle id={titleId} eyebrow={adapts.eyebrow} title={adapts.title} />
        <p className="reveal mt-8 max-w-[420px] text-lg leading-relaxed text-muted">{adapts.body}</p>
      </div>
      <div className="reveal relative aspect-[4/5] overflow-hidden">
        <PhotoFill photo={adapts.photo} sizes="(min-width: 768px) 50vw, 100vw" />
        <div className="absolute inset-0 bg-bg/35" aria-hidden="true" />
        <AdjustedCard />
      </div>
    </section>
  );
}

function AdjustedCard() {
  const { card } = adapts;
  return (
    <figure className="absolute inset-x-6 bottom-6 border border-tick bg-bg/85 p-6 backdrop-blur-md sm:inset-x-auto sm:right-6 sm:w-[300px]">
      <figcaption className="flex justify-between">
        <span className="mono text-[10px] text-muted">{card.title}</span>
        <span className="mono text-[10px] text-accent">{card.badge}</span>
      </figcaption>
      <p className="mt-5 flex items-baseline gap-3">
        <s className="text-[26px] font-bold text-faint">{card.from}</s>
        <span className="display text-[64px] leading-[0.9]">{card.to}</span>
      </p>
      <p className="mt-5 text-sm text-soft">{card.reason}</p>
      <p className="mono mt-2 text-[10px] text-ice">{card.note}</p>
    </figure>
  );
}
