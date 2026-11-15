# Coding standards

レビューで差分に当てるルール。機械的に判定できるものは `scripts/check-banned-apis.sh` などで CI が落とすので、ここには判断が要るものだけを置く。

## 時刻

- 現在時刻は `ClockProvider`（Asia/Tokyo）を注入して取る。日付の境界も Asia/Tokyo で切る。直接の `now()` 呼び出しは CI（`scripts/check-banned-apis.sh`）が検出するが、`Clock.systemDefaultZone()` やデフォルトタイムゾーンへの依存など、パターンに掛からない形も同じ扱いにする。

## エラー

- `WeightSaveError` / `WeightReadError` は `Throwable` を継承し、`Result.failure(...)` で返す。コルーチン境界をまたいで手で throw しない。
- `HealthConnectAvailability.Unknown` は一時的な状態として扱い、UI を `Initializing` のままにする。失敗として表示しない。

## Health Connect

- 利用可否の判定は `HealthConnectClientProvider` を経由させ、`Installed/NotInstalled/UpdateRequired/Unknown` への対応付けを一箇所に保つ。
- 新しい保存経路では、成功時に `LastWeightRepository.setLastWeight(kg)` を呼ぶ。呼ばないとウィジェット中央の候補が最新の記録とずれる。

## 入力

- 入力のフィルタ（数字と `.` 1つ）は `WeightViewModel.filterInput`、妥当性（範囲・小数桁）は `ValidateWeightInputUseCase` に置き、混ぜない。

## UI

- 見た目が変わる Composable の変更には、Roborazzi のスクリーンショットテスト（`app/src/test/.../*ScreenshotTest.kt`）の追加か画像の撮り直しを伴わせる。撮り直した画像は差分を目で確認してからコミットする。
- 語彙は `GLOSSARY.md` に合わせる（体重記録・日次体重・体重推移・表示期間）。
