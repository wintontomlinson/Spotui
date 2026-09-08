# Spotui: Fork Features & Differences

This document outlines the custom features, improvements, and differences introduced in this fork
compared to the main Spotui repository.

---

## 🚀 Release v1.8.8

### 🎨 Warmer Theme, Cleaner Library, Working Artist Images & Lyrics

* **Less pure black.** The surfaces were near black and read flat. The whole app now carries a warm
  amber-brown tint through the background, cards and sheets, so it feels lit while staying dark and
  premium.
* **Library de-duplicated.** Liked Songs and Downloads were showing both as top tiles and again as
  list rows; now each appears once. The extra Listening history tile is gone since Recently played
  already covers it. The Artists filter chip is removed because it only ever produced an empty screen
  on this login-free build.
* **Artist photos now load.** The artists sheet used to only fetch images through Spotify, which
  needs a login this build never has, so it always showed a grey placeholder. It now falls back to
  YouTube, so a real artist photo shows without any login.
* **Lyrics are more reliable.** Exact match is skipped when the artist is unknown (it can't match
  without one), and an extra title search on the raw title is tried before the regional fallback.
  Confirmed the lyrics servers respond correctly, so the remaining gaps were the blank-artist cases
  this fixes.

---

## 🚀 Release v1.8.7

### 🎨 Launch Splash & Naming

* **The launch splash is fixed.** It was showing the dark waveform icon on a dark grey background, so
  it was nearly invisible, a dark blob for a moment before the app opened. The splash background is
  now the amber plate colour, so the launch shows the same dark waveform on amber as the launcher
  icon you tapped. The old unused Spotify green splash logo was removed.
* **Remaining Spotui text is now SOLO.** The battery optimization tips in Settings, the update
  message, and the invalid backup message all say SOLO. Internal identifiers no user ever sees (log
  tags, the package name) are left as is so nothing that depends on them breaks.

---

## 🚀 Release v1.8.6

### 🐛 Bug Fixes

An audit for real bugs, not style. Five fixed, by severity:

* **A crash when starting a track during a cache reset.** The player start chained four non-null
  assertions on a player that a concurrent cache reset or service teardown can release, racing the
  release into an NPE. It now takes one safe reference and abandons the start cleanly if the player
  is gone.
* **Wrong heart on the Liked Songs list.** Rows kept their liked state by screen position, not by
  track, so removing or re-sorting a song could leave a row showing the wrong heart or opening the
  save sheet for the wrong track. State is now tied to the track.
* **A crash on the Deezer data path.** A read before open or after close threw a hard crash instead
  of a recoverable error; it now recovers.
* **A rare crash on the search error message**, where the error could clear itself between being
  checked and being shown.
* **A defensive fix on Home**, so tapping a track can never crash if the list changed underneath the
  tap, plus removal of a dead import and stale comment from the header redesign.

---

## 🚀 Release v1.8.5

### 🎨 Settings Recreated & Player Polished

* **Settings rows share one premium card now.** Every actionable row is a bordered surface with a
  rounded leading icon badge and a trailing chevron, instead of the old inline rows that each carried
  their own flat colour. A reusable row drives Devices and Battery, and the backup, restore and reset
  rows moved onto the same shared surface with a consistent rounded shape and hairline edge. Every
  action, toggle and file picker works exactly as before.
* **Player title and labels polished.** The now playing title is larger in the heavy title typeface,
  the artist line is brighter and heavier, and the PLAYING FROM label is a wider tracked amber
  caption. All playback, gestures, the swipe pager, slider, sheets and the amber play button are
  untouched.

---

## 🚀 Release v1.8.4

### 🎨 Premium Greeting, No Search Button on Home

* **The greeting is a proper masthead now.** A short amber accent bar sits above it, and the greeting
  itself is drawn in the heavy title typeface with a warm amber gradient fill, so the top of Home
  feels premium rather than plain white text.
* **The search button is gone from Home.** Search has its own tab in the bottom navigation, so the
  circle on the header was redundant. The header is now a clean full width masthead.

---

## 🚀 Release v1.8.3

### 🎨 Home, Search, Library & Settings Redesigned

Each of the four main screens got a premium pass, styling only, with all data, navigation and
playback wiring left as it was.

* **Home.** The search button is an amber tinted circle with a hairline edge, the Trending quick
  picks block is a bordered card with a soft top down gradient and amber play badges, and artwork
  cards gained a soft drop shadow.
* **Search.** A taller rounded search field with a hairline edge and an amber search glyph, and
  outlined popular search chips so they read as a tidy set.
* **Library.** The quick access grid dropped the old blue, teal and purple mix for one cohesive
  palette: Liked leads in amber, the rest are graded dark tiles with an amber icon, each with a
  hairline edge and a soft shadow. Filter chips get a subtle outline when unselected.
* **Settings.** Cards get a hairline edge and a little more breathing room so each control reads as
  its own raised surface.

Surfaces across all four were nudged onto the shared dark ramp so nothing looks off palette beside
the new theme.

---

## 🚀 Release v1.8.2

### 🎨 Full Premium Redesign

Redesigned from the shared design system, so the whole app moves together and cohesively, rather
than one screen at a time.

* **One deliberate dark + amber theme.** Dynamic colour is now off. On Android 12+ it was retinting
  the whole app from the user's wallpaper, which overrode the amber accent and dark look and made the
  app look different on every phone. The app now owns its palette: a single dark scheme built around
  amber, with every Material control (buttons, switches, sliders, spinners) picking up the accent
  instead of a stray purple. Surfaces use a real depth ramp, near black base, lighter cards, lighter
  sheets, and titles get tighter, more intentional letter spacing.
* **Mini player** is a lifted pill with a soft shadow, a gradient from the artwork's colour into a
  darker shade, and a hairline edge, so it floats over the content.
* **Bottom navigation** is a floating rounded bar on its own raised surface with a hairline edge and
  a soft amber pill behind the active tab.
* **Search bar and loaders** move onto the new palette: a softer rounded search field, and an amber
  loading spinner in place of the old off brand purple.

All playback, gestures and navigation wiring is untouched; these are visual changes carried by the
shared theme and components.

---

## 🚀 Release v1.8.1

### 🎚 Quality & 🎨 Player Redesign

* **Default quality is Normal again**, on both wifi and cellular. This is safe now: the automatic
  selector no longer grabs the lowest bitrate on mobile data. It takes the best stream under a
  160 kbps ceiling on cellular and the full best on wifi, so Normal means good quality that adapts to
  the connection. High and Low are still there as explicit choices, and downloads stay High.
* **Premium player redesign**, visual only, with all playback, gestures, swipe to skip, the slider
  and every sheet untouched:
  * Background blends from the artwork's dominant colour through a darkened version of itself into
    near black, rather than a hard colour to black cut.
  * Artwork sits in a rounded card with a soft drop shadow, and neighbouring tracks scale down as you
    swipe so the current one stands out.
  * The play button is an amber gradient disc with an accent glow, so the main control reads in the
    app's colour instead of flat white.

---

## 🚀 Release v1.8.0

### ⚡ Slow Network, 📦 Smaller APK, 🎧 Same High Quality

A deep pass over the whole playback path and the build.

* **Buffering tuned for audio on a weak link.** The player used stock ExoPlayer buffering, which is
  built for video: it waits on a big prebuffer to start, then keeps only a modest reserve. That is
  backwards for audio on a slow connection. Playback now starts after 2.5s buffered (5s after a
  rebuffer), so a track begins quickly, and holds up to two minutes buffered so a slow link fills
  far ahead and rides out dips without stalling.
* **HTTP timeouts on the media path.** The stream data source had no connect or read timeout, so a
  stalled socket could freeze playback indefinitely. Both are now 15s, so a dead socket fails fast
  and the resilient data source reconnects.
* **Much smaller download.** The ML Kit translate and language libraries are the biggest thing in
  the app and shipped once per CPU architecture inside one universal APK, most of the old 87 MB. The
  build now produces one APK per architecture: the arm64 build real phones use is about 38 MB. A
  universal APK is still built for sideloading onto an unknown device.
* **Audio quality unchanged and still high.** Both wifi and cellular default to the HIGH tier, which
  picks the highest bitrate stream available and prefers Opus. The deeper buffer is exactly what
  lets that high stream keep playing on a slow connection.

Downloads: `Spotui_v1.8.0_arm64-v8a.apk` for phones (smaller), `Spotui_v1.8.0.apk` universal (works
on any device).

---

## 🚀 Release v1.7.9

### ⚡ Faster First Play

Two things stood between tapping a track and hearing it.

* **No more probe on cached streams.** The disk cache validated every stored URL with a network
  request before playing it, which added a full round trip to the first play of every track after a
  restart. The cache already treats anything near expiry as a miss, so the URL is still valid, and
  the in-memory cache had already dropped this same probe. Removed here too; a URL that has genuinely
  expired is caught by the player and re-resolved by the same track retry from 1.7.7.
* **The PoToken pipeline is warmed at startup.** It used to be built on the first tapped track, so
  that track paid for the WebView cold start and BotGuard handshake, a second or two. It is now built
  in the background right after startup and reused for the session, so the first tap does not wait
  on it.

Next track prefetch was already in place, so transitions within a queue were already seamless; this
targets the very first tap.

---

## 🚀 Release v1.7.8

### 🛠 The Actual Fix: the Missing PoToken Asset

The playback log named the cause outright:

```
potoken   failed: FileNotFoundException: po_token.html
```

The PoToken WebView loads `assets/po_token.html` to run YouTube's BotGuard client, but that file was
never in this fork's assets, only `silent.mp3` was. So every PoToken attempt threw on its first line,
`poToken` was always `no`, and the tracks YouTube now gates behind a PoToken were refused, usually
part way through. That is the half play and skip.

* **`po_token.html` is added**, from the upstream Metrolist project this player was ported from.
  Verified it defines exactly the functions the WebView drives (`runBotGuard`, `createPoTokenMinter`,
  `obtainPoToken`), that the request key matches, and that it is packaged inside the built APK.

This is the piece the last two releases were missing. The 1.7.6 wait fix and the 1.7.7 same track
retry made the app ask for a PoToken and reuse a track once a real URL arrives, but with no asset
there was never a PoToken to obtain. With the file present, protected tracks can resolve with a
PoToken and play through.

---

## 🚀 Release v1.7.7

### 🛠 Don't Skip the Track You Chose

Still on the restored working baseline, additions only. When a stream failed part way through, the
player jumped straight to the next track. That is the skip you see. But you chose that track, so a
failure should first be treated as a stale URL, not a reason to move on.

* **A mid playback error now retries the same track once.** The stale stream is dropped, a fresh URL
  is resolved, and the track resumes from where it stopped. Only if the same track fails a second
  time does the queue advance, so a track that genuinely cannot play still moves on instead of
  looping. This works together with the 1.7.6 PoToken fix: a fresh URL after a PoToken becomes
  available is what lets the retried track actually play.

---

## 🚀 Release v1.7.6

### 🛠 PoToken

The playback log pinned the half plays and skips: tracks resolved with `poToken=no` and then failed
mid playback with `ERROR_CODE_IO_BAD_HTTP_STATUS`. Checked live, those exact tracks answer `Sign in
to confirm you're not a bot` from every logged out client, while ordinary tracks play fine. YouTube
now requires a PoToken for these tracks, and without one the stream is refused, often part way
through, which is exactly the half play. So the cause is PoToken generation, not stream handling.

* **The PoToken wait no longer races the generator.** The outer wait was 5 seconds, the same as the
  generator's own internal 5 second budget, so this side could abandon a token the generator was
  about to return, most likely on the first track of a session while the WebView cold starts. The
  outer wait is now 12 seconds.
* **PoToken outcomes are logged.** Success, timeout, a broken or unusable WebView, or the specific
  error are all recorded now. Earlier every failure was swallowed into a null, so the log could only
  say `poToken=no` with no reason. The reason is now visible.

None of how playback reads a stream was touched.

---

## 🚀 Release v1.7.5

### 🔍 Diagnosing the Half Plays

No playback behaviour changed. Two log lines are added so a track that stops partway can be diagnosed
from the device rather than guessed at, which last time cost a working app.

* **End of track is logged with its numbers.** When a track ends, the log records where it stopped,
  the length the player reports, the length the catalogue gave, and whether a crossfade was running. A
  stop well short of either length means the stream ran out rather than the song finishing, and the
  two lengths disagreeing points at a stream under reporting its own duration.
* **The existing recovery logs when it fires**, so a re-resolve on a short stream is visible in the
  log too.

If a song still stops halfway, open Settings, view the playback log right after, and the `ended` line
will say which of those it was.

---

## 🚀 Release v1.7.4

### 🔄 Back to a Working State

Playback is restored to commit 6afdea0, the last point songs were confirmed to play. Everything the
recent releases changed about how streams are fetched, chunked reads, query string byte ranges, per
format cache keys, fresh URL retries, the n transform condition and the reordered client list, is
reverted. The playback files now differ from that baseline by additions only, with no logic removed.

Two small things are kept on top, neither of which can stop a track that already played:

* **visitorData is fetched on demand.** It is normally fetched by a background job at startup, so a
  track tapped before that landed found it missing and skipped the PoToken attempt. Fetching it right
  before resolution only restores an attempt that was being lost.
* **The playback log stays.** Track starts, player errors, queue moves and how each stream resolved
  are recorded, so a failure can be read out of Settings rather than guessed at.

---

## 🚀 Release v1.7.3

### 🛠 Stream Resolution

A playback log narrowed this down properly. Resolution succeeded and playback still failed at 0s:

```
resolve  2zY3OoqhCzs poToken=no sigTimestamp=true mainClient=skipped
resolve  2zY3OoqhCzs ok via IOS, itag=251 audio/webm expires in 21540s, nTransform=not needed
error    at 0s, ERROR_CODE_IO_BAD_HTTP_STATUS: Source error
```

`poToken=no` is the important part, and only 40ms passed before it was reported, so nothing was even
attempted. A PoToken can only be minted against a session id, which logged out means visitorData.
That is fetched in a background job at app startup, so tapping a track before it lands leaves it
null, and the PoToken is skipped without a word.

Checking that track live confirms why it matters: every logged out client, with the app's own
versions, answers `Sign in to confirm you're not a bot` for it, while ordinary tracks resolve and
play fine from all of them. For tracks like that a PoToken is not optional.

* **visitorData is fetched on demand when it is missing**, right before resolution, so a track tapped
  early no longer silently costs a PoToken.
* **The log says why a PoToken is absent**, distinguishing no session id, not attempted, and
  generation failed, so a failing WebView can be told apart from a missing session.
* **The client chain is ordered by what actually serves audio.** Every client was tested against one
  track: the VR clients and both iOS ones returned playable responses whose streams were served, and
  MOBILE returned a playable response with no stream URLs, TVHTML5 was UNPLAYABLE, its embedded
  variant errored, ANDROID_CREATOR wanted a login, and WEB was UNPLAYABLE. The VR clients now lead,
  since they are the ones built to work without a PoToken. This matters beyond speed, because every
  client tried is another request and a burst of them is what makes the host start refusing.
* **Private tracks no longer jump to a hardcoded position** in that chain, which had long since
  stopped being the client the comment claimed.

---

## 🚀 Release v1.7.2

### 🛠 Playback Fixes

Every one of the app's stream clients was tested live, with the app's own versions and user agents,
against the same track:

```
WEB_REMIX (main, PoToken)  UNPLAYABLE       0 audio
IOS                        OK               5 audio   stream 206 OK
IPADOS                     OK               5 audio   stream 206 OK
MOBILE / ANDROID           OK               0 audio
TVHTML5_SIMPLY_EMBEDDED    ERROR            0 audio
TVHTML5                    UNPLAYABLE       0 audio
ANDROID_VR 1.43.32         OK               4 audio   stream 206 OK
ANDROID_VR 1.61.48         OK               4 audio   stream 206 OK
ANDROID_CREATOR            LOGIN_REQUIRED   0 audio
WEB                        UNPLAYABLE       0 audio
```

Four clients work, and their URLs are served correctly **exactly as issued**. The app was not using
them as issued.

* **The n parameter is no longer descrambled on URLs that do not need it.** That descrambling only
  applies to the web family of clients, but the condition also fired on the mere presence of an `n`
  parameter, and every stream URL has one. So IOS, ANDROID and ANDROID_VR URLs were being rewritten
  too. Their `n` is already usable, the URL is signed, and a rewritten value comes back 403. Since
  validation then failed for every client in turn, the chain ran to its last entry, where validation
  is deliberately skipped, and returned a URL that could not play. That is why tracks were skipped
  with nothing but a 403 to show for it.
* **A rejected descramble falls back to the URL as issued.** When the host rotates the player script
  the descrambler was built against, the descrambled value stops being accepted. Rather than burning
  the whole client chain, the URL as issued is validated and used.
* **The playback log reports the transform**, so `nTransform=applied` or `not needed` now appears on
  every resolve line.

Endpoint, origin headers and visitor data were tested as possible causes and ruled out: IOS and
ANDROID_VR both return playable responses from `www.youtube.com` and `music.youtube.com`, with and
without visitor data.

---

## 🚀 Release v1.7.1

### 🛠 Playback Fixes

The cause of songs stopping partway is now confirmed rather than guessed. A stream URL contains the
client's IP address as a parameter, and that parameter is listed in the URL's own `sparams`, meaning
it is covered by the signature:

```
ip      = 44.213.75.27
sparams = expire,ei,ip,id,itag,source,requiressl,xpc,bui,spc,vprv,svpuc,mime,rqh
```

A stream URL is therefore only valid from the network it was issued to. Mobile networks hand out a
new public address regularly and switching between mobile data and wifi changes it immediately. From
that moment every request for that stream is refused with 403, which is exactly what the playback
logs showed. No change to how the bytes are requested can work around this, which is why several
earlier attempts failed.

* **A refused stream now gets a fresh URL instead of being skipped.** The track is re-resolved and
  resumed from the point the audio stopped, up to four times, with later attempts required to reach
  further into the track than the last so an unplayable track still moves on. Verified from a live
  response that a URL is not single use and that offset, bounded and unbounded requests all succeed
  while the address matches, so a fresh URL is the thing that was missing.

---

## 🚀 Release v1.7.0

### 🔄 Playback Reset

Playback is back to the code it had before this run of fixes started. Chasing songs that stopped
halfway produced two changes that each made things worse: reading a track in bounded chunks (1.6.0),
which these hosts answer with 403 on the follow up requests, and putting byte offsets in the URL's
query string (1.6.3), which returns something a media extractor cannot parse. Both are gone, along
with the retry and circuit breaker logic layered on top of them. `ResilientPlaybackDataSource` is
byte for byte what it was before.

Exactly three deliberate changes remain on top of that baseline, and none of them can stop a track
from playing:

* **No intro preload.** It downloaded the opening megabyte of upcoming tracks using the freshly
  resolved stream URL. The playback log showed that as `opened 1048576 bytes from position 0` over and
  over, with every failure landing on the first request for the bytes after that point. These hosts
  serve one request per stream URL, so the preload spent the track's only request. Resolving the URL
  ahead of time, which is what actually hid the tap latency, still happens.
* **A fresh media cache directory.** Earlier builds wrote entries that cannot be played: a stream that
  stopped early recorded as the real length of a track, and one megabyte fragments from the preload.
  Anything written by an older build is dropped rather than read back.
* **Diagnostics only.** Track start, end of track, player errors, queue moves and stream resolution
  are recorded for the playback log in Settings. Reads and writes nothing else.

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