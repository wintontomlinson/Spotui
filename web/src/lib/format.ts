import type { Image, SimpleArtist } from './types';

/** mm:ss, matching PlayerViewModel.formatDuration. */
export function formatDuration(ms: number | null | undefined): string {
  if (ms == null || Number.isNaN(ms) || ms < 0) return '0:00';
  const totalSeconds = Math.floor(ms / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

export function joinArtists(artists: SimpleArtist[] | undefined | null): string {
  if (!artists || artists.length === 0) return '';
  return artists.map((a) => a.name).join(', ');
}

/** Largest-first image list → a URL at roughly the requested size. */
export function pickImage(images: Image[] | null | undefined, minSize = 0): string | null {
  if (!images || images.length === 0) return null;
  const sorted = [...images].sort((a, b) => (b.width ?? 0) - (a.width ?? 0));
  if (minSize <= 0) return sorted[0].url;
  const match = [...sorted].reverse().find((img) => (img.width ?? 0) >= minSize);
  return (match ?? sorted[0]).url;
}

export function releaseYear(releaseDate: string | undefined | null): string {
  if (!releaseDate) return '';
  return releaseDate.slice(0, 4);
}

/** "1,234" for track counts. */
export function formatCount(value: number | null | undefined): string {
  if (value == null) return '0';
  return value.toLocaleString('en-US');
}

export function idFromUri(uri: string): string {
  const parts = uri.split(':');
  return parts[parts.length - 1] ?? '';
}
