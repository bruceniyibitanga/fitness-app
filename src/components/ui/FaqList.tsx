type Props = {
  items: { q: string; a: string }[];
  /** Groups the details elements so opening one closes the others. Unique per page. */
  name: string;
  className?: string;
};

/**
 * Native details/summary: keyboard and screen-reader friendly with no JavaScript. Every
 * answer is in the HTML even when closed, so search engines and AI answers can read it.
 */
export function FaqList({ items, name, className = "" }: Props) {
  return (
    <div className={`border-t border-line ${className}`}>
      {items.map((item) => (
        <details key={item.q} name={name} className="group border-b border-line">
          <summary className="flex min-h-[76px] cursor-pointer list-none items-center justify-between gap-6 py-4 text-lg font-medium [&::-webkit-details-marker]:hidden">
            <h3>{item.q}</h3>
            <svg width="16" height="16" viewBox="0 0 16 16" aria-hidden="true" className="shrink-0 text-muted transition-transform duration-300 group-open:rotate-45">
              <path d="M3 8h10M8 3v10" stroke="currentColor" strokeWidth="1.6" strokeLinecap="square" />
            </svg>
          </summary>
          <p className="max-w-[560px] pr-10 pb-7 text-base leading-relaxed text-muted">{item.a}</p>
        </details>
      ))}
    </div>
  );
}
