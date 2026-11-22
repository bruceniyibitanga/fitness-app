type Props = {
  /** The h2's id, for the section's aria-labelledby. */
  id: string;
  /** The section's place on the page, like "01". The page passes it, so reordering stays one edit. */
  n: string;
  label: string;
  title: string;
  className?: string;
};

/** A home page section's numbered label ("01 / Meet Levi", with a rule to the edge) and its h2. */
export function NumberedTitle({ id, n, label, title, className = "" }: Props) {
  return (
    <div className={className}>
      <p className="eyebrow reveal">
        <b>{n}</b> / {label}
      </p>
      <h2 id={id} className="headline reveal mt-5 text-balance">
        {title}
      </h2>
    </div>
  );
}
