"use server";

import { headers } from "next/headers";
import { parseWaitlistForm } from "@/lib/waitlist/schema";
import { MemoryRateLimiter } from "@/lib/waitlist/rate-limit";
import { getWaitlistRepository } from "@/lib/waitlist/repository";

export type WaitlistState = {
  status: "idle" | "success" | "error";
  message: string;
};

// 5 attempts per address per 10 minutes.
const limiter = new MemoryRateLimiter(5, 10 * 60 * 1000);

const SUCCESS: WaitlistState = { status: "success", message: "You're on the list. We'll email you when early access opens." };

/**
 * Server Actions are public POST endpoints, so treat every input as hostile:
 * Next.js already rejects cross-origin calls; here we rate limit, validate and
 * keep error messages generic so nothing internal leaks to the browser.
 */
export async function joinWaitlist(_prev: WaitlistState, formData: FormData): Promise<WaitlistState> {
  const requestHeaders = await headers();
  const ip = requestHeaders.get("x-forwarded-for")?.split(",")[0]?.trim() || "unknown";

  if (!limiter.allow(ip)) {
    return { status: "error", message: "Too many tries. Give it a few minutes." };
  }

  const parsed = parseWaitlistForm(formData);
  if (!parsed.ok) {
    return { status: "error", message: parsed.message };
  }

  // A bot filled the hidden field. Pretend it worked so it doesn't try again.
  if (parsed.data.company) {
    return SUCCESS;
  }

  try {
    await getWaitlistRepository().add(parsed.data.email);
    return SUCCESS;
  } catch (error) {
    console.error("[waitlist] save failed:", error instanceof Error ? error.message : "unknown error");
    return { status: "error", message: "Something went wrong on our side. Please try again later." };
  }
}
