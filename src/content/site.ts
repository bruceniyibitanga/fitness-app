import type { PhotoKey } from "./photos";

/**
 * All page copy lives here so wording changes don't mean digging through components.
 * Keep it short: one heading and one line per idea.
 */

export const site = {
  name: "LEVER",
  url: process.env.NEXT_PUBLIC_SITE_URL ?? "https://lever.app",
  title: "LEVER: Adaptive Calisthenics App for iPhone and Apple Watch",
  description:
    "A calisthenics app that tests what you can do, maps the steps to your goal and adjusts every session. Muscle-up, front lever, planche and handstand, on iPhone and Apple Watch.",
  city: "Brisbane",
};

export const nav = [
  { href: "/#path", label: "How it works" },
  { href: "/#skills", label: "Skills" },
  { href: "/#calculator", label: "Calculator" },
  { href: "/#watch", label: "Watch" },
];

export const hero = {
  eyebrow: "Adaptive calisthenics app · iPhone + Apple Watch",
  lead: "From where you are to your first",
  goals: ["muscle-up", "front lever", "handstand", "pull-up"],
  body: "LEVER tests what you can do, maps the steps to your goal, and adjusts every session to how you're actually performing.",
  card: {
    title: "Today · pull-ups",
    badge: "Adjusted",
    from: "3×8",
    to: "3×6",
    reasons: [
      { label: "Last session", value: "0 reps left in the tank" },
      { label: "Rep speed", value: "down 24%" },
    ],
    note: "Back to 3×8 once you recover",
  },
  stats: [
    { value: "Your level", label: "Where the plan starts" },
    { value: "Every session", label: "When the plan adjusts" },
    { value: "Your goal", label: "Where the plan ends" },
  ],
};

export const tickerSkills = [
  "Muscle-up", "Front lever", "Planche", "Handstand", "Weighted pull-up",
  "Ring dips", "L-sit", "Pistol squat", "Back lever", "Human flag",
];

export const path = {
  id: "path",
  number: "01",
  label: "Path",
  title: "Pick a goal. Start where you are.",
  gates: {
    goal: "Muscle-up",
    remaining: "3 to go",
    items: [
      { name: "30 s dead hang", state: "done" },
      { name: "10 straight-bar dips", state: "done" },
      { name: "10 pull-ups", state: "current", progress: "8 / 10" },
      { name: "5 chest-to-bar pull-ups", state: "next" },
      { name: "20 s false-grip hang", state: "next" },
      { name: "Muscle-up", state: "goal" },
    ] as const,
  },
  steps: [
    { title: "Choose the goal.", body: "Your first pull-up, a muscle-up, front lever, planche or handstand." },
    { title: "Take a short test.", body: "A few max-rep sets and holds show where you really are." },
    { title: "Clear one gate at a time.", body: "Each gate is weeks away, not months, and shows what it trains." },
    { title: "Tendons set the pace.", body: "Straight-arm steps unlock on a schedule, even when you feel ready." },
  ],
};

export const adapts = {
  id: "adapts",
  number: "02",
  label: "Adapts",
  title: "It changes when you do.",
  rules: [
    { when: "Reps slowing, nothing left in the tank", then: "Next session gets lighter", tone: "neutral" },
    { when: "Gate cleared early", then: "Next step unlocks sooner", tone: "neutral" },
    { when: "Elbow sore at 6/10", then: "Swaps to a move that loads it less", tone: "care" },
    { when: "Missed a week", then: "Comeback session, not a restart", tone: "neutral" },
  ] as const,
  photo: "pullUpIndoor" as PhotoKey,
  caption: { label: "What it reads", text: "Your reps, how hard they felt, how you slept and what hurts." },
};

type SkillItem = { name: string; range: string; href?: string; photo: PhotoKey };

export const skills = {
  id: "skills",
  number: "03",
  label: "Skills",
  title: "Progressions for the big four.",
  lede: "Free guides, coming soon. Each one is a path inside the app.",
  items: ([
    { name: "Muscle-up", range: "Dead hang → bar", photo: "muscleUp" },
    { name: "Front lever", range: "Tuck → full", photo: "frontLever" },
    { name: "Planche", range: "Lean → straddle", photo: "planche" },
    { name: "Handstand", range: "Wall → freestanding", photo: "handstand" },
  ] satisfies SkillItem[]) as SkillItem[],
};

export const calculator = {
  id: "calculator",
  number: "04",
  label: "Push-up weight calculator",
  title: "How much does a push-up weigh?",
  lede: "Between 41% and 74% of you, depending on the version. LEVER tracks it for every set.",
  source: "Estimate from force-plate studies (Ebben 2011, Suprak 2011). Vest counted at the same share.",
  sourceUrl: "https://ojs.ub.uni-konstanz.de/cpa/article/view/4457",
};

export const watch = {
  id: "watch",
  number: "05",
  label: "Watch",
  title: "Phone stays in the bag.",
  body: "Reps, holds and rest run on Apple Watch. It buzzes when you pass your best hold.",
  photo: "mountain" as PhotoKey,
};

export const keepGoing = {
  number: "06",
  label: "Keeping at it",
  title: "For the weeks you don't feel like it.",
  tiles: [
    { tag: "Missed a session", tone: "accent", title: "Comebacks count.", body: "One miss is fine. Today is the one that counts.", photo: "winter" },
    { tag: "Something hurts", tone: "care", title: "Rate it 0 to 10.", body: "The plan eases or swaps the move.", photo: "rings" },
    { tag: "Short on time", tone: "accent", title: "Ten-minute version.", body: "Same plan, shorter session.", photo: "park" },
  ] satisfies { tag: string; tone: "accent" | "care"; title: string; body: string; photo: PhotoKey }[],
};

export const parks = {
  number: "07",
  label: "Parks",
  title: "Calisthenics parks in Brisbane.",
  names: ["Victoria Park", "Kangaroo Point", "Waterline Park"],
  link: "Park guide · coming soon",
  photo: "park" as PhotoKey,
};

export const faq = {
  number: "08",
  label: "FAQ",
  title: "Short answers.",
  items: [
    { q: "What equipment do I need?", a: "A bar you can hang from. Rings, bands and a weighted vest are optional, and LEVER counts them in your load when you use them." },
    { q: "Is it for beginners?", a: "Yes. If you can't do a pull-up yet, your path starts with moves you can do today. The detail is one tap deeper for people who want it." },
    { q: "Does it work on Apple Watch?", a: "Apple Watch first, with full sessions that don't need your phone. Wear OS comes after." },
    { q: "Is LEVER a medical app?", a: "No. Pain-aware mode adjusts how you load a move and can follow a plan from your physio. It doesn't diagnose or treat anything." },
  ],
};

export const cta = {
  title: "Start where you are.",
  photo: "duo" as PhotoKey,
};

export const waitlistCopy = {
  label: "Early access by email",
  placeholder: "you@example.com",
  button: "Get early access",
  hint: "One email when early access opens.",
};

export const footer = {
  disclaimer:
    "LEVER is a training app, not a medical device. It doesn't diagnose or treat injuries. If pain isn't settling, see a doctor or physiotherapist.",
  links: [
    { href: "/#skills", label: "Guides" },
    { href: "/#calculator", label: "Calculators" },
    { href: "/privacy", label: "Privacy" },
  ],
};
