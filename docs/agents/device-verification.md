# エミュレータでの動作確認

実機が `adb devices` に出ないときは、ローカルの AVD で確認する。

## 起動

```sh
~/Library/Android/sdk/emulator/emulator -list-avds          # Pixel_9_API_35 / Medium_Phone_API_35
~/Library/Android/sdk/emulator/emulator -avd Pixel_9_API_35 -no-snapshot-save -no-audio   # バックグラウンド実行
adb wait-for-device; until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
```

API 35 は Health Connect がシステムに組み込まれているので、別途インストール不要。

## インストール・テスト

```sh
./gradlew installDebug
./gradlew connectedDebugAndroidTest
adb shell am force-stop com.kktaro.simplifyweightrecorder
adb shell am start -W -n com.kktaro.simplifyweightrecorder/.MainActivity
```

## 状態の観測

作業ファイルは worktree 内の `build/`（ignore 済み）に置く。scratchpad のパスは `github` を含むため、ループや `&&` と組み合わせると worktree のサンドボックスに git 操作と誤判定されて拒否される。

- 画面: `scripts/shot.sh <name>` → `build/shots/<name>.png`（長辺 800px に縮小済み）を Read で見る。原寸は `<name>.full.png`。
- タップ: `scripts/tap.sh <表示テキスト>` で、テキストか content-desc が一致する要素の中心を叩く。画像から座標を換算しない。テキストの無い要素は `adb shell uiautomator dump` の `bounds` から求める。
- キーボード表示: `adb shell dumpsys input_method | grep -m1 mInputShown`
- 最前面 Activity: `adb shell dumpsys activity activities | grep -m1 topResumedActivity`
- 入力: `adb shell input text 65.4`、戻る `adb shell input keyevent KEYCODE_BACK`、ホーム `KEYCODE_HOME`

## Health Connect の権限

画面フローを確かめないときは `scripts/grant-health-permissions.sh` で WRITE_WEIGHT / READ_WEIGHT / READ_HEALTH_DATA_HISTORY をまとめて付与する（`revoke` を付けると剥奪）。`connectedDebugAndroidTest` はアプリを入れ直して権限が消えるので、その後は付与し直す。

権限フロー自体を確認するときは剥奪してから操作する。新しい AVD の初回は Health Connect のオンボーディング（Get started）→ 権限画面（トグル → Allow）が出る。

## 後片付け

起動したエミュレータは確認後に停止する（`adb emu kill`）。
