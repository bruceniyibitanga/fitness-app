import { describe, expect, it, vi } from "vitest";
import {
  DevLogWaitlistRepository,
  KitWaitlistRepository,
  SupabaseWaitlistRepository,
  createWaitlistRepository,
} from "./repository";

function okFetch(status = 201) {
  return vi.fn<typeof fetch>(async () => new Response(null, { status }));
}

describe("KitWaitlistRepository", () => {
  it("creates the subscriber with the API key header", async () => {
    const fetchFn = okFetch();
    await new KitWaitlistRepository("kit_test_key_123", undefined, fetchFn).add("a@example.com");

    expect(fetchFn).toHaveBeenCalledTimes(1);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe("https://api.kit.com/v4/subscribers");
    expect(init?.method).toBe("POST");
    expect((init?.headers as Record<string, string>)["X-Kit-Api-Key"]).toBe("kit_test_key_123");
    expect(init?.body).toBe(JSON.stringify({ email_address: "a@example.com" }));
  });

  it("tags the subscriber when a tag ID is set", async () => {
    const fetchFn = okFetch();
    await new KitWaitlistRepository("kit_test_key_123", "42", fetchFn).add("a@example.com");

    expect(fetchFn).toHaveBeenCalledTimes(2);
    expect(fetchFn.mock.calls[1][0]).toBe("https://api.kit.com/v4/tags/42/subscribers");
  });

  it("treats an existing subscriber (200) as success", async () => {
    await expect(new KitWaitlistRepository("kit_test_key_123", undefined, okFetch(200)).add("a@example.com")).resolves.toBeUndefined();
  });

  it("throws on an error status without leaking the key", async () => {
    const repo = new KitWaitlistRepository("kit_test_key_123", undefined, okFetch(401));
    await expect(repo.add("a@example.com")).rejects.toThrow(/401/);
    await expect(repo.add("a@example.com")).rejects.not.toThrow(/kit_test_key_123/);
  });
});

describe("createWaitlistRepository", () => {
  it("prefers Kit when its key is set", () => {
    const repo = createWaitlistRepository({ NODE_ENV: "production", KIT_API_KEY: "kit_test_key_123", SUPABASE_URL: "https://x.supabase.co", SUPABASE_SERVICE_ROLE_KEY: "x".repeat(30) });
    expect(repo).toBeInstanceOf(KitWaitlistRepository);
  });

  it("falls back to Supabase", () => {
    const repo = createWaitlistRepository({ NODE_ENV: "production", SUPABASE_URL: "https://x.supabase.co", SUPABASE_SERVICE_ROLE_KEY: "x".repeat(30) });
    expect(repo).toBeInstanceOf(SupabaseWaitlistRepository);
  });

  it("logs locally when nothing is set", () => {
    expect(createWaitlistRepository({ NODE_ENV: "development" })).toBeInstanceOf(DevLogWaitlistRepository);
  });

  it("refuses to run in production with no storage", () => {
    expect(() => createWaitlistRepository({ NODE_ENV: "production" })).toThrow(/KIT_API_KEY/);
  });
});
