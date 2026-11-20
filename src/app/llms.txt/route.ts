import { llmsText } from "@/lib/llms";

// Built once at deploy time: the text only changes when the site copy does.
export const dynamic = "force-static";

/** /llms.txt: the site in plain text, for AI assistants. */
export function GET() {
  return new Response(llmsText(), {
    headers: { "Content-Type": "text/plain; charset=utf-8" },
  });
}
