#!/usr/bin/env bash
# Open a fixture Result on the device without a camera: bash tools/result-open.sh
#   Launches the app, onboards (English, Asha, 2000-01-15) when Home is not reached, long-presses the Home header
#   (debug Showcase), opens the Fixture lab, opens ScanReady with the lab photo and taps "Read my palm".
#   Env: PKG (default app.palm.dev.debug).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ "$#" -eq 0 ] || { echo "usage: $0" >&2; exit 2; }
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
export PKG
UI=(bash "$ROOT/tools/ui.sh")
# Home in either app language (the Hindi hero title read from the resources, not typed here)
HI_HOME="$(sed -n 's/.*name="home_hero_title">\([^<]*\)<.*/\1/p' "$ROOT/android/app/src/main/res/values-hi/strings_home.xml")"
HOME_RE="Read your palm|$HI_HOME"

"${UI[@]}" launch > /dev/null
if ! UI_TIMEOUT=8 "${UI[@]}" expect "$HOME_RE" > /dev/null 2>&1; then
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
fi
# Labels end with two spaces before the "(x,y)" centre in ui.py lines; tap/hold add the coordinate match themselves
"${UI[@]}" hold '^\[AppName\]  ' # the header long-press opens the Showcase in debug builds
"${UI[@]}" tap "^Fixture lab"
"${UI[@]}" tap "^Open ScanReady"
"${UI[@]}" tap "^(Read my palm|मेरी हथेली पढ़ें)  "
