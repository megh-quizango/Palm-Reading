#!/usr/bin/env bash
# Drive the phone UI through uiautomator dumps (tools/adb/ui.py lines: "label  (cx,cy) flags", first line "activity: ...").
# Usage: bash tools/ui.sh launch | reset | expect REGEX... | absent REGEX... | tap REGEX | hold REGEX | scroll-to REGEX | type TEXT | back | swipe-left
# Env: PKG (default app.palm.dev.debug), UI_TIMEOUT seconds for expect/tap (default 15), UI_SETTLE seconds for absent (default 2).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
usage() { echo "usage: $0 launch|reset|expect REGEX...|absent REGEX...|tap REGEX|hold REGEX|scroll-to REGEX|type TEXT|back|swipe-left" >&2; exit 2; }
[ "$#" -ge 1 ] || usage
CMD="$1"; shift
case "$CMD" in
  launch|reset|back|swipe-left) ;;
  expect|absent|tap|hold|scroll-to|type) [ "$#" -ge 1 ] || usage ;;
  *) usage ;;
esac
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
TIMEOUT="${UI_TIMEOUT:-15}"
A=(adb -s "$ADB_SERIAL")

dump() { python -I "$ROOT/tools/adb/ui.py" 2>&1 || true; }

all_match() { # dump regex...
  local d="$1"; shift
  local r
  for r in "$@"; do printf '%s\n' "$d" | grep -qE -- "$r" || return 1; done
}

wait_for() { # regex... ; prints the last dump, returns 1 on timeout
  local end=$((SECONDS + TIMEOUT)) d
  while :; do
    d="$(dump)"
    if all_match "$d" "$@"; then LAST="$d"; return 0; fi
    if [ "$SECONDS" -ge "$end" ]; then LAST="$d"; return 1; fi
    sleep 1
  done
}

find_xy() { # regex ; sets xy to the first matching node's centre, fails like tap
  if ! wait_for "$1.*\([0-9]+,[0-9]+\)"; then
    printf '%s\n' "$LAST"
    echo "ui FAILED: $CMD $1" >&2
    exit 1
  fi
  xy="$(printf '%s\n' "$LAST" | grep -E -- "$1" | grep -oE '\([0-9]+,[0-9]+\)' | head -n 1 | tr -d '()')"
}

case "$CMD" in
  launch) "${A[@]}" shell am start -S -W -n "$PKG/app.palm.MainActivity" ;;
  reset) "${A[@]}" shell pm clear "$PKG" ;;
  expect)
    if ! wait_for "$@"; then
      printf '%s\n' "$LAST"
      echo "ui FAILED: expect $*" >&2
      exit 1
    fi ;;
  absent)
    sleep "${UI_SETTLE:-2}"
    d="$(dump)"
    for r in "$@"; do
      if printf '%s\n' "$d" | grep -qE -- "$r"; then
        printf '%s\n' "$d"
        echo "ui FAILED: present $r" >&2
        exit 1
      fi
    done ;;
  tap)
    find_xy "$1"
    "${A[@]}" shell input tap "${xy%,*}" "${xy#*,}" ;;
  hold) # long press: a zero-length swipe held for 1 s
    find_xy "$1"
    "${A[@]}" shell input swipe "${xy%,*}" "${xy#*,}" "${xy%,*}" "${xy#*,}" 1000 ;;
  scroll-to) # uiautomator lists only on-screen Compose nodes, so swipe up by half the screen until REGEX shows (max 8)
    size="$("${A[@]}" shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
    w="${size%x*}"; h="${size#*x}"
    d="$(dump)"
    n=0
    until printf '%s\n' "$d" | grep -qE -- "$1"; do
      if [ "$n" -ge 8 ]; then printf '%s\n' "$d"; echo "ui FAILED: scroll-to $1" >&2; exit 1; fi
      "${A[@]}" shell input swipe $((w / 2)) $((h * 3 / 4)) $((w / 2)) $((h / 4)) 300
      n=$((n + 1))
      sleep 0.5
      d="$(dump)"
    done ;;
  type)
    d="$(dump)"
    xy="$(printf '%s\n' "$d" | grep -E '\) [CS]*E[CS]*$' | grep -oE '\([0-9]+,[0-9]+\)' | head -n 1 | tr -d '()')"
    if [ -z "$xy" ]; then printf '%s\n' "$d"; echo "ui FAILED: type (no editable field)" >&2; exit 1; fi
    "${A[@]}" shell input tap "${xy%,*}" "${xy#*,}"
    sleep 0.5
    "${A[@]}" shell input text "${1// /%s}" ;;
  back) "${A[@]}" shell input keyevent 4 ;;
  swipe-left)
    size="$("${A[@]}" shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
    w="${size%x*}"; h="${size#*x}"
    "${A[@]}" shell input swipe $((w * 85 / 100)) $((h / 2)) $((w * 15 / 100)) $((h / 2)) 300 ;;
esac
