#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

log() { printf '\n[jarvis-build] %s\n' "$1"; }
fail() { echo "[jarvis-build] ERROR: $1" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "Java is required."
command -v curl >/dev/null 2>&1 || fail "curl is required to provision missing binary assets."

# Resolve Android SDK from common locations or from the environment.
ANDROID_SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$ANDROID_SDK" ]]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
    if [[ -d "$candidate" ]]; then ANDROID_SDK="$candidate"; break; fi
  done
fi
[[ -n "$ANDROID_SDK" && -d "$ANDROID_SDK" ]] || fail "Android SDK not found. Set ANDROID_SDK_ROOT (or ANDROID_HOME) to an installed SDK."
export ANDROID_SDK_ROOT="$ANDROID_SDK"
export ANDROID_HOME="$ANDROID_SDK"
[[ -d "$ANDROID_SDK/platforms" ]] || fail "Android SDK platforms are missing under $ANDROID_SDK."
[[ -d "$ANDROID_SDK/build-tools" ]] || fail "Android SDK build-tools are missing under $ANDROID_SDK."

if [[ ! -x "$ROOT_DIR/gradlew" ]]; then
  chmod +x "$ROOT_DIR/gradlew"
fi
chmod +x "$ROOT_DIR"/scripts/*.sh

log "Provisioning Sherpa-ONNX AAR"
./scripts/setup_sherpa_tts.sh

log "Provisioning JARVIS English TTS assets"
./scripts/setup_jarvis_voice.sh

log "Provisioning OpenWakeWord assets"
./scripts/setup_wakeword.sh

log "Provisioning pinned whisper.cpp source"
./scripts/setup_whisper.sh

log "Validating all runtime assets and configuration"
./scripts/verify_build_configuration.sh
./scripts/verify_onnx_runtime_configuration.sh
./scripts/verify_source_configuration.sh
./scripts/verify_koog_configuration.sh
./scripts/verify_voice_assets.sh
./scripts/verify_jarvis_rive_source.sh

RIVE="$ROOT_DIR/app/src/main/res/raw/jarvis_orb.riv"
[[ -s "$RIVE" ]] || fail "Missing $RIVE. Copy the real JARVIS Rive binary into res/raw before this command."

log "Running unit tests"
./gradlew --no-daemon testDebugUnitTest --stacktrace

log "Running Android lint"
./gradlew --no-daemon lintDebug --stacktrace

log "Assembling debug APK"
./gradlew --no-daemon assembleDebug --stacktrace

APK="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || fail "assembleDebug finished without producing $APK"

log "APK ready: $APK"
