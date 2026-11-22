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
 * Meet Levi. Written from the visitor's side: each story is a thought people really have a
 * few weeks into training (drawn from forum threads and app reviews: not knowing your level,
 * when to make it harder, flat weeks, sore elbows, stale routines, starting over after time
 * off), answered by what Levi shows in the app at that moment.
 *
 * Every reply uses the app's own wording (CoachPhrases in mobile/core, the phone's
 * strings.xml) and site.test.ts checks it. Every behaviour is one the coach engine has.
 */
export type CoachReply = {
  /** A screen headline, when the app shows one. */
  title?: string;
  /** Levi's line, word for word from the app. */
  line?: string;
  /** Choices the app offers. The first is the one it highlights. */
  choices?: string[];
};

export type CoachStory = {
  /** Roughly when this comes up, as a small tag. */
  when: string;
  /** What the person is thinking. Short, in their voice. */
  thought: string;
  reply: CoachReply;
  photo: PhotoKey;
  /** Where the subject sits in the photo, so the crop keeps them in frame. */
  position?: string;
  /** Put the words at the top when the subject sits low in the photo. Bottom by default. */
  textAt?: "top" | "bottom";
};

export const coach = {
  id: "coach",
  eyebrow: "Meet Levi",
  title: ["You show up.", "Levi does the rest."],
  body: "Levi reads every set you log and plans your next session around it.",
  photo: "adapts" as PhotoKey,
  storiesTitle: "Where most training plans fall apart.",
  stories: [
    {
      when: "Week 1",
      thought: "I don't even know my level.",
      reply: { line: "Still learning your numbers. Targets are on the easy side for now." },
      photo: "levelCheck",
      textAt: "top",
    },
    {
      when: "Week 3",
      thought: "When do I make it harder?",
      reply: { line: "Knee push-up: target up. You hit every set last time." },
      photo: "pushStart",
      position: "55% 40%",
    },
    {
      when: "Week 4",
      thought: "Everything feels heavy this week.",
      reply: { title: "You might want an easier day", choices: ["Keep it", "Go lighter", "Rest today"] },
      photo: "lighter",
    },
    {
      when: "Week 5",
      thought: "My elbow's started complaining.",
      reply: { line: "Working around your elbow today." },
      photo: "elbow",
      position: "50% 30%",
    },
    {
      when: "Week 7",
      thought: "Same workout. Again.",
      reply: { line: "Reverse lunge: new this block. Same muscles, a fresh challenge." },
      photo: "freshBlock",
      position: "45% 60%",
    },
    {
      when: "Week 10",
      thought: "Two weeks off. Back to day one?",
      reply: { line: "You're not starting over: every exercise picks up where you left it." },
      photo: "comeback",
      position: "50% 35%",
    },
  ] satisfies CoachStory[],
  builtIn: {
    title: "Built in, so you never have to plan it.",
    items: [
      { title: "Progressive overload", body: "A small step up every time you've earned one." },
      { title: "Recovery", body: "A day between hard sessions. A lighter week every block." },
      { title: "Your joints", body: "Weekly limits, and pain changes the plan straight away." },
    ],
  },
  note: "Levi is software, not a physio. Its rules come from published training research and are being reviewed by a qualified coach.",
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
