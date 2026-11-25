# chat.sh "message": type and send a chat message, then report screen state at 3 s and 20 s
P="$(cd "$(dirname "$0")" && pwd)"; S="${ADB_SERIAL:?set ADB_SERIAL (see: adb devices)}"
adb -s $S shell input tap 300 1515; sleep 1
adb -s $S shell input text "$(echo "$1" | sed 's/ /%s/g; s/?/\?/g; s/'"'"'/\'"'"'/g')"; sleep 1
o=$(python -I "$P/ui.py"); c=$(echo "$o" | grep -E '\(6[0-9][0-9],1[0-9]{3}\) C' | tail -1 | grep -oE '\([0-9]+,[0-9]+\)' | tr -d '()' | tr ',' ' ')
adb -s $S shell input tap ${c:-650 1515}; sleep 3; echo "== +3s"; python -I "$P/ui.py" | grep -vE '^-' | cut -c1-200 | tail -8
sleep 17; echo "== +20s"; python -I "$P/ui.py" | grep -vE '^-' | cut -c1-400 | tail -10
