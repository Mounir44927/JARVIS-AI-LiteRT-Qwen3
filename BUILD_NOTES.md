# ملاحظات التحقق

تم تدقيق شجرة المشروع والملفات المطلوبة، وفحص صياغة ملفات shell/XML، ومراجعة مسارات Kotlin المتأثرة بالتعديل.

تعذر تنفيذ `./gradlew testDebugUnitTest lintDebug assembleDebug` داخل بيئة الفحص لأن الوصول الخارجي إلى `services.gradle.org` غير متاح، كما أن اعتماد LiteRT-LM وAAR الصوتي غير موجودين في ذاكرة Gradle المحلية. لذلك لا أعتبر هذا الفحص بديلاً عن build حقيقي على جهاز التطوير أو GitHub Actions.

## Wake-word update
The always-on voice service uses openWakeWord on-device. The bundled pretrained model is "Hey Jarvis".

## Previous Cactus integration
The previous revision used the archived Cactus Kotlin integration with `qwen3-0.6` as the local routing model. That integration has now been removed from the app runtime.

## LiteRT-LM + local-first routing (2026-09-17)
- Reversed the gateway order: eligible requests are screened by the local model before Koog/Gemini.
- `search=true` and `CactusRoutingPolicy.mustUseCloud(...)` bypass local inference.
- Local acceptance remains fixed at `CactusRoutingPolicy.MIN_CONFIDENCE = 0.82f`.
- Replaced Cactus with `com.google.ai.edge.litertlm:litertlm-android:0.16.1`.
- Added `LiteRtLmGateway` using the official Kotlin `Engine`/`Conversation` API.
- Added lazy, SHA-256-verified download of `Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm` (~932 MB).
- GPU is opportunistic; CPU is a mandatory fallback. NPU is not assumed or initialized.
- If a live GPU inference fails, the gateway tears down the GPU engine, recreates it on CPU, and retries once.
- Kept `ConfiguredAiGateway` cloud-only so Koog's fallback and web-search tools cannot recurse into the local-first router.
- Added routing, parser, and model-artifact unit tests.


## STT phase 1 (2026-09-17)

- Added pinned whisper.cpp v1.9.4 setup + checksum verification.
- Added `WhisperNative` Kotlin/JNI facade and CPU-first CMake target.
- Added resumable SHA-256-verified `WhisperModelStore` (default small-q5_1).
- Added `ModelRuntimeCoordinator` with ActivityManager `availMem` arbitration.
- No UI/STT controller integration in this phase.
- Real native source is provisioned in CI/build via `scripts/setup_whisper.sh`.
- Benchmark metrics now reserve fields for LiteRT reload time and end-to-end local response time.
