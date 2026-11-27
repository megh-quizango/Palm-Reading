---
phase: 02-reading-pipeline
plan: 06
subsystem: api
tags: [privacy, logging, tempfile, pillow, anthropic-sdk]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: MultiPartParser.spool_max_size fix (02-01); JobService stores, TASKS, conftest FakeSdk/ok_reply/client_factory/post_job/poll and LANDMARKS marker 0.123457 (02-01..02-05)
provides:
  - backend/tests/test_privacy.py (disk, log and app-store privacy proofs for READ-05 / D-08)
  - anthropic and PIL loggers pinned to WARNING at import time (claude_client.py, image_prep.py)
  - README warning against DEBUG on the anthropic / PIL loggers
affects: [02-07, 02-08, 02-09]

actuals:
  tokens: 2000    # chars/4 over the realized diff (7,935 chars)
  tasks: 2
  commits: 3
plan_head_before: 3e49fd7c7ef5b3d87b238426c8617e1e854d51eb
plan_head_after: 002aeee075a27d59a94027dc999c3a2dea965dbb

tech-stack:
  added: []
  patterns:
    - "Privacy store walk: descend only into containers and objects whose type lives in the app package; test doubles and asyncio primitives are leaves"
    - "Third-party loggers that can dump request bodies or EXIF are pinned to WARNING where the app imports the library"

key-files:
  created:
    - backend/tests/test_privacy.py
  modified:
    - backend/app/services/claude_client.py
    - backend/app/services/image_prep.py
    - README.md

key-decisions:
  - "anthropic logger pin sits right after the claude_client import block (not between imports) to keep the block E402-clean; it still runs after the SDK's import-time ANTHROPIC_LOG setup, so it overrides ANTHROPIC_LOG=debug (checked with ANTHROPIC_LOG=debug plus basicConfig(DEBUG): both loggers report isEnabledFor(DEBUG) False)"
  - "The TASKS check compares against a snapshot taken at test start, so a task left by an unrelated earlier test cannot make this test order-dependent"

requirements-completed: [READ-05]

coverage:
  - id: D1
    description: "A 1.5 MB photo never rolls over to a temp file, and temp dir and cwd stay empty after success, model_error and timeout"
    requirement: READ-05
    verification:
      - kind: integration
        ref: "backend/tests/test_privacy.py#test_no_disk_after_success_failure_timeout"
        status: pass
  - id: D2
    description: "Root DEBUG cannot enable the anthropic or PIL loggers"
    requirement: READ-05
    verification:
      - kind: unit
        ref: "backend/tests/test_privacy.py#test_sdk_and_pil_loggers_never_debug"
        status: pass
  - id: D3
    description: "DEBUG logs after ok, model_error, timeout, a 422 and an EXIF photo hold no image bytes, base64, JPEG markers or landmark marker, and no anthropic/PIL records"
    requirement: READ-05
    verification:
      - kind: integration
        ref: "backend/tests/test_privacy.py#test_no_image_or_landmarks_in_logs"
        status: pass
  - id: D4
    description: "No app-held store holds bytes, base64 or landmarks after those outcomes; no job task lingers"
    requirement: READ-05
    verification:
      - kind: integration
        ref: "backend/tests/test_privacy.py#test_no_image_or_landmarks_in_stores"
        status: pass

duration: 2min
completed: 2026-11-27
---

# Phase 2 Plan 6: Privacy proof (READ-05) Summary

**Tests prove a 1.5 MB palm photo never spools to disk and never reaches logs or app-held stores across success, model_error, timeout, a 422 and an EXIF photo; the anthropic and PIL loggers are pinned to WARNING so no logging config can dump the base64 photo or GPS EXIF.**

## Performance

- Duration: about 2 min
- Started: 2026-11-27T05:23:45Z
- Completed: 2026-11-27T05:25:34Z
- Tasks: 2
- Files: 1 created, 3 modified

## Accomplishments

- Task 1 (tracer): `test_no_disk_after_success_failure_timeout` patches `SpooledTemporaryFile.rollover` to raise, points `tempfile.tempdir` and cwd at an empty `tmp_path`, runs the three outcomes on a 1.5 MB random-noise JPEG and asserts the directory stays empty. It passed on the 02-01 spool fix; a one-off run with `spool_max_size` reset to 1 MB fails with "spooled to disk", so the test has teeth.
- Task 2 (TDD): three tests written first. RED: the logger test failed (both loggers inherited root DEBUG) and the log scan failed (`PIL.TiffImagePlugin` DEBUG records while parsing the EXIF photo). The store walk already passed: jobs.py drops the image and landmark references, and Job never held landmarks. GREEN: two one-line logger pins plus the README warning. Full suite: 231 passed.

## Task Commits

1. Task 1: no-disk tracer test - `3502a22` (test)
2. Task 2 RED: failing logger, log-scan and store tests - `32f2750` (test)
3. Task 2 GREEN: logger pins + README warning - `002aeee` (fix)

## Deviations from Plan

None. jobs.py needed no change (no retained reference was found).

## Issues Encountered

- `ruff` is not installed in the backend env, so lint was not run (installing it would be a new download).
- The tracer feedback gate used the pre-commit verify run; nothing changed between that run and the commit.

## Next Phase Readiness

- 02-07 (eval) imports claude_client and image_prep, so it gets the logger pins automatically.

## Self-Check: PASSED

- FOUND: backend/tests/test_privacy.py, backend/app/services/claude_client.py, backend/app/services/image_prep.py, README.md
- FOUND commits: 3502a22, 32f2750, 002aeee
