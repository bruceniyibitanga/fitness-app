/**
 * Every photo on the site, in one place. Swap a URL here and it changes everywhere.
 * All from Unsplash (free licence). Their guidelines ask for hotlinked URLs and a
 * credit linking to the photographer and Unsplash, which the footer renders.
 */
export type Photo = {
  src: string;
  alt: string;
  credit: { name: string; profile: string };
};

const UTM = "?utm_source=lever&utm_medium=referral";

function unsplash(path: string): string {
  return `https://images.unsplash.com/${path}?ixlib=rb-4.1.0&fm=jpg&q=80&crop=entropy&cs=srgb`;
}

function credit(name: string, handle: string) {
  return { name, profile: `https://unsplash.com/@${handle}${UTM}` };
}

export const photos = {
  hero: {
    src: unsplash("photo-1758520705400-1edfdcd78cf5"),
    alt: "Man doing a pull-up on an outdoor bar",
    credit: credit("Vitaly Gariev", "silverkblack"),
  },
  pullUpIndoor: {
    src: unsplash("photo-1772450014504-1914f8afe985"),
    alt: "Woman doing pull-ups in a gym",
    credit: credit("Rodrigo Rodrigues", "wolfart32"),
  },
  barMono: {
    src: unsplash("photo-1526506118085-60ce8714f8c5"),
    alt: "Black and white photo of an athlete training on a bar",
    credit: credit("Edgar Chaparro", "echaparro"),
  },
  rings: {
    src: unsplash("photo-1772450014180-39e8de531a57"),
    alt: "Wooden gymnastic rings hanging from straps",
    credit: credit("Rodrigo Rodrigues", "wolfart32"),
  },
  handstand: {
    src: unsplash("photo-1662795855010-a595a2ba6265"),
    alt: "Person holding a handstand on a dirt path",
    credit: credit("Till Daling", "tilldaling"),
  },
  ringsArcher: {
    src: unsplash("photo-1701826510552-aadcfa841065"),
    alt: "Man doing archer pull-ups on gymnastic rings",
    credit: credit("GMB Fitness", "gmb"),
  },
  ringsChinUp: {
    src: unsplash("photo-1516208962313-9d183d94f577"),
    alt: "Woman doing a chin-up on gymnastic rings",
    credit: credit("GMB Fitness", "gmb"),
  },
  park: {
    src: unsplash("photo-1775850576265-865f60386102"),
    alt: "People training on outdoor gym equipment",
    credit: credit("Nadin Nandin", "nadin_nandin"),
  },
  mountain: {
    src: unsplash("photo-1761539157219-bfc5b02cdb36"),
    alt: "Person holding a handstand on a mountain path",
    credit: credit("Speedy Sandy", "fiftymm"),
  },
  winter: {
    src: unsplash("photo-1758521959675-5874879f3977"),
    alt: "Man doing a pull-up in a park in winter",
    credit: credit("Vitaly Gariev", "silverkblack"),
  },
  duo: {
    src: unsplash("photo-1753889177521-2f2e0a9dea3c"),
    alt: "Two people practising handstands outdoors",
    credit: credit("Mina Rad", "miinrad"),
  },
} satisfies Record<string, Photo>;

export type PhotoKey = keyof typeof photos;

export const UNSPLASH_URL = `https://unsplash.com/${UTM}`;

/** Unique photographers, for the footer credit line. */
export function photoCredits() {
  const seen = new Map<string, string>();
  for (const photo of Object.values(photos)) seen.set(photo.credit.name, photo.credit.profile);
  return [...seen].map(([name, profile]) => ({ name, profile }));
}
