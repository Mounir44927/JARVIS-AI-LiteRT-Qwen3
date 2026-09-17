#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"
GATEWAY="$ROOT_DIR/app/src/main/java/com/jarvis/ai/brain/KoogAgentGateway.kt"
TOOLS="$ROOT_DIR/app/src/main/java/com/jarvis/ai/brain/KoogJarvisTools.kt"
APP="$ROOT_DIR/app/src/main/java/com/jarvis/ai/JarvisApp.kt"

grep -q 'ai.koog:koog-agents:1.1.1' "$BUILD_FILE"
grep -q 'class KoogAgentGateway' "$GATEWAY"
grep -q 'AIAgent(' "$GATEWAY"
grep -q 'simpleGoogleAIExecutor' "$GATEWAY"
grep -q 'GoogleModels.Gemini2_5Flash' "$GATEWAY"
grep -q 'ToolRegistry' "$GATEWAY"
grep -q 'class KoogJarvisTools' "$TOOLS"
grep -q 'class KoogJarvisTools.*ToolSet\|: ToolSet' "$TOOLS"
grep -q '@Tool' "$TOOLS"
grep -q 'KoogAgentGateway(' "$APP"

if grep -R --exclude-dir=.git -n 'gemini-3.6-flash' "$ROOT_DIR/app" "$ROOT_DIR/README.md" "$ROOT_DIR/GITHUB_READY.md" >/dev/null 2>&1; then
  echo "Stale gemini-3.6-flash reference found." >&2
  exit 1
fi

echo "Koog agent integration checks passed."
