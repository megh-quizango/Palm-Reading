#!/usr/bin/env bash
# Build, install and launch the debug app on the single connected phone.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/tools/_device.sh"
bash "$ROOT/tools/gradlew.sh" :app:installDebug -q
adb -s "$ADB_SERIAL" shell am start -S -n app.palm.dev.debug/app.palm.MainActivity
