import { keepGoing } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PhotoFill } from "@/components/ui/PhotoFill";

export function KeepGoingSection() {
  return (
    <section aria-labelledby="keep-title" className="border-b border-line">
      <div className="rail">
        <div className="gutter pt-[clamp(48px,6vw,88px)] pb-10">
          <SectionHeading id="keep-title" number={keepGoing.number} label={keepGoing.label} title={keepGoing.title} />
        </div>
        <ul className="grid gap-px bg-line md:grid-cols-[1.1fr_0.9fr] md:grid-rows-[340px_340px]">
          {keepGoing.tiles.map((tile, i) => (
            <li key={tile.title} className={`relative min-h-[300px] overflow-hidden ${i === 0 ? "min-h-[420px] md:row-span-2" : ""}`}>
              <PhotoFill photo={tile.photo} sizes={i === 0 ? "(min-width: 768px) 55vw, 100vw" : "(min-width: 768px) 45vw, 100vw"} />
              <div className="shade-bottom absolute inset-0" aria-hidden="true" />
              <div className="absolute inset-x-0 bottom-0 p-7">
                <p className={`mono text-[11px] ${tile.tone === "care" ? "text-ice" : "text-accent"}`}>{tile.tag}</p>
                <h3 className="mt-2 text-[28px] leading-[1.05] font-[750] tracking-[-0.015em] [font-stretch:92%]">{tile.title}</h3>
                <p className="mt-1.5 text-base text-soft">{tile.body}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}
