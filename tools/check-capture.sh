#!/usr/bin/env bash
# Prove the processed palm photo is clean: bash tools/check-capture.sh
#   Copies cache/scan/capture.jpg out of the app (debuggable builds only, run-as) into a temp dir that is always
#   removed (the photo is the user's palm), then checks: JPEG, long edge <= 1280, empty getexif(), and no Exif
#   APP1 bytes. Prints "capture OK: WxH, N bytes" or "capture FAILED: reason". Env: PKG (default app.palm.dev.debug).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/tools/_device.sh"
PKG="${PKG:-app.palm.dev.debug}"
PY="$ROOT/backend/.venv/Scripts/python.exe"
T="$(mktemp -d)"
trap 'rm -rf "$T"' EXIT

# exec-out would copy cat's error text into the file, so check the file on the device first
if ! adb -s "$ADB_SERIAL" shell run-as "$PKG" test -s cache/scan/capture.jpg \
  || ! adb -s "$ADB_SERIAL" exec-out run-as "$PKG" cat cache/scan/capture.jpg > "$T/capture.jpg" \
  || [ ! -s "$T/capture.jpg" ]; then
  echo "capture FAILED: cache/scan/capture.jpg is missing or empty" >&2
  exit 1
fi

# The path goes in as an argument (MSYS converts it for the Windows Python); never inside the -c text
CHECK='
import os, sys
from PIL import Image
path = sys.argv[1]
size = os.path.getsize(path)
with open(path, "rb") as f:
    head = f.read(65536)
try:
    im = Image.open(path)
    im.load()
except Exception as e:
    print(f"capture FAILED: not an image ({type(e).__name__})"); sys.exit(1)
w, h = im.size
problems = []
if im.format != "JPEG":
    problems.append(f"format {im.format}, not JPEG")
if max(w, h) > 1280:
    problems.append(f"long edge {max(w, h)} > 1280")
if len(im.getexif()) != 0:
    problems.append(f"{len(im.getexif())} EXIF tags")
if b"Exif\x00\x00" in head:
    problems.append("Exif APP1 marker present")
if problems:
    print("capture FAILED: " + "; ".join(problems)); sys.exit(1)
print(f"capture OK: {w}x{h}, {size} bytes")
'
"$PY" -I -c "$CHECK" "$T/capture.jpg"
