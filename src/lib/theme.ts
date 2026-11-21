/**
 * Light and dark themes for the website.
 *
 * The choice lives in one small first-party cookie. The server reads it while rendering,
 * so the page arrives in the right theme with no flash and no inline script (which the
 * CSP would block anyway). The cookie only ever holds "light" or "dark": anything else is
 * ignored and the default is used, so a tampered cookie can't inject markup or styles.
 */
export const THEMES = ["light", "dark"] as const;
export type Theme = (typeof THEMES)[number];

/** Light by default, whatever the device prefers. */
export const DEFAULT_THEME: Theme = "light";
export const THEME_COOKIE = "levitx-theme";

/** One year. The cookie is a preference, not an identifier, so a long life is fine. */
const MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

/** Browser chrome colour per theme, matching --color-bg in globals.css. */
export const THEME_COLOR: Record<Theme, string> = {
  light: "#f3f0ea",
  dark: "#0b0b0a",
};

/** Turns any cookie value into a known theme. Unknown or missing values give the default. */
export function parseTheme(value: string | undefined | null): Theme {
  return value === "light" || value === "dark" ? value : DEFAULT_THEME;
}

export function otherTheme(theme: Theme): Theme {
  return theme === "light" ? "dark" : "light";
}

/**
 * The Set-Cookie string for a theme. Lax keeps it off cross-site requests, and Secure is
 * added on HTTPS (it's left off on http://localhost so the switch still works in dev).
 */
export function themeCookie(theme: Theme, secure: boolean): string {
  const parts = [`${THEME_COOKIE}=${theme}`, "Path=/", `Max-Age=${MAX_AGE_SECONDS}`, "SameSite=Lax"];
  if (secure) parts.push("Secure");
  return parts.join("; ");
}
