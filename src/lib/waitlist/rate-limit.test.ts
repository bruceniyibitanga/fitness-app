import { describe, expect, it } from "vitest";
import { MemoryRateLimiter } from "./rate-limit";

describe("MemoryRateLimiter", () => {
  it("allows up to the limit, then blocks", () => {
    const limiter = new MemoryRateLimiter(2, 1_000, () => 0);
    expect(limiter.allow("ip")).toBe(true);
    expect(limiter.allow("ip")).toBe(true);
    expect(limiter.allow("ip")).toBe(false);
  });

  it("tracks each key separately", () => {
    const limiter = new MemoryRateLimiter(1, 1_000, () => 0);
    expect(limiter.allow("a")).toBe(true);
    expect(limiter.allow("b")).toBe(true);
    expect(limiter.allow("a")).toBe(false);
  });

  it("resets after the window", () => {
    let now = 0;
    const limiter = new MemoryRateLimiter(1, 1_000, () => now);
    expect(limiter.allow("ip")).toBe(true);
    expect(limiter.allow("ip")).toBe(false);
    now = 1_000;
    expect(limiter.allow("ip")).toBe(true);
  });
});
