import { path } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { MaskedPhoto } from "@/components/ui/MaskedPhoto";

type GateState = (typeof path.gates.items)[number]["state"];

const dotClass: Record<GateState, string> = {
  done: "bg-faint",
  current: "bg-accent",
  next: "border border-tick",
  goal: "border-2 border-accent",
};

/**
 * How it works, laid out like a feature spread: the title, a wide photo with the plan
 * set beside it, then the three steps in a row underneath.
 */
export function PathSection() {
  const titleId = `${path.id}-title`;
  return (
    <section id={path.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={path.eyebrow} title={path.title} className="max-w-[720px]" />

      <div className="mt-[clamp(56px,8vw,104px)] grid items-end gap-x-6 gap-y-10 md:grid-cols-12">
        <MaskedPhoto
          photo={path.photo}
          sizes="(min-width: 768px) 66vw, 100vw"
          frame="aspect-[4/3]"
          position="50% 60%"
          className="md:col-span-8"
        />
        <div className="reveal md:col-span-4">
          <GateList />
        </div>
      </div>

      <ol className="mt-[clamp(56px,7vw,96px)] grid gap-x-6 gap-y-10 border-t border-line pt-10 md:grid-cols-3">
        {path.steps.map((step, i) => (
          <li key={step.title} className="reveal">
            <span className="display text-[40px] leading-none text-accent">{i + 1}</span>
            <h3 className="mt-5 text-xl font-medium tracking-[-0.01em]">{step.title}</h3>
            <p className="mt-2 max-w-[300px] text-[15px] leading-relaxed text-muted">{step.body}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

/** The goal broken into gates, as the app shows it. */
function GateList() {
  const { gates } = path;
  return (
    <figure className="border border-line bg-surface" aria-label={`Example plan for a ${gates.goal}`}>
      <figcaption className="flex justify-between border-b border-line px-4 py-3.5">
        <span className="mono text-[10px] text-muted">Goal · {gates.goal}</span>
        <span className="mono text-[10px] text-accent">{gates.remaining}</span>
      </figcaption>
      <ul>
        {gates.items.map((gate) => {
          const tag = gate.state === "done" ? "Done" : gate.state === "current" ? gate.progress : gate.state === "goal" ? "Goal" : "";
          return (
            <li key={gate.name} className="grid min-h-[46px] grid-cols-[22px_1fr_auto] items-center gap-3 border-b border-line px-4 last:border-b-0">
              <span className={`size-2.5 ${dotClass[gate.state]}`} aria-hidden="true" />
              <span className={`text-sm ${gate.state === "done" ? "text-faint" : ""}`}>{gate.name}</span>
              <span className={`mono text-[10px] ${gate.state === "current" ? "text-accent" : "text-faint"}`}>{tag}</span>
            </li>
          );
        })}
      </ul>
    </figure>
  );
}
