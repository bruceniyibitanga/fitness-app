import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import { connection } from "next/server";
import { site } from "@/content/site";
import { SmoothScroll } from "@/components/motion/SmoothScroll";
import "./globals.css";

// Self-hosted (SIL Open Font License), so no request goes to Google and the CSP stays at font-src 'self'.
const archivo = localFont({
  src: "../fonts/archivo-wdth.woff2",
  variable: "--font-archivo",
  weight: "100 900",
  display: "swap",
  declarations: [{ prop: "font-stretch", value: "62% 125%" }],
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

export const viewport: Viewport = {
  themeColor: "#0b0b0a",
  colorScheme: "dark",
};

export default async function RootLayout({ children }: LayoutProps<"/">) {
  // Render per request so each page gets its own CSP nonce (set in src/proxy.ts).
  await connection();

  return (
    <html lang="en-AU" className={`${archivo.variable} ${jetbrains.variable}`}>
      <body>
        <SmoothScroll />
        {children}
      </body>
    </html>
  );
}
