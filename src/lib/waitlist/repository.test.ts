import { describe, expect, it, vi } from "vitest";
import {
  DevLogWaitlistRepository,
  PlunkWaitlistRepository,
  SupabaseWaitlistRepository,
  createWaitlistRepository,
} from "./repository";

function okFetch(status = 201) {
  return vi.fn<typeof fetch>(async () => new Response(null, { status }));
}

const KEY = "sk_test_secret_key_123";

function jsonFetch(status: number, body: unknown) {
  return vi.fn<typeof fetch>(async () => new Response(JSON.stringify(body), { status }));
}

describe("PlunkWaitlistRepository", () => {
  it("upserts the contact with the secret key as a bearer token", async () => {
    const fetchFn = okFetch();
    await new PlunkWaitlistRepository(KEY, fetchFn).add("a@example.com");

    expect(fetchFn).toHaveBeenCalledTimes(1);
    const [url, init] = fetchFn.mock.calls[0];
    expect(url).toBe("https://next-api.useplunk.com/contacts");
    expect(init?.method).toBe("POST");
    expect((init?.headers as Record<string, string>).Authorization).toBe(`Bearer ${KEY}`);
    expect(JSON.parse(init?.body as string)).toEqual({
      email: "a@example.com",
      subscribed: true,
      data: { waitlist: true },
    });
  });

  it("treats an existing contact (200) as success", async () => {
    await expect(new PlunkWaitlistRepository(KEY, okFetch(200)).add("a@example.com")).resolves.toBeUndefined();
  });

  it("includes Plunk's error code but never the key or the email", async () => {
    const fetchFn = jsonFetch(401, { success: false, error: { code: "INVALID_API_KEY", message: "Invalid key" } });
    const error = await new PlunkWaitlistRepository(KEY, fetchFn).add("a@example.com").catch((e: Error) => e);

    expect(error).toBeInstanceOf(Error);
    expect((error as Error).message).toMatch(/401 \(INVALID_API_KEY\)/);
    expect((error as Error).message).not.toContain(KEY);
    expect((error as Error).message).not.toContain("a@example.com");
  });

  it("ignores an error code that isn't a plain code", async () => {
    const fetchFn = jsonFetch(422, { error: { code: "a@example.com" } });
    const error = await new PlunkWaitlistRepository(KEY, fetchFn).add("a@example.com").catch((e: Error) => e);
    expect((error as Error).message).toBe("Plunk contact request failed with status 422");
  });

  it("copes with an error body that isn't JSON", async () => {
    const fetchFn = vi.fn<typeof fetch>(async () => new Response("Bad gateway", { status: 502 }));
    await expect(new PlunkWaitlistRepository(KEY, fetchFn).add("a@example.com")).rejects.toThrow(/status 502$/);
  });
});

describe("createWaitlistRepository", () => {
  it("prefers Plunk when its key is set", () => {
    const repo = createWaitlistRepository({ NODE_ENV: "production", PLUNK_SECRET_KEY: KEY, SUPABASE_URL: "https://x.supabase.co", SUPABASE_SERVICE_ROLE_KEY: "x".repeat(30) });
    expect(repo).toBeInstanceOf(PlunkWaitlistRepository);
  });

  it("falls back to Supabase", () => {
    const repo = createWaitlistRepository({ NODE_ENV: "production", SUPABASE_URL: "https://x.supabase.co", SUPABASE_SERVICE_ROLE_KEY: "x".repeat(30) });
    expect(repo).toBeInstanceOf(SupabaseWaitlistRepository);
  });

  it("logs locally when nothing is set", () => {
    expect(createWaitlistRepository({ NODE_ENV: "development" })).toBeInstanceOf(DevLogWaitlistRepository);
  });

  it("refuses to run in production with no storage", () => {
    expect(() => createWaitlistRepository({ NODE_ENV: "production" })).toThrow(/PLUNK_SECRET_KEY/);
  });
});
