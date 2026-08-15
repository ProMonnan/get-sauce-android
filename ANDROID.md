# Android app fork

This fork adds a native Android app (Kotlin + Jetpack Compose) on top of the
existing `get-sauce` Go CLI. The Go code is unchanged for desktop use — it
still builds and releases exactly as upstream. The Android app is layered on
in three new places:

```
mobile/                              — gomobile-friendly Go bridge package
downloader/mobile.go                 — extra entry-points used by the bridge
android/                             — full Kotlin/Compose Android project
scripts/build-aar.sh                 — builds the .aar consumed by the app
.github/workflows/android.yml        — CI: builds AAR + debug APK on every push
```

Application ID: `app.sahal.getsauce` (display name: **Get Sauce**).

## Architecture

```
 ┌────────────────────────────────────────────────────────────┐
 │  Kotlin / Compose  (android/app/src/main/java/…)           │
 │  ─ MVVM per screen (Home, Info, Queue, History, Settings)  │
 │  ─ Room for queue + history, DataStore for settings        │
 │  ─ Foreground DownloadService owns the pipeline            │
 │  ─ ffmpeg-kit for the final mux (video + audio + captions) │
 │  ─ SAF (ACTION_OPEN_DOCUMENT_TREE) for user-chosen output  │
 └───────────────┬────────────────────────────────────────────┘
                 │  Java method calls
                 ▼
 ┌────────────────────────────────────────────────────────────┐
 │  gomobile-generated .aar  (built by scripts/build-aar.sh)  │
 │  ─ class mobile.Mobile: static entry points                │
 │  ─ interface mobile.ProgressListener: byte-progress + logs │
 └───────────────┬────────────────────────────────────────────┘
                 │
                 ▼
 ┌────────────────────────────────────────────────────────────┐
 │  Go: package mobile  (mobile/mobile.go)                    │
 │  ─ Init, SetWorkers, SetTimeoutMinutes, SetUserHeaders …   │
 │  ─ ExtractInfo(url) → JSON                                 │
 │  ─ DownloadStreamParts(payload, id, dir, listener) → JSON  │
 │                                                            │
 │  Reuses the existing get-sauce extractors + downloader.    │
 │  Skips the final ffmpeg exec — Kotlin handles it via       │
 │  ffmpeg-kit so we don't ship an ffmpeg binary.             │
 └────────────────────────────────────────────────────────────┘
```

Why cross the boundary as JSON strings rather than binding structs?
gomobile's struct support has enough sharp edges (no maps, awkward slice
handling, nil semantics that don't survive JNI cleanly) that trading it for
one `Json.decodeFromString` on the Kotlin side is a lot less code.

## Build steps

You need this once:

```bash
# Go 1.22+
go install golang.org/x/mobile/cmd/gomobile@latest
gomobile init

# Android SDK + NDK (26+) — Android Studio's SDK Manager is easiest.
export ANDROID_HOME=$HOME/Android/Sdk        # adjust for your setup
```

Then, every time you change the Go code:

```bash
./scripts/build-aar.sh
```

This produces `android/app/libs/getsauce.aar` (all four Android ABIs, ~30 MB).

To build the APK:

```bash
cd android
./gradlew :app:assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Or open the `android/` folder in Android Studio and hit Run.

## Fork instructions

1. Create an empty repository on your GitHub account, e.g. `sahal/get-sauce`.
2. In this project directory, run:
   ```bash
   git remote set-url origin git@github.com:sahal/get-sauce.git
   git push -u origin master
   ```
3. GitHub will show the `Android` workflow on the Actions tab; the first push
   will build both the AAR and a debug APK. Both are downloadable as workflow
   artifacts.
4. To ship signed release APKs, drop a `keystore.properties` file next to
   `android/build.gradle.kts` with:
   ```properties
   storeFile=my-keystore.jks
   storePassword=****
   keyAlias=****
   keyPassword=****
   ```
   Then `./gradlew :app:assembleRelease`. Neither file is committed.

## Known limitations / open work

- **Extractor coverage on mobile.** Every extractor that uses only Go's
  net/http will work. Any that shell out (none currently do, but keep an eye
  on `os/exec` grep results after upstream merges) will fail on Android.
- **HLS-only sites in cellular contexts.** Cloudflare tends to challenge
  mobile IP ranges more aggressively; use Settings → User headers to paste
  in a real browser session's `Cookie` + `User-Agent` when a site trips.
- **Concurrent downloads.** The Go bridge uses process-global config
  (upstream's `config` package), so the DownloadService intentionally
  serializes jobs. Parallelizing would need a refactor to plumb config
  into a request context.
- **Image sets.** No single "output file" exists for those — the Kotlin
  side records the staging directory in History. A future improvement is a
  CBZ helper that reuses `merger.archiveMerger` on the Go side.
- **The launcher icon** is a placeholder vector (`ic_launcher_foreground.xml`).
  Swap it before publishing.

## What's untouched from upstream

- `main.go`, all CLI flags, all existing extractors and mergers.
- `.goreleaser.yml` — desktop release pipeline still produces the same
  binaries.
- `.github/workflows/go.yml` and `release.yml` — desktop CI is unchanged.

The upstream `go 1.26` directive in `go.mod` was lowered to `go 1.24` because
gomobile's own toolchain hasn't been updated for 1.26 yet. Bump it back once
gomobile catches up; nothing in the codebase needs 1.26-only features.
