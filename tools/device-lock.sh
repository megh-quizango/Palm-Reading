#!/usr/bin/env bash
# Run a command while holding the phone lock, so parallel executors never share the phone.
# The lock lives in the git common dir, shared by every worktree of this repo. Usage: bash tools/device-lock.sh <command...>
set -uo pipefail
[ "$#" -ge 1 ] || { echo "usage: $0 <command...>" >&2; exit 2; }
LOCK="$(git rev-parse --path-format=absolute --git-common-dir)/palm-device.lock"
deadline=$((SECONDS + 45 * 60))
shown=""
until mkdir "$LOCK" 2>/dev/null; do
  # A crashed holder cannot block the phone forever
  if [ -n "$(find "$LOCK" -maxdepth 0 -mmin +30 2>/dev/null)" ]; then
    echo "device-lock: removing stale lock $LOCK" >&2
    rm -rf "$LOCK"
    continue
  fi
  if [ -z "$shown" ]; then
    echo "device-lock: waiting for $(cat "$LOCK/owner" 2>/dev/null || echo '?')" >&2
    shown=1
  fi
  [ "$SECONDS" -lt "$deadline" ] || { echo "device-lock: gave up after 45 minutes" >&2; exit 1; }
  sleep 5
done
trap 'rm -rf "$LOCK"' EXIT
echo "pid $$, $(pwd), $(date)" > "$LOCK/owner"
"$@"
exit $?
