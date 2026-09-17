# JARVIS AI — Jarvis AI 2.0

مساعد صوتي يعمل بواجهة عربية وإدخال عربي، مع إخراج صوتي إنجليزي بصوت JARVIS، مدعومًا بـ Gemini AI وWake Word محلي "Hey JARVIS".

## المزايا المنفذة

- RTL عربي حقيقي وواجهة HUD داكنة.
- **الإدخال:** عربي/نصي وصوتي. **الإخراج الصوتي:** English JARVIS فقط (en_GB).
- إعداد أولي للاسم واللقب مع كلمة إيقاظ مدعومة فعليًا بواسطة النموذج المضمن (Hey Jarvis).
- إدخال نصي يعمل دون الشبكة.
- SpeechRecognizer بجلسة قصيرة، مع إعادة محاولة مؤقتة واحدة وfallback للنص.
- صوت JARVIS محلي إنجليزي (en_GB) عبر sherpa-onnx/Piper، مع إعدادات pitch/rate عند توفرها.
- Wake Word كواجهة مستقلة قابلة لإضافة كاشف محلي حقيقي لاحقًا، دون ادّعاء أنه Voice Match أمني.
- Room/SQLite للرسائل والحقائق وذاكرة البحث مع TTL.
- Gemini gateway مباشر، وواجهة Backend gateway، مع Google Search grounding عند طلب البحث.
- معالجة 401/403/429/timeout/no-network دون حلقات لا نهائية.
- LearningPolicy للحفظ الصريح والحقائق المكتشفة.
- GitHub Actions للاختبارات وLint وبناء APK ورفع artifact.
- `build.sh` لرفع المشروع إلى GitHub وانتظار workflow وتنزيل artifact.

## مفتاح Gemini

يمكن إدخال Gemini API Key من داخل التطبيق من شاشة `Gemini والاتصال`، وتغييره أو حذفه لاحقًا. يُخزَّن المفتاح محليًا مشفّرًا باستخدام Android Keystore.

لا تضع مفتاحًا حقيقيًا داخل Git أو في `gradle.properties`. المفتاح الذي يدخله المستخدم داخل التطبيق ليس سرًا محميًا من الاستخراج على جهازه؛ للاستخدام الإنتاجي واسع النطاق، يُفضَّل استخدام Backend لحماية الأسرار.

النموذج المباشر الافتراضي هو `gemini-2.5-flash`. ويمكن استخدام `JARVIS_BACKEND_URL` أو `-PbackendBaseUrl=...` لمسار Backend الاحتياطي.

## GitHub Actions

يستخدم الـworkflow JDK 17 وAGP 9.4 وGradle 9.6، ويجهّز نماذج الصوت وWake Word قبل الاختبارات وLint وبناء `app-debug.apk`، ثم يرفعه كـartifact.

## البناء

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

وللتسليم عبر Termux:

```bash
bash build.sh
```

## ملاحظة Wake Word

لا تعتمد النسخة الأساسية على `SpeechRecognizer` ككاشف استماع دائم. الوضع الافتراضي هو زر الاستماع، بينما خدمة foreground اختيارية ومشروطة بقيود Android والأذونات. كاشف Wake Word محلي فعلي يحتاج نموذج/مكتبة محددة المصدر والترخيص، لذلك لم يتم إدخال نموذج وهمي أو ادعاء دقة غير مقاسة.


## صوت JARVIS (Piper + ONNX)

تم استبدال مسار TTS ليستخدم `sherpa-onnx` لتشغيل نموذج Piper بصيغة ONNX محليًا على Android.

الموديل المستخدم:
`jgkawell/jarvis` — نسخة `high` الإنجليزية البريطانية.

قبل البناء، شغّل:
```bash
./scripts/setup_jarvis_voice.sh
```

ثم:
```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

`build.sh` وGitHub Actions يشغّلان خطوة تجهيز الصوت تلقائيًا.

مهم: النموذج `en_GB` إنجليزي فقط. إدخال المستخدم يمكن أن يكون عربيًا، لكن طبقة الذكاء الاصطناعي تطلب إخراجًا إنجليزيًا قبل إرساله إلى صوت JARVIS.

## Real local wake word

The app uses the `xyz.rementia:openwakeword` Android library with ONNX Runtime for continuous on-device wake-word detection. The GitHub Actions workflow downloads the official `hey_jarvis_v0.1.onnx`, `melspectrogram.onnx`, and `embedding_model.onnx` assets before building.

The bundled pretrained classifier recognizes **"Hey Jarvis"** only. The onboarding flow validates this phrase so the configured phrase cannot diverge from the actual ONNX model. Detection is suspended while Android `SpeechRecognizer` captures the command, then resumed afterward. Continuous listening is opt-in and is never enabled merely because microphone permission was granted.

Sources:
- Re-MENTIA openWakeWord Android: https://github.com/Re-MENTIA/openwakeword-android-kt
- Official openWakeWord models: https://github.com/dscripka/openWakeWord


## Build prerequisites

GitHub Actions downloads the official Sherpa-ONNX 1.10.13 Android AAR before the Android build. Sherpa-ONNX 1.10.13 uses ONNX Runtime 1.18.0, matching OpenWakeWord 0.1.5; the APK therefore packages one compatible ONNX Runtime version and uses a JNI `pickFirst` only for the duplicate copy of that same version.

For a local build, run:

```bash
./scripts/setup_sherpa_tts.sh
./scripts/setup_jarvis_voice.sh
./scripts/setup_wakeword.sh
./scripts/verify_voice_assets.sh
./gradlew assembleDebug
```

The JARVIS voice remains the primary English offline voice. Android system TTS is used only as a safety fallback if the local neural voice cannot initialize or play.


## Koog Agent

JARVIS uses JetBrains Koog `1.1.1` as its agent orchestration layer when a Gemini API key is configured. Koog runs the agent loop and can call local tools for durable memory, current device date/time, and safe arithmetic. The existing Gemini gateway remains the fallback path and handles Google Search grounding requests. The app remains `minSdk 26`.


### تكامل Koog الفعلي

طلبات Gemini العادية تمر عبر `KoogAgentGateway` الذي ينشئ `AIAgent` حقيقيًا من JetBrains Koog ويشغّله مع `Gemini2_5Flash`. الوكيل مسجل له أدوات JARVIS للذاكرة الصريحة، الوقت المحلي، والحساب الآمن. طلبات البحث التي تحتاج بيانات حديثة تبقى على مسار Gemini Search Grounding المختبر في التطبيق.


## Koog Agent + Semantic RAG

JARVIS now uses JetBrains Koog as the agent orchestration layer. The agent can choose a live `webSearch` tool, a semantic `retrieveMemory` tool, device time, memory storage, and safe arithmetic.

Semantic memory/search cache uses the Gemini Embeddings API (`gemini-embedding-2`) and cosine similarity. Existing Room rows are migrated to nullable embedding columns and are lazily backfilled when accessed. Search cache keeps exact-match lookup as a fast path, then performs semantic lookup with a similarity threshold.


### LiteRT-LM local-first routing

JARVIS now routes through a local-first hybrid path. Unless `search=true` or `CactusRoutingPolicy.mustUseCloud(userPrompt)` is true, `LiteRtLmGateway` gets the first attempt. A response is accepted locally only when the model explicitly returns `ROUTE=LOCAL` with confidence `>= 0.82`; otherwise the request falls through to `KoogAgentGateway` (Gemini + tools). The `0.82` threshold is intentionally fixed until the larger model is benchmarked on real devices.

The local model is the official LiteRT Community `Qwen3-1.7B_dynamic_wi4b32_afp32.litertlm` artifact. It is ~932 MB and is downloaded lazily on first eligible local request, stored privately inside the app, and verified with SHA-256. GPU is attempted first. If GPU initialization or inference fails, the gateway rebuilds the engine with CPU and retries. NPU is not assumed or initialized.

The model is not bundled into the APK because of its size. A network connection is therefore required for the first local-model download unless the same verified file is provisioned separately on the device.

Runtime dependency is pinned to LiteRT-LM 0.16.1. The model artifact is pinned by both file size and SHA-256 in `LiteRtLmModelStore`, so a partial or altered download is rejected before engine initialization.
