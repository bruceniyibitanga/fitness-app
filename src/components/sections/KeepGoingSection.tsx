import { keepGoing } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

/** Three photos, three short lines. One tall on the left, two wide ones on the right, set lower. */
export function KeepGoingSection() {
  const [first, ...rest] = keepGoing.tiles;
  return (
    <section aria-labelledby="keep-title" className="gutter section-pad mx-auto max-w-[1200px]">
      <SectionTitle id="keep-title" eyebrow={keepGoing.eyebrow} title={keepGoing.title} className="max-w-[620px] md:ml-[calc(7/12*100%)]" />
      <div className="mt-[clamp(56px,8vw,104px)] grid gap-x-6 gap-y-14 md:grid-cols-12">
        <Tile tile={first} frame="aspect-[4/5]" className="md:col-span-6" />
        <div className="grid gap-y-14 md:col-span-5 md:col-start-8 md:pt-[clamp(80px,12vw,180px)]">
          {rest.map((tile) => (
            <Tile key={tile.title} tile={tile} frame="aspect-[3/2]" />
          ))}
        </div>
      </div>
    </section>
  );
}

type TileProps = {
  tile: (typeof keepGoing.tiles)[number];
  frame: string;
  className?: string;
};

function Tile({ tile, frame, className = "" }: TileProps) {
  return (
    <div className={className}>
      <MaskedPhoto photo={tile.photo} sizes="(min-width: 768px) 50vw, 100vw" frame={frame} />
      <div className="reveal mt-5">
        <p className="mono text-[10px] text-accent">{tile.tag}</p>
        <h3 className="display mt-2 text-[clamp(26px,2.6vw,34px)] leading-tight">{tile.title}</h3>
      </div>
    </div>
  );
}
