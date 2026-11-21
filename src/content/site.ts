import type { PhotoKey } from "./photos";
import type { GuideSlug } from "./guides";

/**
 * All page copy lives here so wording changes don't mean digging through components.
 *
 * House rule: one heading and at most one line per idea. If a section needs a paragraph,
 * it's two sections or it belongs in the FAQ.
 */

export const site = {
  name: "Levitx",
  /** The canonical address. Vercel sets NEXT_PUBLIC_SITE_URL; this is the production fallback. */
  url: (process.env.NEXT_PUBLIC_SITE_URL ?? "https://www.levitx.app").replace(/\/$/, ""),
  title: "Levitx: Calisthenics Coach App for Android and Wear OS",
  description:
    "Levitx tests what you can do, then plans every calisthenics session around today, from your first pull-up to a muscle-up or planche. Android and Wear OS.",
  platforms: ["Android", "Wear OS"],
  publisher: { name: "Xenova Digital", url: "https://xenovadigital.com", city: "Brisbane", region: "QLD", country: "AU" },
};

export const nav = [
  { href: "/#coach", label: "Meet Levi" },
  { href: "/guides", label: "Guides" },
  { href: "/#calculator", label: "Calculator" },
  { href: "/#faq", label: "FAQ" },
];

export const hero = {
  eyebrow: "Android · Wear OS",
  /** Two short lines. The first word carries the search term; the rest carries the promise. */
  title: ["Calisthenics,", "coached."],
  body: "Levi, your coach, plans every session around how you're really doing.",
  cta: "Get early access",
  photo: "hero" as PhotoKey,
};

export const path = {
  id: "path",
  eyebrow: "How it works",
  title: "Pick a goal. Start where you are.",
  photo: "path" as PhotoKey,
  gates: {
    goal: "Muscle-up",
    remaining: "3 to go",
    items: [
      { name: "30 s dead hang", state: "done" },
      { name: "10 straight-bar dips", state: "done" },
      { name: "10 pull-ups", state: "current", progress: "8 / 10" },
      { name: "5 chest-to-bar pull-ups", state: "next" },
      { name: "Muscle-up", state: "goal" },
    ] as const,
  },
  steps: [
    { title: "Take a short test", body: "A few sets show where you really are." },
    { title: "Train today's session", body: "Planned around your last one, your kit and your time." },
    { title: "Clear one gate at a time", body: "Small tests, weeks apart, on the way to the skill." },
  ],
};

/**
 * Meet Levi: the coach, shown at work over a few months. The cards copy the app's own screens
 * and wording (CoachPhrases in mobile/core and the phone's strings.xml), and every behaviour is
 * one the coach engine really has (see the coach-engine design doc). Keep them in step: if the
 * app's wording or the engine changes, this changes in the same pull request.
 */

/** A line of a session as the Today screen lists it. */
export type CoachRow = { name: string; dose: string; tag?: string };

/** A small copy of an app screen. Every field is optional because each moment shows a different screen. */
export type CoachCard = {
  /** The small capitals label at the top of the screen. */
  kicker: string;
  /** The screen's headline, when it has one. */
  title?: string;
  /** Levi's one-line reason for the session. */
  reason?: string;
  /** A target change: before and after. */
  change?: { exercise: string; from: string; to: string };
  /** What Levi noticed, on a suggestion card. */
  noticed?: string[];
  /** The session, on screens that list it. */
  rows?: CoachRow[];
  /** The choices offered, first one highlighted as the app does. */
  choices?: string[];
  /** Small print under the card. */
  note?: string;
};

export type CoachMoment = {
  /** When in the timeline, such as "Week 3". */
  when: string;
  /** What happened, in a few words. */
  what: string;
  title: string;
  body: string;
  card: CoachCard;
};

export const coach = {
  id: "coach",
  eyebrow: "Meet Levi",
  title: ["You show up.", "Levi does the rest."],
  body: "Levi is the coach inside Levitx. Like a good personal trainer, it watches every session and changes the next one before you think to ask, then tells you why in one line.",
  photo: "adapts" as PhotoKey,
  timelineLabel: "A few months with Levi",
  moments: [
    {
      when: "Week 1",
      what: "Your first sessions",
      title: "No fitness quiz. It watches.",
      body: "Your first two sessions end each exercise with one best-effort set. Levi sets your level from what you actually do, not from what you guess.",
      card: {
        kicker: "Today · Training",
        reason: "Still learning your numbers. Targets are on the easy side for now.",
        rows: [
          { name: "Dead hang", dose: "3 × 20 s" },
          { name: "Knee push-up", dose: "3 × 8" },
          { name: "Bodyweight squat", dose: "3 × 12" },
        ],
      },
    },
    {
      when: "Week 3",
      what: "After a strong session",
      title: "A rep more, when you've earned it.",
      body: "Hit every set and the target goes up. Top out the range and you move to a harder version. That's progressive overload, done for you every session.",
      card: {
        kicker: "Today · Training",
        change: { exercise: "Knee push-up", from: "3 × 8", to: "3 × 9" },
        reason: "Knee push-up: target up. You hit every set last time.",
      },
    },
    {
      when: "Week 4",
      what: "Recovery running slow",
      title: "It notices before you do.",
      body: "Reps dropping two sessions running, sets that keep feeling hard. When the signs add up, Levi offers an easier day. Nothing changes unless you say so.",
      card: {
        kicker: "Before you start",
        title: "You might want an easier day",
        noticed: ["Australian row: 10 → 8 → 8 over your last sessions.", "Australian row felt hard two sessions running."],
        choices: ["Keep it", "Go lighter", "Rest today"],
        note: "Whatever you pick is fine. The coach learns what works for you.",
      },
    },
    {
      when: "Week 5",
      what: "Your elbow's sore",
      title: "Works around pain. Knows when to stop.",
      body: "Report a sore elbow and Levi trains around it: legs as normal, pushing kept light. If the warning signs point to more than soreness, it stops loading that area and tells you to get it looked at.",
      card: {
        kicker: "Today · Training",
        reason: "Working around your elbow today.",
        rows: [
          { name: "Split squat", dose: "3 × 10" },
          { name: "Glute bridge", dose: "3 × 15" },
          { name: "Knee push-up", dose: "2 × 7", tag: "Light" },
        ],
      },
    },
    {
      when: "Week 7",
      what: "A new block",
      title: "Fresh, never random.",
      body: "After every lighter week, the extra exercises change to something just as hard that trains the same muscles. Your goal work stays put, because skills need repetition.",
      card: {
        kicker: "Today · Training",
        change: { exercise: "Split squat", from: "Split squat", to: "Reverse lunge" },
        reason: "Reverse lunge: new this block. Same muscles, a fresh challenge.",
      },
    },
    {
      when: "Week 10",
      what: "Back after a break",
      title: "Come back. Don't restart.",
      body: "Missed days don't turn red and the plan doesn't fall apart. The next session simply starts where you are.",
      card: {
        kicker: "Comeback day · 16 days away",
        title: "Welcome back.",
        reason: "You're not starting over: every exercise picks up where you left it. Today is lighter, and the next session is back to normal.",
        choices: ["Lighter", "Full session"],
      },
    },
  ] satisfies CoachMoment[],
  /** Photos set between the moments, to keep the story breathing. Index is the moment they follow. */
  breaks: [
    { after: 1, photo: "lighter" as PhotoKey },
    { after: 4, photo: "comeback" as PhotoKey },
  ],
  builtIn: {
    title: "Built in, so you never have to plan it.",
    items: [
      {
        title: "Progressive overload",
        body: "A rep at a time, then slower lowering, then a harder move. You never have to work out when to step up.",
      },
      {
        title: "Recovery",
        body: "A rest day between hard sessions for each movement, and a lighter week at the end of every block.",
      },
      {
        title: "Your joints",
        body: "Weekly limits keep your strength from outrunning your tendons, and pain changes the plan straight away.",
      },
    ],
  },
  note: "Levi is software, not a person, a doctor or a physio. Its rules come from published training research and are being reviewed by a qualified coach before launch.",
};

type SkillItem = { name: string; range: string; photo: PhotoKey; guide: GuideSlug };

export const skills = {
  id: "skills",
  eyebrow: "Skills",
  title: "Train for the skill you want.",
  items: [
    { name: "Muscle-up", range: "Dead hang to bar", photo: "muscleUp", guide: "muscle-up" },
    { name: "Front lever", range: "Tuck to full", photo: "frontLever", guide: "front-lever" },
    { name: "Planche", range: "Lean to straddle", photo: "planche", guide: "planche" },
    { name: "Handstand", range: "Wall to freestanding", photo: "handstand", guide: "handstand" },
  ] satisfies SkillItem[] as SkillItem[],
};

export const calculator = {
  id: "calculator",
  eyebrow: "Free tool",
  title: "How much does a push-up weigh?",
  /** Written as the answer a search engine can lift whole: about 40 words, numbers first. */
  answer:
    "A regular push-up lifts about 64% of your bodyweight. Raising your hands makes it lighter, down to about 41%. Raising your feet makes it heavier, up to about 74%. Enter your weight to see the load in kilograms.",
  source: "Force-plate studies: Ebben et al. 2011, Suprak et al. 2011. A vest is counted at the same share.",
  sourceUrl: "https://ojs.ub.uni-konstanz.de/cpa/article/view/4457",
};

export const watch = {
  id: "watch",
  eyebrow: "Wear OS",
  title: "Phone stays in the bag.",
  body: "Sets, holds and rest run on your watch.",
  photo: "watch" as PhotoKey,
};


export const privacy = {
  eyebrow: "Private by design",
  title: "Your training stays on your phone.",
  body: "No account, no server, no ads, no tracking.",
  link: { href: "/privacy", label: "Read the privacy policy" },
};

export const faq = {
  id: "faq",
  eyebrow: "FAQ",
  title: "Questions, answered.",
  items: [
    {
      q: "Who is Levi?",
      a: "Levi is the coach built into Levitx. It plans each session from how your last ones went, how you're recovering and any pain you report, and tells you why in one line. It's software, not a person, and it doesn't replace a doctor or physio.",
    },
    {
      q: "What is Levitx?",
      a: "Levitx is a calisthenics coach app for Android and Wear OS. It starts with a short test of what you can do, then plans every session around today: which exercises, how many sets and reps, and when to move to a harder version.",
    },
    {
      q: "Is Levitx good for beginners?",
      a: "Yes. If you can't do a pull-up yet, your plan starts with moves you can do today, like dead hangs and negatives, and builds from there. Each goal is split into gates, small tests a few weeks apart, so you always know what comes next.",
    },
    {
      q: "What equipment do I need for calisthenics?",
      a: "Nothing to start. A wall, a chair and the floor cover the first sessions. A pull-up bar opens up most skills, and rings, dip bars, bands or a weighted vest add more. Tell Levitx what you have and it only plans what you can do with it.",
    },
    {
      q: "Does Levitx work on a smartwatch?",
      a: "Yes, on Wear OS. The watch runs your sets, holds and rest timers, so your phone can stay in your bag. Workouts you finish on the watch go back to your phone's history.",
    },
    {
      q: "Is my training data private?",
      a: "Your training stays on your phone, in an encrypted database. Levitx has no account, no server, no ads and no analytics, so there's nothing to sign up for. You can delete everything from the app's settings at any time.",
    },
    {
      q: "Is Levitx a medical app?",
      a: "No. Levitx is a training app, not a medical device, and it doesn't diagnose or treat injuries. You can always choose a lighter session or a rest day, but pain that doesn't settle is a reason to see a doctor or physiotherapist.",
    },
    {
      q: "When can I get Levitx?",
      a: "Levitx is in early testing on Android. Leave your email and you'll get one message when early access opens.",
    },
  ],
};

export const cta = {
  id: "join",
  title: "Start where you are.",
  photo: "join" as PhotoKey,
};

export const waitlistCopy = {
  label: "Early access",
  placeholder: "you@example.com",
  button: "Get early access",
  hint: "One email when early access opens. Nothing else.",
};

export const footer = {
  /** The closing line, set large. */
  sign: ["Train with", "a plan."],
  disclaimer:
    "Levitx is a training app, not a medical device. It doesn't diagnose or treat injuries. If pain isn't settling, see a doctor or physiotherapist.",
  columns: [
    {
      title: "Explore",
      links: [
        { href: "/#coach", label: "Meet Levi" },
        { href: "/guides", label: "Skill guides" },
        { href: "/#calculator", label: "Push-up calculator" },
        { href: "/#faq", label: "FAQ" },
      ],
    },
    {
      title: "Levitx",
      links: [
        { href: "/#join", label: "Early access" },
        { href: "/privacy", label: "Privacy" },
        { href: "mailto:support@xenovadigital.com", label: "Contact" },
      ],
    },
  ],
  maker: "Made in Brisbane by",
};
