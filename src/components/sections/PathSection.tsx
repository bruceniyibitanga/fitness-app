import { path } from "@/content/site";
import { SectionTitle } from "@/components/ui/SectionTitle";
import { Curtain } from "@/components/ui/Curtain";

type GateState = (typeof path.gates.items)[number]["state"];

const dotClass: Record<GateState, string> = {
  done: "bg-faint",
  current: "bg-accent",
  next: "border border-tick",
  goal: "border-2 border-accent",
};

/**
 * How it works. A tall photo with the goal's gates floating over its corner on the left;
 * three short steps on the right, set lower so the two columns don't line up.
 */
export function PathSection() {
  const titleId = `${path.id}-title`;
  return (
    <section id={path.id} aria-labelledby={titleId} className="gutter section-pad mx-auto max-w-[1200px] scroll-mt-8">
      <SectionTitle id={titleId} eyebrow={path.eyebrow} title={path.title} className="max-w-[760px]" />

      <div className="mt-[clamp(56px,8vw,112px)] grid gap-y-20 md:grid-cols-12 md:gap-x-6">
        <div className="relative md:col-span-6 md:col-start-1">
          <Curtain photo={path.photo} sizes="(min-width: 768px) 50vw, 100vw" className="aspect-[4/5] w-[86%]" />
          <div className="lift absolute right-0 bottom-[-10%] w-[300px] max-w-[78%]">
            <GateList />
          </div>
        </div>

        <ol className="md:col-span-5 md:col-start-8 md:pt-[clamp(80px,14vw,200px)]">
          {path.steps.map((step, i) => (
            <li key={step.title} className={`slide slide-right ${i === 1 ? "later" : i === 2 ? "latest" : ""} border-t border-line py-9`}>
              <span className="mono text-[11px] text-accent">{String(i + 1).padStart(2, "0")}</span>
              <h3 className="mt-4 text-[clamp(28px,3vw,40px)] leading-none font-semibold tracking-[-0.025em]">{step.title}</h3>
              <p className="mt-3 max-w-[360px] text-base leading-relaxed text-muted">{step.body}</p>
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
    <figure className="border border-tick bg-bg/90 backdrop-blur-md" aria-label={`Example plan for a ${gates.goal}`}>
      <figcaption className="flex justify-between border-b border-line px-4 py-3.5">
        <span className="mono text-[10px] text-muted">Goal · {gates.goal}</span>
        <span className="mono text-[10px] text-accent">{gates.remaining}</span>
      </figcaption>
      <ul>
        {gates.items.map((gate) => {
          const tag = gate.state === "done" ? "Done" : gate.state === "current" ? gate.progress : gate.state === "goal" ? "Goal" : "";
          return (
            <li key={gate.name} className="grid min-h-[46px] grid-cols-[22px_1fr_auto] items-center gap-3 border-b border-track px-4 last:border-b-0">
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
