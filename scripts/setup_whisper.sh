#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST_DIR="$ROOT_DIR/app/src/main/cpp/third_party/whisper.cpp"
CACHE_DIR="$ROOT_DIR/.cache"
ARCHIVE="$CACHE_DIR/whisper.cpp-v1.9.4.tar.gz"

URL="${WHISPER_CPP_ARCHIVE_URL:-https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.9.4.tar.gz}"
SHA256="57e280cee375ab02425b806ad5146b99f6eb9357e3c2b31357c8a6af2e2e44ae"

mkdir -p "$CACHE_DIR"
rm -rf "$DEST_DIR"

echo "[whisper] fetching v1.9.4..."
if [[ ! -f "$ARCHIVE" ]]; then
  curl --fail --location --retry 3 --retry-delay 2 "$URL" -o "$ARCHIVE"
fi

echo "$SHA256  $ARCHIVE" | sha256sum -c -

TMP_DIR="$CACHE_DIR/whisper.cpp-v1.9.4.unpack"
rm -rf "$TMP_DIR"
mkdir -p "$TMP_DIR"
tar -xzf "$ARCHIVE" -C "$TMP_DIR"

SOURCE_ROOT="$TMP_DIR/whisper.cpp-1.9.4"
if [[ ! -f "$SOURCE_ROOT/CMakeLists.txt" || ! -f "$SOURCE_ROOT/include/whisper.h" ]]; then
  echo "[whisper] extracted archive is missing expected files" >&2
  exit 1
fi

mkdir -p "$(dirname "$DEST_DIR")"
mv "$SOURCE_ROOT" "$DEST_DIR"
rm -rf "$TMP_DIR"

echo "[whisper] installed pinned source at $DEST_DIR"
