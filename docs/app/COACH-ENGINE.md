# The coach engine

How Levitx decides what you train. This is the design behind [ADR 0007](adr/0007-adaptive-coach.md). Keep it up to date as the engine changes.

The short version: you tell the app your goal, and it acts like a trainer. It plans every session, keeps them varied without being random, notices when you're flat or flying, and tells you why it changed something. You never have to work out which module to do today.

Nothing in here is final until a qualified coach has reviewed the numbers. Where a number is our own conservative choice rather than something the research gives us, it says so.

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

The coach keeps a small model of each person, updated after every set. It is plain data in the encrypted database, so it can be inspected and tested.

- **Capability per movement pattern.** Vertical pull, horizontal pull, vertical push, horizontal push, squat, hinge, core, and one track per skill. Each holds an estimate (best reps or hold time at a known load in kg) and how confident we are in it. Confidence starts low and rises with each set logged.
- **Skill progress.** The current gate on each skill path, using the gates already in `:core`.
- **Fatigue per pattern.** Hard sets in the last 7 days, and hours since the last hard session for that pattern.
- **Tendon load history.** Weekly hard sets per pattern over the last 4 weeks, so increases can be capped (section 4).
- **Turning up.** Sessions done against sessions planned, usual session length, usual training days and times.
- **Preferences.** What they swap out, skip, or rate Easy every time. This nudges exercise choice; it never overrides a hard rule.
- **Bodyweight trend.** See section 6.

### Effort ratings

Easy, Right and Hard map to roughly 4+, 2 to 3, and 0 to 1 reps in reserve. Rating effort by reps left is an established way to adjust training load from day to day ([Zourdos et al. 2016](https://doi.org/10.1519/JSC.0000000000001049); [Helms et al. 2018](https://doi.org/10.3389/fphys.2018.00247)). Novices tend to judge it less accurately than experienced lifters, so early on the coach leans on reps completed and gives effort ratings less weight until they line up with performance.

## 3. The shape of the plan

### Focused or all-round

- **Focused:** most of each session is the goal skill and the patterns that feed it. Other patterns get a maintenance dose.
- **All-round (default):** the goal skill comes first in most sessions, then the rest of the body gets proper work, so the person gets stronger and fitter overall while chasing the skill.

### Blocks

Training runs in blocks of 4 to 6 weeks. The skill work and the main exercises stay the same within a block so they can actually progress. A lighter week ends a block, or comes early if performance stalls (section 5).

### Rolling sessions

The plan is a queue of sessions, not a calendar. The training days the person chose are for reminders and the week view. If they miss Wednesday, Friday's session doesn't disappear and nothing turns red: the next session is simply whatever comes next, adjusted for recovery. If two sessions land on back-to-back days, the second avoids hard work on the patterns the first one trained.

## 4. Choosing a session

When the person opens Today, the coach builds the next session in three steps.

1. **Candidates.** From the block's templates it builds a handful of possible sessions that fit the time and equipment available.
2. **Hard rules filter.** Anything that breaks a rule is dropped:
   - pain rules from the pain-monitoring guidance already in `:core`
   - no hard session for a pattern within 48 hours of the last one (a common default; our choice to review)
   - weekly hard sets per pattern capped, and the week-on-week increase capped. Tendons adapt more slowly than muscle ([Mersmann et al. 2017](https://doi.org/10.3389/fphys.2017.00987)), so the caps exist to keep muscle strength from running ahead of tendon tolerance. The cap values are our conservative choice, not a number from the research.
   - skill gates in order: no exercise from beyond the next gate
3. **Scoring.** Each remaining candidate gets a score made of weighted terms:
   - **Goal progress:** how much it moves the current gate or goal forward.
   - **Volume gap:** how far each pattern is from its weekly target. Sets are counted as 1 for a direct set and 0.5 for an indirect one. Weekly volume has a dose-response with diminishing returns, steeper for strength than for size ([Pelland et al. 2026](https://sportrxiv.org/index.php/server/preprint/view/460)).
   - **Recovery:** prefers patterns that are rested.
   - **Time fit:** how close it comes to the time available, without going over.
   - **Novelty:** see "Variation" below.
   - **Preference:** a small nudge toward things they don't skip.

The highest score wins. The scoring is deterministic: the same model and the same date always give the same session, which makes it testable and lets a bug report be replayed.

### Progression inside a session

Sets keep the current shape (work sets with reps in reserve, then a best-effort set), now per exercise. Reps go up first. When the top of the rep range is reached at the target effort, the exercise moves up a rung or the load goes up. A gate is passed only by a strict, unassisted set, as it is today.

### Variation

Variation is useful when it's planned and harmful when it's random. A systematic review found that some planned variation can help, while swapping exercises too often, or for ones that train the same thing, may hold back gains ([Kassiano et al. 2022](https://doi.org/10.1519/JSC.0000000000004258)). So:

- **The skill and the main exercises don't change within a block.** Skills need repetition.
- **Accessory exercises rotate between blocks,** within a family that trains the same thing (pike push-up one block, decline push-up the next).
- **Sessions vary in ways that don't cost progress:** exercise order for accessories, rep ranges across the week, the finisher, the type of conditioning.
- **Novelty only breaks ties.** It is the lowest-weighted term, so it never beats goal progress or recovery.

## 5. Readiness and suggestions

The coach watches for signs that a person needs an easier day, or is ready for more. Signals, from most to least trusted:

1. **Performance trend.** Reps or hold time at the same load, compared with their recent best. This is the main signal.
2. **Effort ratings** against what the reps would predict.
3. **Gaps.** Days since the last session. A long gap means a comeback session, not a guilt message.
4. **Pain check-ins.** One sore area is enough to trigger the pain rules on its own.
5. **Bodyweight trend** (section 6).
6. **Sleep and resting heart rate from Health Connect.** Later, and only as a soft nudge. Training guided by heart rate variability has given at best small benefits over a fixed plan in meta-analyses ([Düking et al. 2021](https://doi.org/10.1016/j.jsams.2021.04.012); [Manresa-Rocamora et al. 2021](https://doi.org/10.3390/ijerph181910299)), so it never decides anything alone.

A suggestion needs at least two signals pointing the same way, or one strong one (pain). It is always offered with three choices: **Keep it**, **Go lighter**, **Rest today**. Nothing is skipped without the person saying so.

The choice feeds back. If someone often picks Keep after a suggestion and then performs well, the coach raises the bar for suggesting. If they pick Lighter and bounce back, it learns that works for them.

Going the other way, two or three sessions clearly above target at low effort move the person up faster, also with a reason ("You beat your target three times. Moving you up a rung.").

## 6. Bodyweight

Bodyweight matters to the coach in two ways: it sets the kg per rep for every bodyweight exercise, and a fast change in it can mean the person isn't recovering well.

### Getting it

- **Health Connect first.** If the person allows it, the app reads weight records from Health Connect (a smart scale or another app may write them). It asks for `READ_WEIGHT` only. Reads happen when the app is open, so it needs no background read permission. Health Connect lets an app read up to 30 days before permission was first granted, without the extra history permission ([read data guide](https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data)). That is plenty for a trend, so we don't ask for history either.
- **Otherwise, a quick prompt before a session.** Only when there's no reading in the last 7 days, at most once a week, prefilled with the last value, and skippable. A step of 0.1 kg, so it takes one or two taps.

### Using it

- Daily weight swings by a kilogram or more with water and food, so the coach uses a smoothed trend, never a single reading. It needs at least three readings over ten or more days before it acts on a trend.
- **Load per rep** always uses the latest trend value. If someone gains weight, their pull-up gets heavier in kg, and the model already works in kg, so targets stay sensible.
- **Fast loss.** Guidance for people training while losing weight is to lose about 0.5 to 1% of bodyweight a week to keep muscle and strength ([Ruiz-Castellano et al. 2021](https://doi.org/10.3390/nu13093255)). In one study, athletes losing 0.7% a week gained strength, while those aiming for 1.4% didn't ([Garthe et al. 2011](https://doi.org/10.1123/ijsnem.21.2.97)). So when the trend shows more than 1% a week for two weeks running, the coach:
  - holds progression steady instead of adding work, and says why
  - if performance is also dropping, offers a lighter week
  - tells the person plainly once, with no judgement, and suggests talking to a GP if they aren't trying to lose weight

### What it never does

- It never praises weight loss, sets weight targets, or shows weight streaks. Levitx is a training app, and weight is an input to training, not a score.
- It never comments on how someone looks.
- Bodyweight stays on the phone, encrypted, and never goes to the watch (SECURITY.md).

## 7. Explaining itself

Every change the coach makes comes with one plain sentence, built from the scoring term that mattered most. For example:

- "Lighter today: your pull reps dropped two sessions in a row."
- "New push exercise this block: decline push-ups, same muscles, a bit harder."
- "Legs first today: you trained pulling yesterday."
- "Still learning your numbers. Targets are on the easy side for now."

The sentences are templates filled from the engine's data, so they are always true to what the engine did.

## 8. Testing

- **Unit tests** for every hard rule and every scoring term.
- **A simulation harness** in `:core` that runs made-up people through 12 weeks of sessions, with set results drawn from a simple response model. Personas:
  - a beginner at zero pull-ups
  - an intermediate who has plateaued
  - someone who trains once a week however many days they chose
  - a comeback after three weeks off
  - someone losing weight fast
  - someone whose elbow flares in week 5
- **Pass conditions:** no session ever breaks a hard rule; weekly volume stays inside its caps; suggestions come at a sensible rate (not every session, not never); the beginner reaches the first gates in a believable time; the comeback starts lighter. A qualified coach reviews sample 12-week plans from each persona before release.

## 9. Not yet

- Meal plans and nutrition.
- Sleep and resting heart rate as readiness signals.
- Any language model in the loop.

Each gets its own decision record when we get there.

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
