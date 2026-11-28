---
phase: 04-scan-loop
verified: 2026-11-28T05:20:00Z
status: human_needed
score: 2/6 must-haves verified (4 need the reference phone)
covered_files:
  - .planning/phases/04-scan-loop/04-01-PLAN.md
  - .planning/phases/04-scan-loop/04-01-SUMMARY.md
  - .planning/phases/04-scan-loop/04-02-PLAN.md
  - .planning/phases/04-scan-loop/04-02-SUMMARY.md
  - .planning/phases/04-scan-loop/04-03-PLAN.md
  - .planning/phases/04-scan-loop/04-03-SUMMARY.md
  - .planning/phases/04-scan-loop/04-04-PLAN.md
  - .planning/phases/04-scan-loop/04-04-SUMMARY.md
  - .planning/phases/04-scan-loop/04-05-PLAN.md
  - .planning/phases/04-scan-loop/04-05-SUMMARY.md
  - android/app/build.gradle.kts
  - android/app/src/androidTest/AndroidManifest.xml
  - android/app/src/androidTest/assets/photos/bomb_256mp.png
  - android/app/src/androidTest/assets/photos/huge_100mp.jpg
  - android/app/src/androidTest/assets/photos/no_hand.jpg
  - android/app/src/androidTest/assets/photos/orient6_gps.jpg
  - android/app/src/androidTest/java/app/palm/core/photo/PhotoProcessorTest.kt
  - android/app/src/androidTest/java/app/palm/feature/handselect/HandSelectTest.kt
  - android/app/src/androidTest/java/app/palm/feature/scan/ScanFallbackTest.kt
  - android/app/src/debug/java/app/palm/debug/ScanDebug.kt
  - android/app/src/main/AndroidManifest.xml
  - android/app/src/main/assets/hand_landmarker.task
  - android/app/src/main/assets/licenses/MediaPipe-Apache-2.0.txt
  - android/app/src/main/java/app/palm/PalmApp.kt
  - android/app/src/main/java/app/palm/core/camera/ScanCamera.kt
  - android/app/src/main/java/app/palm/core/designsystem/components/Dialog.kt
  - android/app/src/main/java/app/palm/core/designsystem/components/StateBlock.kt
  - android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt
  - android/app/src/main/java/app/palm/core/navigation/Routes.kt
  - android/app/src/main/java/app/palm/core/photo/CropMath.kt
  - android/app/src/main/java/app/palm/core/photo/PhotoProcessor.kt
  - android/app/src/main/java/app/palm/core/photo/ScanResultHolder.kt
  - android/app/src/main/java/app/palm/core/vision/GuidanceStabilizer.kt
  - android/app/src/main/java/app/palm/core/vision/HandDetector.kt
  - android/app/src/main/java/app/palm/core/vision/HandFrame.kt
  - android/app/src/main/java/app/palm/core/vision/HandGuidance.kt
  - android/app/src/main/java/app/palm/core/vision/LiveHandTracker.kt
  - android/app/src/main/java/app/palm/core/vision/ViewMapping.kt
  - android/app/src/main/java/app/palm/feature/handselect/HandSelectEntries.kt
  - android/app/src/main/java/app/palm/feature/scan/CameraPermission.kt
  - android/app/src/main/java/app/palm/feature/scan/CameraPermissionGate.kt
  - android/app/src/main/java/app/palm/feature/scan/GuidanceChip.kt
  - android/app/src/main/java/app/palm/feature/scan/PalmGuideOverlay.kt
  - android/app/src/main/java/app/palm/feature/scan/ProcessingOverlay.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanEntries.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanFallbackScreens.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanReadyScreen.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanReadyViewModel.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanScreen.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanViewModel.kt
  - android/app/src/main/java/app/palm/feature/scan/ScannerContent.kt
  - android/app/src/main/java/app/palm/feature/scan/ScannerControls.kt
  - android/app/src/main/java/app/palm/feature/scan/ScannerViewModel.kt
  - android/app/src/main/res/drawable/ic_camera.xml
  - android/app/src/main/res/drawable/ic_camera_off.xml
  - android/app/src/main/res/drawable/ic_flash.xml
  - android/app/src/main/res/drawable/ic_flash_off.xml
  - android/app/src/main/res/drawable/ic_gallery.xml
  - android/app/src/main/res/drawable/ic_hand.xml
  - android/app/src/main/res/drawable/ic_search.xml
  - android/app/src/main/res/values-hi/strings_home.xml
  - android/app/src/main/res/values-hi/strings_scan.xml
  - android/app/src/main/res/values-hi/strings_scan_guide.xml
  - android/app/src/main/res/values-hi/strings_scan_permission.xml
  - android/app/src/main/res/values/strings_home.xml
  - android/app/src/main/res/values/strings_scan.xml
  - android/app/src/main/res/values/strings_scan_guide.xml
  - android/app/src/main/res/values/strings_scan_permission.xml
  - android/app/src/release/java/app/palm/debug/ScanDebug.kt
  - android/app/src/test/java/app/palm/core/photo/CropMathTest.kt
  - android/app/src/test/java/app/palm/core/photo/ScanResultHolderTest.kt
  - android/app/src/test/java/app/palm/core/vision/GuidanceStabilizerTest.kt
  - android/app/src/test/java/app/palm/core/vision/HandGuidanceTest.kt
  - android/app/src/test/java/app/palm/core/vision/ViewMappingTest.kt
  - android/app/src/test/java/app/palm/feature/scan/CameraInitProblemTest.kt
  - android/app/src/test/java/app/palm/feature/scan/CameraPermissionTest.kt
  - android/app/src/test/java/app/palm/feature/scan/ScanViewModelTest.kt
  - android/app/src/test/java/app/palm/feature/scan/ScannerViewModelTest.kt
  - android/app/src/test/java/app/palm/testing/FakePhotoProcessor.kt
  - android/app/src/test/java/app/palm/testing/FakeScanCamera.kt
  - android/app/src/test/java/app/palm/testing/LandmarkFixtures.kt
  - android/gradle/libs.versions.toml
  - tools/check-capture.sh
  - tools/check-jank.sh
  - tools/check-scan-session.sh
  - tools/gen_landmark_fixtures.py
  - tools/gen_test_photos.py
  - tools/scan-open.sh
covered_digest: "v3:sha256:7f19481d49ef2c39ddd2195b1e9bf5230675464498a73451f9757b14433ee1e1"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Live guidance with both hands (SC2, SCAN-03): scan-open.sh --grant right, then left; hold open palm, back of hand, fist, hand at ~70 cm, waving, hand out of view, each held still for a few seconds"
    expected: "Chip shows Palm detected / Turn your palm toward the camera / Open your hand and spread your fingers / Bring your hand a little closer / Hold still / Place your palm inside the frame, one at a time, no flicker while still; palm vs back not swapped for either hand"
    why_human: "Needs a real hand in front of a real back camera; emulator scene has no hand"
  - test: "Calibration (04-05 Tasks 2-3): record 10 HUD files (right/left x palm/back/fist/far/moving), run gen_landmark_fixtures.py --stats, set GuidanceConfig, answer the fixtures consent (commit or keep local)"
    expected: "HandGuidanceTest.recordedFixturesClassifyOnAtLeast90PercentOfFrames runs (not skipped) and passes; if recordings stay local, ROADMAP SC2 wording and a STATE deviation line are updated"
    why_human: "Recordings are the user's own hand geometry; consent decision is the user's (D-09)"
  - test: "Wrong-hand hint, hold gate, focus-then-capture, tap-to-focus, palm pull-back (SC3, SCAN-04)"
    expected: "Hint appears without blocking capture; shutter enables ~0.75 s after the palm appears and disables within ~0.3 s of pulling back; focus ring at palm centre then capture within ~1 s; tap elsewhere refocuses"
    why_human: "Focus and real-frame timing need camera hardware"
  - test: "Torch (SC3): tap Flash on, off; turn on then press Home / tap shutter / press Back"
    expected: "Torch lights and turns off; off in all three leave cases; no Flash button on a phone without a flash unit"
    why_human: "Torch is hardware; emulator only proves the switch state toggles"
  - test: "Real palm capture proof (SC4 capture path): capture a palm, then bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect \"21 hand points\" && bash tools/check-capture.sh'"
    expected: "Your palm photo upright (fingers up), cropped to the hand, 21 dots, capture OK: WxH with max(W,H) <= 1280, no EXIF/APP1; rotation applied exactly once"
    why_human: "Camera HAL EXIF/rotationDegrees behaviour is device-specific; emulator capture never reaches ScanReady (no hand)"
  - test: "Real HEIC palm photo import + check-capture.sh; processing scrim + Back-cancel; camera-still rejection (Retake only); photo-gone after am kill; rotation/night mode during processing"
    expected: "As listed in 04-04-SUMMARY deferred items 2-5 and 9"
    why_human: "Needs a real palm photo and real processing time on the phone"
  - test: "Permissions on the phone: ScanFallbackTest, scripted permission walk (MIUI wording), real Settings toggle back to the scanner, camera held by another app -> Camera not available -> Try camera again"
    expected: "6/6 tests; scanner reappears after Settings without a tap; retry rebinds"
    why_human: "MIUI dialog/Settings and camera-in-use cannot be forced on the emulator read-only"
  - test: "SCAN-07 release session (SC5): export PKG=app.palm.dev; scan-open.sh --grant; check-scan-session.sh 120 (no hand), then a 2-minute palm-in-view session"
    expected: "No ANR, <= 5% frame deadline missed, thermal status <= 1, app resumed; no visible stutter or heat"
    why_human: "Hardware performance gate; release APK is ARM-only and crashes under the emulator's ARM translation"
  - test: "Phase 3 deferred hardware gates on release (same session): cold-start.sh --compile median < 2000 ms; check-jank.sh idle 10 and swipe 3 <= 5%"
    expected: "All within budget"
    why_human: "Reference-phone performance numbers"
  - test: "Phone ABIs (getprop ro.product.cpu.abilist) and model recorded in STATE.md; TalkBack order on the scanner; 20:9 overlay alignment; en/hi x font scale 1.0/2.0 screenshots on the phone"
    expected: "arm64-v8a or armeabi-v7a present; order Back, hand chip, guidance, preview, gallery, shutter, flash; a palm filling the outline is not Too far"
    why_human: "Phone-specific facts and visual checks"
---

# Phase 4: Scan Loop Verification Report

**Phase Goal:** The user points the phone at a palm, gets stable live guidance, and captures or imports a photo that is processed and ready to upload, running smoothly on the reference low-end phone.
**Verified:** 2026-11-28T05:20:00Z
**Status:** human_needed
**Re-verification:** No, initial verification
**Device this run:** emulator-5554 only (sdk_gphone64_x86_64, API 35). No physical phone was attached.

## Goal Achievement

The code for every success criterion exists, is substantive and is wired (Scan route -> CameraPermissionGate -> ScannerContent -> LiveHandTracker -> HandGuidance -> GuidanceStabilizer -> ScannerUi -> shutter -> ScanViewModel -> AndroidPhotoProcessor -> ScanResultHolder -> ScanReady). Everything that can be tested without a real hand and real camera hardware passed in this run. What is left is the reference-phone session: real-hand guidance, calibration, the camera capture proof, the torch and the SCAN-07 performance gate. None of these failed. They have not been run.

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1: camera permission is requested; Denied shows an explanation with "Open settings" (permanently denied) and "Choose from gallery"; no camera lands on a gallery fallback | VERIFIED | `CameraPermission.kt` reducer (Unknown/Granted/Denied/PermanentlyDenied -> 6 gate screens); `CameraPermissionGate.kt` requests on entry, re-checks on ON_RESUME, opens `ACTION_APPLICATION_DETAILS_SETTINGS` for this package; `ScanFallbackScreens.kt` puts gallery on every fallback, Retry on CameraError; `cameraInitProblem` maps failed init / no back camera; manifest has CAMERA and `camera*` features `required=false`, no storage permission. Tests: ScanFallbackTest 6/6 (re-run here on emulator-5554), CameraPermissionTest 10, CameraInitProblemTest 4 (in the unit run). MIUI wording and the real Settings toggle are phone items |
| 2 | SC2: with both hands on the real phone, guidance reaches and holds back of hand, closed hand, too far, moving, palm detected without flicker; logic passes unit tests on committed recorded-landmark fixtures | ? UNCERTAIN (human) | Logic is real: `HandGuidance.kt` (chirality, curl ratio, too-far and moving hysteresis, wrong-hand hint), `GuidanceStabilizer.kt` (250 ms dwell, 300 ms dropout, 500 ms hold, timestamps only). HandGuidanceTest 9 and GuidanceStabilizerTest 9 pass on 26 synthetic fixtures for both hands; `gen_landmark_fixtures.py --check` exit 0. But `recordedFixturesClassifyOnAtLeast90PercentOfFrames` is skipped (no recordings), `GuidanceConfig` holds uncalibrated research values, and no real hand has been seen. Needs the phone |
| 3 | SC3: capture works only while a palm is detected, focus runs before capture, flash toggle turns the torch on and off | ? UNCERTAIN (human) | `ScannerViewModel.capture` returns unless `captureEnabled` (stabilised PalmDetected held 500 ms, not guide error, not capturing); `focusCapped` runs `startFocusAndMetering` at the palm centre with a 1 s cap before `takeJpeg`; `toggleTorch` calls `enableTorch`, Flash shown only with `hasFlashUnit`, `torchOff` on pause, processing and dispose. ScannerViewModelTest 10 cases cover gate, focus-then-capture, hung/throwing focus, torch. Emulator, live: shutter node `enabled="false"` with no hand in view. Whether the torch lights and focus moves is hardware |
| 4 | SC4a: an imported photo (and the shared pipeline) comes out upright, cropped to the palm, <= 1280 px, without EXIF/GPS, incl. orientation 6 and 100 MP / HEIC; 21 landmarks ready; a no-hand gallery photo is rejected with a clear message | VERIFIED | `PhotoProcessor.kt`: ImageDecoder (API 28+) or BitmapFactory + ExifInterface (26-27), 250 MP refusal before pixels, crop to landmark box + 25%, `scaledSize` <= 1280, JPEG q85 re-encode, landmarks remapped and checked with the backend rule. PhotoProcessorTest 9/9, 0 skipped, re-run here: orientation-6 upright with GPS/Make/DateTime null and no APP1 "Exif", 100 MP sampled down, 256 MP bomb refused, HEIC import ran, `noHandRejected` with the real MediaPipe detector. Dialog text "We couldn't find a hand in this photo. Try a clear photo of your open palm." with Choose another photo / Close. `ScanResultHolder` keeps one `capture.jpg`, cleared on rejection, cancel, retake and app start |
| 5 | SC4b: a photo captured by the real back camera ends up upright (rotation applied once), cropped, <= 1280 px, EXIF-free, with 21 landmarks on ScanReady | ? UNCERTAIN (human) | Capture path code: `CameraXScanCamera.takeJpeg` passes `rotationDegrees`; `rotateCapture` / legacy matrix apply it only when EXIF has no rotation; instrumented `orientation6CaptureAppliesExifOnceAndIgnoresRotationDegrees` and `captureWithoutExifIsRotatedOnceByRotationDegrees` pass on synthetic JPEGs. A real HAL capture has never reached ScanReady (emulator scene has no hand), so `check-capture.sh` has not run on a real capture |
| 6 | SC5: a 2-minute scan session on the reference low-end phone has no ANR, no visible jank, no overheating or throttling | ? UNCERTAIN (human) | `tools/check-scan-session.sh` exists (ANR from `am_anr`, gfxinfo deadline missed, thermal status, battery temperature); analysis throttled to 12 fps, KEEP_ONLY_LATEST, paused below RESUMED and while processing (`tracker.active`). Emulator analysis rate observed here: 47-49 frames / 5 s, inference 28-34 ms. Emulator debug session numbers (5.75% missed, thermal 0, no ANR) are not a gate. Release session cannot run on the emulator (ARM-only release APK) |

**Score:** 2/6 truths verified (0 present-but-behavior-unverified; 4 need the reference phone)

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `core/vision/HandGuidance.kt`, `GuidanceStabilizer.kt`, `ViewMapping.kt` | Pure guidance + stabiliser + FILL_CENTER mapping | VERIFIED | No Android imports; called from `ScannerViewModel.onFrame` |
| `core/vision/LiveHandTracker.kt`, `HandDetector.kt` | LIVE_STREAM analyzer, IMAGE-mode detector | VERIFIED | Analyzer set in `ScannerContent`; detector bound in `VisionModule`, used by `AndroidPhotoProcessor` |
| `core/camera/ScanCamera.kt` | Focus, capture, torch | VERIFIED | Constructed in `ScannerContent`, used by `ScannerViewModel` |
| `core/photo/PhotoProcessor.kt`, `CropMath.kt`, `ScanResultHolder.kt` | One processing pipeline + handover | VERIFIED | Bound in `PhotoModule`; used by `ScanViewModel`, `ScanReadyViewModel`, `PalmApp` |
| `feature/scan/*` (gate, fallbacks, scanner, controls, overlay, chip, processing overlay, ScanReady) | S08 scanner and states | VERIFIED | `ScanEntries.kt` registers Scan and ScanReady; Phase 3 placeholder deleted |
| `assets/hand_landmarker.task` + Apache-2.0 notice | Bundled model, stored | VERIFIED | `noCompress += "task"`; mediapipe pinned 1.2.3 |
| `debug/.../ScanDebug.kt` + release twin | HUD and recorder, debug only | VERIFIED (code) | Release twin is an empty composable; recorder never used yet (phone) |
| `tools/check-scan-session.sh`, `check-capture.sh`, `gen_landmark_fixtures.py`, `gen_test_photos.py` | Gate and fixture tooling | VERIFIED | Fixture `--check` exit 0 here |
| `test/resources/landmarks/recorded/` | Recorded fixtures | NOT YET CREATED | Git-ignored by design until the D-09 consent answer; produced in the phone session |

### Key Link Verification

| From | To | Via | Status |
|---|---|---|---|
| HandSelect Continue | Scan(hand) | Nav3 route | WIRED (walked on the emulator this run) |
| ScannerContent | LiveHandTracker | `controller.setImageAnalysisAnalyzer(tracker.executor, tracker)` | WIRED (PalmScan "analyzed N frames" logged this run) |
| LiveHandTracker | ScannerViewModel | `onFrame` -> `HandGuidance.read` -> `GuidanceStabilizer.update` -> `_ui` | WIRED |
| ScannerUi.captureEnabled | ShutterButton | `enabled = ui.captureEnabled && !busy` | WIRED (node disabled with no hand) |
| Shutter / gallery picker | ScanViewModel.process | `onCaptured` / `PickVisualMedia` callback | WIRED |
| ScanViewModel | ScanResultHolder -> ScanReady | `holder.put` -> `ready` one-shot -> `onReady()` | WIRED |
| PalmApp.onCreate | ScanResultHolder.clear | field injection | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|---|---|---|---|---|
| GuidanceChip | `ui.shown` | MediaPipe LIVE_STREAM result -> HandGuidance | Yes (NoHand on the emulator scene) | FLOWING |
| ScanReadyScreen | photo + 21 points | `holder.latest` written by `ProcessResult.Ok` | Yes (real pipeline); not reached on the emulator with a real hand | FLOWING (code), phone to confirm |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Unit suite | `bash tools/gradlew.sh :app:testDebugUnitTest :app:assembleDebug` | BUILD SUCCESSFUL; 142 tests, 0 failures, 0 errors, 1 skipped (recorded fixtures) | PASS |
| Debug build | same command | `assembleDebug` OK | PASS |
| Processing + fallback screens | `connectedDebugAndroidTest` class=PhotoProcessorTest,ScanFallbackTest, leaveApksInstalled, `tests="0"` guard | PhotoProcessorTest 9/9 (0 skipped, heicImport ran), ScanFallbackTest 6/6; no tests="0" file | PASS |
| Synthetic fixtures match generator | `python -I tools/gen_landmark_fixtures.py --check` | `fixtures OK: 26 files` | PASS |
| Scanner opens and analyses | Home -> Scan my palm -> Right hand -> Continue; dump + logcat | "Place your palm inside the frame", Take photo disabled, Flash switch, Choose from gallery, HUD; `analyzed 47-49 frames, inference 28-34 ms` per 5 s; portrait lock | PASS (emulator) |
| Real-hand guidance, capture, torch, SCAN-07 | n/a | needs phone | SKIP (human) |

### Probe Execution

No `scripts/*/tests/probe-*.sh` exist and no plan declares one. Step 7c not applicable.

### Requirements Coverage

| Requirement | Source Plan | Status | Evidence |
|---|---|---|---|
| SCAN-02 | 04-01, 04-03, 04-05 | SATISFIED in code and emulator tests; phone walk pending | Truth 1 |
| SCAN-03 | 04-01, 04-02, 04-05 | NEEDS HUMAN | Truth 2 (real hands, calibration) |
| SCAN-04 | 04-01, 04-02, 04-05 | NEEDS HUMAN | Truth 3 (torch, focus on hardware) |
| SCAN-05 | 04-01, 04-04, 04-05 | SATISFIED | Truth 4 (gallery import, real-detector no-hand rejection) |
| SCAN-06 | 04-01, 04-04, 04-05 | SATISFIED for imports; capture path NEEDS HUMAN | Truths 4 and 5 |
| SCAN-07 | 04-01, 04-05 | NEEDS HUMAN | Truth 6 |

No orphaned requirements: every Phase 4 ID in REQUIREMENTS.md is claimed by a plan. REQUIREMENTS.md already shows SCAN-05/06 Complete and SCAN-02/03/04/07 Pending, which matches this report.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| (phase files) | - | TBD / FIXME / XXX | none found | - |
| (phase files) | - | TODO / HACK / placeholder | none found | - |
| `core/vision/LiveHandTracker.kt` | 94 | `Log.i` | Info | Debug-only frame count and mean inference time; no points, labels or pixels |
| `android/app/build.gradle.kts` | 27 | `shortcut:` debug-signed release | Info | Documented, owned by REL-03 |
| `.planning/ROADMAP.md` Phase 4 | - | `Mode: mvp` but the goal is not a User Story ("As a ..., I want to ..., so that ....") | Warning | `user-story.validate` returns false. This report used standard goal-backward verification instead of refusing. Fix the format with `/gsd-mvp-phase 4`, or drop `mode: mvp` |
| `.planning/ROADMAP.md` Phase 4 SC2 | - | "committed recorded-landmark fixtures" conflicts with D-09 if the user keeps the recordings local | Warning | Needs the consent answer; if local, reword SC2 as 04-05 Task 3 plans |

### Human Verification Required (reference phone checklist)

Run from the repo root with only the phone attached, or `export ADB_SERIAL=<phone serial>`. On MIUI set MIUIOP 10021 to Allow first. Update installs only.

1. **Phone facts:** `adb -s "$ADB_SERIAL" shell getprop ro.product.cpu.abilist` and the model go in STATE.md. Check for leftover Phase 3 test settings (animations, font scale, battery saver).
2. **Installs:** `bash tools/device-lock.sh bash tools/gradlew.sh :app:installDebug :app:installRelease`.
3. **Scanner up (04-01 #2):** `scan-open.sh --grant`. Expected: "Place your palm inside the frame", no "hand guide couldn't start", at most 60 analysed frames per 5 s.
4. **Connected suite on the phone:** full `connectedDebugAndroidTest` with `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` and the `tests="0"` guard. Record whether `heicImport` ran.
5. **Permissions (SC1):** scripted walk (Don't allow -> Allow camera -> Don't allow -> Open settings), then the real Settings toggle back to the scanner without a tap, and the Recents -> Settings camera-off walk (the permission dialog appears, not a blank screen). Optional: camera held by another app -> "Camera not available" -> "Try camera again" rebinds.
6. **Calibration (04-05 Task 2/3, SC2):** use the HUD to record right and left hands in palm, back, fist, far and moving poses (10 files, `ls files/landmarks` count >= 10). Pull them, run `--stats`, set the `GuidanceConfig` values (swapHandedness, rightPalmChirality, curlRatio, tooFar*, move*), mirror them in the generator and run `--check`. Then answer the **fixtures consent** (commit or keep local). Expected: the recorded-fixture test runs (no `<skipped`) and passes.
7. **Live guidance, both hands (SC2):** walk each state as in the frontmatter item 1, with no flicker while the hand is held still. Then the wrong-hand hint, and the pull-back test (shutter closes within about 0.3 s).
8. **Capture (SC3/SC4b):** the hold gate (about 0.75 s), focus ring at the palm centre then the capture, and tap-to-focus. The photo must come out upright and cropped with 21 dots. Then `check-capture.sh` -> `capture OK: WxH` with max <= 1280.
9. **Torch (SC3):** on and off. It must also be off after Home, after the shutter and after Back.
10. **Processing states (04-04):** real HEIC palm import plus `check-capture.sh`. Back during "Preparing your photo…" leaves no `capture.jpg`. Also check a camera-still no-hand frame (Retake only), the photo-gone state after `am kill`, rotation or night mode during processing, and the gallery no-hand photo.
11. **SCAN-07 (SC5):** release `PKG=app.palm.dev`, `check-scan-session.sh 120` with no hand, then a 2-minute session with a palm in view, with your own stutter and heat impressions. Gate: no ANR, <= 5% deadline missed, thermal <= 1, app resumed.
12. **Phase 3 deferred gates (release):** `cold-start.sh --compile` median < 2000 ms, `check-jank.sh idle 10` <= 5%, `check-jank.sh swipe 3` <= 5%.
13. **Visual/a11y:** en/hi x font scale 1.0/2.0 screenshots, TalkBack order and single announcements, 20:9 overlay alignment, white wall / dark room legibility, the landscape-entry rotation check.
14. **Close-out:** mark the calibration blocker resolved in STATE.md, mark SCAN-02/03/04/07 complete, and set 04-05 complete.

### Gaps Summary

No blocking gaps. No truth failed, no artifact is missing or a stub, no key link is broken, and there are no debt markers. The phase is not done, though: 04-05 is `status: partial`. Its Task 2 (real hands) and Task 3 (calibration and the fixtures decision) have not run, so SC2, SC3 (torch and focus hardware), the SC4 camera capture path and SC5 have no evidence yet. They depend on the reference phone, which was not attached this run. Two process warnings: the ROADMAP Phase 4 goal is not in User Story format even though Phase 4 is `mode: mvp`, and the SC2 wording depends on the D-09 consent answer.

Not re-checked in this run: the 16 KB alignment of the release APK (D-15; 04-05 records it as passing on the host), release-dex recorder absence, and the full 7-suite connected run (only PhotoProcessorTest and ScanFallbackTest were re-run).

---

_Verified: 2026-11-28T05:20:00Z_
_Verifier: Claude (gsd-verifier)_
