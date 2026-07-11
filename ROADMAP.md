# Roadmap

This template is intentionally Android/Desktop-first today. The following
targets are planned but kept out of the default build until their dependency
matrix and starter app are stable.

## Planned Targets

- iOS application target with a native entry point and shared KMP modules.
- Web/Wasm application target with a real `webApp` module and passing browser
  build tasks.

## Acceptance Criteria

- The target has an application module, not only library targets.
- `./gradlew build` passes on a clean checkout.
- The README and CI describe the target accurately.
- Dependency versions are aligned without Compose/Kotlin compatibility warnings.
