# shot.sh <name> : save a phone screenshot (JPEG-ish size via screencap png)
P="$(cd "$(dirname "$0")" && pwd)"
S="${ADB_SERIAL:?set ADB_SERIAL (see: adb devices)}"
mkdir -p "$P/shots"
adb -s $S exec-out screencap -p > "$P/shots/$1.png" && echo "$P/shots/$1.png"
