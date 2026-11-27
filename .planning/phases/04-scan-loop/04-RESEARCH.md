# Phase 4: Scan Loop - Research

**Researched:** 2026-11-27
**Domain:** Android camera (CameraX 1.7.0), on-device hand landmarks (MediaPipe Tasks Vision 1.2.3), photo decode/crop/re-encode, low-end performance verification
**Confidence:** MEDIUM-HIGH (APIs and artifacts verified against Google Maven and official docs; guidance thresholds, handedness semantics on the rear camera and the native-lib ABI/16 KB status need the phone or the downloaded AAR)

## Summary

Everything this phase needs comes from Google Maven and the Android platform, and every artifact version in D-17 checks out. `camera-core`, `camera-camera2`, `camera-lifecycle` and `camera-view` are at `<release>1.7.0` (lastUpdated 20261118). `tasks-vision` and `tasks-core` are at `<release>1.2.3` (lastUpdated 20261111). `exifinterface` 1.4.2 exists. The newest exifinterface is `1.5.0-beta02`; stay on 1.4.2. The model on `storage.googleapis.com` is 7,819,105 bytes, md5 `15318430ea3851670fe9914116a9cfad`, and it is the same file the official docs now link on Hugging Face (Apache-2.0). The POMs turned up three things the consent checkpoint must show: (1) `tasks-core` 1.2.3 is a **32.5 MB AAR** (the native libraries now live there; `tasks-vision` is 486 KB), (2) it pulls in **Google datatransport (CCT) telemetry** that MediaPipe says it sends "for all of our platforms" with no opt-out API, and (3) `exifinterface` reaches the app only as a *runtime* dependency of camera-core, so the app must declare it to compile against it.

Two facts change how the guidance has to be designed. First, MediaPipe's current docs (changed in v1.2.2, November 2026) say handedness "correspond[s] to the subject's hands, not the viewer" and that a mirrored image inverts it. The older docs that PITFALLS.md cites said the model assumes a mirrored selfie image. The model file has not changed since 2023 (Last-Modified 26 Apr 2023), so one of the two docs is wrong for our rear-camera, unmirrored frames, and only the phone can say which. Second, a 2D landmark layout cannot tell a right palm from the back of a left hand: they are mirror images. So the plan has to (a) compute "chirality" from the cross product, (b) compute hand identity from the handedness label after one calibrated `swapHandedness` boolean, falling back to the hand the user chose when the score is low, and (c) treat palm-facing as chirality matching the expected palm chirality *for that identity*. Synthetic fixtures carry the *corrected* label, so the unit tests do not depend on which way MediaPipe labels hands.

Third, the CameraX controller does things the plan must account for. `CameraController` always enables a ViewPort: the PreviewView layout sets a crop rect, so the other use cases match the preview WYSIWYG. It also "listens to device motion sensor and set[s] the target rotation for the use cases". On a 20:9 portrait screen the visible strip is only about 60% of a 4:3 frame's width. So guidance must judge "too far" against the **visible** region, not the full frame, and `setAutoRotationEnabled(false)` (new in 1.7) is the simplest way to keep analysis frames upright in display coordinates. For SCAN-07: the preview is a SurfaceView, so `gfxinfo` sees only the Compose overlay frames. The existing `check-jank.sh idle` floor of 30 frames per second will fail on a mostly static overlay even when nothing is wrong. The session gate needs its own mode.

**Primary recommendation:** Build the tracer as permission -> `LifecycleCameraController` (auto-rotation off, one 4:3 selector) -> RGBA analyzer throttled to 12 fps -> LIVE_STREAM `HandLandmarker` -> pure `HandGuidance` + `GuidanceStabilizer` -> capture as in-memory JPEG bytes -> the **same** `PhotoProcessor` used for gallery imports (bounded decode, EXIF orientation, IMAGE-mode landmarker behind an interface, crop, scale, `Bitmap.compress`) -> `ScanReady`. Put every threshold, plus `swapHandedness` and the expected palm chirality, in `GuidanceConfig`, and set them at the on-device calibration checkpoint.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1; Phase 4 planning started at the user's "Plan Phase 4"). Decisions below are Claude's, grounded in `replica/PRD.md` §3 S08 / §5 scan specs, `.claude/CLAUDE.md` (CameraX and MediaPipe setup), the Phase 2 backend contract (`backend/app/main.py` landmarks field) and the Phase 3 code.

#### Route and screen
- **D-01:** The Phase 3 `Scan(hand)` route keeps its key and argument; its placeholder screen is replaced by the real scanner. The route keeps its background opt-out (no sky). Back from Scan returns to HandSelect. A new `ScanReady` placeholder route (shows the processed photo, the hand, the landmark count, "Retake" and a disabled "Read my palm (next update)") is where a processed capture lands until Phase 5/6 replace it. Processed captures are handed over through a `ScanResultHolder` (in-memory singleton + one file in `cacheDir`), not through route arguments.
- **D-02:** Layout follows Figma "3 · Screens" S08 (the UI researcher maps it): full-bleed preview, palm-outline guide, one guidance chip with icon + text (localized en/hi), capture button, torch toggle, gallery button, back. Guidance text changes are announced to TalkBack (polite live region). No sky behind the preview.

#### Camera (SCAN-02, SCAN-04)
- **D-03:** CameraX 1.7.0 with `LifecycleCameraController` + `PreviewView` (`ImplementationMode.PERFORMANCE`) in `AndroidView`, back camera only, IMAGE_ANALYSIS + IMAGE_CAPTURE. One 4:3 `ResolutionSelector` on all use cases so overlay, analysis frame and capture share one field of view. Analysis: `OUTPUT_IMAGE_FORMAT_RGBA_8888`, `STRATEGY_KEEP_ONLY_LATEST`, ~640x480 (`FALLBACK_RULE_CLOSEST_LOWER`), dedicated single-thread executor, every `ImageProxy` closed in `finally`, frames throttled to <= 12 fps before MediaPipe. Capture: `CAPTURE_MODE_MINIMIZE_LATENCY`, >= 1080 px short side when available.
- **D-04:** Permission: `CAMERA` is requested on entering Scan (not before). States: granted; denied (rationale available) -> explanation with "Allow camera" and "Choose from gallery"; permanently denied -> explanation with "Open settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`) and "Choose from gallery"; no back camera / camera open error -> gallery fallback screen. Permission is re-checked on resume so returning from Settings works. `uses-feature android.hardware.camera.any required=false`.
- **D-05:** Capture is enabled only in the PalmDetected state. Before capture, a focus-and-metering action at the palm centre (from the landmarks) runs with a 1 s cap, then the photo is taken; tap-to-focus on the preview also works. The flash toggle is the torch (`enableTorch`), shown only if the camera has a flash unit, turned off when the screen leaves.

#### Guidance (SCAN-03)
- **D-06:** MediaPipe Tasks Vision `com.google.mediapipe:tasks-vision:1.2.3` (pinned) with `hand_landmarker.task` float16 bundled in `assets/` (stored uncompressed). LIVE_STREAM mode for the preview, IMAGE mode for the processing step; `numHands=1`, CPU delegate, confidences 0.5. Created off the main thread, closed when the screen leaves.
- **D-07:** A pure-Kotlin `HandGuidance` (no Android types; input = 21 landmarks + handedness + image size + timestamp) classifies each frame: NoHand, BackOfHand, ClosedHand, TooFar, Moving, PalmDetected; plus an advisory WrongHand hint when the detected hand confidently differs from the chosen hand (it never blocks capture). Palm vs back from the sign of the palm normal (cross product of wrist->index MCP and wrist->pinky MCP) combined with handedness, corrected for the rear camera (no mirroring). Closed hand from fingertip-to-wrist vs MCP-to-wrist distance ratios. Too far from the hand bounding box relative to the frame. Moving from palm-centre displacement normalised by hand size over a short window. All thresholds live in one `GuidanceConfig`.
- **D-08:** Stability: a raw state must persist for a dwell time (about 250 ms) before the shown state changes, and PalmDetected must hold for about 500 ms before capture is enabled; a short dropout (< ~300 ms of NoHand) does not reset PalmDetected. The stabiliser is pure Kotlin and unit-tested with frame sequences.
- **D-09:** Fixtures: landmark fixtures are JSON (21 normalised points, handedness, label, sequence timestamps) under `app/src/test/resources/landmarks/`. CI fixtures are synthetic, generated by a committed script from a canonical hand pose (mirror for left/right, palm normal flip for back, finger curl for fist, scale for far, rotation, jitter and drift for moving). Real recorded fixtures come from a debug-only "record landmarks" control on the scan screen (writes JSON to app files; pulled with adb) during the on-device calibration checkpoint; they are committed only if the user agrees at that checkpoint (they are the user's own hand geometry and the repo is public), otherwise they stay git-ignored and only the derived thresholds are committed. Thresholds are calibrated on the phone with both hands and recorded in the plan summary.

#### Processing (SCAN-05, SCAN-06)
- **D-10:** One `PhotoProcessor` pipeline for captures and imports, off the main thread: bounded decode (ImageDecoder with target size on API 28+, BitmapFactory with `inSampleSize` plus `ExifInterface` orientation on API 26-27) so a 100 MP or HEIC photo never decodes at full size; bake the rotation upright; run the IMAGE-mode landmarker on the upright image; no hand -> reject with a clear message ("We couldn't find a hand in this photo. Try a clear photo of your open palm."); crop to the landmark bounding box expanded by a margin and clamped to the image; scale so the long edge is <= 1280 px; re-encode JPEG q85 with `Bitmap.compress` (writes no EXIF); map the 21 landmarks into the cropped image's normalised coordinates (what the backend expects: 21 `[x, y, z]`). HEIC on API 26-27 that cannot be decoded gets a clear "unsupported photo" message.
- **D-11:** Gallery import uses `ActivityResultContracts.PickVisualMedia(ImageOnly)` (no storage permission), reachable from the scanner and from both permission fallback screens. Imports go through the same pipeline and the same no-hand rejection.
- **D-12:** Output is a `ProcessedCapture` (JPEG bytes in a `cacheDir` file, width, height, hand side, 21 landmarks, source camera|gallery). Only the latest capture is kept; it is deleted on Retake and at app start. No photo is written anywhere else, and neither images nor landmarks are logged (privacy constraint).

#### Performance and verification (SCAN-07)
- **D-13:** A 2-minute scan session on a physical reference phone: no ANR (logcat), frame-deadline-missed share <= 5% (the Phase 3 A10 metric via gfxinfo), and no thermal throttling (`dumpsys thermalservice` status stays below moderate; battery temperature rise recorded). Analysis pauses when the screen is not resumed. The same phone session also runs the Phase 3 hardware gates that were deferred (release cold start < 2 s, Home idle and intro swipe <= 5% missed frames) because it needs the same device time.
- **D-14:** Camera and guidance cannot be proven on the emulator (no real hand in its virtual camera). Functional and unit work runs anywhere; the guidance calibration, capture and SCAN-07 checks need a physical phone (Redmi Note 10T over wireless adb, or the M2006C3MII). Lessons from Phase 3 apply: never uninstall on MIUI (update installs only), keep test APKs installed, MIUI background pop-up permission for instrumented tests, restore every changed device setting.
- **D-15:** 16 KB pages: check whether the MediaPipe 1.2.3 native libraries are 16 KB-aligned (ELF `p_align` and zip alignment in the built APK) and record the result. Not aligned is a REL-03 release risk, not a Phase 4 blocker.
- **D-16:** Tests: pure Kotlin unit tests for HandGuidance, the stabiliser, crop/scale/landmark mapping maths and the permission state reducer; synthetic-fixture tests in CI; instrumented tests for the processing pipeline (orientation-6 JPEG, a large image, HEIC on API 28+, no-hand image, output has no EXIF/GPS via `ExifInterface`); a Compose UI test for the permission fallback screens; on-device checks for live guidance, capture and the 2-minute session.

#### Dependencies (download consent)
- **D-17:** New downloads expected: `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` 1.7.0 (Google Maven), `com.google.mediapipe:tasks-vision:1.2.3` (Google Maven) with its transitive dependencies, `androidx.exifinterface:exifinterface:1.4.2` (Google Maven), and the `hand_landmarker.task` model file (float16, 7,819,105 bytes, Apache-2.0) from `storage.googleapis.com/mediapipe-models/...`. All are listed in one consent checkpoint at the start of the first plan; nothing is downloaded before the user approves. Include the model's Apache-2.0 notice.

### Claude's Discretion
- Package layout (e.g. `feature/scan`, `core/camera`, `core/vision`), exact threshold starting values, guidance copy wording, overlay drawing, and test split, as long as D-01..D-17 hold.

### Deferred Ideas (OUT OF SCOPE)
- Upload, job polling, Generating screen, result (Phases 5-6).
- Consented palm photo set for the model bake-off (Phase 2, 02-08) - not needed for this phase's guidance, which uses landmark fixtures.
- Front camera / selfie palm scan, multiple hands, auto-capture - not in M1.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| SCAN-02 | Camera permission; denied -> explanation + "Open settings" + "Choose from gallery"; camera unavailable -> gallery fallback | Permission reducer (Pattern 5); `hasCamera` only after `getInitializationFuture`; `CameraState.ERROR_*` mapping; `uses-feature` for `camera`, `camera.any`, `camera.autofocus` and `camera.flash` all `required=false` (Pitfall 9) |
| SCAN-03 | Live guidance: back of hand, closed hand, too far, moving, palm detected; stable, no flicker | `HandGuidance` maths (Pattern 2), the handedness calibration (Pitfall 1), visible-region "too far" (Pitfall 3), pixel-space distances (Pitfall 4), stabiliser + threshold hysteresis (Pattern 3), synthetic fixture generator (Code Examples) |
| SCAN-04 | Capture only when palm detected; flash toggle | `enableTorch` + `getTorchState` + `CameraInfo.hasFlashUnit`; focus-and-metering at the palm centre with 1 s cap and AF-unsupported fallback (Pattern 4); capture via `takePicture(executor, OnImageCapturedCallback)` |
| SCAN-05 | Gallery import; no-hand photo rejected with clear message | `PickVisualMedia(ImageOnly)` + backport `ModuleDependencies` manifest entry; same `PhotoProcessor`; real-landmarker negative test on a generated blank JPEG |
| SCAN-06 | Upright, palm-cropped, <= 1280 px, no EXIF/GPS | Decode matrix by API level (Pattern 6), `ALLOCATOR_SOFTWARE` + ARGB_8888 (Pitfall 5), crop/scale/landmark maths incl. z rescale, byte-level "no Exif APP1" proof + `ExifInterface` asserts, Pillow-generated test images |
| SCAN-07 | 2-minute session on the low-end phone: no ANR, no visible jank, no overheating | Session gate script design (ANR via `am_anr`, gfxinfo without the 30 fps floor, `Thermal Status:` polling, battery `temperature:` in tenths of a degree C), analysis pause below RESUMED, inference-time logging, MIUI rules (Pitfall 8) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Client stack is fixed: Kotlin, Compose, Nav3, Hilt (KSP, never kapt), Room 3, DataStore, CameraX, MediaPipe Tasks Hand Landmarker, OkHttp, kotlinx.serialization, Coil. No `org.jetbrains.kotlin.android` plugin, no `android.disallowKotlinSourceSets=false`.
- MediaPipe: pin `tasks-vision:1.2.3` (never `latest.release`); no legacy MediaPipe Solutions, no hand-wired TFLite, OpenCV or ML Kit hand APIs; `Delegate.CPU` by default (GPU only as an opt-in experiment); model bundled in `assets/`.
- CameraX: `LifecycleCameraController` + `PreviewView` PERFORMANCE in `AndroidView`; RGBA_8888 analysis (no manual YUV conversion); no `Camera2Interop` V1 (`Extender`, `Camera2CameraControl`), which is deprecated in 1.7.
- No Accompanist, no LiveData-based app code, no Gson/Moshi, no Glide/Picasso, no `material-icons-extended`, no core-library desugaring (minSdk 26).
- Privacy: strip EXIF/GPS before upload; no biometric templates stored; never log image bytes or readings. Secrets only on the server.
- Release ABIs: `armeabi-v7a`, `arm64-v8a` only (no x86/x86_64 in release).
- ARTEMIS rule: explore real device behaviour before writing device tests; ask the user which device when several are connected; `adb devices -l` for diagnosis.
- GSD workflow: changes go through `/gsd-execute-phase`. Ponytail: smallest complete change, with a check for every non-trivial piece of logic.
- Task-level rules from the orchestrator: never read `backend/.env`; never copy from `_extract/`; do not download or install before the D-17 consent checkpoint.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Camera permission state + settings deep link | Browser/Client (Android UI) | — | Runtime permission is an Activity concern |
| Camera binding, preview, torch, focus | Client (CameraX, main thread) | — | All `CameraController` methods are `@MainThread` |
| Frame -> landmarks | Client (analysis executor + MediaPipe thread) | — | On-device by requirement; never the network |
| Guidance classification + stabiliser | Client, pure Kotlin (no Android types) | — | Unit-testable with fixtures (D-07, D-08) |
| Photo decode/rotate/crop/encode | Client (`Dispatchers.Default`/IO) | — | Privacy: EXIF is stripped before anything leaves the device |
| Processed capture handoff | Client in-memory singleton + `cacheDir` file | — | D-01/D-12; no route args |
| Landmark validation | API/Backend (`_parse_landmarks`) | Client produces values in range | Server already rejects values outside -0.5..1.5 |
| Telemetry disclosure (MediaPipe metrics) | Policy / store listing | — | Library behaviour; app cannot turn it off (Pitfall 10) |

## Standard Stack

### Core (new in this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.camera:camera-core` | 1.7.0 (Nov 18 2026) | Use cases, `ImageProxy`, `FocusMeteringAction` | Official camera library `[VERIFIED: dl.google.com maven-metadata.xml <release>1.7.0]` |
| `androidx.camera:camera-camera2` | 1.7.0 | Camera2 backend (CameraPipe since 1.6) | Required runtime backend `[VERIFIED: maven-metadata]` |
| `androidx.camera:camera-lifecycle` | 1.7.0 | Lifecycle binding | Used by `LifecycleCameraController` `[VERIFIED: maven-metadata]` |
| `androidx.camera:camera-view` | 1.7.0 | `LifecycleCameraController`, `PreviewView` | D-03 `[VERIFIED: maven-metadata]` |
| `com.google.mediapipe:tasks-vision` | 1.2.3 (Nov 11 2026) | `HandLandmarker` | D-06; Apache-2.0 per POM `[VERIFIED: tasks-vision-1.2.3.pom]` |
| `androidx.exifinterface:exifinterface` | 1.4.2 | Orientation on API 26-27; EXIF assertions in tests | camera-core pulls it in only at **runtime** scope, so it must be declared to compile against it `[VERIFIED: camera-core-1.7.0.pom dependency scopes]` |
| `hand_landmarker.task` (float16) | GCS generation "1" (26 Apr 2023) | Palm detector + 21-landmark model | `Content-Length: 7819105`, md5 `15318430ea3851670fe9914116a9cfad` (both `latest/` and `1/` paths); HF mirror `X-Linked-Size: 7819105`, SHA-256 `fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1`; licence `apache-2.0` `[VERIFIED: HTTP HEAD on both URLs; HF model card]` |

Transitive downloads the consent list must name, per the POMs `[VERIFIED: POMs on dl.google.com]`:
- From `tasks-vision`: `tasks-core:1.2.3` (**AAR is 32,552,186 bytes**; tasks-vision AAR is 486,255 bytes), `com.google.guava:guava:27.0.1-android`, `androidx.annotation:annotation:1.1.0`. The optional `com.google.ai.edge.litertlm:litertlm-android:0.18.0` is marked `<optional>true</optional>` and is not pulled in.
- From `tasks-core`: `com.google.flogger:flogger:0.6`, `flogger-system-backend:0.6`, `com.google.protobuf:protobuf-javalite:4.26.1`, `com.google.android.datatransport:transport-api:3.0.0`, `transport-backend-cct:3.1.0`, `transport-runtime:3.1.0`.
- From CameraX: `camera-camera2-pipe`, `camera-common`, `camera-video` (camera-view depends on it), `androidx.camera.viewfinder:viewfinder-core:1.5.1`, `androidx.core:core-backported-fixes:1.0.0`, `androidx.tracing:tracing:1.3.0`, `com.google.dagger:dagger:2.59` (resolves up to the project's 2.60.1), `concurrent-futures(-ktx):1.1.0`, `auto-value-annotations:1.6.3`, `atomicfu:0.28.0`.

### Supporting (already in the project, no download)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `activity-compose` | 1.13.0 `[VERIFIED: libs.versions.toml:10]` | `rememberLauncherForActivityResult`, `PickVisualMedia`, `LocalActivity` | Permission + picker |
| `kotlinx-serialization-json` | 1.13.0 | Fixture JSON in unit tests, debug landmark recorder | Tests + debug |
| `kotlinx-coroutines` | 1.11.0 | `StateFlow` from analyzer to UI, `withTimeoutOrNull` for the 1 s focus cap | Always |
| Pillow (backend venv) | 13.0.0 `[VERIFIED: backend/.venv python -c "import PIL"]` | Host-side generation of test JPEGs (orientation 6 + GPS, 100 MP, no-hand) | Test asset script only; nothing to install |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `androidx.concurrent:concurrent-futures-ktx` `await()` | 6-line `suspendCancellableCoroutine` over `ListenableFuture.addListener` | ktx is already downloaded transitively (runtime scope); declaring it costs nothing. Use ktx. |
| Rotating the bitmap per frame (official sample) | `ImageProcessingOptions.setRotationDegrees` on `detectAsync` | Saves an allocation per frame, but the docs do not say which coordinate frame the results use when rotation is applied `[ASSUMED]`. Start with the sample's rotate; switch only if profiling shows GC churn |
| HEIC test fixture | `androidx.heifwriter:heifwriter` 1.2.0 (androidTest only) | One more download for one test. SC4 says "100 MP **or** HEIC", so the generated 100 MP JPEG covers it; HEIC becomes a manual import check on the phone |

**Installation (after the D-17 consent only):**
```toml
# gradle/libs.versions.toml
camerax = "1.7.0"
mediapipe = "1.2.3"
exifinterface = "1.4.2"
concurrentFutures = "1.1.0"

androidx-camera-core = { module = "androidx.camera:camera-core", version.ref = "camerax" }
androidx-camera-camera2 = { module = "androidx.camera:camera-camera2", version.ref = "camerax" }
androidx-camera-lifecycle = { module = "androidx.camera:camera-lifecycle", version.ref = "camerax" }
androidx-camera-view = { module = "androidx.camera:camera-view", version.ref = "camerax" }
mediapipe-tasks-vision = { module = "com.google.mediapipe:tasks-vision", version.ref = "mediapipe" }
androidx-exifinterface = { module = "androidx.exifinterface:exifinterface", version.ref = "exifinterface" }
androidx-concurrent-futures-ktx = { module = "androidx.concurrent:concurrent-futures-ktx", version.ref = "concurrentFutures" }
```
```bash
# Model: pin the versioned path, then check the hashes before committing
curl -fL -o "android/app/src/main/assets/hand_landmarker.task" \
  "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
sha256sum android/app/src/main/assets/hand_landmarker.task   # expect fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1
md5sum    android/app/src/main/assets/hand_landmarker.task   # expect 15318430ea3851670fe9914116a9cfad
```
Note: the SHA-256 is Hugging Face's LFS `X-Linked-ETag` for the same-sized file. If it does not match after download but the md5 does, record the actual SHA-256 (md5 comes from GCS itself).

## Package Legitimacy Audit

The seam (`package-legitimacy check`) supports npm/pypi/crates only. These are Maven artifacts, so they were checked against the authoritative registry (Google Maven metadata + POMs), and the coordinates come from official docs (CameraX release notes, MediaPipe Android guide).

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| androidx.camera:camera-core/-camera2/-lifecycle/-view 1.7.0 | Google Maven | 1.7.0 published 2026-11-18; line since 2019 | n/a (Google Maven has no counts) | android.googlesource.com (AndroidX) | OK (manual) | Approved, pending D-17 consent |
| com.google.mediapipe:tasks-vision 1.2.3 (+ tasks-core) | Google Maven | 2026-11-11; versions since 0.1.0-alpha-1 | n/a | github.com/google-ai-edge/mediapipe | OK (manual), **telemetry note** | Approved, pending consent; consent text must mention telemetry + 32.5 MB AAR |
| androidx.exifinterface:exifinterface 1.4.2 | Google Maven | stable | n/a | AndroidX | OK (manual) | Approved |
| androidx.concurrent:concurrent-futures-ktx 1.1.0 | Google Maven | stable, already transitive | n/a | AndroidX | OK (manual) | Approved (no new download) |
| hand_landmarker.task | GCS `mediapipe-models` / HF `litert-community` | 2023-04-26 | n/a | Google model card (Oct 2021) | OK (hash-pinned) | Approved, pending consent |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none (the telemetry is a policy issue, not a legitimacy issue; see Pitfall 10)

## Architecture Patterns

### System Architecture Diagram

```
 HandSelect --Scan(hand)--> ScanScreen
                              |
          (1) permission reducer: Granted | Denied | PermanentlyDenied | NoCamera | CameraError
              |                      \--> Fallback screens --(Allow / Open settings / Choose from gallery)
              v Granted
  LifecycleCameraController (main thread, bound to LocalLifecycleOwner, unbound onDispose)
     |-- Preview ----------------------------------> PreviewView (SurfaceView, FILL_CENTER) [on screen]
     |-- ImageAnalysis RGBA 640x480 4:3 --> analyzer (single thread)
     |         drop if not RESUMED or < 83 ms since last  -> close()
     |         toBitmap + rotate by rotationDegrees        -> close() in finally
     |         detectAsync(mpImage, monotonic ms)
     |                         v (MediaPipe result thread)
     |         HandFrame(21 pts, handedness, score, w, h, ts)
     |                         v
     |         HandGuidance (pure) --raw state--> GuidanceStabilizer (pure, dwell/dropout)
     |                         v
     |         StateFlow<ScanUi>  --> chip (live region), overlay, shutter enabled iff PalmDetected held >= 500 ms
     |-- ImageCapture 4:3 >= 1440 short side
               on shutter: focusAndMetering(palm centre) [<= 1 s] -> takePicture(in-memory JPEG)
                                                   v
 Gallery (PickVisualMedia ImageOnly) --uri--> PhotoProcessor (Dispatchers.Default)
                                                   | bounded decode (API 28+ ImageDecoder / 26-27 BitmapFactory+Exif)
                                                   | upright ARGB_8888 software bitmap
                                                   | HandDetector.detect (IMAGE mode)  -- none --> "couldn't find a hand"
                                                   | crop = landmark bbox + margin, clamp
                                                   | scale long edge <= 1280, compress JPEG q85
                                                   | remap landmarks to crop (x, y, z)
                                                   v
                                ProcessedCapture -> ScanResultHolder (memory + cacheDir file) -> ScanReady
```

### Recommended Project Structure
```
app/src/main/java/app/palm/
├── feature/scan/
│   ├── ScanEntries.kt            # entry<Scan>, entry<ScanReady> (replaces the HandSelectEntries Scan line)
│   ├── ScanScreen.kt             # permission gate + camera + overlay + chip + buttons
│   ├── ScanFallbackScreens.kt    # denied / permanently denied / no camera (stateless, Compose-tested)
│   ├── ScanReadyScreen.kt        # placeholder result (D-01)
│   ├── ScanViewModel.kt          # StateFlow<ScanUi>, capture + import orchestration
│   └── CameraPermission.kt       # pure reducer + resume re-check
├── core/vision/
│   ├── HandFrame.kt              # plain data: points, handedness, score, size, ts (no Android types)
│   ├── HandGuidance.kt           # pure classifier + GuidanceConfig
│   ├── GuidanceStabilizer.kt     # pure dwell/dropout/hold
│   ├── LiveHandTracker.kt        # LIVE_STREAM landmarker + analyzer (Android)
│   └── HandDetector.kt           # interface { fun detect(bitmap): HandFrame? } + MediaPipe IMAGE impl
├── core/photo/
│   ├── CropMath.kt               # pure: bbox, margin, clamp, scale, landmark remap, FILL_CENTER mapping
│   ├── PhotoProcessor.kt         # decode/rotate/detect/crop/encode
│   └── ScanResultHolder.kt       # @Singleton, latest ProcessedCapture + cacheDir file
app/src/main/assets/hand_landmarker.task          (+ assets/licenses/MediaPipe-HandLandmarker-Apache-2.0.txt)
app/src/test/resources/landmarks/*.json           (synthetic, generated)
app/src/androidTest/assets/photos/*.jpg           (generated by tools/gen_test_photos.py)
tools/gen_landmark_fixtures.py                    (stdlib only, seeded)
tools/gen_test_photos.py                          (Pillow from backend/.venv)
tools/check-scan-session.sh                       (SCAN-07 gate)
```

### Pattern 1: Camera controller setup
**What:** One controller, configured before binding, with auto-rotation off so analysis frames are upright in display coordinates.
**Example:**
```kotlin
// Method names VERIFIED from the CameraController reference (all @MainThread):
//   setEnabledUseCases(int), setPreviewResolutionSelector, setImageAnalysisResolutionSelector,
//   setImageCaptureResolutionSelector, setImageAnalysisOutputImageFormat(int),
//   setImageAnalysisBackpressureStrategy(int), setImageAnalysisAnalyzer(Executor, Analyzer),
//   setImageCaptureMode(int), setAutoRotationEnabled(boolean), enableTorch(boolean),
//   getTorchState(), getCameraInfo(), getCameraControl(), hasCamera(CameraSelector), getInitializationFuture()
// Constants ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888 / STRATEGY_KEEP_ONLY_LATEST /
// ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY / AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY: [ASSUMED] standard names, compile confirms
val fourThree = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
fun selector(w: Int, h: Int, rule: Int) = ResolutionSelector.Builder()
    .setAspectRatioStrategy(fourThree)
    .setResolutionStrategy(ResolutionStrategy(Size(w, h), rule))
    .build()

val controller = LifecycleCameraController(context).apply {
    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    setEnabledUseCases(CameraController.IMAGE_ANALYSIS or CameraController.IMAGE_CAPTURE)
    setAutoRotationEnabled(false) // 1.7: rotation follows the display, not the motion sensor (see Pitfall 2)
    previewResolutionSelector = ResolutionSelector.Builder().setAspectRatioStrategy(fourThree).build()
    imageAnalysisResolutionSelector = selector(640, 480, ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)
    imageCaptureResolutionSelector = selector(1920, 1440, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
    imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
    imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
}
// hasCamera "Only call this method after camera is initialized ... Calling it prematurely throws IllegalStateException"
controller.initializationFuture.addListener({
    if (!controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) onNoCamera() else previewView.controller = controller
    controller.bindToLifecycle(lifecycleOwner)
}, ContextCompat.getMainExecutor(context))
```
Resolution sizes are in sensor (landscape) orientation `[ASSUMED]`. `ResolutionStrategy` fallback rules are VERIFIED: `FALLBACK_RULE_NONE = 0`, `FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER = 1`, `FALLBACK_RULE_CLOSEST_HIGHER = 2`, `FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER = 3`, `FALLBACK_RULE_CLOSEST_LOWER = 4` `[CITED: developer.android.com/reference/androidx/camera/core/resolutionselector/ResolutionStrategy]`. CameraX recommends the two `..._THEN_...` rules to avoid an `IllegalArgumentException` at bind. Prefer `FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER` over D-03's `FALLBACK_RULE_CLOSEST_LOWER`: same intent, but it cannot fail binding.

### Pattern 2: Guidance geometry (pure Kotlin)
**What:** All distances in **pixels** (x*w, y*h), not normalised units (see Pitfall 4).
- **Chirality** `c = sign(cross(p5 - p0, p17 - p0))` with image y pointing down. Derived for a right palm facing the rear camera, upright, fingers up: thumb on image right, so `c < 0`. Left palm gives `c > 0`; the back of the right hand gives `c > 0`. In-plane rotation does not change the sign. `[ASSUMED: derived geometry; confirm at calibration]`
- **Identity** `id = if (score >= cfg.minHandednessScore) corrected(label) else chosenHand`, where `corrected` swaps when `cfg.swapHandedness` is true.
- **Facing** `palm = (c == cfg.palmChirality(id))`. Default `palmChirality(RIGHT) = -1`.
- **WrongHand** (advisory) `score >= cfg.wrongHandScore && corrected(label) != chosenHand`.
- **Closed** = count of fingers (index, middle, ring, pinky) with `|tip - p0| < cfg.curlRatio * |mcp - p0|` >= 3. In the canonical open pose the ratio is about 2.0; a curled finger is about 1.0-1.3. Start `curlRatio = 1.5`.
- **TooFar**: `max(bboxW, bboxH)` / short side of the **visible** region < `cfg.minHandFraction`. Start at 0.40; exit at 0.44 (hysteresis).
- **Moving**: palm centre = mean of points 0, 5, 9, 13, 17; handSize = `|p9 - p0|`; Moving if the maximum displacement inside a 300 ms window / handSize > `cfg.moveFraction`. Start at 0.12.
- **Priority** (first match wins): NoHand -> TooFar -> ClosedHand -> BackOfHand -> Moving -> PalmDetected. Far frames make the other checks noisy, and "open your hand" helps the user more than "turn your hand" when the hand is a fist.

### Pattern 3: Stabiliser (pure Kotlin, timestamp-driven)
```kotlin
// shown changes only after candidate persisted dwellMs; NoHand shorter than dropoutMs is ignored while PalmDetected;
// captureEnabled = shown == PalmDetected && heldSince <= now - holdMs. Uses frame timestamps, never the wall clock.
class GuidanceStabilizer(val dwellMs: Long = 250, val holdMs: Long = 500, val dropoutMs: Long = 300) { /* ... */ }
```
Combine the time dwell with **threshold hysteresis** (separate enter/exit values) for the continuous measures (TooFar, Moving). Without it, a hand sitting right on the boundary flips the candidate every frame and the chip never settles. The dwell resets on each flip, but the shown state still lags.

### Pattern 4: Focus then capture
```kotlin
// startFocusAndMetering returns ListenableFuture<FocusMeteringResult>; await() from concurrent-futures-ktx
val point = previewView.meteringPointFactory.createPoint(palmViewX, palmViewY)
val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE).build()
runCatching { withTimeoutOrNull(1_000) { controller.cameraControl?.startFocusAndMetering(action)?.await() } }
// fixed-focus lenses fail or cancel the action: ignore and capture anyway
controller.takePicture(captureExecutor, object : ImageCapture.OnImageCapturedCallback() { /* bytes -> PhotoProcessor; image.close() */ })
```
`takePicture(Executor, OnImageCapturedCallback)` is VERIFIED in the CameraController reference. The FocusMeteringAction names are `[ASSUMED]`, standard CameraX. Map the palm centre from analysis coordinates to view coordinates with the same `FillCenterMapping` the overlay uses.

### Pattern 5: Permission reducer
```kotlin
enum class CamPerm { Unknown, Granted, Denied, PermanentlyDenied }
fun reduce(granted: Boolean, askedThisSession: Boolean, showRationale: Boolean) = when {
    granted -> CamPerm.Granted
    !askedThisSession -> CamPerm.Unknown          // before the first ask, rationale=false means nothing
    showRationale -> CamPerm.Denied
    else -> CamPerm.PermanentlyDenied              // asked, refused, no rationale = "don't ask again" or policy
}
```
Use `rememberLauncherForActivityResult(RequestPermission())` and `LocalActivity.current` for `ActivityCompat.shouldShowRequestPermissionRationale`. Keep `askedThisSession` in `rememberSaveable`. Re-check `ContextCompat.checkSelfPermission` on every `ON_RESUME` with a `LifecycleEventEffect`. For settings: `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))`. `[ASSUMED: standard platform APIs]`

### Pattern 6: Bounded decode matrix
| Source | API 28+ | API 26-27 |
|--------|---------|-----------|
| JPEG / PNG / WebP | `ImageDecoder.decodeBitmap(src) { d, info, _ -> d.setTargetSampleSize(n); d.allocator = ALLOCATOR_SOFTWARE }` (applies EXIF orientation itself) | `BitmapFactory` bounds pass -> `inSampleSize` -> decode; orientation from `ExifInterface(stream)`, values 1-8 incl. mirrored 2/4/5/7 |
| HEIC/HEIF | ImageDecoder if `ImageDecoder.isMimeTypeSupported("image/heif")` ("Device optional", decoder "Android 9.0+") `[CITED: developer.android.com/media/platform/supported-formats]` | not supported -> "unsupported photo" |
| AVIF | decoder Android 12+ `[CITED: same]` | -> "unsupported photo" |
| CameraX capture | the JPEG bytes from the in-memory `ImageProxy` through the same path (EXIF orientation from the bytes) | same |

Pick `n` as the power of two that brings the long edge to <= 3072 px. A palm crop then still has >= 1280 px to scale down from, and peak memory stays under about 28 MB (3072 x 2304 x 4). Reject before decoding if the bounds give more than 250 MP (decompression bomb guard). This is a threshold of our own choosing, not a platform limit.

### Anti-Patterns to Avoid
- **Analysing the preview and uploading it:** the 640x480 analysis frame is too small; capture a still and re-run IMAGE mode on it (the hand moves between the last good frame and the shutter).
- **`latest/` model URL or `latest.release` artifact:** both are unpinned. Use `float16/1/` and `1.2.3`.
- **Calling `close()` on the landmarker from the main thread while `detectAsync` runs on the analyzer thread:** clear the analyzer first, then submit `close()` to the analyzer executor, then shut it down. Ignore results that arrive after closing.
- **Recomposing per analysis frame:** emit guidance state changes only (a `StateFlow` with `distinctUntilChanged`). Draw the debug landmark overlay in a draw-phase lambda, and only in debug builds.
- **Using the handedness label alone for palm vs back:** see Pitfall 1.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| YUV -> RGB | conversion code | `OUTPUT_IMAGE_FORMAT_RGBA_8888` + `ImageProxy.toBitmap()` | `toBitmap()` supports "`YUV_420_888`, `JPEG` or `RGBA_8888`" `[CITED: ImageProxy reference]` and handles row stride; the sample's `copyPixelsFromBuffer` breaks on padded rows |
| Preview/analysis FOV alignment | custom crop logic | One 4:3 selector + controller ViewPort | The controller sets the crop rect from the PreviewView layout (WYSIWYG) `[CITED: CameraController reference]` |
| Hand detection on stills | heuristics or a second model | MediaPipe IMAGE mode | Same model; reliable no-hand rejection |
| EXIF stripping | tag-removal calls on the original | Decode -> `Bitmap.compress` | Re-encoding writes no EXIF by construction; prove it with a byte check |
| Gallery permission | READ_MEDIA_IMAGES | `PickVisualMedia` | No permission; automatic ACTION_OPEN_DOCUMENT fallback `[CITED: photo-picker guide]` |
| 16 KB check | own ELF parser | `zipalign -c -P 16 -v 4` + `llvm-readelf -Wl` | Both installed (build-tools 37.0.0, NDK 29.0.14206865) |

**Key insight:** the hard parts are calibration and coordinate frames, not code. Keep the camera glue thin and push every decision into pure functions that take numbers.

## Common Pitfalls

### Pitfall 1: Handedness semantics on an unmirrored rear-camera frame
**What goes wrong:** Palm and back swap for one hand, or WrongHand fires constantly.
**Why it happens:** The current MediaPipe docs say "Left and Right handedness correspond to the subject's hands, not the viewer" and "Passing a horizontally mirrored image will invert these outputs" `[CITED: developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker]`, added in v1.2.2. The older docs (PITFALLS.md) said the model assumes a mirrored selfie image and that you should swap otherwise. The model file is unchanged since 2023-04-26 `[VERIFIED: GCS Last-Modified]`, so behaviour is what it was and the docs disagree. In 2D, a right palm and the back of a left hand are mirror images, so geometry alone cannot separate them.
**How to avoid:** Keep `swapHandedness` and `palmChirality` in `GuidanceConfig`. At calibration, record the four poses (L/R x palm/back) with the debug overlay showing raw label, score and chirality, then set both values. Synthetic fixtures carry the corrected label, so unit tests pass whatever the calibration finds.
**Warning signs:** The raw label flips between palm and back of the same hand (then the label tracks texture, not identity, and identity should come from the chosen hand).

### Pitfall 2: Sensor-driven rotation in CameraController
**What goes wrong:** The overlay and landmarks are off by 90 degrees when the phone is tilted, although the UI did not rotate.
**Why it happens:** CameraController "listens to device motion sensor and set the target rotation for the use cases" `[CITED: CameraController reference]`.
**How to avoid:** `setAutoRotationEnabled(false)` (added in 1.7.0-alpha02) `[CITED: CameraX release notes]`. Verify on the phone by tilting to landscape during a scan. The exact default rotation source after disabling is `[ASSUMED]` to be the display.

### Pitfall 3: Judging "too far" against the full analysis frame
**What goes wrong:** On a 20:9 screen with FILL_CENTER, about 40% of a 4:3 frame's width is off screen. A hand that looks large on screen counts as small, or a hand half off screen still counts as detected.
**How to avoid:** A pure `FillCenterMapping(imgW, imgH, viewW, viewH)` gives the visible rect in image pixels: `scale = max(viewW/imgW, viewH/imgH)`, visible size = view / scale, centred. Use it for TooFar, for the overlay and for the focus point. In debug, log it once beside `imageProxy.cropRect` to confirm they match.

### Pitfall 4: Distances in normalised units
**What goes wrong:** Curl ratios and TooFar change with frame aspect and rotation.
**Why it happens:** x is normalised by width and y by height ("normalized to [0.0, 1.0] by image width and height") `[CITED: Android guide]`. On a 480x640 frame one unit of x is 0.75 of one unit of y.
**How to avoid:** Multiply by width and height before any distance or cross product. Fixtures store the image size.

### Pitfall 5: ImageDecoder hardware bitmaps and non-8888 configs
**What goes wrong:** MediaPipe or `getPixels` crash with "pixel access is not supported on Config#HARDWARE bitmaps". 10-bit HEIC decodes to RGBA_F16.
**How to avoid:** Always set `decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE`. If `bitmap.config != ARGB_8888`, call `copy(ARGB_8888, false)`. MediaPipe lists only `Bitmap.Config.ARGB_8888` `[CITED: HandLandmarker reference]`. Never use `MEMORY_POLICY_LOW_RAM` (RGB_565). `[ASSUMED: ImageDecoder default allocator behaviour]`

### Pitfall 6: Double rotation on captures
**What goes wrong:** The captured photo ends up sideways or upside down on some devices.
**Why it happens:** An in-memory capture has both `imageInfo.rotationDegrees` and (usually) an EXIF orientation in the JPEG bytes. Applying both rotates twice. `[ASSUMED]`
**How to avoid:** Apply exactly one: the EXIF orientation from the bytes, through the shared decode path. Log `rotationDegrees` in debug for comparison. The on-device check (capture in portrait; ScanReady shows it upright) is the proof.

### Pitfall 7: `check-jank.sh idle` fails a healthy scan screen
**What goes wrong:** "jank FAILED: only N frames" on a perfectly smooth scan.
**Why it happens:** The preview is a SurfaceView (PERFORMANCE), which composites outside the app's HWUI frames. `gfxinfo` counts only overlay frames, and `MIN_FRAMES=$((30 * N))` `[VERIFIED: tools/check-jank.sh:20]` assumes an animating sky.
**How to avoid:** A new `tools/check-scan-session.sh SECONDS`: clear logcat, reset gfxinfo, poll thermal and battery every 10 s, then gate on (a) no `am_anr` / "ANR in" lines for the package, (b) deadline-missed share <= 5% of rendered frames with a low floor (for example >= 60 frames in 120 s; a chip change + shutter enable animate), (c) `Thermal Status:` < 2 throughout, (d) record the battery `temperature:` delta (tenths of a degree C). Formats confirmed on the emulator: `Thermal Status: 0`, `temperature: 250` `[VERIFIED: adb -s emulator-5554 shell dumpsys thermalservice / battery]`. Run it over wireless adb with USB charging off when possible (charging heats the battery); record the power state.

### Pitfall 8: MIUI device friction (carried from Phase 3)
Update-only installs (never uninstall; `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`), "USB debugging (Security settings)" for installs and input, MIUIOP 10021 (background pop-up) set to Allow before instrumented runs, phones dropping off USB, so wireless adb. Pull debug landmark recordings with `adb shell run-as app.palm.dev.debug cat files/landmarks/<name>.json` (debuggable build, no root) `[ASSUMED: run-as works on MIUI for debuggable apps]`. Restore every setting you change.

### Pitfall 9: The CAMERA permission implies required camera features
Play assumes `android.hardware.camera` and `android.hardware.camera.autofocus` are required when `CAMERA` is declared. Declare `camera.any`, `camera`, `camera.autofocus` and `camera.flash` all with `android:required="false"` `[CITED: developer.android.com/guide/topics/manifest/uses-feature-element]`. D-04 lists only `camera.any`; the other three are needed for "no dead end" devices to install at all.

### Pitfall 10: MediaPipe sends usage telemetry with no opt-out
`tasks-core` depends on datatransport CCT `[VERIFIED: tasks-core-1.2.3.pom]`. The MediaPipe privacy notice says it collects performance and utilisation metrics, "MediaPipe does not send that input data to Google servers", and "You are responsible for obtaining informed consent from your app users about Google's processing of MediaPipe metrics data" `[CITED: developers.google.com/edge/mediapipe/solutions/privacy]`. A maintainer said (Jul 2026): "we will be rolling out telemetry for all of our platforms" and "We are not adding an official API to disable this data collection" `[CITED: github.com/google-ai-edge/mediapipe/issues/6291]`. Excluding the datatransport artifacts might crash at runtime `[ASSUMED, untested]`. Phase 4: name it in the D-17 consent text and add a privacy-policy / Data safety item for REL; do not try to strip it.

### Pitfall 11: Native ABIs and 16 KB status of tasks-core 1.2.3 are unknown before download
v0.10.26 notes: "All the latest Android packages from Google Maven are now supporting the Android 16kb page size", and "0.10.26.1 includes also the support for ARM v7 CPUs (32-bit)" `[CITED: github.com/google-ai-edge/mediapipe/releases]`. That suggests some releases shipped without armeabi-v7a, and 1.x moved the native libraries into tasks-core (1.2 MB in 0.10.26 vs 32.5 MB in 1.2.3) `[VERIFIED: Content-Length]`. If the low-end M2006C3MII runs a 32-bit-only userspace `[ASSUMED, check getprop ro.product.cpu.abilist]` and the AAR has no armeabi-v7a, MediaPipe fails to load there. Check right after the consent download: `unzip -l tasks-core-1.2.3.aar | grep '\.so$'`, then on the built APK `zipalign -c -P 16 -v 4` and `llvm-readelf -Wl lib/arm64-v8a/*.so` (LOAD Align `0x4000`; GNU_RELRO end `(VirtAddr+MemSiz) % 0x4000 == 0` or RELRO fills its LOAD) `[CITED: developer.android.com/guide/practices/page-sizes]`. Play: "Starting February 1, 2027, if your app updates don't support 16 KB memory page sizes, you won't be able to release these updates" `[CITED: same]`.

### Pitfall 12: Hands outside the model's scope
The model card puts "hands wearing gloves, holding occluding objects, or covered in heavy decorations" out of scope, and error grows in "low light, and low-end camera noise" `[CITED: huggingface.co/litert-community/MediaPipe-HandLandmarker-Full]`. Heavy mehndi/henna is common in the target market. Expect lower detection rates; the no-hand message must be kind and offer a retry, never an accusation. Include a henna hand in the calibration poses if the user can.

## Code Examples

### Live analyzer (based on the official sample, with throttle and stride-safe conversion)
```kotlin
// Source pattern: mediapipe-samples HandLandmarkerHelper.kt (Apache-2.0) -- detectAsync(mpImage, frameTime), postRotate(rotationDegrees)
private var lastMs = 0L
fun analyze(image: ImageProxy) {
    try {
        val now = SystemClock.uptimeMillis()
        if (!resumed.get() || now - lastMs < 83) return     // <= 12 fps; paused below RESUMED (D-13)
        lastMs = now
        val src = image.toBitmap()                          // handles RGBA row stride
        val m = Matrix().apply { postRotate(image.imageInfo.rotationDegrees.toFloat()) }
        val upright = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, false)
        landmarker.detectAsync(BitmapImageBuilder(upright).build(), now) // strictly increasing: 83 ms gaps
    } finally { image.close() }
}
```
`detectAsync(MPImage, long)`: "The input timestamps must be monotonically increasing" `[CITED: HandLandmarker reference]`. LIVE_STREAM "Automatically drops incoming frames when busy" `[CITED: Android guide]`. Log `SystemClock.uptimeMillis() - result.timestampMs()` in debug builds for SCAN-07 (the sample's inference-time measure).

### Landmarker options
```kotlin
val options = HandLandmarker.HandLandmarkerOptions.builder()
    .setBaseOptions(BaseOptions.builder().setModelAssetPath("hand_landmarker.task").setDelegate(Delegate.CPU).build())
    .setRunningMode(RunningMode.LIVE_STREAM)           // RunningMode.IMAGE for PhotoProcessor (separate instance)
    .setNumHands(1)
    .setMinHandDetectionConfidence(0.5f).setMinHandPresenceConfidence(0.5f).setMinTrackingConfidence(0.5f)
    .setResultListener { result, _ -> onResult(result) } // thread unspecified in docs: only post to a StateFlow
    .setErrorListener { e -> onError(e) }
    .build()
val landmarker = HandLandmarker.createFromOptions(context, options) // throws MediaPipeException; call off main
```
`[CITED: official sample setupHandLandmarker + HandLandmarker reference]`. Gradle `androidResources { noCompress += "task" }` `[ASSUMED: AGP 9 DSL name, compile confirms]`. Check the APK: `unzip -v app-debug.apk assets/hand_landmarker.task` shows `Stored`.

### Crop / remap maths (pure, unit-tested)
```kotlin
// in: upright WxH, landmarks normalised to it. out: crop rect (px) and landmarks normalised to the crop.
fun cropFor(pts: List<P3>, w: Int, h: Int, margin: Float = 0.25f): IntRect {
    val xs = pts.map { it.x * w }; val ys = pts.map { it.y * h }
    val bw = xs.max() - xs.min(); val bh = ys.max() - ys.min()
    return IntRect(
        (xs.min() - margin * bw).toInt().coerceAtLeast(0), (ys.min() - margin * bh).toInt().coerceAtLeast(0),
        (xs.max() + margin * bw).toInt().coerceAtMost(w), (ys.max() + margin * bh).toInt().coerceAtMost(h))
}
fun remap(p: P3, w: Int, h: Int, c: IntRect) =
    P3((p.x * w - c.left) / c.width, (p.y * h - c.top) / c.height, p.z * w / c.width) // z shares x's scale
```
Margin: start at 25%. Joints do not reach the palm edges or the heel; PITFALLS.md suggests 15%. Calibrate on the phone. A clamped crop gives remapped x/y in 0..1, well inside the backend's accepted range: "Exactly 21 [x, y, z] finite numbers with x and y in -0.5..1.5" `[VERIFIED: backend/app/main.py:44-60]`.

### Proving no EXIF survives (instrumented)
```kotlin
val out = File(processed.path)
val exif = ExifInterface(out)
assertNull(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)); assertNull(exif.latLong)
assertNull(exif.getAttribute(ExifInterface.TAG_MAKE)); assertNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
val head = out.readBytes().copyOf(minOf(out.length().toInt(), 65536))
assertFalse("APP1 Exif present", head.indexOf("Exif\u0000\u0000".toByteArray()) >= 0) // byte-level proof
```
Pixel orientation: `tools/gen_test_photos.py` writes raw pixels rotated so that a red block lands top-left **after** orientation 6 is applied, plus GPS tags. The test (fake `HandDetector` returning a full-frame landmark box) asserts the top-left output pixel is red, `max(w, h) <= 1280`, and the EXIF asserts above.

### Synthetic fixtures (`tools/gen_landmark_fixtures.py`, stdlib, seeded)
Canonical right palm facing the rear camera in a 480x640 portrait frame (pixels; normalise by width/height). These are `[ASSUMED]` hand proportions of my own construction:
```
0 wrist (240,560) | thumb 1 (290,520) 2 (330,470) 3 (360,425) 4 (385,385)
index 5 (290,400) 6 (300,330) 7 (305,285) 8 (310,245) | middle 9 (245,395) 10 (248,315) 11 (250,265) 12 (252,220)
ring 13 (200,405) 14 (195,335) 15 (192,290) 16 (190,255) | pinky 17 (160,425) 18 (150,370) 19 (145,335) 20 (140,305)
```
Check: cross(p5-p0, p17-p0) = 50*(-135) - (-160)(-80) = -19550 < 0. Middle finger tip/MCP-to-wrist ratio is about 2.06. Transforms: left = mirror x (label Left); back of right = mirror x, label Right; fist = move PIP/DIP/TIP toward `lerp(MCP, palmCentre, 0.5)`; far = scale x0.3 about the centre; rotations 90/180/270; "still" sequences = 30 frames at 83 ms with Gaussian jitter sigma 2 px; "moving" = drift of 0.2 hand sizes per frame; boundary sequences oscillate around each threshold to test hysteresis. Commit the JSON. A unit test (or `python tools/gen_landmark_fixtures.py --check`) fails if the committed files differ from regenerated output.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| "Handedness assumes mirrored input; swap otherwise" | "Subject's hands; mirrored input inverts" | MediaPipe v1.2.2 docs (Nov 2026) | Calibrate `swapHandedness` on device |
| Native libs in tasks-vision AAR | Native libs in tasks-core AAR (32.5 MB) | MediaPipe 1.0.0 (Jul 2026) | APK size; check ABIs after download |
| Sensor-only rotation in CameraController | `setAutoRotationEnabled(boolean)` | CameraX 1.7.0-alpha02 | Display-locked analysis rotation |
| `getTapToFocusState()` | `getTapToFocusInfoState()` (state + tap position) | CameraX 1.7 era | Use the Info variant if you observe focus |
| `CameraController.OutputSize` / `set*TargetSize` | `ResolutionSelector` | CameraX 1.4 | Already the plan |
| Camera2Interop V1 | `Camera2Interop.forUseCase()` etc. | CameraX 1.7 | Not needed here |

**Deprecated/outdated:** `getTapToFocusState`, `setImageAnalysisTargetSize`, `Camera2Interop.Extender` `[CITED: CameraController reference, CameraX release notes]`. MediaPipe's own doc snippet uses `latest.release`; do not copy it.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Right palm on the unmirrored rear camera gives chirality < 0 | Pattern 2 | Palm/back inverted; fixed by one config value at calibration |
| A2 | MediaPipe's handedness is either correct or swapped consistently, so one boolean suffices | Pitfall 1 | If the label tracks facing rather than identity, identity must come from the chosen hand only |
| A3 | After `setAutoRotationEnabled(false)` the use cases follow display rotation | Pitfall 2 | Overlay off by 90 degrees on tilt; detected on device |
| A4 | Pattern 2 starting thresholds (curl 1.5, hand fraction 0.40/0.44, move 0.12, margin 25%) | Pattern 2, Code Examples | Only starting values; calibration replaces them |
| A5 | In-memory capture JPEG carries the EXIF orientation that CameraX reports | Pitfall 6 | Sideways capture on some devices; caught by the on-device portrait check |
| A6 | ImageDecoder defaults to hardware bitmaps; ALLOCATOR_SOFTWARE fixes it | Pitfall 5 | Crash on API 28+ imports; caught by instrumented tests |
| A7 | M2006C3MII may be 32-bit userspace; tasks-core 1.2.3 ABI list unknown | Pitfall 11 | MediaPipe unusable on that phone; check getprop + AAR listing early |
| A8 | Excluding datatransport would break MediaPipe | Pitfall 10 | None for Phase 4 (we do not exclude it) |
| A9 | `androidResources { noCompress += "task" }` is the AGP 9 DSL | Code Examples | Build error; trivially fixed |
| A10 | Result listener thread is a MediaPipe worker (unspecified in docs) | Code Examples | Only post to StateFlow; safe either way |
| A11 | Resolution bound sizes are given in sensor (landscape) orientation | Pattern 1 | Wrong size picked; visible in the debug log of the chosen resolution |
| A12 | `run-as` works on MIUI for the debug build | Pitfall 8 | Fall back to `getExternalFilesDir` + `adb pull` |
| A13 | The 250 MP decode guard is our own threshold | Pattern 6 | Too strict for extreme panoramas; harmless |

## Open Questions

1. **Does tasks-core 1.2.3 ship armeabi-v7a, and are its .so files 16 KB aligned?**
   - What we know: 16 KB support since 0.10.26; v7a was re-added in 0.10.26.1; native libs moved to tasks-core in 1.x.
   - What's unclear: the current ABI list and alignment (no AAR download allowed during research).
   - Recommendation: right after the consent download, list the AAR's `.so` entries and run `adb shell getprop ro.product.cpu.abilist` on both phones. Record both in the plan summary (D-15). If v7a is missing and a reference phone is 32-bit only, stop and ask the user.
2. **Which way does MediaPipe label hands on our frames?** Settled at calibration (Pitfall 1). Plan a debug overlay showing raw label, score, chirality, curl count, hand fraction and motion.
3. **Which phone is "the reference low-end phone" for SCAN-07?** D-14 names the Redmi Note 10T (API 33) or the M2006C3MII (API 30 per the Phase 3 validation). The M2006C3MII is the harsher test. Ask the user at the device checkpoint (ARTEMIS rule: ask when several devices are attached).
4. **Does MediaPipe run in instrumented tests on the x86_64 emulator?** The emulator reports `x86_64,arm64-v8a` `[VERIFIED: getprop]`, so arm64 translation may cover a missing x86_64 lib. Unknown until built. Run the processing instrumented tests on the phone; treat the emulator as optional.
5. **Capture resolution vs R-PALM-2 (>= 1080 short side):** the palm crop of a 1920x1440 capture is about 900-1100 px. Is "<= 1280 long edge" the only hard output rule (SCAN-06)? Recommendation: treat >= 1080 as the capture target (D-03) and record the crop size seen at calibration. Do not raise capture to max resolution on a low-end phone.
6. **SurfaceView and the Nav3 fade/slide:** PERFORMANCE mode may pop in without fading. Check on device; fall back to `ImplementationMode.COMPATIBLE` only if it looks broken.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK | Gradle | yes | 17.0.17 | — |
| Android SDK platform 37.0 / build-tools 37.0.0 | build, `zipalign -P 16` | yes | 37.0.0 (zipalign supports `-P <pagesize_kb>`) | — |
| NDK llvm-readelf | 16 KB ELF check | yes | NDK 29.0.14206865 | APK Analyzer |
| adb | device checks | yes | 1.0.41; only `emulator-5554` attached now | wireless adb to the phone at checkpoints |
| Physical phone (Redmi) | SCAN-03/04/07 calibration and session | not attached now | — | none: human checkpoint |
| Python 3 (host) | fixture generator (stdlib) | yes | 3.14.0 | — |
| Pillow | test photo generator | yes (backend/.venv) | 13.0.0, AVIF support True | — |
| Gradle cache: camera / mediapipe | build | **no** (only exifinterface 1.3.6/1.3.7/1.4.1 cached) | — | needs the D-17 consent download |
| Emulator | functional permission/fallback UI tests | yes | API 35, 4 KB pages | — |

**Missing with no fallback:** the physical phone for calibration and SCAN-07 (human checkpoint); the D-17 downloads (consent checkpoint).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 (JVM) + kotlinx-coroutines-test + Turbine; androidx.test runner 1.7.0 / ext-junit 1.3.0 + Compose ui-test-junit4 (BOM 2026.09.00) for instrumented |
| Config file | `android/app/build.gradle.kts` (`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`); test resources in `app/src/test/resources/landmarks/`, test assets in `app/src/androidTest/assets/photos/` |
| Quick run command | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.vision.*' --tests 'app.palm.core.photo.*' --tests 'app.palm.feature.scan.*'` |
| Full suite command | `bash tools/gradlew.sh :app:testDebugUnitTest` then `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` (phone, `ADB_SERIAL` set), and check `tests="0"` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| SCAN-02 | reducer: Unknown/Granted/Denied/PermanentlyDenied incl. "rationale false before first ask" | unit | `--tests 'app.palm.feature.scan.CameraPermissionTest'` | no, Wave 0 |
| SCAN-02 | fallback screens show Allow / Open settings / Choose from gallery; no-camera screen offers gallery | Compose instrumented | `-Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.scan.ScanFallbackTest` (emulator OK) | no, Wave 0 |
| SCAN-02 | manifest: CAMERA + 4 uses-feature required=false | static | `aapt2 dump xmltree --file AndroidManifest.xml app-debug.apk \| grep -c 'required.*false'` | no |
| SCAN-03 | each synthetic fixture classifies to its label, L and R, all rotations | unit | `--tests 'app.palm.core.vision.HandGuidanceTest'` | no, Wave 0 |
| SCAN-03 | still sequences: zero shown-state changes; boundary sequences settle; dropout < 300 ms keeps PalmDetected; capture after 500 ms | unit | `--tests 'app.palm.core.vision.GuidanceStabilizerTest'` | no, Wave 0 |
| SCAN-03 | fixtures in sync with the generator | unit (Python) | `python -I tools/gen_landmark_fixtures.py --check` | no, Wave 0 |
| SCAN-03 | all 5 states reached and held with both hands on the phone | manual checkpoint | debug overlay + screen recording; thresholds recorded in summary | — |
| SCAN-04 | shutter disabled unless PalmDetected held; torch on/off; focus before capture | ViewModel unit + manual | `--tests 'app.palm.feature.scan.ScanViewModelTest'`; torch via `dumpsys media.camera` / visual | no, Wave 0 |
| SCAN-05 | blank image rejected by the **real** landmarker with the message | instrumented (phone) | `class=app.palm.core.photo.PhotoProcessorTest#noHandRejected` | no, Wave 0 |
| SCAN-06 | crop/scale/remap maths, z rescale, clamping, FillCenterMapping | unit | `--tests 'app.palm.core.photo.CropMathTest'` | no, Wave 0 |
| SCAN-06 | orientation 6 upright, 100 MP bounded (no OOM, <= 1280), no EXIF/GPS (ExifInterface + byte check), JPEG decodes | instrumented | `class=app.palm.core.photo.PhotoProcessorTest` | no, Wave 0 |
| SCAN-06 | HEIC import on API 28+ | manual (gallery on phone) | — | — |
| SCAN-07 | 2-min session: no ANR, <= 5% deadline-missed, thermal < 2, temperature delta recorded | device script | `bash tools/device-lock.sh bash tools/check-scan-session.sh 120` | no, Wave 0 |
| D-15 | 16 KB + ABI record | static | `zipalign -c -P 16 -v 4 app-release-unsigned.apk`; `llvm-readelf -Wl` on extracted .so | — |

### Sampling Rate
- **Per task commit:** the quick run command (JVM, under 60 s warm)
- **Per wave merge:** full JVM suite + instrumented on the phone (or the emulator for UI-only waves)
- **Phase gate:** full suite green + `check-scan-session.sh 120` on the reference phone + deferred Phase 3 gates (`cold-start.sh`, `check-jank.sh idle 10`, `check-jank.sh swipe 3`, all with `PKG=app.palm.dev`) before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `tools/gen_landmark_fixtures.py` + `app/src/test/resources/landmarks/*.json`
- [ ] `tools/gen_test_photos.py` (run with `backend/.venv/Scripts/python.exe -I`) + `app/src/androidTest/assets/photos/{orient6_gps.jpg,huge_100mp.jpg,no_hand.jpg}`
- [ ] `HandGuidanceTest`, `GuidanceStabilizerTest`, `CropMathTest`, `CameraPermissionTest`, `ScanViewModelTest` (JVM)
- [ ] `PhotoProcessorTest` (instrumented; fake `HandDetector` for the geometry/EXIF cases, real MediaPipe for no-hand)
- [ ] `ScanFallbackTest` (Compose)
- [ ] `tools/check-scan-session.sh`
- [ ] `.gitignore` entry for recorded fixtures (`app/src/test/resources/landmarks/recorded/`); tests skip with `Assume` when absent

## Security Domain

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | yes (runtime permission) | CAMERA asked in context; photo picker grants per-item read only; no storage permission |
| V5 Input Validation | yes | Untrusted gallery files: bounds pass first, megapixel cap, MIME/decoder failure -> "unsupported photo", catch `IOException`/`ImageDecoder.DecodeException`/`IllegalArgumentException`; landmark values finite and clamped before the backend sees them |
| V6 Cryptography | no | — |
| V8 Data Protection | yes | Only the latest capture in `cacheDir`, deleted on Retake and at app start; no images or landmarks in logs; `allowBackup=false` already; recorded hand fixtures stay git-ignored unless the user consents (repo is public) |
| V12 Files | yes | Read via `ContentResolver` only; never write to shared storage; temp names are app-generated, not taken from the URI |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Decompression bomb / huge dimensions | DoS | `inJustDecodeBounds` / `ImageDecoder` info before decode, sample size, MP cap |
| Malformed image crashes the decoder | DoS | Catch and map to the "unsupported photo" message; decode off the main thread |
| GPS/EXIF leaking to the server | Information disclosure | Re-encode via `Bitmap.compress`; byte-level test |
| Hand geometry treated as a biometric | Information disclosure | No persistence beyond the latest capture; no logging; fixture consent gate |
| Third-party telemetry (MediaPipe metrics) | Information disclosure | Disclose in privacy policy / Data safety (REL item); input data is not sent per Google |
| Debug recorder shipped in release | Information disclosure | Debug source set only (the `DebugEntries` pattern already used for the showcase); release `classes*.dex` grep like 03-04-T3 |

## Sources

### Primary (HIGH confidence)
- Google Maven `maven-metadata.xml` and POMs: camera-core/-camera2/-lifecycle/-view 1.7.0, tasks-vision/tasks-core 1.2.3, exifinterface 1.4.2, concurrent-futures-ktx, heifwriter (fetched with curl; AAR sizes from HTTP HEAD)
- GCS and Hugging Face HTTP HEAD for `hand_landmarker.task` (size, md5, LFS SHA-256, Last-Modified)
- In-repo files read this session: `android/gradle/libs.versions.toml`, `android/app/build.gradle.kts`, `AndroidManifest.xml`, `Routes.kt`, `HandSelectEntries.kt`, `ScanPlaceholderScreen.kt`, `AppNavDisplay.kt`, `MainActivity.kt`, `PalmReadingDto.kt`, `backend/app/main.py`, `tools/check-jank.sh`, `tools/_device.sh`, `tools/gradlew.sh`, Phase 3 summaries/validation
- Emulator `dumpsys thermalservice`, `dumpsys battery`, `getprop ro.product.cpu.abilist`, `getconf PAGE_SIZE` (read-only)

### Secondary (MEDIUM confidence, official docs via WebFetch)
- https://developer.android.com/jetpack/androidx/releases/camera (1.7.0 dates, setAutoRotationEnabled, compileSdk notes, Camera2Interop V2)
- https://developer.android.com/reference/androidx/camera/view/CameraController (methods, ViewPort/WYSIWYG, hasCamera rule)
- https://developer.android.com/reference/androidx/camera/core/resolutionselector/ResolutionStrategy ; .../core/ImageProxy ; .../core/CameraState
- https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker and .../android ; HandLandmarker Java reference
- https://raw.githubusercontent.com/google-ai-edge/mediapipe-samples/main/examples/hand_landmarker/android/.../HandLandmarkerHelper.kt
- https://github.com/google-ai-edge/mediapipe/releases ; https://github.com/google-ai-edge/mediapipe/issues/6291 ; https://developers.google.com/edge/mediapipe/solutions/privacy
- https://developer.android.com/guide/practices/page-sizes ; https://developer.android.com/media/platform/supported-formats ; https://developer.android.com/training/data-storage/shared/photo-picker ; https://developer.android.com/guide/topics/manifest/uses-feature-element
- https://huggingface.co/litert-community/MediaPipe-HandLandmarker-Full (licence, limitations)

### Tertiary (LOW confidence)
- [GitHub issue mediapipe-samples #664](https://github.com/google-ai-edge/mediapipe-samples/issues/664) and the [Google AI forum thread](https://discuss.ai.google.dev/t/tflite-vision-task-library-is-not-16kb-compatible/102973) on older unaligned libs (WebSearch)
- The `classify-confidence` seam rates the generic `webfetch` provider LOW even with `--verified`. The tiers above are by source authority (official docs = MEDIUM, registry/tool reads = HIGH).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH (registry metadata + POMs read this session)
- Architecture: MEDIUM-HIGH (official APIs; rotation/crop interplay verified in the docs, final behaviour on device)
- Guidance maths/thresholds: MEDIUM-LOW until calibration (derived geometry, conflicting handedness docs)
- Pitfalls: HIGH for telemetry, uses-feature, 16 KB method, gfxinfo floor; MEDIUM for ImageDecoder/capture rotation details

**Research date:** 2026-11-27
**Valid until:** 2026-12-27 (MediaPipe ships every 1-2 weeks; recheck tasks-vision for 1.2.4+ at the consent checkpoint, but keep the pin)
