import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { GUIDE_SLUGS, getGuide } from "@/content/guides";
import { footer, site } from "@/content/site";
import { LogoMark } from "@/components/layout/Logo";

/**
 * The printable plan behind each guide's free PDF. Not meant to be browsed: it's laid out
 * as two A4 sheets and printed to PDF by scripts/build-plans.mjs. Hidden from search, and
 * always on white paper whatever theme the visitor picked.
 */
export const dynamicParams = false;

export function generateStaticParams() {
  return GUIDE_SLUGS.map((slug) => ({ slug }));
}

export async function generateMetadata({ params }: PageProps<"/guides/[slug]/plan">): Promise<Metadata> {
  const guide = getGuide((await params).slug);
  return {
    title: guide ? `${guide.plan.title} · ${site.name}` : site.name,
    robots: { index: false, follow: false },
  };
}

const WEEKS = [1, 2, 3, 4];

export default async function PlanSheet({ params }: PageProps<"/guides/[slug]/plan">) {
  const guide = getGuide((await params).slug);
  if (!guide) notFound();
  const url = `${site.url.replace(/^https?:\/\//, "")}/guides/${guide.slug}`;

  return (
    <main className="sheet">
      {/* Sheet 1: what it is, and the stages as a checklist. */}
      <section className="sheet-page">
        <SheetHeader url={url} />
        <p className="mono mt-14 text-[9px] text-accent">Free plan · {guide.skill}</p>
        <h1 className="display mt-3 text-[64px] leading-[0.92]">
          {guide.plan.title.replace(/\.$/, "")}
          <span className="text-accent">.</span>
        </h1>
        <p className="mt-6 max-w-[560px] text-[12.5px] leading-relaxed text-soft">{guide.answer}</p>

        <div className="mt-8 flex gap-8 border-y border-line py-4">
          <p className="mono shrink-0 pt-0.5 text-[8px] text-muted">Before you start</p>
          <ul className="flex flex-wrap gap-x-6 gap-y-1.5 text-[11.5px]">
            {guide.before.map((item) => (
              <li key={item} className="flex items-center gap-2">
                <span className="inline-block size-2.5 border border-text" />
                {item}
              </li>
            ))}
          </ul>
        </div>

        <h2 className="mono mt-9 text-[9px] text-muted">The stages · tick each one off when you pass it</h2>
        <table className="mt-3 w-full border-collapse text-left text-[11.5px]">
          <thead>
            <tr className="mono border-b border-text text-[8px] text-muted">
              <th className="w-8 py-2 font-normal" />
              <th className="py-2 font-normal">Stage</th>
              <th className="py-2 font-normal">Pass when you can do</th>
              <th className="w-24 py-2 font-normal">Date passed</th>
            </tr>
          </thead>
          <tbody>
            {guide.stages.map((stage, i) => (
              <tr key={stage.name} className="border-b border-line align-top">
                <td className="py-3">
                  <span className="inline-block size-3.5 border border-text" />
                </td>
                <td className="py-3 pr-4">
                  <p className="display text-[19px] leading-none">
                    <span className="mono mr-2 align-middle text-[8px] text-muted">{String(i + 1).padStart(2, "0")}</span>
                    {stage.name}
                  </p>
                  <p className="mt-1.5 text-[10.5px] leading-snug text-muted">{stage.cue}</p>
                </td>
                <td className="py-3 pr-4 font-medium">{stage.pass}</td>
                <td className="py-3">
                  <span className="mt-3 block border-b border-tick" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <SheetFooter page={1} />
      </section>

      {/* Sheet 2: the week, a four-week log, and the mistakes to check. */}
      <section className="sheet-page">
        <SheetHeader url={url} />
        <h2 className="display mt-12 text-[34px] leading-none">Your week</h2>
        <div className="mt-5 grid grid-cols-3 gap-4">
          {guide.week.map((day) => (
            <div key={day.label} className="border border-line p-4">
              <p className="mono text-[8px] text-muted">{day.label}</p>
              <p className="display mt-1.5 text-[20px] leading-none">{day.focus}</p>
              <ul className="mt-3 space-y-1.5 text-[10.5px]">
                {day.items.map((item) => (
                  <li key={item.name} className="border-b border-line pb-1.5 last:border-b-0">
                    <p>{item.name}</p>
                    <p className="mono mt-0.5 text-[7.5px] text-muted">{item.dose}</p>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <p className="mt-3 text-[10px] text-muted">{guide.weekNote}</p>

        <h2 className="display mt-9 text-[34px] leading-none">Four-week log</h2>
        <table className="mt-4 w-full border-collapse text-[10px]">
          <thead>
            <tr className="mono text-left text-[8px] text-muted">
              <th className="w-16 border-b border-text py-2 font-normal" />
              {guide.week.map((day) => (
                <th key={day.label} className="border-b border-text py-2 font-normal">{day.label} · {day.focus}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {WEEKS.map((week) => (
              <tr key={week}>
                <td className="mono border-b border-line py-2 align-top text-[8px] text-muted">Week {week}</td>
                {guide.week.map((day) => (
                  <td key={day.label} className="h-[64px] border-b border-l border-line px-2 align-top">
                    <span className="mono text-[7px] text-faint">Stage · best hold or reps</span>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>

        <h2 className="mono mt-8 text-[9px] text-muted">Check yourself</h2>
        <ul className="mt-3 grid grid-cols-2 gap-x-6 gap-y-3 text-[10.5px]">
          {guide.mistakes.map((m) => (
            <li key={m.title}>
              <p className="font-medium">{m.title}</p>
              <p className="mt-0.5 leading-snug text-muted">{m.fix}</p>
            </li>
          ))}
        </ul>

        <SheetFooter page={2} />
      </section>
    </main>
  );
}

function SheetHeader({ url }: { url: string }) {
  return (
    <div className="flex items-center justify-between border-b border-text pb-3">
      <p className="flex items-center gap-2">
        <span className="text-accent">
          <LogoMark size={14} />
        </span>
        <span className="mono text-[9px] tracking-[0.22em]">{site.name}</span>
      </p>
      <p className="mono text-[8px] text-muted">{url}</p>
    </div>
  );
}

function SheetFooter({ page }: { page: number }) {
  return (
    <div className="mt-auto flex items-end justify-between gap-8 border-t border-line pt-3 text-[8.5px] leading-snug text-muted">
      <p className="max-w-[420px]">{footer.disclaimer}</p>
      <p className="mono shrink-0 text-[8px]">{page} / 2</p>
    </div>
  );
}
