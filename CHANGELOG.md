# Changelog

All notable changes to the Android app are recorded here. Format is
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — one section per
released version, most recent at the top.

Types of change: **Added**, **Changed**, **Fixed**, **Removed**.

## [Unreleased]

<!-- Add lines here as you work. Move them into a versioned section when you tag. -->

## [1.1.0] - 2026-08-16

**The updater arrives.** Manually checking GitHub, downloading an APK,
tapping install, dismissing Play Protect — that's a five-step ritual
you had to do for every release since v0.1.0. From v1.1 on, MoeGrab
does the first four itself.

### Added
- **In-app auto-updater.** On every cold start, the app quietly queries
  the GitHub Releases API for the latest tag. If a newer version is
  published, a bottom-sheet appears with the version number, size,
  release notes, and an "Update now" button. Tapping it streams the
  arm64 APK into the app's private cache and hands it to Android's
  system installer — you just tap "Install" on the OS prompt and the
  update completes. All wrapped around a proper state machine
  (`IDLE / CHECKING / AVAILABLE / DOWNLOADING / READY_TO_INSTALL /
  FAILED / UP_TO_DATE`) so the UI never gets stuck.
  - The updater is *silent* when there's nothing new — it never
    surfaces the sheet just to say "you're up to date" on cold start.
    A manual "Check now" button in Settings triggers the same flow but
    *does* show the up-to-date state, so you get feedback when you ask.
  - Only stable releases trigger the prompt: drafts and pre-releases
    are ignored, so a nightly tag never nags you to install it.
  - The APK asset is chosen preferring `arm64-v8a`, falling back to
    `universal`, then to any `.apk`. Matches how the release workflow
    names its outputs.
  - First run on Android 8+ bounces you into the "install from unknown
    sources" settings screen so you can grant the permission once —
    after that the installer opens directly.
- **One-tap paste from clipboard.** The URL field on Home now has a
  paste icon on the right. Tap it to fill the field from clipboard —
  no more triple-tap-hold-paste dance. (Not automatic — Android 12+
  toasts every clipboard read, and we don't want to nag on every
  launch. Manual-trigger only.)
- **Manifest wiring.** Added `REQUEST_INSTALL_PACKAGES` permission,
  a `<queries>` element so the installer intent resolves on API 30+,
  a FileProvider (`{applicationId}.updater.fileprovider`) with a
  `cache-path` scope limited to `updates/`, and an XML paths file.
  The FileProvider only exposes the single subdir the updater writes
  to — nothing else in cacheDir crosses the boundary.

### Notes
- **First update via the in-app updater** (v1.1.0 → v1.1.1 whenever that
  ships) will prompt you to grant "install from unknown sources" for
  MoeGrab. Grant it once and every future in-app update installs
  silently after the download.
- Play Protect will still pop its scan dialog for a while — it treats
  every new APK signature as unknown until it's seen enough installs.
  As MoeGrab signature ages (same key across every release now), that
  dialog gets less frequent.

## [1.0.1] - 2026-08-16

Bug-fix pass on top of v1.0.0. If you're on v1.0.0, you'll need to
uninstall it *once* before installing v1.0.1 (see Notes below) — after
that, every future update installs cleanly over the previous version
without an uninstall.

### Fixed
- **Signature mismatch on update.** Every CI run was generating a fresh
  random debug keystore, so each release was signed with a different
  key and Android refused to install the new version over the old one
  ("App not installed as package conflicts…"). Committed a static
  `android/app/debug.keystore` into the repo and wired the debug
  signing config to use it, so every debug APK from now on is signed
  with the same key. Updates just work.
- **UI: giant empty band at the top of every screen.** The status bar
  was set to the app background color while the TopAppBar defaulted to
  `surface`, producing a visible color-shift band above the title.
  Introduced a shared `MoeTopBar` composable with `containerColor =
  Transparent` and centered titles, so the top bar visually merges with
  the background. Applied to Queue, History, Info, Settings, and Sites.
- **UI: Home screen content sat directly under the system clock.** Added
  explicit `WindowInsets.statusBars` padding, tightened outer padding
  from 20→14 dp, and shrank the hero star chip a touch. The whole
  layout feels less cramped and less floaty at the same time.

### Changed
- **Default concurrent-worker count bumped from 4 → 8.** Many hentai
  CDNs throttle per-connection instead of per-IP, so more parallel
  Range-request chunks means noticeably higher throughput on
  single-file MP4s. If a site is still slow, drag the slider in
  Settings up to 12 or 16.

### Notes
- Since the app's debug-signing key changed, Android sees v1.0.1 as a
  different app publisher than v1.0.0 and refuses to overwrite it. **One
  final uninstall of v1.0.0 is required** before installing v1.0.1.
  From v1.0.1 onwards, all future versions share the same key and will
  install as normal updates.
- Play Protect will pop a "scan this app?" dialog on install — that's
  Play Protect flagging any sideloaded APK it hasn't seen before. Tap
  "Install anyway" (or turn Play Protect off in the Play Store app if
  you want silent updates). We can't suppress this without shipping on
  the Play Store proper.
- Download speed on `hentai-moon.com` / `hentaiplay.net` and similar
  sites is often server-throttled (36 KB/s in the reported case). The
  workers bump helps some, but a real speed fix for those hosts needs
  the parallel-chunk enhancements coming in v1.1.

## [1.0.0] - 2026-08-16

**First stable release.** The app is renamed, re-iconed, and now looks the
part — 1.0 is the version where the surface stops looking like a debug
scaffold and starts looking like a real product. No download-pipeline
changes; everything below is UI/UX.

### Added
- **Brand color scheme** derived from the launcher icon: rose + navy in
  dark mode (matches the cat's cream + navy palette), deep rose on soft
  pink-white in light mode. **Dynamic Material You is now off** — the
  app's identity stays consistent regardless of the user's wallpaper.
- **Rounder shape system.** All cards, buttons, and dialogs use larger
  corner radii (8/12/18/24/32 dp) to match the chibi mascot's soft look.
- **Display typography.** Headline and display styles now use extra-bold
  weights with tight tracking — the app name reads like a logo, not
  system default. (A bundled Rubik face lands in v1.1.)
- **Screen transitions.** Nav destinations slide + fade in/out instead of
  cutting; the back gesture reverses direction so hierarchy is legible.
- **Shimmer skeleton loader** on the extract-info screen — replaces the
  bare spinner while a URL is being scraped. Two fake card silhouettes
  with a diagonal sweep, so the wait feels *active* not stalled.
- **Empty states with illustrations.** The empty queue shows a
  hand-drawn sleeping-cat animation (with bobbing "Zzz"s); empty history
  shows a stylized empty box. Includes friendly copy telling the user
  what to do next.
- **Download-complete confetti.** When a queue item flips to COMPLETED
  a burst of rose/cream/gold/navy particles radiates from the center
  of the queue screen. Fires only on new completions, not on scroll-in
  of existing ones.
- **Live UI animations.** Cards on the info screen fade + slide in with
  a small stagger; queue rows animate their own size changes when
  progress bars appear/disappear; completed jobs get a tinted background
  so they stand out from active ones.
- **Improved home hero.** Chip logo + "Grab it. Save it. Watch it later."
  tagline; the fetch button and paste field are taller, rounder, and have
  leading icons so the primary flow is unmistakable.
- **Better queue status icons.** Per-status glyph (hourglass, downloading,
  merging, checkmark, error, cancelled) with role-appropriate tint —
  scannable at a glance without reading the status text.

### Changed
- `Theme.kt` refactored into `Color.kt` + `Shape.kt` + `Type.kt` +
  `Theme.kt` — every design token lives in exactly one file.

### Notes
- Version bumped from the 0.1.x series to **1.0.0**. Future minor bumps
  (1.1, 1.2, ...) will land features; patch bumps (1.0.1) will land
  bug fixes.
- Because `applicationId` did not change, this installs cleanly over
  v0.1.7 with settings and history intact — no need to uninstall first.

## [0.1.7] - 2026-08-16

### Fixed
- **Launcher icon is finally the cat.** The density-specific icon PNGs
  (mipmap-mdpi through mipmap-xxxhdpi, both square and round) plus the
  adaptive-icon foreground PNG were missing from the repo — extracts from
  earlier delta zips left them on disk but never got `git add`ed. Restored
  the full icon set. Also removed the stale
  `drawable/ic_launcher_foreground.xml` placeholder vector that Android
  was falling back to.
## [0.1.5] - 2026-08-16

### Fixed
- `FfmpegMerger.kt` was accidentally reverted to the pre-stub version in the
  v0.1.4 delta zip, breaking compilation with "Unresolved reference 'arthenica'".
  Restored to the phase-1 stub that skips the mux step (video-only output for
  multi-track streams; sidecar audio/caption files stay in staging).
- `android.yml` push CI: bumped Go from 1.22.x to 1.25.x to match the release
  workflow. Old version rejected `go get -tool` (Go 1.24+ syntax) and broke
  the build on every push to main.
## [0.1.4] - 2026-08-16

### Added
- **Custom app icon** — chibi anime cat mascot on a dark navy background,
  waving a paw. Full launcher/adaptive icon set at every density.

### Fixed
- CI: use `bash gradlew` and `bash scripts/build-aar.sh` so Linux runners
  don't hit "Permission denied" when the exec bit is stripped by round-trips
  through Windows filesystems (zip extracts).
- Compose theme + resource references updated to `Theme.MoeGrab` /
  `MoeGrabTheme` (leftover from the rename).
## [0.1.3] - 2026-08-16

### Changed
- **App renamed to MoeGrab.** Launcher label, notifications, release title,
  APK filenames, and Android package ID all use the new name
  (`app.sahal.moegrab`).
- Per-ABI APK splits: `moegrab-v0.1.3-arm64-v8a.apk` (~15 MB, most modern
  phones) and `moegrab-v0.1.3-universal.apk` (~50 MB, works on anything).
  Pick arm64 for your daily driver, universal if unsure or for older devices.

### Notes
- Because the internal package ID changed from `app.sahal.getsauce` to
  `app.sahal.moegrab`, Android treats this as a fresh app. You must
  **uninstall v0.1.2** ("Get Sauce") before installing v0.1.3 ("MoeGrab").
  Downloads and settings from v0.1.2 do not carry over.

## [0.1.2] - 2026-08-15

### Fixed
- Progress counter no longer stuck at `0 B` for large single-file downloads
  (MP4s from oppai.stream, hentaiplay.net, etc.). Byte counter now updates
  live for both HLS-fragment and single-blob download paths.
- CI release workflow: `gomobile bind` now properly registers as a tool
  dependency; tag-triggered builds succeed.

## [0.1.1] - 2026-08-15

### Fixed
- Progress counter no longer stuck at `0 B` for HLS-fragment downloads.

## [0.1.0] - 2026-08-15

### Added
- First Android build.
- URL → info → quality picker → download flow.
- Foreground download service with progress notification.
- Downloads queue, history, per-site settings (proxy, workers, user
  headers, output folder).
- Persistent output folder chosen via Storage Access Framework.
- 35 supported sites, mirrored from upstream get-sauce.
