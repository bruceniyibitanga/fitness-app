import { path } from "@/content/site";
import { SectionHeading } from "@/components/ui/SectionHeading";

type GateState = (typeof path.gates.items)[number]["state"];

const dotClass: Record<GateState, string> = {
  done: "bg-faint",
  current: "bg-accent",
  next: "border border-tick",
  goal: "border-2 border-accent",
};

export function PathSection() {
  const titleId = `${path.id}-title`;
  return (
    <section id={path.id} aria-labelledby={titleId} className="border-b border-line">
      <div className="rail grid md:grid-cols-[0.9fr_1.1fr]">
        <div className="gutter section-pad border-line md:border-r">
          <div className="md:sticky md:top-10">
            <SectionHeading id={titleId} number={path.number} label={path.label} title={path.title} animate={false} />
            <GateList />
          </div>
        </div>

        <ol className="gutter section-pad">
          {path.steps.map((step, i) => (
            <li key={step.title} className="focus-step grid min-h-[220px] grid-cols-[56px_1fr] md:min-h-[300px] md:grid-cols-[72px_1fr] gap-4 border-t border-line py-10">
              <span className="mono pt-2.5 text-xs text-accent">{String(i + 1).padStart(2, "0")}</span>
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

function GateList() {
  const { gates } = path;
  return (
    <div className="mt-10 w-[300px] max-w-full border border-tick bg-bg">
      <div className="flex justify-between border-b border-line px-4 py-3.5">
        <span className="mono text-[10px] text-muted">Goal · {gates.goal}</span>
        <span className="mono text-[10px] text-accent">{gates.remaining}</span>
      </div>
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
    </div>
  );
}
