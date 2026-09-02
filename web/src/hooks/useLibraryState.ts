'use client';

import { useCallback, useEffect, useState } from 'react';

import { isInLibrary, removeFromLibrary, saveToLibrary } from '@/lib/spotify';

/**
 * Saved-state cache shared by every heart/plus button, so a list of 50 rows
 * issues one batched `contains` lookup rather than 50.
 */
const cache = new Map<string, boolean>();
const pending = new Map<string, Promise<boolean>>();
const subscribers = new Map<string, Set<(saved: boolean) => void>>();

let batchQueue: string[] = [];
let batchTimer: ReturnType<typeof setTimeout> | null = null;

function publish(uri: string, saved: boolean): void {
  cache.set(uri, saved);
  const listeners = subscribers.get(uri);
  if (listeners) for (const listener of listeners) listener(saved);
}

function flushBatch(): void {
  const uris = Array.from(new Set(batchQueue));
  batchQueue = [];
  batchTimer = null;
  if (uris.length === 0) return;

  // GET /me/library/contains takes a bounded list; 50 matches the API limit.
  for (let i = 0; i < uris.length; i += 50) {
    const chunk = uris.slice(i, i + 50);
    const promise = isInLibrary(chunk)
      .then((results) => {
        chunk.forEach((uri, index) => publish(uri, results[index] ?? false));
        return true;
      })
      .catch(() => {
        // A failed lookup should not render every row as saved.
        chunk.forEach((uri) => publish(uri, false));
        return false;
      });
    for (const uri of chunk) pending.set(uri, promise.then(() => cache.get(uri) ?? false));
  }
}

function enqueue(uri: string): void {
  if (cache.has(uri) || pending.has(uri)) return;
  batchQueue.push(uri);
  if (!batchTimer) batchTimer = setTimeout(flushBatch, 60);
}

export function useSavedState(uri: string | null | undefined) {
  const [saved, setSaved] = useState<boolean>(() => (uri ? (cache.get(uri) ?? false) : false));
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!uri) return;

    const listeners = subscribers.get(uri) ?? new Set<(value: boolean) => void>();
    listeners.add(setSaved);
    subscribers.set(uri, listeners);

    const known = cache.get(uri);
    if (known !== undefined) setSaved(known);
    else enqueue(uri);

    return () => {
      listeners.delete(setSaved);
      if (listeners.size === 0) subscribers.delete(uri);
    };
  }, [uri]);

  const toggle = useCallback(async () => {
    if (!uri || busy) return;
    const next = !saved;
    setBusy(true);
    publish(uri, next); // optimistic, like SpotifySync's local-first write
    try {
      if (next) await saveToLibrary([uri]);
      else await removeFromLibrary([uri]);
    } catch {
      publish(uri, !next); // roll back
    } finally {
      setBusy(false);
    }
  }, [uri, saved, busy]);

  return { saved, toggle, busy };
}
