# SOLO v9.0 — verification note (implementer)

Branch `solo-v3-redesign`. Iteration: FIRST (no `review.json` present at start). All six fixes +
version bump implemented. UI/polish only; stream resolution, network, auth, cipher/potoken/sabr,
`audio/`, `data/api/`, `com/metrolist/` untouched. `recordOutcome` signature and the TasteProfile
JSON schema unchanged (Home personalisation is read-only use of the profile). `applicationId` /
`namespace` `com.music.spotui`, internal identifiers and the keystore unchanged.

## What was run (cwd /projects/sandbox/Spotui)

Build command:
`JAVA_HOME=/root/.local/share/mise/installs/java/17 ANDROID_HOME=/opt/android-sdk ./gradlew <task> --no-daemon --stacktrace`

| Task | Result |
|------|--------|
| `:app:assembleDebug` (after Phase 1) | BUILD SUCCESSFUL (~33s) |
| `:app:assembleDebug` (after Phase 2) | BUILD SUCCESSFUL (~35s) |
| `:app:assembleRelease` (R8, final) | BUILD SUCCESSFUL (~2m 39s) |

Only warnings are pre-existing (deprecated `hiltViewModel` import, media3 UnstableApi marker); no new
warnings introduced by these edits.

## APK sizes (final build)

- `app/build/outputs/apk/debug/SOLO_v9.0.0.apk` — 98,233,749 bytes (93.7 MB)
- `app/build/outputs/apk/release/SOLO_v9.0.0.apk` — 74,710,347 bytes (71.2 MB)

Output filenames correctly reflect `SOLO_v9.0.0` (versionName 9.0.0, versionCode 2026100710).

## Em-dash grep result (fix #3)

- `grep -c U+2014 app/src/main/res/values/strings.xml` -> `0`
- Quoted string literals under `ui/` containing U+2014 (excluding `//` and `*` comment lines) -> `0`

User-facing em-dashes removed from: `OnboardingScreen.kt` (two onboarding body lines),
`util/ShareCard.kt` (share EXTRA_TEXT fallback), `data/preferences/SettingsPref.kt` (two
audio-quality `detail` labels shown in Settings). Remaining U+2014 occurrences are all inside code
comments / KDoc (e.g. `BrowseTileImages.kt` image-id comments), which the brief explicitly permits.

## Per-fix summary

1. **Home headings** — `SoloSectionHeader` now bound to ALL Home shelves in `FreeHomeScreen.kt`.
   Added headers to the previously headerless Featured carousel ("Featured"), mood chips
   ("Browse moods") and the Mix hero ("Made for you"); added bound subtitles to Jump back in,
   Trending now and Your top artists. Genre/mood/time/"Because you liked" shelves keep their
   data-bound `HomeRow.title`.
2. **Navbar visibility (top priority)** — in `MainBottomNavigation.kt` the docked bar's sole
   `.soloGlass(...)` fill (a translucent RenderEffect blur with no opaque backdrop over the
   edge-to-edge window, which washed out against `#0E1013`) is replaced with a SOLID opaque
   `GlassFillStrong` (`0xF215181C`) background on every SDK level, plus a soft top drop-shadow for
   separation and the existing 1dp `Hairline` top edge. `.navigationBarsPadding()` and the
   compression height are kept, so system-nav insets are respected and the mini player still stacks
   above the bar. Removed the now-unused `soloGlass`/`GlassFill` imports.
3. **Em-dash removal** — see grep result above.
4. **Premium About** — rewrote `AboutCard()` in `SettingsScreen.kt`: centered logo + SOLO wordmark
   + version pill + "One voice. Pure sound." tagline, then grouped `AboutInfoRow` cards for License
   (GPL-3.0), Credits (Neptune, Metrolist, SpotiFLAC, SimpMusic + open fonts), Maintained by
   (SATYAN SHARMA) and Disclaimer (not affiliated with Spotify or YouTube). Accent-tinted icon
   wells, consistent spacing, no em-dash, no source links.
5. **Personalised Home** — `fetchRow()` in `FreeHomeViewModel.kt` now pulls a wider candidate pool
   (24) and ranks it through `TasteRanker.rank(context, candidates, seed = null, recent =
   getListeningHistory(context), limit = 12)` with a raw-order fallback, mirroring `fetchTrending()`.
   Mix, "Because you liked" and Trending already routed through the ranker. Every Home shelf path is
   now personalised. See `personalisation.md`.
6. **Update dialog** — redesigned `UpdatePrompt()` in `UpdatePrompt.kt`: logo + accent header
   ("Update available" / "SOLO <version>"), a labelled "What's new" rendered via `RenderMarkdown`,
   primary accent **Update** + secondary **Later** (Cancel while downloading, Install when ready,
   Retry on error), in-app `LinearProgressIndicator`. The v8 download -> FileProvider ->
   system-installer flow (`UpdateChecker`, `startUpdate`, `installPermissionLauncher`, phase state
   machine, skip/"Don't show again") is unchanged. No browser redirect. `UpdateChecker` posts no OS
   notification (HTTP only), so no notification text change was needed.

Version bump: `app/build.gradle.kts` -> `versionCode = 2026100710`, `versionName = "9.0.0"` (single
line). `CHANGELOG.md` -> new `## 9.0.0 - SOLO` entry (hyphen, no em-dash) above the 8.0.0 entry; all
old entries kept.

## What could NOT be device-verified

No emulator/device is available in this environment, so the following were verified by successful
compilation + code review ONLY, not by observing a rendered frame on a device:

- The navbar now renders opaque/visible with its icons, accent indicator and the mini player above
  content on Home / Explore / Library, clear of the system gesture bar. The fix is structural (solid
  opaque fill + shadow + hairline, insets preserved) and compiles clean, but the actual pixels were
  not inspected on a device or in a rendered PNG (none produced by the build).
- The Home section headers, premium About layout and redesigned update dialog were verified by
  compilation + code review; their on-screen appearance was not device-verified.

A reviewer with a device/emulator should confirm the navbar is visible on each main tab and the
About / update-dialog layouts look as intended.
