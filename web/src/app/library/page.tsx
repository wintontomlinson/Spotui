'use client';

import { useMemo, useState } from 'react';
import Link from 'next/link';
import clsx from 'clsx';
import useSWR from 'swr';

import { Artwork } from '@/components/Artwork';
import { EmptyView, ErrorView, ListSkeleton } from '@/components/StateViews';
import { HeartFilledIcon } from '@/components/icons';
import { joinArtists, pickImage } from '@/lib/format';
import { getFollowedArtists, getMyPlaylists, getSavedAlbums } from '@/lib/spotify';
import { useAuthStore } from '@/store/useAuthStore';

type Filter = 'all' | 'playlists' | 'albums' | 'artists';
type SortOption = 'recents' | 'title';

interface Row {
  key: string;
  href: string;
  title: string;
  subtitle: string;
  imageUrl: string | null;
  round?: boolean;
  kind: Exclude<Filter, 'all'>;
}

/** Your Library — LibraryFilterChips + sort + list, from LibraryScreeen.kt. */
export default function LibraryPage() {
  const [filter, setFilter] = useState<Filter>('all');
  const [sort, setSort] = useState<SortOption>('recents');
  const [term, setTerm] = useState('');
  const profile = useAuthStore((s) => s.profile);

  const playlists = useSWR('library:playlists', () => getMyPlaylists(50));
  const albums = useSWR('library:albums', () => getSavedAlbums(50));
  const artists = useSWR('library:artists', () => getFollowedArtists(50));

  const loading = playlists.isLoading || albums.isLoading || artists.isLoading;
  const error = playlists.error ?? albums.error ?? artists.error ?? null;

  const rows = useMemo<Row[]>(() => {
    const out: Row[] = [];

    for (const playlist of playlists.data?.items ?? []) {
      if (!playlist) continue;
      const total = playlist.items?.total ?? playlist.tracks?.total ?? null;
      out.push({
        key: `playlist-${playlist.id}`,
        href: `/playlist/${playlist.id}`,
        title: playlist.name,
        subtitle: `Playlist${total != null ? ` · ${total} tracks` : ''}`,
        imageUrl: pickImage(playlist.images, 128),
        kind: 'playlists',
      });
    }

    for (const entry of albums.data?.items ?? []) {
      const album = entry.album ?? entry.item;
      if (!album) continue;
      out.push({
        key: `album-${album.id}`,
        href: `/album/${album.id}`,
        title: album.name,
        subtitle: `Album · ${joinArtists(album.artists)}`,
        imageUrl: pickImage(album.images, 128),
        kind: 'albums',
      });
    }

    for (const artist of artists.data?.artists?.items ?? []) {
      out.push({
        key: `artist-${artist.id}`,
        href: `/artist/${artist.id}`,
        title: artist.name,
        subtitle: 'Artist',
        imageUrl: pickImage(artist.images, 128),
        round: true,
        kind: 'artists',
      });
    }

    return out;
  }, [playlists.data, albums.data, artists.data]);

  const visible = useMemo(() => {
    let list = filter === 'all' ? rows : rows.filter((row) => row.kind === filter);
    const needle = term.trim().toLowerCase();
    if (needle) {
      list = list.filter(
        (row) =>
          row.title.toLowerCase().includes(needle) || row.subtitle.toLowerCase().includes(needle),
      );
    }
    if (sort === 'title') list = [...list].sort((a, b) => a.title.localeCompare(b.title));
    return list;
  }, [rows, filter, term, sort]);

  const chips: Array<{ id: Filter; label: string }> = [
    { id: 'all', label: 'All' },
    { id: 'playlists', label: 'Playlists' },
    { id: 'albums', label: 'Albums' },
    { id: 'artists', label: 'Artists' },
  ];

  return (
    <div className="pt-[max(env(safe-area-inset-top),12px)]">
      <header className="px-4 py-3">
        <div className="flex items-center gap-3">
          {profile ? (
            <Artwork
              url={pickImage(profile.images, 64)}
              alt={profile.display_name ?? 'You'}
              rounded="full"
              className="h-8 w-8"
            />
          ) : null}
          <h1 className="text-[22px] font-extrabold text-white">Your Library</h1>
        </div>

        <div className="no-scrollbar mt-3 flex gap-2 overflow-x-auto">
          {chips.map((chip) => (
            <button
              key={chip.id}
              type="button"
              onClick={() => setFilter(chip.id)}
              aria-pressed={filter === chip.id}
              className={clsx(
                'shrink-0 rounded-full px-3 py-1.5 text-[13px] font-semibold transition-colors',
                filter === chip.id
                  ? 'btn-gold font-bold'
                  : 'bg-royal-800/50 text-white hover:bg-royal-700/60',
              )}
            >
              {chip.label}
            </button>
          ))}
        </div>

        <div className="mt-3 flex items-center gap-2">
          <input
            type="search"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
            placeholder="Search your library"
            aria-label="Search your library"
            className="min-w-0 flex-1 rounded-[6px] bg-white/[0.09] px-3 py-2 text-[14px] text-white outline-none placeholder:text-white/40"
          />
          <button
            type="button"
            onClick={() => setSort(sort === 'recents' ? 'title' : 'recents')}
            className="shrink-0 rounded-full border border-white/20 px-3 py-2 text-[12px] font-semibold text-white/80 transition-colors hover:border-white/50"
          >
            {sort === 'recents' ? 'Recents' : 'A–Z'}
          </button>
        </div>
      </header>

      {error ? <ErrorView error={error} /> : null}
      {loading ? <ListSkeleton rows={8} /> : null}

      <div className="px-2">
        {filter === 'all' || filter === 'playlists' ? (
          <Link
            href="/liked"
            className="flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors hover:bg-white/[0.07]"
          >
            <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-[6px] bg-gradient-to-br from-[#4300B0] to-[#8E8EE5] text-white">
              <HeartFilledIcon size={22} />
            </span>
            <span className="min-w-0">
              <span className="block text-[15px] font-medium text-white">Liked Songs</span>
              <span className="block text-[13px] text-text-secondary">Playlist</span>
            </span>
          </Link>
        ) : null}

        {visible.map((row) => (
          <Link
            key={row.key}
            href={row.href}
            className="flex items-center gap-3 rounded-[4px] px-2 py-[6px] transition-colors hover:bg-white/[0.07]"
          >
            <Artwork
              url={row.imageUrl}
              alt={row.title}
              rounded={row.round ? 'full' : 'md'}
              className="h-12 w-12 shrink-0"
            />
            <span className="min-w-0">
              <span className="block truncate text-[15px] font-medium text-white">{row.title}</span>
              <span className="block truncate text-[13px] text-text-secondary">{row.subtitle}</span>
            </span>
          </Link>
        ))}
      </div>

      {!loading && visible.length === 0 ? (
        <EmptyView
          title="Nothing here"
          body={term ? 'No library item matches that search.' : 'Save some music in Spotify first.'}
        />
      ) : null}
    </div>
  );
}
