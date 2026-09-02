'use client';

import { useMemo } from 'react';
import useSWR from 'swr';

import { DetailHeader } from '@/components/DetailHeader';
import { TrackRow } from '@/components/TrackRow';
import { EmptyView, ErrorView, ListSkeleton } from '@/components/StateViews';
import { formatCount } from '@/lib/format';
import { playTrackList } from '@/lib/playback';
import { getAllSavedTracks } from '@/lib/spotify';
import type { Track } from '@/lib/types';

/** Liked Songs — LikedSongsScreen.kt. */
export default function LikedSongsPage() {
  const { data, error, isLoading } = useSWR('liked:all', getAllSavedTracks);

  const tracks = useMemo<Track[]>(
    () =>
      (data ?? [])
        .map((entry) => entry.track ?? entry.item)
        .filter((track): track is Track => Boolean(track)),
    [data],
  );

  const uris = tracks.map((track) => track.uri);

  return (
    <div>
      <DetailHeader
        title="Liked Songs"
        subtitle={tracks.length > 0 ? `${formatCount(tracks.length)} songs` : undefined}
        imageUrl={null}
        accent="#4300B0"
        onPlay={tracks.length > 0 ? () => void playTrackList(uris, 0) : undefined}
        onShuffle={
          tracks.length > 0
            ? () => void playTrackList(uris, Math.floor(Math.random() * uris.length))
            : undefined
        }
      />

      {error ? <ErrorView error={error} /> : null}
      {isLoading ? <ListSkeleton rows={10} /> : null}

      <div className="px-2">
        {tracks.map((track, index) => (
          <TrackRow
            key={`${track.uri}-${index}`}
            track={track}
            showDuration
            onPlay={() => void playTrackList(uris, index)}
          />
        ))}
      </div>

      {!isLoading && tracks.length === 0 && !error ? (
        <EmptyView title="No liked songs" body="Songs you save in Spotify will appear here." />
      ) : null}
    </div>
  );
}
