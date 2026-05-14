# AGENTS.md

## Project

Single-module Android app (`:app`). Namespace: `com.example.myapplication`.

- **AGP 9.1.0 / Gradle 9.3.1** — bleeding-edge; many online examples use older DSL. The `compileSdk` block syntax in `app/build.gradle.kts` is AGP 9 style (not `compileSdk = N`).
- **Java 11 only** — no Kotlin plugin configured. Source files go under `app/src/main/java/...`.
- **minSdk 24 / targetSdk 36 / compileSdk 36**

## Build & Test

```sh
./gradlew assembleDebug          # build debug APK
./gradlew test                   # unit tests (app/src/test/)
./gradlew connectedAndroidTest   # instrumented tests (requires device/emulator)
./gradlew lint                   # Android lint
```

## Gotchas

- `settings.gradle.kts` sets `FAIL_ON_PROJECT_REPOS` — all repositories must be declared there, not in module `build.gradle.kts` files.
- Dependencies are managed via the Gradle version catalog at `gradle/libs.versions.toml`. Use `libs.xxx` references, not hardcoded strings.
- `local.properties` is gitignored and must contain `sdk.dir` pointing to the Android SDK (normally set by Android Studio).
