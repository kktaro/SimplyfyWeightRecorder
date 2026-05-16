# Simplify Weight Recorder

体重を**ワンタップで Health Connect に記録するだけ**の、最小構成の Android アプリ。
履歴表示やグラフ等の機能は持たず、入力 → 保存 だけに特化している。

> **Note:** 本アプリは作者個人のローカル利用を想定しており、Google Play Store 等への公開・配布は予定していない。

- パッケージ: `com.kktaro.simplifyweightrecorder`
- Application ID: `com.kktaro.simplifyweightrecorder`
- UI 言語: 日本語

## 機能

- 体重 (kg) を入力して Health Connect の `WeightRecord` として保存する。
- 記録時刻は端末ロケールに依らず **Asia/Tokyo** で固定。
- Health Connect の状態に応じて画面を出し分ける:
  - 未インストール / 更新が必要 → Play Store への導線を表示
  - 利用可能 → 入力フォームを表示
- 書き込み権限 (`android.permission.health.WRITE_WEIGHT`) は登録ボタン押下時にリクエスト。
- 入力バリデーション:
  - 数値のみ (小数点は 1 個まで)
  - 小数点以下は 1 桁まで
  - `0 < weight ≤ 500 (kg)`

## 動作要件

| 項目 | 値 |
| --- | --- |
| minSdk | 34 (Android 14) |
| targetSdk / compileSdk | 36 |
| 必須アプリ | [Health Connect](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) |

実機 / エミュレーターに Health Connect がインストールされていない、もしくは古い場合は、アプリ内のボタンから Play Store に誘導される。

## ビルド / 実行

リポジトリルートで Gradle Wrapper を使用する:

```sh
./gradlew assembleDebug         # debug APK をビルド
./gradlew installDebug          # 接続中の端末 / エミュレーターにインストール
./gradlew test                  # JVM ユニットテスト
./gradlew connectedAndroidTest  # 計装テスト (要・端末/エミュレーター)
./gradlew lint                  # Android Lint
```

特定テストだけを動かす例:

```sh
./gradlew testDebugUnitTest --tests "com.kktaro.simplifyweightrecorder.ui.weight.WeightViewModelTest"
```

Android Studio で開く場合は、ルートディレクトリを「Open an Existing Project」で開けば動作する。

## 技術スタック

- Kotlin `2.2.10` / AGP `9.2.1`
- Jetpack Compose (BOM `2026.02.01`) + Material 3
- Hilt `2.59.2` + KSP `2.2.10-2.0.2`
- AndroidX Lifecycle (ViewModel / Runtime Compose) `2.8.7`
- Kotlin Coroutines `1.9.0`
- Health Connect Client `1.1.0`
- Test: JUnit 4 / MockK `1.13.12` / Turbine `1.1.0` / `kotlinx-coroutines-test`

依存関係はすべて `gradle/libs.versions.toml` の version catalog 経由で管理している (リポジトリは `dependencyResolutionManagement` で `FAIL_ON_PROJECT_REPOS` に固定)。

## アーキテクチャ

シンプルな 3 層構成:

```
ui (Compose + ViewModel)
  └─ domain (UseCase / Model / ClockProvider)
       └─ data (Health Connect Client / Repository)
```

主要コンポーネント:

| 役割 | クラス |
| --- | --- |
| 画面 | `ui.weight.WeightScreen`, `ui.weight.WeightInputContent` |
| 状態管理 | `ui.weight.WeightViewModel`, `ui.weight.WeightUiState` |
| バリデーション | `domain.usecase.ValidateWeightInputUseCase` |
| 保存処理 | `domain.usecase.SaveWeightUseCase` |
| 可用性チェック | `domain.usecase.CheckHealthConnectAvailabilityUseCase` |
| 時刻供給 | `domain.time.ClockProvider` / `SystemClockProvider` |
| Health Connect 接続 | `data.healthconnect.HealthConnectClientProvider` |
| 永続化 | `data.repository.WeightRepository` ← `HealthConnectWeightRepository` |
| DI | `di.AppModule` |

`WeightUiState` は `Initializing | Unavailable | Ready | Submitting` の sealed interface、エラーやイベントは `SnackbarEvent` 経由で `SharedFlow` として通知する。

### 保存フロー

1. ユーザーが体重を入力 → `onWeightChange` で入力フィルタ + バリデーション。
2. 「登録」ボタン → `onSubmit` が権限リクエストイベントを発火。
3. `WeightScreen` が `WRITE_WEIGHT` の権限ランチャーを起動。
4. 結果が `true` なら `Submitting` に遷移し、`SaveWeightUseCase` 経由で `WeightRecord` を書き込み。
5. 成功時はフォームをクリア + 成功 Snackbar、失敗時は前の入力を復元 + 原因別 Snackbar。

## ライセンス

(未設定)
