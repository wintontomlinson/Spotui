'use client';

import clsx from 'clsx';

import { formatDuration, joinArtists, pickImage } from '@/lib/format';
import type { Track } from '@/lib/types';
import { usePlayerStore } from '@/store/usePlayerStore';
import { useSavedState } from '@/hooks/useLibraryState';

import { Artwork } from './Artwork';
import { CheckCircleIcon, ExplicitBadge, PlusIcon } from './icons';

interface TrackRowProps {
  track: Track;
  onPlay: () => void;
  /** Album/playlist views show a track number instead of artwork. */
  index?: number;
  showArtwork?: boolean;
  showDuration?: boolean;
}

export function TrackRow({
  track,
  onPlay,
  index,
  showArtwork = true,
  showDuration = false,
}: TrackRowProps) {
  const currentUri = usePlayerStore((s) => s.track?.uri ?? null);
  const { saved, toggle } = useSavedState(track.uri);
  const isCurrent = currentUri === track.uri;
  const unplayable = track.is_playable === false;

  return (
    <div
      className={clsx(
        'group flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors',
        'hover:bg-white/[0.07]',
      )}
    >
      <button
        type="button"
        onClick={onPlay}
        disabled={unplayable}
        className="flex min-w-0 flex-1 items-center gap-3 text-left disabled:cursor-not-allowed disabled:opacity-40"
      >
        {showArtwork ? (
          <Artwork
            url={pickImage(track.album?.images, 64)}
            alt={track.name}
            className="h-12 w-12 shrink-0"
          />
        ) : (
          <span className="w-5 shrink-0 text-center text-[14px] tabular-nums text-text-secondary">
            {index != null ? index + 1 : '·'}
          </span>
        )}

        <span className="min-w-0 flex-1">
          <span
            className={clsx(
              'flex items-center truncate text-[15px] font-medium',
              isCurrent ? 'text-spotify-green' : 'text-white',
            )}
          >
            <span className="truncate">{track.name}</span>
            {track.explicit ? <ExplicitBadge /> : null}
          </span>
          <span className="mt-[2px] block truncate text-[13px] text-text-secondary">
            {joinArtists(track.artists)}
            {unplayable ? ' · unavailable' : ''}
          </span>
        </span>
      </button>

      {showDuration ? (
        <span className="shrink-0 text-[12px] tabular-nums text-text-secondary">
          {formatDuration(track.duration_ms)}
        </span>
      ) : null}

      <button
        type="button"
        onClick={() => void toggle()}
        aria-label={saved ? 'Remove from your library' : 'Save to your library'}
        aria-pressed={saved}
        className="shrink-0 p-1 text-white/70 transition-colors hover:text-white"
      >
        {saved ? (
          <CheckCircleIcon size={20} className="text-spotify-green" />
        ) : (
          <PlusIcon size={20} />
        )}
      </button>
    </div>
  );
}
