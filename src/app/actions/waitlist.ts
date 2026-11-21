"use server";

import { headers } from "next/headers";
import { clientIp, signUp } from "@/lib/waitlist/signup";

export type WaitlistState = {
  status: "idle" | "success" | "error";
  message: string;
};

/**
 * The early access form. Server Actions are public POST endpoints, so every input is
 * treated as hostile; the shared signUp() does the rate limiting and validation, and
 * Next.js already rejects cross-origin calls.
 */
export async function joinWaitlist(_prev: WaitlistState, formData: FormData): Promise<WaitlistState> {
  const ip = clientIp((await headers()).get("x-forwarded-for"));
  const result = await signUp(formData, {}, { ip });
  return result.ok
    ? { status: "success", message: "You're on the list. We'll email you when early access opens." }
    : { status: "error", message: result.message };
}
