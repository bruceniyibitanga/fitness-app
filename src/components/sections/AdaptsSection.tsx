import { adapts } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PhotoFill } from "@/components/ui/PhotoFill";

export function AdaptsSection() {
  const titleId = `${adapts.id}-title`;
  return (
    <section id={adapts.id} aria-labelledby={titleId} className="border-b border-line">
      <div className="rail grid md:grid-cols-2">
        <div className="gutter section-pad">
          <SectionHeading id={titleId} number={adapts.number} label={adapts.label} title={adapts.title} />
          <ul className="reveal mt-11 border-t border-line">
            {adapts.rules.map((rule) => (
              <li key={rule.when} className="grid min-h-[88px] grid-cols-[minmax(0,1fr)_28px_minmax(0,1.2fr)] items-center gap-3 border-b border-line py-3">
                <span className={`mono text-[11px] leading-normal ${rule.tone === "care" ? "text-ice" : "text-muted"}`}>{rule.when}</span>
                <span className="text-lg text-tick" aria-hidden="true">→</span>
                <span className="text-[19px] leading-snug font-semibold">{rule.then}</span>
              </li>
            ))}
          </ul>
        </div>
        <div className="relative min-h-[520px] border-line md:min-h-[680px] md:border-l">
          <PhotoFill photo={adapts.photo} sizes="(min-width: 768px) 50vw, 100vw" />
          <div className="shade-bottom absolute inset-0" aria-hidden="true" />
          <div className="absolute inset-x-0 bottom-0 p-7">
            <p className="mono text-[11px] text-muted">{adapts.caption.label}</p>
            <p className="mt-2 max-w-[380px] text-[26px] leading-tight font-bold [font-stretch:95%]">{adapts.caption.text}</p>
          </div>
        </div>
      </div>
    </section>
  );
}
