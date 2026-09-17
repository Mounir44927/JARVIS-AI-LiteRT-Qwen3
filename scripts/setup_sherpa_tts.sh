#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIBS="$ROOT_DIR/app/libs"
AAR="$LIBS/sherpa-onnx-1.10.35.aar"
URL="${SHERPA_AAR_URL:-https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.10.35/sherpa-onnx-1.10.35.aar}"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v unzip >/dev/null || { echo "unzip is required" >&2; exit 1; }
mkdir -p "$LIBS"

verify_aar() {
  local path="$1"
  [[ -s "$path" ]] || return 1
  [[ "$(wc -c < "$path")" -ge 1000000 ]] || return 1
  unzip -t -q "$path" >/dev/null 2>&1 || return 1
  unzip -l "$path" | grep -q ' classes.jar$' || return 1
  unzip -l "$path" | grep -q ' AndroidManifest.xml$' || return 1
}

if verify_aar "$AAR"; then
  echo "Already present and valid: $AAR"
  exit 0
fi
rm -f "$AAR"

TMP="$AAR.download"
rm -f "$TMP"
trap 'rm -f "$TMP"' EXIT

curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 "$URL" -o "$TMP"
test -s "$TMP" || { echo "Downloaded Sherpa-ONNX AAR is empty." >&2; exit 1; }

# Basic archive integrity check. An AAR is a ZIP.
if ! verify_aar "$TMP"; then
  echo "Downloaded Sherpa-ONNX AAR is invalid or incomplete." >&2
  exit 1
fi

mv "$TMP" "$AAR"
echo "Sherpa-ONNX 1.10.35 AAR ready: $AAR"
