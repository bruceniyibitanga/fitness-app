# The coach engine

How Levitx decides what you train. This is the design behind [ADR 0007](adr/0007-adaptive-coach.md). Keep it up to date as the engine changes.

The short version: you tell the app your goal, and it acts like a trainer. It plans every session, keeps them varied without being random, notices when you're flat or flying, and tells you why it changed something. You never have to work out which module to do today.

Nothing in here is final until a qualified coach has reviewed the numbers. Where a number is our own conservative choice rather than something the research gives us, it says so. Every number lives in one file, `CoachTuning.kt`, so that review is one file long.

**Where it stands:** the engine is built and tested in `:core` (package `app.levitx.core.coach`). It isn't wired into the phone or watch yet. Section 10 lists what is and isn't done.

## 1. What we ask, and what we work out

Every question costs the person something, so each one has to earn its place. The rule: if we can work something out safely from data we already have, and the science backs the inference, we don't ask.

| Input | How we get it | Why |
|---|---|---|
| Goal | Asked at setup, one tap | Nothing else tells us what they want. |
| Focused or all-round | Asked on the goal screen, defaults to all-round | Changes the whole plan shape. |
| Can you do a pull-up? | Asked at setup | Picks safe starting moves before the first session. Everything else about level comes from the starting check in session 1. |
| Training days | Asked at setup (defaults Mon, Wed, Fri) | For accountability: reminders and the week view. The plan itself doesn't depend on them (see "Rolling sessions"). |
| Time per session | Asked at setup, default 45 min | Then adjusted from what actually happens: if they finish in about 35 minutes four times running, the coach offers to trim. |
| Equipment | Only "pull-up bar?" at setup | Other kit is asked about when an exercise would use it ("Got rings? Tap to unlock"). |
| Bodyweight | Health Connect if allowed, otherwise an optional prompt before a session | See section 6. Never inferred. |
| Effort per set | Easy / Right / Hard, one tap, optional | Reps are prefilled with the target, so a normal set is one tap or none. |
| Pain | A one-tap check after sessions that loaded a sore area | Only asked when it could change something. |
| Age, sex, height | Not asked | They'd change very little in the plan and they're more personal data to hold. |

Experience level isn't asked. It is measured: the starting check, then every logged set, gives a better answer than a self-rating.

## 2. The model of the person

The coach keeps a small model of each person, updated after every session. It is plain data (`AthleteModel`), so it can be stored in the encrypted database, inspected and replayed in tests.

- **Where they are on each ladder.** One rung per movement: vertical pull, horizontal pull, vertical push, horizontal push, squat, hinge and core.
- **Each exercise they have trained.** The current target, their best set, how many times they've done it (which is how sure the coach is of the target), and the last four results.
- **Skill progress.** Their best at each gate, using the gates already in `:core`.
- **Recent sessions,** six weeks of them: hard sets per movement and which exercises. Recovery, the week's volume and the weekly limits are all read from this.
- **Pain.** The latest advice per sore area.
- **Bodyweight readings** (section 6).
- **The block** they're in and how far through it.
- **What they skip,** per exercise. This nudges exercise choice; it never overrides a hard rule.
- **How past suggestions went,** and when they last took an easier day.

The model holds health data (pain and bodyweight), so it stays on the phone.

### Effort ratings

Easy, Right and Hard are stored as 4, 2 and 0 reps in reserve, the measure the rest of `:core` already uses, so a tap and a typed number are the same data. Rating effort by reps left is an established way to adjust training load from day to day ([Zourdos et al. 2016](https://doi.org/10.1519/JSC.0000000000001049); [Helms et al. 2018](https://doi.org/10.3389/fphys.2018.00247)). Novices judge it less accurately than experienced lifters, so the first two sessions on any exercise place the person from reps alone, with a best-effort set. Ratings only start to move targets after that. A set with no rating still counts: progress then goes on reps.

## 3. The shape of the plan

### Focused or all-round

- **All-round (default):** the goal skill comes first in every session it can, then the rest of the body gets proper work, so the person gets stronger and fitter overall while chasing the skill.
- **Focused:** each session has one more set of skill work. Movements that support the goal keep their full dose (rows help pull-ups, so a pull-up plan keeps its rows). Everything else drops to two sets, enough to hold steady.

Which movements "support" a goal isn't a hand-written list. A movement supports the goal when its exercises also train one of the goal's movements, which the exercise catalogue already records.

With no skill goal (general fitness), every movement gets the same dose.

### The gate is the training for its movement

While someone is working on a gate, that gate is how its movement is trained. On a first pull-up plan, pulling up means the current gate (hangs, then scapular pulls, and so on), not a second pulling exercise alongside it. The other movements each have a ladder of exercises from easiest to hardest.

### Blocks

Training runs in blocks. The skill work and the main exercises stay the same within a block so they can actually progress. A lighter week ends the block, then accessory exercises rotate.

A block is counted in sessions, not calendar weeks: five weeks' worth at the person's planned sessions per week (15 sessions on a three-day plan), then a lighter week of the same number of sessions. Counting sessions means someone who trains once a week isn't sent into a lighter week they haven't earned. Two weeks or more away counts as the lighter week.

### Rolling sessions

The plan is a queue of sessions, not a calendar. The training days the person chose are for reminders and the week view. If they miss Wednesday, Friday's session doesn't disappear and nothing turns red: the next session is simply whatever comes next, adjusted for recovery. If two sessions land on back-to-back days, the second goes easy on what the first one trained.

## 4. Choosing a session

When the person opens Today, the coach builds the next session in three steps.

1. **Candidates.** It builds each session template (full body in three orders, upper, pull and legs, push and legs, lower and core) with the skill work in front and without it, all as hard work, fitted to the person's time and kit.
2. **Hard rules.** Each candidate is made safe. A rule that objects says what would fix it, and the fix is applied: the exercise goes light, or comes out. What is left is the nearest safe version of that candidate. If nothing is left, it's dropped.
3. **Scoring.** The safe candidates are scored and the best one is the session.

Making candidates safe, instead of only throwing unsafe ones away, means a sore elbow gives "legs as normal, upper body light" and not a random other session.

### The hard rules

- **Pain,** from the pain-monitoring guidance already in `:core`. Advice to ease off allows only light work on movements that load the area. Advice to see a professional takes them out of the plan.
- **Recovery.** No hard work on a movement within 36 hours of its last hard session. 36 hours works out as a rest day in between on any normal schedule (Monday evening, then Wednesday morning). A strict 48 would refuse that Wednesday session. Our choice, to review.
- **Weekly limit.** Hard sets of a movement's own exercises are capped at 20 a week. They can also only grow 20% past the busiest of the last four weeks, starting from an allowance of 15. Tendons adapt more slowly than muscle ([Mersmann et al. 2017](https://doi.org/10.3389/fphys.2017.00987)), so the limit exists to keep strength from running ahead of tendon tolerance. The values are our conservative choice, not numbers from the research. The allowance equals the biggest target a plan sets, so a new plan fits and extra work on top of it doesn't.
- **Gate order.** Only the gate the person is on is trained.

"This week" is the last six and a half days, not seven. With seven, the session on the same weekday last week would count as this week's whenever it happened later in the day than now.

### Scoring

Each safe candidate gets a score from 0 to 1, the weighted sum of six terms:

| Term | Weight | What it measures |
|---|---|---|
| Goal progress | 0.30 | Hard sets of skill work, plus half credit for other work on the goal's movements. |
| Volume gap | 0.30 | How much of the week's remaining target the session covers. Sets past a movement's target earn nothing. |
| Recovery | 0.15 | How rested the movements trained hard are, up to 72 hours. |
| Time fit | 0.15 | How close the session comes to the time the person has. |
| Preference | 0.06 | A nudge away from exercises they have skipped twice or more. |
| Novelty | 0.04 | How different the session is from the last one. |

Weekly targets count a set in full for its own movement and half for a movement it trains indirectly. Weekly volume has a dose-response with diminishing returns, steeper for strength than for size ([Pelland et al. 2026](https://sportrxiv.org/index.php/server/preprint/view/460)), which is why sets past the target score nothing. Targets are 12 hard sets a week for the goal's movements (15 in a focused plan) and 6 for the rest (3 for unrelated movements in a focused plan). They shrink together when the person's week is too short to fit them.

The highest score wins. Nothing is random: the same model at the same time always gives the same session, which makes it testable and lets a bug report be replayed.

### Progression inside a session

- **Skill work** keeps the shape it has today: work sets at about 70% of the gate's best with reps in reserve, then a best-effort set. A gate is passed only by a strict, unassisted set.
- **Ladder exercises** are three work sets at a target. Hit every set at an effort that isn't Hard and the target goes up a rep (five seconds for holds), or two when this session and the last were both Easy. At the top of the exercise's range the person moves up a rung. Two sessions in a row short of target ease it back a step.
- **Finding the level.** For the first two sessions on an exercise the last set is a best-effort set. It places the person directly: up a rung if they reach the top of the range (two rungs for double it), down one if they can't reach the bottom, otherwise work sets start at 70% of what they did. This is how the coach learns someone's level from training instead of asking.
- **Light work never changes a target.** Neither does anything done while weight is falling fast (section 6).
- **What counts toward the week is sets done, not sets planned.**

### Variation

Variation is useful when it's planned and harmful when it's random. A systematic review found that some planned variation can help, while swapping exercises too often, or for ones that train the same thing, may hold back gains ([Kassiano et al. 2022](https://doi.org/10.1519/JSC.0000000000004258)). So:

- **The skill and the goal's exercises don't change.** Skills need repetition.
- **Accessory exercises rotate between blocks,** within a rung of the ladder, so the swap is to something of the same difficulty that trains the same thing (split squats one block, reverse lunges the next).
- **Sessions vary through the template chosen:** which movements, in what order.
- **Novelty only breaks ties.** It has the smallest weight of the six terms.

## 5. Readiness and suggestions

The coach watches for signs that a person needs an easier day. Signals, from most to least trusted:

1. **Performance dropping.** The best work set on an exercise fell and stayed down for two sessions, by 15% or more. One bad day isn't a trend, and neither is a rep's wobble. Best-effort sets are left out so sessions with and without one compare fairly.
2. **Felt hard.** The last two sessions of an exercise were both rated Hard.
3. **Bodyweight falling fast** (section 6).
4. **A long gap.** Two weeks or more since the last session. A long gap means a comeback session, not a guilt message.
5. **The block's lighter week.**
6. **Sleep and resting heart rate from Health Connect.** Later, and only as a soft nudge. Training guided by heart rate variability has given at best small benefits over a fixed plan in meta-analyses ([Düking et al. 2021](https://doi.org/10.1016/j.jsams.2021.04.012); [Manresa-Rocamora et al. 2021](https://doi.org/10.3390/ijerph181910299)), so it never decides anything alone.

A long gap or a lighter week is enough alone. Otherwise it takes two different kinds of signal, about exercises in today's session, because any single one is too noisy to act on.

A suggestion is always offered with three choices: **Keep it**, **Go lighter**, **Rest today**. Nothing is skipped without the person saying so. Lighter means the same exercises with two sets each at 80% of the target and no best-effort set. Rest is the suggested choice when the last session was less than a day ago.

Pain isn't a signal here. It works through the hard rules and needs nobody's agreement.

The choice feeds back:

- **Taking the easier day answers the signals that led to it.** They don't ask again until a fresh result is still down. Without this, someone who took the lighter session would be offered it forever, because light work doesn't produce new results.
- **Someone who keeps choosing Keep and then hits their targets, three times running, is asked less often.** It then takes three kinds of signal, not two.

Going the other way, hitting targets at Easy twice running moves the target up twice as fast.

## 6. Bodyweight

Bodyweight matters to the coach in two ways: it sets the kg per rep for every bodyweight exercise, and a fast change in it can mean the person isn't recovering well.

### Getting it

- **Health Connect first.** If the person allows it, the app reads weight records from Health Connect (a smart scale or another app may write them). It asks for `READ_WEIGHT` only. Reads happen when the app is open, so it needs no background read permission. Health Connect lets an app read up to 30 days before permission was first granted, without the extra history permission ([read data guide](https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data)). That is plenty for a trend, so we don't ask for history either.
- **Otherwise, a quick prompt before a session.** Only when there's no reading in the last 7 days, at most once a week, prefilled with the last value, and skippable. A step of 0.1 kg, so it takes one or two taps.

### Using it

- Daily weight swings by a kilogram or more with water and food, so the coach uses a trend, never a single reading: a straight line fitted through the last three weeks of readings. It needs at least three readings over ten or more days before there is a trend at all.
- **Load per rep** always uses the latest trend value. If someone gains weight, their pull-up gets heavier in kg, and the model already works in kg, so targets stay sensible.
- **Fast loss.** Guidance for people training while losing weight is to lose about 0.5 to 1% of bodyweight a week to keep muscle and strength ([Ruiz-Castellano et al. 2021](https://doi.org/10.3390/nu13093255)). In one study, athletes losing 0.7% a week gained strength, while those aiming for 1.4% didn't ([Garthe et al. 2011](https://doi.org/10.1123/ijsnem.21.2.97)). So when the trend shows more than 1% a week across two weeks or more, the coach:
  - holds progression steady instead of adding work, and says why
  - if performance is also dropping or sessions feel hard, offers a lighter day
  - tells the person plainly once, with no judgement, and suggests talking to a GP if they aren't trying to lose weight

### What it never does

- It never praises weight loss, sets weight targets, or shows weight streaks. Levitx is a training app, and weight is an input to training, not a score.
- It never comments on how someone looks.
- Bodyweight stays on the phone, encrypted, and never goes to the watch (SECURITY.md).

## 7. Explaining itself

Every session comes with one plain sentence saying why. The coach picks it in this order:

1. A safety rule changed the session ("Working around your elbow today.").
2. Something changed since last time. The biggest change wins: a new exercise is explained ahead of one more rep.
3. The coach is still finding the person's level (their first two sessions).
4. Targets are being held for weight.
5. Whatever set this session apart from the runner-up: the scoring term where it led by most.

Examples:

- "Still learning your numbers. Targets are on the easy side for now."
- "Knee push-up: target up. You hit every set last time."
- "Reverse lunge: new this block. Same muscles, a fresh challenge."
- "More squatting today: you're 4 sets short this week."
- "Going easy on pulling today: it needs another day after your last session."

The engine returns the reason as data, and one place (`CoachPhrases`) turns it into English. So the sentence is always what the engine actually did, phone and watch say the same thing, and the wording can change or be translated without touching the logic.

## 8. Testing

- **Unit tests** for every hard rule, every scoring term, progression, blocks, readiness, the bodyweight trend and the wording.
- **A simulation** runs made-up people through the coach for twelve weeks. A simulated person has a real strength per movement, gains a little from each hard session, loses a little with time off, and does what that strength allows in each set. Personas:
  - a beginner at zero pull-ups, three days a week
  - someone who has stopped improving
  - a bad fortnight in the middle of a good run
  - someone who trains once a week on a three-day plan
  - a comeback after three weeks off
  - someone losing 1.5% of their bodyweight a week for five weeks
  - an elbow that flares for two weeks
  - five 20-minute sessions a week, a muscle-up plan, and a plan with no goal
- **Safety is checked from the sessions themselves,** not by asking the hard rules again, so a rule that is wrong can't vouch for itself: a rest day between hard sessions of a movement, the weekly ceiling, no hard work on a sore area, gates in order, sessions inside the person's time.
- **Behaviour checks:** the beginner passes one to three gates in twelve weeks and most ladders move up; a steady person is rarely asked to ease off; the bad fortnight is noticed; the once-a-week person always gets a session with the skill first and no lighter week; the comeback starts lighter and is back to normal the session after; no target rises while weight is falling fast and the notice is given once.

The simulation found three real faults while the engine was being written. Each has a test now:

- A best-effort set made the next normal session look like a drop in performance.
- Someone who took the lighter session was offered it again every session, forever.
- The weekly limit was lower than the plan's own weekly target.

A qualified coach still reviews sample twelve-week plans from each persona before release.

## 9. Not yet

- Meal plans and nutrition.
- Sleep and resting heart rate as readiness signals.
- Any language model in the loop.

Each gets its own decision record when we get there.

## 10. What is built and what isn't

Built, in `:core`:

- the exercise catalogue: one ladder per movement, 48 exercises
- the model of the person and how it learns from a session
- the four hard rules and the repair that makes a session safe
- the six scoring terms
- focused and all-round plans, blocks, lighter weeks, accessory rotation
- readiness signals and the Keep / Lighter / Rest suggestion
- the bodyweight trend and the fast-loss hold
- reasons and their wording
- the tests and simulation above

Not built yet:

- **Storing the model.** It is plain data, ready for the encrypted database, but there are no tables for it yet.
- **Running a session with more than one exercise.** `SessionRun` still runs one gate. The phone and watch session screens need a run that walks through a planned session's exercises.
- **Health Connect.** The engine takes weigh-ins; nothing reads them from Health Connect yet.
- **The screens.** Today, the suggestion card, the weigh-in and the week review are designed but not built.
- **Offering to trim sessions** that keep finishing early. The model records how long each session took; nothing acts on it yet.
- **More goals.** Only the first pull-up and muscle-up paths exist. Handstand, front lever, pistol squat and push-up paths need their gates written. The general goals (stronger, muscle, lean, mobility) all get the same even plan for now.
- **Conditioning and mobility.** The engine plans strength and skill work. Cardio and mobility sessions are in the Train tab design but not in the engine.

## References

- Zourdos MC et al. (2016). Novel resistance training-specific rating of perceived exertion scale measuring repetitions in reserve. J Strength Cond Res 30(1):267-275.
- Helms ER et al. (2018). RPE vs. percentage 1RM loading in periodized programs matched for sets and repetitions. Front Physiol 9:247.
- Pelland JC et al. (2026). The resistance-training dose response: meta-regressions exploring the effects of weekly volume and frequency on muscle hypertrophy and strength gains. Sports Med. Preprint on SportRxiv.
- Kassiano W et al. (2022). Does varying resistance exercises promote superior muscle hypertrophy and strength gains? A systematic review. J Strength Cond Res 36(6):1753-1762.
- Mersmann F, Bohm S, Arampatzis A (2017). Imbalances in the development of muscle and tendon as risk factor for tendinopathies in youth athletes. Front Physiol 8:987.
- Düking P et al. (2021). Monitoring and adapting endurance training on the basis of heart rate variability monitored by wearable technologies. J Sci Med Sport 24(11):1180-1192.
- Manresa-Rocamora A et al. (2021). Heart rate variability-guided training for enhancing cardiac-vagal modulation, aerobic fitness, and endurance performance. Int J Environ Res Public Health 18(19):10299.
- Ruiz-Castellano C et al. (2021). Achieving an optimal fat loss phase in resistance-trained athletes: a narrative review. Nutrients 13(9):3255.
- Garthe I et al. (2011). Effect of two different weight-loss rates on body composition and strength and power-related performance in elite athletes. Int J Sport Nutr Exerc Metab 21(2):97-104.
- Android Developers: [Health Connect, read data](https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data) and [data types and permissions](https://developer.android.com/guide/health-and-fitness/health-connect/plan/data-types).
