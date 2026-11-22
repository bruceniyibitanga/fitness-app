import { coach, type CoachReply, type CoachStory } from "@/content/site";
import { PhotoFill } from "@/components/ui/PhotoFill";

/**
 * Meet Levi. Pictures first, very few words. A full-width photo carries the promise; then a
 * staggered grid of photo tiles, each with a thought a person really has a few weeks into
 * training and, a beat later as you scroll, Levi's answer from the app.
 *
 * Text on photos sits on a dark fade and stays light in both themes, so it always reads.
 * Server component with no state: everything comes from `coach` in site.ts.
 */
export function CoachSection() {
  const titleId = `${coach.id}-title`;
  const storiesId = `${coach.id}-stories`;
  return (
    <section id={coach.id} aria-labelledby={titleId} className="section-pad">
      <Opener titleId={titleId} />

      <div className="gutter mx-auto mt-[clamp(96px,13vw,184px)] max-w-[1200px]">
        <h3 id={storiesId} className="h2 reveal max-w-[640px] text-balance">
          {coach.storiesTitle}
        </h3>
        <ul aria-labelledby={storiesId} className="mt-[clamp(48px,7vw,96px)] grid gap-6 md:grid-cols-12 md:gap-y-[clamp(48px,6vw,88px)]">
          {coach.stories.map((story, index) => (
            <StoryTile key={story.when} story={story} layout={LAYOUT[index % LAYOUT.length]} />
          ))}
        </ul>
        <BuiltIn />
      </div>
    </section>
  );
}

/**
 * How the tiles sit on the 12-column grid: big and small, alternating sides, with the small
 * one dropped lower so the grid reads as a magazine spread, not a table. Six entries for six
 * stories; extra stories reuse the pattern.
 */
const LAYOUT = [
  { span: "md:col-span-7", frame: "aspect-[4/5] md:aspect-[5/4]" },
  { span: "md:col-span-5 md:mt-[22%]", frame: "aspect-[4/5]" },
  { span: "md:col-span-5", frame: "aspect-[4/5]" },
  { span: "md:col-span-7 md:mt-[16%]", frame: "aspect-[4/5] md:aspect-[5/4]" },
  { span: "md:col-span-7", frame: "aspect-[4/5] md:aspect-[5/4]" },
  { span: "md:col-span-5 md:mt-[22%]", frame: "aspect-[4/5]" },
] as const;

type Layout = (typeof LAYOUT)[number];

/** The full-width photo with the promise set over its lower left corner. */
function Opener({ titleId }: { titleId: string }) {
  return (
    <div className="mask relative h-[82svh] min-h-[520px] overflow-hidden">
      <PhotoFill photo={coach.photo} sizes="100vw" position="45% 35%" />
      <div aria-hidden="true" className="absolute inset-0 bg-gradient-to-t from-black/75 via-black/20 to-transparent" />
      <div className="inset-left absolute inset-x-0 bottom-0 pb-[clamp(40px,6vw,88px)] pr-[clamp(20px,4vw,48px)] text-white">
        <p className="mono reveal text-[11px] text-white/75">{coach.eyebrow}</p>
        <h2 id={titleId} className="h2 reveal mt-5">
          {coach.title.map((line) => (
            <span key={line} className="block">
              {line}
            </span>
          ))}
        </h2>
        <p className="reveal mt-6 max-w-[380px] text-lg leading-snug text-white/85">{coach.body}</p>
      </div>
    </div>
  );
}

type TileProps = { story: CoachStory; layout: Layout };

/**
 * One story: the photo, the thought over it, and Levi's reply landing a beat later. The words
 * sit at the bottom on a fade, or at the top when the person in the photo is low in the frame.
 */
function StoryTile({ story, layout }: TileProps) {
  const top = story.textAt === "top";
  return (
    <li className={layout.span}>
      <figure className={`mask relative overflow-hidden ${layout.frame}`}>
        <PhotoFill photo={story.photo} sizes="(min-width: 768px) 58vw, 100vw" position={story.position} />
        <div
          aria-hidden="true"
          className={`absolute inset-0 ${top ? "bg-gradient-to-b from-black/80 via-black/25 to-transparent" : "bg-gradient-to-t from-black/80 via-black/25 to-black/10"}`}
        />
        <figcaption className={`absolute inset-x-0 flex flex-col p-5 text-white md:p-7 ${top ? "top-0" : "bottom-0"}`}>
          <p className="mono text-[10px] text-white/75">{story.when}</p>
          <p className={`display reveal text-balance text-[clamp(28px,3vw,42px)] leading-[1.05] ${top ? "mt-4" : "mt-3"}`}>
            <span aria-hidden="true">“</span>
            {story.thought}
            <span aria-hidden="true">”</span>
          </p>
          <Reply reply={story.reply} />
        </figcaption>
      </figure>
    </li>
  );
}

/**
 * Levi's answer, styled like a message on frosted glass. It's a picture of the app, so the
 * choices are plain text, not buttons.
 */
function Reply({ reply }: { reply: CoachReply }) {
  return (
    <div className="reveal-late mt-5 max-w-[380px] border border-white/15 bg-black/40 p-4 backdrop-blur-md">
      <p className="mono text-[9px] text-white/60">Levi</p>
      {reply.title ? <p className="mt-2 text-[15px] text-white">{reply.title}</p> : null}
      {reply.line ? <p className="mt-2 text-[15px] leading-snug text-white">{reply.line}</p> : null}
      {reply.choices ? (
        <ul className="mt-3 flex flex-wrap gap-1.5">
          {reply.choices.map((choice, index) => (
            <li
              key={choice}
              className={`mono px-2.5 py-1.5 text-[9px] ${index === 0 ? "bg-white text-black" : "border border-white/30 text-white/85"}`}
            >
              {choice}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

/** The three things built into every plan: one line each, under a hairline. */
function BuiltIn() {
  const titleId = `${coach.id}-built-in`;
  return (
    <div role="group" aria-labelledby={titleId} className="mt-[clamp(96px,12vw,168px)] border-t border-text pt-10">
      <div className="grid gap-12 md:grid-cols-12 md:gap-6">
        <h3 id={titleId} className="display reveal text-balance text-[clamp(30px,3.4vw,46px)] leading-[1.05] md:col-span-5">
          {coach.builtIn.title}
        </h3>
        <ul className="grid gap-8 sm:grid-cols-3 md:col-span-7 md:gap-6">
          {coach.builtIn.items.map((item) => (
            <li key={item.title} className="reveal">
              <p className="text-text">{item.title}</p>
              <p className="mt-2 text-sm leading-relaxed text-muted">{item.body}</p>
            </li>
          ))}
        </ul>
      </div>
      <p className="mt-14 max-w-[520px] text-xs leading-relaxed text-faint">{coach.note}</p>
    </div>
  );
}
