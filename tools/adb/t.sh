# t.sh tap X Y [wait] | t.sh key KEY [wait] | t.sh text "str" [wait] | t.sh swipe x1 y1 x2 y2 [wait]  -> then dump UI
P="$(cd "$(dirname "$0")" && pwd)"
S="${ADB_SERIAL:?set ADB_SERIAL (see: adb devices)}"
A="adb -s $S shell input"
case $1 in
 tap) $A tap $2 $3; w=${4:-2};;
 key) $A keyevent $2; w=${3:-2};;
 text) $A text "$2"; w=${3:-1};;
 swipe) $A swipe $2 $3 $4 $5 400; w=${6:-2};;
 none) w=${2:-0};;
esac
sleep $w; python -I "$P/ui.py"
