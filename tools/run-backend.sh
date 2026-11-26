#!/usr/bin/env bash
# Run the backend in the foreground on Python 3.13, loopback only (adb reverse reaches it; the LAN cannot).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec uv run --directory "$ROOT/backend" uvicorn app.main:app --host 127.0.0.1 --port 8000
