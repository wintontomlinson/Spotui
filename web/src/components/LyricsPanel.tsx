'use client';

import { useEffect, useMemo, useRef } from 'react';
import clsx from 'clsx';

import { activeLineIndex } from '@/lib/lrc';
import { seekTo } from '@/lib/playback';
import { useLyrics } from '@/hooks/useLyrics';
import { usePlayerStore } from '@/store/usePlayerStore';

import { ChevronDownIcon } from './icons';

/**
 * Full-screen lyrics sheet, mirroring ui/screens/LyricsScreen.kt: a gradient
 * from the artwork accent into black, the active line highlighted against dimmed
 * neighbours, and tapping a synced line seeks to it.
 */
export function LyricsPanel() {
  const track = usePlayerStore((s) => s.track);
  const positionMs = usePlayerStore((s) => s.positionMs);
  const palette = usePlayerStore((s) => s.palette);
  const setLyricsOpen = usePlayerStore((s) => s.setLyricsOpen);

  const { lyrics, isLoading } = useLyrics(
    track?.name,
    track?.artistNames,
    track?.albumName,
    track?.durationMs,
  );

  const activeIndex = useMemo(() => {
    if (!lyrics?.synced) return -1;
    return activeLineIndex(lyrics.lines, positionMs);
  }, [lyrics, positionMs]);

  const activeRef = useRef<HTMLButtonElement | null>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, [activeIndex]);

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col"
      style={{
        backgroundImage: `linear-gradient(to bottom, ${palette.muted} 0%, ${palette.muted}8C 45%, #000000 100%)`,
        backgroundColor: '#121212',
      }}
      role="dialog"
      aria-label="Lyrics"
    >
      <header className="flex items-center gap-3 px-4 pb-2 pt-[max(env(safe-area-inset-top),12px)]">
        <button
          type="button"
          onClick={() => setLyricsOpen(false)}
          aria-label="Close lyrics"
          className="p-1 text-white transition-opacity hover:opacity-70"
        >
          <ChevronDownIcon size={28} />
        </button>
        <div className="min-w-0">
          <p className="text-[11px] uppercase tracking-wide text-white/70">Lyrics</p>
          <p className="truncate text-[15px] font-bold text-white">{track?.name ?? ''}</p>
        </div>
      </header>

      <div className="no-scrollbar flex-1 overflow-y-auto px-6 pb-28 pt-4">
        {isLoading ? (
          <p className="mt-10 text-[22px] font-bold text-white/50">Looking for lyrics…</p>
        ) : !lyrics || lyrics.lines.length === 0 ? (
          <div className="mt-10 space-y-3">
            <p className="text-[22px] font-bold text-white/80">No lyrics found</p>
            <p className="max-w-md text-[14px] leading-relaxed text-white/60">
              This build looks lyrics up on{' '}
              <a
                href="https://lrclib.net"
                target="_blank"
                rel="noreferrer"
                className="underline decoration-white/40"
              >
                LRCLIB
              </a>
              , a community database. Spotify&apos;s own synced lyrics are not available to
              third-party web apps — that endpoint only accepts web-player tokens, which a browser
              cannot mint.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {lyrics.lines.map((line, index) => {
              const isActive = index === activeIndex;
              const isPast = activeIndex >= 0 && index < activeIndex;
              return (
                <button
                  key={`${line.timeMs}-${index}`}
                  ref={isActive ? activeRef : null}
                  type="button"
                  disabled={!lyrics.synced}
                  onClick={() => lyrics.synced && void seekTo(line.timeMs)}
                  className={clsx(
                    'block w-full text-left text-[24px] font-extrabold leading-tight transition-colors duration-200',
                    lyrics.synced ? 'cursor-pointer' : 'cursor-default',
                    isActive ? 'text-white' : isPast ? 'text-white/40' : 'text-white/55',
                  )}
                >
                  {line.text}
                </button>
              );
            })}
            {!lyrics.synced ? (
              <p className="pt-6 text-[12px] text-white/40">
                Unsynced lyrics — no timestamps were available for this track.
              </p>
            ) : null}
          </div>
        )}
      </div>
    </div>
  );
}
