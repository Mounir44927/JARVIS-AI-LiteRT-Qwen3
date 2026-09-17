#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GEN="$ROOT/tools/rive/generate_jarvis_orb.ts"

test -s "$GEN" || { echo "Missing Rive generator: $GEN" >&2; exit 1; }
grep -q 'JARVIS_ORB' "$GEN"
grep -q 'Idle' "$GEN"
grep -q 'Listening' "$GEN"
grep -q 'Thinking' "$GEN"
grep -q 'Speaking' "$GEN"
grep -q 'Error' "$GEN"
grep -q '@stevysmith/rive-generator": "0.1.1"' "$ROOT/tools/rive/package.json"
grep -q 'jarvis_orb' "$ROOT/app/src/main/java/com/jarvis/ai/ui/components/JarvisRiveOrb.kt"

echo "JARVIS Rive source validation: PASS"
