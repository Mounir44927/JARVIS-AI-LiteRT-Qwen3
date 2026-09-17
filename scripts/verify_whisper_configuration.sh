#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC="$ROOT_DIR/app/src/main/cpp/third_party/whisper.cpp"

grep -q 'object WhisperNative' "$ROOT_DIR/app/src/main/java/com/jarvis/ai/voice/WhisperNative.kt"
grep -q 'add_library(jarvis_whisper SHARED WhisperJni.cpp)' "$ROOT_DIR/app/src/main/cpp/CMakeLists.txt"
grep -q 'WHISPER_AVAILABLE' "$ROOT_DIR/app/src/main/cpp/WhisperJni.cpp"
grep -q 'ggml-small-q5_1.bin' "$ROOT_DIR/app/src/main/java/com/jarvis/ai/voice/WhisperModelStore.kt"
grep -q 'ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb' \
  "$ROOT_DIR/app/src/main/java/com/jarvis/ai/voice/WhisperModelStore.kt"

if [[ -f "$SRC/CMakeLists.txt" ]]; then
  echo "whisper.cpp source: PRESENT"
else
  echo "whisper.cpp source: NOT POPULATED (run scripts/setup_whisper.sh)"
fi

echo "whisper native/JNI/model-store configuration: OK"
