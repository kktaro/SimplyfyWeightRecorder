# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app `Simplify Weight Recorder` (package `com.kktaro.simplifyweightrecorder`). Single-module Gradle project (`:app`) built with Jetpack Compose + Material 3. The app captures a body-weight value from the user and writes it to **Health Connect** as a `WeightRecord` (timestamped in `Asia/Tokyo`). It is the sole entry point — there is no list/history screen.

The UI is structured as a single screen (`WeightScreen`) backed by a Hilt-injected `WeightViewModel`, with a `WeightUiState` sealed interface that drives four distinct states: `Initializing`, `Unavailable` (Health Connect missing or needs update), `Ready` (input form), and `Submitting` (in-flight save).

## Toolchain

- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01` (managed in `gradle/libs.versions.toml`).
- `compileSdk = 36` (with `minorApiLevel = 1`), `targetSdk = 36`, `minSdk = 34`.
- Java/Kotlin source & target compatibility: Java 11.
- Compose enabled via the `kotlin-compose` plugin (no separate `composeOptions` block needed).
- DI: Hilt `2.59.2` (KSP `2.2.10-2.0.2`). Application class is `SimplifyWeightRecorderApp` (`@HiltAndroidApp`); `MainActivity` is `@AndroidEntryPoint`.
- Health Connect: `androidx.health.connect:connect-client:1.1.0`.
- Async/state: Kotlin Coroutines `1.9.0`, Lifecycle ViewModel/Runtime Compose `2.8.7`, Hilt Navigation Compose `1.2.0`.
- Test: JUnit 4, MockK `1.13.12`, Turbine `1.1.0`, `kotlinx-coroutines-test`.

## Common commands

Use the Gradle wrapper from the repo root:

```sh
./gradlew assembleDebug              # build debug APK
./gradlew installDebug               # build + install on connected device/emulator
./gradlew test                       # JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "com.kktaro.simplifyweightrecorder.ui.weight.WeightViewModelTest"
./gradlew connectedAndroidTest       # instrumented tests (needs device/emulator)
./gradlew lint                       # Android Lint
./gradlew clean
```

Dependencies must be added through the version catalog (`gradle/libs.versions.toml`) and referenced as `libs.*` in `app/build.gradle.kts` — `dependencyResolutionManagement` is set to `FAIL_ON_PROJECT_REPOS`, so ad-hoc `repositories {}` blocks in modules will break the build.

## Architecture

The code is split into three layers under `com.kktaro.simplifyweightrecorder`:

- **ui/** — Compose UI + ViewModel.
  - `ui.weight.WeightScreen` — composable that wires the ViewModel, observes `uiState`, listens for `permissionRequest` / `snackbarEvents`, and re-runs `refreshAvailability()` on `ON_RESUME`. It owns the Health Connect permission `ActivityResultLauncher` (`HealthPermission.getWritePermission(WeightRecord::class)`).
  - `ui.weight.WeightInputContent` — input form (OutlinedTextField + submit button) with inline validation messages.
  - `ui.weight.WeightViewModel` — Hilt ViewModel that exposes `uiState: StateFlow<WeightUiState>`, `permissionRequest: SharedFlow<Unit>`, `snackbarEvents: SharedFlow<SnackbarEvent>`. Filters input to digits + a single `.` before validating.
  - `ui.weight.WeightUiState` — `Initializing | Unavailable | Ready | Submitting` plus `SnackbarEvent` sealed interface.
  - `ui.theme/` — `Theme.kt`, `Color.kt`, `Type.kt`; `SimplifyWeightRecorderTheme` is the root theme.
- **domain/**
  - `domain.usecase.ValidateWeightInputUseCase` — accepts `^\d+(\.\d+)?$`, max 1 decimal, `0 < kg <= 500` (`MAX_WEIGHT_KG = 500.0`).
  - `domain.usecase.SaveWeightUseCase` — converts kg + `ClockProvider.nowInTokyo()` into Instant/ZoneOffset and delegates to `WeightRepository`.
  - `domain.usecase.CheckHealthConnectAvailabilityUseCase` — thin wrapper over `HealthConnectClientProvider.getStatus()`.
  - `domain.model.WeightInputResult` — `Valid(kg) | Empty | NotANumber | OutOfRange | TooManyDecimals`.
  - `domain.model.WeightSaveError` — `PermissionDenied | HealthConnectUnavailable | Unknown(cause)`. Subclasses `Throwable` so they can flow through `Result.failure`.
  - `domain.time.ClockProvider` / `SystemClockProvider` — provides `ZonedDateTime` in `Asia/Tokyo` (fixed timezone, not the device default).
- **data/**
  - `data.healthconnect.HealthConnectClientProvider` — maps `HealthConnectClient.getSdkStatus()` to `HealthConnectAvailability` and lazily returns a client when installed.
  - `data.healthconnect.HealthConnectAvailability` — `Unknown | Installed | NotInstalled | UpdateRequired`.
  - `data.repository.WeightRepository` (interface) / `HealthConnectWeightRepository` — writes a `WeightRecord` with `Metadata.manualEntry()`. Translates `SecurityException` → `WeightSaveError.PermissionDenied`, any other throwable → `WeightSaveError.Unknown`.
- **di/AppModule.kt** — `@Module @InstallIn(SingletonComponent::class)` binding `WeightRepository ← HealthConnectWeightRepository` and `ClockProvider ← SystemClockProvider`.

### Save flow

1. User types into the field → `onWeightChange` filters and validates.
2. User taps 登録 → `onSubmit` emits to `permissionRequest`.
3. `WeightScreen` launches the Health Connect permission contract for `WRITE_WEIGHT`.
4. `onPermissionResult(granted)` either emits `PermissionDenied` snackbar or transitions to `Submitting` and runs `SaveWeightUseCase`.
5. On success → reset to empty `Ready` + `SaveSuccess` snackbar. On failure → restore previous `Ready` state + appropriate snackbar.

### Manifest essentials

- `uses-permission android:name="android.permission.health.WRITE_WEIGHT"`.
- `<queries><package android:name="com.google.android.apps.healthdata"/></queries>` so the app can resolve the Health Connect package on Android 11+.
- `activity-alias` `ViewMainActivity` exposes `ACTION_VIEW_PERMISSION_USAGE` / `category.HEALTH_PERMISSIONS` (required by Health Connect rationale flow on Android 13 and below).

## Code layout

- `app/src/main/java/com/kktaro/simplifyweightrecorder/`
  - `MainActivity.kt`, `SimplifyWeightRecorderApp.kt`
  - `ui/weight/` — `WeightScreen.kt`, `WeightInputContent.kt`, `WeightViewModel.kt`, `WeightUiState.kt`
  - `ui/theme/` — `Theme.kt`, `Color.kt`, `Type.kt`
  - `domain/usecase/`, `domain/model/`, `domain/time/`
  - `data/healthconnect/`, `data/repository/`
  - `di/AppModule.kt`
- `app/src/main/res/values/strings.xml` — all user-facing strings (Japanese).
- `app/src/test/` — JVM unit tests (JUnit 4 + MockK + Turbine). Existing tests: `WeightViewModelTest`, `ValidateWeightInputUseCaseTest`, `SaveWeightUseCaseTest`.
- `app/src/androidTest/` — instrumented tests; Compose UI tests use `androidx.compose.ui.test.junit4`.

## Conventions / gotchas

- Time is always **Asia/Tokyo** via `ClockProvider`. Do not call `Instant.now()` directly in production code — inject `ClockProvider` so tests can fake it.
- `WeightSaveError` extends `Throwable`; surface it via `Result.failure(...)` rather than throwing across coroutine boundaries by hand.
- `HealthConnectAvailability.Unknown` keeps the UI in `Initializing` — treat it as a transient state, not a failure.
- Input filtering (digits + single `.`) lives in `WeightViewModel.filterInput`; validation rules (range, decimal count) live in `ValidateWeightInputUseCase`. Keep the two responsibilities separate.
- When adding a new flow that touches Health Connect, route status checks through `HealthConnectClientProvider` so the `Installed/NotInstalled/UpdateRequired/Unknown` mapping stays in one place.
