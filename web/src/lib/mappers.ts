import { joinArtists, pickImage, releaseYear } from './format';
import type {
  Artist,
  MediaCardItem,
  PlayHistoryItem,
  Playlist,
  SimpleAlbum,
  Track,
} from './types';

export function albumToCard(album: SimpleAlbum): MediaCardItem {
  return {
    id: album.id,
    uri: album.uri,
    title: album.name,
    subtitle: joinArtists(album.artists) || releaseYear(album.release_date),
    imageUrl: pickImage(album.images, 300),
    href: `/album/${album.id}`,
  };
}

export function artistToCard(artist: Artist): MediaCardItem {
  return {
    id: artist.id,
    uri: artist.uri,
    title: artist.name,
    subtitle: 'Artist',
    imageUrl: pickImage(artist.images, 300),
    href: `/artist/${artist.id}`,
    round: true,
  };
}

export function playlistToCard(playlist: Playlist): MediaCardItem {
  const total = playlist.items?.total ?? playlist.tracks?.total ?? null;
  return {
    id: playlist.id,
    uri: playlist.uri,
    title: playlist.name,
    subtitle:
      total != null
        ? `${total} ${total === 1 ? 'track' : 'tracks'}`
        : (playlist.owner?.display_name ?? 'Playlist'),
    imageUrl: pickImage(playlist.images, 300),
    href: `/playlist/${playlist.id}`,
  };
}

/** Tracks have no detail page here, so cards built from them play on click. */
export function trackToCard(track: Track): MediaCardItem {
  return {
    id: track.id ?? track.uri,
    uri: track.uri,
    title: track.name,
    subtitle: joinArtists(track.artists),
    imageUrl: pickImage(track.album?.images, 300),
    href: null,
  };
}

/**
 * Recently-played is returned per play, so the same album shows up repeatedly.
 * Api.kt's getHomeFeed dedups by uri then by a semantic key; this is the same
 * idea, keyed on the album so a row does not repeat one record five times.
 */
export function recentlyPlayedToCards(items: PlayHistoryItem[]): MediaCardItem[] {
  const seen = new Set<string>();
  const cards: MediaCardItem[] = [];
  for (const entry of items) {
    const album = entry.track?.album;
    if (!album) continue;
    if (seen.has(album.uri)) continue;
    seen.add(album.uri);
    cards.push(albumToCard(album));
  }
  return cards;
}

export function dedupeCards(cards: MediaCardItem[]): MediaCardItem[] {
  const seen = new Set<string>();
  return cards.filter((card) => {
    if (seen.has(card.uri)) return false;
    seen.add(card.uri);
    return true;
  });
}

/** "Good morning" / "Good afternoon" / "Good evening" — GreetingSection. */
export function greeting(date = new Date()): string {
  const hour = date.getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}
