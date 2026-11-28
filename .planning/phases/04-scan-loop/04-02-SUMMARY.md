---
phase: 04-scan-loop
plan: 02
subsystem: android-client
tags: [guidance, mediapipe, stabiliser, camerax, focus, torch, compose-canvas, fixtures]
status: complete
requires:
  - "04-01 frozen interfaces: P3, HandFrame, GuidanceState, LiveHandTracker, ScannerContent, CameraProblem, PhotoSource.Capture, ScannerViewModel tracer API"
provides:
  - "core/vision: GuidanceConfig (every threshold), VisibleRect, GuidanceReading, HandGuidance, GuidanceStabilizer, StableGuidance, FillCenterMapping"
  - "core/camera: ScanCamera, CameraXScanCamera"
  - "feature/scan: ScannerUi (shown, captureEnabled, wrongHand, capturing, captureFailed, focusPoint, hasFlash, torchOn), ScanDebugSample + ScannerViewModel.debug for the 04-05 HUD, setHand / onViewSize / onCameraReady / capture / focusAt / toggleTorch / torchOff"
  - "feature/scan: GuidanceChip, PalmGuideOverlay, CameraIconButton, TorchButton, ShutterButton"
  - "tools/gen_landmark_fixtures.py (--check, --stats DIR) and 26 synthetic fixtures; test helpers LandmarkFixtures, FakeScanCamera"
affects: [04-03, 04-04, 04-05]
tech-stack:
  added: []
  patterns:
    - "Guidance pipeline: frame -> HandGuidance.read(frame, FILL_CENTER visible rect) -> GuidanceStabilizer.update(raw, wrongHand, frame timestamp) -> ScannerUi (emits only on value change)"
    - "Overlay animation read only in draw lambdas (drawWithCache/onDrawBehind), no graphicsLayer; the scan line is composed only while searching, idle and motion allowed"
    - "Capture: ScanCamera interface; withTimeoutOrNull(1_000) focus whose failure or cancellation still captures; ensureActive distinguishes our own cancellation"
key-files:
  created:
    - android/app/src/main/java/app/palm/core/vision/HandGuidance.kt
    - android/app/src/main/java/app/palm/core/vision/GuidanceStabilizer.kt
    - android/app/src/main/java/app/palm/core/vision/ViewMapping.kt
    - android/app/src/main/java/app/palm/core/camera/ScanCamera.kt
    - android/app/src/main/java/app/palm/feature/scan/GuidanceChip.kt
    - android/app/src/main/java/app/palm/feature/scan/PalmGuideOverlay.kt
    - android/app/src/main/java/app/palm/feature/scan/ScannerControls.kt
    - android/app/src/main/res/values/strings_scan_guide.xml
    - android/app/src/main/res/values-hi/strings_scan_guide.xml
    - android/app/src/main/res/drawable/ic_hand.xml
    - android/app/src/main/res/drawable/ic_search.xml
    - android/app/src/main/res/drawable/ic_flash.xml
    - android/app/src/main/res/drawable/ic_flash_off.xml
    - tools/gen_landmark_fixtures.py
    - android/app/src/test/resources/landmarks/*.json (26 files)
    - android/app/src/test/java/app/palm/core/vision/HandGuidanceTest.kt
    - android/app/src/test/java/app/palm/core/vision/GuidanceStabilizerTest.kt
    - android/app/src/test/java/app/palm/core/vision/ViewMappingTest.kt
    - android/app/src/test/java/app/palm/feature/scan/ScannerViewModelTest.kt
    - android/app/src/test/java/app/palm/testing/FakeScanCamera.kt
    - android/app/src/test/java/app/palm/testing/LandmarkFixtures.kt
  modified:
    - android/app/src/main/java/app/palm/feature/scan/ScannerContent.kt
    - android/app/src/main/java/app/palm/feature/scan/ScannerViewModel.kt
    - .gitignore
decisions:
  - "ScannerUi does not carry palmCenter / frame size (the plan listed them): they change every frame and would make the StateFlow emit per frame (T-04-09); the ViewModel keeps them privately for the focus point and the debug flow carries them"
  - "Fixture JSON gains an optional settleFrames field (default 0): the first frame of a moving sequence has no motion history, so it is excluded from the per-frame label check"
  - "Preview contentDescription and the focus onClick sit on the overlay's Compose node: the AndroidView holder is reported invisible to accessibility, so the 04-01 preview label never reached TalkBack/uiautomator"
  - "Shutter uses an unbounded round ripple instead of clip(CircleShape), so the GlowPrimary disc glow is not cut off"
  - "CameraX's own tap-to-focus is disabled (isTapToFocusEnabled = false); the overlay's tap handler drives focus and the ring"
metrics:
  duration: "about 30 min (commits 07:44 to 07:57, plus reading, builds and emulator checks)"
  completed: 2026-11-28
actuals:
  tokens: 110950
  tasks: 3
  commits: 4
plan_head_before: 1143fb42960e9f77a42d7cc3fa2f74b07f2e282b
plan_head_after: c1f8b9098d84a0f5c436ce36ee3bbd95e5365b34
---

# Phase 4 Plan 02: Stable Live Guidance and Scanner Controls Summary

**Pixel-geometry HandGuidance (chirality, curl, TooFar/Moving hysteresis, wrong-hand hint) behind a timestamp-driven 250 ms dwell / 300 ms dropout / 500 ms hold stabiliser, tested on 26 seeded synthetic fixtures for both hands; the scanner now shows the toned guidance chip, a mirrored dashed palm outline with brackets and a draw-phase scan line, and captures only after a held palm with a 1 s capped focus at the palm centre, tap-to-focus and a torch toggle.**

## What was built

- **Task 1 (e8845b3, d780aa8):** `HandGuidance`, `GuidanceConfig`, `GuidanceStabilizer`, `FillCenterMapping`; `tools/gen_landmark_fixtures.py` (seeded `random.Random(4)`, `--check`, `--stats DIR`) and its 26 fixtures; `HandGuidanceTest` (9 tests, the recorded case skips via Assume), `GuidanceStabilizerTest` (8), `ViewMappingTest` (3); `landmarks/recorded/` git-ignored (D-09). Follow-up fix: `--check` compares LF-normalised bytes so a `core.autocrlf` checkout does not fail it.
- **Task 2 (7e8ce9f):** `ScannerViewModel` (setHand, onViewSize, debug flow, stabilised `ScannerUi`), `GuidanceChip`, `PalmGuideOverlay`, top gradient, pane title, 11 strings (en + hi), `ic_hand`, `ic_search`; `ScannerViewModelTest` (5).
- **Task 3 (c1f8b90):** `ScanCamera` / `CameraXScanCamera`, `capture` / `focusAt` / `toggleTorch` / `torchOff` / `onCameraReady`, `ShutterButton` / `CameraIconButton` / `TorchButton` in `ScannerControls.kt`, `ic_flash`, `ic_flash_off`, `FakeScanCamera`, portrait lock, TalkBack traversal order; 5 more `ScannerViewModelTest` cases (10 total).

## Verification (emulator-5554)

| Check | Result |
|---|---|
| `bash tools/gradlew.sh :app:testDebugUnitTest --tests HandGuidanceTest --tests GuidanceStabilizerTest --tests ViewMappingTest` | 20 tests, 0 failures, 1 skipped (recorded fixtures absent) |
| `python -I tools/gen_landmark_fixtures.py --check` | exit 0 ("fixtures OK: 26 files"); after editing right_palm.json: exit 1 naming it; CRLF copy: exit 0 |
| `bash tools/gradlew.sh :app:testDebugUnitTest` (full) | 114 tests, 0 failures, 1 skipped |
| `bash tools/gradlew.sh :app:assembleDebug` | exit 0 |
| Task 2 device command (install, `scan-open.sh --grant`, "Place your palm inside the frame" + "Right hand", no "hand guide couldn't start") | exit 0: uiautomator went idle with the scan line running |
| Task 3 device command (+ "Take photo", "Choose from gallery") | exit 0; after the semantics fix also "Camera preview" found |
| Torch on the emulator (its virtual camera reports a flash unit) | Flash switch shown; tap -> `checked="true"`, tap -> `checked="false"`; no crash |
| Tap-to-focus on the preview centre (emulator) | no crash (focus effect itself is a phone check) |
| Portrait lock | `requestedOrientation=SCREEN_ORIENTATION_PORTRAIT` on Scan, `UNSPECIFIED` after Back to HandSelect |
| `:app:connectedDebugAndroidTest` HandSelectTest (leaveApksInstalled, tests="0" guard) | 1 test, 0 failures, no tests="0" file |
| Acceptance greps (Tasks 1-3: 26 fixtures, @Test counts 8/9/10, no android imports in core/vision, GuidanceConfig + swapHandedness, check-ignore, Polite/mergeDescendants, reduceMotion/PathParser/no graphicsLayer, stabilizer.update/FillCenterMapping, 11 strings en = hi, withTimeoutOrNull(1_000), startFocusAndMetering, enableTorch, Role.Switch, scan_capture_busy, SCREEN_ORIENTATION_PORTRAIT, hasFlash, keepScreenOn, no logging in core/camera + feature/scan) | all pass |

Screenshots on the emulator showed the mirrored outline (thumb on the screen's right for the right hand), the brackets, the scan line, the neutral chip, the disabled shutter, the gallery button and the torch toggle over the virtual scene.

## Deferred to the 04-05 phone session

Not faked, not marked passed. Run with `ADB_SERIAL=<phone serial>` exported (only the phone attached, or the serial set); on MIUI confirm MIUIOP 10021 is Allow first.

1. **Live guidance states with a real hand (SCAN-03, D-07):** `bash tools/device-lock.sh bash -c 'bash tools/install-debug.sh && bash tools/scan-open.sh --grant right'`, then hold the right hand 25-40 cm from the back camera. Expected chip, one at a time, without flicker while held still: open palm -> "Palm detected. Tap to scan" (green border, outline and brackets turn green, scan line stops); back of hand -> "Turn your palm toward the camera"; fist -> "Open your hand and spread your fingers"; hand far (about 70 cm) -> "Bring your hand a little closer"; waving -> "Hold still"; hand out of view -> "Place your palm inside the frame". Repeat with `scan-open.sh --grant left` and the left hand. If palm and back are swapped for one hand, or the wrong-hand hint fires constantly, that is the `swapHandedness` / `rightPalmChirality` calibration (research Pitfall 1), done in 04-05 with the debug HUD.
2. **Wrong-hand hint:** with "Right hand" chosen, show the left palm. Expected: the chip adds "This looks like your left hand. You chose your right." without changing tone, and the shutter still enables after 0.5 s.
3. **Hold gate and focus-then-capture (SCAN-04, D-05, D-08):** hold the right palm still. Expected: the shutter enables about 0.75 s after the hand appears (250 ms dwell + 500 ms hold); tapping it shows the spinner, a focus ring at the palm centre, and the capture finishes within about 1 s plus capture time; "Your palm photo" opens upright. Then `bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect "21 hand points" && bash tools/check-capture.sh'` -> `capture OK: WxH, N bytes`, max(W, H) <= 1280.
4. **Tap-to-focus:** tap a point on the preview away from the controls. Expected: a 64 dp ring appears there (scale-in) and fades within about 1 s; the image visibly refocuses on close objects.
5. **Torch:** on a phone with a flash, tap the flash button. Expected: torch on, gold disc; tap again -> off. Turn it on, then (a) press Home and return, (b) tap the shutter (processing), (c) press Back: in each case the torch is off afterwards. On a phone without a flash, the slot is empty and the shutter stays centred.
6. **TalkBack:** with TalkBack on, swipe through the scanner. Expected order: Back, "Right hand", the guidance message, "Camera preview. Tap to focus.", "Choose from gallery", "Take photo" ("Waiting for your palm" / "Taking photo"), "Flash" switch On/Off; the chip is announced only when the shown state or hint changes.
7. **Overlay alignment (Pitfall 3):** on the phone's 20:9 screen, a palm that fills the on-screen outline must not read as "Bring your hand a little closer".
8. **Carried from 04-01 (still open):** phone ABIs, the Task 2/3 device commands, HandSelectTest and the EXIF/rotation capture proof on the phone, exactly as listed in 04-01-SUMMARY "Deferred to the 04-05 phone session" items 1-7.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixture `--check` failed on CRLF checkouts**
- **Found during:** Task 1 commit (git warned it would rewrite the JSON as CRLF)
- **Issue:** `core.autocrlf` rewrites the committed fixtures on Windows checkouts, so a byte comparison would report every file as changed.
- **Fix:** compare with `\r\n` normalised to `\n`.
- **Files modified:** tools/gen_landmark_fixtures.py
- **Commit:** d780aa8

**2. [Rule 1 - Bug] palmCenter / frameWidth / frameHeight kept out of ScannerUi**
- **Found during:** Task 2
- **Issue:** the plan listed them as ScannerUi fields, but the palm centre changes on every real frame, which would make the ui StateFlow emit per frame and recompose the scanner (contradicts the plan's own truth and T-04-09).
- **Fix:** the ViewModel keeps them privately for the capture focus point; `ScanDebugSample` (debug flow) carries the full reading for 04-05.
- **Commit:** 7e8ce9f

**3. [Rule 1 - Bug] Preview semantics never reached accessibility**
- **Found during:** Task 3 device check (uiautomator listed the AndroidView holder as invisible; "Camera preview" was missing, also in the 04-01 build)
- **Fix:** contentDescription, traversal index and the focus onClick moved to the overlay node that also handles taps.
- **Commit:** c1f8b90

**4. [Scope] Preview onClick has no custom action label**
- The plan asks for "an onClick label for focus", but the 11 string keys are fixed by the acceptance; the action has no custom label and the description "Camera preview. Tap to focus." carries the meaning. A label string can be added with the 04-05 TalkBack check if wanted.

**5. [Process] TDD order**
- Tests and implementation for each task were written in the same pass and run once (all green on the first run except one compile error); there is no separate red run on record.

**6. [Scope] Device checks ran on the emulator**
- The Task 2/3 `<precondition>` names one selected device; by the user's choice ("emulator now, phone later") they ran on emulator-5554 and the phone-only checks are listed above.

## Known Stubs

None in this plan's scope. The 04-01 stubs owned by 04-03 / 04-04 (permission states, processing overlay, API 26-27 decode, rejection buttons) are unchanged.

## Threat Flags

None beyond the plan's threat model. T-04-08: no logging in core/camera or feature/scan (grep), the debug flow is not collected or logged anywhere yet. T-04-09: ScannerUi emits only on value changes (test), overlay animation is draw-phase only, and the scan line stops on PalmDetected, processing or reduced motion. T-04-10: torchOff on pause and processing (one effect on `analysing`) and on dispose; toggle shown only with a flash unit (tests). T-04-SC: no new artifact downloaded.

## Notes

- `actuals.tokens` is chars/4 over the whole diff (443,807 chars), of which the generated fixture JSON is about 361,500; the hand-written code and docs are about 20,560 tokens.
- Thresholds are the research starting values in `GuidanceConfig.Default`; `--stats DIR` prints chirality, tip/MCP ratios, curl counts, hand fraction and motion per file for the 04-05 calibration.
- The emulator is left on HandSelect with CAMERA granted; `/sdcard/Pictures/no_hand_test.jpg` from 04-01 is still there.

## Self-Check: PASSED

- All created files listed above exist; commits e8845b3, d780aa8, 7e8ce9f and c1f8b90 are ancestors of HEAD.
