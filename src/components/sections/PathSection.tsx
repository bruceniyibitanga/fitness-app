import { path } from "@/content/site";
import { NumberedTitle } from "@/components/ui/NumberedTitle";

type GateState = (typeof path.gates.items)[number]["state"];

const dotClass: Record<GateState, string> = {
  done: "bg-faint",
  current: "bg-accent",
  next: "border border-tick",
  goal: "border-2 border-accent",
};

/**
 * How it works. The title and an example plan stay pinned on the left while the steps scroll
 * past on the right, each one lighting up as it crosses the middle of the screen.
 */
export function PathSection({ n }: { n: string }) {
  const titleId = `${path.id}-title`;
  return (
    <section id={path.id} aria-labelledby={titleId} className="scroll-mt-8 border-b border-line">
      <div className="rail grid md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <div className="cell border-b border-line md:border-r md:border-b-0">
          <div className="md:sticky md:top-10">
            <NumberedTitle id={titleId} n={n} label={path.eyebrow} title={path.title} />
            <div className="reveal mt-10 w-[300px] max-w-full">
              <GateList />
            </div>
          </div>
        </div>
        <ol className="cell">
          {path.steps.map((step, i) => (
            <li
              key={step.title}
              className="step-on grid grid-cols-[56px_1fr] gap-4 border-t border-line py-10 first:border-t-0 first:pt-0 md:min-h-[300px] md:grid-cols-[72px_1fr] md:first:border-t md:first:pt-10"
            >
              <span className="mono pt-2.5 text-[12px] text-accent">{String(i + 1).padStart(2, "0")}</span>
              <div>
                <h3 className="text-[clamp(30px,3.4vw,46px)] leading-none font-bold tracking-[-0.025em]">{step.title}</h3>
                <p className="mt-3.5 max-w-[420px] text-[17px] leading-normal text-muted">{step.body}</p>
              </div>
            </li>
          ))}
        </ol>
      </div>
    </section>
  );
}

/** The goal broken into gates, as the app shows it. */
function GateList() {
  const { gates } = path;
  return (
    <figure className="border border-tick bg-bg" aria-label={`Example plan for a ${gates.goal}`}>
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
