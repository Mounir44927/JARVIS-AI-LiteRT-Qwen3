# JARVIS Build Status — Rive + Offline Voice

## Changes in this step

- Added deterministic provisioning/checks for OpenWakeWord ONNX assets.
- Added checksum/size validation for the shared OpenWakeWord feature models.
- Added integrity/size checks for the Sherpa-ONNX AAR.
- Added checksum/size validation for the English JARVIS Piper/VITS model.
- Added a mandatory local Rive asset check for `app/src/main/res/raw/jarvis_orb.riv`.
- Removed the network animation fallback from the Orb so a production/offline build does not silently depend on a CDN.
- Added `scripts/prepare_and_build_debug.sh` for one-command provisioning + tests + lint + `assembleDebug`.

## Required binary assets

The build intentionally does not fabricate ONNX or Rive binaries. These are model/runtime files and must be the real artifacts.

OpenWakeWord:
- `app/src/main/assets/melspectrogram.onnx`
- `app/src/main/assets/embedding_model.onnx`
- `app/src/main/assets/hey_jarvis_v0.1.onnx`

JARVIS TTS:
- `app/src/main/assets/jarvis/jarvis-high.onnx`
- `app/src/main/assets/jarvis/model-info.json`
- `app/src/main/assets/jarvis/tokens.txt`
- `app/src/main/assets/jarvis/espeak-ng-data/`

Native/dependency:
- `app/libs/sherpa-onnx-1.10.13.aar`
- `app/src/main/cpp/third_party/whisper.cpp/`

Rive:
- `app/src/main/res/raw/jarvis_orb.riv`

## Build command

```bash
./scripts/prepare_and_build_debug.sh
```

The current editing environment has no Android SDK, no Gradle distribution cache, and outbound binary downloads are blocked, so `assembleDebug` cannot be executed inside this environment. The source/configuration has therefore been made build-ready and the build is guarded by explicit asset validation instead of producing an APK with fake or partial models.

## Verification facts

The shared OpenWakeWord ONNX feature models are pinned to the following SHA-256 values: melspectrogram `ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f`, embedding `70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f`, and the English JARVIS `jarvis-high.onnx` model `9791877d9c099fabbf30be2825e011451c39b3431e21e81e866f5b6507e72993`.

The uploaded archive used for this edit was inspected directly. It did **not** contain those binary assets, `sherpa-onnx-1.10.13.aar`, the pinned whisper.cpp source, or `app/src/main/res/raw/jarvis_orb.riv`. Therefore no fake model files were generated and no false APK build result is reported.

## Local verification result for this edit

Passed:

- Bash syntax validation for all build/setup scripts.
- Build configuration validation.
- ONNX Runtime configuration validation.
- Source configuration validation.
- Koog configuration validation.
- Rive source validation.

Not runnable in this execution environment:

- `assembleDebug`: Android SDK is not installed in the environment, and Gradle 9.6.0 is not cached.
- Binary asset provisioning: outbound network access is unavailable, so the real ONNX/AAR/Rive binaries cannot be downloaded here.

The uploaded archive itself was verified to be missing the binary assets listed above. No substitute/fake binary was inserted.
