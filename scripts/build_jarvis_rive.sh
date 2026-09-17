#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/tools/rive"

if ! command -v node >/dev/null 2>&1; then
  echo "Node.js 22+ is required to generate the JARVIS .riv asset." >&2
  exit 1
fi

if [ ! -d node_modules/@stevysmith/rive-generator ]; then
  echo "Installing @stevysmith/rive-generator@0.1.1 ..."
  npm install --no-audit --no-fund
fi

npm run generate

echo "JARVIS Rive asset is ready at:"
echo "  $ROOT/app/src/main/res/raw/jarvis_orb.riv"
