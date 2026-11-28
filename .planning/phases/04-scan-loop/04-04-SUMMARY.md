---
phase: 04-scan-loop
plan: 04
subsystem: android-client
tags: [photo, exif, heic, decode, processing, cancel, scanready, photo-picker, i18n]
status: complete
requires:
  - "04-01 frozen interfaces: PhotoProcessor/AndroidPhotoProcessor/PhotoSource/ProcessResult, CropMath, ScanResultHolder, ScanViewModel/ScanUi/Rejection, ScanScreen, ScanReadyScreen/ScanReadyViewModel, NocturneDialog, NocturneStateBlock, MediaPipeHandDetector"
provides:
  - "CropMath: ExifTransform, exifTransform (EXIF 1-8), MAX_DECODE_PIXELS (250 MP)"
  - "AndroidPhotoProcessor: API 26-27 BitmapFactory + ExifInterface path, pixel cap on both paths, @VisibleForTesting useLegacyDecode"
  - "ScanViewModel.cancel() and the io dispatcher seam; ScanResultHolder.clear() removes every file; PalmApp clears the capture at start"
  - "ProcessingOverlay / ProcessingOverlayHost; rejection dialog buttons by origin; ScanReady photo-gone state (ScanReadyViewModel.gone)"
  - "Photo-picker backport manifest entry; strings scan_processing, scan_choose_another, common_close, scan_ready_gone_title, scan_ready_gone_body (en + hi)"
  - "tools/gen_test_photos.py [--check] and four androidTest photos; testing/FakePhotoProcessor"
affects: [04-05, "Phase 6 upload (the processed JPEG and landmarks are what leaves the phone)"]
tech-stack:
  added: []
  patterns:
    - "Decode matrix: ImageDecoder (header listener refuses > 250 MP) on API 28+, BitmapFactory bounds pass -> cap -> inSampleSize decode -> EXIF matrix on API 26-27; capture rotationDegrees only when EXIF has no rotation"
    - "Cancel = cancel the job, ensureActive before put and before onReady, then a NonCancellable join + holder.clear(); the next process() waits for that cleanup"
key-files:
  created:
    - tools/gen_test_photos.py
    - android/app/src/androidTest/assets/photos/orient6_gps.jpg
    - android/app/src/androidTest/assets/photos/huge_100mp.jpg
    - android/app/src/androidTest/assets/photos/no_hand.jpg
    - android/app/src/androidTest/assets/photos/bomb_256mp.png
    - android/app/src/androidTest/java/app/palm/core/photo/PhotoProcessorTest.kt
    - android/app/src/test/java/app/palm/core/photo/CropMathTest.kt
    - android/app/src/test/java/app/palm/core/photo/ScanResultHolderTest.kt
    - android/app/src/test/java/app/palm/feature/scan/ScanViewModelTest.kt
    - android/app/src/test/java/app/palm/testing/FakePhotoProcessor.kt
    - android/app/src/main/java/app/palm/feature/scan/ProcessingOverlay.kt
  modified:
    - android/app/src/main/java/app/palm/core/photo/CropMath.kt
    - android/app/src/main/java/app/palm/core/photo/PhotoProcessor.kt
    - android/app/src/main/java/app/palm/core/photo/ScanResultHolder.kt
    - android/app/src/main/java/app/palm/PalmApp.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanViewModel.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanScreen.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyScreen.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyViewModel.kt
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/res/values/strings_scan.xml
    - android/app/src/main/res/values-hi/strings_scan.xml
decisions:
  - "ScanResultHolder.put replaces capture.jpg with Files.move(REPLACE_EXISTING, ATOMIC_MOVE) instead of File.renameTo, which cannot overwrite on every platform (failed on the Windows JVM test host)"
  - "The overlay fade lives in ProcessingOverlayHost(visible) beside ProcessingOverlay(modifier), so the frozen ProcessingOverlay signature stays and both fade-in and fade-out run"
  - "A pixel-cap refusal maps to Unsupported for captures too (plan: 'above MAX_DECODE_PIXELS -> Unsupported'); other undecodable captures stay Failed"
metrics:
  duration: "about 12 min (02:49 to 03:01 UTC)"
  completed: 2026-11-28
actuals:
  tokens: 13300
  tasks: 3
  commits: 3
plan_head_before: 738618da02de21caab92e128100347e12c4186c6
plan_head_after: 45a6c0b181eb38b8b3cdc9f6cd0381980a9878b3
---

# Phase 4 Plan 04: Photo processing for any photo, rejections, cancel and ScanReady states Summary

**Every capture or import now goes through a two-path decode matrix (ImageDecoder on API 28+, BitmapFactory + ExifInterface on 26-27) with a 250 MP pre-decode cap, comes out upright, cropped, <= 1280 px and EXIF-free (proven on output bytes on both paths, including HEIC and the real landmarker's no-hand rejection), and the user gets a cancellable processing scrim, source-specific rejection dialogs and a ScanReady that shows "This photo is gone" instead of a broken image.**

## What was built

- **Task 1 (21c3d0b):** `exifTransform` / `ExifTransform` / `MAX_DECODE_PIXELS` in CropMath (pure, no android imports). `AndroidPhotoProcessor` keeps the 04-01 constructor and tail; the API 28+ header listener throws a private `TooLarge` marker above 250 MP; the legacy path does a bounds pass (no size -> undecodable, which is how HEIC fails on API 26-27), the cap, an `inSampleSize` decode and one matrix (EXIF rotate + mirror, or the capture's `rotationDegrees` when EXIF has none). `tools/gen_test_photos.py` writes the four photos (1.92 MB together) and `--check`s size, tags and the corners after `exif_transpose`.
- **Task 2 (ad2c3cf):** `ScanViewModel.cancel()` (only while processing), late results dropped via `ensureActive()` before `put` and before `onReady`, cleanup joins the cancelled job then clears; `ScanResultHolder.clear()` deletes every file in its dir; `PalmApp` field-injects the holder and clears it in `onCreate`.
- **Task 3 (45a6c0b):** `ProcessingOverlay` (scrim @0.72 that swallows touches, 48 dp spinner, polite live-region label) faded by `ProcessingOverlayHost`; `BackHandler(enabled = processing) { viewModel.cancel() }` composed after the gate; rejection dialog GALLERY -> "Choose another photo" (re-opens the picker) + "Close", CAMERA -> "Retake photo"; ScanReady "photo gone" state block (ic_camera, brand.link) for no capture or a file that no longer decodes; the Play services photo-picker backport service + meta-data in the manifest; 5 strings in en and hi (32 in `strings_scan.xml`, keys identical in both locales).

## Verification (emulator-5554, API 35, x86_64)

| Check | Result |
|---|---|
| `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.photo.CropMathTest'` | 7 tests, 0 failures |
| `backend/.venv/Scripts/python.exe -I tools/gen_test_photos.py --check` | exit 0, "photos OK: 4 files" |
| `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.core.photo.PhotoProcessorTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' ...` | exit 0; **9 tests, 0 failures, 0 skipped** (each non-HEIC case runs with `useLegacyDecode` false and true) |
| `heicImport` | **ran and passed** on emulator-5554 (API 35 >= 29, `image/heif` supported, HeifWriter encoded the 900x1200 bitmap) |
| `noHandRejected` (real `MediaPipeHandDetector`) | passed: `no_hand.jpg` -> NoHand |
| `:app:testDebugUnitTest --tests ScanViewModelTest --tests ScanResultHolderTest` | 6 + 4 tests, 0 failures (after the Files.move fix, see deviations) |
| `:app:assembleDebug :app:testDebugUnitTest` (full) | exit 0; 142 tests, 0 failures, 1 skipped (the recorded-fixtures skip from 04-02) |
| aapt2 merged manifest check | `com.google.android.gms.metadata.ModuleDependencies` and `photopicker_activity:0:required` present |
| Full `:app:connectedDebugAndroidTest` (regression, tests="0" guard) | 24 tests in 7 suites, 0 failures (AppDatabase 1, PhotoProcessor 9, HandSelect 1, Home 4, LanguageSwitch 1, OnboardingFlow 2, ScanFallback 6) |
| Emulator gallery flow: `scan-open.sh --grant right` -> "Choose from gallery" -> `no_hand_test.jpg` | Dialog "No hand found" / "We couldn't find a hand..." with "Choose another photo" and "Close" (screenshot checked); Close -> back on the live scanner; "Choose another photo" -> system picker re-opens; Cancel -> scanner |
| Emulator gallery import of the 100 MP photo (pushed temporarily) | decoded without a crash, real landmarker -> "No hand found" |
| Emulator gallery import of 8 KB of random bytes named `.jpg` (pushed temporarily) | "Can't open this photo" / "...Try a JPEG or PNG photo." with "Choose another photo" + "Close"; Back dismisses it |
| `run-as app.palm.dev.debug ls cache/scan` after the rejections | no such directory: nothing kept (D-12) |
| Acceptance greps Tasks 1-3 (inJustDecodeBounds / MAX_DECODE_PIXELS / TAG_ORIENTATION, exifTransform w/o android imports, 4 photos < 6 MB, noHandRejected / useLegacyDecode / `Capture(.*, 90)`, HeifWriter + heicImport + `SDK_INT >= 29`, `fun cancel`, `lateinit var` + `clear()` in PalmApp, @Test counts 6 and 4, 32 strings and en = hi keys, viewModel.cancel / scan_choose_another / ProcessingOverlay in ScanScreen, scan_ready_gone_title in ScanReadyScreen, no logging in the four scan files) | all pass |

Device used for every instrumented and UI check: **emulator-5554** ("launcher_test", sdk_gphone64_x86_64, API 35). The two temporary pictures were deleted from the emulator and the MediaStore rescanned; `no_hand_test.jpg` from 04-01 stays.

## Approvals carried for the record

- 04-01 Task 1 download consent: approved by the user in chat on 2026-11-28 (Approve all; telemetry disclosure accepted). Additionally approved on 2026-11-28 after the 04-01 checkpoint: featurecombinationquery 1.7.0, media3-common/container/muxer 1.9.0, guava 33.3.1-android (+ failureaccess 1.0.2, listenablefuture 9999.0-empty), checker-qual 3.43.0, checker-compat-qual 2.5.3, error_prone_annotations 2.28.0, j2objc-annotations 3.0.0, tracing-ktx 2.0.1, kotlin-bom 1.8.0; groups androidx.media3, androidx.camera.featurecombinationquery and org.jetbrains.kotlin allowed in the 04-01 group diff. **This plan downloaded nothing** (test photos generated with the existing backend venv Pillow; the backport is a manifest entry).
- 04-01 Task 4 (real palm on the phone) stays deferred to the 04-05 phone session by the user's choice.

## Deferred to the 04-05 phone session

Not faked, not marked passed. Run with `ADB_SERIAL=<phone serial>` exported; on MIUI confirm MIUIOP 10021 is Allow first.

1. **PhotoProcessorTest on the phone:** clear `android/app/build/outputs/androidTest-results/connected`, then `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.core.photo.PhotoProcessorTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected`. Expected: exit 0, 9 tests; record whether `heicImport` ran or was skipped (Assume: API < 29, no HEIF decoder, or HeifWriter cannot encode) and why.
2. **Real HEIC import (D-16):** take a photo of a palm with the phone's own camera in HEIC/HEIF mode (or copy a `.heic` palm photo to the phone), then `bash tools/scan-open.sh --grant right` -> "Choose from gallery" -> pick it. Expected: "Preparing your photo…" scrim, then "Your palm photo" upright with 21 dots on the palm and meta "21 hand points · W × H px" with max(W, H) <= 1280; then `bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect "21 hand points" && bash tools/check-capture.sh'` -> `capture OK: WxH, N bytes`. On an API 26-27 phone the expected result is "Can't open this photo" instead.
3. **Processing overlay and Back-cancel (UI-SPEC Back behaviour):** with a real palm, tap the shutter. Expected: the scrim with spinner and "Preparing your photo…" covers the scanner, taps on the controls do nothing, the torch (if on) goes off. Repeat and press Back while the scrim shows: it disappears, the app stays on the live scanner (not HandSelect), and `adb -s "$ADB_SERIAL" shell run-as app.palm.dev.debug ls cache/scan` lists no `capture.jpg`. On the emulator processing finished too fast to see the scrim or to press Back during it (even for the 100 MP import), so this is only covered by `ScanViewModelTest.cancelDropsALateResult` so far.
4. **Camera rejection buttons:** capture a frame where the hand leaves just before the shutter fires (or point at a wall right after the shutter enables). Expected if the still has no hand: "No hand found" with only "Retake photo", which returns to the live scanner.
5. **ScanReady photo-gone state:** reach "Your palm photo" with a real palm, then `adb -s "$ADB_SERIAL" shell am kill app.palm.dev.debug` after pressing Home, and reopen the app from Recents. Expected: "This photo is gone" / "Take a new photo to continue." with "Retake photo" (no broken image); Retake returns to the scanner; `run-as ... ls cache/scan` is empty.
6. **Gallery rejection on the phone:** import a non-hand photo. Expected: "No hand found" with "Choose another photo" (re-opens the picker) and "Close".
7. **Photo-picker backport (API 26-29 phones only):** "Choose from gallery" opens the Play services photo picker rather than the documents UI once Play services has installed the module (may take a day after install); otherwise ACTION_OPEN_DOCUMENT is the expected fallback.
8. **Carried:** every item in 04-01-SUMMARY and 04-02-SUMMARY "Deferred to the 04-05 phone session" (real palm, capture EXIF/rotation proof, torch, focus, calibration, SCAN-07 session, Phase 3 hardware gates), unchanged.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `ScanResultHolder.put` could not replace an existing capture on the JVM test host**
- **Found during:** Task 2 (`ScanResultHolderTest.secondPutLeavesOneFileWithTheNewBytes` failed with "cannot replace ...capture.jpg")
- **Issue:** `File.renameTo` does not overwrite an existing file on Windows; on Android it does, so the app itself worked, but the holder's contract depended on the platform.
- **Fix:** `Files.move(tmp, file, REPLACE_EXISTING, ATOMIC_MOVE)` (java.nio.file is available from API 26 = minSdk), temp file still deleted on failure.
- **Files modified:** android/app/src/main/java/app/palm/core/photo/ScanResultHolder.kt
- **Commit:** ad2c3cf

**2. [Rule 2 - Correctness] A new process() waits for the previous cancel's cleanup**
- **Found during:** Task 2
- **Issue:** cancel() deletes the holder's files on the io dispatcher after the cancelled job finishes; a photo picked right after a cancel could have its fresh capture deleted by that cleanup.
- **Fix:** the cleanup job is kept and the next processing coroutine joins it first; cancel() is a no-op when nothing is processing.
- **Commit:** ad2c3cf

**3. [Scope] ProcessingOverlayHost added beside ProcessingOverlay**
- The plan puts the fade inside ProcessingOverlay, but a composable that is removed when processing ends cannot animate its own exit. `ProcessingOverlay(modifier)` keeps its planned signature (the scrim); `ProcessingOverlayHost(visible)` does the fade in and out (instant under reduced motion).

**4. [Process] TDD order**
- Tests and implementation for each task were written in the same pass; the first runs were green except the holder failure above (a real red). There is no separate red run on record for Task 1 (the new API `useLegacyDecode` would not have compiled before the implementation).

**5. [Scope] Device checks ran on the emulator**
- The Task 1 `<precondition>` (one selected device on which MediaPipe runs, per 04-01-SUMMARY) is met by emulator-5554; phone-only checks are listed above.

## Known Stubs

None. The 04-01 shortcuts owned by this plan (Unsupported on API 26-27, ScanReady popping back with no capture, Retake-only rejection dialog) are removed.

## Threat Flags

None beyond the plan's threat model. T-04-15 (bounds before pixels, 250 MP cap on both paths; 100 MP and 256 MP cases pass), T-04-16 (every decoder error mapped, random-bytes cases pass), T-04-17 (output re-encoded, ExifInterface + byte-level APP1/"Exif\0\0" checks pass on both paths), T-04-18 (clear on rejection, cancel and app start; JVM tests assert no file remains), T-04-19 (only the fixed `capture.jpg` name under cacheDir/scan), T-04-SC (nothing downloaded). No logging was added.

## Self-Check: PASSED

- All created files listed above exist; commits 21c3d0b, ad2c3cf and 45a6c0b are ancestors of HEAD.

## Review fixes

- **[major] ScanViewModel called a composition-scoped `onReady` after processing.** A rotation, locale or dark-mode change during the 1-3 s of processing recreated the Activity and the saveable back stack, so the stale lambda pushed ScanReady onto the old stack (user stuck on Scan with a stored capture) and kept the dead composition alive. Fix: `process()` no longer takes a callback; it sets the one-shot `ScanUi.ready` after `holder.put`, and ScanScreen runs `LaunchedEffect(ui.ready) { if (ui.ready) { viewModel.consumeReady(); onReady() } }` with the current composition's `onReady`. `ScanScreen`'s signature is unchanged. `ScanViewModelTest` now asserts `ready` is set once on Ok, cleared by `consumeReady()`, and never set on rejection or cancel. Unit tests: 142 run, 0 failures, 1 skipped (pre-existing). No instrumented test covers ScanScreen, and the emulator cannot reach the Ok path (no real palm photo), so the device check is deferred:
  - **Deferred to the 04-05 phone session (9):** `bash tools/scan-open.sh --grant right`, capture a real palm (or import a palm photo) and rotate the phone (or `adb -s "$ADB_SERIAL" shell cmd uimode night yes`, restore with `night no`) while "Preparing your photo…" shows. Expected: the app lands on "Your palm photo" with 21 dots, and Back from there returns to the scanner (not a second Scan entry). Restore auto-rotate/night mode afterwards.
