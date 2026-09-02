/**
 * LRC parsing and title cleanup, ported from
 * app/src/main/java/com/music/spotui/data/api/LyricsApi.kt.
 */

import type { LyricLine } from './types';

const TIMESTAMP = /\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?\]/g;

/**
 * Parses `[mm:ss.xx]` LRC text. Handles several timestamp tags on one line and
 * normalises 1/2/3-digit fractions to milliseconds.
 */
export function parseLrc(lrc: string): LyricLine[] {
  const out: LyricLine[] = [];

  for (const rawLine of lrc.split('\n')) {
    TIMESTAMP.lastIndex = 0;
    const stamps: number[] = [];
    let match: RegExpExecArray | null;

    while ((match = TIMESTAMP.exec(rawLine)) !== null) {
      const minutes = Number(match[1]);
      const seconds = Number(match[2]);
      const fractionRaw = match[3] ?? '';
      let millis = 0;
      if (fractionRaw.length === 1) millis = Number(fractionRaw) * 100;
      else if (fractionRaw.length === 2) millis = Number(fractionRaw) * 10;
      else if (fractionRaw.length === 3) millis = Number(fractionRaw);
      stamps.push(minutes * 60_000 + seconds * 1000 + millis);
    }

    if (stamps.length === 0) continue;

    const text = rawLine.replace(TIMESTAMP, '').trim();
    if (!text) continue;
    for (const timeMs of stamps) out.push({ timeMs, text });
  }

  return out.sort((a, b) => a.timeMs - b.timeMs);
}

/** Splits unsynced plain lyrics into zero-timestamp lines. */
export function parsePlain(text: string): LyricLine[] {
  return text
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => ({ timeMs: 0, text: line }));
}

/**
 * Strips remaster/feature/version noise before querying LRCLIB — the Kotlin
 * `cleanTitle` drops everything after " - " and removes (feat. …) / (with …)
 * and [remaster|live|version|…] brackets.
 */
export function cleanTitle(title: string): string {
  let out = title;
  const dashIndex = out.indexOf(' - ');
  if (dashIndex > 0) out = out.slice(0, dashIndex);
  out = out.replace(/\((?:feat\.?|ft\.?|with)\s[^)]*\)/gi, '');
  out = out.replace(/\[[^\]]*(?:remaster|live|version|edit|mono|stereo|deluxe|bonus)[^\]]*\]/gi, '');
  out = out.replace(/\((?:[^)]*(?:remaster|remastered|live|version|edit|mono|stereo|deluxe|bonus)[^)]*)\)/gi, '');
  return out.replace(/\s{2,}/g, ' ').trim();
}

/** First artist only — LRCLIB matches poorly on "A, B, C". */
export function firstArtist(artists: string): string {
  return artists.split(/,|&|;|\bfeat\.?\b|\bft\.?\b/i)[0]?.trim() ?? artists.trim();
}

/** Index of the active line for a playback position, or -1 before the first. */
export function activeLineIndex(lines: LyricLine[], positionMs: number): number {
  if (lines.length === 0) return -1;
  let low = 0;
  let high = lines.length - 1;
  let result = -1;
  while (low <= high) {
    const mid = (low + high) >> 1;
    if (lines[mid].timeMs <= positionMs) {
      result = mid;
      low = mid + 1;
    } else {
      high = mid - 1;
    }
  }
  return result;
}
