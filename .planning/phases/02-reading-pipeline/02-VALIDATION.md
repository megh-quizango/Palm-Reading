---
phase: "2"
slug: "reading-pipeline"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-26"
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | pytest 9.2.0 + Starlette TestClient (httpx 0.28.1), always used as a context manager |
| **Config file** | `backend/pyproject.toml` `[tool.pytest.ini_options]` (`testpaths = ["tests"]`, `pythonpath = ["."]`); shared fakes in `backend/tests/conftest.py` (created by 02-01 Task 2) |
| **Quick run command** | `uv run --directory backend pytest -q -x` |
| **Full suite command** | `uv run --directory backend pytest -q` |
| **Eval smoke (no key, no photos)** | `uv run --directory backend python -m evals.run_eval --fake --synthetic --redteam --out "$(mktemp -d)"` |
| **Real eval (02-08 only, after the human-action checkpoint)** | `uv run --directory backend python -m evals.run_eval --models claude-sonnet-5-5,claude-haiku-5-5 --repeats 5 --redteam --redteam-limit 2 --max-usd <A>`, then `--locales hi --repeats 2 --max-usd <B>` and the Opus pass `--max-usd <C>` (A + B + C = the approved budget, projected from the tracer call) |
| **Estimated runtime** | ~10 seconds (full suite; deadline tests use 0.2 s budgets) |

Tests never call the real API: conftest replaces `anthropic.AsyncAnthropic` / `anthropic.Anthropic` with a function that raises and points `ANTHROPIC_BASE_URL` at a closed local port as a backstop; `run_eval.main` takes Settings by injection and every smoke call passes `Settings(_env_file=None)`, so no test reads backend/.env, before or after the user adds the key in 02-08 (D-16).

---

## Sampling Rate

- **After every task commit:** Run `uv run --directory backend pytest -q -x`
- **After every plan wave:** Run `uv run --directory backend pytest -q` (from wave 4 on, also the eval smoke command)
- **Before `/gsd-verify-work`:** Full suite green, 02-EVAL.md committed, decisions recorded in STATE.md (02-09)
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 1 | READ-02 | T-02-SC | Only the four verified packages and their declared dependencies are downloaded | checkpoint (human) | — | n/a | ⬜ pending |
| 2-01-02 | 01 | 1 | READ-02, READ-05, READ-07 | T-02-01, T-02-06, T-02-07 | Upload spool cap above 8 MB; no real client in tests; prompt never-list | integration (FakeSdk) | `uv run --directory backend pytest -q tests/test_jobs_api.py tests/test_claude_client.py` | ❌ W1 (created in task) | ⬜ pending |
| 2-01-03 | 01 | 1 | READ-05 | T-02-02, T-02-03, T-02-04, T-02-05 | Pixel cap, Content-Length cap, NaN-rejecting landmarks, malformed multipart mapped to 422 bad_request, EXIF stripped, code-only errors | unit + integration | `uv run --directory backend pytest -q tests/test_image_prep.py tests/test_jobs_api.py` | ❌ W1 (created in task) | ⬜ pending |
| 2-02-01 | 02 | 2 | READ-06 | T-02-08 | Off-palm LLM trace replaced by template | integration (reader) | `uv run --directory backend pytest -q tests/test_palm_reader.py` | ❌ W2 | ⬜ pending |
| 2-02-02 | 02 | 2 | READ-06 | T-02-08, T-02-09 | All D-10 rules with the failing rule recorded; degenerate landmarks safe | unit | `uv run --directory backend pytest -q tests/test_line_geometry.py tests/test_palm_reader.py` | ❌ W2 | ⬜ pending |
| 2-03-01 | 03 | 2 | READ-02 | T-02-10 | Malformed retried once, then model_error | integration | `uv run --directory backend pytest -q tests/test_job_failures.py` | ❌ W2 | ⬜ pending |
| 2-03-02 | 03 | 2 | READ-02 | T-02-10, T-02-11 | Refusal no retry; busy/timeout/model_error mapping; logs without error text | unit + integration | `uv run --directory backend pytest -q tests/test_claude_client.py tests/test_job_failures.py` | ❌ W2 | ⬜ pending |
| 2-03-03 | 03 | 2 | READ-02 | T-02-12, T-02-13 | Idempotency and single-flight scoped per install; 10-minute expiry | integration | `uv run --directory backend pytest -q tests/test_jobs_api.py -k "idempot or single_flight or expire"` | ✅ (extends 02-01 file) | ⬜ pending |
| 2-04-01 | 04 | 3 | READ-07 | T-02-14 | Death prediction field rewritten | integration (reader) | `uv run --directory backend pytest -q tests/test_palm_reader.py -k rewritten` | ✅ (extends 02-02 file) | ⬜ pending |
| 2-04-02 | 04 | 3 | READ-07 | T-02-14, T-02-15 | en/hi/Hinglish denylist incl. Hindi inflections and zero-width variants, no hits on life-line length or industry words, idempotent, red-team through the API | unit + integration | `uv run --directory backend pytest -q tests/test_safety.py` | ❌ W3 | ⬜ pending |
| 2-05-01 | 05 | 3 | READ-04 | T-02-17, T-02-19 | Identical reading from the cache, no model call | integration | `uv run --directory backend pytest -q tests/test_cache.py` | ❌ W3 | ⬜ pending |
| 2-05-02 | 05 | 3 | READ-04 | T-02-17, T-02-19 | Install-scoped key, misses, expiry, bound; pre-gate without model call | unit + integration | `uv run --directory backend pytest -q tests/test_cache.py -k "pregate or install or expire"` | ✅ (extends task 1 file) | ⬜ pending |
| 2-05-03 | 05 | 3 | READ-10 | T-02-16, T-02-18 | 429 busy + Retry-After before any model call; spend ledger charged per attempt (worst case without usage) | unit + integration (patched clock) | `uv run --directory backend pytest -q tests/test_limits.py` | ❌ W3 | ⬜ pending |
| 2-06-01 | 06 | 4 | READ-05 | T-02-20 | 1.5 MB upload never spools to disk (success, failure, timeout) | integration | `uv run --directory backend pytest -q tests/test_privacy.py -k no_disk` | ❌ W4 | ⬜ pending |
| 2-06-02 | 06 | 4 | READ-05 | T-02-20, T-02-21 | No image or landmark data in logs (incl. 422 and EXIF paths) or app-held stores; anthropic and PIL loggers never at DEBUG | integration | `uv run --directory backend pytest -q tests/test_privacy.py` | ✅ (extends task 1 file) | ⬜ pending |
| 2-07-01 | 07 | 4 | READ-02 | T-02-22 | Fake-mode eval writes rows and summary | smoke | `uv run --directory backend pytest -q tests/test_eval_smoke.py` | ❌ W4 | ⬜ pending |
| 2-07-02 | 07 | 4 | READ-06 | T-02-22, T-02-23, T-02-24, T-02-30 | Injected Settings (no test reads backend/.env), call-time client lookup, module-anchored paths, probe-path ignore gate, per-attempt budget stop; all bake-off metrics per model and locale, aggregates-only summary | smoke | `uv run --directory backend pytest -q tests/test_eval_smoke.py` | ✅ | ⬜ pending |
| 2-07-03 | 07 | 4 | READ-07 | T-02-15 | Image red-team for all six topics runs first; zero after-filter hits; failed cell exits 1; raw and final prose kept locally for review | smoke | `uv run --directory backend pytest -q tests/test_eval_smoke.py -k redteam` | ✅ | ⬜ pending |
| 2-08-01 | 08 | 5 | READ-02 | T-02-25 | Key confirmed via /healthz only | checkpoint (human-action) | — | n/a | ⬜ pending |
| 2-08-02 | 08 | 5 | READ-02 | T-02-26 | One real call before the full run; one real POST through uvicorn with the real lifespan and clean shutdown; cost projected from the tracer | eval (real API) | `uv run --directory backend python -m evals.run_eval --models claude-sonnet-5-5 --limit 1 --repeats 1` and `uv run --directory backend python -m evals.server_e2e` | ✅ (server_e2e.py created in task) | ⬜ pending |
| 2-08-03 | 08 | 5 | READ-06, READ-07 | T-02-26, T-02-27 | Budgeted bake-off (en and hi); red-team readings reviewed; aggregates-only summary with no labels.csv name or stem | eval (real API) + grep gates | `grep -q 'Proposed decisions' .planning/phases/02-reading-pipeline/02-EVAL.md` plus the labels.csv name loop | ❌ (created in task) | ⬜ pending |
| 2-09-01 | 09 | 6 | READ-04, READ-06 | — | User sign-off (D-06, D-11) | checkpoint (decision) | — | n/a | ⬜ pending |
| 2-09-02 | 09 | 6 | READ-04, READ-06 | T-02-28, T-02-29 | Signed-off defaults; suite green | unit (full suite) | `uv run --directory backend pytest -q` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Tracer-first: there is no separate Wave 0. pytest 9.2.0 and the pyproject config already exist (39 tests green at HEAD). The shared fixtures are created by the tracer task that first needs them:

- [ ] `backend/tests/conftest.py` — FakeSdk, ok_reply, make_jpeg, LANDMARKS / LANDMARKS_LEFT, settings, client_factory, post_job, poll, real-client guard (02-01 Task 2)
- [ ] `.gitignore` — `backend/evals/photos/`, `backend/evals/results/`, `backend/evals/models/` before any eval file exists (02-01 Task 2)
- [ ] runtime packages anthropic 2.14.0, pillow 13.0.0, python-multipart 0.0.34 after the legitimacy checkpoint (02-01 Tasks 1-2)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Package legitimacy before download | READ-02 | Never auto-approvable (supply-chain gate) | 02-01 Task 1: check the four PyPI pages and repo links, reply "approved" |
| API key, consented photo set, landmark model, eval budget, MediaPipe download | READ-02, READ-06, READ-07 | Only the user holds the key and the consented photos; the heavy ephemeral download is confirmed in the session that runs it | 02-08 Task 1: follow the checkpoint instructions; Claude verifies via /healthz and file counts |
| Red-team forbidden-directive review | READ-07 | A paraphrased directive can escape any denylist, so SC5 is judged by reading the outputs | 02-08 Task 3: the executor reads every raw and final red-team reading locally and records reviewed / found / followed counts in 02-EVAL.md |
| Model winner, template-first (and drop or keep LLM points), thresholds, refusal fallback | READ-04, READ-06 | D-06 requires user sign-off on the cost/latency trade | 02-09 Task 1: read 02-EVAL.md and reply with the choices |
| Handwritten Hindi injection photos (optional) | READ-07 | Pillow without raqm renders Devanagari unshaped (Pitfall 10) | Supply 2-3 photos labelled "redteam-hi" in labels.csv |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (checkpoint tasks excepted)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (fixtures created by the 02-01 tracer)
- [x] No watch-mode flags
- [x] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter (set by validate-phase)

**Approval:** pending
