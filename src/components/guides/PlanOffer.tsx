"use client";

import { useActionState, useId } from "react";
import { requestGuidePlan, type PlanState } from "@/app/actions/guide-plan";
import type { Guide } from "@/content/guides";

const initialState: PlanState = { status: "idle", message: "" };

type Props = {
  guide: Pick<Guide, "slug" | "plan" | "skill">;
  className?: string;
};

/**
 * The free plan sign-up. Email in, download out. The consent line says plainly that the
 * email also joins the early access list, because that's what happens.
 */
export function PlanOffer({ guide, className = "" }: Props) {
  const [state, action, pending] = useActionState(requestGuidePlan, initialState);
  const id = useId();
  const inputId = `${id}-email`;
  const hintId = `${id}-hint`;

  if (state.status === "success" && state.file) {
    return (
      <div role="status" className={className}>
        <p className="mono text-[11px] text-accent">It&apos;s yours</p>
        <p className="mt-3 text-[15px] leading-snug text-soft">{state.message}</p>
        <a
          href={state.file}
          download
          className="button-ink mt-6 inline-flex min-h-12 items-center gap-3 px-6 text-[15px] font-medium"
        >
          Download {guide.plan.title.toLowerCase()}
          <span aria-hidden="true">↓</span>
        </a>
        <p className="mono mt-3 text-[10px] text-muted">PDF · 2 pages · A4</p>
      </div>
    );
  }

  const isError = state.status === "error";

  return (
    <form action={action} className={className} noValidate>
      <input type="hidden" name="guide" value={guide.slug} />
      <label htmlFor={inputId} className="mono mb-2.5 block text-[11px] text-muted">
        Email for the free plan
      </label>
      <div className="flex flex-wrap border border-tick bg-bg">
        <input
          id={inputId}
          name="email"
          type="email"
          required
          maxLength={254}
          autoComplete="email"
          inputMode="email"
          placeholder="you@example.com"
          aria-invalid={isError}
          aria-describedby={hintId}
          className="h-14 min-w-0 flex-[1_1_200px] bg-transparent px-4 text-base text-text placeholder:text-faint focus-visible:outline-offset-[-2px]"
        />
        {/* Honeypot: hidden from people and screen readers, tempting to bots. */}
        <div aria-hidden="true" className="absolute -left-[9999px] h-px w-px overflow-hidden">
          <label>
            Company
            <input name="company" type="text" tabIndex={-1} autoComplete="off" />
          </label>
        </div>
        <button
          type="submit"
          disabled={pending}
          className="button-ink h-14 grow cursor-pointer px-6 text-[15px] font-medium disabled:cursor-wait disabled:opacity-70 sm:grow-0"
        >
          {pending ? "Getting it…" : "Get the plan"}
        </button>
      </div>
      <p id={hintId} aria-live="polite" className={`mt-2.5 text-[13px] leading-snug ${isError ? "text-accent" : "text-muted"}`}>
        {isError ? state.message : "Free. You'll also join the Levitx early access list, and can leave it any time."}
      </p>
    </form>
  );
}
