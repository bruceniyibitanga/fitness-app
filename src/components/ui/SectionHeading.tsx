type Props = {
  id: string;
  number: string;
  label: string;
  title: string;
  /** Turn off the scroll reveal, e.g. inside a sticky column. */
  animate?: boolean;
  className?: string;
};

/** The "01 / LABEL" line plus the section's h2. Used by every numbered section. */
export function SectionHeading({ id, number, label, title, animate = true, className = "" }: Props) {
  const reveal = animate ? "reveal" : "";
  return (
    <div className={className}>
      <p className={`eyebrow ${reveal}`}>
        <span>
          <b className="font-medium text-accent">{number}</b> / {label}
        </span>
      </p>
      <h2 id={id} className={`h2 mt-5 ${reveal}`}>
        {title}
      </h2>
    </div>
  );
}
