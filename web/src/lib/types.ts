/**
 * Minimal Spotify Web API types, as returned by the API *after* the
 * February/March 2026 Development Mode changes.
 *
 * Fields removed in that migration (track.popularity, album.label,
 * artist.followers, me.product, available_markets, ...) are deliberately
 * absent or optional — see web/README.md.
 */

export interface Image {
  url: string;
  height: number | null;
  width: number | null;
}

export interface SimpleArtist {
  id: string;
  name: string;
  uri: string;
  type: 'artist';
}

export interface Artist extends SimpleArtist {
  images?: Image[];
  genres?: string[];
}

export interface SimpleAlbum {
  id: string;
  name: string;
  uri: string;
  type: 'album';
  album_type?: string;
  release_date?: string;
  total_tracks?: number;
  images: Image[];
  artists: SimpleArtist[];
}

export interface Album extends SimpleAlbum {
  tracks?: Paging<Track>;
}

export interface Track {
  id: string | null;
  name: string;
  uri: string;
  type: 'track';
  duration_ms: number;
  explicit: boolean;
  track_number?: number;
  disc_number?: number;
  is_playable?: boolean;
  album?: SimpleAlbum;
  artists: SimpleArtist[];
}

export interface Episode {
  id: string;
  name: string;
  uri: string;
  type: 'episode';
  duration_ms: number;
  explicit: boolean;
  images: Image[];
  release_date?: string;
  description?: string;
}

export interface Playlist {
  id: string;
  name: string;
  uri: string;
  type: 'playlist';
  description: string | null;
  collaborative: boolean;
  public: boolean | null;
  images: Image[] | null;
  owner: { id: string; display_name?: string | null; uri: string };
  /**
   * Post-migration name for what used to be `tracks`. Kept optional because
   * for playlists the user neither owns nor collaborates on, the API omits it
   * entirely and returns metadata only.
   */
  items?: { href: string; total: number } | null;
  /** Pre-migration name — still present on extended-quota apps. */
  tracks?: { href: string; total: number } | null;
}

export interface Paging<T> {
  href: string;
  items: T[];
  limit: number;
  next: string | null;
  offset: number;
  previous: string | null;
  total: number;
}

export interface CursorPaging<T> {
  href: string;
  items: T[];
  limit: number;
  next: string | null;
  cursors: { after?: string; before?: string } | null;
  total?: number;
}

/**
 * A row inside a playlist's item list. The migration renamed `track` to
 * `item`; both are accepted so the client works either side of the change.
 */
export interface PlaylistItem {
  added_at: string | null;
  item?: Track | Episode | null;
  track?: Track | Episode | null;
}

export interface SavedTrack {
  added_at: string;
  track?: Track | null;
  item?: Track | null;
}

export interface SavedAlbum {
  added_at: string;
  album?: SimpleAlbum | null;
  item?: SimpleAlbum | null;
}

export interface PlayHistoryItem {
  track: Track;
  played_at: string;
  context: { uri: string; type: string } | null;
}

export interface UserProfile {
  id: string;
  display_name: string | null;
  uri: string;
  images?: Image[];
  /** Removed by the Feb 2026 migration for Development Mode apps. */
  product?: string;
}

export interface SearchResults {
  tracks?: Paging<Track>;
  artists?: Paging<Artist>;
  albums?: Paging<SimpleAlbum>;
  playlists?: Paging<Playlist | null>;
}

/** Normalised shape the UI renders, regardless of source entity type. */
export interface MediaCardItem {
  id: string;
  uri: string;
  title: string;
  subtitle: string;
  imageUrl: string | null;
  href: string | null;
  round?: boolean;
}

export type RepeatMode = 'off' | 'context' | 'track';

/** Lyrics, mirroring data/entity/Lyrics.kt. */
export interface LyricLine {
  timeMs: number;
  text: string;
}

export interface Lyrics {
  lines: LyricLine[];
  synced: boolean;
  source: 'lrclib';
}
