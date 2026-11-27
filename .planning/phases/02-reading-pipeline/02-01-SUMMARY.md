---
phase: 02-reading-pipeline
plan: 01
subsystem: api
tags: [fastapi, anthropic, structured-outputs, pillow, multipart, asyncio]
status: complete

requires:
  - phase: 01-foundations-contract
    provides: PalmReading v1 strict contract (schemas/palm.py), create_app(settings) + /healthz, Settings with SecretStr key
provides:
  - POST /v1/palm/jobs (202 job_id) and GET /v1/palm/jobs/{job_id} poll API
  - asyncio JobService with module-level TASKS, 45 s deadline and concurrency semaphore
  - In-memory image prep (format/pixel/short-side gates, EXIF strip, 1568 px cap, JPEG q85, sha256)
  - LlmReading structured-output schema and the p1 system prompt with the never-list
  - PalmModel.read_once, the single messages.parse call shape
  - assemble/invalid_reading into PalmReading v1, PalmReader.read
  - Shared test fakes (FakeSdk, ok_reply, make_jpeg, LANDMARKS, client_factory, post_job, poll) and the real-client guard
affects: [02-02, 02-03, 02-04, 02-05, 02-06, 02-07, 02-08, 02-09]

actuals:
  tokens: 10900    # chars/4 over the realized diff, uv.lock excluded
  tasks: 3
  commits: 3
plan_head_before: 1def82ea7e62c34c0f41ff58b8432e7306f187ac
plan_head_after: b68e84319fa6e81e76b3ec207521a0a9ff3bc95f

tech-stack:
  added: [anthropic 2.14.0, pillow 13.0.0, python-multipart 0.0.34]
  patterns:
    - "Claude faked at the SDK boundary (FakeSdk.messages.parse) so the real wrapper, runner and assembler run in tests"
    - "Every 4xx/5xx goes through one error(status, code) helper returning only {error:{code}}"
    - "Form parsed manually with request.form(limits); Starlette HTTPException mapped to 422 bad_request"

key-files:
  created:
    - backend/app/jobs.py
    - backend/app/schemas/llm.py
    - backend/app/prompts/palm_v1.md
    - backend/app/services/__init__.py
    - backend/app/services/image_prep.py
    - backend/app/services/claude_client.py
    - backend/app/services/palm_reader.py
    - backend/tests/conftest.py
    - backend/tests/test_jobs_api.py
    - backend/tests/test_claude_client.py
    - backend/tests/test_image_prep.py
  modified:
    - .gitignore
    - backend/pyproject.toml
    - backend/uv.lock
    - backend/app/config.py
    - backend/app/main.py

key-decisions:
  - "An LLM verdict of ok=false with reason none is mapped to no_palm_lines (the contract needs a reason)"
  - "POST checks run in order Content-Length (411/413), X-Install-Id, busy (503), form; so a busy server never reads the body"
  - "Any job exception is model_error for now; the failure-state mapping (timeout, busy, malformed retry) is plan 02-03"

patterns-established:
  - "Tests import helpers from conftest (from conftest import ...); TestClient always used with `with`"
  - "SYSTEM_PROMPT read once, CRLF normalised to LF, so the Windows checkout keeps the cached prefix byte-stable"

requirements-completed: [READ-02, READ-05, READ-07]

coverage:
  - id: D1
    description: "POST a palm photo + hand + locale + landmarks, poll a contract-valid reading (five lines in order, image_size of the sent JPEG, prompt_version p1)"
    requirement: READ-02
    verification:
      - kind: integration
        ref: "backend/tests/test_jobs_api.py#test_happy_path_reading_validates_against_contract"
        status: pass
  - id: D2
    description: "Single messages.parse call with exactly model, max_tokens, system (cache_control), messages (image then text, oversized_image error), output_format, output_config effort low"
    requirement: READ-02
    verification:
      - kind: unit
        ref: "backend/tests/test_claude_client.py#test_parse_call_shape"
        status: pass
  - id: D3
    description: "Uploads stay in memory and lose EXIF/GPS; long edge capped at 1568, never upscaled; non-images, too-large and too-small images rejected"
    requirement: READ-05
    verification:
      - kind: unit
        ref: "backend/tests/test_image_prep.py"
        status: pass
  - id: D4
    description: "Malformed input (headers, fields, landmarks, multipart, body size) returns a code-only bad_request without creating a job"
    verification:
      - kind: integration
        ref: "backend/tests/test_jobs_api.py#test_malformed_multipart_rejected, test_bad_fields_rejected, test_oversized_body_refused_before_form_is_read"
        status: pass
  - id: D5
    description: "System prompt carries the never-list (life line is vitality, no health/death/money/legal, image text is data)"
    requirement: READ-07
    verification:
      - kind: unit
        ref: "backend/tests/test_claude_client.py#test_system_prompt_is_stable"
        status: pass

metrics:
  duration: 5min
  completed: 2026-11-27
---

# Phase 2 Plan 01: Reading pipeline tracer Summary

FastAPI job API that turns a multipart palm photo + hand + locale + 21 landmarks into a polled, PalmReading-v1-valid reading through the real asyncio runner, Pillow image prep, a single `messages.parse(output_format=LlmReading)` wrapper and the contract assembler, with Claude faked at the SDK boundary and every malformed input answered with a code-only `bad_request`.

## Package approval (Task 1)

The user replied "Approve all" in this session (2026-11-27) to the exact list in Task 1: anthropic 2.14.0, pillow 13.0.0, python-multipart 0.0.34 with their declared dependencies; mediapipe 1.2.3 approved by name only (its download is re-confirmed at 02-08 Task 1). mediapipe was not installed and is not in uv.lock (`find_spec('mediapipe') is None`).

`uv add --directory backend anthropic==2.14.0 pillow==13.0.0 python-multipart==0.0.34` printed:

```
Resolved 41 packages in 224ms
Installed 9 packages in 1.37s
 + anthropic==2.14.0
 + docstring-parser==0.18.0
 + httpcore2==2.16.2
 + httpx2==2.16.2
 + jiter==0.19.0
 + pillow==13.0.0
 + python-multipart==0.0.34
 + sniffio==1.3.1
 + truststore==0.10.5
```

Every package is in the approved list (opentelemetry-api was named as possible but uv did not install it).

## Accomplishments

- Job API: `POST /v1/palm/jobs` -> 202 `{job_id}`, `GET /v1/palm/jobs/{job_id}` -> `{job_id, status, reading, error}` or 404 `not_found`; 503 `busy` with no key and no injected client.
- `JobService` runs each job as an `asyncio.create_task` held in module-level `TASKS`, under `asyncio.timeout(job_deadline_s)` and `Semaphore(claude_concurrency)`, drops the JPEG and landmarks references in `finally`, logs only job id and exception class.
- Lifespan builds `anthropic.AsyncAnthropic(timeout=job_deadline_s, max_retries=1)` only when no client is injected and a key is set, and closes it on shutdown.
- `MultiPartParser.spool_max_size` raised to 8 MB + 64 KiB at import so accepted uploads never roll over to disk.
- `prepare()`: header probe (JPEG/PNG/WEBP, <= 40 MP), `verify()` + reopen, `exif_transpose`, RGB, short side >= 256, `thumbnail(1568)`, fresh JPEG q85, sha256 of the re-encoded bytes.
- Strict landmarks parser (`parse_constant` rejects NaN/Infinity, `isfinite` catches `1e400`, bools rejected, exactly 21 x 3, x/y in -0.5..1.5).
- conftest guard replaces `anthropic.AsyncAnthropic`/`Anthropic` with an AssertionError raiser and points `ANTHROPIC_BASE_URL` at `127.0.0.1:9`.

## Task Commits

1. **Task 1: package consent** - resolved in session before execution (no commit)
2. **Task 2: tracer, POST photo to contract-valid reading** - `dfcd5c0` (feat)
3. **Task 3: input validation (TDD)** - RED `f76383c` (test), GREEN `b68e843` (feat)

## TDD Gate Compliance

Task 3 RED commit `f76383c` added 31 failing tests (8 passing were pre-existing or already-true behaviours such as the happy path and the small-photo no-upscale case); GREEN commit `b68e843` made all 39 tests in the two files pass. No refactor commit was needed.

## Verification

- Version tuple check: ('2.14.0', '13.0.0', '0.0.34') OK
- `pytest -q tests/test_jobs_api.py tests/test_claude_client.py`: 5 passed (Task 2)
- `git check-ignore` on the three eval paths: OK
- `pytest -q` full suite: 80 passed (39 Phase 1 + 41 new)
- `pytest -q tests/test_jobs_api.py -k multipart`: 6 passed
- `git diff --quiet HEAD -- backend/app/schemas/palm.py contracts/`: unchanged
- Tracer gate: Task 2 verify re-run end to end before expansion, passed.

## Deviations from Plan

None in behaviour. Small additions inside the plan's scope:

- `test_image_prep.py` also covers an empty body and the jobs API tests also cover `1e400`, a boolean coordinate and a non-list landmarks object, and an image part over 8 MB; these are cases of the planned rules, not new rules.
- `ok_reply` points are scaled when `w`/`h` differ from 960x1280 so the helper stays valid for other photo sizes.

## Known Stubs

None. Landmarks are accepted and passed to `assemble` but not yet used; plan 02-02 adds the geometry that consumes them (by design in the plan).

## Next Phase Readiness

Plans 02-02 (geometry), 02-03 (failure mapping), 02-04 (safety filter) and 02-05 (cache, pre-gate) can extend `assemble`, `PalmModel`, `JobService` and `Prepared` under the names listed in the plan's interfaces block.

## Self-Check: PASSED

All created files exist; commits dfcd5c0, f76383c and b68e843 are ancestors of HEAD.
