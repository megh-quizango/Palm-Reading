---
phase: "4"
slug: "scan-loop"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: true
wave_0_complete: false
created: "2026-11-27"
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Source: `04-RESEARCH.md` § Validation Architecture and § Security Domain.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (JVM) + kotlinx-coroutines-test + Turbine; androidx.test runner 1.7.0 / ext-junit 1.3.0 + Compose ui-test-junit4 (BOM-managed) for instrumented; Python (`-I`) for fixture generator checks; bash device scripts for SCAN-07 |
| **Config file** | `android/app/build.gradle.kts` (`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`); test resources `app/src/test/resources/landmarks/`; test assets `app/src/androidTest/assets/photos/` |
| **Quick run command** | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.vision.*' --tests 'app.palm.core.photo.*' --tests 'app.palm.feature.scan.*'` |
| **Full suite command** | `bash tools/gradlew.sh :app:testDebugUnitTest` then `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` (phone, `ADB_SERIAL` set); check the result XML is not `tests="0"` |
| **Estimated runtime** | ~60 seconds (quick, JVM warm); full suite with instrumented on the phone several minutes |

---

## Sampling Rate

- **After every task commit:** Run the quick run command (JVM only)
- **After every plan wave:** Run the full JVM suite + instrumented on the phone (emulator acceptable for UI-only waves, e.g. `ScanFallbackTest`)
- **Before `/gsd-verify-work`:** Full suite green + `bash tools/device-lock.sh bash tools/check-scan-session.sh 120` on the reference phone + deferred Phase 3 gates (`cold-start.sh`, `check-jank.sh idle 10`, `check-jank.sh swipe 3`, all with `PKG=app.palm.dev`)
- **Max feedback latency:** 60 seconds

---

## Per-Task Verification Map

Full command text lives in each PLAN task's `<verify>`; the cells below name the command and what it proves.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 04-01-T1 | 04-01 | 1 | all (D-17) | T-04-SC, T-04-03 | Every artifact, the model and the licence text are approved before any sync or curl; telemetry disclosed | checkpoint (blocking human) | — (user reply "approved") | n/a | ⬜ pending |
| 04-01-T2 | 04-01 | 1 | SCAN-02..SCAN-07 (tracer) | T-04-01, T-04-04, T-04-06, T-04-SC | Model md5 15318430ea3851670fe9914116a9cfad and stored uncompressed; a libmediapipe*.so packaged; no logging in core/photo or feature/scan; CAMERA + each of camera, camera.any, camera.autofocus, camera.flash required=false (checked per name), no storage permission; screen kept on; capture rotation fallback present; the test APK's manifest merges with heifwriter's minSdk 29 overridden in `androidTest/AndroidManifest.xml` only (app minSdk stays 26) | build + static + device (tracer) | `bash tools/gradlew.sh :app:assembleDebug :app:assembleDebugAndroidTest`; model hash / `Stored` / `lib/(arm64-v8a\|armeabi-v7a)/libmediapipe*.so` check on app-debug.apk; aapt2 xmltree per-name uses-feature loop (CAMERA, no READ_MEDIA_IMAGES); `bash tools/gradlew.sh :app:testDebugUnitTest`; `bash tools/device-lock.sh bash -c 'source tools/_device.sh && bash tools/install-debug.sh && adb -s "$ADB_SERIAL" logcat -c && bash tools/scan-open.sh --grant && bash tools/ui.sh expect "Place your palm inside the frame" && ... logcat -d -s PalmScan:I \| grep -q "analyzed [1-9]"'` | ❌ W0 (tools/scan-open.sh created by this task) | ⬜ pending |
| 04-01-T3 | 04-01 | 1 | SCAN-02, SCAN-05 | T-04-07, T-04-13 | scan-open.sh --revoke reaches the system dialog on a fresh process; refused state offers the gallery; Phase 3 HandSelectTest passes on the real Scan route | build + instrumented + device | `bash tools/gradlew.sh :app:assembleDebug`; `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.handselect.HandSelectTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` with the tests="0" guard (MIUIOP 10021 Allow first on MIUI); device `scan-open.sh --revoke` -> `ui.sh expect "Don.t allow\|^Deny"` -> Don't allow -> "Allow camera" "Choose from gallery" -> `scan-open.sh --grant` -> scanner | ❌ W0 (tools/check-capture.sh created by this task) | ⬜ pending |
| 04-01-T4 | 04-01 | 1 | SCAN-04, SCAN-06 | T-04-02, T-04-07 | Real output file: JPEG, <= 1280 px, empty getexif(), no Exif APP1 bytes; host copy deleted | checkpoint (human holds a palm) + device script | `bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect "21 hand points" && bash tools/check-capture.sh'` | ❌ W0 (tools/check-capture.sh created in T3) | ⬜ pending |
| 04-02-T1 | 04-02 | 2 | SCAN-03 | T-04-21 | Recorded hand fixtures git-ignored (`git check-ignore`); tests skip with Assume when absent | unit + Python | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.vision.HandGuidanceTest' --tests 'app.palm.core.vision.GuidanceStabilizerTest' --tests 'app.palm.core.vision.ViewMappingTest'`; `python -I tools/gen_landmark_fixtures.py --check` | ❌ W0 (TDD in task) | ⬜ pending |
| 04-02-T2 | 04-02 | 2 | SCAN-03 | T-04-09 | Shown-state-only emissions; draw-phase overlay (uiautomator stays idle) | unit + device | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.scan.ScannerViewModelTest'`; `bash tools/gradlew.sh :app:assembleDebug`; device `scan-open.sh --grant` then `ui.sh expect "Place your palm inside the frame" "Right hand"` and `ui.sh absent "hand guide couldn.t start"` | ❌ W0 (TDD in task) | ⬜ pending |
| 04-02-T3 | 04-02 | 2 | SCAN-04 | T-04-08, T-04-10 | No logging in core/camera or feature/scan; torch off on pause, processing and dispose | unit + device | `bash tools/gradlew.sh :app:testDebugUnitTest` (ScannerViewModelTest capture, 1 s focus cap, torch cases); `bash tools/gradlew.sh :app:assembleDebug`; device `ui.sh expect "Take photo" "Choose from gallery" "Place your palm inside the frame"` | ❌ W0 (TDD in task) | ⬜ pending |
| 04-03-T1 | 04-03 | 2 | SCAN-02 | T-04-11, T-04-12 | CAMERA asked only on Scan; settings intent for this package only; no storage or media permission merged; fallback screens and their en/hi strings built here (the gate calls them) | unit + static | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.scan.CameraPermissionTest'`; `bash tools/gradlew.sh :app:assembleDebug`; aapt2 xmltree guard | ❌ W0 (TDD in task) | ⬜ pending |
| 04-03-T2 | 04-03 | 2 | SCAN-02 | T-04-13, T-04-14 | Every fallback offers the gallery; CAMERA re-granted after the device check | Compose instrumented + device | `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.scan.ScanFallbackTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` with the tests="0" guard (MIUIOP 10021 Allow first when the MIUI phone is used); device walk `scan-open.sh --revoke` -> Don't allow -> "Allow camera" -> Don't allow -> "Open settings" -> pm grant -> back -> scanner (emulator OK) | ❌ W0 (TDD in task) | ⬜ pending |
| 04-04-T1 | 04-04 | 2 | SCAN-05, SCAN-06 | T-04-15, T-04-16, T-04-17 | Bounds + 250 MP cap before decode; garbage -> Unsupported/Failed; output has no GPS/Make/DateTimeOriginal and no Exif APP1 bytes, on both decode paths; EXIF-less capture rotated once by rotationDegrees, orientation-6 capture never twice; HEIC through the API 28+ ImageDecoder path (heicImport, HeifWriter-made, Assume-skipped below API 29, heifwriter's minSdk, or where unsupported) | unit + Python + instrumented | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.photo.CropMathTest'`; `backend/.venv/Scripts/python.exe -I tools/gen_test_photos.py --check`; `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.core.photo.PhotoProcessorTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` with the tests="0" guard (includes `#noHandRejected` with the real landmarker and `#heicImport`; MIUIOP 10021 Allow first on MIUI) | ❌ W0 (TDD in task) | ⬜ pending |
| 04-04-T2 | 04-04 | 2 | SCAN-05, SCAN-06 | T-04-18 | No file left after rejection or cancel; one file max; cleared at app start | unit | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.scan.ScanViewModelTest' --tests 'app.palm.core.photo.ScanResultHolderTest'` | ❌ W0 (TDD in task) | ⬜ pending |
| 04-04-T3 | 04-04 | 2 | SCAN-05, SCAN-06 | T-04-19 | Fixed app-generated file name; picker backport entry, no permission | build + static | `bash tools/gradlew.sh :app:assembleDebug :app:testDebugUnitTest`; aapt2 xmltree has `com.google.android.gms.metadata.ModuleDependencies` and `photopicker_activity:0:required` | ✅ (aapt2 path proven in Phase 3) | ⬜ pending |
| 04-05-T1 | 04-05 | 3 | SCAN-07, D-13, D-15 | T-04-20, T-04-24 | Recorder absent from the release dex; release ARM-only; thermal status < 2 during the session | build + static + device scripts | `assembleDebug assembleRelease` + `check-scan-session.sh` usage exit 2; release dex / ABI (`lib/arm64-v8a/libmediapipe*.so`, no x86) / `Stored` check; 16 KB record (`llvm-readelf -Wl`, `zipalign -c -P 16 -v 4`); Phase 3 gates (`cold-start.sh`, `cold-start.sh --compile`, `check-jank.sh idle 10`, `check-jank.sh swipe 3`, PKG=app.palm.dev); `bash tools/device-lock.sh bash -c 'export PKG=app.palm.dev && bash tools/scan-open.sh --grant && ... && bash tools/check-scan-session.sh 120'` | ❌ W0 (check-scan-session.sh created by this task) | ⬜ pending |
| 04-05-T2 | 04-05 | 3 | SCAN-02..SCAN-06 on device, SCAN-03 calibration | T-04-21 | Fixture commit only with the user's explicit answer | checkpoint (human session) | — (10 recordings listed through `run-as ... ls files/landmarks`) | n/a | ⬜ pending |
| 04-05-T3 | 04-05 | 3 | SCAN-03, all (final) | T-04-21 | `git ls-files` of recorded/ matches the user's answer | unit + Python + instrumented | `bash tools/gradlew.sh :app:testDebugUnitTest` and no `<skipped` in the HandGuidanceTest report; `python -I tools/gen_landmark_fixtures.py --check`; full `connectedDebugAndroidTest` with the tests="0" guard and the class list; calibration comment + STATE.md line; when fixtures stay local, ROADMAP criterion 2 reworded per D-09 and the STATE.md "criterion 2 deviation" line present | ✅ (tests from 04-02..04-04) | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Threat refs** (STRIDE registers in the PLAN files): T-04-01 photo/landmark persistence and logging; T-04-02 EXIF/GPS on the real output; T-04-03 MediaPipe telemetry (accepted, disclosed at REL); T-04-04 camera/landmarker resource leaks; T-04-05 tracer decode errors; T-04-06 landmark range; T-04-07 host copy of the palm photo; T-04-08 guidance logging; T-04-09 per-frame UI load; T-04-10 torch left on; T-04-11 permission scope; T-04-12 settings deep link; T-04-13 dead ends; T-04-14 device permission state; T-04-15 decompression bomb; T-04-16 malformed files; T-04-17 EXIF/GPS on imports incl. API 26-27; T-04-18 stale capture; T-04-19 uri-derived names; T-04-20 debug recorder in release; T-04-21 recorded hand fixtures in a public repo; T-04-22 host screenshots and photos; T-04-23 phone settings; T-04-24 thermal load; T-04-SC package installs.

---

## Wave 0 Requirements

Every Wave 0 item is created inside the task that first needs it (TDD tasks write the test before the code), so no separate Wave 0 plan exists:

- [ ] `tools/gen_landmark_fixtures.py` + `app/src/test/resources/landmarks/*.json` (26 synthetic fixtures, `--check`, `--stats`): 04-02-T1
- [ ] `tools/gen_test_photos.py` (run with `backend/.venv/Scripts/python.exe -I`) + `app/src/androidTest/assets/photos/{orient6_gps.jpg,huge_100mp.jpg,no_hand.jpg,bomb_256mp.png}`: 04-04-T1
- [ ] `HandGuidanceTest`, `GuidanceStabilizerTest`, `ViewMappingTest` (04-02-T1), `ScannerViewModelTest` (04-02-T2/T3), `CameraPermissionTest` (04-03-T1), `CropMathTest` (04-04-T1), `ScanViewModelTest`, `ScanResultHolderTest` (04-04-T2): JVM
- [ ] `PhotoProcessorTest` (instrumented; fake `HandDetector` for geometry/EXIF, real MediaPipe for `noHandRejected`, both decode paths): 04-04-T1
- [ ] `ScanFallbackTest` (Compose instrumented): 04-03-T2
- [ ] `tools/scan-open.sh`: 04-01-T2; `tools/check-capture.sh`, updated `HandSelectTest`: 04-01-T3; `tools/check-scan-session.sh`: 04-05-T1
- [ ] `.gitignore` entry `app/src/test/resources/landmarks/recorded/`; recorded case skips with `Assume` when absent: 04-02-T1

Framework itself (JUnit, coroutines-test, Turbine, androidx.test, Compose ui-test) is already in the project from earlier phases.

---

## Manual-Only Verifications

All rows below run in the single phone session of 04-05-T2 (the tracer's real-palm capture is 04-01-T4); the release no-hand 2-minute numbers come from 04-05-T1.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| All 5 guidance states (back of hand, closed hand, too far, moving, palm detected) reached and held without flicker, both hands; thresholds calibrated | SCAN-03 | Needs a real hand, real camera and lighting; handedness calibration and "too far" thresholds depend on the physical device | Debug overlay on, screen recording on the reference phone; walk each state with left and right hand; record final thresholds in the plan SUMMARY |
| Capture only when palm detected; focus at palm centre before capture | SCAN-04 | Real AF behaviour and shutter timing cannot be faked on JVM | On phone: confirm shutter disabled in every non-PalmDetected state; hold palm 500 ms, capture; check photo is sharp |
| Flash/torch toggle turns the torch on and off; hidden or disabled when no flash unit | SCAN-04 | Hardware torch | Toggle on S08; verify visually and/or via `adb shell dumpsys media.camera`; verify torch off on leaving the screen |
| Permission denied / permanently denied / no camera flows on real Android settings | SCAN-02 | System permission dialog and Settings deep link | Deny once, deny permanently, then use "Open settings" and "Choose from gallery"; confirm each lands correctly |
| Gallery import of a real HEIC photo on API 28+ (required; backs up 04-04's HeifWriter-made `heicImport`, and is the only HEIC proof on an API 28 phone, where `heicImport` is skipped) | SCAN-06, D-16 | Needs a real camera HEIC on the phone | Pick a HEIC palm photo via the photo picker (if the gallery has none, set the camera to save HEIF and take one); confirm upright, cropped, <= 1280 px result; record "HEIC not verifiable on this phone" only when the phone can neither show nor make one |
| Gallery no-hand photo shows the clear rejection message | SCAN-05 | End-to-end picker UX (automated path covered by `PhotoProcessorTest#noHandRejected`) | Pick a photo with no hand; confirm the message and that the user can retry |
| 2-minute live scan session on the low-end phone: no visible jank, no overheating felt; MIUI rules respected | SCAN-07 | Script covers ANR/frames/thermal numbers; perceived jank and heat need a human | Run `check-scan-session.sh 120` while holding a hand in view; watch for stutter; note device warmth and the recorded temperature delta in SUMMARY |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (the three checkpoints, 04-01-T1 consent, 04-01-T4 tracer with a real palm and 04-05-T2 calibration session, are human gates; 04-01-T4 still runs check-capture.sh on the real output)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s for the per-task JVM commands (device and instrumented commands run per wave)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
