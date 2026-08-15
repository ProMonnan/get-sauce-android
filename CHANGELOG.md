# Changelog

All notable changes to the Android app are recorded here. Format is
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) — one section per
released version, most recent at the top.

Types of change: **Added**, **Changed**, **Fixed**, **Removed**.

## [Unreleased]

<!-- Add lines here as you work. Move them into a versioned section when you tag. -->

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
