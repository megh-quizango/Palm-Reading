#!/usr/bin/env bash
# SCAN-07 session gate (D-13): bash tools/check-scan-session.sh SECONDS
#   Precondition: the Scan screen of PKG is already open and live (for example after tools/scan-open.sh --grant).
#   Clears logcat, resets gfxinfo, then for SECONDS samples "Thermal Status" every 10 s and finally reads:
#   gfxinfo total frames and "Number Frame deadline missed" (missed-vsync fallback, as tools/check-jank.sh),
#   battery temperature start/end (dumpsys battery, tenths of a degree C), ANRs (am_anr events, "ANR in PKG"),
#   and whether PKG is alive and resumed.
#   Gates: any ANR, process gone or not resumed, frames < MIN_FRAMES, missed > MAX_JANK_PCT % of frames,
#   max thermal status > MAX_THERMAL. A temperature rise above 8.0 C is a WARN line, not a gate.
#   Changes no device setting.
# Env: PKG (default app.palm.dev.debug), MAX_JANK_PCT (default 5), MIN_FRAMES (default SECONDS/2), MAX_THERMAL (default 1).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SECS="${1:-}"
[[ "$SECS" =~ ^[1-9][0-9]*$ ]] || { echo "usage: $0 SECONDS  (Scan screen already open)" >&2; exit 2; }
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
MAX_JANK_PCT="${MAX_JANK_PCT:-5}"
MIN_FRAMES="${MIN_FRAMES:-$((SECS / 2))}"
MAX_THERMAL="${MAX_THERMAL:-1}"
A=(adb -s "$ADB_SERIAL")

temp() { "${A[@]}" shell dumpsys battery | tr -d '\r' | awk -F': *' '$1 ~ /^ *temperature$/ { print $2; exit }'; }
thermal() { "${A[@]}" shell dumpsys thermalservice | tr -d '\r' | grep -m 1 'Thermal Status:' | grep -oE '[0-9]+' | head -n 1; }

"${A[@]}" logcat -b all -c
"${A[@]}" shell dumpsys gfxinfo "$PKG" reset > /dev/null
t0="$(temp)"
max_thermal=-1
end=$((SECONDS + SECS))
while :; do
  th="$(thermal || true)"
  [ -n "$th" ] || { echo "session FAILED: no Thermal Status line" >&2; exit 1; }
  [ "$th" -gt "$max_thermal" ] && max_thermal="$th"
  left=$((end - SECONDS))
  [ "$left" -gt 0 ] || break
  sleep $((left < 10 ? left : 10))
done

G="$("${A[@]}" shell dumpsys gfxinfo "$PKG" | tr -d '\r')"
num() { printf '%s\n' "$G" | grep -m 1 "^$1:" | grep -oE '[0-9]+' | head -n 1 || true; }  # a missing line must reach the fallback, not end the script (set -e)
total="$(num 'Total frames rendered')"
missed="$(num 'Number Frame deadline missed')"
metric="frame deadline missed"
if [ -z "$missed" ]; then
  missed="$(num 'Number Missed Vsync')"
  metric="missed vsync (no deadline-missed line on this Android version)"
fi
t1="$(temp)"
anr="$( { "${A[@]}" logcat -d -b events | tr -d '\r' | grep 'am_anr' | grep -F "$PKG" || true; "${A[@]}" logcat -d | tr -d '\r' | grep -F "ANR in $PKG" || true; } | grep -c . || true)"
pid="$("${A[@]}" shell pidof "$PKG" | tr -d '\r' || true)"
resumed="$("${A[@]}" shell dumpsys activity activities | tr -d '\r' | grep -E 'mResumedActivity|topResumedActivity|ResumedActivity:' | grep -F "$PKG/" || true)"

[ -n "$total" ] && [ -n "$missed" ] || { echo "session FAILED: gfxinfo had no frame stats for $PKG" >&2; exit 1; }
pct="$(awk -v m="$missed" -v t="$total" 'BEGIN { printf "%.2f", t ? 100 * m / t : 0 }')"
dt="$(awk -v a="${t0:-0}" -v b="${t1:-0}" 'BEGIN { printf "%.1f", (b - a) / 10 }')"
echo "session: ${SECS}s frames=$total missed=$missed (${pct}%, $metric) max_thermal=$max_thermal temp=$(awk -v a="${t0:-0}" 'BEGIN { printf "%.1f", a / 10 }')C->$(awk -v b="${t1:-0}" 'BEGIN { printf "%.1f", b / 10 }')C delta=${dt}C anr=$anr"
awk -v d="$dt" 'BEGIN { exit !(d > 8.0) }' && echo "WARN: battery temperature rose ${dt}C (recorded, not a gate)"

fail() { echo "session FAILED: $1" >&2; exit 1; }
[ "$anr" -eq 0 ] || fail "$anr ANR line(s) for $PKG"
if [ -z "$pid" ] || [ -z "$resumed" ]; then
  "${A[@]}" shell dumpsys power | tr -d '\r' | grep -m 1 'mWakefulness' >&2 || true
  [ -n "$pid" ] || fail "$PKG process is gone"
  fail "$PKG is not the resumed activity"
fi
[ "$total" -ge "$MIN_FRAMES" ] || fail "only $total frames (want >= $MIN_FRAMES)"
[ $((missed * 100)) -le $((MAX_JANK_PCT * total)) ] || fail "$missed of $total frames (${pct}%) hit $metric, limit $MAX_JANK_PCT%"
[ "$max_thermal" -le "$MAX_THERMAL" ] || fail "thermal status reached $max_thermal (limit $MAX_THERMAL)"
echo "session OK: ${pct}% $metric, max thermal $max_thermal, no ANR, $PKG resumed"
