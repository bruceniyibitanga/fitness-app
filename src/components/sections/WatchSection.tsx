import { watch } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { Curtain } from "@/components/ui/Curtain";

const HOLD_SECONDS = 15;
const HELD = 8;

/** The full-width mountain photo with a round Wear OS face and two short lines over it. */
export function WatchSection() {
  const titleId = `${watch.id}-title`;
  return (
    <section id={watch.id} aria-labelledby={titleId} className="relative">
      <Curtain photo={watch.photo} sizes="100vw" position="30% center" className="h-[72svh] min-h-[480px] md:h-[clamp(640px,92vh,900px)]">
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-[linear-gradient(0deg,#0b0b0a_0%,rgb(11_11_10/0)_35%)] md:bg-[linear-gradient(90deg,rgb(11_11_10/0)_35%,rgb(11_11_10/0.8)_100%)]"
        />
      </Curtain>
      {/* On phones this sits under the photo, the watch overlapping its edge; on wider screens it floats over the right side. */}
      <div className="gutter relative mx-auto -mt-[116px] flex max-w-[1200px] flex-col md:absolute md:inset-0 md:mt-0 md:items-end md:justify-center">
        <div className="w-full max-w-[340px]">
          <div className="slide slide-right mb-12 flex justify-end md:justify-start">
            <WatchFace />
          </div>
          <SectionTitle id={titleId} eyebrow={watch.eyebrow} title={watch.title} />
          <p className="reveal mt-5 text-lg leading-relaxed text-soft">{watch.body}</p>
        </div>
      </div>
    </section>
  );
}

/** A round watch face mid-hold. Drawn in markup so it stays sharp and costs nothing to load. */
function WatchFace() {
  return (
    <div
      role="img"
      aria-label={`Wear OS watch showing an L-sit hold at ${HELD} of ${HOLD_SECONDS} seconds`}
      className="flex size-[232px] flex-col items-center justify-center rounded-full border-[10px] border-[#1d1c1a] bg-black text-center shadow-[0_30px_80px_rgb(0_0_0/0.6)]"
    >
      <p className="mono text-[10px] text-ice">L-sit hold</p>
      <p className="display mt-1 text-[60px] leading-none [font-stretch:78%]">
        {String(HELD).padStart(2, "0")}
        <span className="text-faint">/{HOLD_SECONDS}</span>
      </p>
      <div className="mt-3 flex w-[120px] gap-0.5">
        {Array.from({ length: HOLD_SECONDS }, (_, i) => (
          <span key={i} className={`h-[4px] flex-1 ${i < HELD ? "bg-ice" : "bg-line"}`} />
        ))}
      </div>
    </div>
  );
}
