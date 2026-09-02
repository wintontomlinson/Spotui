'use client';

import Link from 'next/link';
import useSWR from 'swr';

import { Artwork } from '@/components/Artwork';
import { SectionRow, ShortcutGrid } from '@/components/MediaCard';
import { ErrorView, ListSkeleton } from '@/components/StateViews';
import { HeartFilledIcon } from '@/components/icons';
import { pickImage } from '@/lib/format';
import {
  albumToCard,
  artistToCard,
  dedupeCards,
  greeting,
  playlistToCard,
  recentlyPlayedToCards,
  trackToCard,
} from '@/lib/mappers';
import { playTrackList } from '@/lib/playback';
import {
  getMyPlaylists,
  getRecentlyPlayed,
  getSavedAlbums,
  getTopArtists,
  getTopTracks,
} from '@/lib/spotify';
import type { MediaCardItem } from '@/lib/types';
import { useAuthStore } from '@/store/useAuthStore';

/**
 * Home.
 *
 * The Android app's home feed comes from the private pathfinder `home` GraphQL
 * operation — Spotify's real, personalised shelf list. There is no official
 * equivalent, and the Feb 2026 migration additionally removed
 * GET /browse/new-releases and GET /browse/categories. So this screen is
 * assembled from the personalised endpoints that remain: recently played, top
 * tracks, top artists, your playlists and saved albums.
 */
export default function HomePage() {
  const profile = useAuthStore((s) => s.profile);

  const recent = useSWR('home:recently-played', () => getRecentlyPlayed(50));
  const topTracks = useSWR('home:top-tracks', () => getTopTracks(20));
  const topArtists = useSWR('home:top-artists', () => getTopArtists(20));
  const playlists = useSWR('home:playlists', () => getMyPlaylists(50));
  const albums = useSWR('home:saved-albums', () => getSavedAlbums(50));

  const loading =
    recent.isLoading && topTracks.isLoading && playlists.isLoading && albums.isLoading;
  const fatalError = recent.error ?? playlists.error ?? null;

  const recentCards = recentlyPlayedToCards(recent.data?.items ?? []);
  const playlistCards = (playlists.data?.items ?? []).filter(Boolean).map(playlistToCard);
  const albumCards = (albums.data?.items ?? [])
    .map((entry) => entry.album ?? entry.item)
    .filter((album): album is NonNullable<typeof album> => Boolean(album))
    .map(albumToCard);

  // The shortcut grid mixes the most recent albums with your own playlists,
  // matching the shape of Spotify's top-of-home tiles.
  const shortcuts = dedupeCards([...recentCards.slice(0, 4), ...playlistCards.slice(0, 4)]);

  const topTrackList = topTracks.data?.items ?? [];
  const topTrackCards = topTrackList.map(trackToCard);

  const handlePlayTopTrack = (item: MediaCardItem) => {
    const index = topTrackList.findIndex((track) => track.uri === item.uri);
    void playTrackList(
      topTrackList.map((track) => track.uri),
      index < 0 ? 0 : index,
    );
  };

  return (
    <div className="pt-[max(env(safe-area-inset-top),12px)]">
      {/* HomeHeaderRow — greeting plus the avatar from ProfileCache. */}
      <header className="flex items-center justify-between px-4 py-3">
        <h1 className="text-[22px] font-extrabold text-white">{greeting()}</h1>
        {profile ? (
          <span className="flex items-center gap-2">
            <span className="hidden text-[12px] text-text-secondary sm:inline">
              {profile.display_name}
            </span>
            <Artwork
              url={pickImage(profile.images, 64)}
              alt={profile.display_name ?? 'You'}
              rounded="full"
              className="h-8 w-8"
            />
          </span>
        ) : null}
      </header>

      {fatalError ? <ErrorView error={fatalError} /> : null}
      {loading ? <ListSkeleton rows={6} /> : null}

      {shortcuts.length > 0 ? (
        <div className="mb-2">
          {/* Liked Songs is a first-class tile, as in the Android library. */}
          <div className="mb-2 px-4">
            <Link
              href="/liked"
              className="flex h-[54px] items-center overflow-hidden rounded-[4px] bg-white/[0.09] transition-colors hover:bg-white/[0.15]"
            >
              <span className="flex h-full w-[54px] shrink-0 items-center justify-center bg-gradient-to-br from-[#4300B0] to-[#8E8EE5] text-white">
                <HeartFilledIcon size={22} />
              </span>
              <span className="px-3 text-[13px] font-bold text-white">Liked Songs</span>
            </Link>
          </div>
          <ShortcutGrid items={shortcuts} />
        </div>
      ) : null}

      <SectionRow title="Recently played" items={recentCards} />
      <SectionRow
        title="Your top tracks"
        items={topTrackCards}
        onSelect={handlePlayTopTrack}
      />
      <SectionRow title="Your top artists" items={(topArtists.data?.items ?? []).map(artistToCard)} />
      <SectionRow title="Your playlists" items={playlistCards} />
      <SectionRow title="Saved albums" items={albumCards} />

      {!loading && recentCards.length === 0 && playlistCards.length === 0 ? (
        <div className="px-4 py-10">
          <p className="text-[16px] font-bold text-white">Nothing to show yet</p>
          <p className="mt-2 text-[13px] leading-relaxed text-text-secondary">
            Play something in Spotify or save a few playlists, then reload — this page is built from
            your listening history and library.
          </p>
        </div>
      ) : null}
    </div>
  );
}
