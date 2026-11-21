import type { PhotoKey } from "./photos";

/**
 * The skill guides: one page each, plus a free printable plan (PDF) built from the same
 * content. This file is the only place the guide text lives, so the page, the PDF, the
 * structured data and /llms.txt can't drift apart.
 *
 * Pass tests are coaching targets, not medical advice. They're deliberately conservative:
 * hold the stage cleanly before moving on.
 */

export const GUIDE_SLUGS = ["muscle-up", "front-lever", "planche", "handstand"] as const;
export type GuideSlug = (typeof GUIDE_SLUGS)[number];

export type Stage = {
  name: string;
  /** What "passing" this stage looks like. Short, so it fits on a checklist line. */
  pass: string;
  /** One coaching cue for the stage. */
  cue: string;
};

export type Mistake = { title: string; fix: string };

export type Session = {
  label: string;
  focus: string;
  items: { name: string; dose: string }[];
};

export type Guide = {
  slug: GuideSlug;
  skill: string;
  /** <title>. Kept under 60 characters (tested). */
  title: string;
  /** Meta description. 120 to 160 characters (tested). */
  description: string;
  h1: string;
  /** The answer in about 50 words, written so a search engine or assistant can lift it whole. */
  answer: string;
  eyebrow: string;
  photo: PhotoKey;
  /** Where to centre the photo when it's cropped. */
  photoPosition: string;
  before: string[];
  stages: Stage[];
  mistakes: Mistake[];
  week: Session[];
  /** A note under the week plan. */
  weekNote: string;
  faq: { q: string; a: string }[];
  /** An optional safety note the page sets large. */
  callout?: { title: string; body: string };
  plan: {
    title: string;
    /** Served from /public, so the path starts at the site root. */
    file: `/plans/levitx-${GuideSlug}-plan.pdf`;
    pitch: string;
  };
};

export const guides: Guide[] = [
  {
    slug: "muscle-up",
    skill: "Muscle-up",
    title: "How to Do a Muscle-Up: Progressions and Free Plan",
    description:
      "Learn the muscle-up step by step: the pull-up and dip numbers to hit first, seven progressions with pass tests, common mistakes and a free printable plan.",
    h1: "How to do a muscle-up",
    answer:
      "A muscle-up takes you from hanging under a bar to holding yourself above it in one move: a fast, high pull, a quick lean over the bar, then a dip to straight arms. Build 10 strict pull-ups and 10 straight-bar dips first, then train the high pull and the turn over the bar.",
    eyebrow: "Guide · Pull and push",
    photo: "muscleUp",
    photoPosition: "50% 22%",
    before: ["10 strict pull-ups", "10 straight-bar dips", "A 30 second dead hang"],
    stages: [
      { name: "Dead hang", pass: "30 s with active shoulders", cue: "Hang long, then draw the shoulders down a little without bending the arms." },
      { name: "Straight-bar dips", pass: "10 reps to full lockout", cue: "Lean over the bar and keep it close to your hips the whole way." },
      { name: "Pull-ups", pass: "10 strict reps from a dead hang", cue: "Chin clearly over the bar, no kick, full hang at the bottom." },
      { name: "Chest-to-bar pull-ups", pass: "5 reps, bar touches below the collarbone", cue: "Drive the elbows back and pull the bar toward your sternum." },
      { name: "Explosive pull-ups", pass: "3 reps that bring the bar to your lower chest", cue: "Pull as fast as you can, up and slightly back, not just up." },
      { name: "Slow negatives", pass: "3 lowerings of 5 seconds through the turn", cue: "Start on top, lower slowly and keep the bar close as you pass the transition." },
      { name: "First muscle-up", pass: "1 clean rep, both arms turning together", cue: "Pull high, throw the chest over the bar, then finish with a dip." },
    ],
    mistakes: [
      { title: "One arm at a time", fix: "Known as chicken-winging. Build the high pull and do slow negatives until both arms turn over together." },
      { title: "Pulling straight up", fix: "The bar has to travel around you. Start slightly in front of it and pull up and back toward your hips." },
      { title: "Skipping the dip", fix: "The top half of a muscle-up is a dip. If straight-bar dips are weak, the turnover stalls." },
      { title: "Too many reps too soon", fix: "Muscle-ups load the elbows and wrists. Keep sets short and stop while every rep still looks the same." },
    ],
    week: [
      {
        label: "Day 1",
        focus: "Strength",
        items: [
          { name: "Chest-to-bar pull-ups", dose: "4 × 3 to 5" },
          { name: "Straight-bar dips", dose: "4 × 6 to 8" },
          { name: "Hollow body hold", dose: "3 × 20 to 30 s" },
        ],
      },
      {
        label: "Day 2",
        focus: "Skill",
        items: [
          { name: "Explosive pull-ups", dose: "5 × 2 to 3" },
          { name: "Slow negatives", dose: "3 × 1 to 2" },
          { name: "Dead hang", dose: "2 × 30 s" },
        ],
      },
      {
        label: "Day 3",
        focus: "Volume",
        items: [
          { name: "Pull-ups", dose: "3 sets, stop 2 reps short" },
          { name: "Straight-bar dips", dose: "3 sets, stop 2 reps short" },
          { name: "Band-assisted muscle-ups", dose: "3 × 2 to 3" },
        ],
      },
    ],
    weekNote: "Leave a rest day between sessions. Move to the next stage only when you pass the current one on a fresh day.",
    faq: [
      {
        q: "How long does it take to learn a muscle-up?",
        a: "There's no fair single answer. It depends on your strength now, your bodyweight and how often you train. Ten strict pull-ups and ten dips are the usual starting line. From there it's weeks of steady practice rather than days, and the slow negatives tend to be what makes it click.",
      },
      {
        q: "Do I need a false grip for a bar muscle-up?",
        a: "No. On a bar, a normal overhand grip works, with your hands a little over the top of the bar. A false grip matters much more on rings, where the wrist has to stay over the ring through the turn.",
      },
      {
        q: "Should I learn the muscle-up on a bar or rings?",
        a: "Most people learn on a bar first. The bar stays still, so you can focus on pulling high. Rings move and need a false grip, which makes the transition harder at the start. Ring muscle-ups make a good next goal.",
      },
      {
        q: "Is a little swing cheating?",
        a: "A small swing is a common way to learn the timing, as long as your shoulders are ready for it. A strict muscle-up with no swing is harder and builds more strength. Both are real skills; just be honest about which one you're training.",
      },
    ],
    plan: {
      title: "The muscle-up plan",
      file: "/plans/levitx-muscle-up-plan.pdf",
      pitch: "All seven stages as a checklist, a three-day week and a four-week log, on two A4 pages.",
    },
  },
  {
    slug: "front-lever",
    skill: "Front lever",
    title: "How to Do a Front Lever: Progressions and Free Plan",
    description:
      "Learn the front lever from tuck to full: five stages with pass tests, the mistakes that stall progress, a sample week and a free printable training plan.",
    h1: "How to do a front lever",
    answer:
      "A front lever is a straight-arm hold under a bar or rings with your body level and facing up. Your lats and core do most of the work. Learn it in stages, from a tuck to a full lever, and move on only when each hold is clean with straight arms.",
    eyebrow: "Guide · Pull",
    photo: "frontLever",
    photoPosition: "62% 45%",
    before: ["8 to 10 strict pull-ups", "A 30 second hollow body hold", "An active hang with the shoulders drawn down"],
    stages: [
      { name: "Tuck front lever", pass: "15 s with straight arms", cue: "Knees to chest, back level with the floor, push the bar toward your hips." },
      { name: "Advanced tuck", pass: "10 to 15 s with a flat back", cue: "Open the hips until the thighs sit at a right angle to your body. Keep the back flat." },
      { name: "One-leg front lever", pass: "10 s each side", cue: "One leg long, one tucked. Swap legs every set." },
      { name: "Straddle front lever", pass: "5 to 10 s", cue: "Legs wide, body straight from the shoulders to the hips." },
      { name: "Full front lever", pass: "5 s with legs together", cue: "Squeeze the glutes, point the toes and don't let the hips drop." },
    ],
    mistakes: [
      { title: "Bent arms", fix: "The hold is straight-armed. If your elbows bend, the stage is too hard for today: go back one." },
      { title: "Hips dropping", fix: "Squeeze the glutes and brace. If the hips still sag, shorten the lever until they don't." },
      { title: "A rounded back in the advanced tuck", fix: "Keep the back flat even if it means opening the hips less. A rounded back makes it a tuck again." },
      { title: "Shrugged shoulders", fix: "Keep the shoulders pulled down, away from the ears. That's where the lever's strength comes from." },
    ],
    week: [
      {
        label: "Day 1",
        focus: "Holds",
        items: [
          { name: "Holds at your current stage", dose: "5 × 5 to 10 s" },
          { name: "Tuck front lever raises", dose: "3 × 5" },
          { name: "Hollow body hold", dose: "3 × 30 s" },
        ],
      },
      {
        label: "Day 2",
        focus: "Pull strength",
        items: [
          { name: "Pull-ups", dose: "4 × 5 to 8" },
          { name: "Rows", dose: "3 × 8 to 10" },
          { name: "Scapular pull-ups", dose: "3 × 8" },
        ],
      },
      {
        label: "Day 3",
        focus: "Holds and negatives",
        items: [
          { name: "Holds at your current stage", dose: "5 × 5 to 10 s" },
          { name: "Negatives from an inverted hang", dose: "3 × 3" },
          { name: "Dead hang", dose: "2 × 30 s" },
        ],
      },
    ],
    weekNote: "Stop each hold before your arms bend or your hips drop. A clean 6 seconds beats a messy 12.",
    faq: [
      {
        q: "Is the front lever harder than the back lever?",
        a: "For most people, yes. The back lever lets the chest and shoulders share the load, while the front lever leans hard on the lats and core. Plenty of people learn the back lever first, but you don't need it to start front lever training.",
      },
      {
        q: "Should I train the front lever on a bar or rings?",
        a: "Either works. A bar is easier to find and grip. Rings let your hands settle at their own angle, which some people find kinder on the elbows. Use whichever you can train on most often.",
      },
      {
        q: "How often should I train the front lever?",
        a: "Two or three times a week is a common rhythm. Long straight-arm holds are hard on the elbows and shoulders, so leave a day between sessions and end each set before your form breaks.",
      },
      {
        q: "Do I need weights for the front lever?",
        a: "No. Your body position is the weight. Moving from a tuck to a straddle to a full lever makes the lever longer, which makes it harder without adding any load.",
      },
    ],
    plan: {
      title: "The front lever plan",
      file: "/plans/levitx-front-lever-plan.pdf",
      pitch: "Five stages from tuck to full as a checklist, a three-day week and a four-week log, on two A4 pages.",
    },
  },
  {
    slug: "planche",
    skill: "Planche",
    title: "How to Do a Planche: Progressions and Free Plan",
    description:
      "Learn the planche safely: wrist prep, six stages from lean to full with pass tests, the mistakes to avoid, a sample week and a free printable training plan.",
    h1: "How to do a planche",
    answer:
      "A planche is a straight-arm hold with your body level above the floor, held up by your hands alone. Your shoulders lean well past your wrists to balance it. It's one of the hardest bodyweight skills, so work through a lean, a tuck and a straddle, and look after your wrists from day one.",
    eyebrow: "Guide · Push",
    photo: "planche",
    photoPosition: "55% 40%",
    before: ["A wrist warm-up every session", "20 push-ups in a row", "A 30 second hollow body hold"],
    stages: [
      { name: "Planche lean", pass: "30 s, shoulders past the hands", cue: "Arms locked, hands turned out a little, lean until your shoulders are in front of your wrists." },
      { name: "Frog stand", pass: "30 s balanced", cue: "Knees resting on the backs of the arms, tip forward until your feet float." },
      { name: "Tuck planche", pass: "10 to 15 s with straight arms", cue: "Round the upper back, push the floor away, knees tucked and feet off." },
      { name: "Advanced tuck planche", pass: "10 s with a flat back", cue: "Open the hips until your back is flat, and lean further to stay balanced." },
      { name: "Straddle planche", pass: "5 to 10 s", cue: "Legs wide, hips level with the shoulders." },
      { name: "Full planche", pass: "5 s with legs together", cue: "Body in one straight line from the shoulders to the toes." },
    ],
    mistakes: [
      { title: "Bent elbows", fix: "The planche is a straight-arm hold, and that's where the strength comes from. If the elbows bend, go back a stage." },
      { title: "Not leaning far enough", fix: "Without enough lean, the feet won't lift. Your shoulders need to sit well in front of your wrists." },
      { title: "Shoulders sinking", fix: "Push the floor away so your upper back rounds slightly. Sinking between the shoulders makes every stage harder." },
      { title: "Skipping wrist prep", fix: "Your wrists carry the whole load. Warm them up every session and add volume slowly." },
    ],
    week: [
      {
        label: "Day 1",
        focus: "Lean and press",
        items: [
          { name: "Planche leans", dose: "5 × 20 to 30 s" },
          { name: "Pseudo planche push-ups", dose: "4 × 6 to 8" },
          { name: "Hollow body hold", dose: "3 × 30 s" },
        ],
      },
      {
        label: "Day 2",
        focus: "Holds",
        items: [
          { name: "Holds at your current stage", dose: "6 × 5 to 10 s" },
          { name: "Support hold on bars", dose: "3 × 30 s" },
          { name: "Pike push-ups", dose: "3 × 8" },
        ],
      },
      {
        label: "Day 3",
        focus: "Strength",
        items: [
          { name: "Planche leans", dose: "4 × 20 s" },
          { name: "Holds at your current stage", dose: "4 × 5 to 10 s" },
          { name: "Dips", dose: "3 × 8 to 10" },
        ],
      },
    ],
    weekNote: "Warm the wrists up before every session. If a wrist aches the next day, cut the volume and see how it settles.",
    callout: {
      title: "Wrists first, every time.",
      body: "Your wrists carry the whole planche. Before each session, circle them, rock gently forward on your palms and then your fingertips, and ease into a few light leans. Add volume slowly, and back off if a wrist aches the next day.",
    },
    faq: [
      {
        q: "How long does it take to learn a planche?",
        a: "Longer than most skills. The planche needs a lot of straight-arm pushing strength, and tendons adapt more slowly than muscles. Think in months for a solid tuck and longer for the straddle and full. Steady sessions beat heroic ones.",
      },
      {
        q: "Should I use parallettes for the planche?",
        a: "They help. Parallettes keep the wrists straighter and give you room to lean, and many people find tuck holds easier on them. The floor works too, as long as you warm your wrists up well.",
      },
      {
        q: "Can I train the planche every day?",
        a: "It's better not to. Straight-arm holds stress the wrists, elbows and shoulders, and they need time to adapt. Two or three sessions a week, with rest between, is a good place to start.",
      },
      {
        q: "Do resistance bands help with the planche?",
        a: "Yes, if you treat them as a step. A band under the hips takes some weight off so you can practise the full shape. Keep training the unassisted stage alongside it.",
      },
    ],
    plan: {
      title: "The planche plan",
      file: "/plans/levitx-planche-plan.pdf",
      pitch: "Six stages from lean to full as a checklist, a wrist-first week and a four-week log, on two A4 pages.",
    },
  },
  {
    slug: "handstand",
    skill: "Handstand",
    title: "How to Do a Handstand: Wall to Freestanding, Free Plan",
    description:
      "Learn a handstand from the wall to freestanding: how to bail safely, six stages with pass tests, common mistakes, a sample week and a free printable plan.",
    h1: "How to do a handstand",
    answer:
      "A handstand is balancing upside down on your hands with your body in one straight line. Start against a wall to build strength and shape, learn to bail out safely, then practise kicking up and balancing away from the wall with small corrections from your fingers.",
    eyebrow: "Guide · Balance",
    photo: "handstand",
    photoPosition: "86% 50%",
    before: ["A wrist warm-up every session", "A 30 second plank", "Clear space, or grass, around you"],
    stages: [
      { name: "Wall walk", pass: "30 s with hips over hands", cue: "Feet on the wall, walk your hands in until your body is nearly vertical." },
      { name: "Chest-to-wall handstand", pass: "60 s in a straight line", cue: "Face the wall, ribs in, push tall through the shoulders." },
      { name: "Bail out", pass: "Cartwheel out to each side, 5 times", cue: "Learn to fall before you need to: turn one hand and step down to the side." },
      { name: "Kick up to balance", pass: "5 kick-ups that hold for 3 s", cue: "Kick with a straight leg, gently. Most kick-ups are too hard." },
      { name: "Freestanding handstand", pass: "10 s", cue: "Press through the fingers to stop tipping over, the heels of the hands to stop falling back." },
      { name: "Long hold", pass: "30 s", cue: "Stay tall, keep breathing, make small corrections." },
    ],
    mistakes: [
      { title: "A banana back", fix: "An arched lower back. Squeeze the glutes and pull the ribs in. Chest-to-wall work fixes it." },
      { title: "Bent arms", fix: "Lock the elbows and push the floor away. Bent arms tire fast and wobble." },
      { title: "Kicking too hard", fix: "Overkicking sends you over the top. Kick gently and let the top leg float up." },
      { title: "Only training against the wall", fix: "The wall builds shape, not balance. Once bailing feels easy, add freestanding attempts every session." },
    ],
    week: [
      {
        label: "Day 1",
        focus: "Shape",
        items: [
          { name: "Chest-to-wall handstand", dose: "4 × 30 to 45 s" },
          { name: "Wall shoulder taps", dose: "3 × 10" },
          { name: "Hollow body hold", dose: "3 × 30 s" },
        ],
      },
      {
        label: "Day 2",
        focus: "Balance",
        items: [
          { name: "Kick-ups", dose: "10 minutes of attempts" },
          { name: "Heel pulls off the wall", dose: "5 × 5" },
          { name: "Bail practice", dose: "5 each side" },
        ],
      },
      {
        label: "Day 3",
        focus: "Both",
        items: [
          { name: "Chest-to-wall handstand", dose: "3 × 45 s" },
          { name: "Kick-ups", dose: "10 minutes of attempts" },
          { name: "Wrist push-ups", dose: "2 × 10" },
        ],
      },
    ],
    weekNote: "Handstands like short, frequent practice. A few minutes on most days works better than one long session a week.",
    callout: {
      title: "Learn to fall first.",
      body: "Before you kick up away from the wall, practise the bail: turn one hand out, twist your hips and step down to the side, like half a cartwheel. Once it's automatic, falling stops being scary and balance work gets much faster.",
    },
    faq: [
      {
        q: "How long does it take to learn a freestanding handstand?",
        a: "It's a balance skill, so once you can hold 60 seconds against the wall, practice time matters more than strength. Short daily sessions tend to beat one long weekly one. Expect weeks of attempts before your first 10 second hold.",
      },
      {
        q: "Should I practise chest to wall or back to wall?",
        a: "Chest to wall. It teaches the straight line you want and makes it hard to arch. Back to wall lets the lower back sag, which builds a habit you'll later have to undo.",
      },
      {
        q: "Is it safe to kick up into a handstand?",
        a: "Yes, once you know how to bail. Learn to cartwheel out to the side before freestanding work, and practise on grass or a mat with clear space around you.",
      },
      {
        q: "Where should I look in a handstand?",
        a: "At the floor between your hands. Looking far ahead pulls your head back and arches your spine, and tucking your head in tips you over.",
      },
    ],
    plan: {
      title: "The handstand plan",
      file: "/plans/levitx-handstand-plan.pdf",
      pitch: "Six stages from wall to freestanding as a checklist, a short-and-often week and a four-week log, on two A4 pages.",
    },
  },
];

export const guideHub = {
  title: "Calisthenics Skill Guides: Free Progressions and Plans",
  description:
    "Step-by-step guides to the muscle-up, front lever, planche and handstand, with pass tests for every stage and a free printable plan for each skill.",
  eyebrow: "Guides",
  h1: ["Four skills,", "one step at a time."],
  lede: "Each guide breaks a skill into stages with a clear test for each one, so you always know what to train next. Every guide comes with a free printable plan.",
};

export function getGuide(slug: string): Guide | undefined {
  return guides.find((guide) => guide.slug === slug);
}

export function isGuideSlug(value: unknown): value is GuideSlug {
  return typeof value === "string" && (GUIDE_SLUGS as readonly string[]).includes(value);
}
