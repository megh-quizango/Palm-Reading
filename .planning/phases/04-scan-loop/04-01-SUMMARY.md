---
phase: 04-scan-loop
plan: 01
subsystem: android-client
tags: [camerax, mediapipe, hand-landmarker, scan, tracer, exif, permissions]
status: complete
requires: []
provides:
  - "Frozen wave-2 interfaces: P3, HandFrame, GuidanceState, HandDetector/MediaPipeHandDetector/VisionModule, LiveHandTracker, CropMath, PhotoProcessor/AndroidPhotoProcessor/PhotoSource/ProcessResult/PhotoOrigin/PhotoModule, ScanResultHolder/ProcessedCapture, ScannerContent/CameraProblem, ScannerViewModel, CameraPermissionGate, ScanViewModel/ScanUi/Rejection, ScanScreen, ScanReadyScreen/ScanReadyViewModel, NocturneStateBlock, NocturneDialog, ScanReady route"
  - "tools/scan-open.sh [--grant|--revoke] [left|right] and tools/check-capture.sh"
  - "Pinned float16 hand_landmarker.task in assets (stored) + Apache-2.0 notice"
affects: [04-02, 04-03, 04-04, 04-05, "Phase 6 upload (landmark convention)", "STATE.md Blockers/Concerns (telemetry, ABIs)"]
tech-stack:
  added:
    - "androidx.camera camera-core/camera-camera2/camera-lifecycle/camera-view 1.7.0"
    - "com.google.mediapipe:tasks-vision 1.2.3 (tasks-core 1.2.3 transitive)"
    - "androidx.exifinterface:exifinterface 1.4.2"
    - "androidx.concurrent:concurrent-futures-ktx 1.1.0"
    - "androidx.heifwriter:heifwriter 1.2.0 (androidTest only, tools:overrideLibrary in the androidTest manifest)"
  patterns:
    - "LifecycleCameraController + PreviewView (PERFORMANCE, FILL_CENTER) in AndroidView; analyzer on the tracker's own single-thread executor; analyzer cleared before the landmarker is closed on that executor"
    - "One PhotoProcessor for captures and gallery imports: ImageDecoder (software allocator, sample size to <= 3072) -> capture-only rotationDegrees fallback when EXIF orientation is undefined/normal -> IMAGE-mode detect -> crop (25% margin) -> scale (<= 1280) -> JPEG q85 (no EXIF)"
    - "Landmarks: 21 [x, y, z] normalised to the processed crop, z on x's scale, validated by the backend's rule before storing"
key-files:
  created:
    - android/app/src/main/java/app/palm/core/vision/HandFrame.kt
    - android/app/src/main/java/app/palm/core/vision/HandDetector.kt
    - android/app/src/main/java/app/palm/core/vision/LiveHandTracker.kt
    - android/app/src/main/java/app/palm/core/photo/CropMath.kt
    - android/app/src/main/java/app/palm/core/photo/PhotoProcessor.kt
    - android/app/src/main/java/app/palm/core/photo/ScanResultHolder.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/StateBlock.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/Dialog.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanEntries.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanScreen.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanViewModel.kt
    - android/app/src/main/java/app/palm/feature/scan/CameraPermissionGate.kt
    - android/app/src/main/java/app/palm/feature/scan/ScannerContent.kt
    - android/app/src/main/java/app/palm/feature/scan/ScannerViewModel.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyScreen.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyViewModel.kt
    - android/app/src/main/res/values/strings_scan.xml
    - android/app/src/main/res/values-hi/strings_scan.xml
    - android/app/src/main/res/drawable/ic_camera.xml
    - android/app/src/main/res/drawable/ic_camera_off.xml
    - android/app/src/main/res/drawable/ic_gallery.xml
    - android/app/src/main/assets/hand_landmarker.task
    - android/app/src/main/assets/licenses/MediaPipe-Apache-2.0.txt
    - android/app/src/androidTest/AndroidManifest.xml
    - tools/scan-open.sh
    - tools/check-capture.sh
  modified:
    - android/gradle/libs.versions.toml
    - android/app/build.gradle.kts
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/java/app/palm/core/navigation/Routes.kt
    - android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt
    - android/app/src/main/java/app/palm/feature/handselect/HandSelectEntries.kt
    - android/app/src/main/res/values/strings_home.xml
    - android/app/src/main/res/values-hi/strings_home.xml
    - android/app/src/androidTest/java/app/palm/feature/handselect/HandSelectTest.kt
    - .planning/STATE.md
  deleted:
    - android/app/src/main/java/app/palm/feature/handselect/ScanPlaceholderScreen.kt
decisions:
  - "Phase 4 download consent: approved by the user in chat on 2026-11-28 (Approve all; telemetry disclosure accepted); additional resolved artifacts approved by the user on 2026-11-28 after the 04-01 checkpoint"
  - "MediaPipe native library name is libmediapipe_tasks_jni.so; tasks-core 1.2.3 ships it for arm64-v8a, armeabi-v7a, x86 and x86_64, so a 32-bit-only phone is covered"
  - "ScanReady with no capture (process death) pops straight back to Scan until 04-04 adds the photo-gone state block"
  - "PhotoProcessor returns Unsupported on API 26-27 until 04-04 adds the BitmapFactory + ExifInterface path"
metrics:
  duration: "about 1 h 10 min (resumed session: Task 2 code, emulator checks, Task 3)"
  completed: 2026-11-28
actuals:
  tokens: 27600
  tasks: 3
  commits: 2
plan_head_before: f0d035dfe704dc03b3a269af46d85ba0972eeb8c
plan_head_after: c2bd52dd6ad7c6ebff281d8fd24383ba7e91eeaa
---

# Phase 4 Plan 01: Scan Loop Tracer Summary

**CameraX 1.7.0 live preview feeding a 12 fps LIVE_STREAM MediaPipe Hand Landmarker (raw palm / no-palm chip), shutter capture and gallery import through one ImageDecoder -> detect -> crop -> 1280 px EXIF-free JPEG pipeline, handed to a ScanReady screen that draws the 21 landmarks; runs on the emulator, real-palm checks deferred to the 04-05 phone session.**

## What was built

- **Task 1 (consent, checkpoint):** approved by the user in chat on 2026-11-28 ("Approve all"; telemetry disclosure accepted). Recorded, not re-asked.
- **Task 2 (tracer):** dependencies, model + notice, manifest, `ScanReady` route and `scanEntries`, `core/vision`, `core/photo`, `NocturneStateBlock`, scanner, permission gate, `ScanViewModel`, ScanReady, strings (27 keys, en + hi), two drawables, `tools/scan-open.sh`, STATE.md items. The Phase 3 placeholder screen and its two strings are gone.
- **Task 3:** `NocturneDialog`, rejection dialog on `ScanScreen`, the scanner's gallery button (`ic_gallery`), `tools/check-capture.sh`, and HandSelectTest moved onto the real Scan route.
- **Task 4 (real palm on the phone, checkpoint):** deferred by the user's choice to the 04-05 phone session (see below). Not marked passed.

## Dependency consent and group diff (Task 1 acceptance)

The user approved the Task 1 list on 2026-11-28 and, after the 04-01 checkpoint the same day, also approved: `androidx.camera.featurecombinationquery:featurecombinationquery` 1.7.0, `androidx.media3:media3-common` / `media3-container` / `media3-muxer` 1.9.0 (via camera-video), and the resolved version shifts guava 33.3.1-android (+ failureaccess 1.0.2, listenablefuture 9999.0-empty), checker-qual 3.43.0, checker-compat-qual 2.5.3, error_prone_annotations 2.28.0, j2objc-annotations 3.0.0, androidx.tracing:tracing-ktx 2.0.1 and org.jetbrains.kotlin:kotlin-bom 1.8.0. Groups androidx.media3, androidx.camera.featurecombinationquery and org.jetbrains.kotlin are allowed in this acceptance on that basis.

New groups (before/after `:app:dependencies`):
- `debugRuntimeClasspath`: androidx.camera, androidx.camera.featurecombinationquery, androidx.camera.viewfinder, androidx.exifinterface, androidx.media3, com.google.android.datatransport, com.google.auto.value, com.google.errorprone, com.google.firebase, com.google.flogger, com.google.j2objc, com.google.mediapipe, com.google.protobuf, org.checkerframework
- `debugAndroidTestRuntimeClasspath`: androidx.heifwriter

Every group is on the plan's list or in the user's additional approval. heifwriter was kept (not dropped), so 04-04 keeps its HEIC instrumented case.

## Recorded facts

- Model: `hand_landmarker.task` 7,819,105 bytes, md5 `15318430ea3851670fe9914116a9cfad`, SHA-256 `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1` (matches research). `unzip -v` shows it `Stored` in app-debug.apk.
- APK `lib/` ABIs (debug): arm64-v8a, armeabi-v7a, x86, x86_64. MediaPipe library: `libmediapipe_tasks_jni.so` in all four (matches the verify's `libmediapipe[^ ]*\.so` check). Release `abiFilters` = armeabi-v7a + arm64-v8a.
- Emulator `ro.product.cpu.abilist`: `x86_64,arm64-v8a`. The phone's abilist was not read (phone not connected); deferred, see below.
- Emulator (emulator-5554, API 35) shows the scanner **without** "The hand guide couldn't start": the landmarker loads on x86_64, so wave-2 instrumented tests can run on the emulator. Debug log on the emulator: `PalmScan: analyzed 52 frames, inference 24 ms` per 5 s (about 10 fps, under the 12 fps cap).

## Verification (emulator-5554)

| Check | Result |
|---|---|
| `bash tools/gradlew.sh :app:assembleDebug :app:assembleDebugAndroidTest` | exit 0 (heifwriter override merges) |
| Model hash / Stored / libmediapipe / licence check | exit 0 |
| aapt2 manifest: CAMERA, 4 camera features required=false, no storage permission | exit 0 |
| `bash tools/gradlew.sh :app:testDebugUnitTest` | exit 0 (Phase 1-3 tests still green) |
| Task 2 device command (install, `scan-open.sh --grant`, chip, no error chip, PalmScan "analyzed N") with ADB_SERIAL=emulator-5554 | exit 0 |
| Task 2 acceptance greps (catalog 5, noCompress/abiFilters, overrideLibrary, placeholder gone, ScanReady/scanEntries, ScannerContent/PhotoProcessor/LiveHandTracker keys, no logging in core/photo + feature/scan, 27 strings en = hi, usage exit 2, datatransport in STATE) | all pass |
| `:app:connectedDebugAndroidTest` HandSelectTest (leaveApksInstalled, tests="0" guard) | 1 test, 0 failures, no tests="0" file |
| Task 3 permission flow (`--revoke` -> system dialog -> Don't allow -> "Allow camera" + "Choose from gallery" -> `--grant` -> scanner + gallery button) | exit 0 (regex adjusted, see deviations) |
| Gallery import of a pushed no-hand photo (`/sdcard/Pictures/no_hand_test.jpg`) from the scanner | "No hand found" dialog with "Retake photo"; `cache/scan` does not exist afterwards; Retake returns to the live scanner |
| `tools/check-capture.sh` self-test with synthetic files written into the app's cache | clean 960x1280 JPEG -> `capture OK: 960x1280, 19829 bytes`; 2000x1500 JPEG with EXIF -> `capture FAILED: long edge 2000 > 1280; 2 EXIF tags; Exif APP1 marker present`; missing file -> `capture FAILED: ... missing or empty` |

A screenshot of the scanner on the emulator showed the full-bleed preview (virtual scene), back button, "Right hand" chip, the no-hand chip and the disabled shutter, no sky.

## Deferred to the 04-05 phone session

None of these were faked or marked passed. Run each on the physical phone with `ADB_SERIAL=<phone serial>` exported (and only the phone attached, or the serial set).

1. **Phone ABIs:** `adb -s "$ADB_SERIAL" shell getprop ro.product.cpu.abilist`. Expected: contains arm64-v8a or armeabi-v7a (both are packaged). Record it in STATE.md (replace the "still to be read" note).
2. **Task 2 device command on the phone:** `bash tools/device-lock.sh bash -c 'source tools/_device.sh && bash tools/install-debug.sh && adb -s "$ADB_SERIAL" logcat -c && bash tools/scan-open.sh --grant && bash tools/ui.sh expect "Place your palm inside the frame" && bash tools/ui.sh absent "hand guide couldn.t start|Camera not available" && sleep 6 && adb -s "$ADB_SERIAL" logcat -d -s PalmScan:I | grep -q "analyzed [1-9]"'`. Expected: exit 0; the logged frame count per 5 s is at most 60 (12 fps cap). On MIUI, confirm MIUIOP 10021 is Allow first.
3. **HandSelectTest on the phone:** `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.handselect.HandSelectTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected` (clear that results directory first). Expected: exit 0, 1 test passed.
4. **Task 3 permission flow on the phone:** `bash tools/device-lock.sh bash -c 'bash tools/install-debug.sh && bash tools/scan-open.sh --revoke && bash tools/ui.sh expect "Don.{1,3}t allow|^Deny" && bash tools/ui.sh tap "Don.{1,3}t allow|^Deny" && bash tools/ui.sh expect "Allow camera" "Choose from gallery" && bash tools/scan-open.sh --grant right && bash tools/ui.sh expect "Place your palm inside the frame" "Choose from gallery"'`. Expected: exit 0; the phone is left on the live scanner (Right hand).
5. **Task 4, real palm (human):** with the scanner open (Right hand), hold the right palm open 25-40 cm from the back camera in indoor light. Expected: the chip changes to "Palm detected. Tap to scan" and the shutter enables (flicker is acceptable in this build). Tap the shutter: "Your palm photo" opens with the photo upright (fingers up), cropped around the hand, purple dots on wrist, knuckles and fingertips, and "21 hand points · W × H px" with max(W, H) <= 1280. "Retake photo" returns to the live scanner. Optional: gallery -> a photo without a hand -> "No hand found". Capture once more and leave "Your palm photo" open.
6. **Capture EXIF proof (right after 5, ScanReady still open):** `bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect "21 hand points" && bash tools/check-capture.sh'`. Expected: `capture OK: WxH, N bytes` with max(W, H) <= 1280; paste the line into the 04-05 summary. No copy stays on the host (the script's trap removes it).
7. **Capture rotation on the phone (D-10, Pitfall 6):** part of 5: a portrait capture must come out upright, confirming that the EXIF / rotationDegrees fallback applies rotation exactly once on that phone.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] scan-open.sh typed the name before the name screen was shown**
- **Found during:** Task 2 device verify on the emulator
- **Issue:** `ui.sh type` dumps once without waiting; right after "Get started" the transition was still running, the text was lost and "Choose a date" never appeared.
- **Fix:** wait for "Your first name" before typing and for "Asha" after it.
- **Files modified:** tools/scan-open.sh
- **Commit:** 18dba57

**2. [Rule 1 - Bug] The plan's "Don.t allow" regex cannot match the system dialog**
- **Found during:** Task 3 device verify on the emulator
- **Issue:** Android writes "Don’t allow" with a curly apostrophe (3 bytes in UTF-8); grep in the byte locale matches `.` to one byte, so `Don.t allow` never matched.
- **Fix:** used `Don.{1,3}t allow|^Deny` in the verify command (no code change; the deferred phone command above uses it too).
- **Commit:** none (verify command only)

**3. [Rule 1 - Bug] check-capture.sh reported "not an image" for a missing file**
- **Found during:** Task 3 self-test
- **Issue:** `exec-out run-as cat` writes cat's error text into the output file, so a missing capture looked like a corrupt one.
- **Fix:** check the file on the device with `run-as PKG test -s` first.
- **Files modified:** tools/check-capture.sh
- **Commit:** c2bd52d

**4. [Rule 1 - Bug] requirements.mark-complete would have closed SCAN-02..07 after the tracer**
- `gsd_run query requirements.mark-complete` ticked all six SCAN requirements, but flash, Open settings, stable guidance and the 2-minute session belong to 04-02..04-05. The REQUIREMENTS.md change was reverted; the requirements stay Pending until the phase verifies them.

**5. [Scope] Device checks ran on the emulator, not the physical phone**
- The Task 2 and Task 3 `<precondition>` names a physical phone. By the user's choice ("emulator now, phone later") every emulator-capable check ran on emulator-5554 and the phone-only checks are listed above for 04-05.

## Known Stubs

- `PhotoProcessor` returns `Unsupported` on API 26-27 (`core/photo/PhotoProcessor.kt`, shortcut comment); 04-04 adds the BitmapFactory + ExifInterface path.
- `ScanReadyScreen` pops back when there is no capture instead of the "photo gone" state block (`feature/scan/ScanReadyScreen.kt`, shortcut comment); 04-04 adds it.
- Rejection dialog has only "Retake photo" for every origin; 04-04 adds "Choose another photo" / "Close" for gallery imports.
- Permission gate covers granted / refused / camera problem only; 04-03 adds permanently denied, Settings, camera retry and resume re-check.
- Scanner has no guide overlay, torch, tap-to-focus, stabiliser or wrong-hand hint; 04-02 adds them.

All are planned work of the wave-2 plans, not gaps in this plan's goal.

## Threat Flags

None beyond the plan's threat model. T-04-01 (no logging in core/photo and feature/scan; only the debug-only PalmScan count/timing line), T-04-02 (re-encode without EXIF, check-capture.sh), T-04-06 (landmarksValid before storing) and T-04-07 (trap-removed temp dir) are implemented; T-04-02's on-phone proof is deferred item 6.

## Notes

- A test gradient photo `/sdcard/Pictures/no_hand_test.jpg` was left on the emulator for 04-04's gallery checks. The emulator is on the live scanner with CAMERA granted.
- Hindi strings are Claude-written (existing STATE concern covers native review).

## Self-Check: PASSED

- Created files listed above exist; commits 18dba57 and c2bd52d are ancestors of HEAD.

## Review fixes

- **[major] Failed CameraX init crashed instead of showing "Camera not available"** (`feature/scan/ScannerContent.kt`): the `initializationFuture` listener called `hasCamera` without checking the future, which throws `IllegalStateException` on the main thread when init failed. New `cameraInitProblem(awaitInit, hasBackCamera)` maps a failed future, or a throwing `hasCamera`, to `CameraProblem.OpenFailed` and only queries the camera after a successful init. Covered by `CameraInitProblemTest` (4 cases).
- Verified: `:app:testDebugUnitTest` green (84 tests, 0 failures); `HandSelectTest` on emulator-5554 1/1 passed, `tests="0"` guard clean; `scan-open.sh --grant right` on the emulator opens the live scanner with the no-hand chip, no crash in the crash buffer. Forcing a real init failure would need disabling the camera via device policy, which was not done (security setting); the unit test covers the mapping.
