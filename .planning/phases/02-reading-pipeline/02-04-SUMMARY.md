---
phase: 02-reading-pipeline
plan: 04
subsystem: api
tags: [safety, denylist, hindi, red-team, READ-07]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: PalmReader/ReadResult/assemble (02-01, 02-02), conftest ok_reply, FakeSdk, client_factory, post_job, poll
provides:
  - safety.py (normalise, RULES, hits, filter_llm, NEUTRAL en/hi)
  - PalmReader.read filters the model reply before assemble; ReadResult.safety_hits (defaulted field)
affects: [02-05, 02-07, 02-08, 02-09]

actuals:
  tokens: 4300    # chars/4 over the realized diff (17,013 chars)
  tasks: 2
  commits: 3
plan_head_before: fe44064307914e9fafdb5ce44dd0e06e93294800
plan_head_after: 0f2ab6e6e894631184a43d203a493480cdbac0e2

tech-stack:
  added: []
  patterns:
    - "Rules run on a normalised match copy (NFKC, casefold, Cf dropped, nukta removed, chandrabindu to anusvara); patterns go through the same normalise so stems fold like text"
    - "Devanagari rules use plain stems and a Devanagari-block lookbehind instead of \\b (A10)"

key-files:
  created:
    - backend/app/services/safety.py
    - backend/tests/test_safety.py
  modified:
    - backend/app/services/palm_reader.py
    - backend/tests/test_palm_reader.py

key-decisions:
  - "ReadResult.llm stays the unfiltered model output (eval enum agreement and prompt compliance); only the filtered reading reaches jobs and cache"
  - "A list field (traits, ideal_industries) with a flagged item counts as one rewritten field: flagged items dropped, refilled to 3 from NEUTRAL defaults without duplicates"
  - "Bare शेयर is not a Hindi directive trigger (it also means 'share your feelings'); only शेयरों / शेयर में / शेयर बाज़ार count, and the Hindi verb set uses चाहिए alone so 'लगाना चाहिए' is caught"
  - "NEUTRAL hi shipped with Task 1 so a hi reading never hits a missing-locale KeyError between commits"

requirements-completed: [READ-07]

metrics:
  duration: 4min
  completed: 2026-11-27
---

# Phase 2 Plan 04: Safety output filter Summary

NFKC/casefold denylist in English, Hindi (Devanagari) and Hinglish plus advice-only directive patterns, run over every prose field before assembly; a hit field is rewritten from neutral per-locale text and the job still succeeds.

## What was built

- `backend/app/services/safety.py`: `normalise`, `RULES` (rule id, compiled regex), `hits(text)` returning rule ids only, `NEUTRAL` (en, hi; one sentence per string field plus a 3-item default for `traits` and `ideal_industries`), `filter_llm(llm, locale) -> (LlmReading, count)` covering summary, traits, the love and career fields, ideal_industries and all five line interpretations.
- `PalmReader.read` calls `filter_llm` before `assemble` and returns the count in `ReadResult.safety_hits`.
- Tests: `test_death_prediction_is_rewritten` (reader level) and `tests/test_safety.py` (10 test functions, 62 cases): en / hi / Hindi-variant / Hinglish red-team hits, 14 clean strings (Finance, Medicine, life-line length sentences, "You diagnose problems quickly.", "a die-hard romantic", कमरे में, innocuous Hindi), rewrite with count 6, idempotency, zero hits in every NEUTRAL value, and a red-team reading through the job API in en and hi with zero hits over every string of the returned reading.

## Verification

- `uv run --directory backend pytest -q tests/test_palm_reader.py -k rewritten`: 1 passed
- `uv run --directory backend pytest -q tests/test_safety.py`: 62 passed; `-k redteam`: 24 passed
- `uv run --directory backend pytest -q`: 207 passed

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Hindi directive false positive on "share your feelings"**
- **Found during:** Task 2
- **Issue:** The planned pattern `(निवेश|शेयर|...).{0,30}(करें|...)` flags "अपनी भावनाएँ शेयर करें", a likely love-field sentence, and missed "शेयर बाज़ार में पैसा लगाना चाहिए".
- **Fix:** `शेयर` narrowed to `शेयर(ों| में| बाज़ार)`, verb set `(करें|चाहिए|कीजिए|मत)`; both sentences added to the tests.
- **Files modified:** backend/app/services/safety.py, backend/tests/test_safety.py
- **Commit:** 0f2ab6e

**2. [Rule 2 - Critical] Two extra English death/lifespan terms**
- **Found during:** Task 1
- **Issue:** "pass away" and "longevity" are common lifespan euphemisms not in the planned list.
- **Fix:** added to `en.death` / `en.lifespan`.
- **Commit:** dc746b5

## TDD Gate Compliance

Task 2: RED 99d9240 (41 failing), GREEN 0f2ab6e. No refactor commit needed.

## Known limits

- Ruff is not installed in the backend env, so lint was not run.
- Denylist is lexical: paraphrases outside the stems (for example a Hindi sentence about money with no listed noun) pass. The image-injection red-team and real-model compliance (`safety_hits`) are measured in 02-07..02-09.

## Self-Check: PASSED

- FOUND: backend/app/services/safety.py, backend/tests/test_safety.py
- FOUND: dc746b5, 99d9240, 0f2ab6e
