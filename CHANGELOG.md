# Spotui: Fork Features & Differences

This document outlines the custom features, improvements, and differences introduced in this fork
compared to the main Spotui repository.

---

## 🚀 Release v1.6.6

### 🔍 Playback Diagnostics

The playback log covered what happened once a stream was open, but said nothing about how the stream
was obtained. That is the missing half: a stream refused on playback means something entirely
different depending on which client minted it and whether a PoToken was available.

* **Resolution is logged.** Each attempt records whether a PoToken and signature timestamp were
  obtained and whether the main client was skipped, then either the client that produced the stream
  with its format and expiry, or the failure and how far the client chain got.

No playback behaviour changed in this release.

---

## 🚀 Release v1.6.5

### 🛠 Playback Fixes

* **Byte offsets go back to a Range header.** Putting the offset in the URL's query string, added in
  1.6.3, is reverted. The log line `opened -1 bytes from position 703837 via query range` followed by
  `ERROR_CODE_PARSING_CONTAINER_MALFORMED` says it plainly: that request came back with no content
  length and contents the extractor could not parse, which is what a response framed in another
  protocol looks like to a media extractor. Raw bytes with a Range header are what the player can
  read, so that is what playback asks for again.
* **Playback stops instead of racing through the queue.** When three tracks fail back to back,
  playback pauses and says so rather than skipping onward, which looked like every song being
  skipped. Skipping also meant a burst of further requests, which makes a host that is already
  refusing them refuse more.

Together with 1.6.4 this leaves the request shape playback originally had, one plain request per
stream URL, minus the intro preload that was spending that request before the song ever started.

---

## 🚀 Release v1.6.4

### 🛠 Playback Fixes

The playback log finally showed the cause of songs stopping halfway. `stream opened 1048576 bytes
from position 0` appeared over and over, and every single failure was a request for the bytes after
that same point being refused with 403. 1048576 is the size of the intro preload.

* **The intro preload is gone.** It downloaded the opening megabyte of upcoming tracks into the media
  cache using the freshly resolved stream URL. These hosts serve one request per stream URL, so the
  preload spent the track's only request. Playback then had about a minute of cached audio and no way
  to fetch anything after it, which is where the song stopped. At the bitrates in the logs that
  megabyte is 55 to 60 seconds, which is half of a short song, exactly what it looked like. Some
  tracks would not start at all, which was the same thing happening at the very first byte.
  Resolving the URL ahead of time, which is what actually hid the tap latency, still happens.
* **Only the network facing layer may put an offset in the URL.** The log showed the wrapper above
  the cache opening `position 1048576 via query range` and getting a length of -1 back, because a
  rewritten URL is a different resource as far as the cache is concerned. That rewrite is now
  restricted to the one instance that talks to the network.
* **The cache directory is versioned again**, so the one megabyte fragments earlier builds wrote are
  abandoned instead of being read back and failed on.

---

## 🚀 Release v1.6.3

### 🛠 Playback Fixes

From a second playback log, which showed a stream open successfully for 3261926 bytes and then fail
with `Response code: 403` after only 194 bytes, on both recovery attempts, as soon as a request at a
non zero offset was needed. These hosts serve the first request for a URL and refuse later ones when
the offset is asked for with a Range header.

* **Byte offsets go in the query string for these hosts.** Every request past the first one, whether
  it comes from a seek or from reconnecting after a drop, now carries the offset as a query parameter
  and sends no Range header, which is how the official clients ask. Header based offsets were being
  refused outright, which broke seeking and made every recovery attempt fail at the same place.
* **The base URL is reported to the cache, never a ranged one.** CacheDataSource remembers the uri a
  source reports and reuses it, so a ranged uri leaking into that would have made every later
  request ask for the wrong bytes.
* **Chunked reads are off.** Paging a track into bounded requests avoided the throttling on open
  ended responses, but it needs a request per chunk and these hosts refuse them. The previous log
  showed the first chunk served and the next refused with 403 at exactly the 1048576 byte boundary,
  so playback stopped about a minute into every track. One request per URL is what these URLs
  support, and a dropped one is handled by re-resolving the track and resuming.

---

## 🚀 Release v1.6.2

### 🛠 Playback Fixes

Driven by a real playback log rather than a guess. The log showed a track failing with
`ERROR_CODE_IO_BAD_HTTP_STATUS` at 37s, a recovery re-resolving it, and then the stream giving up
with `Response code: 403` at exactly 1048576 bytes, which is the chunk boundary introduced in 1.6.0.
The first bounded request was served and the follow up was refused.

* **Ranged continuations fall back to a single request.** When a host serves the first chunk and then
  refuses a later one, playback now asks for everything that is left in one request instead of
  failing, and the reconnect logic still covers the drops that a long request brings. The refusal is
  remembered for ten minutes, so the sources created for later loads and seeks do not each spend a
  request rediscovering it.
* **A refusal part way through a track is no longer fatal.** Only a request refused before any audio
  arrived goes straight back to be re-resolved, because that means the URL itself is dead. Once audio
  has flowed, a refusal is treated as this particular request being declined and worked around.
* **The first recovery attempts are always allowed.** Recovery previously required every attempt to
  reach further into the song than the last, so a stream that failed twice at the same position gave
  up immediately. The first two attempts now always run, which gives the fallback above a chance to
  engage.

---

## 🚀 Release v1.6.1

### 🛠 Cache Correctness

* **One cache entry per audio format.** A track is offered in several audio formats and the one
  used is chosen per playback from the network conditions. Checked against a live response, the four
  audio formats of a single track were 1,231,355 / 1,300,631 / 3,433,755 / 3,449,447 bytes, and all
  four produced the same cache key, because only the host path and an id went into it. Sharing one
  entry across them means bytes from one format being fed to the player as another, and a recorded
  length belonging to a different format, which ends a song early with no error shown at all. The
  format is now part of the key.
* **Cache keys survive re-resolution.** Stream URLs carry a per request token, so keying on it
  produced a fresh key every time a track was resolved and the cache was never actually reused. The
  stable source id is used instead, which also makes the intro preload land where playback looks
  for it.
* **Per track cache clearing works.** It built its key from the request rather than the stream URL,
  so it silently cleared nothing. It now drops every format entry belonging to the track.
* **Version bumped so builds can be told apart.** Several fixes shipped as 1.6.0, which made it
  impossible to check which one was installed.

---

## 🚀 Release v1.6.0

### 🛠 Playback Fixes

Songs stopping partway through and skipping to the next track had three separate causes, all now
fixed. The behaviour was reproduced against a real stream before changing anything: a single open
ended request for a whole track was throttled to about 28 KB/s and then reset by the host at 95% of
the file, while the same bytes fetched as bounded range requests arrived complete and at full speed.

* **Streams are paged in bounded chunks.** Playback no longer holds one request open for an entire
  track. Each chunk is a short lived range request, which avoids both the throttling and the reset.
* **Any dropped connection reconnects.** A reset used to surface as a fatal player error, because
  only `unexpected end of stream` was treated as recoverable. Any mid stream failure now reopens
  from the exact byte offset already played, with a retry budget that refreshes on real progress and
  a fast path out for responses that were rejected outright.
* **A short stream can no longer poison the cache.** media3 reports a response that stops early as a
  clean end of input, which CacheDataSource stores as the content length of the track, so the song
  then ended at the same point on every later play. Truncation is now raised as an error instead, the
  cache entry is dropped when a track is recovered, and the cache directory was versioned so entries
  poisoned by older builds are abandoned.
* **Recovery keeps going.** Re-resolving a track used to be allowed once, so a stream that dropped
  twice still lost the rest of the song. Up to six attempts are allowed now, each required to reach
  further into the song than the last, and the queue advances only when a track truly cannot play.
* **Track length is sanity checked.** When the player reports a length well short of what the search
  result said, the catalogue duration wins, so a partial stream is not mistaken for a short song.

### 🔍 Playback Diagnostics

* **Playback log in Settings.** A song cutting out looks identical to the listener whether the stream
  was truncated, a request was rejected, the reported length was wrong, or the queue was advanced on
  purpose. Playback now records those events, with positions, durations, byte offsets and reconnects,
  and Settings can show and copy the log. Kept in memory only, so nothing is written to storage from
  the playback threads.

### 🎨 Identity

* **New launcher icon:** an amber plate with a dark five bar waveform, replacing the previous dark
  plate and music note. Adaptive, round, themed monochrome and Play Store variants are all
  generated from the same mark by `tools/generate_launcher_icons.py`.

---

## 🚀 Release v1.4.5

### 💾 Lossless & Stream Controls

* **Universal Downloads:** Lossless downloads now work directly from Amazon, Qobuz, Deezer, and
  SoundCloud based on provider priority.
* **Provider Toggles:** Enable or disable individual audio providers using checkboxes in Settings (
  YouTube Music remains fallback).
* **Cache Resets:** Per-song "Invalidate cache" player menu option and global cache reset for stream
  overrides.

### 🛠 Playback Polish & Fixes

* **Crossfade Sync:** Fixed UI flickering and premature metadata/artwork updates during crossfades.
* **Accurate Resolution Logs:** Trace logs report whether playback originates from memory, disk
  cache, or offline downloads.
* **Gesture & Engine Fixes:** Resolved swipe-to-skip artwork desync, YouTube cache poisoning, replay
  loops, and prefetch network restarts.

---

## 🚀 Release v1.4.4

### 🎧 Lossless Ecosystem & Deezer

* **Native Provider Integration:** Direct stream resolution for Amazon Music, Qobuz, SoundCloud, and
  Deezer.
* **Custom Provider Priority:** Rank search order for audio providers in Settings.
* **Native Deezer Login:** Secure ARL login support for direct FLAC/HQ streaming with automated
  prompts.
* **Live Bitrate Display:** Parses FLAC frames in real time to display live stream bitrates on
  screen.
* **Resilient Playback:** Instant connection recovery at the exact millisecond during network drops.

### 📱 Library, Home & Local Files

* **Local Music Import:** Import device audio files with full metadata and embedded artwork parsing.
* **Home Screen Filters & Feed:** Added category filter pills (Music, Podcasts, Audiobooks, Followed
  Artists) and a "Latest Releases" feed.
* **Fast Scrollbars:** Spotify-style draggable scrollbar thumb with floating position indicators
  across long lists.
* **Global Search & Sorting:** Inline search and multi-criteria sorting across Albums, Liked Songs,
  History, Podcasts, and Library.

### ⚙️ Player UI & System Integration

* **Audio Device Switcher:** Direct output selector (Speaker, Bluetooth, Wired) accessible from the
  player screen or Settings.
* **Mini-Player Gestures:** Swipe left or right on the mini-player to quickly skip tracks.
* **Quality-Aware Caching:** Hot-swaps playback quality immediately upon changing settings without
  stopping playback.
* **Trace Logs & Bot Detection:** Copyable provider attempt logs and a manual "Reset Session" button
  for YouTube bot blocks.

---

## 🚀 Release v1.4.3

### 🎧 Audio Engine

* **Concurrent Search:** Searches Tidal, Qobuz, Amazon, and Deezer simultaneously for instant
  lossless playback.
* **Smart Fallbacks:** Automatically switches providers if one fails or hits rate limits.
* **Custom Timeouts:** Configurable lossless timeouts for Wi-Fi, Cellular, and Downloads.
* **Stream Diagnostics:** Tap the source badge to view detailed audio routing resolution.
* **Provider Dashboard:** View real-time provider status and cooldown timers in Settings.

### 📚 Library & Offline

* **Quick Filters:** Filter library by Playlists, Albums, Artists, or Offline content.
* **Smarter Deduplication:** Improved logic for merging duplicate tracks across all sources.
* **Rename Playlists:** Edit custom local playlist names directly from the header.
* **Persistent Cover Art:** Locally caches artwork for offline display and system notifications.
* **Canvas Optimization:** Automatically disables Spotify Canvas for offline tracks.

### 🎵 UI & Experience

* **Explicit Tags:** Global visibility for explicit (🅴) badges across all lists and players.
* **Accurate Play States:** Play buttons strictly reflect the actively playing list.
* **Gesture Fixes:** Resolved duplicate swipe-to-play-next triggers.
* **Smoother Search:** Instant keyboard focus on tab tap; clean clearing via system back.
* **Visual Polish:** Modernized icons and enforced light status bar for dark UI legibility.

---

## 🚀 Release v1.4.2

### 🗂️ Local Playlists & Linking

* **On-Device Playlists:** Create and manage playlists locally to bypass API limits.
* **Backup & Restore:** Save and restore local playlists, likes, and preferences to storage.
* **Structured Library:** Offline albums and playlists retain original folder layouts.
* **Deep Linking:** Natively intercepts and opens Spotify URIs and web links.
* **Guided Setup:** Prompt for Android 12+ users to set the app as the default link handler.

### 🎨 UI & Stability

* **Edge-to-Edge:** Immersive UI with transparent system bars.
* **Lyrics Redesign:** Smoother gradients, reduced shadows, and increased text contrast.
* **Playback Stability:** Eliminated UI flickering during rapid toggles and buffering.
* **Smart Navigation:** Bottom bar remembers root tabs and prevents back-history clutter.

---

## 🚀 Release v1.4.1

### 🚗 Android Auto & Controls

* **Android Auto Support:** Projects the in-app playlist timeline to car displays.
* **In-Car Search:** Search and view results directly from Android Auto.
* **Automation:** Exposed Next/Previous controls to apps like Tasker and MacroDroid.
* **Sync & Timeouts:** Fixed display sync issues and increased browser timeouts.

### 🎵 Gestures & Updates

* **Swipe Action:** Swiping now sets items to "Play next" instead of adding to the bottom.
* **Auto-Play:** Optional setting to resume the last played track on app launch.
* **Rich Updates:** In-app prompts render zoomable images and HTML/URL formatting.

---

## 🚀 Release v1.4.0

### 🌐 Offline Lyrics Translation

* **On-Device Translation:** Translate lyrics into 50+ languages securely via Google ML Kit.
* **Inline Auto-Save:** Translations save locally and appear below original lyrics.
* **Floating Controls:** Manage translations and languages via a new lyrics screen panel.
* **Offline Availability:** Lyrics auto-save to disk when downloading a song.

### 🎵 Queue & Profiles

* **Swipe-to-Queue:** Global left-to-right swipe to add any track to the queue.
* **Batch Queueing:** "Add all to queue" buttons on Playlist, Album, and Liked headers.
* **Strict Repeat:** "Repeat All" loops the exact queue without appending radio tracks.
* **Artist Navigation:** Tap artist names for profiles or to view collaboration sheets.
* **Quick Creation:** Long-press "Like" to instantly create public or private playlists.

### ⚡ Performance

* **Parallel Resolution:** Resolves lossless and standard streams simultaneously.
* **Search Caching:** Caches YouTube video IDs for faster subsequent loads.
* **Live Sleep Timer:** Real-time countdown displayed in the sleep timer menu.

---

## 🚀 Release v1.3.8

### 🔍 Alternative Streams

* **In-App Search:** Find alternative YouTube streams directly within the player.
* **Audio Previews:** Transient player to preview streams without disrupting the main queue.
* **Rich Cards:** View video thumbnails and external links in the stream editor.
* **Streamlined UI:** Editor auto-dismisses on selection; back button stops active previews.

### 📱 UI & Notifications

* **Swipe-Up Feedback:** Progressive fading and spring animations for mini player gestures.
* **Compact Player:** Reduced mini player height and optimized seekbar touch targets.
* **Persistent Repeat:** Repeat modes (Off, All, One) persist across app restarts.
* **Sync Fixes:** Immediate system notification updates for play/pause and repeat states.
* **Scroll Fix:** Resolved accidental player dismissals from the lyrics view.

---

## 🚀 Release v1.3.7

### 🔔 Notifications & Routing

* **Notification Repeat:** Functional repeat button (Off/One/All) in system notifications.
* **Smart Routing:** Checks SpotiFLAC health before probing; falls back to YouTube instantly.
* **UI Fixes:** Stabilized mini-player buffering spinner and fixed navigation bar clipping.

---

## 🚀 Release v1.3.6

### 🎨 Seekbar & Timestamps

* **Custom Seekbar:** Replaced broken Material3 slider with a custom Canvas-drawn track.
* **Timestamp Fix:** Prevented negative timestamps when dragging before track load finishes.

---

## 🚀 Release v1.3.5

### ⚙️ Under the Hood

* **Persistent Queue:** Restores the full playback session across app restarts.
* **Modern Build:** Upgraded to SDK 37, Gradle 9.6.1, and Kotlin 2.4.0.
* **Codebase Cleanup:** Resolved Kotlin warnings and removed deprecated Media3 calls.

---

## 🚀 Release v1.3.4

### 🔋 Background & History

* **Battery Bypass:** Setting to request exemption from Android background restrictions.
* **Device Guides:** Specific instructions for MIUI, One UI, EMUI, etc., to disable restrictions.
* **History Redesign:** Tap-to-replay history tracks with a new gradient stats dashboard.
* **Visual Polish:** Progress bars, rankings, and circular thumbnails for top artists/tracks.

### ⚡ Updates & Stability

* **Configurable Source:** Point the update checker to custom GitHub repository URLs.
* **Markdown Notes:** Fully renders GitHub-flavored Markdown in update dialogs.
* **Background Safety:** Timeouts added to PoToken (12s) and signatures (10s) to prevent hangs.

---

## 🚀 Release v1.3.3

### ⚡ Performance & Gestures

* **Instant Playback:** Persistent caching skips lookups for recently played/searched tracks.
* **Seamless Transitions:** Background prefetching and pre-warmed playback pipeline.
* **Tri-State Repeat:** Full support for Repeat Off, All, and One with visual indicators.
* **Fluid Animations:** Spring/tween swipe gestures and full edge-to-edge player rendering.

### 🛡️ Reliability

* **Error Badges:** Explicit red error badges replacing silent load failures.
* **Background Stability:** Placeholder media states resolve background crash issues.
* **Battery Optimized:** UI polling slows down when paused to conserve battery life.

---

## 🚀 Release v1.3.2

### 💾 Offline & Playlists

* **Offline Access:** Home screen shortcut for unauthenticated or offline access.
* **Local Search:** Dedicated search bars inside Downloads and specific Playlists.
* **Advanced Sorting:** Sort by Date, Title, or Artist with persistent preferences per playlist.
* **Safe Deletion:** Confirmation dialogs added when clearing downloaded tracks.

### 🎨 System Integration

* **Themed Icons:** Monochrome icon support for Android 13+ material themes.
* **Fluid Navigation:** Swipe-up to expand and slide-to-dismiss player gestures.
* **Notification Exit:** Dedicated "Close" button to cleanly exit the app from notifications.
* **Concurrent Installs:** Unique application IDs allow Debug and Release parallel installations.