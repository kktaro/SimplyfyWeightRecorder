#!/bin/zsh
# usage: scripts/tap.sh <text>
# 画面上で text / content-desc が <text> を含む最初の要素の中心をタップする。座標の換算は不要。
set -euo pipefail
ADB=${ADB:-~/Library/Android/sdk/platform-tools/adb}
"$ADB" shell uiautomator dump /sdcard/ui.xml > /dev/null
XY=$("$ADB" exec-out cat /sdcard/ui.xml | python3 -I -c '
import re, sys
target = sys.argv[1]
for m in re.finditer(r"<node [^>]*>", sys.stdin.read()):
    n = m.group(0)
    attrs = dict(re.findall(r"([\w-]+)=\"([^\"]*)\"", n))
    if target in attrs.get("text", "") or target in attrs.get("content-desc", ""):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", attrs["bounds"]))
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1")
if [[ -z "$XY" ]]; then
  echo "not found: $1" >&2
  exit 1
fi
"$ADB" shell input tap ${=XY}
echo "tapped $1 at $XY"
