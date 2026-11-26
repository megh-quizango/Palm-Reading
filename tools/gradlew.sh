#!/usr/bin/env bash
# Run the Android Gradle wrapper from anywhere: bash tools/gradlew.sh <gradle args>
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PROPS="$ROOT/android/local.properties"
if [ ! -f "$PROPS" ]; then
  # ANDROID_HOME is not set on this PC; point Gradle at the default SDK location
  : "${LOCALAPPDATA:?LOCALAPPDATA not set; create android/local.properties with sdk.dir=...}"
  printf 'sdk.dir=%s\n' "$(printf '%s' "$LOCALAPPDATA/Android/Sdk" | tr '\' '/')" > "$PROPS"
fi
if [ -n "${ADB_SERIAL:-}" ]; then export ANDROID_SERIAL="$ADB_SERIAL"; fi
cd "$ROOT/android"
exec ./gradlew "$@"
