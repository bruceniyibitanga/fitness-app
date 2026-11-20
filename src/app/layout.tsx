import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { cookies } from "next/headers";
import { connection } from "next/server";
import { site } from "@/content/site";
import { SmoothScroll } from "@/components/motion/SmoothScroll";
import { THEME_COLOR, THEME_COOKIE, parseTheme } from "@/lib/theme";
import "./globals.css";

// Self-hosted (SIL Open Font License), so no request goes to Google and the CSP stays at font-src 'self'.
const archivo = localFont({
  src: "../fonts/archivo-wdth.woff2",
  variable: "--font-archivo",
  weight: "100 900",
  display: "swap",
  declarations: [{ prop: "font-stretch", value: "62% 125%" }],
});

// Instrument Serif (SIL Open Font License) for headlines: one weight and its italic.
const serif = localFont({
  src: [
    { path: "../fonts/instrument-serif.woff2", weight: "400", style: "normal" },
    { path: "../fonts/instrument-serif-italic.woff2", weight: "400", style: "italic" },
  ],
  variable: "--font-instrument",
  display: "swap",
});

const jetbrains = localFont({
  src: "../fonts/jetbrains-mono.woff2",
  variable: "--font-jetbrains",
  weight: "100 800",
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(site.url),
  title: site.title,
  description: site.description,
  applicationName: site.name,
  publisher: site.publisher.name,
  creator: site.publisher.name,
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    url: "/",
    siteName: site.name,
    title: site.title,
    description: site.description,
    locale: "en_AU",
  },
  twitter: { card: "summary_large_image", title: site.title, description: site.description },
  robots: { index: true, follow: true },
};

/** The visitor's saved theme, or light. Reading the cookie is cheap and never throws. */
async function currentTheme() {
  return parseTheme((await cookies()).get(THEME_COOKIE)?.value);
}

export async function generateViewport(): Promise<Viewport> {
  const theme = await currentTheme();
  return { themeColor: THEME_COLOR[theme], colorScheme: theme };
}

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Render per request so each page gets its own CSP nonce (set in src/proxy.ts).
  await connection();
  const theme = await currentTheme();

  return (
    <html
      lang="en-AU"
      data-theme={theme}
      style={{ colorScheme: theme }}
      className={`${archivo.variable} ${serif.variable} ${jetbrains.variable}`}
    >
      <body>
        <SmoothScroll />
        {children}
      </body>
    </html>
  );
}
