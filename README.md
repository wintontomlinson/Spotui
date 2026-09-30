<p align="center"><img src="assets/icon.png" width="128" alt="Solo app icon"></p>

# Solo

A premium music player for Android, built with Jetpack Compose. Search and play any song for free, with no account required.

## Features

- 🎵 **Free search and play**: find any song and play it instantly, with no login.
- 🏠 **Home and Explore**: shelves based on what you play, plus image-led mood and genre tiles.
- 📝 **Lyrics**: synced lyrics with a live preview on the player, a full-screen view and on-device translation.
- 🎧 **High-quality audio**: direct, ad-free streaming, including lossless FLAC through community providers.
- 🎚️ **Crossfade and DJ-style mixing**, **downloads** and **offline caching**.
- ♾️ **Autoplay radio**: when the queue ends, related songs keep playing.
- 📀 **Spotify (optional)**: sign in to bring your playlists, liked songs, followed artists, history and recommendations.
- ✨ **"Midnight Velvet & Gold" design**: a floating glass tab bar, an artwork-tinted player and smooth motion throughout.

## Install

Download the latest APK from [Releases](https://github.com/wintontomlinson/Spotui/releases) and sideload it (Android 8.0 or later). Allow "install from unknown sources" when asked.

Solo keeps the package name of earlier Spotui builds, so it installs over them and keeps your library, likes, downloads, settings and Spotify login.

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
