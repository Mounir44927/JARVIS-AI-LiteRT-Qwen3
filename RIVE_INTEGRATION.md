# JARVIS Rive integration

The Android runtime dependency is pinned to `app.rive:rive-android:11.12.1`.
`JarvisRiveOrb.kt` uses the official Rive Android `RiveAnimationView` through Compose `AndroidView`.

For the current source archive, a dedicated JARVIS `.riv` binary was not provided, so the integration uses the official Rive CDN sample `vapor_loader.riv` as a temporary live asset and keeps the existing vector orb as a deterministic visual fallback.

Replace `RIVE_ORB_URL` in `JarvisRiveOrb.kt` with the final `jarvis_orb.riv` URL or switch the initializer to `setRiveResource(R.raw.jarvis_orb)` once the final binary is supplied. Do not invent artboard/state-machine names in code; match the actual Rive file.
