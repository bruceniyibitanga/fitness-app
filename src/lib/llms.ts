import { calculator, faq, how, privacy, site, skills } from "@/content/site";

/**
 * A plain-text summary of Levitx for AI assistants and crawlers that read /llms.txt
 * (the proposal at llmstxt.org). Built from the same copy as the page, so the two can't drift.
 */
export function llmsText(): string {
  const lines = [
    `# ${site.name}`,
    "",
    `> ${site.description}`,
    "",
    `${site.name} is made by ${site.publisher.name} in ${site.publisher.city}, Australia. It runs on ${site.platforms.join(" and ")}.`,
    "",
    "## How it works",
    ...how.steps.map((s) => `- ${s.title}: ${s.body}`),
    "",
    "## Skills",
    ...skills.items.map((s) => `- ${s.name}: ${s.range.toLowerCase()}`),
    "",
    "## Privacy",
    `${privacy.title} ${privacy.body}`,
    "",
    `## ${calculator.title}`,
    calculator.answer,
    `Source: ${calculator.source}`,
    "",
    "## FAQ",
    ...faq.items.flatMap((item) => [`### ${item.q}`, item.a, ""]),
    "## Links",
    `- [Home](${site.url}/)`,
    `- [Push-up weight calculator](${site.url}/#calculator)`,
    `- [Privacy policy](${site.url}/privacy)`,
    `- [${site.publisher.name}](${site.publisher.url})`,
    "",
  ];
  return lines.join("\n");
}
