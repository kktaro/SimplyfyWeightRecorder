# CLAUDE.md

## Project

Android app `Simplify Weight Recorder` (package `com.kktaro.simplifyweightrecorder`, single module `:app`, Compose + Material 3, Hilt). Writes a body-weight `WeightRecord` to **Health Connect**. Two entry points:

- **App** — single screen `ui.weight.WeightScreen` + `WeightViewModel`, driven by `WeightUiState` (`Initializing | Unavailable | Ready | Submitting`). Below the input form, `WeightTrendSection` + `WeightTrendViewModel` show the 体重推移 graph (Vico) of daily weights read back from Health Connect; vocabulary is in `GLOSSARY.md`.
- **Widget** — Jetpack Glance (`widget/`): 11 candidate buttons (±0.5 kg around the last saved weight); a tap saves directly from the widget.

Layers under `com.kktaro.simplifyweightrecorder`: `ui/` → `domain/` (use cases, models, `ClockProvider`) → `data/` (Health Connect, DataStore); bindings in `di/AppModule.kt`.

## Commands

```sh
./gradlew testDebugUnitTest lintDebug assembleDebugAndroidTest   # what CI runs (.github/workflows/ci.yml)
./gradlew connectedDebugAndroidTest                              # instrumented tests; needs device/emulator
```

Device/emulator verification (AVD launch, adb observation, Health Connect permission flow): `docs/agents/device-verification.md`.

Dependencies go through `gradle/libs.versions.toml` as `libs.*`; `FAIL_ON_PROJECT_REPOS` breaks the build on module-level `repositories {}` blocks. Vico is pinned to `3.2.x`: 3.3+ is built with Kotlin 2.4, which the project's Kotlin 2.2 compiler cannot read.

## Conventions / gotchas

- Time is always **Asia/Tokyo** via `ClockProvider`. Do not call `Instant.now()` directly in production code — inject `ClockProvider` so tests can fake it.
- `WeightSaveError` extends `Throwable`; surface it via `Result.failure(...)` rather than throwing across coroutine boundaries by hand.
- `HealthConnectAvailability.Unknown` keeps the UI in `Initializing` — treat it as a transient state, not a failure.
- Input filtering (digits + single `.`) lives in `WeightViewModel.filterInput`; validation rules (range, decimal count) live in `ValidateWeightInputUseCase`. Keep the two responsibilities separate.
- When adding a new flow that touches Health Connect, route status checks through `HealthConnectClientProvider` so the `Installed/NotInstalled/UpdateRequired/Unknown` mapping stays in one place.
- Whenever a new save path is added, call `LastWeightRepository.setLastWeight(kg)` on success so the widget's center cell stays in sync with the latest entry.
- Glance components cannot use Hilt's constructor injection. Resolve dependencies through `WeightWidgetEntryPoint` (extend it if you need more bindings) and `EntryPointAccessors.fromApplication(...)`.
- Permission acquisition for Health Connect cannot be triggered from a widget. In error states the widget surfaces an "アプリを開く" link that launches `MainActivity` so the existing in-app permission flow takes over.
- The manifest's `ViewMainActivity` `activity-alias` (`ACTION_VIEW_PERMISSION_USAGE` / `HEALTH_PERMISSIONS`) is required by the Health Connect permission rationale flow — keep it even though nothing in code references it.
- The trend graph never requests `READ_WEIGHT` automatically — only from its 許可する button, via a launcher separate from the write one. Both permission launches must call `autoFocusState.onPermissionRequestLaunched()` / `onPermissionResult()` so returning from Health Connect doesn't re-pop the keyboard.
- androidTest has no Hilt test setup; to UI-test screen logic, extract a ViewModel-free composable (e.g. `WeightRouter`) and test that.

## Agent skills

### Issue tracker

issue は GitHub Issues（kktaro/SimplyfyWeightRecorder）で `gh` CLI を使って管理する。詳細は `docs/agents/issue-tracker.md`。

### Triage labels

デフォルトの5ロール（`needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human`、`wontfix`）をそのまま使う。詳細は `docs/agents/triage-labels.md`。

### Domain docs

single-context 構成：リポジトリ直下に `GLOSSARY.md` と `docs/adr/` を1つずつ置く。詳細は `docs/agents/domain.md`。
