import "server-only";
import { serverEnv } from "@/lib/env";

export interface WaitlistRepository {
  /** Saves an email. Adding one that's already there is not an error. */
  add(email: string): Promise<void>;
}

/**
 * Writes to a Supabase table through its REST API, so we don't need the client library.
 * Uses the service role key, which must only ever exist on the server.
 * The table has row level security on and no policies (see supabase/migrations),
 * so the public anon key can't read or write it.
 */
export class SupabaseWaitlistRepository implements WaitlistRepository {
  constructor(
    private readonly url: string,
    private readonly serviceKey: string,
  ) {}

  async add(email: string): Promise<void> {
    const response = await fetch(`${this.url}/rest/v1/waitlist?on_conflict=email`, {
      method: "POST",
      headers: {
        apikey: this.serviceKey,
        Authorization: `Bearer ${this.serviceKey}`,
        "Content-Type": "application/json",
        // Duplicates are ignored, so the response is the same whether or not the
        // email was already on the list. That stops people checking who signed up.
        Prefer: "resolution=ignore-duplicates,return=minimal",
      },
      body: JSON.stringify({ email }),
      cache: "no-store",
      signal: AbortSignal.timeout(5_000),
    });

    if (!response.ok) {
      throw new Error(`Waitlist insert failed with status ${response.status}`);
    }
  }
}

/** Local development without Supabase: logs a masked email instead of saving it. */
export class DevLogWaitlistRepository implements WaitlistRepository {
  async add(email: string): Promise<void> {
    const [name, domain] = email.split("@");
    console.info(`[waitlist:dev] would save ${name?.slice(0, 2)}***@${domain}`);
  }
}

export function getWaitlistRepository(): WaitlistRepository {
  const env = serverEnv();
  if (env.SUPABASE_URL && env.SUPABASE_SERVICE_ROLE_KEY) {
    return new SupabaseWaitlistRepository(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
  }
  if (env.NODE_ENV === "production") {
    throw new Error("Waitlist storage is not configured. Set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY.");
  }
  return new DevLogWaitlistRepository();
}
