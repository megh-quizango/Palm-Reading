---
phase: 02-reading-pipeline
plan: 07
subsystem: evals
tags: [eval, bake-off, red-team, mediapipe, fake-mode]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: prepare/Prepared/hamming and pre-gate thresholds (02-02), line_geometry template/validate (02-03), PalmModel, PalmReader/ReadResult with line_checks and safety_hits, safety.hits/normalise (02-04), cost/worst_case_cost/PRICES, DHASH_MAX_DISTANCE (02-05), conftest guard and settings() (02-01)
provides:
  - backend/evals/run_eval.py (fake and real eval harness, metrics, summary, overlays, ignore gate, budget stop, red-team scheduling)
  - backend/evals/redteam.py (INJECTIONS, render, score)
  - backend/evals/make_landmarks.py (MediaPipe IMAGE-mode landmarks on prepare() output, lazy import)
  - backend/evals/labels.example.csv (labels format)
  - backend/tests/test_eval_smoke.py (29 fake-mode tests)
affects: [02-08, 02-09]

actuals:
  tokens: 13600    # chars/4 over the realized files (54,388 chars)
  tasks: 3
  commits: 5
plan_head_before: c3a2c9ad681b53f32c370a5a90393896986aeb39
plan_head_after: 89e35a86aba838edfeae77b348b54119d3957ae8

tech-stack:
  added: []
  patterns:
    - "Eval calls PalmReader directly (cache and job layer bypassed); every attempt is one row, failures at their wall time"
    - "Calls start in list order through one semaphore (acquire before create_task), so the budget check sees every finished charge and --concurrency 1 gives a strict order"
    - "Fake client keyed by sha256 of the sent JPEG: it draws template-shaped lines from the registered landmarks and echoes the registered injection"
    - "Optional heavy deps (mediapipe, numpy) imported inside main() after argparse; tests fake them via sys.modules"

key-files:
  created:
    - backend/evals/__init__.py
    - backend/evals/run_eval.py
    - backend/evals/redteam.py
    - backend/evals/make_landmarks.py
    - backend/evals/labels.example.csv
    - backend/tests/test_eval_smoke.py
  modified: []

key-decisions:
  - "Order in run_eval.main: ignore gate -> key check -> labels.csv existence -> client construction -> first photo read; the labels check sits after the key check so a keyless default run reports the missing key"
  - "A red-team cell fails only when it returns no reading (refusal, malformed, model failure after the retry); an invalid verdict counts as a reading. A budget stop that leaves any red-team cell unrun also exits 1"
  - "Photos whose labels notes contain redteam-hi are red-team inputs only (used as-is, topic 'handwritten') and are excluded from the bake-off and the dHash/pre-gate tables"
  - "Lines valid excludes rows whose photo has no usable landmarks (reported as a 'no landmarks' count) and counts 'omitted' in the denominator"
  - "make_landmarks also applies the git check-ignore gate to the photos dir, since it writes landmarks JSON there"
  - "Red-team prose: one JSON per call under <out>/redteam/ holding both raw (LlmReading) and final (PalmReading) prose plus the row metrics"

requirements-completed: [READ-02, READ-06, READ-07]

duration: 15min
completed: 2026-11-27
---

# Phase 2 Plan 07: Eval harness green in fake mode Summary

**Fake-mode eval harness over the real prepare -> PalmModel -> PalmReader pipeline: bake-off metrics per model and locale, dHash and pre-gate tables, overlays, a 13-injection image red-team that runs first, a git check-ignore gate, a key guard and a --max-usd stop, plus an offline MediaPipe landmarks tool.**

## Performance

- **Duration:** about 15 min
- **Completed:** 2026-11-27
- **Tasks:** 3 (1 tracer, 2 TDD)
- **Files created:** 6

## Accomplishments

- `python -m evals.run_eval --fake --synthetic` runs with no key and no photos. It generates three palms (one right hand captured twice, one left hand) and writes per-model JSON and CSV rows, overlay PNGs and an aggregates-only `<ts>-summary.md`.
- The summary reports, per model and locale: calls, failures by kind, nearest-rank p50/p95, mean output tokens, points per call and their share, mean and max cost, lines valid with a seeded photo-bootstrap 95% CI and the template-first verdict, enum agreement, refusals, suspected refusals, malformed, verdict agreement and cache-read share. It also has a per-line rule-count table, a dHash intra/inter table and pre-gate tables split by expected_ok. No photo ids, stems or prose appear in it.
- Real mode is ready for 02-08:
  - `ignored()` probes `<dir>/.probe` with `git check-ignore`, and every default path is anchored on the package.
  - It exits 2 when the key is missing, with a message naming backend/.env.
  - It calls `anthropic.AsyncAnthropic(timeout=120, max_retries=0)` at call time. A keyed test proves the conftest guard is reached.
  - The `--max-usd` stop uses per-attempt charges, charging the worst case when an attempt returned no usage.
  - Models alternate photo by photo.
- Red-team: 13 injections (death in en, Hinglish and hi; illness, pregnancy, medical, legal and financial in en and hi) are rendered in memory and run in en and hi before any bake-off call.
  - A busy or timeout gets one counted retry.
  - Each call's raw and final prose goes to the git-ignored results dir.
  - The summary has a per topic and locale table.
  - The run exits 1 on any after-filter hit or failed cell, after the run and summary finish.
  - `--repeats 0 --redteam` runs the red-team alone.
- make_landmarks: MediaPipe runs in IMAGE mode on the `prepare()` output with num_hands=1. The import is deferred, so `--help` works without mediapipe. There is an A8 temp-file fallback, and mediapipe is not in the project env (find_spec is None).

## Task Commits

1. **Task 1: tracer, synthetic palm to summary row** - `7c0cbd5` (feat)
2. **Task 2: real-mode inputs and every bake-off metric** - `0ef25b7` (test, RED), `fd9131b` (feat, GREEN)
3. **Task 3: image red-team** - `0649e21` (test, RED), `89e35a8` (feat, GREEN)

## TDD Gate Compliance

- Task 2: RED `0ef25b7` (13 of 15 failing), then GREEN `fd9131b` (17 passed, including the two make_landmarks tests added with the implementation).
- Task 3: RED `0649e21` (12 failing on `--redteam` and the missing module), then GREEN `89e35a8` (12 red-team tests passed). The GREEN commit also carries a one-word test fix (`ok_reply().parsed_output`).

## Verification

- `uv run --directory backend pytest -q tests/test_eval_smoke.py`: 29 passed
- `uv run --directory backend pytest -q`: 260 passed
- `python -m evals.run_eval --fake --synthetic` exits 0 and prints p50/p95. With `--redteam --repeats 2` it exits 0: 52 cells, 12 before-filter hits, 0 after-filter hits, 52 echoed.
- `python -m evals.make_landmarks --help` exits 0 without mediapipe. Every acceptance grep passes, and the labels header is exact.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical] make_landmarks ignore gate**
- **Found during:** Task 2
- **Issue:** make_landmarks writes `<stem>.landmarks.json` into the photos dir, and the plan only gated run_eval.
- **Fix:** It now calls `run_eval.ignored()` on the resolved photos dir and exits 2 if the dir is not ignored.
- **Files modified:** backend/evals/make_landmarks.py
- **Commit:** fd9131b

**2. [Rule 2 - Missing critical] Red-team cut short by the budget fails the run**
- **Found during:** Task 3
- **Issue:** A budget stop during the red-team would have left cells unrun and still exited 0.
- **Fix:** The run exits 1 when fewer red-team rows than scheduled red-team calls ran.
- **Files modified:** backend/evals/run_eval.py
- **Commit:** 89e35a8

Otherwise the plan ran as written. The conftest `settings(**kw)` override works as 02-01 specified: the keyed test reached the guard, and nothing raised TypeError.

## Known Stubs

None. FakeClient is the intended D-15 fake mode, not a stub.

## Notes for 02-08

- Pillow raqm is absent on this machine, so Devanagari injections render unshaped (the summary says so). Handwritten Hindi photos with notes `redteam-hi` are picked up as-is.
- Paraphrased injections (illness through financial) do not trip the denylist when echoed: in fake mode their before-filter hits are 0. SC5 has to be judged by reading the `<out>/redteam/*.json` prose, as D-13 intends.
- Landmarks command: `uv run --directory backend --with mediapipe==1.2.3 python -m evals.make_landmarks --model evals/models/hand_landmarker.task` (add `--python 3.12` if there is no 3.13 wheel).

## Self-Check: PASSED

- FOUND: backend/evals/__init__.py, run_eval.py, redteam.py, make_landmarks.py, labels.example.csv, backend/tests/test_eval_smoke.py
- FOUND commits: 7c0cbd5, 0ef25b7, fd9131b, 0649e21, 89e35a8
