/**
 * Share of total load pushed in each push-up variation, measured on force plates.
 * Ebben et al. 2011 (J Strength Cond Res 25:2891) and Suprak et al. 2011.
 */
export const PUSH_UP_VARIANTS = [
  { id: "hands-61", name: "Hands on 61 cm box", share: 0.41 },
  { id: "knees", name: "Knee push-up", share: 0.49 },
  { id: "hands-30", name: "Hands on 30 cm box", share: 0.55 },
  { id: "regular", name: "Regular push-up", share: 0.64 },
  { id: "feet-30", name: "Feet on 30 cm box", share: 0.7 },
  { id: "feet-61", name: "Feet on 61 cm box", share: 0.74 },
] as const;

export type PushUpVariantId = (typeof PUSH_UP_VARIANTS)[number]["id"];

export const LIMITS = {
  bodyweight: { min: 35, max: 160, step: 1 },
  vest: { min: 0, max: 40, step: 2.5 },
} as const;

export function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value));
}

export function variantById(id: PushUpVariantId) {
  const variant = PUSH_UP_VARIANTS.find((v) => v.id === id);
  if (!variant) throw new Error(`Unknown push-up variant: ${id}`);
  return variant;
}

/**
 * Estimated kilograms pushed per rep. A vest is counted at the same share as bodyweight,
 * which is a fair estimate for a vest worn on the torso.
 */
export function pushUpLoadKg(bodyweightKg: number, vestKg: number, variantId: PushUpVariantId): number {
  const body = clamp(bodyweightKg, LIMITS.bodyweight.min, LIMITS.bodyweight.max);
  const vest = clamp(vestKg, LIMITS.vest.min, LIMITS.vest.max);
  const kg = (body + vest) * variantById(variantId).share;
  return Math.round(kg * 10) / 10;
}
