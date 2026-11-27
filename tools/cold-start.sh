#!/usr/bin/env bash
# Cold start to first frame (D-16): PKG=app.palm.dev bash tools/cold-start.sh [--compile] [runs]
#   --compile: one untimed launch, wait for ProfileInstaller, then compile speed-profile (research Pitfall 8)
#   Then N timed runs (default 5) of am start -S -W; prints each TotalTime and the median.
# Env: PKG (required), BUDGET_MS (default 2000). Exits 1 when a run lacks TotalTime or median >= budget.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -n "${PKG:-}" ] || { echo "usage: PKG=<package> $0 [--compile] [runs]" >&2; exit 2; }
COMPILE=""
if [ "${1:-}" = "--compile" ]; then COMPILE=1; shift; fi
RUNS="${1:-5}"
BUDGET_MS="${BUDGET_MS:-2000}"
source "$ROOT/tools/_device.sh"
A=(adb -s "$ADB_SERIAL")
ACT="$PKG/app.palm.MainActivity"

if [ -n "$COMPILE" ]; then
  "${A[@]}" shell am start -S -W -n "$ACT" > /dev/null
  # ProfileInstaller writes the embedded profile about 5-6 s after the first frame; -W returns at the first frame
  sleep 12
  "${A[@]}" shell cmd package compile -m speed-profile -f "$PKG"
  "${A[@]}" shell dumpsys package dexopt | tr -d '\r' | grep -A 3 "\[$PKG\]" | grep -m 1 -E 'filter|status' || echo "compiler filter line not found for $PKG"
fi

times=()
for i in $(seq 1 "$RUNS"); do
  t="$("${A[@]}" shell am start -S -W -n "$ACT" | tr -d '\r' | awk '/TotalTime/ { print $2 }')"
  [ -n "$t" ] || { echo "cold-start FAILED: run $i printed no TotalTime" >&2; exit 1; }
  echo "run $i: $t ms"
  times+=("$t")
  sleep 3
done
median="$(printf '%s\n' "${times[@]}" | sort -n | sed -n "$(( (RUNS + 1) / 2 ))p")"
echo "median $median ms over $RUNS runs (budget $BUDGET_MS ms)"
[ "$median" -lt "$BUDGET_MS" ] || { echo "cold-start FAILED: median $median ms >= $BUDGET_MS ms" >&2; exit 1; }
