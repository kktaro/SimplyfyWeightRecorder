#!/bin/sh
# 本番コードで直接呼んではいけない API を検出する。CI から呼ばれる。
# 現在時刻は ClockProvider（Asia/Tokyo）を注入して取る。実装本体の ClockProvider.kt だけは除外する。
set -eu
cd "$(dirname "$0")/.."
PATTERN='(Instant|LocalDate|LocalDateTime|ZonedDateTime|OffsetDateTime|LocalTime)\.now\(|System\.currentTimeMillis\('
if grep -rnE "$PATTERN" app/src/main --include='*.kt' | grep -v '/domain/time/ClockProvider.kt:'; then
  echo "error: 現在時刻は ClockProvider を注入して取得すること（CLAUDE.md / CODING_STANDARDS.md 参照）" >&2
  exit 1
fi
echo "check-banned-apis: ok"
