#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT/app/src/main/assets"
mkdir -p "$ASSETS"

BASE="${OPENWAKEWORD_BASE_URL:-https://github.com/dscripka/openWakeWord/releases/download/v0.5.1}"
MELSPECTROGRAM_URL="${OPENWAKEWORD_MELSPECTROGRAM_URL:-$BASE/melspectrogram.onnx}"
EMBEDDING_URL="${OPENWAKEWORD_EMBEDDING_URL:-$BASE/embedding_model.onnx}"
HEY_JARVIS_URL="${OPENWAKEWORD_HEY_JARVIS_URL:-$BASE/hey_jarvis_v0.1.onnx}"

command -v curl >/dev/null || { echo "curl is required" >&2; exit 1; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 1; }

# Download atomically so a broken/partial response can never replace a good asset.
download() {
  local url="$1" out="$2"
  if [[ -s "$out" ]]; then
    echo "Already present: $out"
    return
  fi
  local tmp="${out}.download"
  rm -f "$tmp"
  echo "Downloading $(basename "$out")"
  curl -fL --retry 5 --retry-delay 2 --connect-timeout 20 "$url" -o "$tmp"
  [[ -s "$tmp" ]] || { echo "Downloaded file is empty: $url" >&2; exit 1; }
  mv "$tmp" "$out"
}

download "$MELSPECTROGRAM_URL" "$ASSETS/melspectrogram.onnx"
download "$EMBEDDING_URL" "$ASSETS/embedding_model.onnx"
download "$HEY_JARVIS_URL" "$ASSETS/hey_jarvis_v0.1.onnx"

# Known checksums for the shared feature models. The classifier is intentionally
# checked by a conservative size floor because the upstream release does not
# publish a stable checksum manifest for that asset.
python3 - "$ASSETS" <<'PY'
import hashlib
import pathlib
import sys

assets = pathlib.Path(sys.argv[1])
checks = {
    "melspectrogram.onnx": (1_000_000, "ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f"),
    "embedding_model.onnx": (1_200_000, "70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f"),
}
for name, (min_size, expected) in checks.items():
    p = assets / name
    data = p.read_bytes()
    if len(data) < min_size:
        raise SystemExit(f"Invalid {name}: only {len(data)} bytes")
    actual = hashlib.sha256(data).hexdigest()
    if actual != expected:
        raise SystemExit(f"Integrity check failed for {name}: {actual} != {expected}")
    print(f"Verified {name}: {len(data)} bytes, sha256={actual}")

classifier = assets / "hey_jarvis_v0.1.onnx"
if classifier.stat().st_size < 100_000:
    raise SystemExit(f"Invalid hey_jarvis_v0.1.onnx: only {classifier.stat().st_size} bytes")
print(f"Verified hey_jarvis_v0.1.onnx: {classifier.stat().st_size} bytes")
PY

echo "Wake-word assets ready."
