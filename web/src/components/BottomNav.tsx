'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import clsx from 'clsx';

import { usePlayerStore } from '@/store/usePlayerStore';

import { HomeIcon, LibraryIcon, SearchIcon } from './icons';
import { MiniPlayer } from './MiniPlayer';

/** The three tabs from ui/navigation/Routes.kt that carry an icon and label. */
const TABS = [
  { href: '/', label: 'Home', Icon: HomeIcon },
  { href: '/search', label: 'Search', Icon: SearchIcon },
  { href: '/library', label: 'Your Library', Icon: LibraryIcon },
] as const;

export function BottomNav() {
  const pathname = usePathname();
  const hasTrack = usePlayerStore((s) => s.track !== null);

  return (
    // Brush.verticalGradient(Transparent -> Black -> Black) so content scrolls under.
    <div className="pointer-events-none fixed inset-x-0 bottom-0 z-30 bg-gradient-to-b from-transparent via-[#130824]/95 to-[#0C0518] pt-6">
      <div className="pointer-events-auto">
        {hasTrack ? <MiniPlayer /> : null}
        <nav
          className="mt-[10px] flex items-center justify-around px-[30px] pb-[max(env(safe-area-inset-bottom),8px)] pt-2"
          aria-label="Primary"
        >
          {TABS.map(({ href, label, Icon }) => {
            const active = href === '/' ? pathname === '/' : pathname.startsWith(href);
            return (
              <Link
                key={href}
                href={href}
                aria-current={active ? 'page' : undefined}
                className={clsx(
                  'flex flex-col items-center gap-1 py-1 transition-colors',
                  active ? 'text-gold-400' : 'text-[#B4A8D6] hover:text-white/80',
                )}
              >
                <Icon size={24} />
                <span className="text-[11px] leading-none">{label}</span>
              </Link>
            );
          })}
        </nav>
      </div>
    </div>
  );
}
