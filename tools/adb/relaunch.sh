# relaunch.sh: force-stop app, relaunch, close splash ad, dismiss rating/what's-new; ends on home
P="$(cd "$(dirname "$0")" && pwd)"; S="${ADB_SERIAL:?set ADB_SERIAL (see: adb devices)}"; PK=com.ai.palmreading.scanpalmistry.horoscope
adb -s $S shell am force-stop $PK; adb -s $S shell monkey -p $PK -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
for i in $(seq 1 12); do sleep 4; o=$(python -I "$P/ui.py")
  if echo "$o" | grep -q 'Palmistry Reader' && ! echo "$o" | grep -qE 'Got it|satisfied'; then echo "home reached"; exit 0; fi
  c=$(echo "$o" | grep -E '^(Close Ad|Close|Got it) ' | head -1 | grep -oE '\([0-9]+,[0-9]+\)' | tr -d '()' | tr ',' ' ')
  [ -z "$c" ] && echo "$o" | grep -q 'satisfied' && c="614 664"
  [ -n "$c" ] && { echo "tap $c ($(echo "$o" | head -1 | sed 's/.*\///'))"; adb -s $S shell input tap $c; }
done; echo "not home"; echo "$o" | head -5
