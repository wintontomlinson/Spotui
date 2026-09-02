'use client';

import useSWR from 'swr';

import type { Lyrics } from '@/lib/types';

async function fetchLyrics(url: string): Promise<Lyrics | null> {
  const res = await fetch(url);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Lyrics lookup failed (${res.status}).`);
  return (await res.json()) as Lyrics;
}

/**
 * Lyrics for the given track. Deduped and cached by SWR; the route handler adds
 * an s-maxage so repeated lookups hit the CDN rather than LRCLIB.
 */
export function useLyrics(
  title: string | undefined,
  artist: string | undefined,
  album: string | undefined,
  durationMs: number | undefined,
) {
  const key =
    title && artist
      ? `/api/lyrics?${new URLSearchParams({
          title,
          artist,
          album: album ?? '',
          duration: String(Math.round((durationMs ?? 0) / 1000)),
        }).toString()}`
      : null;

  const { data, error, isLoading } = useSWR<Lyrics | null>(key, fetchLyrics, {
    revalidateOnFocus: false,
    shouldRetryOnError: false,
  });

  return { lyrics: data ?? null, error, isLoading: Boolean(key) && isLoading };
}
