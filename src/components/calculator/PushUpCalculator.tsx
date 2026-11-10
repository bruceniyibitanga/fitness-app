"use client";

import { useState } from "react";
import { LIMITS, PUSH_UP_VARIANTS, clamp, pushUpLoadKg, variantById, type PushUpVariantId } from "@/lib/pushup-load";
import { calculator } from "@/content/site";

const MIN_SHARE = PUSH_UP_VARIANTS[0].share;
const MAX_SHARE = PUSH_UP_VARIANTS[PUSH_UP_VARIANTS.length - 1].share;

export function PushUpCalculator() {
  const [bodyweight, setBodyweight] = useState(74);
  const [vest, setVest] = useState(0);
  const [variantId, setVariantId] = useState<PushUpVariantId>("regular");

  const share = variantById(variantId).share;
  const kg = pushUpLoadKg(bodyweight, vest, variantId);
  const meter = ((share - MIN_SHARE) / (MAX_SHARE - MIN_SHARE)) * 100;

  return (
    <div className="grid border border-line md:grid-cols-2">
      <div className="p-6 md:p-8">
        <Stepper
          label="Your bodyweight"
          value={bodyweight}
          onChange={(v) => setBodyweight(clamp(v, LIMITS.bodyweight.min, LIMITS.bodyweight.max))}
          step={LIMITS.bodyweight.step}
        />
        <Stepper
          label="Weighted vest"
          value={vest}
          onChange={(v) => setVest(clamp(v, LIMITS.vest.min, LIMITS.vest.max))}
          step={LIMITS.vest.step}
          className="mt-7"
        />
        <fieldset className="mt-7">
          <legend className="mono text-[11px] text-muted">Variation</legend>
          <div className="mt-3 grid grid-cols-2 gap-2">
            {PUSH_UP_VARIANTS.map((v) => {
              const active = v.id === variantId;
              return (
                <button
                  key={v.id}
                  type="button"
                  aria-pressed={active}
                  onClick={() => setVariantId(v.id)}
                  className={`min-h-11 cursor-pointer border px-3.5 text-left text-sm transition-colors ${
                    active ? "border-accent bg-accent/10 text-text" : "border-tick text-muted hover:border-muted"
                  }`}
                >
                  {v.name}
                </button>
              );
            })}
          </div>
        </fieldset>
      </div>

      <div className="flex flex-col justify-between gap-8 border-t border-line bg-surface p-6 md:border-t-0 md:border-l md:p-8">
        <div aria-live="polite">
          <p className="mono text-[11px] text-muted">You push about</p>
          <p className="display mt-4 text-[clamp(88px,11vw,150px)] leading-[0.85] text-accent">
            {kg.toFixed(1)}
            <span className="text-[32px] tracking-normal text-text"> kg</span>
          </p>
          <p className="mt-3 text-[17px] text-muted">per rep · {Math.round(share * 100)}% of total load</p>
        </div>
        <div>
          <div className="relative h-2 bg-track" aria-hidden="true">
            <span className="absolute -top-1 h-4 w-1 bg-accent transition-[left] duration-500" style={{ left: `calc(${meter}% - 2px)` }} />
          </div>
          <div className="mt-2 flex justify-between">
            <span className="mono text-[10px] text-faint">Easiest {Math.round(MIN_SHARE * 100)}%</span>
            <span className="mono text-[10px] text-faint">Hardest {Math.round(MAX_SHARE * 100)}%</span>
          </div>
          <p className="mt-5 text-[13px] leading-normal text-faint">
            {calculator.source}{" "}
            <a href={calculator.sourceUrl} rel="noopener noreferrer" target="_blank" className="text-muted underline hover:text-text">
              Read the study
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}

type StepperProps = {
  label: string;
  value: number;
  step: number;
  onChange: (value: number) => void;
  className?: string;
};

function Stepper({ label, value, step, onChange, className = "" }: StepperProps) {
  const buttonClass = "h-12 w-12 cursor-pointer border border-tick text-xl hover:border-muted";
  return (
    <div className={className}>
      <p className="mono text-[11px] text-muted">{label}</p>
      <div className="mt-3 flex items-center gap-3">
        <button type="button" className={buttonClass} aria-label={`Decrease ${label.toLowerCase()}`} onClick={() => onChange(value - step)}>
          −
        </button>
        <p className="min-w-[120px] text-center text-[40px] font-[750] [font-stretch:85%]" aria-live="polite">
          {value}
          <span className="text-lg text-muted"> kg</span>
        </p>
        <button type="button" className={buttonClass} aria-label={`Increase ${label.toLowerCase()}`} onClick={() => onChange(value + step)}>
          +
        </button>
      </div>
    </div>
  );
}
