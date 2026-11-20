"use client";

import { useCallback } from "react";
import { THEME_COLOR, otherTheme, parseTheme, themeCookie } from "@/lib/theme";

/**
 * Switches between light and dark. The server already rendered the right theme from the
 * cookie, so this only flips the attribute on <html>, updates the browser chrome colour
 * and saves the choice. Both icons are in the markup and CSS shows the right one, so the
 * server and client render the same thing and there's nothing to hydrate differently.
 */
export function ThemeToggle() {
  const toggle = useCallback(() => {
    const root = document.documentElement;
    const next = otherTheme(parseTheme(root.dataset.theme));
    root.dataset.theme = next;
    root.style.colorScheme = next;
    document.querySelector('meta[name="theme-color"]')?.setAttribute("content", THEME_COLOR[next]);
    document.cookie = themeCookie(next, window.location.protocol === "https:");
  }, []);

  return (
    <button
      type="button"
      onClick={toggle}
      className="theme-toggle inline-flex size-11 cursor-pointer items-center justify-center text-soft transition-colors hover:text-text"
    >
      <span className="sr-only theme-label-light">Switch to dark theme</span>
      <span className="sr-only theme-label-dark">Switch to light theme</span>
      {/* Moon, shown in light mode. */}
      <svg className="theme-icon-light" width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
        <path d="M14.5 11.2A6.5 6.5 0 0 1 6.8 3.5a6.5 6.5 0 1 0 7.7 7.7Z" fill="none" stroke="currentColor" strokeWidth="1.4" />
      </svg>
      {/* Sun, shown in dark mode. */}
      <svg className="theme-icon-dark" width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
        <circle cx="9" cy="9" r="3.4" fill="none" stroke="currentColor" strokeWidth="1.4" />
        <path d="M9 1.5v2M9 14.5v2M1.5 9h2M14.5 9h2M3.7 3.7l1.4 1.4M12.9 12.9l1.4 1.4M3.7 14.3l1.4-1.4M12.9 5.1l1.4-1.4" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
      </svg>
    </button>
  );
}
