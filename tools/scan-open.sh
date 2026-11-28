#!/usr/bin/env bash
# Open the Scan route on the phone: bash tools/scan-open.sh [--grant|--revoke] [left|right]
#   --grant / --revoke change CAMERA before the app starts (revoking a granted runtime permission kills the
#   app's process); --revoke also clears the user-set / user-fixed flags so the system dialog appears again.
#   Onboards (English, Asha, 2000-01-15) when Home is not reached, then re-applies the permission change,
#   because onboarding starts with pm clear. Env: PKG (default app.palm.dev.debug).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
usage() { echo "usage: $0 [--grant|--revoke] [left|right]" >&2; exit 2; }
PERM=""
HAND=right
for arg in "$@"; do
  case "$arg" in
    --grant|--revoke) [ -z "$PERM" ] || usage; PERM="$arg" ;;
    left|right) HAND="$arg" ;;
    *) usage ;;
  esac
done
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
export PKG
A=(adb -s "$ADB_SERIAL")
UI=(bash "$ROOT/tools/ui.sh")

perm() {
  case "$PERM" in
    --grant) "${A[@]}" shell pm grant "$PKG" android.permission.CAMERA ;;
    --revoke)
      "${A[@]}" shell pm revoke "$PKG" android.permission.CAMERA
      "${A[@]}" shell pm clear-permission-flags "$PKG" android.permission.CAMERA user-set user-fixed ;;
  esac
}

perm
"${UI[@]}" launch > /dev/null
if ! UI_TIMEOUT=8 "${UI[@]}" expect "Read your palm" > /dev/null 2>&1; then
  "${UI[@]}" reset
  "${UI[@]}" launch > /dev/null
  "${UI[@]}" tap "^English"
  "${UI[@]}" tap "Continue in English"
  "${UI[@]}" tap "^Next"
  "${UI[@]}" tap "^Next"
  "${UI[@]}" tap "Get started"
  "${UI[@]}" expect "Your first name" > /dev/null # type dumps once, so wait for the name screen first
  "${UI[@]}" type Asha
  "${UI[@]}" expect "Asha" > /dev/null
  "${UI[@]}" tap "^Continue"
  "${UI[@]}" tap "Choose a date"
  "${UI[@]}" tap "January 15, 2000|15 January 2000"
  "${UI[@]}" tap "Set date"
  "${UI[@]}" tap "^Continue"
  "${UI[@]}" expect "Read your palm" > /dev/null
  perm # pm clear wiped the permission state
  "${UI[@]}" launch > /dev/null
  "${UI[@]}" expect "Read your palm" > /dev/null
fi
"${UI[@]}" tap "Scan my palm"
if [ "$HAND" = left ]; then "${UI[@]}" tap "Left hand"; fi
"${UI[@]}" tap "^Continue"
