import "server-only";
import { serverEnv, type ServerEnv } from "@/lib/env";

export interface WaitlistRepository {
  /** Saves an email. Adding one that's already there is not an error. */
  add(email: string): Promise<void>;
}

const TIMEOUT_MS = 5_000;

/**
 * Kit (formerly ConvertKit). Free for up to 10,000 subscribers, and it handles
 * unsubscribes and the launch email, so there's no database to run.
 * Creating a subscriber is an upsert, so a repeat signup is harmless.
 * If a tag ID is set, the subscriber is tagged so the waitlist is easy to filter.
 */
export class KitWaitlistRepository implements WaitlistRepository {
  private static readonly API = "https://api.kit.com/v4";

  constructor(
    private readonly apiKey: string,
    private readonly tagId?: string,
    private readonly fetchFn: typeof fetch = fetch,
  ) {}

  async add(email: string): Promise<void> {
    await this.post("/subscribers", { email_address: email });
    if (this.tagId) {
      await this.post(`/tags/${this.tagId}/subscribers`, { email_address: email });
    }
  }

  private async post(path: string, body: Record<string, string>) {
    const response = await this.fetchFn(`${KitWaitlistRepository.API}${path}`, {
      method: "POST",
      headers: { "X-Kit-Api-Key": this.apiKey, "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(body),
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) {
      throw new Error(`Kit request to ${path.split("/")[1]} failed with status ${response.status}`);
    }
  }
}

/**
 * Supabase table through its REST API, for when the project moves to a database.
 * The service role key must only ever exist on the server. The table has row level
 * security on and no policies (see supabase/migrations).
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
        // Duplicates are ignored, so the reply is the same whether or not the email was already there.
        Prefer: "resolution=ignore-duplicates,return=minimal",
      },
      body: JSON.stringify({ email }),
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) {
      throw new Error(`Waitlist insert failed with status ${response.status}`);
    }
  }
}

/** Local development with no keys: logs a masked email instead of saving it. */
export class DevLogWaitlistRepository implements WaitlistRepository {
  async add(email: string): Promise<void> {
    const [name, domain] = email.split("@");
    console.info(`[waitlist:dev] would save ${name?.slice(0, 2)}***@${domain}`);
  }
}

/** Picks storage from whichever keys are set. Kit first, then Supabase. */
export function createWaitlistRepository(env: ServerEnv): WaitlistRepository {
  if (env.KIT_API_KEY) {
    return new KitWaitlistRepository(env.KIT_API_KEY, env.KIT_TAG_ID);
  }
  if (env.SUPABASE_URL && env.SUPABASE_SERVICE_ROLE_KEY) {
    return new SupabaseWaitlistRepository(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
  }
  if (env.NODE_ENV === "production") {
    // Fail loudly rather than drop signups on the floor.
    throw new Error("Waitlist storage is not configured. Set KIT_API_KEY.");
  }
  return new DevLogWaitlistRepository();
}

export function getWaitlistRepository(): WaitlistRepository {
  return createWaitlistRepository(serverEnv());
}
