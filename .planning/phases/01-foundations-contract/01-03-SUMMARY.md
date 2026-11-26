---
phase: 01-foundations-contract
plan: 03
subsystem: dev-networking, backend-config, dev-tooling
status: complete
tags: [android, okhttp, adb-reverse, diagnostics, fastapi, pydantic-settings, secrets, readme]
requires:
  - "01-01: HealthClient / BackendStatusViewModel / BackendStatusRow, create_app(), tools/*.sh"
provides:
  - "app.palm.core.network.causeOf(Throwable) and HTTP/body checks in HealthClient.check"
  - "BackendStatusViewModel.retry() (single-flight) plus base URL line and Retry button in BackendStatusRow"
  - "backend/app/config.py BACKEND_DIR + Settings (absolute env_file, SecretStr key)"
  - "create_app(settings) and GET /healthz -> {status, anthropic_key: configured|missing}"
  - "smoke-phone.sh switches SMOKE_NO_REVERSE / SMOKE_NO_BACKEND / SMOKE_RETRY_EXPECT"
  - "tools/test_device.sh (stub-adb test of _device.sh), README.md"
affects: [02-*, 04-*, 06-*]
tech-stack:
  added: []
  patterns:
    - "Failure causes name the script that fixes them, shown on the phone instead of in Logcat"
    - "Settings injected into create_app(); tests pass _env_file=None and clear the OS env var"
    - "Shell scripts tested with a stub binary first on PATH"
key-files:
  created:
    - android/app/src/test/java/app/palm/core/network/HealthCauseTest.kt
    - backend/app/config.py
    - backend/tests/test_health.py
    - backend/.env.example
    - tools/test_device.sh
    - README.md
  modified:
    - android/app/src/main/java/app/palm/core/network/HealthClient.kt
    - android/app/src/main/java/app/palm/showcase/BackendStatus.kt
    - tools/smoke-phone.sh
    - backend/app/main.py
key-decisions:
  - "Backend stopped behind an existing adb reverse rule shows up as a clean close with zero bytes (OkHttp: 'unexpected end of stream' wrapping EOFException), checked on the Redmi with toybox nc; mapped to 'No response: start tools/run-backend.sh'"
  - "HealthClient now rethrows CancellationException and maps any other failure through causeOf(); a 200 with an unparsable body or status != ok reads 'Unexpected response'"
  - "MIUI unattended install works now (Remember my choice ticked): 4 installs this plan, no tap needed"
requirements-completed: [FND-04, FND-01]
duration: 7 min
started: 2026-11-26T10:49:26Z
completed: 2026-11-26
plan_head_before: dfd7b62fbfd321328f08f5bcb5c8f71a9128b2b1
plan_head_after: 9e3feb54563458baedcb9ed0af404fdff5d73983
actuals:
  tokens: 4570     # chars/4 over git diff dfd7b62..9e3feb5 (18,278 chars, 10 files)
  tasks: 2
  commits: 3
coverage:
  - deliverable: "No reverse rule: phone shows Backend: Unreachable, the refused cause naming tools/dev-reverse.sh, the base URL and Retry; Retry after repair turns it Connected without relaunch"
    human_judgment: false
    verification:
      - kind: command
        ref: 'SMOKE_NO_REVERSE=1 SMOKE_RETRY_EXPECT="Backend: Connected" bash tools/smoke-phone.sh "Backend: Unreachable" "Connection refused" "127\.0\.0\.1:8000" "Retry"'
        status: pass
  - deliverable: "Reverse rule present, backend stopped: Unreachable with No response cause"
    human_judgment: false
    verification:
      - kind: command
        ref: 'SMOKE_NO_BACKEND=1 bash tools/smoke-phone.sh "Backend: Unreachable" "No response"'
        status: pass
  - deliverable: "Default smoke path unchanged"
    human_judgment: false
    verification:
      - kind: command
        ref: 'bash tools/smoke-phone.sh "Backend: Connected"'
        status: pass
  - deliverable: "causeOf maps every exception type"
    human_judgment: false
    verification:
      - kind: test
        ref: "android/app/src/test/java/app/palm/core/network/HealthCauseTest.kt (tests=9 failures=0)"
        status: pass
  - deliverable: "Retry is single-flight"
    human_judgment: false
    verification:
      - kind: review
        ref: "BackendStatusViewModel.retry(): returns while probe?.isActive; called on the main thread only (backstop, no automated test)"
        status: pass
  - deliverable: "/healthz key status: missing for None/empty/blank, configured otherwise, exact key set, key never echoed, absolute env_file"
    human_judgment: false
    verification:
      - kind: test
        ref: "backend/tests/test_health.py (7 tests); whole suite uv run --directory backend pytest -q: 33 passed"
        status: pass
      - kind: command
        ref: "live server started via uv --directory backend, curl /healthz -> {\"status\":\"ok\",\"anthropic_key\":\"missing\"}"
        status: pass
  - deliverable: "Env example committed with empty key; real env file untracked and ignored"
    human_judgment: false
    verification:
      - kind: command
        ref: 'test "$(tr -d "\r" < backend/.env.example)" = "ANTHROPIC_API_KEY=" && test -z "$(git ls-files backend/.env)" && git check-ignore -q backend/.env'
        status: pass
  - deliverable: "_device.sh: 0, 1, 2, preset (and unknown preset) device cases"
    human_judgment: false
    verification:
      - kind: command
        ref: "bash tools/test_device.sh (5 ok lines)"
        status: pass
---

# Phase 1 Plan 03: Backend Reachability Diagnostics and Key Status Summary

**The status row on the phone now says why the backend is unreachable and which script fixes it (`Connection refused` -> dev-reverse.sh, `No response` -> run-backend.sh). It also shows `http://127.0.0.1:8000` and has a single-flight Retry button that recovers without a relaunch. `/healthz` reports `anthropic_key: configured|missing` from an absolute `backend/.env` path and never returns the key.**

## Performance

- Duration: about 7 min (2026-11-26T10:49:26Z to 10:56Z)
- Tasks: 2/2
- Files: 10 (6 created, 4 modified)

## Accomplishments

- `causeOf(Throwable)` in `HealthClient.kt` covers: refused, timeout, cleartext blocked, host not found, no response (EOF / end of stream / socket reset), and a `Network error: <Class>` fallback. `check()` maps non-200 to `HTTP <code>` and a bad body or `status != ok` to `Unexpected response`.
- `BackendStatusRow` always shows the status line, the cause when unreachable, `BuildConfig.BASE_URL` and an `OutlinedButton` "Retry". `retry()` ignores taps while a probe is active, and `init` runs the first probe through it.
- `smoke-phone.sh` has three new switches. `SMOKE_NO_REVERSE=1` removes the reverse rule. `SMOKE_NO_BACKEND=1` keeps the backend down and refuses to run if :8000 already answers. `SMOKE_RETRY_EXPECT` starts the backend if needed, runs dev-reverse.sh, taps the centre of the `Retry` line from the ui.py dump and waits up to 15 s. Backend startup moved into an `ensure_backend` function, and the default path behaves as before.
- Backend: `Settings` (pydantic-settings, `env_file=BACKEND_DIR / ".env"`, `SecretStr`), `create_app(settings)`, `/healthz` returning exactly `{status, anthropic_key}`. `backend/.env.example` holds `ANTHROPIC_API_KEY=` only.
- `tools/test_device.sh`: a stub `adb` first on PATH, with listings that include CRLF and an `offline` row. It covers 0, 1, 2, preset and unknown-preset cases.
- `README.md`: prerequisites, the one-command install (`bash tools/install-debug.sh`), the run table from SKELETON.md, the key setup and a table of Unreachable causes with their fixes.

## On-device findings

- No reverse rule: `ConnectException`, shown as `Connection refused: run tools/dev-reverse.sh and tools/run-backend.sh`.
- Reverse rule present, backend down: adbd accepts the connection and then closes it with zero bytes (`nc` printed nothing, rc=0). OkHttp raises `IOException("unexpected end of stream ...")` caused by `EOFException`, shown as `No response: start tools/run-backend.sh`.
- Final dump: `Backend: Unreachable` / `No response: start tools/run-backend.sh` / `http://127.0.0.1:8000` / `Retry (82,311)`.

## Task Commits

1. **Task 1: phone names the unreachable cause, Retry recovers** - `637409f` (feat)
2. **Task 2 RED: failing /healthz tests + stub-adb device test** - `a774399` (test)
3. **Task 2 GREEN: Settings, /healthz key status, env example, README** - `9e3feb5` (feat)

## TDD Gate Compliance

- RED `a774399`: `pytest tests/test_health.py` failed at collection with ImportError (`app.config` did not exist yet). `tools/test_device.sh` passed straight away, because it is a regression test of `_device.sh` behaviour that plan 01-01 already built.
- GREEN `9e3feb5`: 7 health tests pass, and the full suite has 33 passed.
- REFACTOR: none needed.

## Decisions Made

See `key-decisions` in the frontmatter. The MIUI blocker in STATE.md is resolved: every install in this plan (4 runs of `install-debug.sh`) went through unattended.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Correctness] HealthClient rethrows CancellationException**
- **Found during:** Task 1
- **Issue:** The 01-01 `catch (e: Exception)` swallowed coroutine cancellation.
- **Fix:** Rethrow `CancellationException`, and send everything else through `causeOf`.
- **Files modified:** android/app/src/main/java/app/palm/core/network/HealthClient.kt
- **Commit:** 637409f

**2. [Extra case] test_device.sh also covers an unknown preset ADB_SERIAL**
- One extra assertion ("unknown preset"), so it prints 5 ok lines (the plan asked for at least 4).

**3. Commit message wording**
- A PreToolUse secret guard blocked a commit message that contained the literal env file path. The message was reworded. Nothing was read and no file changed.

**Total deviations:** 3 minor. **Impact:** none.

## Issues Encountered

- The known `StarletteDeprecationWarning` from `fastapi.testclient` still appears. It is harmless, and the plan does not use warnings-as-errors.

## Known Stubs

None.

## User Setup Required

None. The Anthropic key is only needed from Phase 2 (`cp backend/.env.example backend/.env`).

## Self-Check: PASSED

All 6 created files and commits 637409f, a774399, 9e3feb5 verified present.
