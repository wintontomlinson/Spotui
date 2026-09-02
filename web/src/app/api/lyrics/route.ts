/**
 * Lyrics endpoint, backed by LRCLIB.
 *
 * The Android app prefers Spotify's own synced lyrics
 * (spclient.wg.spotify.com/color-lyrics/v2/track/{id}) and only falls back to
 * LRCLIB. That primary source is unavailable here: color-lyrics is a private
 * endpoint that accepts web-player tokens minted from an `sp_dc` cookie, not
 * OAuth tokens, and it sends no CORS headers. So LRCLIB is the only source, and
 * this route reproduces the Kotlin fallback logic from
 * app/src/main/java/com/music/spotui/data/api/LyricsApi.kt:
 *   - fire the exact /get and the fuzzy /search concurrently (serial fallbacks
 *     used to stack three 5 s timeouts)
 *   - score candidates by  |durationDiff| + (synced ? 0 : 100k) + (artistMismatch ? 500k : 0)
 *   - fall back to a title-only search as a last resort
 *
 * Running it server-side keeps LRCLIB's rate limit attributable to the
 * deployment rather than every visitor's IP, and avoids a CORS round trip.
 */

import { NextResponse } from 'next/server';

import { cleanTitle, firstArtist, parseLrc, parsePlain } from '@/lib/lrc';
import type { Lyrics } from '@/lib/types';

const LRCLIB = 'https://lrclib.net/api';
const USER_AGENT = 'spotui-web (https://github.com/wintontomlinson/Spotui)';
const TIMEOUT_MS = 6000;

interface LrclibRecord {
  id: number;
  trackName: string;
  artistName: string;
  albumName: string | null;
  duration: number | null;
  instrumental: boolean;
  plainLyrics: string | null;
  syncedLyrics: string | null;
}

async function lrclibFetch<T>(path: string): Promise<T | null> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const res = await fetch(`${LRCLIB}${path}`, {
      headers: { 'User-Agent': USER_AGENT, Accept: 'application/json' },
      signal: controller.signal,
      next: { revalidate: 86_400 },
    });
    if (!res.ok) return null;
    return (await res.json()) as T;
  } catch {
    return null;
  } finally {
    clearTimeout(timer);
  }
}

/** Lower is better, mirroring the Kotlin candidate scoring. */
function scoreCandidate(record: LrclibRecord, durationSec: number, artist: string): number {
  const durationDiff =
    durationSec > 0 && record.duration != null ? Math.abs(record.duration - durationSec) : 0;
  const syncedPenalty = record.syncedLyrics ? 0 : 100_000;
  const wanted = artist.toLowerCase();
  const got = (record.artistName ?? '').toLowerCase();
  const artistMismatch =
    wanted && got && !got.includes(wanted) && !wanted.includes(got) ? 500_000 : 0;
  return durationDiff + syncedPenalty + artistMismatch;
}

function toLyrics(record: LrclibRecord): Lyrics | null {
  if (record.instrumental) {
    return { lines: [{ timeMs: 0, text: '♪ Instrumental' }], synced: false, source: 'lrclib' };
  }
  if (record.syncedLyrics) {
    const lines = parseLrc(record.syncedLyrics);
    if (lines.length > 0) return { lines, synced: true, source: 'lrclib' };
  }
  if (record.plainLyrics) {
    const lines = parsePlain(record.plainLyrics);
    if (lines.length > 0) return { lines, synced: false, source: 'lrclib' };
  }
  return null;
}

export async function GET(request: Request): Promise<NextResponse> {
  const params = new URL(request.url).searchParams;
  const rawTitle = params.get('title')?.trim();
  const rawArtist = params.get('artist')?.trim() ?? '';
  const album = params.get('album')?.trim() ?? '';
  const durationSec = Math.round(Number(params.get('duration') ?? '0'));

  if (!rawTitle) {
    return NextResponse.json({ error: 'Missing title parameter.' }, { status: 400 });
  }

  const title = cleanTitle(rawTitle);
  const artist = firstArtist(rawArtist);

  const exactQuery = new URLSearchParams({ track_name: title, artist_name: artist });
  if (album) exactQuery.set('album_name', album);
  if (durationSec > 0) exactQuery.set('duration', String(durationSec));

  const fuzzyQuery = new URLSearchParams({ track_name: title });
  if (artist) fuzzyQuery.set('artist_name', artist);

  // Concurrent, not serial — the Kotlin comment notes serial fallbacks stacked timeouts.
  const [exact, fuzzy] = await Promise.all([
    lrclibFetch<LrclibRecord>(`/get?${exactQuery.toString()}`),
    lrclibFetch<LrclibRecord[]>(`/search?${fuzzyQuery.toString()}`),
  ]);

  const candidates: LrclibRecord[] = [];
  if (exact) candidates.push(exact);
  if (Array.isArray(fuzzy)) candidates.push(...fuzzy);

  if (candidates.length === 0) {
    // Last resort: title only.
    const titleOnly = await lrclibFetch<LrclibRecord[]>(
      `/search?${new URLSearchParams({ track_name: title }).toString()}`,
    );
    if (Array.isArray(titleOnly)) candidates.push(...titleOnly);
  }

  const ranked = candidates
    .map((record) => ({ record, value: scoreCandidate(record, durationSec, artist) }))
    .sort((a, b) => a.value - b.value);

  for (const { record } of ranked) {
    const lyrics = toLyrics(record);
    if (lyrics) {
      return NextResponse.json(lyrics, {
        headers: { 'Cache-Control': 'public, s-maxage=86400, stale-while-revalidate=604800' },
      });
    }
  }

  return NextResponse.json({ error: 'No lyrics found.' }, { status: 404 });
}
