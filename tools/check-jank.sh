#!/usr/bin/env bash
# Frame pacing gate (FND-03): bash tools/check-jank.sh idle SECONDS | swipe COUNT
#   Resets gfxinfo, then idles SECONDS (sky animating) or does COUNT horizontal swipes, then reads gfxinfo.
#   Gate: share of frames with "Frame deadline missed" <= MAX_JANK_PCT (the "Janky frames" line also counts
#   "High input latency" frames, so it is not the signal; see 01-05 summary).
# Env: PKG (default app.palm.dev.debug), MAX_JANK_PCT (default 5).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-}"; N="${2:-}"
case "$MODE" in idle|swipe) ;; *) echo "usage: $0 idle SECONDS | swipe COUNT" >&2; exit 2 ;; esac
[[ "$N" =~ ^[1-9][0-9]*$ ]] || { echo "usage: $0 idle SECONDS | swipe COUNT" >&2; exit 2; }
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
MAX_JANK_PCT="${MAX_JANK_PCT:-5}"
A=(adb -s "$ADB_SERIAL")

"${A[@]}" shell dumpsys gfxinfo "$PKG" reset > /dev/null
if [ "$MODE" = idle ]; then
  sleep "$N"
  MIN_FRAMES=$((30 * N))
else
  size="$("${A[@]}" shell wm size | tr -d '\r' | grep -oE '[0-9]+x[0-9]+' | tail -n 1)"
  w="${size%x*}"; h="${size#*x}"
  for _ in $(seq 1 "$N"); do
    "${A[@]}" shell input swipe $((w * 85 / 100)) $((h / 2)) $((w * 15 / 100)) $((h / 2)) 300
    sleep 1
  done
  MIN_FRAMES=30
fi
G="$("${A[@]}" shell dumpsys gfxinfo "$PKG" | tr -d '\r')"
printf '%s\n' "$G" | grep -E '^(Total frames rendered|Janky frames|50th percentile|90th percentile|99th percentile|Number Missed Vsync|Number Slow UI thread|Number Frame deadline missed):' || true
num() { printf '%s\n' "$G" | grep -m 1 "^$1:" | grep -oE '[0-9]+' | head -n 1; }
total="$(num 'Total frames rendered')"
missed="$(num 'Number Frame deadline missed')"
metric="frame deadline missed"
if [ -z "$missed" ]; then
  missed="$(num 'Number Missed Vsync')"
  metric="missed vsync (no deadline-missed line on this Android version)"
fi
[ -n "$total" ] && [ -n "$missed" ] || { echo "jank FAILED: gfxinfo had no frame stats for $PKG" >&2; exit 1; }
if [ "$total" -lt "$MIN_FRAMES" ]; then
  echo "jank FAILED: only $total frames (want >= $MIN_FRAMES; is the sky animating?)" >&2
  exit 1
fi
if [ $((missed * 100)) -gt $((MAX_JANK_PCT * total)) ]; then
  echo "jank FAILED: $missed of $total frames ($((missed * 100 / total))%) hit $metric, limit $MAX_JANK_PCT%" >&2
  exit 1
fi
echo "jank OK: $missed of $total frames missed the deadline ($metric)"
