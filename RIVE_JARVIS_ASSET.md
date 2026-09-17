# JARVIS Rive Asset

This project now carries the source needed to generate the dedicated JARVIS Orb as a real `.riv` runtime asset.

## Custom asset design

The generated asset is a concentric JARVIS intelligence orb with:

- dark blue structural field
- cyan energy rings
- deep inner chamber
- amber intelligence core
- five named animations:
  - `Idle`
  - `Listening`
  - `Thinking`
  - `Speaking`
  - `Error`

The Android runtime looks for a local resource named `jarvis_orb` and selects the matching animation by `AssistantState`. The Android runtime is now local-only; it does not use a CDN fallback. The build explicitly verifies that the binary is present.

## Generate the real `.riv`

From the project root:

```bash
./scripts/build_jarvis_rive.sh
```

The script installs the fixed version:

`@stevysmith/rive-generator@0.1.1`

and generates:

`app/src/main/res/raw/jarvis_orb.riv`

The generated binary is then consumed locally by `JarvisRiveOrb.kt`.

## Why the binary is not checked in here

The execution environment for this edit did not have the Rive generator package or Rive CLI cached, and outbound package downloads were unavailable. I did not fabricate a `.riv` binary because a malformed binary would make the Android runtime fail.

Rive documents `.riv` as the runtime export produced from Rive authoring/RML, and the generator package used by this project exports actual `.riv` bytes.

## Validation

- generator source syntax: PASS
- JARVIS animation names present: PASS
- Android local-resource lookup present: PASS
- project ZIP integrity after update: checked
