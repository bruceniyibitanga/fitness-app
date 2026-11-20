/**
 * Every photo on the site, in one place. Swap a URL here and it changes everywhere.
 *
 * Photos come from Unsplash under its free licence. Its guidelines ask for hotlinked URLs
 * and a credit linking to the photographer, which the footer renders. Each photo's download
 * was recorded through the Unsplash API when it was picked, as the guidelines also ask.
 */
export type PhotoSource = "Unsplash" | "Pexels";

export type Photo = {
  src: string;
  alt: string;
  credit: { name: string; profile: string; source: PhotoSource };
};

const UTM = "?utm_source=levitx&utm_medium=referral";

export const SOURCE_URLS: Record<PhotoSource, string> = {
  Unsplash: `https://unsplash.com/${UTM}`,
  Pexels: "https://www.pexels.com/",
};

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
    src: unsplash("photo-1631645789778-b4cf1e791801"),
    alt: "Athlete flying level through a long concrete car park",
    credit: unsplashCredit("Fotis Fotopoulos", "ffstop"),
  },
  adapts: {
    src: unsplash("photo-1758520705400-1edfdcd78cf5"),
    alt: "Man resting between sets at an outdoor pull-up bar",
    credit: unsplashCredit("Vitaly Gariev", "silverkblack"),
  },
  muscleUp: {
    src: unsplash("photo-1581792618071-6b7af43985bb"),
    alt: "Athlete at the top of a bar against a pale sky",
    credit: unsplashCredit("Cozy Dreams", "blagov"),
  },
  frontLever: {
    src: unsplash("photo-1701826510552-aadcfa841065"),
    alt: "Man pulling on gymnastic rings in a bright room",
    credit: unsplashCredit("GMB Fitness", "gmb"),
  },
  planche: {
    src: unsplash("photo-1546375448-06761090185a"),
    alt: "Athlete leaning forward over his hands in a support hold",
    credit: unsplashCredit("Bogdan Pasca", "bogdipasca"),
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
  rest: {
    src: unsplash("photo-1775850576183-a7671ba5dc46"),
    alt: "People taking it easy at an outdoor fitness park",
    credit: unsplashCredit("Nadin Nandin", "nadin_nandin"),
  },
  join: {
    src: unsplash("photo-1573858129585-6ca05f716932"),
    alt: "Man kicking up to a handstand beside a concrete wall",
    credit: unsplashCredit("Eduardo Cano Photo Co.", "eduardocanophotoco"),
  },
} satisfies Record<string, Photo>;

export type PhotoKey = keyof typeof photos;

/** Unique photographers, for the footer credit line. */
export function photoCredits() {
  const seen = new Map<string, Photo["credit"]>();
  for (const photo of Object.values(photos)) seen.set(photo.credit.name, photo.credit);
  return [...seen.values()];
}

/** The photo sites actually in use, so the footer only names the ones we rely on. */
export function photoSources(): PhotoSource[] {
  return [...new Set(Object.values(photos).map((p) => p.credit.source))];
}
