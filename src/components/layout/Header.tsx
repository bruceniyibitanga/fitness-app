import Link from "next/link";
import { nav, site } from "@/content/site";
import { LogoMark } from "./Logo";

export function Header() {
  return (
    <header className="absolute inset-x-0 top-0 z-10">
      <nav aria-label="Main" className="gutter mx-auto flex min-h-[76px] max-w-[1320px] flex-wrap items-center justify-between gap-x-8 gap-y-3">
        <Link href="/" className="flex min-h-11 items-center gap-3">
          <span className="text-accent">
            <LogoMark />
          </span>
          <span className="mono text-[13px] font-medium tracking-[0.24em]">{site.name}</span>
        </Link>
        <div className="flex flex-wrap items-center gap-x-7 gap-y-1">
          {nav.map((item) => (
            <Link key={item.href} href={item.href} className="mono hidden py-3.5 text-[11px] text-muted transition-colors hover:text-text md:inline">
              {item.label}
            </Link>
          ))}
          <Link href="/#join" className="inline-flex min-h-11 items-center bg-accent px-5 text-sm font-bold text-bg transition-colors hover:bg-accent-hover">
            Get early access
          </Link>
        </div>
      </nav>
    </header>
  );
}
