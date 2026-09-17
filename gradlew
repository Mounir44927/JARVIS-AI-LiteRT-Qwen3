#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_PROPS="$ROOT_DIR/gradle/wrapper/gradle-wrapper.properties"
GRADLE_VERSION="$(sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-bin.zip$/\1/p' "$WRAPPER_PROPS")"
[ -n "$GRADLE_VERSION" ] || GRADLE_VERSION=9.6.0

CACHE_ROOT="${GRADLE_USER_HOME:-${XDG_CACHE_HOME:-$HOME/.cache}}/jarvis-ai"
DIST_DIR="$CACHE_ROOT/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"

if [ -n "${JARVIS_GRADLE_BIN:-}" ]; then
  [ -x "$JARVIS_GRADLE_BIN" ] || { echo "JARVIS_GRADLE_BIN is not executable: $JARVIS_GRADLE_BIN" >&2; exit 2; }
  exec "$JARVIS_GRADLE_BIN" --project-dir "$ROOT_DIR" "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$CACHE_ROOT"
  TMP="$CACHE_ROOT/gradle-$GRADLE_VERSION.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Gradle غير مثبت محليًا؛ تنزيل Gradle $GRADLE_VERSION..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 10 "$URL" -o "$TMP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP" "$URL"
  else
    echo "curl أو wget مطلوب لتنزيل Gradle." >&2
    exit 2
  fi
  rm -rf "$DIST_DIR"
  mkdir -p "$DIST_DIR"
  unzip -q "$TMP" -d "$DIST_DIR/.."
  rm -f "$TMP"
fi

exec "$GRADLE_BIN" --project-dir "$ROOT_DIR" "$@"
