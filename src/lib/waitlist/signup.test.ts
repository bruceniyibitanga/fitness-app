import { describe, expect, it, vi } from "vitest";
import { clientIp, signUp } from "./signup";
import { MemoryRateLimiter } from "./rate-limit";
import type { WaitlistRepository } from "./repository";

function form(fields: Record<string, string>): FormData {
  const data = new FormData();
  for (const [key, value] of Object.entries(fields)) data.set(key, value);
  return data;
}

function repo(fail = false) {
  const add = vi.fn<WaitlistRepository["add"]>(async () => {
    if (fail) throw new Error("Plunk contact request failed with status 500");
  });
  return { add } satisfies WaitlistRepository;
}

const open = () => new MemoryRateLimiter(100, 1_000);

describe("signUp", () => {
  it("saves a valid email with its context", async () => {
    const repository = repo();
    const result = await signUp(form({ email: " Ana@Example.com " }), { guide: "handstand" }, { ip: "1.1.1.1", limiter: open(), repository });
    expect(result).toEqual({ ok: true });
    expect(repository.add).toHaveBeenCalledWith("ana@example.com", { guide: "handstand" });
  });

  it("rejects a bad email without saving", async () => {
    const repository = repo();
    const result = await signUp(form({ email: "nope" }), {}, { ip: "1.1.1.1", limiter: open(), repository });
    expect(result.ok).toBe(false);
    expect(repository.add).not.toHaveBeenCalled();
  });

  it("pretends to succeed for bots but saves nothing", async () => {
    const repository = repo();
    const result = await signUp(form({ email: "a@example.com", company: "Spam Inc" }), {}, { ip: "1.1.1.1", limiter: open(), repository });
    expect(result).toEqual({ ok: true });
    expect(repository.add).not.toHaveBeenCalled();
  });

  it("stops after the rate limit, before validating or saving", async () => {
    const repository = repo();
    const limiter = new MemoryRateLimiter(1, 60_000, () => 0);
    await signUp(form({ email: "a@example.com" }), {}, { ip: "2.2.2.2", limiter, repository });
    const second = await signUp(form({ email: "a@example.com" }), {}, { ip: "2.2.2.2", limiter, repository });
    expect(second).toEqual({ ok: false, message: "Too many tries. Give it a few minutes." });
    expect(repository.add).toHaveBeenCalledTimes(1);
  });

  it("keeps storage errors generic and out of the reply", async () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    const result = await signUp(form({ email: "a@example.com" }), {}, { ip: "1.1.1.1", limiter: open(), repository: repo(true) });
    expect(result).toEqual({ ok: false, message: "Something went wrong on our side. Please try again later." });
    expect(spy.mock.calls.flat().join(" ")).not.toContain("a@example.com");
    spy.mockRestore();
  });
});

describe("clientIp", () => {
  it("takes the first address from x-forwarded-for", () => {
    expect(clientIp("203.0.113.5, 10.0.0.1")).toBe("203.0.113.5");
  });

  it("falls back to a shared bucket", () => {
    expect(clientIp(null)).toBe("unknown");
    expect(clientIp("  ")).toBe("unknown");
  });
});
