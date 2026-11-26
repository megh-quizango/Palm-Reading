#!/usr/bin/env bash
# Tests tools/_device.sh against a stub adb: 0, 1, 2 devices, a preset ADB_SERIAL, an unknown preset.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
STUB="$(mktemp -d)"
trap 'rm -rf "$STUB"' EXIT
cat > "$STUB/adb" <<'EOF'
#!/usr/bin/env bash
# Stub: `adb devices` lists the space-separated serials in STUB_DEVICES
printf 'List of devices attached\r\n'
for s in $STUB_DEVICES; do printf '%s\tdevice\r\n' "$s"; done
printf 'offline-1\toffline\r\n\r\n'
EOF
chmod +x "$STUB/adb"
export PATH="$STUB:$PATH"
unset ADB_SERIAL ANDROID_SERIAL

# run_case <devices> <preset serial or empty>: prints rc, exported serials and messages
run_case() {
  STUB_DEVICES="$1" bash -c '
    [ -n "$2" ] && export ADB_SERIAL="$2"
    source "$1" && rc=0 || rc=$?
    echo "rc=$rc ADB_SERIAL=${ADB_SERIAL:-} ANDROID_SERIAL=${ANDROID_SERIAL:-}"
  ' _ "$ROOT/tools/_device.sh" "$2" 2>&1
}

expect() { # expect <name> <output> <regex>...
  local name="$1" out="$2"; shift 2
  for re in "$@"; do
    printf '%s\n' "$out" | grep -Eq -- "$re" || { printf '%s\n' "$out"; echo "FAIL $name: missing /$re/" >&2; exit 1; }
  done
  echo "ok $name"
}

expect "zero devices" "$(run_case "" "")" "^rc=1 " "no device"
expect "one device" "$(run_case "SER1" "")" "^rc=0 ADB_SERIAL=SER1 ANDROID_SERIAL=SER1$"
expect "two devices" "$(run_case "SER1 SER2" "")" "^rc=1 " "several devices" "^SER1$" "^SER2$"
expect "two devices, preset" "$(run_case "SER1 SER2" "SER2")" "^rc=0 ADB_SERIAL=SER2 ANDROID_SERIAL=SER2$"
expect "unknown preset" "$(run_case "SER1" "NOPE")" "^rc=1 " "NOPE is not listed"

# callers run under set -e: zero devices must still print why (WR-01)
out="$(STUB_DEVICES="" bash -c 'set -e; source "$1"' _ "$ROOT/tools/_device.sh" 2>&1 || true)"
expect "zero devices under set -e" "$out" "no device"
