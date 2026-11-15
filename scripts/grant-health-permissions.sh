#!/bin/zsh
# usage: scripts/grant-health-permissions.sh [revoke]
# アプリが使う Health Connect の権限をまとめて付与（revoke で剥奪）する。
# connectedDebugAndroidTest はアプリを入れ直すので、その後は付与し直す。
set -euo pipefail
ADB=${ADB:-~/Library/Android/sdk/platform-tools/adb}
PKG=com.kktaro.simplifyweightrecorder
ACTION=${1:-grant}
for p in WRITE_WEIGHT READ_WEIGHT READ_HEALTH_DATA_HISTORY; do
  "$ADB" shell pm "$ACTION" "$PKG" "android.permission.health.$p"
done
echo "$ACTION: WRITE_WEIGHT READ_WEIGHT READ_HEALTH_DATA_HISTORY"
