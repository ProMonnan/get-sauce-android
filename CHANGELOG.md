# Changelog

All notable changes to the Android app are recorded here. Format is
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — one section per
released version, most recent at the top.

Types of change: **Added**, **Changed**, **Fixed**, **Removed**.

## [Unreleased]

<!-- Add lines here as you work. Move them into a versioned section when you tag. -->

## [0.1.2] - 2026-08-16

   ### Fixed
   - CI release workflow: `gomobile` bind now properly registers as a tool dependency, letting tag-triggered builds succeed.
   - `scripts/build-aar.sh` runs `go get -tool` before invoking gomobile, matching what modern gomobile requires.

   ### Notes
   - v0.1.1 was tagged before these CI fixes and published desktop binaries only (the goreleaser workflow ran, the Android release didn't). v0.1.2 is the first tag where the Android release workflow completes end-to-end.

## [0.1.1] - 2026-08-15

### Fixed
- Progress counter no longer stuck at `0 B` for large single-file downloads
  (MP4s from oppai.stream, hentaiplay.net, etc.). Byte counter now updates
  live for both HLS-fragment and single-blob download paths.

## [0.1.0] - 2026-08-15

### Added
- First Android build of `get-sauce`.
- URL → info → quality picker → download flow.
- Foreground download service with progress notification.
- Downloads queue, history, and per-site settings (proxy, workers, user
  headers, output folder).
- Persistent output folder chosen via Storage Access Framework.
- 35 supported sites, mirrored from upstream get-sauce.
