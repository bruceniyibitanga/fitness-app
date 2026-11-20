/**
 * Every photo on the site, in one place. Swap a URL here and it changes everywhere.
 *
 * Photos come from Unsplash and Pexels, both free licences. Unsplash's guidelines ask
 * for hotlinked URLs and a credit linking to the photographer, which the footer renders.
 * Pexels doesn't require credit, but we give it anyway.
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

function pexels(id: number): string {
  return `https://images.pexels.com/photos/${id}/pexels-photo-${id}.jpeg?cs=srgb&fm=jpg`;
}

function unsplashCredit(name: string, handle: string) {
  return { name, profile: `https://unsplash.com/@${handle}${UTM}`, source: "Unsplash" as const };
}

function pexelsCredit(name: string, handle: string) {
  return { name, profile: `https://www.pexels.com/@${handle}/`, source: "Pexels" as const };
}

export const photos = {
  heroDusk: {
    src: unsplash("photo-1691533539807-7666752914ff"),
    alt: "Person holding a handstand on a beach at dusk",
    credit: unsplashCredit("Tobias Pfeifer", "tobi_sap"),
  },
  lightWall: {
    src: unsplash("photo-1649429479266-ea5f862c3e73"),
    alt: "Man holding a handstand in front of a wall of lights at night",
    credit: unsplashCredit("Jerrin James", "jerrinjms"),
  },
  pullUpIndoor: {
    src: unsplash("photo-1772450014504-1914f8afe985"),
    alt: "Woman doing pull-ups in a gym",
    credit: unsplashCredit("Rodrigo Rodrigues", "wolfart32"),
  },
  muscleUp: {
    src: pexels(4803687),
    alt: "Man doing a muscle-up on a bar",
    credit: pexelsCredit("Ketut Subiyanto", "ketut-subiyanto"),
  },
  frontLever: {
    src: unsplash("photo-1647871071971-6688017ee28a"),
    alt: "Man training on a pull-up bar",
    credit: unsplashCredit("Salah Regouane", "salaheregouane"),
  },
  planche: {
    src: unsplash("photo-1516208813382-cbaf53501037"),
    alt: "Man holding a planche on a bar",
    credit: unsplashCredit("GMB Fitness", "gmb"),
  },
  rings: {
    src: unsplash("photo-1772450014180-39e8de531a57"),
    alt: "Wooden gymnastic rings hanging from straps",
    credit: unsplashCredit("Rodrigo Rodrigues", "wolfart32"),
  },
  handstand: {
    src: unsplash("photo-1662795855010-a595a2ba6265"),
    alt: "Person holding a handstand on a dirt path",
    credit: unsplashCredit("Till Daling", "tilldaling"),
  },
  resting: {
    src: unsplash("photo-1530821875964-91927b611bad"),
    alt: "Athlete sitting on a ramp, catching his breath between sets",
    credit: unsplashCredit("John Fornander", "johnfo"),
  },
  park: {
    src: unsplash("photo-1775850576265-865f60386102"),
    alt: "People training on outdoor gym equipment",
    credit: unsplashCredit("Nadin Nandin", "nadin_nandin"),
  },
  mountain: {
    src: unsplash("photo-1761539157219-bfc5b02cdb36"),
    alt: "Person holding a handstand on a mountain path",
    credit: unsplashCredit("Speedy Sandy", "fiftymm"),
  },
  winter: {
    src: unsplash("photo-1758521959675-5874879f3977"),
    alt: "Man doing a pull-up in a park in winter",
    credit: unsplashCredit("Vitaly Gariev", "silverkblack"),
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
