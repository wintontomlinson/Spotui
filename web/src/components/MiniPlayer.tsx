'use client';

import { useRef } from 'react';

import { nextTrack, previousTrack, seekTo, togglePlay } from '@/lib/playback';
import { usePlayerStore } from '@/store/usePlayerStore';
import { useSavedState } from '@/hooks/useLibraryState';

import { Artwork } from './Artwork';
import { Seekbar } from './Seekbar';
import { CheckCircleIcon, PauseIcon, PlayIcon, PlusIcon } from './icons';

/**
 * Mirrors `MiniPlayer` in ui/components/AppComponents.kt: rounded 8 dp bar
 * tinted with the artwork's dark-vibrant colour, 42 dp cover, save + play/pause
 * on the right, progress bar beneath, tap to open the player, and horizontal
 * swipe to skip.
 */
export function MiniPlayer() {
  const track = usePlayerStore((s) => s.track);
  const isPaused = usePlayerStore((s) => s.isPaused);
  const isBuffering = usePlayerStore((s) => s.isBuffering);
  const positionMs = usePlayerStore((s) => s.positionMs);
  const durationMs = usePlayerStore((s) => s.durationMs);
  const palette = usePlayerStore((s) => s.palette);
  const setPlayerOpen = usePlayerStore((s) => s.setPlayerOpen);
  const { saved, toggle } = useSavedState(track?.uri);

  const touchStart = useRef<{ x: number; y: number } | null>(null);

  if (!track) return null;

  const handleTouchStart = (event: React.TouchEvent) => {
    const point = event.touches[0];
    touchStart.current = { x: point.clientX, y: point.clientY };
  };

  const handleTouchEnd = (event: React.TouchEvent) => {
    const start = touchStart.current;
    touchStart.current = null;
    if (!start) return;

    const point = event.changedTouches[0];
    const dx = point.clientX - start.x;
    const dy = point.clientY - start.y;

    // Vertical wins when it dominates — swipe up opens the player (Kotlin: >20 dp).
    if (Math.abs(dy) > Math.abs(dx)) {
      if (dy < -20) setPlayerOpen(true);
      return;
    }
    // Horizontal skip threshold in the Kotlin code is 40 dp.
    if (dx <= -40) void nextTrack();
    else if (dx >= 40) void previousTrack();
  };

  return (
    <div className="px-[13px]">
      <div
        className="overflow-hidden rounded-[8px] transition-colors duration-500"
        style={{ backgroundColor: palette.darkVibrant }}
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        <div className="flex items-center gap-3 p-2">
          <button
            type="button"
            onClick={() => setPlayerOpen(true)}
            className="flex min-w-0 flex-1 items-center gap-3 text-left"
            aria-label="Open the player"
          >
            <Artwork url={track.imageUrl} alt={track.name} className="h-[42px] w-[42px] shrink-0" />
            <span className="min-w-0 max-w-[200px] flex-1">
              <span className="flex items-center truncate text-[13px] font-bold text-white">
                <span className="truncate">{track.name}</span>
              </span>
              <span className="mt-[1px] block truncate text-[12px] text-[#D3D3D3]">
                {track.artistNames}
              </span>
            </span>
          </button>

          <div className="flex w-[70px] shrink-0 items-center justify-end gap-2">
            <button
              type="button"
              onClick={() => void toggle()}
              aria-label={saved ? 'Remove from your library' : 'Save to your library'}
              aria-pressed={saved}
              className="p-1 text-white transition-opacity hover:opacity-80"
            >
              {saved ? (
                <CheckCircleIcon size={22} className="text-spotify-green" />
              ) : (
                <PlusIcon size={22} />
              )}
            </button>

            <button
              type="button"
              onClick={() => void togglePlay()}
              aria-label={isPaused ? 'Play' : 'Pause'}
              className="p-1 text-white transition-opacity hover:opacity-80"
            >
              {isBuffering ? (
                <span className="block h-[24px] w-[24px] animate-spin rounded-full border-2 border-white/30 border-t-white" />
              ) : isPaused ? (
                <PlayIcon size={26} />
              ) : (
                <PauseIcon size={26} />
              )}
            </button>
          </div>
        </div>

        <div className="px-2 pb-2">
          <Seekbar
            compact
            positionMs={positionMs}
            durationMs={durationMs}
            onSeek={(value) => void seekTo(value)}
          />
        </div>
      </div>
    </div>
  );
}
