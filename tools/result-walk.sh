#!/usr/bin/env bash
# Phase 5 emulator walk: bash tools/result-walk.sh
#   For en then hi: every fixture variant through tools/result-open.sh (aspect cycles 3:4, 1:1, 9:16, skin alternates),
#   expecting the Result title or "Photo not accepted" plus the reason title, with the Love and Career tabs for mixed;
#   then font scale 2.0 in en and hi (mixed, long_text, invalid_jewelry, Home card); then force-stop + reopen the last
#   reading and open the share sheet (in en). Screenshots: tools/adb/shots/phase5-*.png (git-ignored).
#   Font scale and the app locale are saved first and restored on exit. Env: PKG (default app.palm.dev.debug).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
export PKG
A=(adb -s "$ADB_SERIAL")
UI=(bash "$ROOT/tools/ui.sh")
RES="$ROOT/android/app/src/main/res"
CODES=(mixed all_llm all_template all_none present_not_drawn hindi long_text invalid_blurry_or_dark invalid_too_far invalid_jewelry invalid_bent invalid_fingers_tight invalid_incomplete invalid_no_palm_lines)
REASONS=(blurry_or_dark too_far jewelry bent fingers_tight incomplete no_palm_lines)
ASPECTS=(3:4 1:1 9:16)
SKINS=(light dark)

ORIG_FS="$("${A[@]}" shell settings get system font_scale | tr -d '\r')"
ORIG_LOC="$("${A[@]}" shell cmd locale get-app-locales "$PKG" | tr -d '\r' | sed -n 's/.*\[\(.*\)\].*/\1/p')"
restore() {
  if [ "$ORIG_FS" = "null" ]; then "${A[@]}" shell settings delete system font_scale > /dev/null
  else "${A[@]}" shell settings put system font_scale "$ORIG_FS"; fi
  "${A[@]}" shell cmd locale set-app-locales "$PKG" --locales "${ORIG_LOC:-en}"
}
trap restore EXIT

declare -A P # P[lang:key] = literal ERE of that string
load() { # lang dir file key...
  local lang="$1" xml="$RES/$2/$3.xml"; shift 3
  local keys=("$@") lines i
  mapfile -t lines < <(python -I "$ROOT/tools/res_pattern.py" "$xml" "${keys[@]}" | tr -d '\r') # Windows Python writes CRLF
  [ "${#lines[@]}" -eq "${#keys[@]}" ] || { echo "res_pattern.py failed for $xml" >&2; exit 1; }
  for i in "${!keys[@]}"; do P["$lang:${keys[$i]}"]="${lines[$i]}"; done
}
for lang in en hi; do
  dir=values; [ "$lang" = en ] || dir="values-$lang"
  load "$lang" "$dir" strings_result result_title invalid_title result_tab_love result_tab_career result_share \
    $(for r in "${REASONS[@]}"; do printf 'invalid_%s_title ' "$r"; done)
  load "$lang" "$dir" strings_home home_last_reading_overline
done

CUR="" # what is being walked, for the failure message
run() { "$@" || { echo "walk FAILED: $CUR" >&2; exit 1; }; }
shot() { run bash "$ROOT/tools/adb/shot.sh" "$1"; }
set_lang() { "${A[@]}" shell cmd locale set-app-locales "$PKG" --locales "$1"; }

open_variant() { # lang code aspect skin
  local lang="$1" code="$2"
  CUR="$lang $code ($3, $4)"
  run bash "$ROOT/tools/result-open.sh" --aspect "$3" --skin "$4" --variant "$code"
  case "$code" in
    invalid_*) run "${UI[@]}" expect "${P[$lang:invalid_title]}" "${P[$lang:${code}_title]}" ;;
    *) run "${UI[@]}" expect "${P[$lang:result_title]}" ;;
  esac
}

for lang in en hi; do
  set_lang "$lang"
  for i in "${!CODES[@]}"; do
    code="${CODES[$i]}"
    open_variant "$lang" "$code" "${ASPECTS[$((i % 3))]}" "${SKINS[$((i % 2))]}"
    shot "phase5-$lang-$code"
    if [ "$code" = mixed ]; then
      for tab in love career; do
        CUR="$lang mixed tab $tab"
        run "${UI[@]}" scroll-to "^${P[$lang:result_tab_$tab]}  \("
        run "${UI[@]}" tap "^${P[$lang:result_tab_$tab]}  "
        sleep 1 # tab content cross-fade
        shot "phase5-$lang-mixed-$tab"
      done
    fi
  done
done

"${A[@]}" shell settings put system font_scale 2.0
for lang in en hi; do
  set_lang "$lang"
  for code in mixed long_text invalid_jewelry; do
    open_variant "$lang" "$code" 3:4 light
    shot "phase5-$lang-$code-fs2"
  done
  CUR="$lang home fs2"
  run "${UI[@]}" launch > /dev/null
  run "${UI[@]}" scroll-to "${P[$lang:home_last_reading_overline]}"
  shot "phase5-$lang-home-fs2"
done
"${A[@]}" shell settings put system font_scale 1.0

# Reopen after a kill and share, in English (the patterns below are the en strings)
set_lang en
CUR="reopen after force-stop"
"${A[@]}" shell am force-stop "$PKG"
run "${UI[@]}" launch > /dev/null
run "${UI[@]}" scroll-to "${P[en:home_last_reading_overline]}"
run "${UI[@]}" tap "${P[en:home_last_reading_overline]}"
run "${UI[@]}" expect "${P[en:result_title]}"
shot phase5-reopen
CUR="share sheet"
run "${UI[@]}" scroll-to "^${P[en:result_share]}  \("
run "${UI[@]}" tap "^${P[en:result_share]}  "
run "${UI[@]}" expect "^activity: .*Chooser"
sleep 1 # chooser slide-in, so the screenshot shows the card preview
shot phase5-share-sheet
"${UI[@]}" back
echo "walk OK: $(ls "$ROOT"/tools/adb/shots/phase5-*.png | wc -l) phase5 screenshots"
