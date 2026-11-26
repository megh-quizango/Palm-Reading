#!/usr/bin/env bash
# Forward the phone's 127.0.0.1:8000 to the PC backend. Idempotent; re-run after any adb reconnect.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/tools/_device.sh"
adb -s "$ADB_SERIAL" reverse --remove-all
adb -s "$ADB_SERIAL" reverse tcp:8000 tcp:8000 > /dev/null
adb -s "$ADB_SERIAL" reverse --list
