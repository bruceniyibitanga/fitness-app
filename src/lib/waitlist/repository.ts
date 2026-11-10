import "server-only";
import { serverEnv, type ServerEnv } from "@/lib/env";

export interface WaitlistRepository {
  /** Saves an email. Adding one that's already there is not an error. */
  add(email: string): Promise<void>;
}

const TIMEOUT_MS = 5_000;

/**
 * Plunk (useplunk.com). Its free tier covers the launch email, and it handles
 * unsubscribes, so there's no database to run.
 *
 * `POST /contacts` is an upsert matched by email, so a repeat signup updates
 * the same contact instead of failing. It needs the secret key (`sk_`), which
 * is why this only ever runs on the server.
 *
 * Contacts get `waitlist: true` in their data, so a Plunk segment on that
 * field gives the launch list.
 */
export class PlunkWaitlistRepository implements WaitlistRepository {
  private static readonly API = "https://next-api.useplunk.com";

  constructor(
    private readonly secretKey: string,
    private readonly fetchFn: typeof fetch = fetch,
  ) {}

  async add(email: string): Promise<void> {
    const response = await this.fetchFn(`${PlunkWaitlistRepository.API}/contacts`, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${this.secretKey}`,
        "Content-Type": "application/json",
        Accept: "application/json",
      },
      body: JSON.stringify({
        email,
        // Joining the list is the opt-in. Set it explicitly: Plunk's docs
        // disagree on whether new contacts default to subscribed.
        subscribed: true,
        data: { waitlist: true },
      }),
      cache: "no-store",
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) {
      // Only Plunk's error code goes in the message, never the email or the key.
      const code = await errorCode(response);
      throw new Error(`Plunk contact request failed with status ${response.status}${code ? ` (${code})` : ""}`);
    }
  }
}

/** Reads `error.code` from a Plunk error body, if there is one. */
async function errorCode(response: Response): Promise<string | undefined> {
  try {
    const body: unknown = await response.json();
    const code = (body as { error?: { code?: unknown } }).error?.code;
    return typeof code === "string" && /^[A-Z_]{1,64}$/.test(code) ? code : undefined;
  } catch {
    return undefined;
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

/** Picks storage from whichever keys are set. Plunk first, then Supabase. */
export function createWaitlistRepository(env: ServerEnv): WaitlistRepository {
  if (env.PLUNK_SECRET_KEY) {
    return new PlunkWaitlistRepository(env.PLUNK_SECRET_KEY);
  }
  if (env.SUPABASE_URL && env.SUPABASE_SERVICE_ROLE_KEY) {
    return new SupabaseWaitlistRepository(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
  }
  if (env.NODE_ENV === "production") {
    // Fail loudly rather than drop signups on the floor.
    throw new Error("Waitlist storage is not configured. Set PLUNK_SECRET_KEY.");
  }
  return new DevLogWaitlistRepository();
}

export function getWaitlistRepository(): WaitlistRepository {
  return createWaitlistRepository(serverEnv());
}
