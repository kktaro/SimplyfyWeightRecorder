#!/bin/zsh
# usage: scripts/shot.sh <name>
# 端末の画面を撮り、Read で見る用に縮小した build/shots/<name>.png を作る（原寸は <name>.full.png）。
set -euo pipefail
ADB=${ADB:-~/Library/Android/sdk/platform-tools/adb}
DIR="$(cd "$(dirname "$0")/.." && pwd)/build/shots"
mkdir -p "$DIR"
"$ADB" exec-out screencap -p > "$DIR/$1.full.png"
sips -Z "${SHOT_MAX_PX:-800}" "$DIR/$1.full.png" --out "$DIR/$1.png" > /dev/null
echo "$DIR/$1.png"
