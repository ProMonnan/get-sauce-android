#!/usr/bin/env bash
# Build the Go bridge as an Android .aar and drop it into android/app/libs/getsauce.aar.
#
# Prereqs (once):
#   * Go 1.22+                    (`go version`)
#   * Android SDK + NDK 26+       — env ANDROID_HOME must be set
#   * gomobile installed          — `go install golang.org/x/mobile/cmd/gomobile@latest`
#                                    `gomobile init`  (only needed the first time)
#
# Usage:
#   ./scripts/build-aar.sh
#
# The generated .aar contains .so files for all four Android ABIs. If you want
# to trim size, pass ANDROID_TARGET_ARCHS, e.g. `ANDROID_TARGET_ARCHS=arm64 ./scripts/build-aar.sh`.

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
out_dir="$repo_root/android/app/libs"
out_aar="$out_dir/getsauce.aar"

# If ANDROID_TARGET_ARCHS is unset we build for all four Android ABIs.
# Otherwise pass a comma-separated arch list (e.g. "arm64,amd64").
archs="${ANDROID_TARGET_ARCHS:-}"
if [[ -z "$archs" ]]; then
  target="android"
else
  target="android/${archs//,/,android/}"
fi

# Some environments set GOTOOLCHAIN=auto and try to download a newer Go — we
# want the one the user actually installed.
export GOTOOLCHAIN="${GOTOOLCHAIN:-local}"

if ! command -v gomobile >/dev/null 2>&1; then
  echo "gomobile not found on PATH. Install with:"
  echo "  go install golang.org/x/mobile/cmd/gomobile@latest"
  echo "  gomobile init"
  exit 1
fi

if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "ANDROID_HOME (or ANDROID_SDK_ROOT) is not set. Point it at your Android SDK."
  exit 1
fi

mkdir -p "$out_dir"

echo "→ Running go mod tidy"
(cd "$repo_root" && go mod tidy)

echo "→ gomobile bind → $out_aar (target=$target)"
# Note: no -javapkg — we want Java classes under the default package `mobile`
# (matching Go package name), so Kotlin imports `mobile.Mobile` and
# `mobile.ProgressListener`. Changing that requires updating the Kotlin
# bridge imports too.
(cd "$repo_root" && gomobile bind \
    -target "$target" \
    -androidapi 26 \
    -o "$out_aar" \
    -trimpath \
    -ldflags "-s -w -X github.com/gan-of-culture/get-sauce/mobile.version=$(git describe --tags --always --dirty 2>/dev/null || echo dev)" \
    ./mobile)

ls -lh "$out_aar"
echo "✔ Done. Open the Android project (android/) in Android Studio and build."
