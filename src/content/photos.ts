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

const UTM = "?utm_source=lever&utm_medium=referral";

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
  hero: {
    src: unsplash("photo-1758520705400-1edfdcd78cf5"),
    alt: "Man doing a pull-up on an outdoor bar",
    credit: unsplashCredit("Vitaly Gariev", "silverkblack"),
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
  duo: {
    src: unsplash("photo-1753889177521-2f2e0a9dea3c"),
    alt: "Two people practising handstands outdoors",
    credit: unsplashCredit("Mina Rad", "miinrad"),
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
