#!/usr/bin/env bash
# On-device motion check for the star background: bash tools/check-motion.sh off|on|saver
#   off:   animator_duration_scale=0 -> two captures 2 s apart must be identical (static under reduce motion)
#   on:    setting deleted (animations on) -> the two captures must differ
#   saver: animations on + battery saver on at start -> the two captures must be identical
# Env: PKG (default app.palm.dev.debug; release: PKG=app.palm.dev).
# The phone's original animator_duration_scale (and in saver mode low_power and the battery override) is restored on exit.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-}"
case "$MODE" in off|on|saver) ;; *) echo "usage: $0 off|on|saver" >&2; exit 2 ;; esac
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"

ORIG="$(adb -s "$ADB_SERIAL" shell settings get global animator_duration_scale | tr -d '\r')"
ORIG_LP=""
restore() {
  if [ "$ORIG" = null ]; then
    adb -s "$ADB_SERIAL" shell settings delete global animator_duration_scale > /dev/null
  else
    adb -s "$ADB_SERIAL" shell settings put global animator_duration_scale "$ORIG"
  fi
  echo "animator_duration_scale restored to $ORIG"
  if [ -n "$ORIG_LP" ]; then
    if [ "$ORIG_LP" = null ]; then
      adb -s "$ADB_SERIAL" shell settings delete global low_power > /dev/null
    else
      adb -s "$ADB_SERIAL" shell settings put global low_power "$ORIG_LP"
    fi
    adb -s "$ADB_SERIAL" shell dumpsys battery reset
    echo "low_power restored to $ORIG_LP, battery override reset"
  fi
}
trap restore EXIT

if [ "$MODE" = off ]; then
  adb -s "$ADB_SERIAL" shell settings put global animator_duration_scale 0
else
  adb -s "$ADB_SERIAL" shell settings delete global animator_duration_scale > /dev/null
fi
if [ "$MODE" = saver ]; then
  ORIG_LP="$(adb -s "$ADB_SERIAL" shell settings get global low_power | tr -d '\r')"
  # Battery saver switches itself off while the phone charges
  adb -s "$ADB_SERIAL" shell dumpsys battery unplug
  adb -s "$ADB_SERIAL" shell settings put global low_power 1
fi
# NocturneTheme reads the scale once per process, so restart the app
adb -s "$ADB_SERIAL" shell am start -S -n "$PKG/app.palm.MainActivity" > /dev/null
sleep 6
if [ "$MODE" = saver ] && [ "$(adb -s "$ADB_SERIAL" shell settings get global low_power | tr -d '\r')" != 1 ]; then
  echo "motion FAILED: battery saver did not turn on" >&2
  exit 1
fi
# Rows below the status bar (clock and icons change on their own)
H1="$(python -I "$ROOT/tools/screen.py" hash 120 1400)"
sleep 2
H2="$(python -I "$ROOT/tools/screen.py" hash 120 1400)"
echo "capture 1: $H1"
echo "capture 2: $H2"
case "$MODE" in
  off)
    [ "$H1" = "$H2" ] && { echo "motion OK: static with animations off"; exit 0; }
    echo "motion FAILED: the screen changed with animations off" >&2 ;;
  on)
    [ "$H1" != "$H2" ] && { echo "motion OK: animating with animations on"; exit 0; }
    echo "motion FAILED: the screen did not change with animations on" >&2 ;;
  saver)
    [ "$H1" = "$H2" ] && { echo "motion OK: static under battery saver"; exit 0; }
    echo "motion FAILED: the screen changed under battery saver" >&2 ;;
esac
exit 1
