type Props = {
  id: string;
  eyebrow: string;
  /** One string, or a few lines when where the break falls matters. */
  title: string | readonly string[];
  className?: string;
};

/** A section's small label and its h2. The label names the topic; the h2 says one thing about it. */
export function SectionTitle({ id, eyebrow, title, className = "" }: Props) {
  return (
    <div className={className}>
      <p className="mono reveal text-[11px] text-accent">{eyebrow}</p>
      <h2 id={id} className="h2 reveal mt-6 text-balance">
        {typeof title === "string"
          ? title
          : title.map((line) => (
              <span key={line} className="block">
                {line}
              </span>
            ))}
      </h2>
    </div>
  );
}
