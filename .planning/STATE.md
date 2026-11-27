---
gsd_state_version: "1.0"
current_phase: 2
current_phase_name: Reading Pipeline
status: executing
stopped_at: 02-07 complete; 02-08/02-09 deferred, backend runs in template mode
last_updated: "2026-11-27T05:42:46.297Z"
last_activity: 2026-11-27
last_activity_desc: Phase 2 execution started
state_head: 89e35a86aba838edfeae77b348b54119d3957ae8
progress:
  total_phases: 6
  completed_phases: 1
  total_plans: 14
  completed_plans: 12
  percent: 17
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-11-26)

**Core value:** The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds.
**Current focus:** Phase 2 — Reading Pipeline

## Current Position

Phase: 2 (Reading Pipeline) — EXECUTING
Plan: 8 of 9
Status: 02-08/02-09 deferred (template mode); Phase 2 not verified
Last activity: 2026-11-27 — Phase 2 execution started

Progress: [██░░░░░░░░] 17%

## Performance Metrics

**Velocity:**
- Total plans completed: 5
- Average duration: - min
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 1 | 5 | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: -

*Updated after each plan completion*
**Per-Plan Metrics:**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 1 P01 | 19 min | 2 tasks | 38 files |
| Phase 01 P02 | 4 min | 2 tasks | 10 files |
| Phase 01 P03 | 7 min | 2 tasks | 10 files |
| Phase 01 P04 | 14 min | 2 tasks | 26 files |
| Phase 01 P05 | 14 min | 2 tasks | 13 files |
| Phase 02 P01 | 5min | 3 tasks | 16 files |
| Phase 02 P02 | 4min | 2 tasks | 4 files |
| Phase 02 P03 | 3min | 3 tasks | 7 files |
| Phase 02 P04 | 4min | 2 tasks | 4 files |
| Phase 02 P05 | 20min | 3 tasks | 9 files |
| Phase 02 P06 | 2min | 2 tasks | 4 files |
| Phase 02 P07 | 15min | 3 tasks | 6 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:
- [Phase 2, 2026-11-27]: User chose to leave the Anthropic backend for now. PALM_MODE=template (default) serves canned en/hi readings (backend/app/services/template_model.py, 3 en + 2 hi variants picked by photo SHA-256) with landmark-anchored template lines and no API call; PALM_MODE=claude restores the real path. Plans 02-08 (real eval) and 02-09 (model sign-off) deferred until the user supplies the key, photos and budget. Dev testing uses synthetic template photos.

- [Roadmap]: Navigation 3 + Room 3, minSdk 26 / targetSdk 36 / compileSdk 37; fallback is Kotlin 2.3.21 / Room 2.8.5 / Navigation-Compose if the Phase 1 scaffold fails
- [Roadmap]: Contract uses normalised 0-1 traces with `trace_source` (llm|template|none); invalid photo is a successful job with `verdict.ok=false`; poll is the job contract (no SSE in M1)
- [Roadmap]: Client uploads a palm crop at most 1280 px plus the 21 landmarks (sent transiently, never stored or logged); 24 h perceptual-hash result cache
- [Roadmap]: Birth place deferred to the astrology milestone; report button and privacy notice / age check deferred to "required before public release" (REL-*); share card and reopen-last-reading are in M1
- [Roadmap]: Phase 2 (backend) runs parallel to Phases 3-5 against a fake repository; Phase 6 wires them
- [Phase 1] Toolchain gate: TARGET SET built and ran green on the Redmi (API 30), no fallback-ladder step used: AGP 9.5.3 built-in Kotlin, KGP/Kotlin 2.4.21 (buildscript classpath), KSP 2.3.14, Hilt 2.60.1 + androidx.hilt 1.4.0, Navigation 3 1.2.2, Room 3 3.0.3 + sqlite-bundled 2.7.1 (no @ConstructedBy, no kotlin-metadata-jvm override), Gradle 9.6.0, compileSdk 37.0. One deviation: Compose BOM 2026.09.00 (Compose 1.12.1, material3 1.4.0) instead of 2026.11.01, because Compose 1.13.0 requires compileSdk 37.1+ and only platforms;android-37.0 is installed (installing android-37.2 was not permitted in-session). Return to BOM 2026.11.01 once platforms;android-37.2 is installed and compileSdk is raised to 37.2
- [Pre-release] applicationId app.palm.dev (debug app.palm.dev.debug) is a placeholder: replace it with the brand id before the first Play upload; it is one-way after that upload (D-08, REL-03)
- [Phase 01]: PalmReading v1 frozen: Pydantic models author contracts/palm_reading.v1.schema.json (drift-tested); invariants enforced by model_validators; Kotlin DTOs strict-decode the same samples
- [Phase 01]: Stopped backend behind adb reverse = clean close (OkHttp end-of-stream/EOF), shown on the phone as 'No response: start tools/run-backend.sh'
- [Phase 01]: /healthz returns exactly {status, anthropic_key: configured|missing}; Settings reads an absolute backend env path; blank key counts as missing
- [Phase 01]: 01-04: google/fonts pinned at 6ebe737e (FONTS_SHA in tools/fonts.sh); fontTools 4.68.0 run via uv run python -I -m (not uvx) so reads of downloaded fonts are isolated
- [Phase 01]: 01-04: generator emits NocturneColors.ByPath and NocturneTypeSpecs.ByName so showcase lists never hand-copy token paths or names
- [Phase 01]: Effects.kt is the single token-to-Compose conversion point: dropShadow glows and CSS-angle gradient brushes; components pass NocturneGradientAngles constants
- [Phase 01]: NocturneBackground: seeded 60-star sky, animated values read only in drawBehind, no transition under reduceMotion; content in its own graphicsLayer
- [Phase 2 planning]: Plan check: 2 rounds (multi-lens review + adversarial verify), 27 + 2 confirmed issues fixed; real-API eval budget estimate is ~$10-15 (Hindi pass, 6-topic red-team), approved by the user at 02-08 Task 1
- [Phase 02]: 02-01: LLM verdict ok=false with reason none maps to no_palm_lines
- [Phase 02]: 02-01: POST gate order is Content-Length (411/413), X-Install-Id, busy (503), then form parsing
- [Phase 02]: 02-02: assemble returns (PalmReading, line_checks); PalmReader.read is its only caller
- [Phase 02]: 02-02: TEMPLATES retuned inside the landmark hull (A3); thresholds are named constants for 02-09
- [Phase 02]: 02-03: a POST that single-flights onto a running job also maps its own Idempotency-Key to that job
- [Phase 02]: 02-03: failure logs are job_failed job_id/code/error=<class of __cause__ or exception>, never the message
- [Phase 02]: 02-03: OverloadedError (529) maps to busy through the InternalServerError branch
- [Phase 02]: ReadResult.llm stays unfiltered model output; only the filtered reading reaches jobs and cache
- [Phase 02]: Safety list field with flagged items counts as one rewrite: dropped and refilled to 3 from NEUTRAL defaults
- [Phase 02]: Bare शेयर is not a Hindi directive trigger; only शेयरों / शेयर में / शेयर बाज़ार count
- [Phase 02]: 02-05: laplacian_var crops the 1 px border Pillow leaves unfiltered (a smooth blurred gradient scored ~92 otherwise)
- [Phase 02]: 02-05: attempts ending without usage (incl. contract ValidationError and deadline cancel) are charged worst_case_cost; API status errors charge nothing
- [Phase 02]: anthropic and PIL loggers pinned to WARNING at import (claude_client, image_prep): overrides root DEBUG and ANTHROPIC_LOG=debug so the base64 photo and EXIF/GPS never reach logs
- [Phase 02]: 02-07: eval red-team cell fails only on no reading (refusal/malformed/model failure after retry); a budget stop leaving red-team cells unrun also exits 1
- [Phase 02]: 02-07: photos labelled notes=redteam-hi are red-team inputs only, excluded from bake-off, dHash and pre-gate tables
- [Phase 02]: 02-07: make_landmarks also applies the git check-ignore gate to the photos dir it writes into

### Pending Todos

None yet.

### Blockers/Concerns
- [Phase 2]: 02-08/02-09 deferred: real-Claude eval needs ANTHROPIC_API_KEY, 20+ consented photos + labels.csv, MediaPipe/model approval and a ~$10-15 budget. Until then readings are canned (template mode).

- ~~[Phase 1]: Kotlin 2.4.21 + Hilt 2.60.1 + KSP 2.3.14 + Room 3 + AGP 9.5.3 may not build together; scaffold build is the first gate~~ RESOLVED by plan 01-01 (target set green on device; Compose BOM held at 2026.09.00 until platforms;android-37.2 is installed)
- ~~[Phase 1]: MIUI on the Redmi asks "Install via USB?" for every adb install and auto-denies after about 10 s (INSTALL_FAILED_USER_RESTRICTED). Unattended installs need the user to tick "Remember my choice" once on that prompt~~ RESOLVED (user ticked "Remember my choice"; plan 01-03 ran 4 installs unattended)
- [Phase 2, 4]: No consented palm photo set exists yet (20-40 labelled photos, kept out of git); needed for the model bake-off and guidance calibration
- [Phase 2]: Palm model winner (Sonnet 5.5 vs Haiku 5.5) needs user sign-off after the eval
- [Phase 4]: Handedness and palm-facing thresholds need on-device calibration; verify MediaPipe 16 KB-aligned native libs

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-11-27T05:42:46.144Z
Stopped at: Completed 02-07-PLAN.md
Resume file: None
