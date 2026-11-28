#!/usr/bin/env bash
# Open a fixture Result on the device without a camera: bash tools/result-open.sh [--aspect 3:4|1:1|9:16] [--skin light|dark] [--variant CODE]
#   Launches the app, onboards (English, Asha, 2000-01-15) when Home is not reached, long-presses the Home header
#   (debug Showcase), opens the Fixture lab, picks the given aspect / skin / variant (CODE: a FixtureVariant code),
#   opens ScanReady with the lab photo and taps "Read my palm". Without options the lab defaults apply (launch restarts the process, clearing the last choice).
#   Env: PKG (default app.palm.dev.debug).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CODES="mixed all_llm all_template all_none present_not_drawn hindi long_text invalid_blurry_or_dark invalid_too_far invalid_jewelry invalid_bent invalid_fingers_tight invalid_incomplete invalid_no_palm_lines"
usage() { echo "usage: $0 [--aspect 3:4|1:1|9:16] [--skin light|dark] [--variant CODE]" >&2; echo "  CODE: $CODES" >&2; exit 2; }
ASPECT=""; SKIN=""; VARIANT=""
while [ "$#" -gt 0 ]; do
  [ "$#" -ge 2 ] || usage
  case "$1" in
    --aspect) case "$2" in 3:4|1:1|9:16) ASPECT="$2" ;; *) usage ;; esac ;;
    --skin) case "$2" in light) SKIN="Light" ;; dark) SKIN="Dark" ;; *) usage ;; esac ;;
    --variant) case " $CODES " in *" $2 "*) VARIANT="$2" ;; *) usage ;; esac ;;
    *) usage ;;
  esac
  shift 2
done
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
# Pickers in screen order (scroll-to only scrolls down); the two buttons are pinned below the scroll
if [ -n "$ASPECT" ]; then "${UI[@]}" scroll-to "^$ASPECT  \("; "${UI[@]}" tap "^$ASPECT  "; fi
if [ -n "$SKIN" ]; then "${UI[@]}" scroll-to "^$SKIN  \("; "${UI[@]}" tap "^$SKIN  "; fi
if [ -n "$VARIANT" ]; then "${UI[@]}" scroll-to "\($VARIANT\)  \("; "${UI[@]}" tap "\($VARIANT\)  "; fi
"${UI[@]}" tap "^Open ScanReady"
"${UI[@]}" tap "^(Read my palm|मेरी हथेली पढ़ें)  "
