/**
 * Spotify Web API client.
 *
 * Written against the endpoint set that survives the February/March 2026
 * Development Mode restrictions. Notably absent, because the API no longer
 * offers them to Development Mode apps:
 *   - GET /browse/new-releases, /browse/categories  (home rows, Search "Browse all")
 *   - GET /artists/{id}/top-tracks                  (artist screen top tracks)
 *   - GET /recommendations                          (track radio / autoplay)
 *   - batch GET /tracks|/albums|/artists?ids=       (fetch individually instead)
 * See web/README.md for the full list and what it costs us.
 */

import { ensureAccessToken, invalidateAccessToken, NotAuthenticatedError } from './token';
import type {
  Album,
  Artist,
  CursorPaging,
  Paging,
  PlayHistoryItem,
  Playlist,
  PlaylistItem,
  SavedAlbum,
  SavedTrack,
  SearchResults,
  SimpleAlbum,
  Track,
  UserProfile,
} from './types';

const BASE = 'https://api.spotify.com/v1';

/** Search `limit` was capped at 10 by the Feb 2026 migration. */
export const SEARCH_LIMIT = 10;

export class SpotifyApiError extends Error {
  readonly status: number;
  readonly body: string;

  constructor(status: number, body: string, message?: string) {
    super(message ?? `Spotify API error ${status}: ${body}`);
    this.name = 'SpotifyApiError';
    this.status = status;
    this.body = body;
  }
}

interface RequestOptions {
  method?: 'GET' | 'PUT' | 'POST' | 'DELETE';
  body?: unknown;
  /** Treat 404 as "nothing here" and resolve to null instead of throwing. */
  nullOn404?: boolean;
  signal?: AbortSignal;
}

const MAX_RETRIES = 3;

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, nullOn404 = false, signal } = options;
  const url = path.startsWith('http') ? path : `${BASE}${path}`;

  let attempt = 0;
  // Retries cover 401 (stale token), 429 (rate limit) and 5xx.
  for (;;) {
    const token = await ensureAccessToken();
    const res = await fetch(url, {
      method,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal,
    });

    if (res.status === 204 || res.headers.get('content-length') === '0') {
      return undefined as T;
    }

    if (res.ok) {
      const text = await res.text();
      if (!text) return undefined as T;
      return JSON.parse(text) as T;
    }

    if (res.status === 404 && nullOn404) return null as T;

    const detail = await res.text();
    attempt += 1;

    if (res.status === 401 && attempt <= MAX_RETRIES) {
      invalidateAccessToken();
      continue;
    }

    if (res.status === 429 && attempt <= MAX_RETRIES) {
      const retryAfter = Number(res.headers.get('Retry-After') ?? '1');
      await sleep((Number.isFinite(retryAfter) ? retryAfter : 1) * 1000);
      continue;
    }

    if (res.status >= 500 && attempt <= MAX_RETRIES) {
      await sleep(400 * attempt);
      continue;
    }

    if (res.status === 401) throw new NotAuthenticatedError('Spotify session expired.');

    if (res.status === 403) {
      throw new SpotifyApiError(
        403,
        detail,
        'Spotify refused this request (403). In Development Mode your account must be added to the app\'s user list, and playback needs Premium.',
      );
    }

    throw new SpotifyApiError(res.status, detail);
  }
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Walks `next` links up to a page budget — the Kotlin `fetchAllPages`. */
async function fetchAllPages<T>(firstPath: string, maxPages = 10): Promise<T[]> {
  const all: T[] = [];
  let next: string | null = firstPath;
  let pages = 0;
  while (next && pages < maxPages) {
    const page: Paging<T> = await request<Paging<T>>(next);
    all.push(...(page.items ?? []));
    next = page.next;
    pages += 1;
  }
  return all;
}

// ---------------------------------------------------------------- profile

export function getMe(): Promise<UserProfile> {
  return request<UserProfile>('/me');
}

// ---------------------------------------------------------------- home

export function getRecentlyPlayed(limit = 50): Promise<CursorPaging<PlayHistoryItem>> {
  return request<CursorPaging<PlayHistoryItem>>(`/me/player/recently-played?limit=${limit}`);
}

export function getTopTracks(
  limit = 20,
  timeRange: 'short_term' | 'medium_term' | 'long_term' = 'medium_term',
): Promise<Paging<Track>> {
  return request<Paging<Track>>(`/me/top/tracks?limit=${limit}&time_range=${timeRange}`);
}

export function getTopArtists(
  limit = 20,
  timeRange: 'short_term' | 'medium_term' | 'long_term' = 'medium_term',
): Promise<Paging<Artist>> {
  return request<Paging<Artist>>(`/me/top/artists?limit=${limit}&time_range=${timeRange}`);
}

// ---------------------------------------------------------------- library

export function getMyPlaylists(limit = 50): Promise<Paging<Playlist>> {
  return request<Paging<Playlist>>(`/me/playlists?limit=${limit}`);
}

export function getAllMyPlaylists(): Promise<Playlist[]> {
  return fetchAllPages<Playlist>('/me/playlists?limit=50');
}

export function getSavedTracks(limit = 50, offset = 0): Promise<Paging<SavedTrack>> {
  return request<Paging<SavedTrack>>(`/me/tracks?limit=${limit}&offset=${offset}`);
}

export function getAllSavedTracks(): Promise<SavedTrack[]> {
  return fetchAllPages<SavedTrack>('/me/tracks?limit=50', 20);
}

export function getSavedAlbums(limit = 50): Promise<Paging<SavedAlbum>> {
  return request<Paging<SavedAlbum>>(`/me/albums?limit=${limit}`);
}

export function getFollowedArtists(limit = 50): Promise<{ artists: CursorPaging<Artist> }> {
  return request<{ artists: CursorPaging<Artist> }>(`/me/following?type=artist&limit=${limit}`);
}

// ---------------------------------------------------------------- entities

export function getPlaylist(id: string): Promise<Playlist> {
  return request<Playlist>(`/playlists/${id}`);
}

/**
 * Post-migration this is `/items`, not `/tracks`, and it only returns contents
 * for playlists the user owns or collaborates on. Falls back to `/tracks` so
 * the app keeps working on extended-quota credentials.
 */
export async function getPlaylistItems(id: string, limit = 100): Promise<PlaylistItem[]> {
  try {
    return await fetchAllPages<PlaylistItem>(`/playlists/${id}/items?limit=${limit}`, 5);
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      return fetchAllPages<PlaylistItem>(`/playlists/${id}/tracks?limit=${limit}`, 5);
    }
    throw error;
  }
}

export function getAlbum(id: string): Promise<Album> {
  return request<Album>(`/albums/${id}`);
}

export function getAlbumTracks(id: string, limit = 50): Promise<Paging<Track>> {
  return request<Paging<Track>>(`/albums/${id}/tracks?limit=${limit}`);
}

export function getArtist(id: string): Promise<Artist> {
  return request<Artist>(`/artists/${id}`);
}

export function getArtistAlbums(id: string, limit = 50): Promise<Paging<SimpleAlbum>> {
  return request<Paging<SimpleAlbum>>(
    `/artists/${id}/albums?limit=${limit}&include_groups=album,single`,
  );
}

export function getTrack(id: string): Promise<Track> {
  return request<Track>(`/tracks/${id}`);
}

// ---------------------------------------------------------------- search

export function search(
  query: string,
  types: Array<'track' | 'artist' | 'album' | 'playlist'> = ['track', 'artist', 'album', 'playlist'],
  limit = SEARCH_LIMIT,
  signal?: AbortSignal,
): Promise<SearchResults> {
  const params = new URLSearchParams({
    q: query,
    type: types.join(','),
    limit: String(Math.min(limit, SEARCH_LIMIT)),
  });
  return request<SearchResults>(`/search?${params.toString()}`, { signal });
}

// ---------------------------------------------------------------- library writes

/**
 * The generic library endpoints introduced in Feb 2026 replace
 * PUT/DELETE /me/tracks|/me/albums|/me/following. Falls back to the old
 * per-type endpoints when the account still has the pre-migration API.
 */
export async function saveToLibrary(uris: string[]): Promise<void> {
  try {
    await request<void>('/me/library', { method: 'PUT', body: { uris } });
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      await legacyLibraryWrite('PUT', uris);
      return;
    }
    throw error;
  }
}

export async function removeFromLibrary(uris: string[]): Promise<void> {
  try {
    await request<void>('/me/library', { method: 'DELETE', body: { uris } });
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      await legacyLibraryWrite('DELETE', uris);
      return;
    }
    throw error;
  }
}

async function legacyLibraryWrite(method: 'PUT' | 'DELETE', uris: string[]): Promise<void> {
  const byType = new Map<string, string[]>();
  for (const uri of uris) {
    const [, type, id] = uri.split(':');
    if (!type || !id) continue;
    const list = byType.get(type) ?? [];
    list.push(id);
    byType.set(type, list);
  }
  await Promise.all(
    Array.from(byType.entries()).map(([type, ids]) => {
      if (type === 'artist') {
        return request<void>(`/me/following?type=artist&ids=${ids.join(',')}`, { method });
      }
      return request<void>(`/me/${type}s?ids=${ids.join(',')}`, { method });
    }),
  );
}

export async function isInLibrary(uris: string[]): Promise<boolean[]> {
  if (uris.length === 0) return [];
  try {
    const params = new URLSearchParams({ uris: uris.join(',') });
    return await request<boolean[]>(`/me/library/contains?${params.toString()}`);
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      const ids = uris.map((uri) => uri.split(':')[2]).filter(Boolean);
      if (ids.length === 0) return uris.map(() => false);
      return request<boolean[]>(`/me/tracks/contains?ids=${ids.join(',')}`);
    }
    throw error;
  }
}

// ---------------------------------------------------------------- playlist writes

export async function createPlaylist(name: string, description = ''): Promise<Playlist> {
  try {
    return await request<Playlist>('/me/playlists', {
      method: 'POST',
      body: { name, description, public: false },
    });
  } catch (error) {
    if (error instanceof SpotifyApiError && error.status === 404) {
      const me = await getMe();
      return request<Playlist>(`/users/${me.id}/playlists`, {
        method: 'POST',
        body: { name, description, public: false },
      });
    }
    throw error;
  }
}

export async function addToPlaylist(playlistId: string, uris: string[]): Promise<void> {
  try {
    await request<void>(`/playlists/${playlistId}/items`, { method: 'POST', body: { uris } });
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      await request<void>(`/playlists/${playlistId}/tracks`, { method: 'POST', body: { uris } });
      return;
    }
    throw error;
  }
}

export async function removeFromPlaylist(playlistId: string, uris: string[]): Promise<void> {
  const payload = { items: uris.map((uri) => ({ uri })) };
  try {
    await request<void>(`/playlists/${playlistId}/items`, { method: 'DELETE', body: payload });
  } catch (error) {
    if (error instanceof SpotifyApiError && (error.status === 404 || error.status === 400)) {
      await request<void>(`/playlists/${playlistId}/tracks`, {
        method: 'DELETE',
        body: { tracks: uris.map((uri) => ({ uri })) },
      });
      return;
    }
    throw error;
  }
}

// ---------------------------------------------------------------- playback

export function transferPlayback(deviceId: string, play = false): Promise<void> {
  return request<void>('/me/player', { method: 'PUT', body: { device_ids: [deviceId], play } });
}

export function playUris(
  deviceId: string,
  uris: string[],
  offsetPosition = 0,
  positionMs = 0,
): Promise<void> {
  return request<void>(`/me/player/play?device_id=${deviceId}`, {
    method: 'PUT',
    body: { uris, offset: { position: offsetPosition }, position_ms: positionMs },
  });
}

export function playContext(
  deviceId: string,
  contextUri: string,
  offsetPosition = 0,
  positionMs = 0,
): Promise<void> {
  return request<void>(`/me/player/play?device_id=${deviceId}`, {
    method: 'PUT',
    body: { context_uri: contextUri, offset: { position: offsetPosition }, position_ms: positionMs },
  });
}

export function setShuffle(deviceId: string, state: boolean): Promise<void> {
  return request<void>(`/me/player/shuffle?state=${state}&device_id=${deviceId}`, { method: 'PUT' });
}

export function setRepeat(
  deviceId: string,
  state: 'off' | 'context' | 'track',
): Promise<void> {
  return request<void>(`/me/player/repeat?state=${state}&device_id=${deviceId}`, { method: 'PUT' });
}

export function addToQueue(deviceId: string, uri: string): Promise<void> {
  return request<void>(
    `/me/player/queue?uri=${encodeURIComponent(uri)}&device_id=${deviceId}`,
    { method: 'POST' },
  );
}
