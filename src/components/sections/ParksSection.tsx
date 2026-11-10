import { parks } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";
import { PhotoFill } from "@/components/ui/PhotoFill";

export function ParksSection() {
  return (
    <section aria-labelledby="parks-title" className="relative min-h-[460px] overflow-hidden border-b border-line">
      <PhotoFill photo={parks.photo} sizes="100vw" />
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-[linear-gradient(90deg,rgb(11_11_10/0.92)_0%,rgb(11_11_10/0.6)_50%,rgb(11_11_10/0.15)_100%)]"
      />
      <div className="relative mx-auto flex min-h-[460px] max-w-[1320px] items-center">
        <div className="gutter max-w-[560px] py-14">
          <SectionHeading id="parks-title" number={parks.number} label={parks.label} title={parks.title} />
          <ul className="reveal mt-6 flex flex-wrap gap-2">
            {parks.names.map((name) => (
              <li key={name} className="mono border border-tick px-3 py-2.5 text-[11px]">
                {name}
              </li>
            ))}
          </ul>
          <p className="mono reveal mt-5 py-3 text-[11px] text-accent">{parks.link}</p>
        </div>
      </div>
    </section>
  );
}
