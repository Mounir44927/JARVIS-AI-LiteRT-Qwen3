#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"

grep -q 'sherpa-onnx-1.10.13.aar' "$BUILD_FILE"
grep -q 'xyz.rementia:openwakeword:0.1.5' "$BUILD_FILE"
grep -q 'jniLibs\.pickFirsts.add("\*\*/libonnxruntime\.so")' "$BUILD_FILE"
if grep -q 'sherpa-onnx-static-link-onnxruntime-1.13.8\|sherpa-onnx-1.13.8' "$BUILD_FILE"; then
  echo "Conflicting Sherpa-ONNX 1.13.8 reference found." >&2
  exit 1
fi
if grep -q 'exclude(' "$BUILD_FILE" && grep -q 'onnxruntime-android' "$BUILD_FILE"; then
  echo "OpenWakeWord ONNX Runtime must remain available; do not exclude it." >&2
  exit 1
fi
echo "ONNX Runtime configuration is aligned on 1.18.0."
