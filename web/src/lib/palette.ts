/**
 * Artwork colour extraction, standing in for di/Palette.kt (AndroidX Palette).
 *
 * The Kotlin code uses two extractors:
 *   - extractFirstColorFromImageUrl  → darkVibrantSwatch, for the mini player background
 *   - extractSecondColorFromCoverUrl → mutedSwatch on a 50×50 downscale, for the
 *     player's gradient top colour and the lyrics accent
 * Both force full opacity and no-op when the swatch is missing, so callers keep
 * a sensible default. This reproduces that behaviour with a canvas histogram.
 *
 * Spotify's image CDN does not send permissive CORS headers, so the bitmap is
 * pulled through our own /api/art proxy — same-origin keeps the canvas
 * untainted and getImageData() legal.
 */

export interface ArtworkPalette {
  /** darkVibrantSwatch equivalent — mini player background. */
  darkVibrant: string;
  /** mutedSwatch equivalent — player gradient top colour, lyrics accent. */
  muted: string;
}

export const DEFAULT_PALETTE: ArtworkPalette = {
  // GridBackground 0xFF2A2A2A / AppBackground 0xFF0B0B0F
  darkVibrant: '#2A2A2A',
  muted: '#0B0B0F',
};

const cache = new Map<string, ArtworkPalette>();

interface Swatch {
  r: number;
  g: number;
  b: number;
  h: number;
  s: number;
  l: number;
  population: number;
}

function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  const rf = r / 255;
  const gf = g / 255;
  const bf = b / 255;
  const max = Math.max(rf, gf, bf);
  const min = Math.min(rf, gf, bf);
  const l = (max + min) / 2;
  if (max === min) return [0, 0, l];
  const d = max - min;
  const s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
  let h: number;
  if (max === rf) h = ((gf - bf) / d + (gf < bf ? 6 : 0)) / 6;
  else if (max === gf) h = ((bf - rf) / d + 2) / 6;
  else h = ((rf - gf) / d + 4) / 6;
  return [h, s, l];
}

function toHex(r: number, g: number, b: number): string {
  const part = (v: number) => Math.max(0, Math.min(255, Math.round(v))).toString(16).padStart(2, '0');
  return `#${part(r)}${part(g)}${part(b)}`;
}

/**
 * Scores a swatch against an AndroidX Palette-style target.
 * Weights follow Palette's defaults: saturation 0.24, lightness 0.52,
 * population 0.24.
 */
function score(
  swatch: Swatch,
  targetSaturation: number,
  targetLightness: number,
  maxPopulation: number,
): number {
  const saturationScore = 1 - Math.abs(swatch.s - targetSaturation);
  const lightnessScore = 1 - Math.abs(swatch.l - targetLightness);
  const populationScore = maxPopulation > 0 ? swatch.population / maxPopulation : 0;
  return saturationScore * 0.24 + lightnessScore * 0.52 + populationScore * 0.24;
}

function quantize(data: Uint8ClampedArray): Swatch[] {
  // 5 bits per channel → at most 32³ buckets, the same trade-off Palette makes.
  const buckets = new Map<number, { r: number; g: number; b: number; count: number }>();

  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    if (alpha < 125) continue;
    const r = data[i];
    const g = data[i + 1];
    const b = data[i + 2];
    const key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3);
    const bucket = buckets.get(key);
    if (bucket) {
      bucket.r += r;
      bucket.g += g;
      bucket.b += b;
      bucket.count += 1;
    } else {
      buckets.set(key, { r, g, b, count: 1 });
    }
  }

  const swatches: Swatch[] = [];
  for (const bucket of buckets.values()) {
    const r = bucket.r / bucket.count;
    const g = bucket.g / bucket.count;
    const b = bucket.b / bucket.count;
    const [h, s, l] = rgbToHsl(r, g, b);
    // Palette ignores near-white and near-black, and the red-tinted skin range.
    if (l > 0.95 || l < 0.05) continue;
    swatches.push({ r, g, b, h, s, l, population: bucket.count });
  }
  return swatches;
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error(`Failed to load artwork: ${src}`));
    img.src = src;
  });
}

export async function extractPalette(imageUrl: string | null): Promise<ArtworkPalette> {
  if (!imageUrl) return DEFAULT_PALETTE;
  const cached = cache.get(imageUrl);
  if (cached) return cached;

  try {
    const proxied = `/api/art?url=${encodeURIComponent(imageUrl)}`;
    const img = await loadImage(proxied);

    // Matches extractSecondColorFromCoverUrl's 50×50 downscale.
    const size = 50;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d', { willReadFrequently: true });
    if (!ctx) return DEFAULT_PALETTE;
    ctx.drawImage(img, 0, 0, size, size);

    const { data } = ctx.getImageData(0, 0, size, size);
    const swatches = quantize(data);
    if (swatches.length === 0) return DEFAULT_PALETTE;

    const maxPopulation = swatches.reduce((max, s) => Math.max(max, s.population), 0);

    // DARK_VIBRANT: target saturation 1.0, target lightness 0.26, lightness <= 0.45
    const darkVibrantCandidates = swatches.filter((s) => s.l <= 0.45 && s.s >= 0.35);
    const darkVibrant = pickBest(
      darkVibrantCandidates.length > 0 ? darkVibrantCandidates : swatches,
      1.0,
      0.26,
      maxPopulation,
    );

    // MUTED: target saturation 0.3 (max 0.4), target lightness 0.5
    const mutedCandidates = swatches.filter((s) => s.s <= 0.4);
    const muted = pickBest(
      mutedCandidates.length > 0 ? mutedCandidates : swatches,
      0.3,
      0.5,
      maxPopulation,
    );

    const palette: ArtworkPalette = {
      darkVibrant: darkVibrant ? toHex(darkVibrant.r, darkVibrant.g, darkVibrant.b) : DEFAULT_PALETTE.darkVibrant,
      muted: muted ? darken(muted) : DEFAULT_PALETTE.muted,
    };
    cache.set(imageUrl, palette);
    return palette;
  } catch {
    // Palette.kt silently no-ops when extraction fails; so do we.
    return DEFAULT_PALETTE;
  }
}

function pickBest(
  swatches: Swatch[],
  targetSaturation: number,
  targetLightness: number,
  maxPopulation: number,
): Swatch | null {
  let best: Swatch | null = null;
  let bestScore = -1;
  for (const swatch of swatches) {
    const value = score(swatch, targetSaturation, targetLightness, maxPopulation);
    if (value > bestScore) {
      bestScore = value;
      best = swatch;
    }
  }
  return best;
}

/**
 * The player gradient runs from this colour into black, so a mid-lightness
 * muted swatch is pulled down to keep white text legible over it.
 */
function darken(swatch: Swatch): string {
  const factor = swatch.l > 0.35 ? 0.35 / swatch.l : 1;
  return toHex(swatch.r * factor, swatch.g * factor, swatch.b * factor);
}
