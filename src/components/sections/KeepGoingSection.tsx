import { keepGoing } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { Curtain } from "@/components/ui/Curtain";

/** Three photos, three short lines. One tall on the left, two stacked and set lower on the right. */
export function KeepGoingSection() {
  const [first, ...rest] = keepGoing.tiles;
  return (
    <section aria-labelledby="keep-title" className="gutter section-pad mx-auto max-w-[1200px]">
      <SectionTitle id="keep-title" eyebrow={keepGoing.eyebrow} title={keepGoing.title} className="max-w-[640px] md:ml-[42%]" />
      <ul className="mt-[clamp(56px,8vw,112px)] grid gap-x-6 gap-y-14 md:grid-cols-12">
        <Tile tile={first} from="left" className="md:col-span-7 md:row-span-2" frame="aspect-[4/5]" />
        {rest.map((tile, i) => (
          <Tile key={tile.title} tile={tile} from="right" className={`md:col-span-5 ${i === 0 ? "md:mt-[clamp(80px,12vw,180px)]" : ""}`} frame="aspect-[4/3]" />
        ))}
      </ul>
    </section>
  );
}

type TileProps = {
  tile: (typeof keepGoing.tiles)[number];
  from: "left" | "right";
  className: string;
  frame: string;
};

function Tile({ tile, from, className, frame }: TileProps) {
  return (
    <li className={className}>
      <Curtain photo={tile.photo} sizes="(min-width: 768px) 50vw, 100vw" from={from} className={frame} />
      <div className="reveal mt-5">
        <p className="mono text-[10px] text-accent">{tile.tag}</p>
        <h3 className="mt-2 text-[clamp(22px,2.2vw,28px)] leading-tight font-semibold tracking-[-0.02em]">{tile.title}</h3>
      </div>
    </li>
  );
}
