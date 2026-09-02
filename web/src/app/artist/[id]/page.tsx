'use client';

import { use } from 'react';
import useSWR from 'swr';

import { DetailHeader } from '@/components/DetailHeader';
import { MediaCard } from '@/components/MediaCard';
import { ErrorView, ListSkeleton } from '@/components/StateViews';
import { pickImage } from '@/lib/format';
import { albumToCard } from '@/lib/mappers';
import { playContextUri } from '@/lib/playback';
import { getArtist, getArtistAlbums } from '@/lib/spotify';

/**
 * Artist.
 *
 * The Android app leads with the artist's top tracks (pathfinder
 * `queryArtistOverview`, plus GET /artists/{id}/top-tracks). Both are gone for
 * Development Mode apps after the Feb 2026 migration — top-tracks was removed
 * outright — so this page is a releases grid, and Play starts the artist
 * context so Spotify picks the running order.
 */
export default function ArtistPage({ params }: { params: Promise<{ id: string }> }) {
  // Next 16 hands route params to pages as a promise.
  const { id } = use(params);

  const artist = useSWR(['artist', id], ([, key]) => getArtist(key));
  const albums = useSWR(['artist-albums', id], ([, key]) => getArtistAlbums(key));

  const releases = albums.data?.items ?? [];
  // Bound locally so the closure below narrows.
  const artistUri = artist.data?.uri;

  return (
    <div>
      <DetailHeader
        title={artist.data?.name ?? 'Artist'}
        subtitle={artist.data?.genres?.slice(0, 2).join(' · ') || undefined}
        imageUrl={pickImage(artist.data?.images, 400)}
        round
        onPlay={artistUri ? () => void playContextUri(artistUri, 0) : undefined}
      />

      {artist.error ? <ErrorView error={artist.error} /> : null}
      {albums.error ? <ErrorView error={albums.error} /> : null}
      {artist.isLoading || albums.isLoading ? <ListSkeleton rows={6} /> : null}

      {releases.length > 0 ? (
        <section className="px-4 pt-2">
          <h2 className="mb-3 text-[20px] font-bold text-white">Releases</h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
            {releases.map((album) => (
              <MediaCard key={album.id} item={albumToCard(album)} width={undefined} />
            ))}
          </div>
        </section>
      ) : null}

      {!artist.isLoading && !albums.isLoading && releases.length === 0 ? (
        <p className="px-4 py-8 text-[13px] leading-relaxed text-text-secondary">
          No releases came back for this artist.
        </p>
      ) : null}
    </div>
  );
}
