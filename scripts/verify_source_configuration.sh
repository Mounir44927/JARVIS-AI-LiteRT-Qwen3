#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

HOME_SCREEN="$ROOT_DIR/app/src/main/java/com/jarvis/ai/ui/screens/HomeScreen.kt"
TTS_CONTROLLER="$ROOT_DIR/app/src/main/java/com/jarvis/ai/voice/TtsController.kt"
BUILD_FILE="$ROOT_DIR/app/build.gradle.kts"
WORKFLOW="$ROOT_DIR/.github/workflows/android.yml"

grep -q 'import androidx.compose.material.icons.filled.Settings' "$HOME_SCREEN"
grep -q 'Icons.Filled.Settings' "$HOME_SCREEN"
! grep -q 'icons.automirrored.filled.Settings' "$HOME_SCREEN"
! grep -q 'Icons.AutoMirrored.Filled.Settings' "$HOME_SCREEN"

grep -q 'sherpa-onnx-1.10.13.aar' "$BUILD_FILE"
grep -q 'jniLibs\.pickFirsts.*libonnxruntime' "$BUILD_FILE"
grep -q 'setup_sherpa_tts.sh' "$WORKFLOW"
grep -q 'setup_jarvis_voice.sh' "$WORKFLOW"
grep -q 'setup_wakeword.sh' "$WORKFLOW"
grep -q 'setup_whisper.sh' "$WORKFLOW"
grep -q 'verify_voice_assets.sh' "$WORKFLOW"

grep -q 'UtteranceProgressListener' "$TTS_CONTROLLER"
! grep -q 'postDelayed(onSpeechFinished, 1200L)' "$TTS_CONTROLLER"

echo "JARVIS source configuration checks passed."
