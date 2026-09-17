# JARVIS STT — Phase 1

This phase adds the offline Arabic STT foundation without changing the UI or `SpeechRecognizerController` yet.

## Native layer

- whisper.cpp pinned to `v1.9.4` (release commit `927cfce`)
- Kotlin/JNI facade: `WhisperNative`
- CPU-first native inference
- abort callback wired for safe cancellation once the pinned source is provisioned
- `arm64-v8a` + `armeabi-v7a`

## Model

Default model: `ggml-small-q5_1.bin`

- multilingual (Arabic supported)
- exact upstream SHA-256: `ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb`
- exact upstream LFS size: `190085487` bytes

The model is downloaded to private app storage with resumable HTTP Range support and atomic finalization.

## Runtime memory policy

`ModelRuntimeCoordinator` reads `ActivityManager.MemoryInfo` immediately before arbitration.

- >= 6 GiB total RAM + >= 1.5 GiB available + `lowMemory == false`: keep LiteRT-LM and Whisper resident together; the shared `Mutex` only serializes inference.
- otherwise: Whisper may release LiteRT-LM before native inference and reload it afterward.
- reload and local-response timings are recorded for the later real-device benchmark.

Phase 1 intentionally does not change `JarvisViewModel`, `SpeechRecognizerController`, Wake Word lifecycle, or the conversation UI.
