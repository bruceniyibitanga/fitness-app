import { calculator, coach, faq, path, privacy, site, skills } from "@/content/site";
import { guides } from "@/content/guides";

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
    `## ${coach.eyebrow}`,
    `${coach.title.join(" ")} ${coach.body}`,
    ...coach.moments.map((m) => `- ${m.what}: ${m.body}`),
    ...coach.builtIn.items.map((i) => `- ${i.title}: ${i.body}`),
    coach.note,
    "",
    "## How it works",
    ...path.steps.map((s) => `- ${s.title}: ${s.body}`),
    "",
    "## Skills",
    ...skills.items.map((s) => `- ${s.name}: ${s.range.toLowerCase()}`),
    "",
    "## Skill guides",
    ...guides.flatMap((g) => [
      `### [${g.h1}](${site.url}/guides/${g.slug})`,
      g.answer,
      ...g.stages.map((s, i) => `${i + 1}. ${s.name}: pass with ${s.pass}.`),
      "",
    ]),
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
    `- [Skill guides](${site.url}/guides)`,
    `- [Privacy policy](${site.url}/privacy)`,
    `- [${site.publisher.name}](${site.publisher.url})`,
    "",
  ];
  return lines.join("\n");
}
