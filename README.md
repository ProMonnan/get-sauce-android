<div align="center">

<img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" height="128" alt="MoeGrab icon" />

# MoeGrab

**An Android app for downloading hentai videos and images from 35+ supported sites.**

A mobile port of the [`get-sauce`](https://github.com/gan-of-culture/get-sauce) CLI.

[![Latest release](https://img.shields.io/github/v/release/ProMonnan/get-sauce-android?label=latest&color=B4262A)](https://github.com/ProMonnan/get-sauce-android/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/ProMonnan/get-sauce-android/total?color=B4262A)](https://github.com/ProMonnan/get-sauce-android/releases)
[![License](https://img.shields.io/github/license/ProMonnan/get-sauce-android?color=B4262A)](LICENSE)

</div>

---

## Install

1. Grab the latest APK from the [Releases page](https://github.com/ProMonnan/get-sauce-android/releases/latest):
   - **`moegrab-vX.Y.Z-arm64-v8a.apk`** — recommended for any phone from ~2017 onwards (~15 MB)
   - **`moegrab-vX.Y.Z-universal.apk`** — works on any phone including older ARMv7 and x86 (~50 MB)
2. Transfer the file to your phone (email, Drive, USB — whatever).
3. Tap the APK. Android will block it once — tap **Settings**, allow installs from the source you used, back → **Install**.
4. Open **MoeGrab** → **Settings** → **Pick folder** to choose where downloads go.
5. **Home** → paste a URL → **Fetch info** → **Download**.

## Features

- One-tap download from 35+ supported sites (paste URL → pick quality → done)
- Foreground download service with progress notifications
- Downloads queue and history, both persisted across restarts
- Custom output folder via Android's Storage Access Framework
- Per-site cookie / header settings for Cloudflare-protected sites
- HTTP proxy support
- Per-ABI + universal APK builds

## Supported sites

35 sites, same list as [upstream `get-sauce`](https://github.com/gan-of-culture/get-sauce#supported-sites). Full list is visible inside the app under **Home → Supported sites**.

## Cloudflare / DDoS-Guard

Some sites (hstream.moe, hentaimama.io, etc.) sit behind Cloudflare and challenge mobile IPs aggressively. If a download stalls at 0 B or extraction fails, paste real browser cookies into **Settings → User headers**:

1. Open the site on a desktop browser, F12 → Network → any request → **Request Headers**.
2. Copy the values of `cookie:` and `user-agent:`.
3. In MoeGrab, paste them into User headers, one per line: Cookie: cf_clearance=abc123...; other=xyz...
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ...

4. Retry.

The `cf_clearance` cookie usually expires after ~2 hours; re-grab when downloads start failing again. Automating this via WebView is on the roadmap.

## Roadmap

- [ ] **v0.2.0** — Auto Cloudflare bypass (WebView cookie warmup)
- [ ] **v0.3.0** — Restore ffmpeg-kit muxing for multi-track videos
- [ ] **v0.4.0** — In-app updater + clipboard auto-detect + notification actions
- [ ] **v0.5.0** — Parallel downloads (currently serialized)
- [ ] **v0.6.0** — UI polish pass (animations, shimmer loading, better empty states)
- [ ] **v1.0.0** — Stable, share-with-friends ready

Requests welcome via [Issues](https://github.com/ProMonnan/get-sauce-android/issues).

## Architecture

┌────────────────────────────────────────────────────────┐
│ Kotlin / Compose UI (MVVM per screen) │
│ Room queue + history · DataStore settings │
│ Foreground DownloadService · SAF output │
└──────────────────────┬─────────────────────────────────┘
│ Java method calls
▼
┌────────────────────────────────────────────────────────┐
│ gomobile-generated .aar │
│ class mobile.Mobile · interface mobile.ProgressListener│
└──────────────────────┬─────────────────────────────────┘
│
▼
┌────────────────────────────────────────────────────────┐
│ Go: package mobile │
│ Reuses upstream get-sauce extractors + downloader │
└────────────────────────────────────────────────────────┘

## Building from source

Need Go 1.25+, Android SDK, NDK 26+, and gomobile installed.

```bash
# once
go install golang.org/x/mobile/cmd/gomobile@latest
gomobile init

# every time
bash scripts/build-aar.sh              # produces android/app/libs/moegrab.aar
cd android && bash gradlew assembleDebug
# APK: android/app/build/outputs/apk/debug/app-universal-debug.apk
```

Or open the `android/` folder in Android Studio and hit Run — Gradle will invoke the AAR build automatically on first sync.

## Releasing

1. Move `## [Unreleased]` bullets in [CHANGELOG.md](CHANGELOG.md) into a new `## [x.y.z] - YYYY-MM-DD` section.
2. Bump `versionCode` (int) and `versionName` (string) in `android/app/build.gradle.kts`.
3. Commit + push to `main`.
4. `git tag vX.Y.Z && git push origin vX.Y.Z`.
5. [GitHub Actions](.github/workflows/android-release.yml) builds and publishes the release ~10 min later.

## Credits

- **[gan-of-culture/get-sauce](https://github.com/gan-of-culture/get-sauce)** — the Go CLI this app wraps. All extractor magic lives there.
- **[golang.org/x/mobile](https://pkg.go.dev/golang.org/x/mobile)** — the gomobile toolchain that bridges Go to Android.

## License

MIT, same as upstream — see [LICENSE](LICENSE).
