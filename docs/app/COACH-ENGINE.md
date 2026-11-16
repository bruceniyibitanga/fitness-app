# The coach engine

How Levitx decides what you train. This is the design behind [ADR 0007](adr/0007-adaptive-coach.md). Keep it up to date as the engine changes.

The short version: you tell the app your goal, and it acts like a trainer. It plans every session, keeps them varied without being random, notices when you're flat or flying, and tells you why it changed something. You never have to work out which module to do today.

Nothing in here is final until a qualified coach has reviewed the numbers. Where a number is our own conservative choice rather than something the research gives us, it says so. Every number lives in one file, `CoachTuning.kt`, so that review is one file long.

**Where it stands:** the engine is built and tested in `:core` (package `app.levitx.core.coach`) and runs the phone app. The watch runs the sessions it plans. Section 10 lists what is and isn't done, and section 11 lists where every exercise and number comes from.

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

One rule sits above the scoring: when the goal's skill work can be done safely today, the session has it. Candidates without it are only scored when none with it survived step 2. Without this rule a person whose goal was a pull-up could get a session with no pulling in it at all, because a session without the skill work sometimes scored a little higher on balance.

### Kit

Nobody is assumed to own anything. A person with no kit trains on what every home has: the floor, a wall and a chair. Each exercise lists the kit it needs (a pull-up bar, a low bar, dip bars, rings, a band, or a box or step of a set height), and the ladders skip any rung the person can't do with what they have, up and down. If the goal itself needs kit they haven't added, Today says what is missing and where to add it ("Pull-up work needs a pull-up bar. Add it under Body to start it.") and the plan trains everything else in the meantime.

Making candidates safe, instead of only throwing unsafe ones away, means a sore elbow gives "legs as normal, upper body light" and not a random other session.

### The hard rules

- **Pain,** from the pain-monitoring guidance already in `:core`. Advice to ease off allows only light work on movements that load the area. Advice to see a professional takes them out of the plan.
- **Recovery.** No hard work on a movement within 36 hours of its last hard session. 36 hours works out as a rest day in between on any normal schedule (Monday evening, then Wednesday morning). A strict 48 would refuse that Wednesday session. General guidance is 48 hours between sessions for the same muscles ([Garber et al. 2011](https://doi.org/10.1249/MSS.0b013e318213fefb)), but that assumes hard sets. Ours stop 2 or more reps short of failure, and sets stopped well short of failure have been shown to recover within 24 to 48 hours, where sets to failure took longer ([Morán-Navarro et al. 2017](https://doi.org/10.1007/s00421-017-3725-7)). So 36 is our choice, with a reason, and still one to review.
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
- **Timed sets are counted by the timer, then checked by the person.** A hold counts whole seconds from the end of a 3 second count-in until the person taps to stop. Slow negatives are paced by sound, since nobody can tap a phone while hanging from a bar, and count only the reps fully lowered. Either way the count fills in the set, and the person corrects it before logging, so the coach learns from what they say they did.

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
2. The goal needs kit the person hasn't added.
3. Something changed since last time. The biggest change wins: a new exercise is explained ahead of one more rep.
4. The coach is still finding the person's level (their first two sessions).
5. Targets are being held for weight.
6. Whatever set this session apart from the runner-up: the scoring term where it led by most.

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

- the exercise catalogue: one ladder per movement, 49 exercises, each checked against a source (section 11)
- the model of the person and how it learns from a session
- the four hard rules and the repair that makes a session safe
- the six scoring terms
- focused and all-round plans, blocks, lighter weeks, accessory rotation
- readiness signals and the Keep / Lighter / Rest suggestion
- the bodyweight trend and the fast-loss hold
- reasons and their wording
- the tests and simulation above
- `WorkoutRun`, which runs a planned session exercise by exercise
- workouts picked from the Train tab (`Coach.workout`), built and made safe the same way as the coach's own sessions
- form cues and set-up notes for every exercise (`ExerciseGuides`)
- storage in the encrypted database (`CoachRepository`), with the workout in progress saved after every set
- the watch: the phone sends it the next session, the watch runs it with `WorkoutRun`, and the finished workout comes back to the coach's history like one done on the phone

Not built yet:

- **Health Connect.** The engine takes weigh-ins; nothing reads them from Health Connect yet.
- **Some screens.** Setup, Today, the suggestion card, the workout, Skills and the Train library are built on the phone. The weigh-in prompt and the week review are designed but not built.
- **Offering to trim sessions** that keep finishing early. The model records how long each session took; nothing acts on it yet.
- **More goals.** Only the first pull-up and muscle-up paths exist. Handstand, front lever, pistol squat and push-up paths need their gates written. The general goals (stronger, muscle, lean, mobility) all get the same even plan for now.
- **Conditioning and mobility.** The engine plans strength and skill work. Cardio and mobility sessions are in the Train tab design but not in the engine.

## 11. Where the exercises and numbers come from

Every exercise in the catalogue and every number the coach decides with was checked against an outside source in November 2026. This section is the record of that check. When an exercise or a number changes, update its row here in the same pull request.

### How it was checked

- **Is it a real, standard exercise?** Each exercise was matched by name in two open exercise datasets: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (public domain, 876 entries) and the exercise data behind [wger](https://github.com/wger-project/wger) (872 English entries, Creative Commons per entry). Only the names were compared. Nothing was copied from either, so neither licence applies to our code.
- **Is it on the right rung?** Ladder order was checked against the [r/bodyweightfitness Recommended Routine](https://www.reddit.com/r/bodyweightfitness/wiki/kb/recommended_routine/) (the RR below), which is the most widely used published bodyweight progression, and against measured loads where they exist.
- **Do the numbers have a reason?** Each rule was matched to a study, a position stand or, where neither exists, marked as our own choice.

The datasets were useful for names and kit, and not much else. Their difficulty labels are rough: free-exercise-db calls a pull-up "beginner" and a hanging leg raise "expert", and files a chair squat under "machine". So no rung was decided from a dataset label.

### The ladders

| Movement | Our order, easiest first | Checked against |
|---|---|---|
| Vertical pull | dead hang, scapular pull-up, negative or band-assisted pull-up, pull-up or chin-up, chest-to-bar or wide pull-up | RR pull-up progression: scapular pulls, arch hangs, negatives, pull-ups. Chest-to-bar is the RR wiki's prerequisite for a muscle-up. |
| Horizontal pull | rows with knees bent, rows with straight body, rows with feet raised | RR row progression goes from steep to flat body angle. A steeper body is easier. |
| Vertical push | pike push-up with hands raised, pike push-up, feet-raised pike or negative dip, wall handstand push-up negative or dip | RR dip progression: support hold, negative dips, dips. Pike to handstand push-up is common coaching practice; we found no study ranking it. |
| Horizontal push | wall, hands on 61 cm box, knees, hands on 30 cm box, floor, feet on 30 cm box, feet on 61 cm box | Force-plate loads of 41, 49, 55, 64, 70 and 74% of bodyweight for the six box and floor positions (Ebben et al. 2011). Our order matches the measured order exactly. The wall push-up is a gentler start below all of them, so someone with no kit has a first rung. |
| Squat | squat to a chair, squat, split squat or reverse lunge, Bulgarian split squat or high step-up, pistol to a chair, pistol | RR squat progression: assisted squat, squat, Bulgarian split squat, then shrimp squats. Our split squat rung is a smaller step between squat and Bulgarian, and we use pistols at the top instead of shrimps. |
| Hinge | glute bridge, single-leg bridge or hip thrust, single-leg deadlift, sliding leg curl, Nordic curl negative | RR hinge progression: Romanian deadlift, single-leg deadlift, then Nordic curl work, with floor slides as its alternative bridge between the two. Bridges are an easier start we added in place of the Romanian deadlift. |
| Core | plank or dead bug, hollow hold or lying leg raise, hanging knee raise or tuck L-sit, hanging leg raise | RR core work (anti-extension), and free-exercise-db's ranking of lying leg raises below hanging ones. |

### The exercises

A dash means the dataset has no entry for it. Every exercise is in at least one dataset or in the RR.

| Exercise | free-exercise-db | wger | Note |
|---|---|---|---|
| Dead hang | - | Deadhang | |
| Scapular pull-up | Scapular_Pull-Up | - | |
| Slow negative pull-up | - | - | RR level 3 |
| Band-assisted pull-up | Band_Assisted_Pull-Up | Assisted Pull-Up | |
| Pull-up | Pullups | Pull-ups | |
| Chin-up | Chin-Up | Chin-ups | |
| Chest-to-bar pull-up | - | - | RR muscle-up prerequisite |
| Wide-grip pull-up | - | Pull-Ups (Wide Grip) | |
| Australian rows (3 angles) | Inverted_Row | Inverted Rows | |
| Ring rows (3 angles) | Inverted_Row_with_Straps | - | |
| Seated band row | - | - | **No source found.** A common easy row; flagged for the coach review. |
| Pike push-ups (3 heights) | - | Pike Push Ups | |
| Slow negative dip | - | - | RR dip level 2. New in this check. |
| Dip | Parallel_Bar_Dip | Dips | |
| Wall handstand push-up negative | Handstand_Push-Ups | Handstand Push Up | The negative half of the listed exercise. |
| Wall push-up | - | - | The no-kit start of the push-up ladder. Not measured, so the app shows no load per rep for it. |
| Push-ups, hands raised (2 heights) | Incline_Push-Up | Incline Push up | Load measured by Ebben et al. Needs a box or step. |
| Knee push-up | - | knee push-ups | Load measured by Ebben et al. |
| Push-up | Pushups | Push-Up | Load measured by Ebben et al. |
| Close-grip push-up | Push-Ups_-_Close_Triceps_Position | Diamond push ups | Kept on the push-up rung; see open points. |
| Push-ups, feet raised (2 heights) | Push-Ups_With_Feet_Elevated | Decline Pushups | Load measured by Ebben et al. |
| Squat to a chair | Chair_Squat | - | |
| Bodyweight squat | Bodyweight_Squat | Bodyweight Squat HD | |
| Split squat | - | - | Bodyweight version of both datasets' dumbbell split squat |
| Reverse lunge | - | Reverse lunges | |
| Bulgarian split squat | - | Bulgarian split squats left | RR squat level |
| High step-up | - | Step-ups | Needs a box or step. |
| Pistol squat to a chair | Single-Leg_High_Box_Squat | - | |
| Pistol squat | - | Pistol Squat | |
| Glute bridge | Butt_Lift_Bridge | Glute Bridge | |
| Single-leg glute bridge | Single_Leg_Glute_Bridge | Single Leg Glute Bridge | |
| Hip thrust, shoulders raised | Barbell_Hip_Thrust | Hip Thrust | Bodyweight version |
| Single-leg deadlift | - | Romanian deadlift, single leg | RR hinge level |
| Sliding leg curl | Platform_Hamstring_Slides | - | RR's floor slide alternative |
| Nordic curl negative | Natural_Glute_Ham_Raise | Nordic Curl | RR hinge level |
| Plank | Plank | Plank | |
| Dead bug | Dead_Bug | - | |
| Hollow body hold | - | Hollow Hold | |
| Lying leg raise | Flat_Bench_Lying_Leg_Raise | Lying Leg Raise | |
| Hanging knee raise | Knee_Hip_Raise_On_Parallel_Bars | Knee Raises | |
| Tuck L-sit hold | - | Tuck L-sit | |
| Hanging leg raise | Hanging_Leg_Raise | Hanging Leg Raises | |

### The numbers

| Rule | Value | Source | Status |
|---|---|---|---|
| Sets per exercise | 3 | RR uses 3 sets throughout; ACSM recommends multiple sets for most adults ([ACSM 2009](https://doi.org/10.1249/MSS.0b013e3181915670)) | Backed |
| Rep ranges | 3 to 20 depending on the exercise | Muscle grows across a wide range of reps; strength favours the heavier end ([Schoenfeld et al. 2021](https://doi.org/10.3390/sports9020032)). The RR uses 5 to 8 for most work and 10 to 30 seconds for holds. Our harder pulls sit near 3 to 8 and easier leg work higher, which fits both. | Backed |
| Move up a rung | top of the range on every work set | Same rule as the RR (3 sets of 8, then the next progression) | Backed |
| Reps left in the tank on work sets | 2 | Strength gains are similar across a wide range of reps in reserve; muscle growth improves closer to failure ([Robinson et al. 2024](https://doi.org/10.1007/s40279-024-02069-2)). 2 is a middle ground that also recovers faster (below). | Backed, a choice within the evidence |
| Rest between sets | 120 s for reps, 90 s for holds | ACSM suggests 1 to 2 minutes for growth and longer for heavy strength work; a 2024 meta-analysis found a small benefit over 60 s and no clear difference past 90 s ([Singer et al. 2024](https://doi.org/10.3389/fspor.2024.1429789)). The RR uses 90 s. | Backed |
| Gap between hard sessions of a movement | 36 h | See section 4: shorter than the usual 48 h guidance, justified by sets stopping short of failure ([Morán-Navarro et al. 2017](https://doi.org/10.1007/s00421-017-3725-7)) | Our choice, with a reason |
| Sessions a week | the person's choice, 1 to 7 | ACSM: 2 to 3 days a week for novices. RR: 3 days with a rest day between. | Backed for 2 to 3. More is allowed because the recovery rule spreads the work, so no movement is trained hard on back-to-back days. |
| Weekly hard sets | 12 for goal movements, 6 for the rest, 20 at most | Dose-response up to 10 or more sets a week per muscle ([Schoenfeld et al. 2017](https://doi.org/10.1080/02640414.2016.1210197)), with diminishing returns ([Pelland et al. 2026](https://sportrxiv.org/index.php/server/preprint/view/460)) | Targets backed; the cap is our choice (section 4) |
| Indirect sets count as | half | Pelland et al. 2026 | Backed |
| Push-up loads | 41 to 74% of bodyweight | Ebben et al. 2011, measured on a force plate | Measured |
| Pull-up and dip loads | 95% of bodyweight, plus any belt in full | Everything but the forearms and hands moves. Forearm and hand are about 2.2% of body mass per arm in men and 1.9% in women ([de Leva 1996](https://doi.org/10.1016/0021-9290(95)00178-6)). | Estimate from published segment masses. Changed in this check (was 100%). |
| Fast weight loss | over 1% a week | Ruiz-Castellano et al. 2021; Garthe et al. 2011 (section 6) | Backed |
| Lowering time on slow negatives | 5 s a rep | Coaching write-ups on beginner negatives agree on 3 to 5 seconds down. We take the top of that, and the phone's timer paces each rep to it (4 s to get back to the top, then 5 s down). No study sets the number. | Our choice, to review |
| Effort taps | Easy 4, Right 2, Hard 0 reps in reserve | Zourdos et al. 2016; Helms et al. 2018 (section 2) | Backed |
| Skill gate targets | e.g. 30 s hang, 10 pull-ups and 10 dips before a muscle-up | Coaching practice: the RR wiki lists chest-to-bar pull-ups and deep dips as muscle-up prerequisites. No study sets the exact numbers. | Our choice, to review |
| Block length, signals for a suggestion, scoring weights | see `CoachTuning` | No study gives these | Our choice, to review |

### What this check changed

- **Dips moved up a rung, and slow negative dips took their old place.** Before, someone could be handed a full dip as soon as they topped out pike push-ups. The RR's dip progression has negatives first, so now they do too. Full dips sit with the wall handstand push-up negative. No rung moved, so stored progress still points at the right place.
- **Pull-ups and dips now count 95% of bodyweight, not 100%.** The hands and forearms stay on the bar. A belt or vest still counts in full on these, because everything worn on the body is lifted. On push-ups a vest still counts at the push-up's share, because the hands carry part of it.

### Still open for the coach review

- **Seated band row** has no source. It stays as the row option for people with only a band, because otherwise they'd have no horizontal pull at all.
- **Close-grip push-up** is a bit harder than a regular push-up, and the RR puts diamond push-ups one level up. It stays as an alternative on the push-up rung for now, because its target is learned separately, so a harder exercise just gets a lower target. Moving it to its own rung would shift everyone's stored rung, which needs a migration.
- **Nordic curl negatives** need something to hook the feet under, like a heavy couch. The kit list doesn't have that, so the exercise guide says it.
- **Tuck L-sit on the floor** is much harder than on dip bars or parallettes. The exercise guide says so; asking about the kit would be better.
- **Plank and hollow hold ranges** (20 to 45 s and 15 to 40 s) aren't from the RR, which uses 10 to 30 s for holds. They're reasonable, but unsourced.

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
- Ebben WP et al. (2011). Kinetic analysis of several variations of push-ups. J Strength Cond Res 25(10):2891-2894.
- de Leva P (1996). Adjustments to Zatsiorsky-Seluyanov's segment inertia parameters. J Biomech 29(9):1223-1230.
- Morán-Navarro R et al. (2017). Time course of recovery following resistance training leading or not to failure. Eur J Appl Physiol 117(12):2387-2399.
- Garber CE et al. (2011). Quantity and quality of exercise for developing and maintaining cardiorespiratory, musculoskeletal, and neuromotor fitness in apparently healthy adults. Med Sci Sports Exerc 43(7):1334-1359.
- American College of Sports Medicine (2009). Progression models in resistance training for healthy adults. Med Sci Sports Exerc 41(3):687-708.
- Schoenfeld BJ, Grgic J, Van Every DW, Plotkin DL (2021). Loading recommendations for muscle strength, hypertrophy, and local endurance: a re-examination of the repetition continuum. Sports 9(2):32.
- Schoenfeld BJ, Ogborn D, Krieger JW (2017). Dose-response relationship between weekly resistance training volume and increases in muscle mass: a systematic review and meta-analysis. J Sports Sci. doi:10.1080/02640414.2016.1210197.
- Robinson ZP et al. (2024). Exploring the dose-response relationship between estimated resistance training proximity to failure, strength gain, and muscle hypertrophy: a series of meta-regressions. Sports Med 54(9):2209-2231.
- Singer A et al. (2024). Give it a rest: a systematic review with Bayesian meta-analysis on the effect of inter-set rest interval duration on muscle hypertrophy. Front Sports Act Living 6:1429789.
- r/bodyweightfitness. [Recommended Routine](https://www.reddit.com/r/bodyweightfitness/wiki/kb/recommended_routine/) and [muscle-up](https://www.reddit.com/r/bodyweightfitness/wiki/exercises/muscle-up/) wiki pages.
- Datasets checked by name: [free-exercise-db](https://github.com/yuhonas/free-exercise-db) (Unlicense) and [wger](https://github.com/wger-project/wger) exercise data (Creative Commons per entry).
