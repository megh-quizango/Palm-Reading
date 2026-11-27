---
phase: 02-reading-pipeline
plan: 05
subsystem: api
tags: [cache, dhash, pre-gate, rate-limit, spend-cap, pillow]
status: complete

requires:
  - phase: 02-reading-pipeline
    provides: JobService.submit with idempotency + single-flight, Malformed/Refused usage+model, ModelFailure.billable (02-03); invalid_reading (02-01)
provides:
  - image_prep dhash, hamming, laplacian_var, PREGATE_MIN_MEAN (35), PREGATE_MIN_LAPLACIAN_VAR (15); Prepared.dhash/laplacian_var/brightness
  - jobs DHASH_MAX_DISTANCE (6), CACHE_TTL_S (86400), CACHE_MAX (2000), Busy(retry_after), today (patchable), JobService.cache / admitted / ledger
  - claude_client PRICES, cost(model, usage), MAX_TOKENS (8000), WORST_CASE_INPUT_TOKENS (6000), worst_case_cost(model)
  - config rate_limit_10min (5), rate_limit_day (20), daily_spend_cap_usd (5.0, env DAILY_SPEND_CAP_USD)
  - POST /v1/palm/jobs 429 + Retry-After + {error:{code:busy}}
  - conftest jpeg_variant(i) (new sha256, same dHash)
affects: [02-06, 02-07, 02-08, 02-09]

actuals:
  tokens: 7300    # chars/4 over the realized diff (29,284 chars)
  tasks: 3
  commits: 5
plan_head_before: b5dbad98c86c477ffb44776b3e301a5aa2a8d9d0
plan_head_after: 20ad385d418fe850b77aca7996172dd6cc5aa6bb

tech-stack:
  added: []
  patterns:
    - "submit order: idempotency -> result cache -> single-flight -> pre-gate -> admit (rate windows + spend cap) -> spawn; only the last step counts or costs"
    - "Each model attempt runs through JobService._read, whose finally charges the ledger, so a deadline cancellation is charged too"
    - "Tests read thresholds via module constants (jobs.DHASH_MAX_DISTANCE, ip.PREGATE_*), never literals"

key-files:
  created:
    - backend/tests/test_cache.py
    - backend/tests/test_limits.py
  modified:
    - backend/app/services/image_prep.py
    - backend/app/services/claude_client.py
    - backend/app/jobs.py
    - backend/app/main.py
    - backend/app/config.py
    - backend/tests/conftest.py
    - backend/tests/test_jobs_api.py

key-decisions:
  - "laplacian_var crops the 1 px border after the Kernel filter: Pillow copies border pixels unfiltered, which gave a smooth blurred gradient a variance of about 92 (would pass the pre-gate)"
  - "A contract ValidationError after a model reply (usage not surfaced by PalmReader) is charged worst case, like any unexpected exception: over-charging is the safe side of a budget guard"
  - "Spend-cap Retry-After is the seconds to the next UTC midnight; window Retry-After is the seconds until the oldest in-window job leaves it (minimum 1)"

requirements-completed: [READ-04, READ-10]

coverage:
  - id: D1
    description: "Same photo or a re-capture within DHASH_MAX_DISTANCE from the same install, hand, locale and prompt version within 24 h returns a done job with the identical reading and no model call"
    requirement: READ-04
    verification:
      - kind: integration
        ref: "backend/tests/test_cache.py#test_same_photo_same_install_is_cached, test_recapture_within_distance_is_hit"
        status: pass
  - id: D2
    description: "Other install, hand, locale, prompt version, dHash distance above threshold, or age over 24 h misses; cache bounded and holds (float, int, str) only"
    requirement: READ-04
    verification:
      - kind: integration
        ref: "backend/tests/test_cache.py#test_other_install_never_gets_the_entry, test_key_change_is_miss, test_visibly_different_photo_is_miss, test_entries_expire_after_24h, test_cache_is_bounded_and_holds_json_only"
        status: pass
  - id: D3
    description: "Dark or blurry photo -> done, verdict blurry_or_dark, lines [], no model call"
    verification:
      - kind: unit
        ref: "backend/tests/test_cache.py#test_pregate_metrics_unit"
        status: pass
      - kind: integration
        ref: "backend/tests/test_cache.py#test_pregate_answers_without_model_call"
        status: pass
  - id: D4
    description: "6th model-bound job in 10 min / 21st in a day per install -> 429 busy + Retry-After, no job, no call; other installs unaffected; free paths do not count; spend cap blocks until the next UTC day"
    requirement: READ-10
    verification:
      - kind: integration
        ref: "backend/tests/test_limits.py#test_sixth_job_in_ten_minutes_is_busy, test_twenty_first_job_in_a_day_is_busy, test_free_paths_do_not_count, test_spend_cap_blocks_until_next_utc_day"
        status: pass
  - id: D5
    description: "Per-attempt charging: usage x price (ok, max_tokens, refusal), worst case without usage (SDK ValidationError, deadline cancel), nothing for RateLimitError; cost() math incl. cache write/read, unknown model, None fields"
    requirement: READ-10
    verification:
      - kind: unit
        ref: "backend/tests/test_limits.py#test_cost_from_usage"
        status: pass
      - kind: integration
        ref: "backend/tests/test_limits.py#test_max_tokens_twice_charges_both_attempts, test_attempts_without_usage_charge_worst_case, test_refusal_charges_usage_and_status_errors_nothing"
        status: pass

duration: 20min
completed: 2026-11-27
---

# Phase 2 Plan 05: Cache, Pre-gate and Limits Summary

**Install-scoped reading cache (exact SHA-256, then dHash within 6/64 bits, 24 h, 2,000 entries, JSON only), a Pillow brightness/Laplacian pre-gate that answers dark or blurry photos as `blurry_or_dark` without a model call, and per-install 5/10 min + 20/day windows plus a per-attempt-charged UTC-day spend cap that return 429 busy with Retry-After.**

## Performance

- **Duration:** about 20 min
- **Started:** 2026-11-27T05:16:52Z
- **Completed:** 2026-11-27
- **Tasks:** 3 (1 tracer, 2 TDD)
- **Files modified:** 9

## Accomplishments

- Re-scanning the same palm from the same install returns the same reading (same `reading_id`) at once with no model call; the cache key carries install id, hand, locale and `PROMPT_VERSION`, so one install never sees another's reading.
- Obviously unusable photos are answered without a model call, and that answer is cached like any other.
- Model-bound requests over the per-install windows or the global daily cap get a clean 429 busy before any job or model call; cache hits, idempotent repeats and pre-gate results stay free.
- Every attempt the API may bill is charged to the ledger, including attempts the job deadline cancels.

## Task Commits

1. **Task 1: same photo, same install -> cached reading (tracer)** - `556b1c5` (feat)
2. **Task 2: re-captures hit, others miss, pre-gate** - `b2a7918` (test, RED), `409d601` (feat, GREEN)
3. **Task 3: rate windows and spend cap -> 429 busy** - `eecec70` (test, RED), `20ad385` (feat, GREEN)

## Decisions Made

See key-decisions in the frontmatter. Prices were checked against the claude-api skill's models table (Sonnet 5.5 $2/$10, cache read $0.10; Opus 5.5 $4/$20, cache read $0.20; Haiku 5.5 $0.10/$0.50, cache read not listed, so A5's $0.01 stands).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Laplacian variance inflated by the unfiltered border**
- **Found during:** Task 2 (GREEN)
- **Issue:** Pillow's `ImageFilter.Kernel` copies the 1 px border unfiltered, so a smooth gradient blurred with radius 12 scored about 92 (threshold 15) and skipped the pre-gate (A9 risk)
- **Fix:** crop the border before `ImageStat`
- **Files modified:** backend/app/services/image_prep.py
- **Commit:** 409d601

**2. [Rule 1 - Test update] Expiry test expected a second model call**
- **Found during:** Task 1
- **Issue:** `test_expired_job_is_404_and_key_is_released` re-posted the same photo after 601 s and expected 2 model calls; the new 24 h cache now serves it
- **Fix:** assertion is now 1 call; `new != old` still proves the key was released
- **Files modified:** backend/tests/test_jobs_api.py
- **Commit:** 556b1c5

**Total deviations:** 2 auto-fixed. **Impact:** none on scope.

## Issues Encountered

- `ruff` is not installed in the backend env, and new downloads are outside this session's approval, so lint was not run.

## Known Stubs

None.

## Next Phase Readiness

- 02-07's eval budget can use `cost()` and `worst_case_cost()`; 02-09 retunes `DHASH_MAX_DISTANCE` and the `PREGATE_*` constants without touching tests.
- Recommend an Anthropic Console spend limit at the 02-08 checkpoint (T-02-16): the in-process cap can overshoot by up to `claude_concurrency` calls.

## Self-Check: PASSED

- Files exist: backend/tests/test_cache.py, backend/tests/test_limits.py, backend/app/services/image_prep.py, backend/app/jobs.py
- Commits on HEAD: 556b1c5, b2a7918, 409d601, eecec70, 20ad385
- Full suite: 227 passed
