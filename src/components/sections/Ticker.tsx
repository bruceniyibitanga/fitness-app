import { ticker } from "@/content/site";

/**
 * A strip of moves drifting sideways under the hero. The list is drawn twice so the loop
 * joins up without a gap. Decorative, so screen readers skip it; reduced motion holds it still.
 */
export function Ticker() {
  const items = [...ticker, ...ticker];
  return (
    <div aria-hidden="true" className="ticker overflow-hidden border-b border-line whitespace-nowrap">
      <div className="ticker-track inline-flex">
        {items.map((move, i) => (
          <span key={`${move}-${i}`} className="mono border-r border-line px-7 py-5 text-[12px] tracking-[0.18em] text-muted">
            {move}
          </span>
        ))}
      </div>
    </div>
  );
}
