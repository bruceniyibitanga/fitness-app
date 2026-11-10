# LEVER website

Landing page for LEVER, an adaptive calisthenics app for iPhone and Apple Watch.

Next.js 16 (App Router), TypeScript, Tailwind CSS 4.

## Running it

```bash
npm install
cp .env.example .env.local   # optional, see below
npm run dev
```

Without a Kit key, waitlist signups are logged to the terminal (masked) instead of saved. In production the form shows an error until the key is set, so nothing gets silently dropped.

```bash
npm run check   # typecheck, lint, tests
npm run build
```

## Where things live

| Path | What's in it |
|---|---|
| `src/content/site.ts` | All page copy. Change wording here, not in components. |
| `src/content/photos.ts` | Every photo and its credit. |
| `src/components/sections/` | One file per page section. |
| `src/components/ui/` | Small shared pieces (section heading, photo). |
| `src/lib/pushup-load.ts` | Push-up load maths, from force-plate studies. |
| `src/lib/waitlist/` | Validation, rate limiting and storage (Kit, or Supabase later) for the waitlist. |
| `src/app/actions/waitlist.ts` | The server action the form posts to. |
| `src/proxy.ts`, `src/lib/csp.ts` | Content Security Policy with a fresh nonce per request. |
| `supabase/migrations/` | SQL for a waitlist table, if you move off Kit. |

## Security notes

- **CSP.** Scripts only run with the request's nonce. That means pages render per request instead of being static. For a landing page that's a fair trade.
- **Headers.** HSTS, `nosniff`, `X-Frame-Options: DENY`, a strict referrer policy and a locked-down permissions policy are set in `next.config.ts`.
- **Waitlist.** It's a server action, so Next.js rejects cross-origin posts. Input is validated with zod. There's a honeypot field and a per-IP rate limit. Errors shown to people are generic, and duplicates get the same response as new signups, so nobody can check who's on the list.
- **Storage keys.** `KIT_API_KEY` (and the Supabase keys, if you switch later) are server-only (`import "server-only"`) and never reach the browser.
- **No third parties in the browser.** Fonts are self-hosted, and Unsplash photos go through `next/image`, so the browser only ever talks to this origin.

Known limit: the rate limiter keeps counts in memory, so each serverless instance counts on its own. Swap in Upstash Redis or a Supabase table (anything implementing `RateLimiter`) once real traffic arrives.

## Waitlist setup (Kit, free)

Signups go to [Kit](https://kit.com). The free plan covers up to 10,000 subscribers and handles unsubscribes and the launch email, so there's no database to pay for.

1. Create a free Kit account.
2. Optional: make a tag called `lever-waitlist` and copy its number from the tag's URL.
3. Copy your v4 API key from Settings > Developer.
4. In Vercel, add `KIT_API_KEY` (and `KIT_TAG_ID` if you made the tag) as **Sensitive** environment variables, then redeploy.
5. Set `NEXT_PUBLIC_SITE_URL` to the real domain so canonical links and the sitemap are right.

Moving to a database later is a config change. A Supabase version and its migration (`supabase/migrations/`) are already there: set `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` and remove the Kit key. Any other store is one class that implements `WaitlistRepository`.

## Photos

Photos are hotlinked from Unsplash, as their guidelines ask, with credits in the footer. To swap one, change its URL in `src/content/photos.ts`.
