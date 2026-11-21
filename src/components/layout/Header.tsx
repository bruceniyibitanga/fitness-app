import Link from "next/link";
import { nav, site } from "@/content/site";
import { LogoMark } from "./Logo";
import { ThemeToggle } from "./ThemeToggle";

export function Header() {
  return (
    <header className="absolute inset-x-0 top-0 z-10">
      <nav aria-label="Main" className="gutter mx-auto flex min-h-[88px] max-w-[1200px] flex-wrap items-center justify-between gap-x-8 gap-y-3">
        <Link href="/" className="flex min-h-11 items-center gap-3">
          <span className="text-accent">
            <LogoMark />
          </span>
          <span className="mono text-[13px] font-medium tracking-[0.24em]">{site.name}</span>
        </Link>
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1">
          {nav.map((item) => (
            <Link key={item.href} href={item.href} className="mono hidden py-3.5 text-[11px] text-soft transition-colors hover:text-text md:inline">
              {item.label}
            </Link>
          ))}
          <ThemeToggle />
          <Link href="/#join" className="inline-flex min-h-11 items-center border border-text/25 px-5 text-sm font-medium transition-colors hover:border-text hover:bg-text hover:text-bg">
            Get early access
          </Link>
        </div>
      </nav>
    </header>
  );
}
