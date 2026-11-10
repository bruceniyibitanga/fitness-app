import type { Metadata } from "next";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";

export const metadata: Metadata = {
  title: "Privacy · LEVER",
  description: "What LEVER collects on this website and what we do with it.",
  alternates: { canonical: "/privacy" },
};

// Starting point for the pre-launch site. Have it reviewed before launch,
// and rewrite it once the app collects training or health data.
const sections = [
  {
    title: "What we collect",
    body: "If you join the early access list, we keep your email address. That's it. This site doesn't use analytics, ad trackers or third-party cookies.",
  },
  {
    title: "What we use it for",
    body: "To email you when early access opens and to send occasional updates about LEVER. We don't sell or share your email.",
  },
  {
    title: "Where it's stored",
    body: "With Kit (kit.com), the email service we use to send updates. Our server sends it there over HTTPS. Kit stores it under its own privacy policy.",
  },
  {
    title: "Removing it",
    body: "Every email has an unsubscribe link. Reply to any email and we'll delete your address completely.",
  },
];

export default function PrivacyPage() {
  return (
    <>
      <Header />
      <main className="gutter mx-auto max-w-[760px] pt-40 pb-24">
        <p className="mono text-xs text-muted">Privacy</p>
        <h1 className="h2 mt-5">Your email, and nothing else.</h1>
        <div className="mt-12 border-t border-line">
          {sections.map((s) => (
            <section key={s.title} className="border-b border-line py-8">
              <h2 className="text-xl font-semibold">{s.title}</h2>
              <p className="mt-3 text-[17px] leading-relaxed text-muted">{s.body}</p>
            </section>
          ))}
        </div>
      </main>
      <Footer />
    </>
  );
}
