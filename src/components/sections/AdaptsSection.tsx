import { adapts } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { Curtain } from "@/components/ui/Curtain";

/**
 * One example of the coach changing a session, shown rather than explained. The photo runs
 * off the left edge of the screen and the session card overlaps its right side.
 */
export function AdaptsSection() {
  const titleId = `${adapts.id}-title`;
  return (
    <section id={adapts.id} aria-labelledby={titleId} className="section-pad">
      <div className="grid items-end gap-y-16 md:grid-cols-12">
        <div className="relative md:col-span-7">
          <Curtain photo={adapts.photo} sizes="(min-width: 768px) 60vw, 100vw" from="left" className="aspect-[4/5] md:aspect-[6/5]">
            <div className="absolute inset-0 bg-bg/25" aria-hidden="true" />
          </Curtain>
          <div className="lift gutter absolute -bottom-10 right-0 w-full sm:w-auto md:-right-[clamp(40px,8vw,120px)] md:px-0">
            <AdjustedCard />
          </div>
        </div>
        <div className="gutter md:col-span-4 md:col-start-9 md:pb-6 md:pl-0">
          <SectionTitle id={titleId} eyebrow={adapts.eyebrow} title={adapts.title} />
          <p className="reveal mt-8 max-w-[340px] text-lg leading-relaxed text-muted">{adapts.body}</p>
        </div>
      </div>
    </section>
  );
}

function AdjustedCard() {
  const { card } = adapts;
  return (
    <figure className="ml-auto border border-tick bg-bg/90 p-6 backdrop-blur-md sm:w-[300px]">
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
