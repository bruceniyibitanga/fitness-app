export interface RateLimiter {
  /** Returns true if this key may make another request now. */
  allow(key: string): boolean;
}

/**
 * Fixed-window limiter kept in memory.
 *
 * Good enough to stop one person hammering the form. On serverless hosting each
 * instance has its own memory, so swap this for a shared store (Upstash Redis,
 * a Supabase table) before relying on it at scale. Anything that implements
 * RateLimiter can be dropped in without touching the action.
 */
export class MemoryRateLimiter implements RateLimiter {
  private readonly hits = new Map<string, { count: number; resetAt: number }>();

  constructor(
    private readonly limit: number,
    private readonly windowMs: number,
    private readonly now: () => number = Date.now,
  ) {}

  allow(key: string): boolean {
    const time = this.now();
    const entry = this.hits.get(key);

    if (!entry || entry.resetAt <= time) {
      this.hits.set(key, { count: 1, resetAt: time + this.windowMs });
      this.prune(time);
      return true;
    }

    entry.count += 1;
    return entry.count <= this.limit;
  }

  private prune(time: number) {
    if (this.hits.size < 5_000) return;
    for (const [key, entry] of this.hits) {
      if (entry.resetAt <= time) this.hits.delete(key);
    }
  }
}
