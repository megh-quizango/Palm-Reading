---
gsd_state_version: "1.0"
current_phase: 2
current_phase_name: Reading Pipeline
status: executing
stopped_at: Completed 04-01-PLAN.md
last_updated: "2026-11-28T02:01:17.851Z"
last_activity: 2026-11-27
last_activity_desc: Phase 2 execution started
state_head: c2bd52dd6ad7c6ebff281d8fd24383ba7e91eeaa
progress:
  total_phases: 6
  completed_phases: 2
  total_plans: 24
  completed_plans: 18
  percent: 33
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-11-26)

**Core value:** The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds.
**Current focus:** Phase 4 — Scan Loop (planned)

## Current Position

Phase: 4 (Scan Loop) — PLANNED, ready to execute (Phase 3 complete 2026-11-27, hardware perf gate folded into 04-05; Phase 2: 02-08/02-09 deferred)
Plan: 1 of 5 (waves: 04-01, 04-02+04-03+04-04, 04-05)
Status: Phase 4 plans checked (2 rounds; the last blocker, heifwriter minSdk 29 vs app 26, fixed in a final revision via an androidTest-only overrideLibrary). Execution needs download consent (CameraX 1.7.0, MediaPipe 1.2.3 + hand_landmarker.task, exifinterface, heifwriter test-only) and a physical phone for 04-01 tracer and 04-05
Last activity: 2026-11-27 — Phase 4 planned

Progress: [███░░░░░░░] 33%

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
| Phase 03 P01 | 16min | 3 tasks | 33 files |
| Phase 03 P02 | 10min | 3 tasks | 26 files |
| Phase 03 P03 | 135min | 2 tasks | 20 files |
| Phase 03 P04 | 82min | 3 tasks | 24 files |
| Phase 04 P01 | 70 min | 3 tasks | 36 files |

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
- [Phase 03]: 03-01: Profile DataStore uses OkioStorage (same format) because FileStorage rename fails on Windows JVM test hosts
- [Phase 03]: 03-01: vivo V2225 (API 35) is the reference phone; API 33+ LocaleManager path verified, API 26-32 AppCompat path not verified on hardware
- [Phase 03]: 03-01: LanguageScreen takes onContinue; entries decide the next step so 03-03 only edits OnboardingEntries
- [Phase 03]: 03-02: the sky clock is a MutableLongState read as a primitive in onDrawBehind (-1 = rest); no clock when animate=false, animator scale 0 or battery saver
- [Phase 03]: 03-02: Phase 3 illustrations are ported from our own 2026-11-26 design-session SVG (Figma MCP unavailable); tips and hero_orbit are hand-authored stand-ins under final names
- [Phase 03]: Only the birth step completes onboarding; Language Continue pushes Intro
- [Phase 03]: Birth time picker uses BasicAlertDialog, not an experimental time-picker dialog wrapper
- [Phase 03]: Instrumented Back uses sendKeyDownUpSync(KEYCODE_BACK); no pressBack dependency added
- [Phase 03]: Coming-soon is a ComingSoon(feature) route rendered by a Nav3 OverlayScene strategy hosting an M3 ModalBottomSheet over Home
- [Phase 03]: Sky animate flag = top route's showsBackground, so Scan pauses it
- [Phase 03]: NavBackStack.popFrom(from) guards double Back/OK pops
- [Phase 03]: Exit-confirm BackHandler stays in HomeScreen (wins over NavDisplay on the phone; A2 fallback not needed)
- [Phase 03]: Showcase lives in src/debug; src/release ships no-op debugEntries/debugHomeLongPress
- [Phase 03]: Per-app locale via AppCompat 1.8.0 AppCompatDelegate.setApplicationLocales + autoStoreLocales (LocaleManager on API 33+, AppCompat storage on API 26-32)
- [Phase 03]: 03-05 cold start (D-16) is gated on the profile-compiled median (cold-start.sh --compile, 5 runs of am start -S -W TotalTime), with the fresh-install median reported beside it. Emulator (API 35, x86_64, not a gate): fresh 565 ms, compiled 565 ms. The Redmi hardware gate is still to be measured
- [Phase 03]: 03-05 jank metric is the share of "Frame deadline missed" frames in dumpsys gfxinfo (limit 5%); the "Janky frames" line also counts High input latency frames and is unreliable on Android 11. Emulator release (not a gate): Home idle 10 s 29/578 (5.02%), intro swipe x3 9/251 (3.59%)
- [Phase 03]: 03-05 HeroOrb spin/breathe moved from graphicsLayer to draw-only transforms: per-frame layer updates fired onLayoutChange accessibility events, so uiautomator never went idle on Home
- [Phase 04]: 04-01: MediaPipe native lib is libmediapipe_tasks_jni.so, packaged for arm64-v8a/armeabi-v7a/x86/x86_64; landmarker runs on the x86_64 emulator, so wave-2 instrumented tests can use emulator-5554
- [Phase 04]: 04-01: real-palm checks (Task 4, capture EXIF proof, phone abilist, phone runs of the Task 2/3 device commands) deferred to the 04-05 phone session by the user's choice

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 2]: 02-08/02-09 deferred: real-Claude eval needs ANTHROPIC_API_KEY, 20+ consented photos + labels.csv, MediaPipe/model approval and a ~$10-15 budget. Until then readings are canned (template mode).

- ~~[Phase 1]: Kotlin 2.4.21 + Hilt 2.60.1 + KSP 2.3.14 + Room 3 + AGP 9.5.3 may not build together; scaffold build is the first gate~~ RESOLVED by plan 01-01 (target set green on device; Compose BOM held at 2026.09.00 until platforms;android-37.2 is installed)
- ~~[Phase 1]: MIUI on the Redmi asks "Install via USB?" for every adb install and auto-denies after about 10 s (INSTALL_FAILED_USER_RESTRICTED). Unattended installs need the user to tick "Remember my choice" once on that prompt~~ RESOLVED (user ticked "Remember my choice"; plan 01-03 ran 4 installs unattended)
- [Phase 2, 4]: No consented palm photo set exists yet (20-40 labelled photos, kept out of git); needed for the model bake-off and guidance calibration
- [Phase 2]: Palm model winner (Sonnet 5.5 vs Haiku 5.5) needs user sign-off after the eval
- [Phase 4]: Handedness and palm-facing thresholds need on-device calibration; verify MediaPipe 16 KB-aligned native libs
- [Phase 3]: all Hindi strings (values-hi) are Claude-written; native-speaker review needed before release (D-06)
- [Phase 3]: per-app locale: the API 33+ LocaleManager path is verified (vivo V2225 API 35 reference phone, Redmi Note 10T API 33, emulator API 35); the API 26-32 AppCompat path is unverified on hardware (no API 26-32 device)
- [Phase 3]: 03-05 performance gates (release cold start < 2 s, <= 5% deadline-missed frames on Home idle and intro swipes) were measured only on the x86 emulator, which is not a gate; the hardware run is deferred to the next phone session (debug intro swipe on the Redmi was 37.5%, so fix the intro pager if release is still over 5%)
- [Phase 4]: MediaPipe tasks-core sends usage metrics through Google datatransport (no opt-out API); disclose in the privacy policy and Play Data safety before release (REL-02/REL-03). Accepted by the user at the 04-01 consent (2026-11-28)
- [Phase 4]: native ABIs: debug APK lib/ has arm64-v8a, armeabi-v7a, x86, x86_64 (MediaPipe: libmediapipe_tasks_jni.so in all four; release keeps only armeabi-v7a + arm64-v8a); emulator-5554 abilist x86_64,arm64-v8a; the reference phone's abilist is still to be read in the 04-05 phone session (`adb shell getprop ro.product.cpu.abilist`)
- [Phase 3]: the Redmi Note 10T may have a test setting left changed (animations / font scale / battery saver) from an interrupted 03-05 run; check it next time it is connected

## Deferred Items

Items acknowledged and deferred at milestone close, most recent first:

| Category | Item | Status | Deferred At | Milestone |
|----------|------|--------|-------------|-----------|
| *(none)* | | | | |

## Session Continuity

Last session: 2026-11-28T02:01:17.633Z
Stopped at: Completed 04-01-PLAN.md
Resume file: None
