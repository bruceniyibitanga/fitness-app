import Link from "next/link";
import { footer, site } from "@/content/site";
import { LogoMark } from "./Logo";

/**
 * A closing spread: a short line on the left, two small columns of links on the right, the
 * name set very large across the page, and the fine print underneath. No second sign-up
 * button here, because the form sits directly above.
 */
export function Footer() {
  const year = new Date().getFullYear();
  return (
    <footer className="border-t border-line">
      <div className="gutter mx-auto max-w-[1200px] pt-[clamp(72px,10vw,128px)]">
        <div className="grid grid-cols-2 gap-x-6 gap-y-14 md:grid-cols-12">
          <div className="col-span-2 md:col-span-6">
            <p className="display text-[clamp(40px,4.6vw,64px)] leading-[0.98]">
              {footer.sign[0]}
              <br />
              <em className="italic">{footer.sign[1]}</em>
            </p>
            <p className="mono mt-8 text-[10px] text-muted">{site.platforms.join(" · ")} · Early access soon</p>
          </div>

          {footer.columns.map((column, i) => (
            <nav
              key={column.title}
              aria-label={column.title}
              className={`col-span-1 md:col-span-2 ${i === 0 ? "md:col-start-8" : ""}`}
            >
              <p className="mono text-[10px] text-muted">{column.title}</p>
              <ul className="mt-5 space-y-1">
                {column.links.map((link) => (
                  <li key={link.href}>
                    <Link href={link.href} className="inline-flex min-h-10 items-center text-[15px] text-soft transition-colors hover:text-text">
                      {link.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>

        {/* The name, set large as a sign-off. Decorative: the brand is named elsewhere. */}
        <p aria-hidden="true" className="display mt-[clamp(64px,9vw,120px)] select-none text-[clamp(112px,31vw,460px)] leading-[0.78] tracking-[-0.04em]">
          {site.name}
        </p>

        <div className="mt-10 grid gap-y-5 border-t border-line py-8 text-[13px] text-faint md:grid-cols-12 md:items-start">
          <p className="flex flex-wrap items-center gap-x-2 md:col-span-4">
            <span className="text-accent">
              <LogoMark size={14} />
            </span>
            © {year}{" "}
            <span>
              {footer.maker}{" "}
              <a href={site.publisher.url} rel="noopener" className="underline-offset-4 hover:text-text hover:underline">
                {site.publisher.name}
              </a>
            </span>
          </p>
          <p className="max-w-[560px] leading-relaxed md:col-span-7 md:col-start-6">{footer.disclaimer}</p>
        </div>
      </div>
    </footer>
  );
}
