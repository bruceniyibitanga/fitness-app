import { z } from "zod";

export const waitlistSchema = z.object({
  email: z
    .string()
    .trim()
    .toLowerCase()
    .max(254, "That email is too long.")
    .pipe(z.email("That email doesn't look right. Check it and try again.")),
  // Honeypot. Hidden from people, filled in by bots.
  company: z.string().max(200).optional().default(""),
});

export type WaitlistInput = z.infer<typeof waitlistSchema>;

export type ParseResult =
  | { ok: true; data: WaitlistInput }
  | { ok: false; message: string };

/** Turns raw form data into a clean, validated email. Pure, so it's easy to test. */
export function parseWaitlistForm(formData: FormData): ParseResult {
  const parsed = waitlistSchema.safeParse({
    email: formData.get("email") ?? "",
    company: formData.get("company") ?? "",
  });
  if (!parsed.success) {
    return { ok: false, message: parsed.error.issues[0]?.message ?? "Check your email and try again." };
  }
  return { ok: true, data: parsed.data };
}
