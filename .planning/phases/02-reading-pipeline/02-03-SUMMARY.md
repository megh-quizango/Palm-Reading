---
phase: 02-reading-pipeline
plan: 03
subsystem: api
tags: [anthropic, errors, retry, idempotency, asyncio]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: PalmModel.read_once, PalmReader.read/ReadResult, JobService, FakeSdk/client_factory/post_job/poll (02-01)
provides:
  - claude_client Malformed / ModelFailure(code, billable) / Refused(usage, model) and the SDK result + exception mapping in read_once
  - jobs RETRY_MIN_LEFT_S (15 s) retry-once policy, D-01 failure codes, JOB_TTL_S (600 s) lazy sweep
  - JobService.submit(..., *, install_id, idem_key=None) with per-install idempotency and single-flight maps
  - Optional Idempotency-Key header (UUID) on POST /v1/palm/jobs
  - conftest sdk_error (real 2.14.0 exception constructors) and reply_with helpers
affects: [02-05, 02-06, 02-07, 06]

actuals:
  tokens: 3700    # chars/4 over the realized diff
  tasks: 3
  commits: 5
plan_head_before: e185f553f343c0841f952c3d9adba11c52fbd2e6
plan_head_after: ebb480be028581c155899c18c919b583067bbfc4

tech-stack:
  added: []
  patterns:
    - "SDK exceptions are wrapped with `raise ModelFailure(...) from e`, so the failure log names the original class (e.g. RateLimitError) without its message"
    - "Malformed retries are decided inside the asyncio.timeout block from `deadline.when() - loop.time()`"

key-files:
  created:
    - backend/tests/test_job_failures.py
  modified:
    - backend/app/services/claude_client.py
    - backend/app/jobs.py
    - backend/app/main.py
    - backend/tests/conftest.py
    - backend/tests/test_claude_client.py
    - backend/tests/test_jobs_api.py

key-decisions:
  - "A second POST that single-flights onto a running job also maps its own Idempotency-Key to that job, so its retries return the same id"
  - "Failure log line is `job_failed job_id=... code=... error=<class of __cause__ or the exception>`; never the message"
  - "OverloadedError (529) is covered by the InternalServerError branch (it subclasses it in 2.14.0) and has its own test"

requirements-completed: [READ-02]

coverage:
  - id: D1
    description: "Malformed output (parse ValidationError, max_tokens, no parsed output, contract ValidationError) retried once; second malformed -> model_error; no retry with < RETRY_MIN_LEFT_S left -> timeout"
    requirement: READ-02
    verification:
      - kind: integration
        ref: "backend/tests/test_job_failures.py#test_malformed_once_then_ok, test_malformed_twice_is_model_error, test_max_tokens_cut_is_retried, test_no_retry_without_deadline_left_is_timeout"
        status: pass
  - id: D2
    description: "Refusal -> model_error after 1 call; rate limit/5xx/529/connection -> busy; SDK timeout and 45 s deadline -> timeout; 400/401/404 and unexpected exceptions -> model_error"
    requirement: READ-02
    verification:
      - kind: integration
        ref: "backend/tests/test_job_failures.py"
        status: pass
      - kind: unit
        ref: "backend/tests/test_claude_client.py#test_sdk_errors_map_to_codes, test_refusal_raises_refused_with_usage, test_cut_or_unparsed_reply_is_malformed_with_usage, test_sdk_exception_classes_exist"
        status: pass
  - id: D3
    description: "Failure logs carry job id, code and class name, never the exception message"
    verification:
      - kind: integration
        ref: "backend/tests/test_job_failures.py#test_unexpected_exception_logs_no_message"
        status: pass
  - id: D4
    description: "Same install + Idempotency-Key -> same job, 1 model call; identical in-flight bytes per install/hand/locale share a job; other installs and hands do not; jobs 404 after 10 min and their key is released"
    requirement: READ-02
    verification:
      - kind: integration
        ref: "backend/tests/test_jobs_api.py#test_idempotent_retry_returns_same_job, test_idempotency_key_must_be_uuid, test_single_flight_*, test_expired_job_is_404_and_key_is_released"
        status: pass

metrics:
  duration: 3min
  completed: 2026-11-27
---

# Phase 2 Plan 03: Failure mapping, retry and idempotency Summary

Every model failure now ends a job with a D-01 code: malformed output gets one identical retry when at least 15 s of the deadline remain, refusals and SDK status errors end as `model_error`, throttling and connection errors as `busy`, SDK timeouts and the overall deadline as `timeout`; a retried POST with the same install id and `Idempotency-Key` (or identical in-flight bytes from the same install, hand and locale) returns the same job, and jobs are swept lazily after 10 minutes.

## Task Commits

1. **Task 1 (tracer): malformed retried once, then model_error** - `e632260` (feat). Tracer gate: verify re-run on the committed state end to end, passed.
2. **Task 2 (TDD): refusals, SDK errors, deadlines, log hygiene** - RED `ea27dec` (test), GREEN `af2466e` (feat)
3. **Task 3 (TDD): idempotency, single-flight, expiry** - RED `5993f0a` (test), GREEN `ebb480b` (feat)

## TDD Gate Compliance

- Task 2: RED `ea27dec` had 16 failing tests (mapping, refusal, max_tokens, busy, SDK timeout); 11 already passed because Task 1's runner already mapped TimeoutError, unexpected exceptions and the retry-deadline rule. GREEN `af2466e`: 27 passed in the two files.
- Task 3: RED `5993f0a` had 4 failing (idempotent retry, key validation, same-install single-flight, expiry); the cross-install and other-hand tests passed at RED because every POST made a new job then, and they guard against over-sharing. GREEN `ebb480b`: 6 passed. No refactor commits.

## Verification

- `pytest -q tests/test_job_failures.py tests/test_claude_client.py tests/test_jobs_api.py`: all pass (run per task)
- `pytest -q` full suite: 143 passed
- `-k "malformed"`: 2 passed; `-k "refusal or busy or timeout or unexpected"`: 9 passed; `-k "idempot or single_flight or expire"`: 6 passed
- No real Anthropic client: all SDK errors are built from the installed 2.14.0 classes and raised by FakeSdk

## Deviations from Plan

**1. [Rule 3 - Blocking] Shared test helpers added to conftest.py (not in files_modified)**
- **Found during:** Task 2
- **Issue:** both test_claude_client.py and test_job_failures.py need SDK exceptions built with the installed constructors and replies with a changed stop_reason.
- **Fix:** `sdk_error(cls, status=None)` (uses `httpx2`, the SDK's transport) and `reply_with(**kw)` in conftest.
- **Commit:** `ea27dec`

**2. Scope additions inside the plan's rules:** an OverloadedError (529) case in the busy tests and the mapping test; `test_malformed_once_then_ok` also asserts the retry is byte-identical to the first call (anti-pattern: no error text appended).

`ruff` is not installed in the backend venv (it is a listed dev tool, but new downloads are not approved this session), so lint was not run.

## Known Stubs

None.

## Next Phase Readiness

02-05 can bill attempts from `Malformed.usage/model`, `Refused.usage/model` and `ModelFailure.billable`; 02-07 can count refusals via `isinstance(e, Refused)`. The Phase 6 client gets only `busy`, `timeout`, `model_error`, `bad_request`, `not_found`.

## Self-Check: PASSED

All key files exist; commits e632260, ea27dec, af2466e, 5993f0a and ebb480b are ancestors of HEAD.
