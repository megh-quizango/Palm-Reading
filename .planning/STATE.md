---
gsd_state_version: "1.0"
current_phase: 1
current_phase_name: Foundations & Contract
status: executing
stopped_at: Completed 01-04-PLAN.md
last_updated: "2026-11-26T11:13:17.367Z"
last_activity: 2026-11-26
last_activity_desc: Phase 1 execution started
state_head: f54957d46f82c7ae8f3b9bfa8c8a2d0f2e42c91f
progress:
  total_phases: 6
  completed_phases: 0
  total_plans: 5
  completed_plans: 4
  percent: 0
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-11-26)

**Core value:** The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds.
**Current focus:** Phase 1 — Foundations & Contract

## Current Position

Phase: 1 (Foundations & Contract) — EXECUTING
Plan: 5 of 5
Status: Ready to execute
Last activity: 2026-11-26 — Phase 1 execution started

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: - min
- Total execution time: 0.0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

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

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

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

### Pending Todos

None yet.

### Blockers/Concerns

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

Last session: 2026-11-26T11:13:17.348Z
Stopped at: Completed 01-04-PLAN.md
Resume file: None
