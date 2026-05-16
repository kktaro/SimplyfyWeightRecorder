# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app `Simplify Weight Recorder` (package `com.kktaro.simplifyweightrecorder`). Single-module Gradle project (`:app`) built with Jetpack Compose + Material 3. Currently a fresh Android Studio template — `MainActivity` only renders a placeholder `Greeting` composable, so most product code is yet to be written.

## Toolchain

- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01` (managed in `gradle/libs.versions.toml`).
- `compileSdk = 36` (with `minorApiLevel = 1`), `targetSdk = 36`, `minSdk = 34`.
- Java/Kotlin source & target compatibility: Java 11.
- Compose enabled via the `kotlin-compose` plugin (no separate `composeOptions` block needed).

## Common commands

Use the Gradle wrapper from the repo root:

```sh
./gradlew assembleDebug              # build debug APK
./gradlew installDebug               # build + install on connected device/emulator
./gradlew test                       # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.kktaro.simplifyweightrecorder.ExampleUnitTest.addition_isCorrect"
./gradlew connectedAndroidTest       # instrumented tests (needs device/emulator)
./gradlew lint                       # Android Lint
./gradlew clean
```

Dependencies must be added through the version catalog (`gradle/libs.versions.toml`) and referenced as `libs.*` in `app/build.gradle.kts` — `dependencyResolutionManagement` is set to `FAIL_ON_PROJECT_REPOS`, so ad-hoc `repositories {}` blocks in modules will break the build.

## Code layout

- `app/src/main/java/com/kktaro/simplifyweightrecorder/MainActivity.kt` — single Compose entry point using `enableEdgeToEdge()` + `Scaffold`.
- `app/src/main/java/com/kktaro/simplifyweightrecorder/ui/theme/` — `Theme.kt`, `Color.kt`, `Type.kt`. `SimplifyWeightRecorderTheme` is the root theme wrapper for all Compose UI.
- `app/src/test/` — local JVM unit tests (JUnit 4).
- `app/src/androidTest/` — instrumented tests; Compose UI tests use `androidx.compose.ui.test.junit4`.
