#!/usr/bin/env bash
# Full local stack check: backend up, adb reverse, install + launch, then wait until
# every regex argument matches the phone's UI dump. Usage: bash tools/smoke-phone.sh "Backend: Connected" ...
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

if ! curl -fsS "$HEALTH" > /dev/null 2>&1; then
  bash "$ROOT/tools/run-backend.sh" > /dev/null 2>&1 &
  STARTED_PID=$!
  for _ in $(seq 60); do curl -fsS "$HEALTH" > /dev/null 2>&1 && break; sleep 1; done
  curl -fsS "$HEALTH" > /dev/null || { echo "backend did not answer $HEALTH within 60 s" >&2; exit 1; }
fi

bash "$ROOT/tools/dev-reverse.sh"
bash "$ROOT/tools/install-debug.sh"

DUMP=""
for _ in $(seq 15); do
  sleep 2
  DUMP="$(python -I "$ROOT/tools/adb/ui.py" 2>&1 || true)"
  ok=1
  for re in "$@"; do printf '%s\n' "$DUMP" | grep -Eq -- "$re" || { ok=0; break; }; done
  if [ "$ok" -eq 1 ]; then echo "smoke OK: $*"; exit 0; fi
done
printf '%s\n' "$DUMP"
echo "smoke FAILED: not all of [$*] found on screen within 30 s" >&2
exit 1
