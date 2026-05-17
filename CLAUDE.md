# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app `Simplify Weight Recorder` (package `com.kktaro.simplifyweightrecorder`). Single-module Gradle project (`:app`) built with Jetpack Compose + Material 3. The app captures a body-weight value from the user and writes it to **Health Connect** as a `WeightRecord` (timestamped in `Asia/Tokyo`). It is the sole entry point — there is no list/history screen.

The UI is structured as a single screen (`WeightScreen`) backed by a Hilt-injected `WeightViewModel`, with a `WeightUiState` sealed interface that drives four distinct states: `Initializing`, `Unavailable` (Health Connect missing or needs update), `Ready` (input form), and `Submitting` (in-flight save).

In addition, the app provides a **home-screen widget** (Jetpack Glance) that lays out 11 weight candidate buttons around the last saved value (±0.5 kg in 0.1 kg steps). Tapping a button writes a `WeightRecord` to Health Connect directly from the widget without opening the app.

## Toolchain

- AGP `9.2.1`, Kotlin `2.2.10`, Compose BOM `2026.02.01` (managed in `gradle/libs.versions.toml`).
- `compileSdk = 36` (with `minorApiLevel = 1`), `targetSdk = 36`, `minSdk = 34`.
- Java/Kotlin source & target compatibility: Java 11.
- Compose enabled via the `kotlin-compose` plugin (no separate `composeOptions` block needed).
- DI: Hilt `2.59.2` (KSP `2.2.10-2.0.2`). Application class is `SimplifyWeightRecorderApp` (`@HiltAndroidApp`); `MainActivity` is `@AndroidEntryPoint`.
- Health Connect: `androidx.health.connect:connect-client:1.1.0`.
- Widget: Jetpack Glance `1.1.1` (`glance-appwidget` + `glance-material3`).
- Storage: `androidx.datastore:datastore-preferences:1.1.7` (for persisting the last saved weight).
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
- **widget/** — Jetpack Glance home-screen widget.
  - `widget.WeightWidget` — `GlanceAppWidget` that renders the title, current status text, and the 11-cell candidate grid. Uses `PreferencesGlanceStateDefinition` for transient UI state.
  - `widget.WeightWidgetReceiver` — `GlanceAppWidgetReceiver` registered in the manifest.
  - `widget.WeightWidgetEntryPoint` — Hilt `@EntryPoint` (SingletonComponent) exposing `LastWeightRepository`, `ComputeWeightCandidatesUseCase`, `SaveWeightUseCase`, `CheckHealthConnectAvailabilityUseCase`. Glance components fetch dependencies via `EntryPointAccessors.fromApplication(...)`.
  - `widget.WeightSelectionActionCallback` — `ActionCallback` invoked on cell tap. Runs availability check → updates state to `Saving` → saves → on success updates last weight and shows `Success` for 3 s before reverting to `Idle`; on failure maps `WeightSaveError` to `WidgetErrorReason` and shows `Error`.
  - `widget.WeightWidgetUiState` — sealed (`Idle | Saving | Success | Error`) plus Preferences serialization helpers (`toWidgetUiState` / `writeWidgetUiState`) and `statusText(context)` extension.
  - `widget.WidgetErrorReason` — `PermissionDenied | HealthConnectUnavailable | Unknown(message)` plus `Throwable.toWidgetErrorReason()` mapper.
- **domain/**
  - `domain.usecase.ValidateWeightInputUseCase` — accepts `^\d+(\.\d+)?$`, max 1 decimal, `0 < kg <= 500` (`MAX_WEIGHT_KG = 500.0`).
  - `domain.usecase.SaveWeightUseCase` — converts kg + `ClockProvider.nowInTokyo()` into Instant/ZoneOffset and delegates to `WeightRepository`.
  - `domain.usecase.CheckHealthConnectAvailabilityUseCase` — thin wrapper over `HealthConnectClientProvider.getStatus()`.
  - `domain.usecase.ComputeWeightCandidatesUseCase` — produces 11 `WeightCandidate`s around `baseKg` (or `DEFAULT_BASELINE_KG = 60.0` if null) at 0.1 kg steps using `BigDecimal` to avoid float noise. Values outside `0 < kg ≤ MAX_WEIGHT_KG` are dropped.
  - `domain.model.WeightInputResult` — `Valid(kg) | Empty | NotANumber | OutOfRange | TooManyDecimals`.
  - `domain.model.WeightSaveError` — `PermissionDenied | HealthConnectUnavailable | Unknown(cause)`. Subclasses `Throwable` so they can flow through `Result.failure`.
  - `domain.model.WeightCandidate` — `(kg: Double, isBaseline: Boolean)`.
  - `domain.time.ClockProvider` / `SystemClockProvider` — provides `ZonedDateTime` in `Asia/Tokyo` (fixed timezone, not the device default).
- **data/**
  - `data.healthconnect.HealthConnectClientProvider` — maps `HealthConnectClient.getSdkStatus()` to `HealthConnectAvailability` and lazily returns a client when installed.
  - `data.healthconnect.HealthConnectAvailability` — `Unknown | Installed | NotInstalled | UpdateRequired`.
  - `data.repository.WeightRepository` (interface) / `HealthConnectWeightRepository` — writes a `WeightRecord` with `Metadata.manualEntry()`. Translates `SecurityException` → `WeightSaveError.PermissionDenied`, any other throwable → `WeightSaveError.Unknown`.
  - `data.preferences.LastWeightRepository` (interface) / `DataStoreLastWeightRepository` — persists the most recently saved weight as a `Float` in DataStore Preferences (`last_weight_preferences`). `WeightViewModel` and `WeightSelectionActionCallback` both call `setLastWeight` on save success so the widget centers on the latest value.
- **di/AppModule.kt** — `@Module @InstallIn(SingletonComponent::class)` binding `WeightRepository ← HealthConnectWeightRepository`, `ClockProvider ← SystemClockProvider`, and `LastWeightRepository ← DataStoreLastWeightRepository`.

### Save flow (app)

1. User types into the field → `onWeightChange` filters and validates.
2. User taps 登録 → `onSubmit` emits to `permissionRequest`.
3. `WeightScreen` launches the Health Connect permission contract for `WRITE_WEIGHT`.
4. `onPermissionResult(granted)` either emits `PermissionDenied` snackbar or transitions to `Submitting` and runs `SaveWeightUseCase`.
5. On success → call `LastWeightRepository.setLastWeight(kg)`, reset to empty `Ready` + `SaveSuccess` snackbar. On failure → restore previous `Ready` state + appropriate snackbar.

### Save flow (widget)

1. `WeightWidget.provideGlance` reads `LastWeightRepository.getLastWeight()` and feeds it into `ComputeWeightCandidatesUseCase` to render 11 cells; current UI status is read from Glance Preferences state.
2. User taps a cell → `WeightSelectionActionCallback.onAction(kg)` runs in the background.
3. Callback checks `HealthConnectAvailability`. Anything other than `Installed` writes `Error(HealthConnectUnavailable)` and returns.
4. Callback writes `Saving(kg)` → calls `SaveWeightUseCase` → on success calls `setLastWeight(kg)`, writes `Success(kg)`, then `delay(3 s)` and writes `Idle`. On failure maps `WeightSaveError` to `WidgetErrorReason` and writes `Error`.
5. When the widget is in `Error`, an "アプリを開く" link appears with `actionStartActivity<MainActivity>()` so the user can re-grant permissions or recover.

### Manifest essentials

- `uses-permission android:name="android.permission.health.WRITE_WEIGHT"`.
- `<queries><package android:name="com.google.android.apps.healthdata"/></queries>` so the app can resolve the Health Connect package on Android 11+.
- `activity-alias` `ViewMainActivity` exposes `ACTION_VIEW_PERMISSION_USAGE` / `category.HEALTH_PERMISSIONS` (required by Health Connect rationale flow on Android 13 and below).
- `<receiver android:name=".widget.WeightWidgetReceiver">` with `APPWIDGET_UPDATE` intent-filter and `@xml/weight_widget_info` meta-data so the home-screen widget is discoverable.

## Code layout

- `app/src/main/java/com/kktaro/simplifyweightrecorder/`
  - `MainActivity.kt`, `SimplifyWeightRecorderApp.kt`
  - `ui/weight/` — `WeightScreen.kt`, `WeightInputContent.kt`, `WeightViewModel.kt`, `WeightUiState.kt`
  - `ui/theme/` — `Theme.kt`, `Color.kt`, `Type.kt`
  - `widget/` — `WeightWidget.kt`, `WeightWidgetReceiver.kt`, `WeightWidgetEntryPoint.kt`, `WeightWidgetUiState.kt`, `WeightSelectionActionCallback.kt`, `WidgetErrorReason.kt`
  - `domain/usecase/`, `domain/model/`, `domain/time/`
  - `data/healthconnect/`, `data/repository/`, `data/preferences/`
  - `di/AppModule.kt`
- `app/src/main/res/values/strings.xml` — all user-facing strings (Japanese).
- `app/src/main/res/xml/weight_widget_info.xml` — `appwidget-provider` metadata.
- `app/src/main/res/layout/weight_widget_preview.xml` — widget picker preview layout.
- `app/src/test/` — JVM unit tests (JUnit 4 + MockK + Turbine). Tests: `WeightViewModelTest`, `ValidateWeightInputUseCaseTest`, `SaveWeightUseCaseTest`, `ComputeWeightCandidatesUseCaseTest`, `WidgetErrorReasonMapperTest`.
- `app/src/androidTest/` — instrumented tests; Compose UI tests use `androidx.compose.ui.test.junit4`.

## Conventions / gotchas

- Time is always **Asia/Tokyo** via `ClockProvider`. Do not call `Instant.now()` directly in production code — inject `ClockProvider` so tests can fake it.
- `WeightSaveError` extends `Throwable`; surface it via `Result.failure(...)` rather than throwing across coroutine boundaries by hand.
- `HealthConnectAvailability.Unknown` keeps the UI in `Initializing` — treat it as a transient state, not a failure.
- Input filtering (digits + single `.`) lives in `WeightViewModel.filterInput`; validation rules (range, decimal count) live in `ValidateWeightInputUseCase`. Keep the two responsibilities separate.
- When adding a new flow that touches Health Connect, route status checks through `HealthConnectClientProvider` so the `Installed/NotInstalled/UpdateRequired/Unknown` mapping stays in one place.
- Whenever a new save path is added, call `LastWeightRepository.setLastWeight(kg)` on success so the widget's center cell stays in sync with the latest entry.
- Glance components cannot use Hilt's constructor injection. Resolve dependencies through `WeightWidgetEntryPoint` (extend it if you need more bindings) and `EntryPointAccessors.fromApplication(...)`.
- Permission acquisition for Health Connect cannot be triggered from a widget. In error states the widget surfaces an "アプリを開く" link that launches `MainActivity` so the existing in-app permission flow takes over.
