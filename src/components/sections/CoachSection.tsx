import { coach, type CoachReply, type CoachTile } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";
import { PhotoFill } from "@/components/ui/PhotoFill";

type Props = { n: string; builtInN: string };

/**
 * Meet Levi, in two ruled sections. First a split: the thoughts people have a few weeks into
 * training, each answered by what Levi shows in the app, beside a tall photo. Then a mosaic
 * of three photos for what's built into every plan.
 */
export function CoachSection({ n, builtInN }: Props) {
  return (
    <>
      <Stories n={n} />
      <BuiltIn n={builtInN} />
    </>
  );
}

function Stories({ n }: { n: string }) {
  const titleId = `${coach.id}-title`;
  return (
    <section id={coach.id} aria-labelledby={titleId} className="scroll-mt-8 border-b border-line">
      <div className="rail grid md:grid-cols-2">
        <div className="cell">
          <NumberedTitle id={titleId} n={n} label={coach.eyebrow} title={coach.title} />
          <p className="reveal mt-5 max-w-[460px] text-[17px] leading-normal text-muted">{coach.body}</p>
          <ul className="reveal mt-11 border-t border-line">
            {coach.stories.map((story) => (
              <li
                key={story.thought}
                className="grid min-h-[88px] grid-cols-[minmax(0,1fr)_20px_minmax(0,1.25fr)] items-center gap-3 border-b border-line py-3"
              >
                <p className="text-[15px] leading-snug text-muted">“{story.thought}”</p>
                <span aria-hidden="true" className="text-tick">
                  →
                </span>
                <Reply reply={story.reply} />
              </li>
            ))}
          </ul>
        </div>
        <div className="zoom on-photo relative min-h-[560px] overflow-hidden border-t border-line md:min-h-[680px] md:border-t-0 md:border-l">
          <PhotoFill photo={coach.photo} sizes="(min-width: 768px) 50vw, 100vw" position="45% 35%" />
          <div aria-hidden="true" className="shade-up absolute inset-0" />
          <div className="absolute inset-x-0 bottom-0 p-7">
            <p className="mono text-[11px] text-muted">{coach.caption.label}</p>
            <p className="mt-2 max-w-[380px] text-[26px] leading-[1.15] font-bold [font-stretch:95%]">{coach.caption.text}</p>
          </div>
        </div>
      </div>
    </section>
  );
}

/** Levi's answer: the app's line, or a screen title with the choices it offers. */
function Reply({ reply }: { reply: CoachReply }) {
  return (
    <div>
      <p className="text-[17px] leading-snug font-semibold">{reply.title ?? reply.line}</p>
      {reply.choices ? (
        <ul className="mt-2 flex flex-wrap gap-1.5" aria-label="Choices">
          {reply.choices.map((choice, i) => (
            <li key={choice} className={`mono px-2 py-1 text-[9px] ${i === 0 ? "bg-text text-bg" : "border border-tick text-muted"}`}>
              {choice}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

/** Three photos with a line each: one tall on the left, two stacked on the right. */
function BuiltIn({ n }: { n: string }) {
  const titleId = `${coach.id}-built-in-title`;
  const [first, ...rest] = coach.builtIn.tiles;
  return (
    <section aria-labelledby={titleId} className="border-b border-line">
      <div className="rail">
        <div className="cell pb-10 md:pb-10">
          <NumberedTitle id={titleId} n={n} label={coach.builtIn.eyebrow} title={coach.builtIn.title} className="max-w-[880px]" />
        </div>
        <div className="grid gap-px bg-line md:grid-cols-[1.1fr_0.9fr] md:grid-rows-[340px_340px]">
          <Tile tile={first} className="min-h-[420px] md:row-span-2" sizes="(min-width: 768px) 55vw, 100vw" />
          {rest.map((tile) => (
            <Tile key={tile.tag} tile={tile} className="min-h-[300px]" sizes="(min-width: 768px) 45vw, 100vw" />
          ))}
        </div>
        <p className="wrap py-6 text-[13px] leading-relaxed text-faint">{coach.note}</p>
      </div>
    </section>
  );
}

function Tile({ tile, className, sizes }: { tile: CoachTile; className: string; sizes: string }) {
  return (
    <div className={`zoom on-photo relative overflow-hidden bg-surface ${className}`}>
      <PhotoFill photo={tile.photo} sizes={sizes} position="50% 35%" />
      <div aria-hidden="true" className="shade-up absolute inset-0" />
      <div className="absolute inset-x-0 bottom-0 p-7">
        <p className="mono text-[11px] text-accent">{tile.tag}</p>
        <h3 className="mt-2 text-[28px] leading-[1.05] font-[750] tracking-[-0.015em] [font-stretch:92%]">{tile.title}</h3>
        <p className="mt-1.5 text-base text-soft">{tile.body}</p>
      </div>
    </div>
  );
}
