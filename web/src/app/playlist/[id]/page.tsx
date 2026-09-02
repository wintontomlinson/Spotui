'use client';

import { use, useMemo } from 'react';
import useSWR from 'swr';

import { DetailHeader } from '@/components/DetailHeader';
import { TrackRow } from '@/components/TrackRow';
import { ErrorView, ListSkeleton } from '@/components/StateViews';
import { formatCount, pickImage } from '@/lib/format';
import { playContextUri, playTrackList } from '@/lib/playback';
import { getPlaylist, getPlaylistItems } from '@/lib/spotify';
import type { Track } from '@/lib/types';

export default function PlaylistPage({ params }: { params: Promise<{ id: string }> }) {
  // Next 16 hands route params to pages as a promise.
  const { id } = use(params);

  const playlist = useSWR(['playlist', id], ([, key]) => getPlaylist(key));
  const items = useSWR(['playlist-items', id], ([, key]) => getPlaylistItems(key), {
    shouldRetryOnError: false,
  });

  const tracks = useMemo<Track[]>(
    () =>
      (items.data ?? [])
        .map((row) => row.item ?? row.track)
        .filter((entry): entry is Track => Boolean(entry) && entry!.type === 'track'),
    [items.data],
  );

  const uris = tracks.map((track) => track.uri);
  const total = playlist.data?.items?.total ?? playlist.data?.tracks?.total ?? tracks.length;

  /**
   * Post-migration, GET /playlists/{id}/items only returns contents for
   * playlists the user owns or collaborates on. For everything else — including
   * every Spotify editorial playlist — we get metadata only. Playback still
   * works by handing Spotify the context URI instead of a track list.
   */
  const contentsWithheld = !items.isLoading && !items.error && tracks.length === 0 && total > 0;

  // Bound locally so the closure below narrows.
  const playlistUri = playlist.data?.uri;

  return (
    <div>
      <DetailHeader
        title={playlist.data?.name ?? 'Playlist'}
        subtitle={playlist.data?.owner?.display_name ?? undefined}
        meta={total > 0 ? `${formatCount(total)} tracks` : undefined}
        imageUrl={pickImage(playlist.data?.images, 400)}
        onPlay={
          uris.length > 0
            ? () => void playTrackList(uris, 0)
            : playlistUri
              ? () => void playContextUri(playlistUri, 0)
              : undefined
        }
        onShuffle={
          uris.length > 0
            ? () => void playTrackList(uris, Math.floor(Math.random() * uris.length))
            : undefined
        }
      />

      {playlist.error ? <ErrorView error={playlist.error} /> : null}
      {items.error ? <ErrorView error={items.error} /> : null}
      {playlist.isLoading || items.isLoading ? <ListSkeleton rows={10} /> : null}

      {playlist.data?.description ? (
        <p
          className="px-4 pb-2 text-[13px] leading-relaxed text-text-secondary"
          // Spotify returns escaped HTML entities in descriptions.
          dangerouslySetInnerHTML={{ __html: playlist.data.description }}
        />
      ) : null}

      {contentsWithheld ? (
        <div className="mx-4 mt-4 rounded-[8px] border border-white/15 bg-white/[0.05] p-4">
          <p className="text-[13px] font-bold text-white">Track list not available</p>
          <p className="mt-1 text-[12px] leading-relaxed text-white/60">
            Since the February 2026 API changes, Spotify only returns the contents of playlists you
            own or collaborate on. Editorial playlists like this one come back as metadata only —
            but <strong className="text-white/80">Play</strong> still works, because playback is
            handed the playlist URI rather than a list of tracks.
          </p>
        </div>
      ) : null}

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
    </div>
  );
}
