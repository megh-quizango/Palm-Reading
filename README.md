# [AppName]: AI Palm Reading & Astrology (Android)

Android app (Kotlin, Jetpack Compose) plus a local FastAPI backend that turns a palm photo into a reading.

## Prerequisites

- JDK 17
- Android SDK under `%LOCALAPPDATA%\Android\Sdk` (platform-tools, `platforms;android-37.0`)
- [uv](https://docs.astral.sh/uv/) (installs CPython 3.13 for the backend)
- Git Bash (all commands below run from the repo root)
- One Android phone paired for wireless debugging (`adb devices` lists exactly one `device`; with several, set `ADB_SERIAL=<serial>`)

## Run it

**One command installs and launches the app:** `bash tools/install-debug.sh`

| Step | Command |
|---|---|
| Start the backend (Python 3.13, 127.0.0.1:8000) | `bash tools/run-backend.sh` |
| Forward the phone's 127.0.0.1:8000 to the PC (re-run after any adb reconnect) | `bash tools/dev-reverse.sh` |
| Build, install and launch the debug app on the single connected phone | `bash tools/install-debug.sh` |
| All of the above plus a UI assertion (used by verification) | `bash tools/smoke-phone.sh "Backend: Connected"` |
| Any Gradle task (writes `android/local.properties` if missing) | `bash tools/gradlew.sh <task>` |

Tests: `uv run --directory backend pytest -q`, `bash tools/gradlew.sh :app:testDebugUnitTest`, `bash tools/test_device.sh`.

## Anthropic key (needed from Phase 2)

```bash
cp backend/.env.example backend/.env   # then put your key after ANTHROPIC_API_KEY=
```

`backend/.env` is git-ignored; never commit it. The key stays on the server and never goes into the app. `GET /healthz` reports `"anthropic_key": "configured"` or `"missing"`, never the key itself.

## When the app shows `Backend: Unreachable`

The status row shows the cause, the base URL and a Retry button. Fix the cause, then tap Retry.

| Cause on the phone | Fix |
|---|---|
| `Connection refused` | The adb reverse rule is gone: `bash tools/dev-reverse.sh` (and make sure `bash tools/run-backend.sh` is running) |
| `No response` | The reverse rule exists but nothing listens on the PC: `bash tools/run-backend.sh` |
| `Timed out` | The backend is stuck or slow: restart `bash tools/run-backend.sh` |
| `Plain HTTP blocked` | A release build, or the debug network security config is missing: install with `bash tools/install-debug.sh` |
