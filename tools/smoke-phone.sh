#!/usr/bin/env bash
# Full local stack check: backend up, adb reverse, install + launch, then wait until
# every regex argument matches the phone's UI dump. Usage: bash tools/smoke-phone.sh "Backend: Connected" ...
# Env switches: SMOKE_NO_REVERSE=1 (remove the adb reverse rule), SMOKE_NO_BACKEND=1 (keep the backend down),
# SMOKE_RETRY_EXPECT=<regex> (then start backend + reverse, tap Retry, wait up to 15 s for the regex).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ "$#" -ge 1 ] || { echo "usage: $0 <regex>..." >&2; exit 2; }
source "$ROOT/tools/_device.sh"

HEALTH=http://127.0.0.1:8000/healthz
STARTED_PID=""

port_pid() { # PID listening on 127.0.0.1:8000 (Windows), empty if none
  netstat -ano 2>/dev/null | tr -d '\r' | awk '$1 == "TCP" && $2 == "127.0.0.1:8000" && $4 == "LISTENING" { print $5; exit }'
}

cleanup() {
  [ -n "$STARTED_PID" ] || return 0
  kill "$STARTED_PID" 2>/dev/null || true
  for _ in 1 2 3 4 5; do curl -fsS "$HEALTH" > /dev/null 2>&1 || break; sleep 1; done
  # On Windows the uvicorn child can outlive the bash job
  local pid; pid="$(port_pid)"
  if [ -n "$pid" ]; then taskkill //F //T //PID "$pid" > /dev/null 2>&1 || true; fi
  sleep 1
  if [ -n "$(port_pid)" ]; then echo "warning: port 8000 is still in use" >&2; else echo "backend stopped, port 8000 free"; fi
}
trap cleanup EXIT

ensure_backend() {
  curl -fsS "$HEALTH" > /dev/null 2>&1 && return 0
  bash "$ROOT/tools/run-backend.sh" > /dev/null 2>&1 &
  STARTED_PID=$!
  for _ in $(seq 60); do curl -fsS "$HEALTH" > /dev/null 2>&1 && break; sleep 1; done
  curl -fsS "$HEALTH" > /dev/null || { echo "backend did not answer $HEALTH within 60 s" >&2; exit 1; }
}

ui_dump() { python -I "$ROOT/tools/adb/ui.py" 2>&1 || true; }

# SMOKE_NO_BACKEND=1: leave the backend down (checks the "No response" diagnosis)
if [ "${SMOKE_NO_BACKEND:-}" = 1 ]; then
  if curl -fsS "$HEALTH" > /dev/null 2>&1; then
    echo "SMOKE_NO_BACKEND=1 but $HEALTH already answers: stop the running backend first" >&2
    exit 1
  fi
else
  ensure_backend
fi

# SMOKE_NO_REVERSE=1: drop the adb reverse rule (checks the "Connection refused" diagnosis)
if [ "${SMOKE_NO_REVERSE:-}" = 1 ]; then
  adb -s "$ADB_SERIAL" reverse --remove-all
else
  bash "$ROOT/tools/dev-reverse.sh"
fi
bash "$ROOT/tools/install-debug.sh"

DUMP=""
for _ in $(seq 15); do
  sleep 2
  DUMP="$(ui_dump)"
  ok=1
  for re in "$@"; do printf '%s\n' "$DUMP" | grep -Eq -- "$re" || { ok=0; break; }; done
  [ "$ok" -eq 1 ] && break
done
if [ "$ok" -ne 1 ]; then
  printf '%s\n' "$DUMP"
  echo "smoke FAILED: not all of [$*] found on screen within 30 s" >&2
  exit 1
fi
echo "smoke OK: $*"
[ -n "${SMOKE_RETRY_EXPECT:-}" ] || exit 0

# SMOKE_RETRY_EXPECT=<regex>: repair the link, tap Retry (no relaunch), expect the regex
ensure_backend
bash "$ROOT/tools/dev-reverse.sh"
XY="$(printf '%s\n' "$DUMP" | grep -E '^Retry  \(' | head -1 | sed -E 's/.*\(([0-9]+),([0-9]+)\).*/\1 \2/')"
[ -n "$XY" ] || { printf '%s\n' "$DUMP"; echo "smoke FAILED: no Retry button in the dump" >&2; exit 1; }
# shellcheck disable=SC2086
adb -s "$ADB_SERIAL" shell input tap $XY
deadline=$((SECONDS + 15))
while [ "$SECONDS" -lt "$deadline" ]; do
  sleep 1
  DUMP="$(ui_dump)"
  if printf '%s\n' "$DUMP" | grep -Eq -- "$SMOKE_RETRY_EXPECT"; then echo "smoke OK after Retry: $SMOKE_RETRY_EXPECT"; exit 0; fi
done
printf '%s\n' "$DUMP"
echo "smoke FAILED: [$SMOKE_RETRY_EXPECT] not on screen within 15 s of tapping Retry" >&2
exit 1
