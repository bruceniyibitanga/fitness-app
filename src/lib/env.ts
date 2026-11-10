import "server-only";
import { z } from "zod";

/** Treats `KEY=` (empty) the same as not set, so copying .env.example doesn't break validation. */
function optional<T extends z.ZodType>(type: T) {
  return z.preprocess((value) => (value === "" ? undefined : value), type.optional());
}

/**
 * Server-only settings, checked once when first read.
 * Storage keys are optional so the site runs locally without them;
 * the waitlist repository decides what to do when they're missing.
 */
const schema = z.object({
  NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  // The secret key, which Plunk's contacts endpoint requires. The check stops
  // the public key (pk_) being pasted here by mistake.
  PLUNK_SECRET_KEY: optional(z.string().regex(/^sk_\S{10,}$/, "must be the Plunk secret key (sk_...)")),
  SUPABASE_URL: optional(z.url()),
  SUPABASE_SERVICE_ROLE_KEY: optional(z.string().min(20)),
});

export type ServerEnv = z.infer<typeof schema>;

let cached: ServerEnv | undefined;

export function serverEnv(): ServerEnv {
  if (!cached) {
    const parsed = schema.safeParse(process.env);
    if (!parsed.success) {
      // Name the bad variables, never print their values.
      const names = parsed.error.issues.map((issue) => issue.path.join(".")).join(", ");
      throw new Error(`Invalid server environment variables: ${names}`);
    }
    cached = parsed.data;
  }
  return cached;
}
