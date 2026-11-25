# back.sh [until-regex]: press Back, close any ads/dialogs until the screen matches the regex
P="$(cd "$(dirname "$0")" && pwd)"; S="${ADB_SERIAL:?set ADB_SERIAL (see: adb devices)}"
adb -s $S shell input keyevent 4; sleep 4
for i in 1 2 3 4; do o=$(python -I "$P/ui.py")
  echo "$o" | grep -qE "${1:-MainActivity}" && ! echo "$o" | grep -q 'satisfied' && { echo "ok: $(echo "$o" | head -1 | sed 's/.*\///')"; exit 0; }
  echo "$o" | grep -q AppLovinFullscreen && { echo "STUCK applovin"; exit 2; }
  c=$(echo "$o" | grep -E '^(Close Ad|Close|Skip) ' | head -1 | grep -oE '\([0-9]+,[0-9]+\)' | tr -d '()' | tr ',' ' ')
  [ -z "$c" ] && echo "$o" | grep -q satisfied && c="614 664"
  [ -n "$c" ] && { echo "closing on $(echo "$o" | head -1 | sed 's/.*\///')"; adb -s $S shell input tap $c; }
  sleep 4; done; echo "unmatched"; echo "$o" | head -6
