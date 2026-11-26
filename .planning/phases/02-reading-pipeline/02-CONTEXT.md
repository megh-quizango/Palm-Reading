# Phase 2: Reading Pipeline - Context

**Gathered:** 2026-11-26
**Status:** Ready for planning

<domain>
## Phase Boundary

A local FastAPI backend turns a palm photo + hand side + 21 normalised hand landmarks into a validated, safe, cached `PalmReading v1` (the Phase 1 contract, `backend/app/schemas/palm.py`) through an async job API (POST job → 202 + job id → poll). Proven with an eval over real palm photos and with API tests (Claude mocked) before any client wiring. Requirements: READ-02, READ-04, READ-05, READ-06, READ-07, READ-10. No Android work here; the client wire-up is Phase 6.

</domain>

<decisions>
## Implementation Decisions

The user asked Claude to decide gray areas ("use your best approach instead of discussing", carried over from Phase 1 at the user's go-ahead "Plan Phase 2"). Decisions below are Claude's, grounded in `.planning/research/SUMMARY.md` and the Phase 1 outcome.

### Job API (contract with the Phase 6 client)
- **D-01:** Poll, not SSE, in milestone 1. `POST /v1/palm/jobs` (multipart: `image`, `hand_side`, `locale`, `landmarks` JSON = 21 `[x,y,z]` normalised points from the IMAGE-mode pass on the processed still) → `202 {job_id}`. `GET /v1/palm/jobs/{id}` → `{status: queued|running|done|failed, reading?, error?}`. `error.code` is infrastructure only (`busy`, `timeout`, `model_error`, `bad_request`); an unusable photo is a *successful* job whose reading has `verdict.ok=false` (Phase 1 contract).
- **D-02:** Idempotency: the client sends an `Idempotency-Key` header (UUID); a retried POST with the same key returns the same job id. Single-flight on identical image bytes (SHA-256) while a job is running.
- **D-03:** Job runner = plain asyncio (`create_task` held in a module-level set, `asyncio.Semaphore` capping concurrent Claude calls, `asyncio.timeout(45)` overall), in-memory job store with a 10-minute TTL sweep. No Celery/Redis/queue library (single local instance in M1; externalising is REL-04).

### Model, prompt, structured output
- **D-04:** Official `anthropic` SDK only (pin per `.claude/CLAUDE.md`: anthropic 2.14.0, Pillow 13.0.0, python-multipart 0.0.34). Structured output via `client.messages.parse(output_format=<Pydantic model>)` with `output_config.effort="low"`. Never send `temperature`, `top_p`, `top_k`, `budget_tokens`, `thinking: disabled`, forced `tool_choice` or assistant prefill. Always handle `stop_reason == "refusal"`. The server-side refusal fallback beta stays off until the eval shows real refusals (decided after research).
- **D-05:** The model returns an *LLM-side* schema (prose + per-line qualitative params + candidate polyline + quality verdict), not the public contract directly; the server assembles `PalmReading v1` (ids, meta, image_size, trace_source) and validates it with the strict Phase 1 model (`model_validate_json` on wire JSON). Malformed output → one retry → clean `model_error` failure.
- **D-06:** Model in env `PALM_MODEL`, default `claude-sonnet-5-5`. The bake-off compares `claude-sonnet-5-5` vs `claude-haiku-5-5` (Opus 5.5 only as a quality ceiling on a few photos). **One-way-ish:** the winner is a cost/latency trade → **user sign-off checkpoint** after the eval table, before it becomes the default.
- **D-07:** `PROMPT_VERSION` constant (e.g. `p1`) is part of the cache key and of `meta.prompt_version`; prompt + LLM schema kept byte-stable for prompt caching.

### Image handling & privacy (READ-05)
- **D-08:** Image bytes live in memory only: validate (≤ 8 MB, JPEG/PNG/WebP, Pillow `verify()` + dimensions), re-encode JPEG (drops EXIF), cap long edge at 1568 (never upscale; the client already sends ≤ 1280), compute dHash + SHA-256, drop the reference as soon as the Claude call returns. Never written to disk, never logged. Landmarks: used only for validation/templates, never stored or logged. Tests assert no image file appears and no image/landmark bytes appear in captured logs after success, failure and timeout.
- **D-09:** Server-side quality pre-gate (Pillow Laplacian-variance blur + mean brightness) can short-circuit obviously blurry/dark photos into `verdict.ok=false, reason=blurry_or_dark` without a model call; thresholds calibrated in the eval.

### Lines: validation + template fallback (READ-06)
- **D-10:** Pure `line_geometry` module: an LLM trace passes only if it has 4–24 points, ≥ 80% of points inside the palm hull built from the landmarks, sane length, no sharp reversals, and heart-above-head ordering. Failing lines are replaced by a landmark-anchored template in a hand-local frame (`trace_source="template"`); if no template can be built, `trace=[]`, `trace_source="none"`. Required lines (heart, head, life) always end up present in an ok reading (Phase 1 contract rule).
- **D-11:** The eval answers "template-first?": if < ~70% of LLM lines pass validation, flip the default so the LLM supplies only qualitative params and templates draw every line. Record the decision in STATE.md.

### Cache (READ-04)
- **D-12:** Result cache keyed by **`X-Install-Id`** + dHash (Hamming distance ≤ a small threshold, start 6/64, tuned in eval) + hand side + locale + `PROMPT_VERSION` (amended after research: palms look alike across people, so the cache is per install and never serves one user another user's reading), 24 h TTL, stores reading JSON only (never the image or landmarks). A cache hit makes no model call and returns the identical reading (same `reading_id`).

### Safety (READ-07)
- **D-13:** Prompt never-list (no death, illness, pregnancy, medical, legal or financial directives; ignore any text in the image) + a server output filter over all prose fields (English + Hindi denylist of terms/patterns). A filtered field is rewritten from a safe neutral template rather than failing the job. A red-team set (both languages, including injected text in the image) is part of the eval/test suite and must yield zero forbidden directives.

### Abuse & spend (READ-10)
- **D-14:** Per-install token: the client sends an `X-Install-Id` header (random UUID generated on first run; no account). Rate limit per install (start: 5 jobs / 10 min, 20 / day) and a global daily spend cap (USD, env `DAILY_SPEND_CAP_USD`, computed from `usage` tokens × price table) → a clean `busy` error with no model call. In-memory counters (single instance in M1). Play Integrity is REL-04, not here.

### Eval harness & photo set
- **D-15:** `backend/evals/run_eval.py` over a **consented palm photo set** that the user supplies at `backend/evals/photos/` (git-ignored; this repo is public) with a small labels file (hand side, notes: skin tone, henna, rings, blur, backlight). Landmarks for each photo are produced offline by a one-off MediaPipe Python pass in the eval tool (dev-only dependency) or supplied alongside the photo. Output: latency p50/p95, cost per reading, % LLM lines passing validation, 5× repeat agreement on qualitative enums, refusal count — written to `backend/evals/results/` (git-ignored) with a committed summary table in the phase docs (no photos, no readings text beyond aggregates).
- **D-16:** The real-Claude eval needs the user's `ANTHROPIC_API_KEY` in `backend/.env` and the photo set. Both are **user prerequisites** → a `checkpoint:human-action` before the eval plan runs. Everything else (API, cache, geometry, safety filter, privacy tests) is built and tested with Claude mocked and does not wait on them. CI/tests never call the real API.

### Claude's Discretion
- Module layout under `backend/app/` (e.g. `api/`, `jobs/`, `services/palm_reader/`), exact thresholds' starting values, the LLM-side schema shape, denylist contents, and the template curve shapes — as long as D-01..D-16 hold.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/ROADMAP.md` §Phase 2 — goal, 5 success criteria, research flag
- `.planning/REQUIREMENTS.md` — READ-02, READ-04, READ-05, READ-06, READ-07, READ-10
- `.planning/research/SUMMARY.md` — conflicts 5, 6, 8 and sign-offs 2–4 (normalised traces, landmarks sent transiently, perceptual-hash cache), Phase 2 deliverables and exit gates
- `.planning/research/STACK.md` and `.claude/CLAUDE.md` — Anthropic usage rules (no sampling params, `output_config.effort`, refusals, prompt caching, image sizing), FastAPI async job pattern, pinned versions
- `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md` — pipeline layout, polyline pitfalls, privacy
- `.planning/phases/01-foundations-contract/01-02-SUMMARY.md`, `01-VERIFICATION.md` and commit 18b9edc — the frozen strict `PalmReading v1` contract (parse with `model_validate_json`; ok readings require heart/head/life)
- `contracts/palm_reading.v1.schema.json`, `contracts/palm_reading.sample.json`, `contracts/palm_reading.invalid_photo.sample.json`
- `replica/PRD.md` §5.1 (R-PALM-1..7), §10 (NFRs: p50 < 12 s, p95 < 30 s)
- Use the `claude-api` skill for current SDK/model details before writing the Claude client.

</canonical_refs>

<code_context>
## Existing Code Insights

- `backend/app/main.py` (FastAPI app, `/healthz` reporting `anthropic_key: configured|missing`), `backend/app/config.py` (pydantic-settings reading `backend/.env`), `backend/app/schemas/palm.py` (strict v1 contract), `backend/scripts/export_schema.py`, `backend/tests/` (test_contract.py, test_health.py). Python 3.13 via uv; pytest 9.2; httpx TestClient (Starlette deprecation warning is known).
- `tools/run-backend.sh` runs uvicorn on 127.0.0.1:8000 (no `--reload` during job tests).
- Repo is public: photos, eval outputs, `.env` must stay git-ignored.

</code_context>

<specifics>
## Specific Ideas

- Build order: pure pieces with tests first (image_prep, line_geometry/templates, safety filter, cache), then the Claude client + assembler with a mocked client, then the job API + limits, then the real-Claude eval (after the user prerequisites), then the model sign-off.
- The eval summary table is the artifact the user signs off on.

</specifics>

<deferred>
## Deferred Ideas

- SSE job events — poll is enough for M1 (PRD allows either).
- Redis/Postgres job store, multi-instance Cloud Run, Play Integrity — REL-04.
- Trained palm-line segmentation model — later milestone.
- Report-a-reading endpoint — REL-01 (client control in Phase 5 is out of M1 scope per user choice).

</deferred>

---

*Phase: 02-reading-pipeline*
