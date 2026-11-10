/**
 * Structured data for search engines. `<` is escaped so a stray string can never close
 * the script tag (the Next.js JSON-LD guide's recommendation).
 */
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, "\\u003c") }} />;
}
