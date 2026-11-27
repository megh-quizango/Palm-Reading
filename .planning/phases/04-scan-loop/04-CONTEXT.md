# Phase 4: Scan Loop - Context

**Gathered:** 2026-11-27
**Status:** Ready for planning

<domain>
## Phase Boundary

The Scan route that Phase 3 left as a placeholder becomes the real scanner (PRD S08): camera permission with denied / permanently denied / no-camera fallbacks, a live camera preview with on-device MediaPipe hand guidance (back of hand, closed hand, too far, moving, palm detected) that is steady while the hand is still, capture only while a palm is detected (with focus before capture and a torch toggle), gallery import, and a processing step that turns a captured or imported photo into an upright, palm-cropped, <= 1280 px, EXIF-free JPEG plus the 21 landmarks of that processed photo, held on the device ready for upload. Requirements: SCAN-02, SCAN-03, SCAN-04, SCAN-05, SCAN-06, SCAN-07. Not in this phase: the upload, the backend call, the Generating screen and the result (Phases 5-6); the processed capture ends on a placeholder "ready" screen. No ads.

</domain>

<decisions>
## Implementation Decisions

The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1; Phase 4 planning started at the user's "Plan Phase 4"). Decisions below are Claude's, grounded in `replica/PRD.md` §3 S08 / §5 scan specs, `.claude/CLAUDE.md` (CameraX and MediaPipe setup), the Phase 2 backend contract (`backend/app/main.py` landmarks field) and the Phase 3 code.

### Route and screen
- **D-01:** The Phase 3 `Scan(hand)` route keeps its key and argument; its placeholder screen is replaced by the real scanner. The route keeps its background opt-out (no sky). Back from Scan returns to HandSelect. A new `ScanReady` placeholder route (shows the processed photo, the hand, the landmark count, "Retake" and a disabled "Read my palm (next update)") is where a processed capture lands until Phase 5/6 replace it. Processed captures are handed over through a `ScanResultHolder` (in-memory singleton + one file in `cacheDir`), not through route arguments.
- **D-02:** Layout follows Figma "3 · Screens" S08 (the UI researcher maps it): full-bleed preview, palm-outline guide, one guidance chip with icon + text (localized en/hi), capture button, torch toggle, gallery button, back. Guidance text changes are announced to TalkBack (polite live region). No sky behind the preview.

### Camera (SCAN-02, SCAN-04)
- **D-03:** CameraX 1.7.0 with `LifecycleCameraController` + `PreviewView` (`ImplementationMode.PERFORMANCE`) in `AndroidView`, back camera only, IMAGE_ANALYSIS + IMAGE_CAPTURE. One 4:3 `ResolutionSelector` on all use cases so overlay, analysis frame and capture share one field of view. Analysis: `OUTPUT_IMAGE_FORMAT_RGBA_8888`, `STRATEGY_KEEP_ONLY_LATEST`, ~640x480 (`FALLBACK_RULE_CLOSEST_LOWER`), dedicated single-thread executor, every `ImageProxy` closed in `finally`, frames throttled to <= 12 fps before MediaPipe. Capture: `CAPTURE_MODE_MINIMIZE_LATENCY`, >= 1080 px short side when available.
- **D-04:** Permission: `CAMERA` is requested on entering Scan (not before). States: granted; denied (rationale available) -> explanation with "Allow camera" and "Choose from gallery"; permanently denied -> explanation with "Open settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`) and "Choose from gallery"; no back camera / camera open error -> gallery fallback screen. Permission is re-checked on resume so returning from Settings works. `uses-feature android.hardware.camera.any required=false`.
- **D-05:** Capture is enabled only in the PalmDetected state. Before capture, a focus-and-metering action at the palm centre (from the landmarks) runs with a 1 s cap, then the photo is taken; tap-to-focus on the preview also works. The flash toggle is the torch (`enableTorch`), shown only if the camera has a flash unit, turned off when the screen leaves.

### Guidance (SCAN-03)
- **D-06:** MediaPipe Tasks Vision `com.google.mediapipe:tasks-vision:1.2.3` (pinned) with `hand_landmarker.task` float16 bundled in `assets/` (stored uncompressed). LIVE_STREAM mode for the preview, IMAGE mode for the processing step; `numHands=1`, CPU delegate, confidences 0.5. Created off the main thread, closed when the screen leaves.
- **D-07:** A pure-Kotlin `HandGuidance` (no Android types; input = 21 landmarks + handedness + image size + timestamp) classifies each frame: NoHand, BackOfHand, ClosedHand, TooFar, Moving, PalmDetected; plus an advisory WrongHand hint when the detected hand confidently differs from the chosen hand (it never blocks capture). Palm vs back from the sign of the palm normal (cross product of wrist->index MCP and wrist->pinky MCP) combined with handedness, corrected for the rear camera (no mirroring). Closed hand from fingertip-to-wrist vs MCP-to-wrist distance ratios. Too far from the hand bounding box relative to the frame. Moving from palm-centre displacement normalised by hand size over a short window. All thresholds live in one `GuidanceConfig`.
- **D-08:** Stability: a raw state must persist for a dwell time (about 250 ms) before the shown state changes, and PalmDetected must hold for about 500 ms before capture is enabled; a short dropout (< ~300 ms of NoHand) does not reset PalmDetected. The stabiliser is pure Kotlin and unit-tested with frame sequences.
- **D-09:** Fixtures: landmark fixtures are JSON (21 normalised points, handedness, label, sequence timestamps) under `app/src/test/resources/landmarks/`. CI fixtures are synthetic, generated by a committed script from a canonical hand pose (mirror for left/right, palm normal flip for back, finger curl for fist, scale for far, rotation, jitter and drift for moving). Real recorded fixtures come from a debug-only "record landmarks" control on the scan screen (writes JSON to app files; pulled with adb) during the on-device calibration checkpoint; they are committed only if the user agrees at that checkpoint (they are the user's own hand geometry and the repo is public), otherwise they stay git-ignored and only the derived thresholds are committed. Thresholds are calibrated on the phone with both hands and recorded in the plan summary.

### Processing (SCAN-05, SCAN-06)
- **D-10:** One `PhotoProcessor` pipeline for captures and imports, off the main thread: bounded decode (ImageDecoder with target size on API 28+, BitmapFactory with `inSampleSize` plus `ExifInterface` orientation on API 26-27) so a 100 MP or HEIC photo never decodes at full size; bake the rotation upright; run the IMAGE-mode landmarker on the upright image; no hand -> reject with a clear message ("We couldn't find a hand in this photo. Try a clear photo of your open palm."); crop to the landmark bounding box expanded by a margin and clamped to the image; scale so the long edge is <= 1280 px; re-encode JPEG q85 with `Bitmap.compress` (writes no EXIF); map the 21 landmarks into the cropped image's normalised coordinates (what the backend expects: 21 `[x, y, z]`). HEIC on API 26-27 that cannot be decoded gets a clear "unsupported photo" message.
- **D-11:** Gallery import uses `ActivityResultContracts.PickVisualMedia(ImageOnly)` (no storage permission), reachable from the scanner and from both permission fallback screens. Imports go through the same pipeline and the same no-hand rejection.
- **D-12:** Output is a `ProcessedCapture` (JPEG bytes in a `cacheDir` file, width, height, hand side, 21 landmarks, source camera|gallery). Only the latest capture is kept; it is deleted on Retake and at app start. No photo is written anywhere else, and neither images nor landmarks are logged (privacy constraint).

### Performance and verification (SCAN-07)
- **D-13:** A 2-minute scan session on a physical reference phone: no ANR (logcat), frame-deadline-missed share <= 5% (the Phase 3 A10 metric via gfxinfo), and no thermal throttling (`dumpsys thermalservice` status stays below moderate; battery temperature rise recorded). Analysis pauses when the screen is not resumed. The same phone session also runs the Phase 3 hardware gates that were deferred (release cold start < 2 s, Home idle and intro swipe <= 5% missed frames) because it needs the same device time.
- **D-14:** Camera and guidance cannot be proven on the emulator (no real hand in its virtual camera). Functional and unit work runs anywhere; the guidance calibration, capture and SCAN-07 checks need a physical phone (Redmi Note 10T over wireless adb, or the M2006C3MII). Lessons from Phase 3 apply: never uninstall on MIUI (update installs only), keep test APKs installed, MIUI background pop-up permission for instrumented tests, restore every changed device setting.
- **D-15:** 16 KB pages: check whether the MediaPipe 1.2.3 native libraries are 16 KB-aligned (ELF `p_align` and zip alignment in the built APK) and record the result. Not aligned is a REL-03 release risk, not a Phase 4 blocker.
- **D-16:** Tests: pure Kotlin unit tests for HandGuidance, the stabiliser, crop/scale/landmark mapping maths and the permission state reducer; synthetic-fixture tests in CI; instrumented tests for the processing pipeline (orientation-6 JPEG, a large image, HEIC on API 28+, no-hand image, output has no EXIF/GPS via `ExifInterface`); a Compose UI test for the permission fallback screens; on-device checks for live guidance, capture and the 2-minute session.

### Dependencies (download consent)
- **D-17:** New downloads expected: `androidx.camera:camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` 1.7.0 (Google Maven), `com.google.mediapipe:tasks-vision:1.2.3` (Google Maven) with its transitive dependencies, `androidx.exifinterface:exifinterface:1.4.2` (Google Maven), and the `hand_landmarker.task` model file (float16, 7,819,105 bytes, Apache-2.0) from `storage.googleapis.com/mediapipe-models/...`. All are listed in one consent checkpoint at the start of the first plan; nothing is downloaded before the user approves. Include the model's Apache-2.0 notice.

### Claude's Discretion
- Package layout (e.g. `feature/scan`, `core/camera`, `core/vision`), exact threshold starting values, guidance copy wording, overlay drawing, and test split, as long as D-01..D-17 hold.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/ROADMAP.md` §Phase 4 (goal, 5 success criteria, research flag)
- `.planning/REQUIREMENTS.md` (SCAN-02..07)
- `.planning/research/SUMMARY.md`, `STACK.md`, `ARCHITECTURE.md`, `PITFALLS.md` (CameraX, MediaPipe, low-end phones)
- `.claude/CLAUDE.md` (pinned versions; CameraX and MediaPipe setup sections; what not to use)
- `.planning/phases/03-client-shell/03-CONTEXT.md`, `03-*-SUMMARY.md` (Scan route, background opt-out, MIUI device lessons, tools/ui.sh, tools/device-lock.sh, check-jank.sh, cold-start.sh)
- `backend/app/main.py` `_parse_landmarks` (21 `[x, y, z]`, x/y in -0.5..1.5) and `contracts/` (palm-reading contract)
- Figma "[AppName] – UI" (file key `Ej98dV7fZQUhRwUyfQIlyJ`): page "3 · Screens" S08 scan frames and states; "2 · Components"
- `replica/PRD.md` §3 S08, §5 scan specs, §10 performance
- MediaPipe Hand Landmarker Android guide; CameraX 1.7.0 release notes
- Clean room: never copy from `_extract/`

</canonical_refs>

<code_context>
## Existing Code Insights

- Nav3 routes in `core/navigation/Routes.kt` (`Scan(hand)`, `pushFrom`/`popFrom` helpers), `AppNavDisplay` with per-route background opt-out; `feature/handselect/ScanPlaceholderScreen.kt` is what this phase replaces.
- Design system: Nocturne tokens, buttons, chips, `Effects.kt` (`rasterizedBehind` for static layers); strings in `values` + `values-hi`.
- No camera, MediaPipe or EXIF dependencies yet; manifest has only INTERNET.
- Dev tools: `tools/ui.sh`, `tools/device-lock.sh`, `tools/check-jank.sh`, `tools/check-motion.sh`, `tools/cold-start.sh`, `tools/screen.py`.

</code_context>

<specifics>
## Specific Ideas

- Tracer first: permission -> preview -> raw MediaPipe state chip -> capture -> processed JPEG on the ScanReady placeholder, end to end on the phone; then guidance quality, fallbacks, gallery, hardening.
- Calibration is a human-in-the-loop checkpoint: the user holds each hand in each pose while the debug overlay shows raw scores.

</specifics>

<deferred>
## Deferred Ideas

- Upload, job polling, Generating screen, result (Phases 5-6).
- Consented palm photo set for the model bake-off (Phase 2, 02-08) - not needed for this phase's guidance, which uses landmark fixtures.
- Front camera / selfie palm scan, multiple hands, auto-capture - not in M1.

</deferred>

---

*Phase: 04-scan-loop*
