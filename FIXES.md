# Critical fixes applied

## 1. Wake-word event delivery across activity lifecycle
- Added `WakeWordEventStore` to persist a detected wake-word event before broadcasting it.
- `MainActivity` consumes pending detections on `onStart()`, so a detection while the UI is stopped is not lost.
- The existing broadcast remains for the foreground case.

## 2. Wake phrase is now consistent with the actual model
- The bundled classifier is explicitly represented by `SUPPORTED_WAKE_PHRASE = "Hey Jarvis"`.
- Onboarding rejects unsupported phrases instead of storing a phrase the ONNX classifier cannot detect.
- Existing profiles with an unsupported phrase are normalized to the actual bundled model phrase at startup.

## 3. Continuous microphone listening is opt-in
- Added persistent `always_listening_enabled` setting, defaulting to `false`.
- Granting `RECORD_AUDIO` no longer starts the foreground service automatically.
- Added an explicit toggle in Settings.
- Service restart only resumes the wake-word detector when the persisted opt-in is enabled.

## 4. Voice assets are now a build requirement
- Added `verifyVoiceAssets` Gradle task wired to `preBuild`.
- Added `scripts/verify_voice_assets.sh` and wired it into CI/local build flow.
- Missing ONNX/TTS assets now fail the build clearly instead of producing an APK that silently loses voice functionality.

## Validation
- Android XML parsing: passed.
- Shell syntax checks: passed.
- Critical reference/invariant scan: passed.
- Full Gradle test/lint/build could not run in this sandbox because Gradle is not installed and `services.gradle.org` is unreachable from the environment.
- The repository currently contains the setup scripts, but the binary ONNX/TTS assets are not embedded in this sandbox because external downloads are unavailable. CI/local preparation scripts must run before a voice-enabled build.

## Ordinary-fix pass — 2026-09-16
- Wake-word pending events are consumed immediately when the Activity is already started, preventing duplicate command sessions after lifecycle changes.
- Manual voice input no longer starts or controls the foreground wake-word service unless always-listening is explicitly enabled.
- Concurrent text/voice AI requests are guarded by a single in-flight processing job.
- JARVIS TTS playback serialization now waits for actual AudioTrack completion before releasing the speech lock and returning to Idle.
- Speech rate is loaded from SettingsStore into the TTS controller; the unused pitch setting was removed instead of exposing a non-functional control.
- Natural-language learning suggestions honor the learningEnabled setting; explicit save commands remain explicit saves.
- Android automatic backup is disabled for local profile, memory, and conversation data.
- Backend configuration is now validated as a real HTTP(S) URL before being reported as configured or used.


### LiteRT-LM local-first integration
LiteRT-LM is integrated as an actual local inference gateway rather than a placeholder.
The gateway uses Qwen3-1.7B dynamic_wi4b32_afp32 and a conservative route gate. It never performs sensitive
Android actions, and uncertain/current/complex requests are sent to the configured
Gemini gateway.
