# Spotui Web

A browser build of Spotui, deployable to Vercel. Next.js (App Router) + TypeScript + Tailwind, talking to the **official** Spotify Web API and streaming through the **Spotify Web Playback SDK**.

This is a separate front end, not a port of the Kotlin code — the Android app's Compose UI and its data layer cannot run in a browser. Read [Why this is not a 1:1 port](#why-this-is-not-a-11-port) before deploying, because the constraints are real and some of them are severe.

---

## Read this first: three hard limits

**1. Playback requires Spotify Premium.** The [Web Playback SDK requires a Premium subscription](https://developer.spotify.com/documentation/web-playback-sdk/) (mobile-only Premium plans excluded). On a free account browsing and search work, but no audio plays — the SDK raises `account_error` and the UI surfaces it.

**2. A Development Mode app is limited to 5 listeners.** Per the [February 2026 migration guide](https://developer.spotify.com/documentation/web-api/tutorials/february-2026-migration-guide), new apps get 1 Client ID and a maximum of 5 users, and **the app owner must hold an active Premium subscription** or the app stops working. Every listener must be added by hand under *Users and access* in the dashboard. This deployment therefore cannot be a public website. Lifting that cap means applying for extended quota mode.

**3. Spotify's own synced lyrics are not available.** Those come from a private endpoint that only accepts web-player tokens; this build uses [LRCLIB](https://lrclib.net) instead. See [Lyrics](#lyrics).

---

## Setup

### 1. Create a Spotify app

At the [developer dashboard](https://developer.spotify.com/dashboard):

- Add a redirect URI **exactly** matching where the app runs:
  - local: `http://127.0.0.1:3000/callback` — Spotify no longer accepts `localhost`, only the explicit loopback IP
  - production: `https://<your-deployment>.vercel.app/callback`
- Enable the **Web API** and **Web Playback SDK** APIs.
- Under *Users and access*, add every Spotify account that will sign in.

Copy the **Client ID**. There is no client secret to configure: the app uses Authorization Code with PKCE, so it holds no credentials at all.

### 2. Run it locally

```bash
cd web
cp .env.example .env.local     # then paste your Client ID in
npm install
npm run dev
```

Open <http://127.0.0.1:3000> — not `localhost`, or the redirect URI will not match.

### 3. Deploy to Vercel

Import the repository, then **set Root Directory to `web`**. This is the important step: the repository root is an Android Gradle project, and Vercel will fail the build if it looks there.

| Setting | Value |
| --- | --- |
| Root Directory | `web` |
| Framework Preset | Next.js (auto-detected) |
| Build Command | `npm run build` (default) |

Add both environment variables in the Vercel project:

| Variable | Value |
| --- | --- |
| `NEXT_PUBLIC_SPOTIFY_CLIENT_ID` | your Client ID |
| `NEXT_PUBLIC_REDIRECT_URI` | `https://<your-deployment>.vercel.app/callback` |

Then add that same callback URL to the Spotify dashboard's redirect URIs. Note that Vercel preview deployments get their own hostnames, so a preview URL will not authenticate unless you register its callback too.

---

## What works

- **Sign in** with Spotify OAuth (PKCE, no secret), with silent token refresh and single-flight refresh dedupe
- **Playback** in the browser: play/pause, seek, skip, shuffle, repeat (off → context → track), and "Play here" to claim the Spotify Connect device
- **Home** built from your recently played, top tracks, top artists, playlists and saved albums
- **Search** across tracks, artists, albums and playlists
- **Your Library** with type filters, text filter and A–Z sort
- **Liked Songs**, paginated in full
- **Album**, **artist** and **playlist** pages
- **Save / unsave** tracks and albums, with optimistic UI and batched `contains` lookups
- **Lyrics** from LRCLIB, line-highlighted against playback position, click a line to seek
- **Artwork-derived colour**, standing in for `di/Palette.kt` — dark-vibrant tints the mini player, muted drives the player gradient

## What is missing, and why

| Missing | Reason |
| --- | --- |
| Spotify's real home feed | Comes from the private pathfinder `home` GraphQL operation. No official equivalent. |
| Search "Browse all" genre grid | `GET /browse/categories` was removed for Development Mode apps. |
| Artist top tracks | `GET /artists/{id}/top-tracks` was removed. Artist pages show releases instead. |
| Track radio / autoplay | `GET /recommendations` is restricted, and the Android app's version used the private `inspiredby-mix` endpoint. |
| New releases row | `GET /browse/new-releases` was removed. |
| Tracklists of editorial playlists | Playlist contents are now returned **only** for playlists you own or collaborate on. Discover Weekly and friends come back as metadata only — **Play still works**, because playback is handed the playlist URI rather than a track list. |
| Spotify Canvas looping video | Private `spclient` endpoint. |
| Lossless / FLAC, downloads, offline cache, crossfade | The whole point of the Android app's audio engine, and none of it is reachable from a browser. Audio here is whatever the Web Playback SDK serves. |
| Spotify's synced lyrics | Private `color-lyrics` endpoint; needs a web-player token. |
| SpotifyMix fonts | Licensed Spotify assets, not redistributable. Figtree is used as a stand-in. |
| Podcasts / shows | Not wired up in this build. |

Search results are also capped at **10 per type** — the migration lowered the `limit` maximum from 50.

---

## Why this is not a 1:1 port

The Android app does not use Spotify OAuth at all. `spotify/src/main/kotlin/com/metrolist/spotify/SpotifyAuth.kt` harvests an `sp_dc` cookie from a WebView login, pulls a TOTP secret from a public GitHub Gist, and mints **web-player** tokens at `open.spotify.com/api/token`. Those tokens unlock the private API surface the app actually depends on: `api-partner.spotify.com/pathfinder` (GraphQL persisted queries) and `spclient.wg.spotify.com` (lyrics, track radio, Canvas).

None of that is portable to a browser:

- `sp_dc` is an **HttpOnly** cookie on a different origin, so JavaScript cannot read it and a fetch cannot attach it
- neither `api-partner` nor `spclient` sends CORS headers, so the browser blocks the response regardless
- proxying it server-side would mean the deployment holding one person's Spotify session cookie, and asking visitors to paste their own `sp_dc` into a website is a credential-harvesting pattern this build deliberately avoids

So the web app is built on the official, documented API. That is why the feature gap above exists: it is the gap between Spotify's private web-client API and what third-party developers are given.

---

## Architecture

```
web/src/
├── app/
│   ├── layout.tsx            root layout, font, AppShell
│   ├── page.tsx              Home
│   ├── login/ callback/      PKCE login and code exchange
│   ├── search/ library/ liked/
│   ├── album/[id]/ artist/[id]/ playlist/[id]/
│   └── api/
│       ├── lyrics/route.ts   LRCLIB lookup, server-side
│       └── art/route.ts      same-origin artwork proxy
├── components/               AppShell, BottomNav, MiniPlayer, PlayerOverlay, LyricsPanel, …
├── hooks/                    useWebPlayback, useLyrics, usePalette, useLibraryState
├── lib/
│   ├── auth.ts               PKCE: challenge, redirect, code exchange, refresh
│   ├── token.ts              token cache, 60 s expiry skew, single-flight refresh
│   ├── spotify.ts            API client with 401/429/5xx retry and pagination
│   ├── playback.ts           transport + Player API actions
│   ├── palette.ts            canvas histogram colour extraction
│   ├── lrc.ts                LRC parsing, title cleanup
│   └── mappers.ts            API entities → UI card items
├── store/                    Zustand: usePlayerStore, useAuthStore
└── types/spotify-sdk.d.ts    ambient Web Playback SDK types
```

Deliberate choices worth knowing about:

- **Transport vs. loading.** Play/pause/seek/skip go through the SDK instance because they are local and instant. Anything that changes *what* is loaded — a new track list, shuffle, repeat — goes through the Player REST API aimed at this tab's `device_id`.
- **`activateElement()` before first play.** Browsers block audio until a user gesture, so this is called inside the click handler.
- **The player is an overlay, not a route.** It mirrors `MyNavHost.kt`, where `player` is registered as a `dialog(...)` rather than a `composable`.
- **`/api/art` exists only for the canvas.** Spotify's image CDN sends no CORS headers, which would taint the canvas and make `getImageData()` throw. The proxy is host-allowlisted so it cannot be used as an open relay. Display images load straight from the CDN.
- **Lyrics are fetched server-side** so LRCLIB's rate limit is attributed to the deployment rather than every visitor, and responses are cached at the CDN.
- **Plain `<img>`, not `next/image`.** Spotify already serves these at several sizes; running them through the Next optimizer only adds Vercel image-transform billing.
- **Tailwind 3, not 4.** Keeps the JS config file and the token names mirrored from `ui/theme/Color.kt`.

### Ported behaviours

Some logic was carried across deliberately rather than reinvented:

- `lib/lrc.ts` — `parseLrc` and `cleanTitle` follow `LyricsApi.kt`, including multiple timestamps per line and stripping `" - 2011 Remaster"` / `(feat. …)` noise before querying
- `api/lyrics/route.ts` — fires the exact and fuzzy LRCLIB lookups **concurrently** (the Kotlin comment notes that serial fallbacks stacked three 5 s timeouts) and scores candidates with the same weights: `|durationDiff| + (synced ? 0 : 100k) + (artistMismatch ? 500k : 0)`
- `lib/token.ts` — mirrors `SpotifyTokenProvider.kt`: reuse until 60 s before expiry, serialise concurrent refreshes
- `lib/palette.ts` — approximates AndroidX Palette's dark-vibrant and muted targets on a 50×50 downscale, as `di/Palette.kt` does
- `mappers.ts` — dedupes feed items the way `Api.getHomeFeed` does, since recently-played repeats the same album

---

## Commands

```bash
npm run dev        # dev server
npm run build      # production build
npm run start      # serve the production build
npm run typecheck  # tsc --noEmit
```

## Disclaimer

Educational project. Spotify is a trademark of Spotify AB. Per the [Web Playback SDK terms](https://developer.spotify.com/documentation/web-playback-sdk/reference/), the Spotify platform may not be used to build commercial streaming integrations.
