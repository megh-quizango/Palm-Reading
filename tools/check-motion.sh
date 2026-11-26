#!/usr/bin/env bash
# On-device motion check for the star background: bash tools/check-motion.sh off|on
#   off: animator_duration_scale=0 -> two captures 2 s apart must be identical (static under reduce motion)
#   on:  setting deleted (animations on) -> the two captures must differ
# The phone's original animator_duration_scale is always restored on exit.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-}"
case "$MODE" in off|on) ;; *) echo "usage: $0 off|on" >&2; exit 2 ;; esac
source "$ROOT/tools/_device.sh"
PKG=app.palm.dev.debug

ORIG="$(adb -s "$ADB_SERIAL" shell settings get global animator_duration_scale | tr -d '\r')"
restore() {
  if [ "$ORIG" = null ]; then
    adb -s "$ADB_SERIAL" shell settings delete global animator_duration_scale > /dev/null
  else
    adb -s "$ADB_SERIAL" shell settings put global animator_duration_scale "$ORIG"
  fi
  echo "animator_duration_scale restored to $ORIG"
}
trap restore EXIT

if [ "$MODE" = off ]; then
  adb -s "$ADB_SERIAL" shell settings put global animator_duration_scale 0
else
  adb -s "$ADB_SERIAL" shell settings delete global animator_duration_scale > /dev/null
fi
# NocturneTheme reads the scale once per process, so restart the app
adb -s "$ADB_SERIAL" shell am start -S -n "$PKG/app.palm.MainActivity" > /dev/null
sleep 6
# Rows below the status bar (clock and icons change on their own)
H1="$(python -I "$ROOT/tools/screen.py" hash 120 1400)"
sleep 2
H2="$(python -I "$ROOT/tools/screen.py" hash 120 1400)"
echo "capture 1: $H1"
echo "capture 2: $H2"
if [ "$MODE" = off ]; then
  [ "$H1" = "$H2" ] && { echo "motion OK: static with animations off"; exit 0; }
  echo "motion FAILED: the screen changed with animations off" >&2
else
  [ "$H1" != "$H2" ] && { echo "motion OK: animating with animations on"; exit 0; }
  echo "motion FAILED: the screen did not change with animations on" >&2
fi
exit 1
