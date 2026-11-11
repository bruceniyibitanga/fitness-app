import { watch } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PhotoFill } from "@/components/ui/PhotoFill";

const HOLD_SECONDS = 15;
const HELD = 8;

export function WatchSection() {
  const titleId = `${watch.id}-title`;
  return (
    <section id={watch.id} aria-labelledby={titleId} className="relative min-h-[760px] overflow-hidden border-b border-line">
      <PhotoFill photo={watch.photo} sizes="100vw" />
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(90deg,rgb(11_11_10/0.1)_0%,rgb(11_11_10/0.55)_55%,rgb(11_11_10/0.92)_100%)]"
      />
      <div className="relative mx-auto flex min-h-[760px] max-w-[1320px] items-center justify-end">
        <div className="gutter flex flex-wrap items-center gap-12 py-16">
          <WatchFace />
          <div className="max-w-[420px]">
            <SectionHeading id={titleId} number={watch.number} label={watch.label} title={watch.title} />
            <p className="reveal mt-4 text-lg leading-normal text-soft">{watch.body}</p>
          </div>
        </div>
      </div>
    </section>
  );
}

function WatchFace() {
  return (
    <div
      className="reveal flex size-[240px] flex-col items-center justify-center rounded-full border-2 border-tick bg-black p-[34px] text-center shadow-[0_30px_80px_rgb(0_0_0/0.6)]"
      role="img"
      aria-label={`Wear OS watch showing an L-sit hold at ${HELD} of ${HOLD_SECONDS} seconds`}
    >
      <p className="mono text-[10px] text-ice">L-sit hold</p>
      <p className="display mt-1.5 text-[64px] leading-none [font-stretch:78%]">
        {String(HELD).padStart(2, "0")}
        <span className="text-faint">/{HOLD_SECONDS}</span>
      </p>
      <div className="mt-3 flex w-full gap-0.5">
        {Array.from({ length: HOLD_SECONDS }, (_, i) => (
          <span key={i} className={`h-[5px] flex-1 ${i < HELD ? "bg-ice" : "bg-line"}`} />
        ))}
      </div>
      <p className="mono mt-3 text-[9px] text-accent">Buzz at best</p>
    </div>
  );
}
