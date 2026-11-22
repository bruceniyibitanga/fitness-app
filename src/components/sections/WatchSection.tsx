import { watch } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

const HOLD_SECONDS = 15;
const HELD = 8;

/** A full-width photo band: the watch face and the words sit on the right, over a dark fade. */
export function WatchSection({ n }: { n: string }) {
  const titleId = `${watch.id}-title`;
  return (
    <section id={watch.id} aria-labelledby={titleId} className="zoom on-photo relative min-h-[760px] overflow-hidden border-b border-line bg-surface">
      <PhotoFill photo={watch.photo} sizes="100vw" position="35% center" />
      <div aria-hidden="true" className="shade-left absolute inset-0" />
      <div className="relative mx-auto flex min-h-[760px] max-w-[1320px] items-end justify-end md:items-center">
        <div className="flex flex-wrap items-center gap-12 px-[clamp(20px,4vw,48px)] py-16">
          <WatchFace />
          <div className="max-w-[420px]">
            <NumberedTitle id={titleId} n={n} label={watch.eyebrow} title={watch.title} />
            <p className="reveal mt-4 text-lg leading-normal text-soft">{watch.body}</p>
          </div>
        </div>
      </div>
    </section>
  );
}

/** A round Wear OS face mid-hold. It's a device, so it stays dark in both themes. */
function WatchFace() {
  return (
    <div
      role="img"
      aria-label={`Wear OS watch showing an L-sit hold at ${HELD} of ${HOLD_SECONDS} seconds`}
      className="reveal flex size-[220px] flex-col items-center justify-center rounded-full border-[10px] border-[#2a2925] bg-[#050505] text-center text-[#f2efe8] shadow-[0_30px_80px_rgb(0_0_0/0.6)]"
    >
      <p className="mono text-[10px] text-[#8db4d9]">L-sit hold</p>
      <p className="figure mt-1 text-[56px] leading-none">
        {String(HELD).padStart(2, "0")}
        <span className="text-[#7e7a71]">/{HOLD_SECONDS}</span>
      </p>
      <div className="mt-3 flex w-[110px] gap-0.5">
        {Array.from({ length: HOLD_SECONDS }, (_, i) => (
          <span key={i} className={`h-[4px] flex-1 ${i < HELD ? "bg-[#8db4d9]" : "bg-[#24231f]"}`} />
        ))}
      </div>
    </div>
  );
}
