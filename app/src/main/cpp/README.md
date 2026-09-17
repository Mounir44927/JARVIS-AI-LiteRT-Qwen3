# JARVIS whisper.cpp native layer

The project pins **whisper.cpp v1.9.4** for the first real STT implementation.

The ZIP intentionally does not vendor the 9+ MB source tree. Use:

```bash
./scripts/setup_whisper.sh
```

That script downloads the pinned source archive, verifies its SHA-256, and extracts it to:

```text
app/src/main/cpp/third_party/whisper.cpp/
```

The Kotlin/JNI surface is intentionally tiny:

- `initContext(path)`
- `transcribeArabic(handle, pcmFloat32, threads)`
- `cancel(handle)`
- `freeContext(handle)`

Until the pinned source is populated, CMake builds a small stub library so the Android project can still configure; calling STT reports that the native backend is not provisioned.


## Why the source is not copied into the ZIP

The working environment for this change could not resolve GitHub at build time, so the full third-party source tree could not be fetched and vendored safely. The project therefore carries a pinned, checksum-verified setup script instead of pretending the source is present.

The v1.9.4 source tarball checksum is pinned to:

`57e280cee375ab02425b806ad5146b99f6eb9357e3c2b31357c8a6af2e2e44ae`

After `scripts/setup_whisper.sh`, the real native target is built from that pinned source.
