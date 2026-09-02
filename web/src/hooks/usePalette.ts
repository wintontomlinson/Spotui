'use client';

import { useEffect } from 'react';

import { extractPalette } from '@/lib/palette';
import { usePlayerStore } from '@/store/usePlayerStore';

/**
 * Keeps usePlayerStore.palette in sync with the current track's artwork,
 * standing in for the Compose `LaunchedEffect` blocks that call
 * Palette().extractFirstColorFromImageUrl / extractSecondColorFromCoverUrl.
 */
export function useArtworkPalette(): void {
  const imageUrl = usePlayerStore((s) => s.track?.imageUrl ?? null);
  const setPalette = usePlayerStore((s) => s.setPalette);

  useEffect(() => {
    let cancelled = false;
    void extractPalette(imageUrl).then((palette) => {
      if (!cancelled) setPalette(palette);
    });
    return () => {
      cancelled = true;
    };
  }, [imageUrl, setPalette]);
}
