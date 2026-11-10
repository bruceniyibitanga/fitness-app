import { describe, expect, it } from "vitest";
import { parseWaitlistForm } from "./schema";

function form(values: Record<string, string>) {
  const data = new FormData();
  for (const [key, value] of Object.entries(values)) data.set(key, value);
  return data;
}

describe("parseWaitlistForm", () => {
  it("trims and lowercases a valid email", () => {
    const result = parseWaitlistForm(form({ email: "  Bruce@Example.COM " }));
    expect(result).toEqual({ ok: true, data: { email: "bruce@example.com", company: "" } });
  });

  it("rejects something that isn't an email", () => {
    const result = parseWaitlistForm(form({ email: "not-an-email" }));
    expect(result.ok).toBe(false);
  });

  it("rejects a missing email", () => {
    expect(parseWaitlistForm(new FormData()).ok).toBe(false);
  });

  it("rejects an email longer than 254 characters", () => {
    const long = `${"a".repeat(250)}@example.com`;
    expect(parseWaitlistForm(form({ email: long })).ok).toBe(false);
  });

  it("passes the honeypot through so the action can drop bots quietly", () => {
    const result = parseWaitlistForm(form({ email: "bot@example.com", company: "Acme" }));
    expect(result.ok && result.data.company).toBe("Acme");
  });
});
