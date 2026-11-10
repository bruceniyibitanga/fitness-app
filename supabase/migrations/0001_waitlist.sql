-- Early access waitlist.
-- Only the server writes here, using the service role key. Row level security is on
-- with no policies, so the public anon key can't read or write this table.

create table if not exists public.waitlist (
  id bigint generated always as identity primary key,
  email text not null unique check (char_length(email) <= 254 and email = lower(email)),
  created_at timestamptz not null default now()
);

alter table public.waitlist enable row level security;

revoke all on table public.waitlist from anon, authenticated;
