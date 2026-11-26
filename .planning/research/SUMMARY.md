# Project Research Summary

**Project:** [AppName] - AI Palm Reading & Astrology (Android)
**Domain:** Consumer AI palmistry/astrology entertainment app (Kotlin/Compose client, FastAPI + Claude vision backend, ads-only, India/SEA/LatAm on mid/low-end phones)
**Researched:** 2026-11-26
**Confidence:** MEDIUM-HIGH (versions and APIs verified against primary sources; the Core Value path depends on three empirical unknowns that only the device and an eval set can settle)

## Executive Summary

This is a camera-first consumer app whose whole bet is one moment: scan a palm live, get a believable Palm/Love/Career reading with detected lines drawn over the user's own photo, in under 30 s. Experts build this as an on-device perception layer (CameraX + MediaPipe Hand Landmarker driving a pure-Kotlin guidance state machine) plus a thin server-side LLM pipeline (FastAPI, async job, Claude vision with schema-constrained output). Everything else (tarot, horoscope, Vedic chart, chat, ads SDKs) is breadth that must not delay proving that moment. Milestone 1 is therefore one vertical slice, built fake-first on the client and eval-first on the server.

Recommended approach: single `:app` module, package-by-feature, Room as the single source of truth for readings (navigation passes `readingId` only), a versioned JSON contract frozen on day one, poll as the job contract, and ad/usage gates as interfaces with stubs. The three things that can sink the Core Value are (1) LLM polyline quality, (2) hand-guidance reliability on low-end hardware, and (3) latency/cost of the vision call. Attack all three in isolation (eval harness, landmark fixtures, bake-off) before wiring anything. The contract uses normalized 0..1 trace coordinates with a `trace_source` field so the overlay survives any resize and a trained model can replace the LLM later without a client change.

Key risks and mitigations: LLM lines will be imprecise, so validate against client-sent landmarks and fall back to landmark-anchored templates, labelled "approximate". Handedness/palm-vs-back is mirrored on the rear camera, so use geometry + handedness + selected hand, with hysteresis, calibrated from recorded fixtures. The 5.x Claude models reject sampling params, so determinism comes from a server cache, not temperature. Privacy ("image deleted after processing") must be true by construction (memory only, no logging) and honest in the Data safety form. Reading content needs an output filter and a report control, not just a prompt.

## Resolved and Flagged Conflicts Between Researchers

Each item states the decision this summary makes. Items marked **SIGN-OFF** need the user before the affected phase starts.

| # | Conflict | Positions | Decision |
|---|----------|-----------|----------|
| 1 | MediaPipe running mode | STACK + PITFALLS: `LIVE_STREAM` + `detectAsync`. ARCHITECTURE: `VIDEO` with synchronous `detectForVideo` on the dedicated analyzer thread | **Start with `VIDEO` sync on the analyzer thread** behind a `HandLandmarkerHelper` seam. CameraX `KEEP_ONLY_LATEST` already gives backpressure, there is no listener/ordering complexity, and it is easy to unit-test. Switch to `LIVE_STREAM` only if the Redmi soak test shows a problem. Both modes share the tracking caveat (a wrong handedness can persist), so hysteresis is required either way. Use `IMAGE` mode on the captured still and on gallery imports (all three researchers agree). |
| 2 | SSE vs polling for the palm job | STACK: FastAPI 0.165 has built-in SSE, ship it. ARCHITECTURE: poll is the contract of record, defer SSE. PITFALLS: poll baseline, SSE enhancement | **Poll is the M1 contract** (`POST /v1/palm/jobs` -> 202, `GET /v1/palm/jobs/{id}`, 1 -> 3 s backoff, 45 s deadline). Claude's call is non-streaming, so SSE adds no real progress, and poll survives proxies, dropped connections and process death. **Do not add `okhttp-sse` or FastAPI SSE in M1.** Build SSE once, for chat, in the chat milestone, and test built-in disconnect cleanup then (STACK open risk #4 moves there). PROJECT.md says "SSE/poll", so no requirement change. |
| 3 | Navigation 3 vs typed navigation-compose | STACK: Navigation 3 1.2.2 (stable). PROJECT.md/PRD: Navigation-Compose typed (2.10.2) | **Recommend Navigation 3 1.2.2** behind `core/navigation` (owned back-stack list suits onboarding reset and Scan -> Generating -> Result replacement). Fallback is `navigation-compose` 2.10.2 with `@Serializable` routes and identical Hilt wiring. **SIGN-OFF** because it deviates from the PROJECT.md constraint. Cost of reversing later is low if routes stay `@Serializable` and screens stay stateless. |
| 4 | Room 3 vs Room 2 | STACK: Room 3.0.3 (`androidx.room3`, KSP-only). PROJECT.md says only "Room" | **Recommend Room 3.0.3**, gated by the Phase 1 scaffold build. ARCHITECTURE stores each reading as one JSON-document column, so the choice is low-stakes; if the scaffold gate fails, fall back to Room 2.8.5 immediately. Never mix the two. **SIGN-OFF** bundled with Nav3. |
| 5 | Upload size | ARCHITECTURE: pre-resize to long edge <= 1280. STACK: server downscales to ~1200-1600. PITFALLS: crop to palm ROI, long edge 1024-1568. PRD R-PALM-2: >= 1080 short side | **The client does the resize/crop; the server never upscales and only caps at 1568 long edge as a defence.** Capture at >= 1080 short side (R-PALM-2 satisfied at capture), then crop to palm bbox + ~15% margin (from IMAGE-mode landmarks), cap long edge at 1280 (start value; the bake-off may test 1568), JPEG q85, EXIF gone by re-encode, target 300-500 KB. `image_size` is the size actually sent to Claude. **The saved local photo must be that same processed image**, so normalized coordinates map 1:1 with no crop-rect bookkeeping. **SIGN-OFF**: deviation from R-PALM-2's "1080 short side" for the upload. |
| 6 | What drives the overlay: LLM polylines or landmark templates | ARCHITECTURE: LLM trace validated, template fallback. PITFALLS: landmarks are ground truth, LLM only picks parameters | **Layered ladder, eval decides the default:** validated LLM trace -> landmark-anchored template -> empty trace (card without overlay), tracked by `trace_source` = `llm|template|none`. Template fallback is **mandatory to build** (expected to fire often). If the Phase 2 eval shows < ~70% of LLM lines passing hull/geometry validation, flip the default to template-first with the LLM supplying only qualitative parameters (long/short, curved, broken/forked). Always show an "approximate lines" note. Landmark-driven templates require landmarks to reach the server (see sign-off list). |
| 7 | Uninstall-intercept screen (S35) | FEATURES: anti-feature, skip. Nothing else covers it | **Drop it.** Not in M1 anyway; record in PROJECT.md Out of Scope (fragile, near-policy edge, low value). Use a capped honest win-back notification in the retention milestone. **SIGN-OFF** (one line). |

Additional conflicts found during synthesis:

| # | Conflict | Decision |
|---|----------|----------|
| 8 | Determinism: STACK says no `temperature` (400 on 5.x); PITFALLS suggests temperature 0-0.3 and perceptual hash | **Never send `temperature`/`top_p`/`top_k`/`budget_tokens`.** Determinism = server-side cache keyed by dHash/pHash of the normalized palm image (small Hamming threshold) + hand + locale + `PROMPT_VERSION`, JSON only, never the image. Plain SHA-256 alone gives a near-zero hit rate across re-captures. Keep SHA-256 only for idempotency/single-flight on identical bytes. Add "view your last reading" on the client as the primary UX answer. **SIGN-OFF**: perceptual hash vs PRD R-PALM-7's "image hash for 24 h". Keep 24 h TTL for M1. |
| 9 | Palm model default: STACK Sonnet 5.5; ARCHITECTURE leans Haiku-class | `PALM_MODEL` env var, default `claude-sonnet-5-5`, **bake-off vs `claude-haiku-5-5` (+ `claude-opus-5-5` as quality ceiling only) on 20-30 real palm photos**; set `output_config.effort` explicitly (`low`). Gate: p50 < 12 s, p95 < 30 s, line plausibility, cost. **SIGN-OFF** on the winner (cost + latency trade). |
| 10 | Python version: STACK 3.13 container / 3.14 local OK; ARCHITECTURE + PITFALLS warn on 3.14 wheels | **Pin 3.13 with `uv python pin` from day one.** Dev machine is 3.14; do not debug wheel gaps. |
| 11 | Body cap: STACK <= 8 MB; ARCHITECTURE <= 2 MB | **2 MB** (client sends ~300-500 KB), plus Pillow pixel cap and JPEG magic-byte check. |
| 12 | Kotlin 2.4.21 + Hilt 2.60.1 + KSP 2.3.14 + Room 3 + AGP 9.5.3 may not build together | **First task of Phase 1 is a green scaffold build of exactly this set.** Fallback: Kotlin 2.3.21 (still supported by KSP 2.3.14 and Room 3.0.x). |

## Decisions Requiring User Sign-Off (carried forward)

1. **minSdk 26** (targetSdk 36, compileSdk 37). Library floor is 24; 26 gives native `java.time`, no desugaring, and is far below the reference app's 30. Verify in Play Console "Reach and devices" after first release; drop to 24 only if > ~2% of target users are on API 24-25 (then add core-library desugaring).
2. **Contract change vs PRD R-PALM-4:** trace coordinates are **normalized 0..1** (not pixels) and each line carries **`trace_source`** (`llm|template|none`); readings are Pydantic-validated, schema_version 1, with `image_size` = size sent to Claude. Invalid photo is a *successful* job with `verdict.ok=false` (7-reason enum); HTTP/job failure is infrastructure only.
3. **Landmarks sent to the server transiently:** the client uploads the 21 normalized hand landmarks (re-run in IMAGE mode on the processed still) in the job `meta`. Used only for polyline validation and template fallback; never stored or logged. This is hand-geometry data, so the privacy notice (S07/S08), privacy policy and Data safety form must say so. Alternative if refused: run a server-side landmark pass (heavier dependency) or ship LLM-only traces with no validation (not recommended).
4. **Upload size deviation from R-PALM-2** (see conflict 5) and **perceptual-hash cache** (conflict 8).
5. **Palm-model bake-off** and model default (conflict 9).
6. **Nav3 + Room 3** (conflicts 3, 4) and **drop S35** (conflict 7).
7. **Small M1 scope additions the research recommends:** image-hash result cache (R-PALM-7), report-reading control on S10 (Play AI-content policy, PRD S38), one-line photo privacy notice, age screen folded into S04 (13+ gate; birth date stays on-device), birth time optional with "I don't know", reopen-last-reading from Home (no full history list), no-ads-in-first-reading rule enforced in `AdPolicy`. Share card only if the slice has slack. **Open question for the user:** capture birthplace + IANA timezone in S05 now (PITFALLS #21, avoids re-collecting for the Vedic milestone) or defer.

## Key Findings

### Recommended Stack

Toolchain: AGP 9.5.3 (built-in Kotlin; do NOT apply `org.jetbrains.kotlin.android`; never kapt) + Gradle 9.6.0 + JDK 17 + Kotlin 2.4.21 + KSP 2.3.14. Compose BOM 2026.11.01. Versions were read from Google Maven/Maven Central/PyPI on the research date; copy-ready catalog is in STACK.md.

**Core technologies:**
- **CameraX 1.7.0** (`LifecycleCameraController`, Preview + ImageAnalysis + ImageCapture, one 4:3 ViewPort): shared field of view prevents overlay drift; RGBA_8888 analysis, `KEEP_ONLY_LATEST`, ~640x480, 10-15 fps cap.
- **MediaPipe tasks-vision 1.2.3** (pinned, never `latest.release`) with bundled `hand_landmarker.task` (7.8 MB, `noCompress`), `numHands=1`, **CPU delegate**: only on-device ground truth for guidance and overlay validation. Palm-vs-back is geometry (cross product of wrist->index MCP and wrist->pinky MCP) + handedness (mirrored on rear camera) + selected hand; calibrate on the phone.
- **Hilt 2.60.1 / androidx.hilt 1.4.0, Room 3.0.3 (JSON-document column), DataStore 1.3.0, OkHttp 5.5.1 (direct small ApiClient, no Retrofit, no okhttp-sse in M1), kotlinx.serialization 1.13.0, Coroutines 1.11.0, Coil 3.6.3, Navigation 3 1.2.2**.
- **FastAPI 0.165.0 + Uvicorn 0.54.0 + Pydantic 2.14.4 + anthropic 2.14.0 + Pillow 13.0.0 + python-multipart 0.0.34**, Python 3.13: plain-asyncio job runner (`create_task` + Semaphore + `asyncio.timeout`), in-memory `JobStore`/`ReadingCache` Protocols; no Celery/Redis/WebSockets in M1.
- **Claude:** `AsyncAnthropic.messages.parse(output_format=PydanticModel)`, `output_config.effort="low"`, check `stop_reason` (refusal/max_tokens) before parsing, key only in server env. JSON-schema limits (no min/max, no maxItems, no oneOf) mean all range/count checks live in Pydantic.

Targets: minSdk 26 (sign-off), targetSdk 36 (Play requirement from 2026-08-31; edge-to-edge enforced, build it in from day one), compileSdk 37, ABIs armeabi-v7a + arm64-v8a only.

### Expected Features

**Must have (M1 table stakes):**
- Live scan with guidance (frame, hand-detected states, flash, gallery, camera-denied/unavailable) - the category hook
- Photo-quality rejection with a reason (on-device: too far/back of hand/fist/motion; LLM verdict: blur, jewelry, bent, tight, incomplete, no lines) plus a corrective hint and retake CTA per reason
- Palm / Love / Career reading with Heart/Head/Life/Fate/Sun line cards and lines drawn over the user's own photo
- Entertainment disclaimer on every reading; privacy notice before upload; report-this-reading control
- Onboarding S01-S05, Home S06 with coming-soon tiles, hand select S07; offline/busy/timeout states; en + hi minimum with `locale` passed to the LLM
- Saved reading in Room (EXIF stripped, server deletes image), reopen last reading

**Should have (differentiators, cheap in M1):**
- Under-30 s scan-to-reading, no paywall; 24 h/perceptual-hash result cache (stable readings for the same photo - a top competitor complaint)
- Overlay on the user's own photo tied to line cards; locale-native readings (not translate-after)
- Ad/usage-gate interfaces with stubs and the first-reading-ad-free rule

**Defer (v2+ / later milestones):**
- Daily report, love test, tarot, horoscope pipeline (retention), Vedic chart/moon sign/soul energy, AI chat with `PalmContext`, real ads + UMP + caps, notifications, rating flow, localisation tier 1, share card (if no slack), saved-readings list, left/right comparison, palmistry glossary, extended palmistry fields, trained palm-line model, kundli matching/panchang, iOS
- Anti-features, stay out: paywall/IAP, lock-screen, fake "secret in your palm" notifications, uninstall intercept (S35), interstitial on Back, rewarded pods of 3, human astrologer marketplace, medical/death/pregnancy predictions, server-side palm image storage, face reading/accounts, copying anything from `_extract/`

### Architecture Approach

Single `:app` module, package-by-feature, one-way dependencies (`feature -> core`, never `feature -> feature`), no use-case layer. Scan and palm are separate features coupled only by a `PendingScan` (file + hand side + landmarks) in a `ScanSession` singleton. Room is truth; screens observe it. `FakePalmRepository` (sample JSON from `contracts/`, 3 s delay, toggleable busy/invalid/offline) is built before the remote one so Result/Generating/overlay and every error state are testable first. Backend: `api/` -> `jobs/` (JobStore, ReadingCache, runner) -> `services/palm_reader` (image_prep -> cache -> claude_client -> Pydantic validate + one retry -> line_geometry -> assemble `PalmReading v1`). Dev networking: `adb reverse tcp:8000`, `127.0.0.1` (not `localhost`), debug-only cleartext config, no `uvicorn --reload` during job tests.

**Major components:**
1. `feature/scan` - CameraX session, `HandLandmarkerHelper`, pure `GuidanceEvaluator` + hysteresis (enter READY after 500 ms still; leave after 2 failing frames; non-READY hold >= 300 ms), capture, gallery import, `ImageProcessor` (rotate, crop, resize, strip EXIF, hash)
2. `feature/palm` - domain/data/UI together: `PalmRepository` (Fake/Remote), DTO mappers, Room, Generating S09, Result S10, `LineOverlay` (maps normalized points into the displayed image rect, Catmull-Rom path)
3. `core/designsystem` (generated `NocturneTokens.kt` via `tools/gen_tokens.py`, no animated background on scan/generating screens), `core/ads` (`AdGate`, `AdPolicy`, `UsageGate`, `AdSlot`, stubs incl. fail/never-closes/no-reward simulations)
4. Backend `palm_reader` pipeline + pure `line_geometry` (4-24 points, in-hull >= 80%, length/reversal checks, heart-vs-head sanity, template fallback in a hand-local frame) + eval harness (`evals/run_eval.py`)

### Critical Pitfalls

1. **Trusting LLM polylines** (the Core Value screen) - treat as hints; validate against landmarks; template fallback; normalized coordinates; pre-resize and tell the model the exact size; decide strategy before building the overlay (retrofit = rewrite).
2. **Palm-vs-back flicker and rear-camera handedness inversion** - geometry + corrected handedness + selected hand, hysteresis, pure-Kotlin state machine tested against recorded landmark fixtures (left/right x palm/back/fist/far/rotated), re-validate the high-res still in IMAGE mode before upload.
3. **Low-end frame analysis (Redmi/Realme/Galaxy A)** - 640x480 RGBA, KEEP_ONLY_LATEST, reused bitmap, always `close()` ImageProxy, CPU delegate, 10-15 fps, 2-minute soak test on the reference phone (analyzer p95 < 60 ms, no thermal throttling) as a success criterion.
4. **EXIF stripped before orientation is baked in; gallery surprises** - decode, apply rotation to pixels, crop, resize, re-encode (never "remove tags"); Photo Picker with bounded `inSampleSize`; orientation-6 test; 100 MP and HEIC import test.
5. **Latency/cost/abuse and privacy** - 1280 px upload, one call, schema-constrained, tight `max_tokens`; per-install token + rate limits + spend cap before any public URL; server rewarded-gate ceiling; image bytes in memory only, never logged, `finally` deletion tests; honest Data safety form; backup exclusion; no biometric templates.

Also carried: unsafe content/injection (never-list, denylist in 5+ languages, text-in-image is data, report flag, red-team set), non-deterministic readings, Play release gates (targetSdk 36, 16 KB page alignment of MediaPipe `.so`, closed-testing calendar cost for new personal accounts), ads policy (interface must model consent/readiness/frequency/failure), Compose background cost, camera lifecycle/OEM process kill (persist `pendingJobId`), single-process job store (Cloud Run CPU throttling after response; `--max-instances=1` + CPU always allocated at deploy).

## Implications for Roadmap

Suggested M1 build order mapped to phase labels (P-Shell, P-Scan, P-Pipeline, P-Result, P-Harden, used throughout PITFALLS.md). The roadmapper may merge or rename, but the dependencies hold: contract first; backend pipeline and client shell independent; scan loop independent of both and carries the most device risk; wiring needs all three; hardening last.

### Phase 1: Foundations & Contract (P-Shell part 1)
**Rationale:** Everything depends on the frozen contract and a build that works; the toolchain stack is the newest-possible combination and is the first risk.
**Delivers:** repo layout (`android/`, `backend/`, `contracts/`), Gradle + version catalog green with Hilt/KSP/Room 3/Nav3 (**gate task one**, fallback Kotlin 2.3.21/Room 2/Nav2), `NocturneTheme` + `gen_tokens.py`, typed nav skeleton, edge-to-edge at targetSdk 36, backend `uv` project pinned to Python 3.13 + `/healthz`, `tools/dev-reverse.sh`, debug cleartext config, `.env` git-ignored, **`palm_reading.v1.schema.json` + `palm_reading.sample.json`**, quoted-path build check (repo path has a space).
**Avoids:** Pitfalls 11, 12, 26; toolchain surprise; contract drift.
**Exit gate:** app launches on the phone; `GET /healthz` reachable from the phone via `adb reverse`.

### Phase 2: Reading Pipeline - Spike, Eval, then Job API (P-Pipeline)
**Rationale:** Highest-uncertainty, can run unattended and in parallel with Phases 3-5; its eval results decide model, prompt, geometry thresholds and whether templates are the default. Start the spike first, the job API second.
**Delivers:** `image_prep` (orient, strip, <=1280, dHash, blur/brightness pre-gate), `claude_client`, Pydantic schemas, validation + one retry, safety output filter + never-list prompt, `line_geometry` + `line_templates`, eval harness (10-30 consented photos, gitignored; latency p50/p95, cost, % lines valid, 5x-repeat enum agreement), then routes, `JobStore`/`ReadingCache`, runner, idempotency, single-flight, perceptual-hash cache, per-install token + rate limits + spend cap, API tests with Claude mocked.
**Uses:** FastAPI, anthropic `messages.parse`, Pillow, asyncio.
**Avoids:** Pitfalls 1, 5, 6, 7, 8, 9 (server side), 16, 17, 19.
**Exit gates:** eval table picks model + prompt version and answers "template fallback default?"; `curl` end to end; retried POST returns same job; identical photo is a cache hit; no image remains on disk/in logs after success/failure/timeout.

### Phase 3: Client Shell (P-Shell part 2)
**Rationale:** Needs only Phase 1; makes the app presentable and carries onboarding data that downstream modules need.
**Delivers:** Splash, Language (en + hi), Intro, Name/Gender (+ age screen), Birth (time nullable "I don't know", place/tz decision), Home S06 (coming-soon tiles, reopen last reading), HandSelect S07 (4 bad-example chips + privacy notice), `NocturneBackground` (draw-phase only, paused/static on camera and generating screens, reduce-motion aware), `ProfileRepository` (DataStore), `allowBackup` exclusions.
**Avoids:** Pitfalls 14, 21, 23.
**Exit gate:** first-run path to Home, state survives restart, cold start < 2 s on a release build on the reference phone.

### Phase 4: Scan Loop (P-Scan)
**Rationale:** Most device-dependent risk; independent of backend. Fixtures-first removes the camera from the tuning loop.
**Delivers:** permission/denied/unavailable states, CameraX + `HandLandmarkerHelper` (VIDEO, CPU), `GuidanceEvaluator` pure class + debug "record landmarks" action + committed fixtures, state-machine UI, tap-to-focus then capture (auto-capture off), `ImageProcessor`, IMAGE-mode landmark pass on the still/gallery (zero hands -> local "no palm"), Photo Picker import, on-device quality checks calibrated on a labelled set incl. deep skin tones/henna/indoor light.
**Avoids:** Pitfalls 2, 3, 4, 5, 13, 15.
**Exit gate:** all 5 guidance states reachable and stable with both hands on the real phone; fixtures committed and unit-tested; 2-minute soak with no ANR/jank/thermal throttling; orientation-6 and 100 MP/HEIC tests pass.

### Phase 5: Result Against the Fixture (P-Result)
**Rationale:** UI polish can proceed on the fake repository while Phases 2 and 4 are unresolved.
**Delivers:** `FakePalmRepository`, Room entity (JSON column, schema_version, ~1080-1280 px stored photo, delete removes row + file, exported schema + first migration test), Result S10 (Palm/Love/Career tabs, `LineOverlay` with tested transform utility, line cards, per-reason invalid-photo screen with retake hint, always-visible disclaimer at 4.5:1, report control sending reading id), `AdSlot` no-op.
**Avoids:** Pitfalls 1 (renderer half), 13, 20, 22.
**Exit gate:** correct on sample JSON for all `trace_source` variants and every invalid reason; overlay screenshots on 3 aspect ratios.

### Phase 6: Wire-Up (P-Result/integration)
**Rationale:** Needs 3 (scan), 4 (result UI) and 5 (server) each working independently.
**Delivers:** `RemotePalmRepository` (upload with idempotency key + landmarks meta, poll with deadlines, 25 s upload/10 s poll/45 s overall), Generating S09 (busy/offline/expired/timeout, staged copy, cancel), optional `pendingJobId` resume, swap Fake for Remote via `USE_FAKE_BACKEND`.
**Exit gate:** a real palm on the phone yields a real reading with overlay in < 30 s, repeatably (p50 < 12 s target).

### Phase 7: Stubs & Hardening (P-Harden)
**Rationale:** Last, but several items (privacy checklist, policy) must start earlier.
**Delivers:** `AdGate`/`AdPolicy`/`UsageGate`/`AdSlot` stubs with failure simulations + first-reading rule + rewarded gate on "Scan again", disclaimer audit, delete-after-processing and logging audit, red-team safety set, manifest audit (no `USE_FULL_SCREEN_INTENT`/`SYSTEM_ALERT_WINDOW`), clean-room sweep, minified **release** build end to end (R8 vs MediaPipe/serialization), 16 KB alignment check, internal-track upload, Data safety draft, closed-test start, ARTEMIS end-to-end run and error-state sweep (airplane mid-upload, 429/529, process death, call interrupt), Baseline Profile.
**Avoids:** Pitfalls 7 (abuse), 8, 9, 10, 11, 24, 25.
**Exit gate:** every PRD M1 checkbox demonstrable on the device.

### Phase Ordering Rationale
- The contract (Phase 1) unlocks parallel work; the Fake repository decouples UI from the two unresolved risks.
- Risk-first: LLM line quality (Phase 2) and guidance reliability (Phase 4) are attacked in isolation before wiring.
- Phase 2 can start immediately after Phase 1 and run in parallel with Phases 3-5 for a solo developer; do not start Phase 6 until 4 and 5 independently work.
- Cross-cutting items must not wait for Phase 7: privacy no-image-at-rest tests (Phase 2), backup exclusion (Phase 3/5), edge-to-edge/targetSdk (Phase 1), report control (Phase 5).

### Research Flags

Needs deeper research or a spike during planning (`/gsd-plan-phase --research-phase`):
- **Phase 2:** model choice and polyline quality are empirical (needs consented photo set, bake-off); also verify Anthropic data-retention terms and Play AI-Generated-Content policy wording; multilingual denylist.
- **Phase 4:** handedness/palm-facing semantics, thresholds and performance on a Redmi-class phone cannot be settled on paper; verify which MediaPipe release ships 16 KB-aligned `.so`.
- **Phase 1:** scaffold gate for the Kotlin 2.4/Hilt/KSP/Room 3/AGP 9.5 combination (a spike, not a research phase).
- **Phase 7:** Play Data safety form, closed-testing numbers, DPDP/age-of-child rules need legal review before release.

Standard patterns (skip research-phase):
- **Phase 3:** onboarding, DataStore, nav, Compose theming are well-documented (except the draw-phase animation budget, a known rule).
- **Phase 5:** Room, Compose tabs, canvas overlay are standard once the transform utility is unit-tested.
- **Phase 6:** polling client with OkHttp is routine.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH (versions/compat) / MEDIUM (3 judgement calls) | Versions read from primary registries on 2026-11-26. MEDIUM on Nav3, Room 3, palm model tier, and minSdk 26 (Play Console reach not verified). Open risk: Hilt vs Kotlin 2.4 metadata. |
| Features | MEDIUM | PRD and first-hand device check are strong; competitor evidence is thin (iOS listings and review aggregators only, no Play data). |
| Architecture | MEDIUM-HIGH | Claude/structured-output facts verified; MediaPipe/CameraX wiring, thresholds, `adb reverse`, Cloud Run behaviour rest on docs plus judgement and need on-device calibration. |
| Pitfalls | MEDIUM-HIGH | Engineering pitfalls well established; policy/legal items (Play AI-content wording, DPDP, 16 KB dates, Anthropic retention) are partly unverified. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **LLM polyline quality and model tier:** unknown until the Phase 2 eval. Handle with the eval gate; keep the contract and fallback ladder model-agnostic.
- **Guidance thresholds and rear-camera handedness sign table:** calibrate on the physical phone; commit landmark fixtures. Hysteresis from day one.
- **minSdk 26 reach:** check Play Console after first release; desugaring is a trivial later add if dropping to 24.
- **Hilt + Kotlin 2.4.21 compatibility:** verify in the Phase 1 scaffold; fall back to Kotlin 2.3.21.
- **Built-in FastAPI SSE disconnect cleanup:** unverified; moved to the chat milestone (write one test, else `sse-starlette`).
- **Birthplace/IANA timezone capture in S05:** needs a user decision.
- **Policy/legal items unverified:** Play AI-Generated Content policy, India DPDP age-of-child and consent, Anthropic API data retention, new-account closed-testing numbers, 16 KB cutoff date. Verify in Phase 7 research; do not make unverifiable privacy promises.
- **Cloud Run deploy** (CPU always allocated, `min=max=1`, or externalise the job store) is a later-milestone concern; the `JobStore`/`ReadingCache` Protocols are the only seam M1 builds.
- **Python 3.14 wheel coverage** is moot once pinned to 3.13.
- **Android Studio version** must support AGP 9.5 if the IDE is used (CLI builds unaffected).
- **No consented palm photo set exists yet:** acquiring 20-40 labelled photos (skin tones, henna, rings, kids/elderly, blur, backlight) is a prerequisite for Phase 2 and Phase 4 calibration; keep out of git.

## Sources

### Primary (HIGH confidence)
- Android Jetpack release index, Google Maven and Maven Central metadata, AGP 9.5 and built-in-Kotlin migration guides, KSP, Room 3, CameraX 1.7 release notes (retrieved 2026-11-26)
- MediaPipe Hand Landmarker Android guide (options, result fields, mirrored-handedness note; model file size verified by headers)
- Anthropic docs: Vision (image limits, resize behaviour, coordinates), Structured outputs (json_schema limits, `messages.parse`), claude-api skill (model IDs/pricing, sampling-param removal, `output_config.effort`, refusals)
- Play Console help: target API 36 from 2026-08-31, Data safety, Ads and Families policies; FastAPI SSE docs
- PyPI JSON API for all backend pins
- Project: `replica/PRD.md` (R-PALM-1..8, section 16 on-device findings), `.planning/PROJECT.md`

### Secondary (MEDIUM confidence)
- Google Play AI-Generated Content policy (in-app flagging requirement); 16 KB page-size developer page (date inconsistent across sources)
- Engineering knowledge: CameraX orientation/EXIF behaviour, OkHttp timeouts, cleartext defaults, `adb reverse`, Windows firewall profiles, Compose draw-phase reads
- Palm-vs-back geometry method (inference; validate on fixtures)

### Tertiary (LOW confidence, needs validation)
- iOS App Store palm-reader listings and review aggregators (JustUseApp, MWM): sentiment clusters (expensive subscriptions, vague/inconsistent readings)
- Cloud Run CPU-throttle behaviour and Python 3.14 wheel coverage (not re-verified)
- Android platform distribution shares (no usable data returned); India DPDP specifics; Anthropic retention terms

Detailed findings: `STACK.md`, `FEATURES.md`, `ARCHITECTURE.md`, `PITFALLS.md` in `.planning/research/`.

---
*Research completed: 2026-11-26*
*Ready for roadmap: yes (after the sign-off items above are confirmed or defaulted to the stated recommendations)*
