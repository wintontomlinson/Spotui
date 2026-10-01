<p align="center"><img src="assets/icon.png" width="128" alt="Solo app icon"></p>

# Solo

A premium music player for Android, built with Jetpack Compose. Search and play any song for free, with no account required.

## Features

- ⚡ **Instant playback**: the next songs are resolved and pre-buffered ahead of time, so tracks start right away.
- 🎧 **High-quality audio by default**: the best Opus/AAC stream, and FLAC for your local and downloaded files.
- 🧠 **Smart recommendations**: an on-device taste profile (plays, skips, likes, recency) drives autoplay radio and Home, with artist variety and no recent repeats.
- 🌅 **Mix for you**: a fresh daily mix, plus "Jump back in" and your top artists.
- 🎵 **Free search and play**: find any song and play it, with no login.
- 🎚️ **Equalizer, speed and pitch, volume normalization**, crossfade with DJ-style mixing, downloads and offline caching.
- 📝 **Lyrics**: synced lyrics with a live preview on the player, a full-screen view and on-device translation.
- 📀 **Spotify (optional)**: sign in to bring your playlists, liked songs, followed artists, history and recommendations.
- ✨ **"Obsidian Aurora" design**: an artwork-tinted player, a floating glass tab bar and smooth motion throughout.

The icon is **Solo Facet**: an original four-facet gem lit from the top right, with a single dot for "solo".

See [CHANGELOG.md](CHANGELOG.md) for everything new in 3.1.0.

## Install

Download the latest APK from [Releases](https://github.com/wintontomlinson/Spotui/releases) and sideload it (Android 8.0 or later). Allow "install from unknown sources" when asked.

Solo keeps the package name of earlier Spotui builds, so it installs over them and keeps your library, likes, downloads, settings and Spotify login.

If Android refuses to install the APK, uninstall any copy that came from a different source, download the APK again in full, and allow the install when Play Protect asks.

## Build from source

Requirements: JDK 17 and the Android SDK (compileSdk 37).

```bash
./gradlew :app:assembleDebug     # debug build: app/build/outputs/apk/debug/Solo_v<version>.apk
./gradlew :app:assembleRelease   # release build
```

Releases are published by the **Build & Release APK** GitHub Actions workflow (run it manually or push a `v*` tag).

## Credits

Solo builds on these open-source projects:

- [Neptune](https://github.com/navneet851/spotify-clone-jetpack-compose): the original Jetpack Compose music player this app started from.
- [Metrolist](https://github.com/MetrolistGroup/Metrolist): the YouTube Music streaming internals (InnerTube client, stream cipher and PoToken handling).
- [SpotiFLAC](https://github.com/spotbye/SpotiFLAC): lossless (FLAC) track resolving.
- [SimpMusic](https://github.com/maxrave-dev/SimpMusic): crossfade and DJ-style audio filter processing.
- [Fraunces](https://github.com/undercasetype/Fraunces) and [Manrope](https://github.com/googlefonts/manrope): typefaces under the SIL Open Font License 1.1 (see [`licenses/fonts`](licenses/fonts)).

Maintained by **SATYAN SHARMA**.

## License

Solo is free software released under the [GNU General Public License v3.0](LICENSE).

## Disclaimer

This project is for educational purposes only. Spotify is a trademark of Spotify AB and YouTube is a trademark of Google LLC. Solo is not affiliated with, or endorsed by, either company.
