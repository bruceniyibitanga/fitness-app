import type { PhotoKey } from "./photos";

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
  { href: "/#path", label: "How it works" },
  { href: "/#skills", label: "Skills" },
  { href: "/#calculator", label: "Calculator" },
  { href: "/#faq", label: "FAQ" },
];

export const hero = {
  eyebrow: "Android · Wear OS",
  /** Two short lines. The first word carries the search term; the rest carries the promise. */
  title: ["Calisthenics,", "coached."],
  body: "Every session planned around what you can do today.",
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

export const adapts = {
  id: "adapts",
  eyebrow: "It adapts",
  title: "It changes when you do.",
  body: "Nothing left in the tank last time? Today is lighter.",
  card: {
    title: "Today · pull-ups",
    badge: "Adjusted",
    from: "3×8",
    to: "3×6",
    reason: "Last session: nothing left in the tank",
    note: "Back to 3×8 once you recover",
  },
  photo: "adapts" as PhotoKey,
};

type SkillItem = { name: string; range: string; photo: PhotoKey };

export const skills = {
  id: "skills",
  eyebrow: "Skills",
  title: "Train for the skill you want.",
  items: [
    { name: "Muscle-up", range: "Dead hang to bar", photo: "muscleUp" },
    { name: "Front lever", range: "Tuck to full", photo: "frontLever" },
    { name: "Planche", range: "Lean to straddle", photo: "planche" },
    { name: "Handstand", range: "Wall to freestanding", photo: "handstand" },
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

export const keepGoing = {
  eyebrow: "Keeping at it",
  title: "For the weeks you don't feel like it.",
  tiles: [
    { tag: "Missed a week", title: "Come back. Don't restart.", photo: "comeback" },
    { tag: "Running on empty", title: "Take the lighter session.", photo: "lighter" },
    { tag: "Need a day off", title: "Rest counts too.", photo: "rest" },
  ] satisfies { tag: string; title: string; photo: PhotoKey }[],
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
        { href: "/#path", label: "How it works" },
        { href: "/#skills", label: "Skills" },
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
