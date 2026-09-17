#!/usr/bin/env bash
set -euo pipefail

REPO_URL="${JARVIS_GITHUB_REPO:-${1:-}}"
BRANCH="${JARVIS_GITHUB_BRANCH:-main}"
ARTIFACT_NAME="jarvis-apk"

command -v git >/dev/null || { echo "git غير مثبت." >&2; exit 1; }
command -v gh >/dev/null || { echo "gh غير مثبت. ثبّته ثم نفّذ gh auth login." >&2; exit 1; }

if [[ -z "${REPO_URL}" ]]; then
  if git remote get-url origin >/dev/null 2>&1; then
    REPO_URL="$(git remote get-url origin)"
  else
    echo "حدّد المستودع عبر JARVIS_GITHUB_REPO أو أضف origin." >&2
    exit 2
  fi
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

# A plain local build should not require GitHub CLI or a remote repository.
if [[ "${JARVIS_LOCAL_BUILD_ONLY:-0}" == "1" ]]; then
  exec ./scripts/prepare_and_build_debug.sh
fi

./scripts/setup_sherpa_tts.sh
./scripts/verify_build_configuration.sh
./scripts/setup_jarvis_voice.sh
./scripts/setup_wakeword.sh
./scripts/setup_whisper.sh
./scripts/verify_voice_assets.sh
./gradlew testDebugUnitTest lintDebug assembleDebug

git add .
if ! git diff --cached --quiet; then
  git commit -m "build: Jarvis AI $(date +%Y-%m-%d)"
fi
git push origin "$BRANCH"

RUN_ID="$(gh run list --repo "$(gh repo view --json nameWithOwner -q .nameWithOwner)" --workflow android.yml --branch "$BRANCH" --limit 1 --json databaseId -q '.[0].databaseId')"
if [[ -z "$RUN_ID" ]]; then
  echo "لم أجد تشغيل workflow بعد الدفع." >&2
  exit 3
fi

echo "انتظار GitHub Actions: $RUN_ID"
if ! gh run watch "$RUN_ID" --exit-status; then
  echo "فشل workflow." >&2
  exit 4
fi

mkdir -p "$ROOT_DIR/artifacts"
gh run download "$RUN_ID" -n "$ARTIFACT_NAME" -D "$ROOT_DIR/artifacts"
echo "تم تنزيل artifact إلى $ROOT_DIR/artifacts"
