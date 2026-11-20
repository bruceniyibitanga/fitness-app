import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export const metadata: Metadata = {
  title: "Privacy · Levitx",
  description: "What the Levitx app and this website keep, where, and how to take it with you or delete it.",
  alternates: { canonical: "/privacy" },
};

// This is the privacy policy Google Play links to, so it has to match what the
// apps actually do. When a feature changes what the app keeps or sends, change
// this page in the same pull request, and the date below with it.
const updated = "16 November 2026";

type Section = { title: string; body: string[] };

const app: Section[] = [
  {
    title: "What the app keeps",
    body: [
      "Your training: the workouts you do, each set's reps or seconds, how hard it felt, any vest, belt or band, and your bests. Your goal and the plan the coach makes from it. Your kit and bands. Your bodyweight, if you choose to enter it. And a few settings, like the count-in and whether timers beep.",
      "All of it is stored on your phone only, in a database encrypted with a key that Android keeps for Levitx. There is no account, no Levitx server, no analytics, no ads and no crash reporting.",
    ],
  },
  {
    title: "Your watch",
    body: [
      "If you use the Levitx watch app, your phone sends it your goal, your bests, your bands and your next session, and the watch sends finished workouts back. Your bodyweight never goes to the watch.",
      "They talk through Google Play services. Usually that's Bluetooth. When it isn't available, the data goes through Google's servers, end-to-end encrypted, so Google can't read it.",
      "The watch shows your heart rate during a session if you allow it. Heart rate isn't saved or sent anywhere.",
    ],
  },
  {
    title: "Notifications and the lock screen",
    body: [
      "During a workout, a notification shows the exercise, the set and the rest or hold clock, including on the lock screen. It never shows reps, loads or your weight. You can turn it off in Android's settings, which the app's Settings links to.",
    ],
  },
  {
    title: "Permissions",
    body: [
      "Notifications, for that workout notification. On the watch, heart rate. The rest are granted when you install and keep a workout running with the screen off: a foreground service, a wake lock, vibration, and a sensor permission Android asks fitness apps to hold for that. Levitx doesn't read any phone sensors.",
    ],
  },
  {
    title: "Backups",
    body: [
      "Android's automatic backup is turned off for Levitx, so your data isn't copied to Google Drive or to a new phone. To keep a copy, use the export below.",
    ],
  },
  {
    title: "Taking it with you, and deleting it",
    body: [
      "Settings has Export everything, which saves all of your data as a zip of JSON and CSV files wherever you choose. The file isn't encrypted, so keep it somewhere private.",
      "You can delete a single workout from History, or delete everything from Settings. When you delete everything, your watch deletes its copy too, the next time it's connected to your phone. Uninstalling the app also deletes everything on the phone, but not on the watch, so clear or uninstall Levitx there as well.",
    ],
  },
  {
    title: "Who we are, and questions",
    body: [
      "Levitx is made by Xenova Digital (xenovadigital.com). For anything about your data or this policy, email support@xenovadigital.com.",
    ],
  },
];

const website: Section[] = [
  {
    title: "What we collect",
    body: [
      "If you join the early access list, or download a free plan from one of our guides, we keep your email address and, for a plan, which guide it was for. That's it. This site doesn't use analytics, ad trackers or third-party cookies. If you switch between light and dark, one small cookie remembers that choice. It holds only the word light or dark, and nothing about you.",
    ],
  },
  {
    title: "What we use it for",
    body: [
      "To email you when early access opens and to send occasional updates about Levitx. We don't sell or share your email.",
    ],
  },
  {
    title: "Where it's stored",
    body: [
      "With Plunk (useplunk.com), the email service we use to send updates. Our server sends it there over HTTPS. Plunk stores it under its own privacy policy, and may store it outside Australia.",
    ],
  },
  {
    title: "Removing it",
    body: [
      "Every email has an unsubscribe link. Reply to any email, or write to support@xenovadigital.com, and we'll delete your address completely.",
    ],
  },
];

function Sections({ heading, sections }: { heading: string; sections: Section[] }) {
  return (
    <>
      <h2 className="mono mt-16 text-xs text-muted">{heading}</h2>
      <div className="mt-4 border-t border-line">
        {sections.map((s) => (
          <section key={s.title} className="border-b border-line py-8">
            <h3 className="text-xl font-semibold">{s.title}</h3>
            {s.body.map((paragraph) => (
              <p key={paragraph} className="mt-3 text-[17px] leading-relaxed text-muted">
                {paragraph}
              </p>
            ))}
          </section>
        ))}
      </div>
    </>
  );
}

export default function PrivacyPage() {
  return (
    <>
      <Header />
      <main className="gutter mx-auto max-w-[760px] pt-40 pb-24">
        <p className="mono text-xs text-muted">Privacy</p>
        <h1 className="h2 mt-5">Your training stays on your phone.</h1>
        <p className="mt-6 text-[17px] leading-relaxed text-muted">Last updated {updated}.</p>
        <Sections heading="The Levitx app" sections={app} />
        <Sections heading="This website" sections={website} />
      </main>
      <Footer />
    </>
  );
}
