'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import useSWR from 'swr';

import { Artwork } from '@/components/Artwork';
import { TrackRow } from '@/components/TrackRow';
import { EmptyView, ErrorView, ListSkeleton } from '@/components/StateViews';
import { SearchIcon } from '@/components/icons';
import { joinArtists, pickImage, releaseYear } from '@/lib/format';
import { playTrackList } from '@/lib/playback';
import { search, SEARCH_LIMIT } from '@/lib/spotify';

/**
 * Search.
 *
 * The Android app's Search tab also renders a "Browse all" grid of genre tiles,
 * built on GET /browse/categories. That endpoint was removed for Development
 * Mode apps in the Feb 2026 migration, so the empty state here is a prompt
 * rather than a category grid.
 *
 * Note the result cap: `limit` maximum dropped from 50 to 10 in the same change.
 */
export default function SearchPage() {
  const [input, setInput] = useState('');
  const [query, setQuery] = useState('');

  // Debounce so typing does not burn through the rate limit.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(input.trim()), 350);
    return () => clearTimeout(timer);
  }, [input]);

  const { data, error, isLoading } = useSWR(
    query ? ['search', query] : null,
    ([, term]) => search(term),
    { keepPreviousData: true },
  );

  const tracks = data?.tracks?.items ?? [];
  const artists = data?.artists?.items ?? [];
  const albums = data?.albums?.items ?? [];
  const playlists = (data?.playlists?.items ?? []).filter(
    (playlist): playlist is NonNullable<typeof playlist> => Boolean(playlist),
  );

  const hasResults =
    tracks.length > 0 || artists.length > 0 || albums.length > 0 || playlists.length > 0;

  return (
    <div className="pt-[max(env(safe-area-inset-top),12px)]">
      <header className="sticky top-0 z-20 bg-app-bg/95 px-4 py-3 backdrop-blur">
        <h1 className="mb-3 text-[22px] font-extrabold text-white">Search</h1>
        <div className="flex items-center gap-2 rounded-[6px] bg-white px-3 py-2.5">
          <SearchIcon size={20} className="shrink-0 text-black/60" />
          <input
            type="search"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="Songs, artists, albums"
            aria-label="Search Spotify"
            autoComplete="off"
            className="w-full bg-transparent text-[15px] font-medium text-black outline-none placeholder:text-black/50"
          />
        </div>
      </header>

      {error ? <ErrorView error={error} /> : null}
      {query && isLoading && !data ? <ListSkeleton rows={8} /> : null}

      {!query ? (
        <EmptyView
          title="Find something to play"
          body="Search your Spotify catalogue by song, artist, album or playlist."
        />
      ) : null}

      {query && !isLoading && !hasResults && !error ? (
        <EmptyView title={`No results for “${query}”`} />
      ) : null}

      {tracks.length > 0 ? (
        <section className="mt-4 px-2">
          <h2 className="px-2 pb-1 text-[16px] font-bold text-white">Songs</h2>
          {tracks.map((track, index) => (
            <TrackRow
              key={`${track.uri}-${index}`}
              track={track}
              onPlay={() =>
                void playTrackList(
                  tracks.map((item) => item.uri),
                  index,
                )
              }
            />
          ))}
        </section>
      ) : null}

      {artists.length > 0 ? (
        <section className="mt-6 px-2">
          <h2 className="px-2 pb-1 text-[16px] font-bold text-white">Artists</h2>
          {artists.map((artist) => (
            <Link
              key={artist.id}
              href={`/artist/${artist.id}`}
              className="flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors hover:bg-white/[0.07]"
            >
              <Artwork
                url={pickImage(artist.images, 64)}
                alt={artist.name}
                rounded="full"
                className="h-12 w-12 shrink-0"
              />
              <span className="min-w-0">
                <span className="block truncate text-[15px] font-medium text-white">
                  {artist.name}
                </span>
                <span className="block text-[13px] text-text-secondary">Artist</span>
              </span>
            </Link>
          ))}
        </section>
      ) : null}

      {albums.length > 0 ? (
        <section className="mt-6 px-2">
          <h2 className="px-2 pb-1 text-[16px] font-bold text-white">Albums</h2>
          {albums.map((album) => (
            <Link
              key={album.id}
              href={`/album/${album.id}`}
              className="flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors hover:bg-white/[0.07]"
            >
              <Artwork
                url={pickImage(album.images, 64)}
                alt={album.name}
                className="h-12 w-12 shrink-0"
              />
              <span className="min-w-0">
                <span className="block truncate text-[15px] font-medium text-white">
                  {album.name}
                </span>
                <span className="block truncate text-[13px] text-text-secondary">
                  {releaseYear(album.release_date)} · {joinArtists(album.artists)}
                </span>
              </span>
            </Link>
          ))}
        </section>
      ) : null}

      {playlists.length > 0 ? (
        <section className="mt-6 px-2">
          <h2 className="px-2 pb-1 text-[16px] font-bold text-white">Playlists</h2>
          {playlists.map((playlist) => (
            <Link
              key={playlist.id}
              href={`/playlist/${playlist.id}`}
              className="flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors hover:bg-white/[0.07]"
            >
              <Artwork
                url={pickImage(playlist.images, 64)}
                alt={playlist.name}
                className="h-12 w-12 shrink-0"
              />
              <span className="min-w-0">
                <span className="block truncate text-[15px] font-medium text-white">
                  {playlist.name}
                </span>
                <span className="block truncate text-[13px] text-text-secondary">
                  Playlist · {playlist.owner?.display_name ?? 'Spotify'}
                </span>
              </span>
            </Link>
          ))}
        </section>
      ) : null}

      {hasResults ? (
        <p className="px-4 py-6 text-[11px] leading-relaxed text-white/35">
          Spotify caps Development Mode search results at {SEARCH_LIMIT} per type.
        </p>
      ) : null}
    </div>
  );
}
