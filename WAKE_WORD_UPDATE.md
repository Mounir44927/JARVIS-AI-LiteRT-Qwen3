# Wake Word Update

This version adds a real always-on on-device wake-word pipeline.

Current trigger: **Hey Jarvis**

Pipeline:
microphone -> openWakeWord ONNX -> wake event -> Android SpeechRecognizer -> Jarvis

The wake-word detector runs locally and does not send the continuous microphone stream to a cloud API. The detector is paused while SpeechRecognizer captures the command, then resumed.

The pretrained openWakeWord model bundled by the build pipeline recognizes **Hey Jarvis** only. The app now validates and stores that same supported phrase instead of allowing a UI value that the ONNX model cannot detect. To make **JARVIS** the actual wake word, replace `hey_jarvis_v0.1.onnx` with another supported openWakeWord classifier and update `MODEL_NAME`/`SUPPORTED_WAKE_PHRASE` accordingly.

The GitHub Actions workflow downloads the three required wake-word assets before building:
- melspectrogram.onnx
- embedding_model.onnx
- hey_jarvis_v0.1.onnx
