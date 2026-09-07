# Spotu Web — YouTube Music Player

A polished, zero-build music website that streams audio from **YouTube**. Search
any song and play it instantly, or pick one of the built-in featured playlists.

It's plain **HTML + CSS + JavaScript** — no framework, no bundler, no server.
Open `index.html` and it works.

![tech: vanilla JS](https://img.shields.io/badge/tech-vanilla%20JS-f7df1e) ![no build](https://img.shields.io/badge/build-none-brightgreen)

---

## Features

- 🎵 **Play from YouTube** via the official [IFrame Player API](https://developers.google.com/youtube/iframe_api_reference) (audio only, hidden player)
- 🔎 **Search all of YouTube** when you add a free API key (music results only)
- 📀 **6 featured playlists** that play fully **without any key**
- ⏯️ Full transport: play/pause, next/prev, **shuffle**, **repeat** (off → all → one)
- 🎚️ Seekable progress bar, volume + mute
- ⌨️ Spacebar = play/pause
- 📱 Responsive — collapses to an icon sidebar on mobile
- 💾 Your API key is stored **only in your browser** (`localStorage`), never sent anywhere except Google

## How playback works

Audio comes from the YouTube IFrame player, which is the official, terms-compliant
way to play YouTube content on the web. The iframe is kept off-screen (1×1px) so
only the audio is heard while the site shows its own player UI.

## Run it

No build step. Any static server works:

```bash
cd web-music
python3 -m http.server 8080
# then open http://localhost:8080
```

Or just open `index.html` directly in a browser. (A local server is recommended
so the YouTube API and thumbnails load without file:// quirks.)

## Enable full search (optional)

Featured playlists play without setup. To search **all** of YouTube:

1. Create a free API key in the [Google Cloud Console](https://console.cloud.google.com/apis/library/youtube.googleapis.com):
   - Enable the **YouTube Data API v3**.
   - Create an **API key** under *Credentials*.
2. In the site, click **⚙ Settings** (bottom-left) and paste the key, then **Save**.

The key stays in your browser only. Search uses `youtube/v3/search` restricted to
the Music category (`videoCategoryId=10`). Note the Data API has a daily free quota
(each search costs 100 units of the default 10,000/day).

## Deploy

It's fully static, so it hosts anywhere:

- **GitHub Pages** — set the source to this folder (or copy its contents to the site root).
- **Vercel / Netlify** — deploy as a static site with this folder as the root; no build command.
- **Any web server** — copy the files and serve them.

## Files

```
web-music/
├── index.html      app shell (sidebar, views, player bar, settings dialog)
├── styles.css      dark, responsive music-app theme
├── app.js          player, queue, controls, search, view routing
├── playlists.js    curated featured playlists (real YouTube video IDs)
└── README.md
```

## Customising the playlists

Edit `playlists.js`. Each track needs a YouTube `videoId` (the `v=` value in a
watch URL). Example:

```js
{ title: "Song name", artist: "Artist", videoId: "dQw4w9WgXcQ" }
```

## Disclaimer

Educational project. Playback is delegated to YouTube's official embedded player;
this site does not download, re-host, or extract media. YouTube is a trademark of
Google LLC.
