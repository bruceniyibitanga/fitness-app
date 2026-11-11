# 0007: An adaptive coach plans every session, with explicit safety rules underneath

**Status:** Accepted, November 2026. The engine is built in `:core`. The numbers in it need review by a qualified coach before release, like the gates.

## Context

Testing the Phase 2 and 3 builds showed the app felt like a set of screens, not a trainer. People had to decide what to do each day. We want the opposite: the person picks a goal, and the app works out every session, keeps it varied, notices when they're flat or progressing fast, and says why it changed something.

Two things pull against each other:

1. It has to feel intelligent and personal, not like a chain of if/else checks.
2. It handles health data and gives training advice, so it has to be predictable, safe and testable. A plan nobody can reproduce can't be checked.

## Decision

**The coach is a deterministic engine in `:core`, in three layers.**

1. **Hard rules.** Pain rules, weekly load limits, recovery spacing and skill gate order. Written out plainly, unit tested, never traded off against anything else. A session that breaks one is eased or cut until it doesn't.
2. **A model of the person** that updates after every set: what they can do per movement, how sure we are, recent fatigue, how often they turn up, what they swap or skip, and their bodyweight trend.
3. **Scoring.** Each session is chosen by building a few candidates that pass the hard rules and scoring them against goal progress, weekly volume, recovery, time, equipment, novelty and preference. Same inputs, same session.

Every change the coach makes carries a short reason the person can read.

Changes to the plan that the person would notice (a lighter day, a rest day, a lighter week) are offered, not imposed. The person picks Keep, Lighter or Rest, and that choice feeds back into the model.

The full design is in [COACH-ENGINE.md](../COACH-ENGINE.md).

## Alternatives considered

- **A language model writes the plan.** It would read well, but it can't be reproduced or tested, and it can invent numbers. It may later help word explanations or answer questions. It doesn't choose sets and reps.
- **Fixed programs with a few branches.** Easy to test, but it is the if/else feel we are trying to get away from, and it can't respond to the person.
- **A trained machine-learning model.** We have no data to train it on, and we couldn't explain its choices. Worth looking at once there are enough consenting users, if ever.

## Consequences

- The coach is the most important code in the app, and it gets the most tests: one per scoring term, plus a simulation that runs made-up people through 12 weeks (see COACH-ENGINE.md).
- For the first two or three weeks the model knows little about a person. The coach starts conservative and says so.
- Bodyweight from Health Connect means a new permission (`READ_WEIGHT` only) and a Play Console declaration. It stays on the phone, encrypted, and never goes to the watch, as SECURITY.md already requires.
- The phone app's screens are rebuilt around this (Today shows the coach's reason; Train becomes the off-plan library).
