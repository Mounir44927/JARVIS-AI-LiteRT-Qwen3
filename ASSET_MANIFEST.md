# JARVIS — Offline Voice Asset Manifest

This manifest records the exact runtime assets expected by the Android build.
The project intentionally refuses to package fake, partial, or HTML error responses as model files.

## OpenWakeWord v0.5.1

Source family: https://github.com/dscripka/openWakeWord/releases/tag/v0.5.1

Files:

- `app/src/main/assets/melspectrogram.onnx`
  - minimum size: 1,000,000 bytes
  - SHA-256: `ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f`
- `app/src/main/assets/embedding_model.onnx`
  - minimum size: 1,200,000 bytes
  - SHA-256: `70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f`
- `app/src/main/assets/hey_jarvis_v0.1.onnx`
  - minimum size: 100,000 bytes
  - the upstream project does not publish a stable checksum manifest for this release asset, so the build uses a size floor plus successful download validation

The shared feature-model hashes above match an openWakeWord-compatible public mirror of those files.

## JARVIS English TTS

Model source: https://huggingface.co/jgkawell/jarvis

- `app/src/main/assets/jarvis/jarvis-high.onnx`
  - minimum size: 100,000,000 bytes
  - SHA-256: `9791877d9c099fabbf30be2825e011451c39b3431e21e81e866f5b6507e72993`
- `app/src/main/assets/jarvis/model-info.json`
- `app/src/main/assets/jarvis/tokens.txt`
- `app/src/main/assets/jarvis/espeak-ng-data/`

The provisioning script also verifies that the model metadata is English and that the token map is non-empty and structurally valid.

## Native/runtime dependencies

- `app/libs/sherpa-onnx-1.10.13.aar`
  - ZIP/AAR integrity, `classes.jar`, `AndroidManifest.xml`, and minimum size are checked.
- `app/src/main/cpp/third_party/whisper.cpp/`
  - provisioned from pinned whisper.cpp v1.9.4 with the checksum already enforced by `scripts/setup_whisper.sh`.

## Rive

- `app/src/main/res/raw/jarvis_orb.riv`
  - required local binary
  - checked by the Gradle `verifyRiveAsset` task before `preBuild`

## One-command build

```bash
./scripts/prepare_and_build_debug.sh
```

That command provisions the binaries, verifies their integrity, runs unit tests, runs lint, and finally runs `assembleDebug`.
