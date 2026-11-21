import { watch } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

const HOLD_SECONDS = 15;
const HELD = 8;

/**
 * The page's one full-screen moment. The photo starts as a framed picture in the middle
 * of the screen and opens out to fill it as you scroll; the words and the watch face follow
 * underneath. Without scroll timelines (or with reduced motion) it's simply a full-width photo.
 */
export function WatchSection() {
  const titleId = `${watch.id}-title`;
  return (
    <section id={watch.id} aria-labelledby={titleId}>
      <div className="expand">
        <div className="expand-stage h-[min(100svh,960px)] md:h-[100svh]">
          <div className="expand-frame relative h-full overflow-hidden bg-surface">
            <PhotoFill photo={watch.photo} sizes="100vw" position="55% center" />
          </div>
        </div>
      </div>

      <div className="gutter section-pad mx-auto grid max-w-[1200px] items-center gap-14 md:grid-cols-12">
        <div className="md:col-span-5 md:col-start-2">
          <SectionTitle id={titleId} eyebrow={watch.eyebrow} title={watch.title} />
          <p className="reveal mt-6 max-w-[340px] text-lg leading-relaxed text-muted">{watch.body}</p>
        </div>
        <div className="reveal flex justify-center md:col-span-4 md:col-start-8">
          <WatchFace />
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
      className="flex size-[240px] flex-col items-center justify-center rounded-full border-[10px] border-[#2a2925] bg-[#050505] text-center text-[#f2efe8] shadow-[0_30px_70px_rgb(0_0_0/0.28)]"
    >
      <p className="mono text-[10px] text-[#8db4d9]">L-sit hold</p>
      <p className="mt-1 text-[60px] leading-none font-extrabold tracking-[-0.03em] [font-stretch:78%]">
        {String(HELD).padStart(2, "0")}
        <span className="text-[#7e7a71]">/{HOLD_SECONDS}</span>
      </p>
      <div className="mt-3 flex w-[120px] gap-0.5">
        {Array.from({ length: HOLD_SECONDS }, (_, i) => (
          <span key={i} className={`h-[4px] flex-1 ${i < HELD ? "bg-[#8db4d9]" : "bg-[#24231f]"}`} />
        ))}
      </div>
    </div>
  );
}
