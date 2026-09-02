'use client';

import { use } from 'react';
import Link from 'next/link';
import useSWR from 'swr';

import { DetailHeader } from '@/components/DetailHeader';
import { TrackRow } from '@/components/TrackRow';
import { ErrorView, ListSkeleton } from '@/components/StateViews';
import { joinArtists, pickImage, releaseYear } from '@/lib/format';
import { playTrackList } from '@/lib/playback';
import { getAlbum, getAlbumTracks } from '@/lib/spotify';

export default function AlbumPage({ params }: { params: Promise<{ id: string }> }) {
  // Next 16 hands route params to pages as a promise.
  const { id } = use(params);

  const album = useSWR(['album', id], ([, key]) => getAlbum(key));
  const tracksResult = useSWR(['album-tracks', id], ([, key]) => getAlbumTracks(key));

  // /albums/{id}/tracks omits the album object on each track; graft it back on
  // so rows and the player have artwork.
  const tracks = (tracksResult.data?.items ?? []).map((track) => ({
    ...track,
    album: track.album ?? album.data,
  }));
  const uris = tracks.map((track) => track.uri);

  const meta = album.data
    ? [
        album.data.album_type ? album.data.album_type.replace(/^\w/, (c) => c.toUpperCase()) : null,
        releaseYear(album.data.release_date),
        album.data.total_tracks ? `${album.data.total_tracks} tracks` : null,
      ]
        .filter(Boolean)
        .join(' · ')
    : undefined;

  return (
    <div>
      <DetailHeader
        title={album.data?.name ?? 'Album'}
        subtitle={joinArtists(album.data?.artists)}
        meta={meta}
        imageUrl={pickImage(album.data?.images, 400)}
        onPlay={uris.length > 0 ? () => void playTrackList(uris, 0) : undefined}
        onShuffle={
          uris.length > 0
            ? () => void playTrackList(uris, Math.floor(Math.random() * uris.length))
            : undefined
        }
      />

      {album.error ? <ErrorView error={album.error} /> : null}
      {tracksResult.error ? <ErrorView error={tracksResult.error} /> : null}
      {album.isLoading || tracksResult.isLoading ? <ListSkeleton rows={8} /> : null}

      <div className="px-2">
        {tracks.map((track, index) => (
          <TrackRow
            key={`${track.uri}-${index}`}
            track={track}
            index={index}
            showArtwork={false}
            showDuration
            onPlay={() => void playTrackList(uris, index)}
          />
        ))}
      </div>

      {album.data?.artists?.length ? (
        <div className="px-4 pt-6">
          <p className="text-[11px] font-bold uppercase tracking-wide text-white/50">Artists</p>
          <div className="mt-2 flex flex-wrap gap-2">
            {album.data.artists.map((artist) => (
              <Link
                key={artist.id}
                href={`/artist/${artist.id}`}
                className="rounded-full bg-white/[0.09] px-3 py-1.5 text-[13px] font-semibold text-white transition-colors hover:bg-white/[0.15]"
              >
                {artist.name}
              </Link>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}
