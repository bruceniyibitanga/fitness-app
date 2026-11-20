"use server";

import { headers } from "next/headers";
import { getGuide, isGuideSlug } from "@/content/guides";
import { clientIp, signUp } from "@/lib/waitlist/signup";

export type PlanState = {
  status: "idle" | "success" | "error";
  message: string;
  /** Set on success. Always one of the plan files in guides.ts, never anything from the request. */
  file?: string;
};

/**
 * The free plan form on each guide. The guide comes from a hidden field, so it's checked
 * against the allowlist before anything else; the download link is looked up from our own
 * content, never echoed back from the request.
 */
export async function requestGuidePlan(_prev: PlanState, formData: FormData): Promise<PlanState> {
  const slug = formData.get("guide");
  if (!isGuideSlug(slug)) {
    return { status: "error", message: "Something went wrong. Refresh the page and try again." };
  }

  const ip = clientIp((await headers()).get("x-forwarded-for"));
  const result = await signUp(formData, { guide: slug }, { ip });
  if (!result.ok) {
    return { status: "error", message: result.message };
  }

  return {
    status: "success",
    message: "Your plan is ready. We'll also email you when early access opens.",
    file: getGuide(slug)?.plan.file,
  };
}
