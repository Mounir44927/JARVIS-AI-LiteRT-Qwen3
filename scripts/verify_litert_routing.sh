#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APP="$ROOT/app"

grep -q 'com.google.ai.edge.litertlm:litertlm-android:0.16.1' "$APP/build.gradle.kts"
grep -q 'class LiteRtLmGateway' "$APP/src/main/java/com/jarvis/ai/brain/LiteRtLmGateway.kt"
grep -q 'class LocalFirstRoutingGateway' "$APP/src/main/java/com/jarvis/ai/brain/LocalFirstRoutingGateway.kt"
grep -q 'CactusRoutingPolicy.MIN_CONFIDENCE = 0.82f' "$ROOT/BUILD_NOTES.md"
grep -q 'backend = Backend.GPU()' "$APP/src/main/java/com/jarvis/ai/brain/LiteRtLmGateway.kt"
grep -q 'backend = Backend.CPU()' "$APP/src/main/java/com/jarvis/ai/brain/LiteRtLmGateway.kt"
if grep -RInE 'com\\.cactus|CactusGateway|CactusLM|CactusContextInitializer|qwen3-0\\.6' "$APP/src" "$APP/build.gradle.kts" >/dev/null 2>&1; then
  echo 'Stale Cactus runtime reference found.' >&2
  exit 1
fi

echo 'LiteRT-LM local-first routing configuration looks consistent.'
