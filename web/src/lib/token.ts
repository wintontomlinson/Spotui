/**
 * Access-token lifecycle, mirroring
 * app/src/main/java/com/music/spotui/data/api/SpotifyTokenProvider.kt:
 * reuse the cached token until 60 s before expiry, otherwise refresh — and
 * serialise concurrent refreshes so a burst of requests mints only one token.
 */

import { clearTokens, loadTokens, refreshTokens, saveTokens, type StoredTokens } from './auth';

const EXPIRY_SKEW_MS = 60_000;

let cached: StoredTokens | null = null;
let inFlight: Promise<StoredTokens> | null = null;
const listeners = new Set<(signedIn: boolean) => void>();

function notify(signedIn: boolean): void {
  for (const listener of listeners) listener(signedIn);
}

export function onAuthChange(listener: (signedIn: boolean) => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function primeTokens(tokens: StoredTokens): void {
  cached = tokens;
  saveTokens(tokens);
  notify(true);
}

export function hasSession(): boolean {
  return (cached ?? loadTokens()) !== null;
}

export function signOut(): void {
  cached = null;
  inFlight = null;
  clearTokens();
  notify(false);
}

/** Thrown when there is no usable session and the user must log in again. */
export class NotAuthenticatedError extends Error {
  constructor(message = 'Not signed in to Spotify.') {
    super(message);
    this.name = 'NotAuthenticatedError';
  }
}

export async function ensureAccessToken(): Promise<string> {
  const current = cached ?? loadTokens();
  if (!current) throw new NotAuthenticatedError();
  cached = current;

  if (current.expiresAt - EXPIRY_SKEW_MS > Date.now()) return current.accessToken;

  if (!inFlight) {
    inFlight = refreshTokens(current.refreshToken)
      .then((next) => {
        cached = next;
        return next;
      })
      .catch((error: unknown) => {
        // A failed refresh means the grant is gone — force a fresh login.
        signOut();
        throw error instanceof Error ? error : new NotAuthenticatedError();
      })
      .finally(() => {
        inFlight = null;
      });
  }

  const refreshed = await inFlight;
  return refreshed.accessToken;
}

/** Forces the next call to refresh — used after a 401 from the API. */
export function invalidateAccessToken(): void {
  if (cached) cached = { ...cached, expiresAt: 0 };
}
