'use client';

import Link from 'next/link';
import clsx from 'clsx';

import { idFromUri } from '@/lib/format';
import {
  becomeActiveDevice,
  cycleRepeat,
  nextTrack,
  previousTrack,
  seekTo,
  togglePlay,
  toggleShuffle,
} from '@/lib/playback';
import { useSavedState } from '@/hooks/useLibraryState';
import { usePlayerStore } from '@/store/usePlayerStore';

import { Artwork } from './Artwork';
import { LyricsPanel } from './LyricsPanel';
import { Seekbar } from './Seekbar';
import {
  CheckCircleIcon,
  ChevronDownIcon,
  DevicesIcon,
  LyricsIcon,
  NextIcon,
  PauseIcon,
  PlayIcon,
  PlusIcon,
  PreviousIcon,
  RepeatIcon,
  ShuffleIcon,
} from './icons';

/**
 * Full-screen now-playing view, mirroring ui/screens/PlayerScreen.kt — which is
 * registered as a `dialog(...)` route in MyNavHost.kt, i.e. an overlay rather
 * than a page, so it lives here rather than under app/.
 *
 * Not ported, because the browser cannot reach the required endpoints:
 *   - Spotify Canvas looping video (SpotifyCanvas.kt / spclient)
 *   - the swipe-through-queue artwork pager backed by a locally managed queue
 */
export function PlayerOverlay() {
  const open = usePlayerStore((s) => s.playerOpen);
  const lyricsOpen = usePlayerStore((s) => s.lyricsOpen);
  const track = usePlayerStore((s) => s.track);
  const isPaused = usePlayerStore((s) => s.isPaused);
  const isBuffering = usePlayerStore((s) => s.isBuffering);
  const isActive = usePlayerStore((s) => s.isActive);
  const positionMs = usePlayerStore((s) => s.positionMs);
  const durationMs = usePlayerStore((s) => s.durationMs);
  const shuffle = usePlayerStore((s) => s.shuffle);
  const repeatMode = usePlayerStore((s) => s.repeatMode);
  const nextTracks = usePlayerStore((s) => s.nextTracks);
  const palette = usePlayerStore((s) => s.palette);
  const blocker = usePlayerStore((s) => s.blocker);
  const errorMessage = usePlayerStore((s) => s.errorMessage);
  const setPlayerOpen = usePlayerStore((s) => s.setPlayerOpen);
  const setLyricsOpen = usePlayerStore((s) => s.setLyricsOpen);

  const { saved, toggle } = useSavedState(track?.uri);

  if (!open || !track) return null;
  if (lyricsOpen) return <LyricsPanel />;

  return (
    <div
      className="fixed inset-0 z-40 flex flex-col"
      // Brush.verticalGradient(dominentColor -> Black, startY = 100f)
      style={{
        backgroundImage: `linear-gradient(to bottom, ${palette.muted} 0%, #000000 85%)`,
        backgroundColor: '#0B0B0F',
      }}
      role="dialog"
      aria-label="Now playing"
    >
      {/* PlayerTopBar */}
      <header className="flex items-center justify-between px-4 pb-2 pt-[max(env(safe-area-inset-top),12px)]">
        <button
          type="button"
          onClick={() => setPlayerOpen(false)}
          aria-label="Close the player"
          className="p-1 text-white transition-opacity hover:opacity-70"
        >
          <ChevronDownIcon size={28} />
        </button>
        <p className="min-w-0 flex-1 truncate px-3 text-center text-[12px] font-semibold uppercase tracking-wide text-white/80">
          {track.albumName}
        </p>
        <span className="w-[30px]" />
      </header>

      <div className="flex min-h-0 flex-1 flex-col justify-between px-6 pb-[max(env(safe-area-inset-bottom),16px)]">
        {/* Artwork — sizeIn(max 385.dp), aspectRatio(1f), rounded 10 dp */}
        <div className="flex min-h-0 flex-1 items-center justify-center py-4">
          <Artwork
            url={track.imageUrl}
            alt={track.name}
            rounded="lg"
            priority
            className="max-h-full w-full max-w-[385px] object-cover shadow-2xl"
          />
        </div>

        <div className="shrink-0">
          {/* PlayerInfo */}
          <div className="flex items-end justify-between gap-4">
            <div className="min-w-0">
              <h1 className="truncate text-[22px] font-extrabold leading-tight text-white">
                {track.name}
              </h1>
              <p className="mt-1 truncate text-[15px] text-text-secondary">{track.artistNames}</p>
            </div>
            <button
              type="button"
              onClick={() => void toggle()}
              aria-label={saved ? 'Remove from your library' : 'Save to your library'}
              aria-pressed={saved}
              className="shrink-0 p-1 text-white/80 transition-colors hover:text-white"
            >
              {saved ? (
                <CheckCircleIcon size={26} className="text-spotify-green" />
              ) : (
                <PlusIcon size={26} />
              )}
            </button>
          </div>

          <div className="mt-4">
            <Seekbar
              positionMs={positionMs}
              durationMs={durationMs}
              onSeek={(value) => void seekTo(value)}
              disabled={!isActive}
            />
          </div>

          {/* PlayerFull transport row */}
          <div className="mt-3 flex items-center justify-between">
            <button
              type="button"
              onClick={() => void toggleShuffle()}
              aria-label="Shuffle"
              aria-pressed={shuffle}
              className={clsx(
                'p-1 transition-colors',
                shuffle ? 'text-spotify-green' : 'text-white hover:text-white/80',
              )}
            >
              <ShuffleIcon size={25} />
            </button>

            <button
              type="button"
              onClick={() => void previousTrack()}
              aria-label="Previous track"
              className="p-1 text-white transition-opacity hover:opacity-80"
            >
              <PreviousIcon size={35} />
            </button>

            {/* requiredSize(64.dp) white circle with a black 30 dp icon */}
            <button
              type="button"
              onClick={() => void togglePlay()}
              aria-label={isPaused ? 'Play' : 'Pause'}
              className="flex h-16 w-16 items-center justify-center rounded-full bg-white text-black transition-transform hover:scale-[1.04]"
            >
              {isBuffering ? (
                <span className="block h-[30px] w-[30px] animate-spin rounded-full border-[3px] border-black/25 border-t-black" />
              ) : isPaused ? (
                <PlayIcon size={30} />
              ) : (
                <PauseIcon size={30} />
              )}
            </button>

            <button
              type="button"
              onClick={() => void nextTrack()}
              aria-label="Next track"
              className="p-1 text-white transition-opacity hover:opacity-80"
            >
              <NextIcon size={35} />
            </button>

            <button
              type="button"
              onClick={() => void cycleRepeat()}
              aria-label={`Repeat: ${repeatMode}`}
              className={clsx(
                'relative p-1 transition-colors',
                repeatMode !== 'off' ? 'text-spotify-green' : 'text-white hover:text-white/80',
              )}
            >
              <RepeatIcon size={20} />
              {repeatMode === 'track' ? (
                <span className="absolute -right-0.5 -top-0.5 text-[9px] font-bold">1</span>
              ) : null}
              {repeatMode !== 'off' ? (
                <span className="absolute bottom-0 left-1/2 h-1 w-1 -translate-x-1/2 rounded-full bg-spotify-green" />
              ) : null}
            </button>
          </div>

          {/* PlayerConnectRow + lyrics entry point */}
          <div className="mt-4 flex items-center justify-between">
            <button
              type="button"
              onClick={() => void becomeActiveDevice(false)}
              className={clsx(
                'flex items-center gap-2 text-[12px] font-semibold transition-colors',
                isActive ? 'text-spotify-green' : 'text-white/70 hover:text-white',
              )}
            >
              <DevicesIcon size={18} />
              {isActive ? 'Playing on this browser' : 'Play here'}
            </button>

            <button
              type="button"
              onClick={() => setLyricsOpen(true)}
              className="flex items-center gap-2 text-[12px] font-semibold text-white/70 transition-colors hover:text-white"
            >
              <LyricsIcon size={18} />
              Lyrics
            </button>
          </div>

          {blocker ? (
            <p className="mt-3 rounded-[6px] bg-app-error/15 px-3 py-2 text-[12px] leading-relaxed text-app-error">
              {errorMessage}
            </p>
          ) : null}

          {/* Up next, from the SDK's track_window */}
          {nextTracks.length > 0 ? (
            <div className="mt-4">
              <p className="text-[11px] font-bold uppercase tracking-wide text-white/50">Up next</p>
              <ul className="mt-2 space-y-1">
                {nextTracks.slice(0, 3).map((item) => (
                  <li key={item.uri} className="flex items-center gap-2 text-[13px] text-white/70">
                    <Artwork url={item.imageUrl} alt={item.name} className="h-7 w-7 shrink-0" />
                    <span className="truncate">
                      {item.name}
                      <span className="text-white/40"> · {item.artistNames}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}

          {track.albumUri ? (
            <Link
              href={`/album/${idFromUri(track.albumUri)}`}
              onClick={() => setPlayerOpen(false)}
              className="mt-4 inline-block text-[12px] text-white/50 underline decoration-white/30 transition-colors hover:text-white"
            >
              Go to album
            </Link>
          ) : null}
        </div>
      </div>
    </div>
  );
}
