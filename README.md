# LEVER website

Landing page for LEVER, an adaptive calisthenics app for iPhone and Apple Watch.

Next.js 16 (App Router), TypeScript, Tailwind CSS 4.

## Running it

```bash
npm install
cp .env.example .env.local   # optional, see below
npm run dev
```

Without a Plunk key, waitlist signups are logged to the terminal (masked) instead of saved. In production the form shows an error until the key is set, so nothing gets silently dropped.

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
| `src/lib/waitlist/` | Validation, rate limiting and storage (Plunk, or Supabase later) for the waitlist. |
| `src/app/actions/waitlist.ts` | The server action the form posts to. |
| `src/proxy.ts`, `src/lib/csp.ts` | Content Security Policy with a fresh nonce per request. |
| `supabase/migrations/` | SQL for a waitlist table, if you move off Plunk. |

## Security notes

- **CSP.** Scripts only run with the request's nonce. That means pages render per request instead of being static. For a landing page that's a fair trade.
- **Headers.** HSTS, `nosniff`, `X-Frame-Options: DENY`, a strict referrer policy and a locked-down permissions policy are set in `next.config.ts`.
- **Waitlist.** It's a server action, so Next.js rejects cross-origin posts. Input is validated with zod. There's a honeypot field and a per-IP rate limit. Errors shown to people are generic, and duplicates get the same response as new signups, so nobody can check who's on the list.
- **Storage keys.** `PLUNK_SECRET_KEY` (and the Supabase keys, if you switch later) are server-only (`import "server-only"`) and never reach the browser.
- **No third parties in the browser.** Fonts are self-hosted, and Unsplash photos go through `next/image`, so the browser only ever talks to this origin.

Known limit: the rate limiter keeps counts in memory, so each serverless instance counts on its own. Swap in Upstash Redis or a Supabase table (anything implementing `RateLimiter`) once real traffic arrives.

## Waitlist setup (Plunk, free)

Signups go to [Plunk](https://www.useplunk.com). It's open source, the free tier covers 1,000 emails a month, and it handles unsubscribes and the launch email, so there's no database to pay for.

1. Create a Plunk account and a project for Levitx.
2. In **Settings > API Keys**, copy the **secret** key (starts with `sk_`). The public `pk_` key won't work: the contacts endpoint only accepts the secret one, and the site checks the prefix at startup.
3. In Vercel, add it as `PLUNK_SECRET_KEY`, marked **Sensitive**, then redeploy.
4. In Plunk, make a segment where `waitlist` is `true`. That's your launch list.
5. Set `NEXT_PUBLIC_SITE_URL` to the real domain so canonical links and the sitemap are right.

Before sending the launch email, verify levitx.app in Plunk (**Guides > Verifying domains** in their docs) so mail comes from your domain and doesn't land in spam.

Moving to a database later is a config change. A Supabase version and its migration (`supabase/migrations/`) are already there: set `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` and remove the Plunk key. Any other store is one class that implements `WaitlistRepository`.

## Photos

Photos are hotlinked from Unsplash, as their guidelines ask, with credits in the footer. To swap one, change its URL in `src/content/photos.ts`.
