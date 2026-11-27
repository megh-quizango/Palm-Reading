# Roadmap: [AppName]

## Overview

Milestone 1 delivers the palm-scan vertical slice of the AI Palm Reading & Astrology Android app: a real reading on a real phone. The journey starts by freezing the toolchain and the versioned palm-reading contract (Phase 1). From there the work splits into two tracks that meet at the end. The backend reading pipeline is proven on real palm photos (Phase 2). The client is built against a fake repository: the onboarding and Home shell (Phase 3), the live scan loop (Phase 4) and the result screen with line overlay (Phase 5). Phase 6 wires scan, backend and result into one loop, hits the latency targets on the reference phone, and adds the stubbed ad gates for repeat use. Everything beyond the palm scan (tarot, horoscope, Vedic chart, chat, real ads, notifications) is later milestones.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundations & Contract** - Green build on the phone, Nocturne theme, reachable backend, frozen reading contract (completed 2026-11-26)
- [ ] **Phase 2: Reading Pipeline** - Backend turns a palm photo + landmarks into a validated, safe, cached reading via an async job API
- [x] **Phase 3: Client Shell** - Onboarding, Home hub with cosmic background, and hand selection, persisted across restarts
- [ ] **Phase 4: Scan Loop** - Live hand guidance, capture and gallery import, processed upload-ready photo, smooth on the low-end phone
- [ ] **Phase 5: Result Against the Fixture** - Palm / Love / Career result with line overlay, saved and shareable, built on fixture data
- [ ] **Phase 6: Wire-Up & Ad Gates** - Real palm to real reading in under 30 s, generating/error states, stubbed rewarded gate for repeat scans

## Phase Details

### Phase 1: Foundations & Contract

**Goal:** The project builds, installs and runs on the physical phone with the Nocturne theme, the phone can reach the local backend, and client and server share one frozen, versioned palm-reading contract.
**Depends on**: Nothing (first phase)
**Requirements**: FND-01, FND-02, FND-04, FND-05
**Success Criteria** (what must be TRUE):
  1. Developer runs one documented command from the repo (its path contains a space) and the debug app installs and launches on the physical phone on the target toolchain (Kotlin 2.4 / AGP 9.5 / Hilt / KSP / Room 3 / Navigation 3), or on the documented fallback set (Kotlin 2.3.21 / Room 2.8.5 / Navigation-Compose) if the first set fails to build.
  2. The launched screen renders in the Nocturne theme (colours, type scale, shapes, glows) generated from `design-system/nocturne.tokens.json` with no hand-copied token values, edge-to-edge under the system bars at targetSdk 36 and minSdk 26.
  3. `GET /healthz` on the local FastAPI backend (Python 3.13 pinned) answers from the phone over `adb reverse` at `127.0.0.1`, and the app shows that the backend is reachable; cleartext is allowed in debug only.
  4. `contracts/` holds a versioned palm-reading JSON schema and a sample reading that both the Kotlin and the Pydantic side parse unchanged; trace coordinates are normalised 0-1, each line carries a `trace_source`, and an unusable photo is a successful reading with `verdict.ok=false` and one of 7 reason codes.

**Plans:** 5/5 plans complete

Plans:
- [x] 01-01-PLAN.md — Walking skeleton: one command installs the target-toolchain app on the phone and it shows the backend Connected; Room 3 on device; toolchain gate recorded (wave 1)
- [x] 01-02-PLAN.md — Frozen PalmReading v1 contract: Pydantic models, exported schema, samples parsed unchanged by Pydantic and Kotlin DTOs (wave 2)
- [x] 01-03-PLAN.md — Backend diagnosis on the phone (Unreachable cause, base URL, Retry), /healthz key configured/missing, device-detection tests, README (wave 2)
- [x] 01-04-PLAN.md — Nocturne theme generated from tokens: colours, shapes, bundled OFL fonts with Devanagari fallback, 11 text styles on the showcase (wave 3)
- [x] 01-05-PLAN.md — Showcase components with glows, restyled status chip, animated star background (static under reduce motion), orb launcher icon, phase-end visual check (wave 4)

**UI hint**: yes

### Phase 2: Reading Pipeline

**Goal:** A local backend turns a palm photo, hand side and hand landmarks into a validated, safe, cached Palm / Love / Career reading through an async job API, proven on real palm photos before any client wiring.
**Mode:** mvp
**Depends on**: Phase 1 (contract and backend project). Independent of Phases 3-5; can run in parallel with them.
**Requirements**: READ-02, READ-04, READ-05, READ-06, READ-07, READ-10
**Success Criteria** (what must be TRUE):
  1. Developer can `POST` a palm photo, hand side and landmarks to the job endpoint (202 + job id), poll the job, and receive a reading that validates against the contract (summary, traits, Heart/Head/Life/Fate/Sun lines with traces, love, career, verdict); malformed model output is retried once and then fails cleanly, and a retried POST returns the same job.
  2. An eval run over the consented palm set reports latency p50/p95, cost, share of lines passing validation and repeat-run agreement, and it picks the model and prompt version; lines that fail validation against the landmarks are replaced by landmark-anchored template lines (`trace_source` records which), and the eval answers whether templates should be the default.
  3. The same photo, or a re-capture within a small perceptual-hash distance, with the same hand, language and prompt version within 24 hours returns the identical reading without a second model call.
  4. After a success, a failure and a timeout, no palm image exists on disk or in any log, and landmarks are never stored or logged (asserted by tests).
  5. A red-team set (death, illness, pregnancy, medical, legal, financial, injected text in the image; English and Hindi) never yields a forbidden directive in a returned reading, and a client over its rate limit or a tripped daily spend cap gets a clean "busy" response instead of a model call.

**Research flag**: Needs deeper research and a spike (`/gsd-plan-phase` with research): palm model bake-off (default Sonnet 5.5 vs Haiku 5.5, Opus as quality ceiling only), polyline quality, multilingual denylist, Anthropic data-retention terms. Prerequisite: a consented palm photo set (20-40 labelled photos covering skin tones, henna, rings, blur, backlight), kept out of git. The winning model needs user sign-off (cost and latency trade).
**Plans:** 7/9 plans executed

Plans:
- [x] 02-01-PLAN.md — Tracer: POST a palm photo, poll a contract-valid reading (Claude mocked at the SDK); package legitimacy checkpoint; input validation and EXIF strip (wave 1)
- [x] 02-02-PLAN.md — Lines validated against the landmarks, template fallback, required lines, template-first switch (wave 2)
- [x] 02-03-PLAN.md — Failure states: malformed retried once, refusal, busy/timeout/model_error, idempotency, single-flight, job expiry (wave 2)
- [x] 02-04-PLAN.md — Safety filter: en/hi/Hinglish denylist with field rewrite and a red-team gate (wave 3)
- [x] 02-05-PLAN.md — Install-scoped dHash result cache, blur/dark pre-gate, rate limit and daily spend cap (wave 3)
- [x] 02-06-PLAN.md — Privacy proof: no image on disk or in logs, no landmarks stored, after success, failure and timeout (wave 4)
- [x] 02-07-PLAN.md — Eval harness green in fake mode: bake-off metrics, MediaPipe landmarks tool, image red-team (wave 4)
- [ ] 02-08-PLAN.md — User prerequisites, then the real-Claude bake-off and red-team; 02-EVAL.md (wave 5) — DEFERRED (user 2026-11-27: template mode for now)
- [ ] 02-09-PLAN.md — Model, template-first and threshold sign-off applied and recorded in STATE.md (wave 6) — DEFERRED (follows 02-08)

### Phase 3: Client Shell

**Goal:** A user can open the app, onboard once, land on a Home hub over the animated cosmic background, and pick which hand to scan, with everything persisted across restarts.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: FND-03, ONB-01, ONB-02, ONB-03, ONB-04, ONB-05, ONB-06, HOME-01, HOME-02, HOME-04, SCAN-01
**Success Criteria** (what must be TRUE):
  1. On first launch the user sees the branded splash, picks English or Hindi (the UI switches language), pages through 3 intro screens with Next / Get started, enters a first name (at least 2 letters) with optional gender, enters a birth date with an optional birth time or "I don't know my birth time", and arrives on Home.
  2. After the app is killed and relaunched, the user goes from splash straight to Home with name, language and birth data intact and onboarding is not shown again; the profile is excluded from backup, and cold start to first frame is under 2 s on a release build on the reference phone.
  3. Home shows a time-of-day greeting with the user's name, the hero card and the feature grid; tiles outside milestone 1 open a "coming soon" state; pressing Back on Home asks to confirm leaving the app.
  4. Onboarding and Home show the layered animated cosmic background without visible jank on the reference phone; it is static when system animations are off, and the scan and generating screens opt out of it.
  5. From the hero card the user chooses left or right hand and sees photo tips (one good example and four bad examples) before continuing to the scanner; the chosen hand is carried forward.

**Plans:** 4/5 plans executed

Plans:
- [x] 03-01-PLAN.md — Tracer: consent for all new artifacts, then splash -> Language (en/hi switch in place) -> Home, persisted across a relaunch on the Redmi; profile repository, routes, AppCompat locale, test fixtures, tools/ui.sh + tools/device-lock.sh (wave 1)
- [x] 03-02-PLAN.md — Nocturne visual layer: six-layer draw-phase sky (static under reduce motion / battery saver), Phase 3 type scale on every M3 slot, the 20 drawables (wave 1, no phone)
- [x] 03-03-PLAN.md — Onboarding steps: 3 intro pages, first name + gender with validation, birth date / optional time / "I don't know", saves, prefill and the Back chain (wave 2)
- [x] 03-04-PLAN.md — Home hub and hand choice: greeting, hero, 7-tile grid, coming-soon sheet route, exit confirm, hand selection with photo tips, Scan(hand) placeholder without sky, first on-device sky motion check, debug-only showcase (wave 2)
- [x] 03-05-PLAN.md — Phase gate on the Redmi: release-type cold start < 2 s, motion (animations off/on, battery saver) and frame pacing, full instrumented suite, clean release APK, en/hi screenshot walk, user visual sign-off (wave 3)

**UI hint**: yes

### Phase 4: Scan Loop

**Goal:** The user points the phone at a palm, gets stable live guidance, and captures or imports a photo that is processed and ready to upload, running smoothly on the reference low-end phone.
**Mode:** mvp
**Depends on**: Phase 3 (hand selection is the entry point and supplies the hand side; the camera work itself needs only Phase 1)
**Requirements**: SCAN-02, SCAN-03, SCAN-04, SCAN-05, SCAN-06, SCAN-07
**Success Criteria** (what must be TRUE):
  1. The user is asked for camera permission; if denied they see an explanation with "Open settings" and "Choose from gallery", and if the camera is unavailable they land on a gallery fallback instead of a dead end.
  2. With both hands on the real phone, live guidance reaches and holds each state - back of hand, closed hand, too far, moving, palm detected - and stays steady (no flicker) while the hand is held still; the guidance logic passes unit tests against committed recorded-landmark fixtures.
  3. The capture button works only while a palm is detected, tap-to-focus runs before capture, and the flash toggle turns the torch on and off.
  4. A captured or imported photo ends up upright, cropped to the palm, at most 1280 px on the long edge, with no EXIF or GPS data (verified on the output file, including an orientation-6 photo and a 100 MP or HEIC import), the 21 landmarks from the processed photo are ready to send, and a gallery photo with no hand is rejected on the device with a clear message.
  5. A 2-minute scan session on the reference low-end phone finishes with no ANR, no visible jank and no overheating or thermal throttling.

**Research flag**: Needs on-device calibration: palm-facing and handedness thresholds (rear-camera mirroring), analyzer performance on the reference phone, and which MediaPipe release ships 16 KB-aligned native libraries. Prerequisite: the same consented palm photo set (including deep skin tones, henna, indoor light) and recorded landmark fixtures (left/right x palm/back/fist/far/rotated).
**Plans**: TBD
**UI hint**: yes

### Phase 5: Result Against the Fixture

**Goal:** The user sees a believable Palm / Love / Career reading with the detected lines drawn over their own photo, saved on the device and shareable, built and verified against fixture data before the real backend is wired.
**Mode:** mvp
**Depends on**: Phase 3 (Home and hand selection for reopen and scan again; contract and theme from Phase 1)
**Requirements**: RES-01, RES-02, RES-03, RES-04, RES-05, RES-06, RES-07, READ-03, HOME-03
**Success Criteria** (what must be TRUE):
  1. With fixture data the result opens on Palm, Love and Career tabs that the user can switch between, and the detected lines are drawn over the user's own photo with a colour and text legend, correct on three photo aspect ratios and for every `trace_source` (a line with a `llm` or `template` source is labelled approximate, one with `none` shows its card without an overlay).
  2. The user sees a card per detected line (Heart, Head, Life, Fate, Sun) with a short label and interpretation, and an entertainment disclaimer is visible on every reading at readable contrast.
  3. For each of the 7 unusable-photo reasons (blurry/dark, too far, jewellery, bent, fingers too tight, incomplete, no palm lines) the user sees that reason with a retake tip and a retake button; the attempt is not saved as a reading and does not count as a completed scan.
  4. A reading (photo and JSON) is saved on the device and still opens after the app is killed and relaunched, and Home lets the user reopen their last reading.
  5. The user can share the reading as a branded image card through the system share sheet, can start another scan from the result, and "Ask [Guide]" shows a coming-soon state.

**Plans**: TBD
**UI hint**: yes

### Phase 6: Wire-Up & Ad Gates

**Goal:** A real palm on the real phone produces a real reading with its overlay in under 30 seconds, every wait and failure state is handled, and repeat scans sit behind a stubbed rewarded gate while the first reading stays ad-free.
**Mode:** mvp
**Depends on**: Phase 2, Phase 4, Phase 5
**Requirements**: READ-01, READ-08, READ-09, ADS-01, ADS-02, ADS-03
**Success Criteria** (what must be TRUE):
  1. On the physical phone the user scans a real palm, passes through a generating screen with progress and cancel, and gets a real reading with line overlay from the local backend; the photo, hand side and 21 landmarks are uploaded as a job and polled for the result under a 45-second overall deadline.
  2. The generating screen handles a busy backend, offline mode (airplane mode mid-upload) and a timeout, each with a clear message and a working retry, and an app killed during generating recovers cleanly.
  3. Across at least 10 consecutive real scans on the reference phone, scan-to-reading time is under 12 s at p50 and under 30 s at p95.
  4. A first-time user sees no ad slot or ad prompt before or during their first reading, and retaking after an unusable-photo result does not trigger the ad gate.
  5. From the second reading on, "Scan again" carries an AD chip and opens the "Watch a short ad to unlock" sheet, the stubbed reward unlocks the scan, and the failure simulations (ad fails to load, never closes, gives no reward) leave the user able to retry or continue; ad slots and the gate sit behind interfaces so a real ad SDK can replace the stub without changing screens.

**Plans**: TBD
**UI hint**: yes

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6. After Phase 1, Phase 2 (backend) is independent of Phases 3-5 (client) and can run in parallel; Phase 6 starts only when Phases 2, 4 and 5 each work on their own.

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundations & Contract | 5/5 | Complete    | 2026-11-26 |
| 2. Reading Pipeline | 7/9 | In Progress | - |
| 3. Client Shell | 5/5 | Complete (hardware perf gate deferred) | 2026-11-27 |
| 4. Scan Loop | 0/TBD | Not started | - |
| 5. Result Against the Fixture | 0/TBD | Not started | - |
| 6. Wire-Up & Ad Gates | 0/TBD | Not started | - |
