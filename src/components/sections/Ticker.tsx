import { tickerSkills } from "@/content/site";

export function Ticker() {
  // Rendered twice so the strip can loop by sliding exactly half its width.
  const items = [...tickerSkills, ...tickerSkills];
  return (
    <div className="ticker overflow-hidden border-b border-line whitespace-nowrap" aria-hidden="true">
      <div className="ticker-track inline-flex">
        {items.map((skill, i) => (
          <span key={`${skill}-${i}`} className="mono border-r border-line px-7 py-5 text-xs tracking-[0.18em] text-muted">
            {skill}
          </span>
        ))}
      </div>
    </div>
  );
}
