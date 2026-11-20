import { describe, expect, it } from "vitest";
import { DEFAULT_THEME, THEME_COLOR, THEME_COOKIE, otherTheme, parseTheme, themeCookie } from "./theme";

describe("parseTheme", () => {
  it("accepts the two known themes", () => {
    expect(parseTheme("light")).toBe("light");
    expect(parseTheme("dark")).toBe("dark");
  });

  it("falls back to light for anything else", () => {
    expect(DEFAULT_THEME).toBe("light");
    for (const value of [undefined, null, "", "Dark", " dark", "dark;", "blue", '"><script>', "light\n"]) {
      expect(parseTheme(value)).toBe("light");
    }
  });
});

describe("otherTheme", () => {
  it("flips between the two", () => {
    expect(otherTheme("light")).toBe("dark");
    expect(otherTheme("dark")).toBe("light");
  });
});

describe("themeCookie", () => {
  it("is first-party, site-wide, lax and long-lived", () => {
    const cookie = themeCookie("dark", true);
    expect(cookie).toBe(`${THEME_COOKIE}=dark; Path=/; Max-Age=31536000; SameSite=Lax; Secure`);
  });

  it("drops Secure on plain http so local dev still works", () => {
    expect(themeCookie("light", false)).not.toContain("Secure");
  });
});

describe("THEME_COLOR", () => {
  it("has a colour for every theme", () => {
    expect(Object.keys(THEME_COLOR).sort()).toEqual(["dark", "light"]);
  });
});
