# Changelog

All notable changes to the Android app are recorded here. Format is
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — one section per
released version, most recent at the top.

Types of change: **Added**, **Changed**, **Fixed**, **Removed**.

## [Unreleased]

<!-- Add lines here as you work. Move them into a versioned section when you tag. -->

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
