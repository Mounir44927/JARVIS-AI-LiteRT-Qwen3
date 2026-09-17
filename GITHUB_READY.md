# Jarvis AI — GitHub-ready build

This version keeps the supplied offline JARVIS English Piper/Sherpa-ONNX voice only. The Arabic TTS path is not used for assistant replies.

## Voice
- Offline JARVIS voice: `app/src/main/assets/jarvis/`
- Build script: `scripts/setup_jarvis_voice.sh`
- Wake phrase model: `scripts/setup_wakeword.sh` (`Hey Jarvis`)

## Microphone
The foreground service no longer starts wake-word capture from `onCreate`. It starts only on an explicit resume/default service command. Manual SpeechRecognizer use suspends the wake-word engine through the service itself, then waits briefly before opening the microphone.

## Gemini
The app can accept, replace, and remove a Gemini API key from its Connection screen. The key is stored locally using Android Keystore. The direct Gemini model is `gemini-2.5-flash`. A backend can still be configured through `JARVIS_BACKEND_URL`/`backendBaseUrl` as the safer production path.

## Build
GitHub Actions runs unit tests, lint, then `assembleDebug`, and uploads `jarvis-apk`.


## JetBrains Koog integration

The app now routes normal Gemini-backed assistant requests through a real Koog `AIAgent` when a Gemini API key is configured. The agent has registered JARVIS tools for reading/saving explicit memory, current local date/time, and safe arithmetic. Search-required requests continue through the existing grounded Gemini gateway. Koog is pinned to `1.1.1` for Android `minSdk 26` compatibility.


## LiteRT-LM local-first intelligence layer

Runtime: `com.google.ai.edge.litertlm:litertlm-android:0.16.1`. The model is provisioned lazily rather than bundled into the APK.

LiteRT-LM is now a live local routing/answer layer, not a placeholder. JARVIS uses the
`Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm` on-device model to screen eligible low-risk requests. Explicit search,
time-sensitive, long-form, coding, research, legal/medical/financial, and other
higher-risk/complex requests bypass local generation and stay on the Gemini path.
LiteRT-LM is accepted only when its structured route is `LOCAL` and confidence is at least 0.82. A failure or uncertain route transparently hands off to Gemini.

The first LiteRT-LM use downloads `Qwen3-1.7B dynamic_wi4b32_afp32` if it is not already installed on the device.
