# 0009: A backup file the person encrypts with words only they hold

**Status:** Proposed, November 2026. Three choices below need the maintainer's yes before any code is written.

## Context

[ADR 0002](0002-local-first-encrypted-storage.md) turned automatic backup off and promised a password-protected export in its place. That half was approved and never built. Today a new phone, a lost phone or a factory reset loses everything.

It matters more than it looks. In the reviews of other calisthenics apps (see the review research in the project docs), losing history on a phone change is one of the things people leave over. For Levitx it is the one real cost of having no account and no server.

The export we have is a zip of JSON and CSV for reading in a spreadsheet. It is not encrypted and nothing can read it back in.

**What we are defending against.** A backup file ends up in Drive, in an email to yourself, on a USB stick. Anyone who gets the file can guess at it on their own hardware, as fast as they like, forever. The app can't slow them down or lock them out. So the file is exactly as strong as the secret that encrypts it, and that is the choice that matters most here.

## Decision (proposed)

**One file, made and read only by Levitx.** Settings > Your data gets "Back up" and "Restore". The plain export stays as it is, for people who want to read their data.

**What is in it.** The same document the export writes: plan, workouts, weigh-ins, kit, bands, settings and the coach's stored state. That includes bodyweight and which areas are sore, which is why it is encrypted.

**How it is encrypted.**
- AES-256-GCM from the platform's `javax.crypto`, the same cipher that already protects the database key. GCM also detects any change to the file.
- A fresh random salt and nonce for every backup, from `SecureRandom`. Each file gets its own key, so a nonce can never repeat under one key.
- The header (format version, how the key was made, the salt) is authenticated along with the contents. Nobody can swap in weaker settings and have the file still open.
- The key is made from the secret with a slow function. Which one is choice 2.

**Restore.**
- The person picks the file in the system's file picker, so no storage permission is needed.
- Anything over a size cap is refused before it is read into memory. The header must name settings this version of the app knows. A crafted file can't make the phone burn memory or minutes of CPU.
- The key is worked out off the main thread. A wrong secret and a damaged file give the same message, because GCM can't tell them apart and neither should we.
- Once open, every field is checked against fixed limits before it reaches the database, the way data from the watch already is. A file that fails is refused whole.
- Everything is written in one database transaction. A restore that is interrupted leaves the phone as it was.
- Restore is refused while a workout is in progress.
- A file from a newer Levitx is refused with "update first". Older ones are brought forward.

**Handling the secret.** It is never logged, never put in saved state, never sent anywhere, and the screens that show or take it are kept out of screenshots and the recent apps view. It is wiped from memory when the work is done. The database key doesn't change and never goes in the file.

## The three choices

**1. Who makes the secret?**
- **(a) Levitx makes it: six random words** from a fixed list of 7,776 (about 77 bits). Shown once to write down, and the person types two of them back to prove they did. *Recommended.*
- (b) The person types their own password.
- (c) Words by default, own password allowed.

People's own passwords are short and reused, and this file can be attacked offline for years. Six random words are out of reach of guessing however fast the hardware gets. The cost is real: lose the words and the backup is gone, and we can't help, because we hold nothing.

**2. How is the key made from the secret?**
- **(a) PBKDF2-HMAC-SHA256, 600,000 rounds** (the figure in OWASP's password storage guidance). In the platform, no new dependency. *Recommended if choice 1 is (a).*
- (b) Argon2id. Costs an attacker memory as well as time, so it holds up far better for weak passwords. Needs a new native or cryptography library, which is one more thing to review and keep patched.

With six random words the strength is in the words, and PBKDF2 is plenty. If own passwords are allowed (1b or 1c), Argon2id is the right answer and the dependency is worth it.

**3. What does restore do to data already on the phone?**
- **(a) Replace it, after asking.** *Recommended.*
- (b) Merge the two.

Merging two coach histories has no obviously right answer (which target wins, which block are you in), and a wrong merge quietly corrupts the plan. Restore is for a new or wiped phone. Replace is simple to explain and to test.

## Consequences

- A backup can sit in a cloud drive or an inbox without exposing health data.
- Lose the words, lose the backup. The screen has to say that plainly, before the file is made.
- Settings shows when the last backup was made. No nagging.
- If (1a): a bundled word list (about 60 KB). Its licence is checked and credited before it ships.
- Work: the file format and checks in `:data` (plain Kotlin, tested without a phone), two screens designed on the canvas first, and "Have a backup?" on the first setup screen.
- Tests: published test vectors for the key function, round trips, a wrong secret, a flipped bit in every part of the file, cut-short and oversized files, unknown versions, changed header settings, out-of-range and oversized values inside a file that opens, and a restore cancelled part way.
