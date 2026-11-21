import { adapts } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/**
 * One example of the coach changing a session, shown rather than explained. Words and the
 * session card on the left; a wide photo running off the right edge of the screen.
 */
export function AdaptsSection() {
  const titleId = `${adapts.id}-title`;
  return (
    <section id={adapts.id} aria-labelledby={titleId} className="section-pad">
      <div className="grid items-center gap-y-14 md:grid-cols-[minmax(0,5fr)_minmax(0,7fr)] md:gap-x-[clamp(40px,6vw,96px)]">
        <div className="inset-left pr-[clamp(20px,4vw,48px)] md:pr-0">
          <SectionTitle id={titleId} eyebrow={adapts.eyebrow} title={adapts.title} />
          <p className="reveal mt-7 max-w-[360px] text-lg leading-relaxed text-muted">{adapts.body}</p>
          <div className="reveal mt-12 max-w-[320px]">
            <AdjustedCard />
          </div>
        </div>
        <MaskedPhoto photo={adapts.photo} sizes="(min-width: 768px) 58vw, 100vw" frame="aspect-[4/3] md:aspect-[6/5]" position="40% center" />
      </div>
    </section>
  );
}

function AdjustedCard() {
  const { card } = adapts;
  return (
    <figure className="border border-line bg-surface p-6">
      <figcaption className="flex justify-between">
        <span className="mono text-[10px] text-muted">{card.title}</span>
        <span className="mono text-[10px] text-accent">{card.badge}</span>
      </figcaption>
      <p className="mt-5 flex items-baseline gap-3">
        <s className="text-[24px] text-faint">{card.from}</s>
        <span className="display text-[64px] leading-[0.9]">{card.to}</span>
      </p>
      <p className="mt-5 text-sm text-soft">{card.reason}</p>
      <p className="mono mt-2 text-[10px] text-ice">{card.note}</p>
    </figure>
  );
}
