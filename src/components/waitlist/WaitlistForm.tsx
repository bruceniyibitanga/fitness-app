"use client";

import { useActionState, useId } from "react";
import { joinWaitlist, type WaitlistState } from "@/app/actions/waitlist";
import { waitlistCopy } from "@/content/site";

const initialState: WaitlistState = { status: "idle", message: "" };

export function WaitlistForm({ className = "" }: { className?: string }) {
  const [state, action, pending] = useActionState(joinWaitlist, initialState);
  const id = useId();
  const inputId = `${id}-email`;
  const hintId = `${id}-hint`;

  if (state.status === "success") {
    return (
      <div role="status" className={`border border-accent bg-surface/90 px-5 py-4 ${className}`}>
        <p className="mono text-[11px] text-accent">You&apos;re on the list</p>
        <p className="mt-1.5 text-[15px] leading-snug">{state.message}</p>
      </div>
    );
  }

  const isError = state.status === "error";

  return (
    <form action={action} className={className} noValidate>
      <label htmlFor={inputId} className="mono mb-2.5 block text-[11px] text-muted">
        {waitlistCopy.label}
      </label>
      <div className="flex flex-wrap border border-tick bg-bg/70 backdrop-blur-sm">
        <input
          id={inputId}
          name="email"
          type="email"
          required
          maxLength={254}
          autoComplete="email"
          inputMode="email"
          placeholder={waitlistCopy.placeholder}
          aria-invalid={isError}
          aria-describedby={hintId}
          className="h-14 min-w-0 flex-[1_1_220px] bg-transparent px-4 text-base text-text placeholder:text-faint focus-visible:outline-offset-[-2px]"
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
          className="h-14 cursor-pointer bg-accent px-6 text-[15px] font-bold text-bg transition-colors hover:bg-accent-hover disabled:cursor-wait disabled:opacity-70"
        >
          {pending ? "Adding you…" : waitlistCopy.button}
        </button>
      </div>
      <p id={hintId} aria-live="polite" className={`mt-2.5 text-[13px] ${isError ? "text-accent" : "text-muted"}`}>
        {isError ? state.message : waitlistCopy.hint}
      </p>
    </form>
  );
}
