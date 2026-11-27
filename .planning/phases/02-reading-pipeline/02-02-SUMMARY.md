---
phase: 02-reading-pipeline
plan: 02
subsystem: api
tags: [geometry, palm-lines, templates, validation]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: assemble/PalmReader/ReadResult, conftest LANDMARKS, LANDMARKS_LEFT, ok_reply, FakeSdk (02-01)
provides:
  - line_geometry.py (Palm, palm_from_landmarks, validate, pair_ok, template, TEMPLATES, all thresholds as named constants)
  - assemble resolves every line through line_geometry and returns (reading, line_checks)
  - ReadResult.line_checks (defaulted field) and palm_reader.TEMPLATE_FIRST (D-11 switch)
affects: [02-04, 02-07, 02-09, 05, 06]

actuals:
  tokens: 4000    # chars/4 over the realized diff
  tasks: 2
  commits: 3
plan_head_before: 42a723e6094f4f2520aa34dd1ffd380d09508395
plan_head_after: cada838daffb18907f8fb58e02f5208fe691f1f4

tech-stack:
  added: []
  patterns:
    - "Geometry in pixel space, in a hand-local (s, t) frame: origin wrist, s toward middle MCP in units of L, +t toward the pinky"
    - "Tests build curves in (s, t) and map them with Palm.world, so they do not depend on hand-picked pixels"

key-files:
  created:
    - backend/app/services/line_geometry.py
    - backend/tests/test_line_geometry.py
    - backend/tests/test_palm_reader.py
  modified:
    - backend/app/services/palm_reader.py

key-decisions:
  - "assemble now returns (PalmReading, line_checks); its only caller is PalmReader.read, so no wrapper was kept"
  - "TEMPLATES tuned from RESEARCH Pattern 7 (A3): the start values for heart, head, life, fate and sun put points outside the landmark hull, which has no hypothenar point; the tuned table keeps start->end order and the palmistry layout"
  - "An optional line the model marked present with no points also gets a template and line_checks omitted (same as a required line)"
  - "With degenerate landmarks no line is validated, so line_checks has only omitted entries"

requirements-completed: [READ-06]

coverage:
  - id: D1
    description: "An off-palm LLM heart line comes back as a template inside the palm with trace_source template; valid lines keep llm"
    requirement: READ-06
    verification:
      - kind: unit
        ref: "backend/tests/test_palm_reader.py#test_off_palm_heart_becomes_template"
        status: pass
  - id: D2
    description: "validate rules count, hull, length, folds (with consecutive-duplicate dropping) and pair_ok"
    requirement: READ-06
    verification:
      - kind: unit
        ref: "backend/tests/test_line_geometry.py"
        status: pass
  - id: D3
    description: "Templates inside the hull for right and left fixture hands; left templates mirror right ones"
    requirement: READ-06
    verification:
      - kind: unit
        ref: "backend/tests/test_line_geometry.py#test_templates_inside_hull, test_left_template_mirrors_right"
        status: pass
  - id: D4
    description: "Required lines always present, absent fate stays absent, swapped heart/head both templates, degenerate landmarks give empty traces, TEMPLATE_FIRST switch"
    requirement: READ-06
    verification:
      - kind: unit
        ref: "backend/tests/test_palm_reader.py -k 'absent or degenerate or swapped or template_first'"
        status: pass

metrics:
  duration: 4min
  completed: 2026-11-27
---

# Phase 2 Plan 02: Line geometry and templates Summary

Pure-Python palm geometry (`line_geometry.py`, `math` only) checks each model line against the landmark hull in a hand-local frame and swaps any failing line for a landmark-anchored template, with `trace_source` and a per-line `line_checks` result for the eval, plus the `TEMPLATE_FIRST` switch for D-11.

## Accomplishments

- `palm_from_landmarks` builds the expanded convex hull of landmarks 0, 1, 2, 5, 9, 13, 17 (factor `HULL_EXPAND` 1.15) and the frame; returns None for non-finite values, L under `MIN_L_PX` (20 px) or a collinear palm.
- `validate` drops exact consecutive duplicates, then returns the first failing rule: `count` (4-24), `hull` (80% inside), `length` (0.25-2.2 L), `folds` (more than one turn over 150 degrees), else `pass`. Every threshold is a named constant (`MIN_POINTS`, `MAX_POINTS`, `MIN_INSIDE`, `MIN_LEN`, `MAX_LEN`, `MAX_FOLD_DEG`, `MAX_FOLDS`, `HULL_EXPAND`, `PAIR_MIN_GAP`).
- `pair_ok`: heart mean s minus head mean s >= `PAIR_MIN_GAP`; on failure both lines become templates with `line_checks` `pair`.
- `assemble`: heart, head and life always present; a line the model left absent or empty gets a template and `omitted`; absent fate and sun stay `present=false`, `[]`, `none`; no Palm gives `[]`/`none`; normalisation to 0..1 (round 4, clamp) happens after the geometry check.
- Left hands mirror through the pinky-side sign of the frame, with no handedness label.

## Task Commits

1. **Task 1: tracer, off-palm heart becomes a template** - `195e8ea` (feat)
2. **Task 2: every D-10 rule, required lines, mirror, template-first (TDD)** - RED `5c77556` (test), GREEN `cada838` (feat)

## TDD Gate Compliance

RED commit `5c77556` added 12 geometry tests (19 cases) and 5 reader tests; 10 failed (missing rules, `pair_ok`, constants, `TEMPLATE_FIRST`, omitted handling). GREEN commit `cada838` made all 32 pass. No refactor commit was needed.

## Verification

- `pytest -q tests/test_palm_reader.py` (Task 1): 1 passed; full suite 81 passed. Tracer gate: verify passed end to end before expansion.
- `pytest -q tests/test_line_geometry.py tests/test_palm_reader.py`: 32 passed.
- `pytest -q` full suite: 112 passed.
- `pytest -q tests/test_palm_reader.py -k "absent or degenerate or swapped or template_first"`: 5 passed.
- `grep -c 'def test_' tests/test_line_geometry.py`: 12.
- Importing `line_geometry` pulls in neither PIL nor numpy.

## Deviations from Plan

1. **[Planned option, A3] TEMPLATES retuned.** The RESEARCH start table put the heart start (t 0.62), sun (t 0.32 at s 0.34), head/life start (t -0.50) and life end outside the fixture hull, because the hull has no hypothenar landmark. Values were pulled inside with the same layout and order. Plan 02-09 retunes from real eval overlays.
2. **[Rule 1-style simplification] `assemble` returns a tuple.** The plan's interface kept `assemble(...) -> PalmReading`; it now returns `(PalmReading, line_checks)`. Its only caller is `PalmReader.read` (checked, including 02-03..02-09 plans, which only use `PalmReader.read` and `ReadResult`).

## Known Stubs

None.

## Next Phase Readiness

02-07's eval can break lines-valid down by `ReadResult.line_checks` values and flip `TEMPLATE_FIRST`; 02-09 can retune every constant and `TEMPLATES` without test edits (tests pin `TEMPLATE_FIRST` where they depend on it).

## Self-Check: PASSED

All created files exist; commits 195e8ea, 5c77556 and cada838 are ancestors of HEAD.
