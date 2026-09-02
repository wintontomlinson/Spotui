/**
 * Spotify Authorization Code flow with PKCE.
 *
 * The Android app does not do this — it harvests an `sp_dc` cookie in a WebView
 * and mints web-player tokens with a TOTP secret pulled from a public Gist
 * (see spotify/src/main/kotlin/com/metrolist/spotify/SpotifyAuth.kt). That
 * approach cannot work in a browser: `sp_dc` is an HttpOnly cookie on a
 * different origin, and neither api-partner.spotify.com nor
 * spclient.wg.spotify.com send CORS headers. So this port uses the official
 * public-client flow, which needs no secret and is safe to run entirely
 * client-side.
 */

const AUTH_ENDPOINT = 'https://accounts.spotify.com/authorize';
const TOKEN_ENDPOINT = 'https://accounts.spotify.com/api/token';

const VERIFIER_KEY = 'spotui.pkce_verifier';
const STATE_KEY = 'spotui.pkce_state';
const TOKENS_KEY = 'spotui.tokens';

export const SCOPES = [
  // Web Playback SDK
  'streaming',
  'user-read-email',
  'user-read-private',
  // transport control
  'user-read-playback-state',
  'user-modify-playback-state',
  'user-read-currently-playing',
  // library
  'user-library-read',
  'user-library-modify',
  // playlists
  'playlist-read-private',
  'playlist-read-collaborative',
  'playlist-modify-private',
  'playlist-modify-public',
  // home / stats
  'user-top-read',
  'user-read-recently-played',
  'user-follow-read',
  'user-follow-modify',
] as const;

export interface StoredTokens {
  accessToken: string;
  refreshToken: string;
  /** Epoch ms. */
  expiresAt: number;
}

export function getClientId(): string {
  const id = process.env.NEXT_PUBLIC_SPOTIFY_CLIENT_ID;
  if (!id) {
    throw new Error(
      'NEXT_PUBLIC_SPOTIFY_CLIENT_ID is not set. Copy web/.env.example to web/.env.local and fill it in.',
    );
  }
  return id;
}

export function getRedirectUri(): string {
  const configured = process.env.NEXT_PUBLIC_REDIRECT_URI;
  if (configured) return configured;
  if (typeof window !== 'undefined') return `${window.location.origin}/callback`;
  throw new Error('NEXT_PUBLIC_REDIRECT_URI is not set.');
}

function base64UrlEncode(bytes: ArrayBuffer): string {
  let binary = '';
  const view = new Uint8Array(bytes);
  for (let i = 0; i < view.byteLength; i += 1) binary += String.fromCharCode(view[i]);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function randomString(length: number): string {
  const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
  const values = new Uint8Array(length);
  crypto.getRandomValues(values);
  return Array.from(values, (v) => charset[v % charset.length]).join('');
}

async function sha256(input: string): Promise<ArrayBuffer> {
  return crypto.subtle.digest('SHA-256', new TextEncoder().encode(input));
}

/** Kicks off the redirect to Spotify's consent page. */
export async function beginLogin(returnTo?: string): Promise<void> {
  const verifier = randomString(96);
  const state = randomString(24);
  const challenge = base64UrlEncode(await sha256(verifier));

  sessionStorage.setItem(VERIFIER_KEY, verifier);
  sessionStorage.setItem(STATE_KEY, state);
  if (returnTo) sessionStorage.setItem('spotui.return_to', returnTo);

  const params = new URLSearchParams({
    client_id: getClientId(),
    response_type: 'code',
    redirect_uri: getRedirectUri(),
    code_challenge_method: 'S256',
    code_challenge: challenge,
    state,
    scope: SCOPES.join(' '),
  });

  window.location.assign(`${AUTH_ENDPOINT}?${params.toString()}`);
}

/** Exchanges the `?code=` from the callback for tokens. */
export async function completeLogin(code: string, state: string | null): Promise<StoredTokens> {
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  const expectedState = sessionStorage.getItem(STATE_KEY);

  if (!verifier) throw new Error('Missing PKCE verifier — restart the login from /login.');
  if (expectedState && state && expectedState !== state) {
    throw new Error('State mismatch — the login response did not match this browser session.');
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: getRedirectUri(),
    client_id: getClientId(),
    code_verifier: verifier,
  });

  const res = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`Token exchange failed (${res.status}): ${detail}`);
  }

  const json = (await res.json()) as {
    access_token: string;
    refresh_token: string;
    expires_in: number;
  };

  sessionStorage.removeItem(VERIFIER_KEY);
  sessionStorage.removeItem(STATE_KEY);

  const tokens: StoredTokens = {
    accessToken: json.access_token,
    refreshToken: json.refresh_token,
    expiresAt: Date.now() + json.expires_in * 1000,
  };
  saveTokens(tokens);
  return tokens;
}

export async function refreshTokens(refreshToken: string): Promise<StoredTokens> {
  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: getClientId(),
  });

  const res = await fetch(TOKEN_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  });

  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`Token refresh failed (${res.status}): ${detail}`);
  }

  const json = (await res.json()) as {
    access_token: string;
    refresh_token?: string;
    expires_in: number;
  };

  const tokens: StoredTokens = {
    accessToken: json.access_token,
    // Spotify rotates refresh tokens; fall back to the existing one when absent.
    refreshToken: json.refresh_token ?? refreshToken,
    expiresAt: Date.now() + json.expires_in * 1000,
  };
  saveTokens(tokens);
  return tokens;
}

export function loadTokens(): StoredTokens | null {
  if (typeof window === 'undefined') return null;
  const raw = window.localStorage.getItem(TOKENS_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as StoredTokens;
    if (!parsed.accessToken || !parsed.refreshToken) return null;
    return parsed;
  } catch {
    return null;
  }
}

export function saveTokens(tokens: StoredTokens): void {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(TOKENS_KEY, JSON.stringify(tokens));
}

export function clearTokens(): void {
  if (typeof window === 'undefined') return;
  window.localStorage.removeItem(TOKENS_KEY);
}

export function consumeReturnTo(): string {
  if (typeof window === 'undefined') return '/';
  const target = sessionStorage.getItem('spotui.return_to') ?? '/';
  sessionStorage.removeItem('spotui.return_to');
  return target;
}
