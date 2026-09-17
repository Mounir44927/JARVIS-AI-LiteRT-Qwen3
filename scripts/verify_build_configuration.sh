#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"

grep -q 'sherpa-onnx-1.10.13.aar' "$BUILD_FILE"
grep -q 'jniLibs\.pickFirsts.*libonnxruntime\.so' "$BUILD_FILE"
! grep -R --exclude-dir=.git -n 'sherpa-onnx-static-link-onnxruntime-1\.13\.8\|sherpa-onnx:v1\.13\.8' "$ROOT_DIR/app" "$ROOT_DIR/build.gradle.kts" >/dev/null 2>&1 || {
  echo "Old Sherpa-ONNX 1.13.8 artifacts are still referenced." >&2
  exit 1
}
! grep -R --exclude-dir=.git -n 'onnxruntime-android:1\.18\.0' "$ROOT_DIR/app/build.gradle.kts" >/dev/null 2>&1 || {
  echo "Do not pin a second direct ONNX Runtime dependency; OpenWakeWord supplies 1.18.0." >&2
  exit 1
}
echo "Build configuration checks passed."
