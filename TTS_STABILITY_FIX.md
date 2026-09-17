# TTS stability fix

## Root cause addressed

The application previously combined:
- Sherpa-ONNX Android (`v1.10.13`), which uses ONNX Runtime 1.18.0.
- OpenWakeWord Android (`0.1.5`), which brings ONNX Runtime Android 1.18.0.

The application also used a Gradle `jniLibs.pickFirsts` rule for `libonnxruntime.so`. That does not resolve the native ABI/version mismatch; it merely selects one copy when packaging.

## Fix

JARVIS TTS now uses the official Sherpa-ONNX 1.10.13 Android AAR, aligned with OpenWakeWord's ONNX Runtime 1.18.0 dependency:
`sherpa-onnx-1.10.13.aar`

The CI script downloads that AAR before Gradle configuration and the app consumes it as a local file dependency. The shared `libonnxruntime.so` pick-first rule was removed.

OpenWakeWord keeps its own ONNX Runtime dependency.

## Startup/audio hardening

JARVIS TTS is initialized lazily on the first actual speech request rather than during ViewModel construction. If the neural TTS model cannot initialize or play, Android's English (UK) system TTS is used as a fallback so a TTS initialization failure cannot leave the assistant permanently silent.

The primary voice remains the local JARVIS English voice whenever it initializes successfully.
