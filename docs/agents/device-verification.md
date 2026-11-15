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

- 画面: `adb exec-out screencap -p > <scratchpad>/x.png` を撮って Read で見る。座標は画像表示倍率から実解像度（1080x2424）に換算してから `adb shell input tap` する。
- キーボード表示: `adb shell dumpsys input_method | grep -m1 mInputShown`
- 最前面 Activity: `adb shell dumpsys activity activities | grep -m1 topResumedActivity`
- 入力: `adb shell input text 65.4`、戻る `adb shell input keyevent KEYCODE_BACK`、ホーム `KEYCODE_HOME`

## Health Connect の権限

新しい AVD では、初回の登録時に Health Connect のオンボーディング（Get started）→ 権限画面（Weight トグル → Allow）が出る。画面フローを試さないなら、事前付与を試す（未検証。効かなければ画面で許可する）:

```sh
adb shell pm grant com.kktaro.simplifyweightrecorder android.permission.health.WRITE_WEIGHT
```

権限フロー自体を確認するときは `adb shell pm revoke ...` で戻す。

## 後片付け

起動したエミュレータは確認後に停止する（`adb emu kill`）。
