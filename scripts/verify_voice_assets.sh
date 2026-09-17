#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS="$ROOT_DIR/app/src/main/assets"

for f in "$ASSETS/melspectrogram.onnx" "$ASSETS/embedding_model.onnx" "$ASSETS/hey_jarvis_v0.1.onnx" \
         "$ASSETS/jarvis/jarvis-high.onnx" "$ASSETS/jarvis/tokens.txt" "$ASSETS/jarvis/model-info.json"; do
  [[ -s "$f" ]] || { echo "Missing voice asset: $f" >&2; exit 1; }
done
[[ -d "$ASSETS/jarvis/espeak-ng-data" ]] || { echo "Missing English JARVIS eSpeak runtime data" >&2; exit 1; }
[[ ! -e "$ASSETS/jarvis/ar" && ! -e "$ASSETS/jarvis/en_US" ]] || { echo "Unexpected extra language voice assets found" >&2; exit 1; }

sha256() { sha256sum "$1" | awk '{print $1}'; }

check_sha() {
  local file="$1" expected="$2" actual
  actual="$(sha256 "$file")"
  [[ "$actual" == "$expected" ]] || {
    echo "Checksum mismatch for $file" >&2
    echo "  expected: $expected" >&2
    echo "  actual:   $actual" >&2
    exit 1
  }
}

check_min_size() {
  local file="$1" min="$2" size
  size="$(wc -c < "$file")"
  (( size >= min )) || { echo "Asset is suspiciously small: $file ($size bytes)" >&2; exit 1; }
}

check_min_size "$ASSETS/melspectrogram.onnx" 1000000
check_min_size "$ASSETS/embedding_model.onnx" 1200000
check_min_size "$ASSETS/hey_jarvis_v0.1.onnx" 100000
check_min_size "$ASSETS/jarvis/jarvis-high.onnx" 100000000
check_sha "$ASSETS/melspectrogram.onnx" "ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f"
check_sha "$ASSETS/embedding_model.onnx" "70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f"
check_sha "$ASSETS/jarvis/jarvis-high.onnx" "9791877d9c099fabbf30be2825e011451c39b3431e21e81e866f5b6507e72993"

python3 - "$ASSETS/jarvis/model-info.json" "$ASSETS/jarvis/tokens.txt" <<'PY'
import json
import pathlib
import sys

cfg = json.loads(pathlib.Path(sys.argv[1]).read_text(encoding='utf-8'))
tokens = pathlib.Path(sys.argv[2]).read_text(encoding='utf-8').strip().splitlines()
assert cfg.get('language', {}).get('code', 'en').lower().startswith('en'), 'JARVIS TTS model must be English'
assert tokens, 'JARVIS tokens.txt is empty'
assert all(len(line.rsplit(' ', 1)) == 2 for line in tokens), 'Invalid JARVIS token line'
print('JARVIS voice metadata/tokens: PASS')
PY

echo "Voice assets verified: wake word + English JARVIS only."
