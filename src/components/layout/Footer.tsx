import Link from "next/link";
import { footer, site } from "@/content/site";
import { SOURCE_URLS, photoCredits, photoSources } from "@/content/photos";
import { LogoMark } from "./Logo";

export function Footer() {
  const credits = photoCredits();
  const sources = photoSources();
  return (
    <footer className="border-t border-line">
      <div className="gutter mx-auto flex max-w-[1320px] flex-wrap justify-between gap-x-8 gap-y-4 pt-10 pb-12">
        <div className="flex items-center gap-3">
          <span className="text-accent">
            <LogoMark size={18} />
          </span>
          <span className="mono text-[11px] tracking-[0.22em]">{site.name}</span>
          <span className="mono text-[11px] text-faint">· Built in {site.city}</span>
        </div>
        <nav aria-label="Footer" className="flex flex-wrap gap-x-6 gap-y-1">
          {footer.links.map((link) => (
            <Link key={link.href} href={link.href} className="mono py-3 text-[11px] text-muted hover:text-text">
              {link.label}
            </Link>
          ))}
        </nav>
        <p className="basis-full text-[13px] leading-relaxed text-faint">{footer.disclaimer}</p>
        <p className="mono basis-full text-[10px] leading-loose text-faint">
          Photos by{" "}
          {credits.map((c, i) => (
            <span key={c.name}>
              <a href={c.profile} rel="noopener noreferrer" target="_blank" className="hover:text-text">
                {c.name}
              </a>
              {i < credits.length - 1 ? " · " : " "}
            </span>
          ))}
          on{" "}
          {sources.map((source, i) => (
            <span key={source}>
              {i > 0 ? " and " : ""}
              <a href={SOURCE_URLS[source]} rel="noopener noreferrer" target="_blank" className="hover:text-text">
                {source}
              </a>
            </span>
          ))}
        </p>
      </div>
    </footer>
  );
}
