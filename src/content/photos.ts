/**
 * Every photo on the site, in one place. Swap a URL here and it changes everywhere.
 *
 * Photos come from Unsplash under its free licence, hotlinked as Unsplash asks. Each photo's
 * download was recorded through the Unsplash API when it was picked. Credit isn't required,
 * so the site doesn't show it, but each entry keeps its photographer as a record.
 */
export type PhotoSource = "Unsplash";

export type Photo = {
  src: string;
  alt: string;
  credit: { name: string; profile: string; source: PhotoSource };
};

const UTM = "?utm_source=levitx&utm_medium=referral";

function unsplash(path: string): string {
  return `https://images.unsplash.com/${path}?ixlib=rb-4.1.0&fm=jpg&q=80&crop=entropy&cs=srgb`;
}

function unsplashCredit(name: string, handle: string) {
  return { name, profile: `https://unsplash.com/@${handle}${UTM}`, source: "Unsplash" as const };
}

export const photos = {
  hero: {
    src: unsplash("photo-1656744175229-d39a3a61e75d"),
    alt: "Man holding a one-arm handstand in a bright white room",
    credit: unsplashCredit("Vikas Deswal", "deswalvikas29"),
  },
  path: {
    src: unsplash("photo-1526314149856-d8cf115d62f1"),
    alt: "Man holding a handstand on parallel bars at an outdoor gym by the beach",
    credit: unsplashCredit("Kevin McCutcheon", "kevinmccutcheon"),
  },
  adapts: {
    src: unsplash("photo-1758520705400-1edfdcd78cf5"),
    alt: "Man resting between sets at an outdoor pull-up bar",
    credit: unsplashCredit("Vitaly Gariev", "silverkblack"),
  },
  muscleUp: {
    src: unsplash("photo-1586178486070-57d66f5347fd"),
    alt: "Man pulling his chest over the bar in a muscle-up",
    credit: unsplashCredit("Kate Trysh", "katetrysh"),
  },
  frontLever: {
    src: unsplash("photo-1595554919371-7e38f2558855"),
    alt: "Man holding a front lever on gymnastic rings in a park",
    credit: unsplashCredit("GMB Fitness", "gmb"),
  },
  planche: {
    src: unsplash("photo-1516208813382-cbaf53501037"),
    alt: "Man holding a planche on playground bars",
    credit: unsplashCredit("GMB Fitness", "gmb"),
  },
  handstand: {
    src: unsplash("photo-1586930713012-7059dc74a72b"),
    alt: "Woman holding a handstand against a white wall",
    credit: unsplashCredit("Nathaniel Vala", "spydernaz"),
  },
  watch: {
    src: unsplash("photo-1561212942-0d77ee345b35"),
    alt: "Fitness band on a wrist, mountains in the distance",
    credit: unsplashCredit("FitNish Media", "fitnish"),
  },
  comeback: {
    src: unsplash("photo-1515614726597-e686ac6dbfde"),
    alt: "Man hanging from a bar at sunset",
    credit: unsplashCredit("Alexei Scutari", "scutal"),
  },
  lighter: {
    src: unsplash("photo-1733077151425-67af89045a9f"),
    alt: "Woman stretching on the floor in soft daylight",
    credit: unsplashCredit("Logan Weaver", "lgnwvr"),
  },
  join: {
    src: unsplash("photo-1573858129585-6ca05f716932"),
    alt: "Man kicking up to a handstand beside a concrete wall",
    credit: unsplashCredit("Eduardo Cano Photo Co.", "eduardocanophotoco"),
  },
} satisfies Record<string, Photo>;

export type PhotoKey = keyof typeof photos;
