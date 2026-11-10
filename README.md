# LEVER website

Landing page for LEVER, an adaptive calisthenics app for iPhone and Apple Watch.

Next.js 16 (App Router), TypeScript, Tailwind CSS 4.

## Running it

```bash
npm install
cp .env.example .env.local   # optional, see below
npm run dev
```

Without Supabase keys, waitlist signups are logged to the terminal (masked) instead of saved. In production the app refuses to save signups until the keys are set, so nothing gets silently dropped.

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
| `src/lib/waitlist/` | Validation, rate limiting and storage for the waitlist. |
| `src/app/actions/waitlist.ts` | The server action the form posts to. |
| `src/proxy.ts`, `src/lib/csp.ts` | Content Security Policy with a fresh nonce per request. |
| `supabase/migrations/` | SQL for the waitlist table. |

## Security notes

- **CSP.** Scripts only run with the request's nonce. That means pages render per request instead of being static. For a landing page that's a fair trade.
- **Headers.** HSTS, `nosniff`, `X-Frame-Options: DENY`, a strict referrer policy and a locked-down permissions policy are set in `next.config.ts`.
- **Waitlist.** It's a server action, so Next.js rejects cross-origin posts. Input is validated with zod. There's a honeypot field and a per-IP rate limit. Errors shown to people are generic, and duplicates get the same response as new signups, so nobody can check who's on the list.
- **Database.** The Supabase service role key is server-only (`import "server-only"`). The table has RLS on with no policies, so the public anon key can't touch it.
- **No third parties in the browser.** Fonts are self-hosted, and Unsplash photos go through `next/image`, so the browser only ever talks to this origin.

Known limit: the rate limiter keeps counts in memory, so each serverless instance counts on its own. Swap in Upstash Redis or a Supabase table (anything implementing `RateLimiter`) once real traffic arrives.

## Supabase setup

1. Run `supabase/migrations/0001_waitlist.sql` in the SQL editor.
2. Set `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` in your host's environment variables. Never prefix them with `NEXT_PUBLIC_`.
3. Set `NEXT_PUBLIC_SITE_URL` to the real domain so canonical links and the sitemap are right.

## Photos

Photos are hotlinked from Unsplash, as their guidelines ask, with credits in the footer. To swap one, change its URL in `src/content/photos.ts`.
