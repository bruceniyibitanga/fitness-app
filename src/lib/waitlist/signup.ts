import "server-only";
import { parseWaitlistForm } from "./schema";
import { MemoryRateLimiter, type RateLimiter } from "./rate-limit";
import { getWaitlistRepository, type SignupContext, type WaitlistRepository } from "./repository";

export type SignupResult = { ok: true } | { ok: false; message: string };

type Deps = {
  ip: string;
  limiter?: RateLimiter;
  repository?: WaitlistRepository;
};

/**
 * One limiter for every signup form, so a single address can't get around it by
 * switching forms. 8 attempts per 10 minutes leaves room for someone grabbing a
 * few guide plans in a row.
 */
const sharedLimiter = new MemoryRateLimiter(8, 10 * 60 * 1000);

/**
 * The steps every signup goes through, in one place so both forms behave the same:
 * rate limit, validate, quietly drop bots, save. Errors shown to people stay generic
 * so nothing internal leaks; the real reason goes to the server log without the email.
 */
export async function signUp(formData: FormData, context: SignupContext, deps: Deps): Promise<SignupResult> {
  const limiter = deps.limiter ?? sharedLimiter;
  if (!limiter.allow(deps.ip)) {
    return { ok: false, message: "Too many tries. Give it a few minutes." };
  }

  const parsed = parseWaitlistForm(formData);
  if (!parsed.ok) {
    return { ok: false, message: parsed.message };
  }

  // A bot filled the hidden field. Pretend it worked so it doesn't try again.
  if (parsed.data.company) {
    return { ok: true };
  }

  try {
    await (deps.repository ?? getWaitlistRepository()).add(parsed.data.email, context);
    return { ok: true };
  } catch (error) {
    console.error("[waitlist] save failed:", error instanceof Error ? error.message : "unknown error");
    return { ok: false, message: "Something went wrong on our side. Please try again later." };
  }
}

/** The caller's IP from the proxy header, or a shared bucket if there isn't one. */
export function clientIp(forwardedFor: string | null): string {
  return forwardedFor?.split(",")[0]?.trim() || "unknown";
}
