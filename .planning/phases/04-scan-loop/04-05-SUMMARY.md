---
phase: 04-scan-loop
plan: 05
subsystem: scan
status: partial
tags: [scan, debug-hud, landmark-recorder, scan-07, 16kb, emulator, phone-pending]
requires: [04-01, 04-02, 04-03, 04-04]
provides:
  - "ScanDebugHud (debug) + release no-op twin; LandmarkRecorder (debug only)"
  - "tools/check-scan-session.sh (SCAN-07 session gate)"
  - "16 KB alignment record of the release APK (D-15)"
affects: [android/app/src/main/java/app/palm/feature/scan/ScannerContent.kt, android/app/src/main/java/app/palm/feature/scan/ScannerViewModel.kt, .planning/STATE.md]
tech-stack:
  added: []
  patterns: ["debug/release source-set twin for debug tooling (as DebugEntries.kt)"]
key-files:
  created:
    - android/app/src/debug/java/app/palm/debug/ScanDebug.kt
    - android/app/src/release/java/app/palm/debug/ScanDebug.kt
    - tools/check-scan-session.sh
  modified:
    - android/app/src/main/java/app/palm/feature/scan/ScannerContent.kt
    - android/app/src/main/java/app/palm/feature/scan/ScannerViewModel.kt
    - .planning/STATE.md
decisions:
  - "04-05 split by the user's choice ('Continue on emulator'): Task 1 tooling + every emulator-runnable check now; Task 2 (real hands) and Task 3 (calibration, fixtures decision) wait for the reference phone"
  - "Release scan session cannot run on emulator-5554: the release APK is ARM-only and MediaPipe segfaults under the x86_64 native bridge; the emulator session ran on the debug build (x86_64 libs) instead"
  - "16 KB page alignment (D-15) verified on the release APK: all native libs LOAD align 0x4000, zipalign -P 16 OK; no REL-03 risk from alignment"
metrics:
  duration: "~25 min"
  completed: 2026-11-28
actuals:
  tokens: 9000
  tasks: 1
  commits: 2
plan_head_before: 572650c1217a57da696dc201fe22ddeb3e581347
plan_head_after: 9ea6c60a00330318a5b52cc00f77f8c6efe31a9c
---

# Phase 4 Plan 05: Phone gate and calibration — emulator part (PARTIAL)

**What this run did:** a debug-only scanner HUD with a 3 s landmark recorder (the release twin is empty and ships no recorder), the SCAN-07 session gate script, release-APK checks, a real 16 KB alignment check (all native libraries aligned to 16 KB), and every check the emulator can run, all on emulator-5554 with emulator numbers labelled. Task 2 (real hands, blocking-human) and Task 3 (calibration from those recordings) were **not run, not faked, and no thresholds changed**. They wait for the reference phone.

Status: **partial**. SCAN-02..07 are not marked complete; the phase stays in progress in STATE/ROADMAP.

## Device

emulator-5554: `sdk_gphone64_x86_64`, API 35, abilist `x86_64,arm64-v8a`. No physical phone was attached. Every number below marked *(emulator, not a gate)* is for the record only.

## Task 1: built

| Artifact | What |
|---|---|
| `android/app/src/debug/java/app/palm/debug/ScanDebug.kt` | `ScanDebugHud(samples, hand, modifier)`: a "HUD" chip at top end under the guidance chip; when expanded, on a SurfaceScrim@0.72 panel: raw state, raw handedness + score, identity / wrong hand, chirality, curled fingers, hand fraction, motion, analysed fps; pose chips palm/back/fist/far/moving; "Record 3 s" → `LandmarkRecorder` writes `files/landmarks/HAND_POSE.json` in the 04-02 fixture shape (kotlinx Json, Dispatchers.IO, no logging); "no hand seen, nothing saved" if no frame had a hand |
| `android/app/src/release/java/app/palm/debug/ScanDebug.kt` | same `ScanDebugHud` signature, empty body, no recorder class |
| `ScannerContent.kt` | one `ScanDebugHud(viewModel.debug, hand, ...)` call above the overlay, below the chips |
| `ScannerViewModel.kt` | `ScanDebugSample` got `timestampMs` (see deviation 1) |
| `tools/check-scan-session.sh SECONDS` | validates SECONDS before sourcing `_device.sh` (usage + exit 2); clears logcat, resets gfxinfo, samples `Thermal Status` every 10 s (fails if the line is missing), then reads frames / `Number Frame deadline missed` (missed-vsync fallback), battery temperature start/end, `am_anr` + `ANR in PKG`, pidof + resumed activity (prints `mWakefulness` when that fails); one summary line + gate; WARN when temperature rises > 8.0 C; changes no setting. Env PKG, MAX_JANK_PCT=5, MIN_FRAMES=SECONDS/2, MAX_THERMAL=1 |

HUD exercised on the emulator: expanded with `raw NoHand`, `label -`, `chirality 0 curled 0`, `fraction 0.000`, `motion 0.000`, `fps 10` (the PalmScan log at the same time showed 47-49 frames per 5 s); "Record 3 s" with no hand → "no hand seen, nothing saved", `files/landmarks` not created; collapses again.

## Task 1 verify results

| Verify | Result |
|---|---|
| `bash tools/gradlew.sh :app:assembleDebug :app:assembleRelease && bash tools/check-scan-session.sh; test $? -eq 2` | pass (both builds OK; `usage: ... SECONDS`, exit 2) |
| Release dex has no `LandmarkRecorder`, debug dex has it; no `lib/x86*` in release; `lib/arm64-v8a/libmediapipe_tasks_jni.so` present; `assets/hand_landmarker.task` `Stored` | pass, all five checks |
| 16 KB check (D-15), **host-side, real** | pass (see below). The plan's `unzip ... 'lib/*'` matches nothing with this unzip (`*` does not cross `/`), so `'lib/*/*'` was used |
| Release onboarding + cold start + Home idle jank (release `app.palm.dev`) | onboarding OK; cold start below; idle jank 5.91% *(emulator, not a gate)* |
| Release intro swipe jank | 8.71% *(emulator, not a gate)* |
| Release scan session 120 s | **could not run on the emulator**: release app crashes on opening the scanner (native SIGSEGV in `libmediapipe_tasks_jni.so` under `libnative_bridge_vdso.so`; arm64 code on the x86_64 emulator's translation layer). Not an app bug: release `abiFilters` are ARM-only on purpose. The session ran on the debug build instead (below) |

### 16 KB page alignment (D-15): real result

```
lib/arm64-v8a/libandroidx.graphics.path.so: LOAD align 0x4000
lib/arm64-v8a/libdatastore_shared_counter.so: LOAD align 0x4000
lib/arm64-v8a/libimage_processing_util_jni.so: LOAD align 0x4000
lib/arm64-v8a/libmediapipe_tasks_jni.so: LOAD align 0x4000
lib/arm64-v8a/libsqliteJni.so: LOAD align 0x4000
lib/arm64-v8a/libsurface_util_jni.so: LOAD align 0x4000
lib/armeabi-v7a/libandroidx.graphics.path.so: LOAD align 0x4000
lib/armeabi-v7a/libdatastore_shared_counter.so: LOAD align 0x4000
lib/armeabi-v7a/libimage_processing_util_jni.so: LOAD align 0x4000
lib/armeabi-v7a/libmediapipe_tasks_jni.so: LOAD align 0x4000
lib/armeabi-v7a/libsqliteJni.so: LOAD align 0x4000
lib/armeabi-v7a/libsurface_util_jni.so: LOAD align 0x4000
zipalign -c -P 16 -v 4 app-release.apk: every lib/*.so (OK) ... Verification successful
```

All 12 release libraries are 16 KB aligned and stored 16 KB aligned in the APK. The 16 KB part of the STATE.md Phase 4 blocker is resolved; no REL-03 line needed.

### Emulator numbers *(emulator, not a gate)*

| Measure | Value |
|---|---|
| Release cold start, fresh (5 runs) | 654, 579, 676, 651, 688 ms → median **654 ms** |
| Release cold start after `--compile` (speed-profile, filter `x86_64: [status=speed-profile]`) | 668, 684, 633, 657, 634 ms → median **657 ms** |
| Release Home idle 10 s | 575 frames, 34 deadline missed (**5.91%**), p50 27 ms, p90 36 ms, p99 53 ms, missed vsync 21 |
| Release intro 3 swipes | 241 frames, 21 deadline missed (**8.71%**), p50 27 ms, p90 38 ms, p99 53 ms |
| Scan session, **debug** build, no hand, 120 s (`check-scan-session.sh 120`) | `session: 120s frames=6817 missed=405 (5.94%, frame deadline missed) max_thermal=0 temp=25.0C->25.0C delta=0.0C anr=0` → script verdict FAILED on 5.94% > 5%; no ANR, app alive and resumed, thermal 0, emulator battery temperature is fixed at 25 C |
| Analysis rate (debug, no hand) | 47-49 frames per 5 s (≈ 9.6 fps, under the 12 fps cap), inference 25-29 ms |
| Analysis pause on background | Home pressed: **no** `PalmScan: analyzed` line in 12 s; same process (pid unchanged); back to the app: 47 frames per 5 s again and the scanner shows "Place your palm inside the frame" |

The emulator misses about 6% of frame deadlines even on idle Home (p50 27 ms, so the emulator renders at roughly 37 fps). The 5.94% debug scan session and the 5.91% release Home idle come from different builds and are not compared here; the like-for-like debug Home idle baseline is in "Review fixes" below. These are emulator rendering numbers, not phone numbers.

## Deferred checks run on the emulator

| From | Check | Result on emulator-5554 |
|---|---|---|
| 04-01 #2 | install-debug, `scan-open.sh --grant`, scanner up, no "hand guide couldn't start", `analyzed [1-9]` in log | exit 0 (`analyzed 49 frames, inference 29 ms`) |
| 04-01 #3, 04-03 #1, 04-04 #1 | HandSelectTest, ScanFallbackTest, PhotoProcessorTest | full connected suite (below): 1, 6 and 9 tests passed. `heicImport` **ran** (not skipped, 0.70 s) on API 35 |
| 04-01 #4, 04-03 #2 (scripted part) | revoke → system dialog → Don't allow → "Allow camera" + "Choose from gallery" → Allow camera → Don't allow → "Open settings" + "Choose from gallery" | passed twice per font scale as part of the screenshot walk (Settings page and the real Settings toggle were not walked here) |
| 04-04 #6 | gallery import of a no-hand photo (`/sdcard/Pictures/no_hand_test.jpg` via the system photo picker) | "No hand found" with "Choose another photo" and "Close"; Close → live scanner. Screenshot `phase4-en-gallery-nohand.png` |
| 04-02 #10 | Scan entered sideways with auto-rotate on (`adb emu rotate` on Home) | Home ROTATION_270 → Scan ROTATION_0, stays 0 after 4 s → Back to HandSelect/Home ROTATION_270; sensor restored to 0 |
| Step 5 screenshots | en / hi × font scale 1.0 / 2.0 × scanner / Denied / PermanentlyDenied | 12 files (list below). Reviewed hi-scanner-fs2 and hi-permdenied-fs2: Devanagari not clipped, nothing truncated |

Extra observation (rotation **locked** to landscape, `accelerometer_rotation 0`, `user_rotation 1`): entering Scan goes portrait, and on leaving, Home stays portrait. The app restores `SCREEN_ORIENTATION_UNSPECIFIED` correctly (`requestedOrientation=SCREEN_ORIENTATION_UNSPECIFIED`). The system itself rewrote `user_rotation` to 0 when the app asked for portrait, so this is platform behaviour, not an app bug. The settings were restored to `accelerometer_rotation 1`, `user_rotation 0`.

Screenshots (`tools/adb/shots/`, git-ignored, no hand in view): `phase4-en-scanner.png`, `phase4-hi-scanner.png`, `phase4-en-denied.png`, `phase4-hi-denied.png`, `phase4-en-permdenied.png`, `phase4-hi-permdenied.png`, the same six with `-fs2`, plus `phase4-en-gallery-nohand.png` and `phase4-en-scanner-hud.png`. `font_scale.orig` = `1.0`; font scale restored to 1.0 (verified). The app locale for the debug package was set back to `en`.

At font scale 2.0 the debug "HUD" chip slightly overlaps the bottom-right edge of the taller guidance chip. This is debug-only and does not affect release.

## Final green state (emulator-5554)

- `:app:testDebugUnitTest`: 142 tests, 0 failures. 1 skipped: `HandGuidanceTest.recordedFixturesClassifyOnAtLeast90PercentOfFrames`, the expected skip because no recordings exist yet (D-09).
- `:app:assembleDebug`, `:app:assembleRelease`: OK.
- `:app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`, run after the last code commit: AppDatabaseTest 1, PhotoProcessorTest 9, HandSelectTest 1, HomeTest 4, LanguageSwitchTest 1, OnboardingFlowTest 2, ScanFallbackTest 6. That is 24 tests, 0 failures, no `tests="0"` file, and none of the 7 classes missing.
- The emulator is left with the debug app on the live scanner (Right hand) and CAMERA granted. Installs were update installs only; nothing was uninstalled.

## Deviations from Plan

**1. [Rule 1 - Bug] HUD fps and recorder dropped equal no-hand frames.** Found while exercising the HUD: it read `fps 1` while the log showed about 10 fps. Cause: `ScanDebugSample` is a data class in a StateFlow, so identical no-hand samples were never emitted. Fix: added `timestampMs` to `ScanDebugSample`. The ViewModel passes the frame timestamp, and the HUD and recorder use it. Files: ScannerViewModel.kt, ScanDebug.kt. Commit 9ea6c60. Now shows fps 10.

**2. [Precondition / user choice] Task 1 ran on the emulator, not the reference phone.** This was the user's explicit request ("Continue on emulator"). The plan's precondition (physical phone) is unmet, so no Task 1 gate counts as passed. All device numbers are labelled as emulator numbers.

**3. [Blocking, emulator limit] Release scan session swapped to the debug build.** The release APK is ARM-only, and its MediaPipe crashes under the emulator's ARM translation. The release session stays on the phone checklist.

**4. [Verify command] 16 KB unzip pattern.** `'lib/*'` → `'lib/*/*'`. No code change.

**5. [Verify command] `ui.sh tap "^HUD$"` cannot match.** ui.sh matches against lines with coordinates appended, so `^HUD` was used.

Tasks 2 and 3 were not executed by design.

## Phone session still required

Run with only the reference phone attached, or `export ADB_SERIAL=<phone serial>`. On MIUI, set MIUIOP 10021 to Allow first. Run every command from the repo root. Nothing below was faked or marked passed.

**Task 1 on the phone (the real gates):**
1. ABIs: `adb -s "$ADB_SERIAL" shell getprop ro.product.cpu.abilist` → record in STATE.md (replace the "still to be read" note). Also record the phone model.
2. Installs: `bash tools/device-lock.sh bash tools/gradlew.sh :app:installDebug :app:installRelease`
3. Phase 3 gates on release: the plan's Task 1 verify command 4 (onboarding + `BUDGET_MS=100000 bash tools/cold-start.sh && bash tools/cold-start.sh --compile` + `check-jank.sh idle 10`). Gate: compiled median < 2000 ms, idle ≤ 5%.
4. Intro swipes on release: Task 1 verify command 5 (`check-jank.sh swipe 3`). Gate ≤ 5%. 03-05 named the intro pager as the likely fix if it fails.
5. Release SCAN-07 session, no hand: `bash tools/device-lock.sh bash -c 'export PKG=app.palm.dev && bash tools/scan-open.sh --grant && bash tools/ui.sh expect "Place your palm inside the frame" && bash tools/check-scan-session.sh 120'`. Gate: no ANR, ≤ 5% deadline missed, thermal ≤ 1, app resumed. Paste the session line here.
6. Phone screenshots (step 5): re-run the en/hi × fs 1.0/2.0 × scanner/denied/permdenied walk on the phone (on API 30-32, switch to Hindi through `tools/ui.sh reset` + the Language screen). The emulator set exists already.

**Task 2 (checkpoint:human-verify, blocking-human, real hands):** every step 1-10 of the plan's Task 2 `how-to-verify`. This covers HUD calibration recordings (right and left × palm/back/fist/far/moving, 10 files), live guidance per hand, wrong-hand hint, hold gate, focus-then-capture, tap-to-focus, torch on/off and off after leaving, permissions with the real Settings toggle, HEIC gallery photo (D-16), no-hand gallery photo, the 2-minute palm-in-view release session with stutter/heat impressions, white wall / dark room legibility, optional TalkBack, the **fixtures consent answer** ("commit" or "keep local"), and a decision on any Phase 3 gate failure. Check the recordings with: `L=$(bash tools/device-lock.sh bash -c 'source tools/_device.sh && adb -s "$ADB_SERIAL" shell run-as app.palm.dev.debug ls files/landmarks') && test "$(printf '%s\n' "$L" | grep -c '\.json')" -ge 10`.

**Task 3 (after Task 2):** pull the recordings into `android/app/src/test/resources/landmarks/recorded/` (`adb exec-out run-as app.palm.dev.debug cat files/landmarks/NAME.json`), then `python -I tools/gen_landmark_fixtures.py --stats android/app/src/test/resources/landmarks/recorded`. Derive the GuidanceConfig values (swapHandedness, rightPalmChirality, curlRatio, tooFarEnter/Exit, moveEnter/Exit) with a "calibrated" comment, mirror them in the generator and run `--check`. Apply the fixtures decision, including the ROADMAP criterion 2 rewording plus the STATE deviation line if the recordings stay local. Then run `bash tools/gradlew.sh :app:testDebugUnitTest` (with no `<skipped` in the HandGuidanceTest XML) and the full `connectedDebugAndroidTest` on the phone. Finally, mark the calibration blocker resolved in STATE.md and SCAN-02..07 complete.

**Carried phone-only items from earlier summaries (not doable on the emulator):**
- 04-01 #5-7: real palm capture → "Your palm photo" upright with 21 dots, then `bash tools/device-lock.sh bash -c 'bash tools/ui.sh expect "21 hand points" && bash tools/check-capture.sh'` → `capture OK: WxH, N bytes` with max ≤ 1280 (EXIF/rotation proof). The emulator camera scene has no hand, so ScanReady cannot be reached.
- 04-02 #1-7, #9: live states with each hand, wrong-hand hint, hold gate timing, tap-to-focus, torch (Home/shutter/Back), TalkBack order and single announcements, overlay alignment on 20:9, and the pull-back test.
- 04-03 #3-4 and the Recents→Settings camera-off walk: real Settings toggle, camera in use by another app.
- 04-04 #2-5, #7, #9: real HEIC import + check-capture, processing scrim and Back-cancel (`run-as app.palm.dev.debug ls cache/scan` shows no capture.jpg), camera-still rejection with Retake only, photo-gone after `am kill`, photo-picker backport (API 26-29 only), and rotation/night-mode during processing.
- ~~STATE Phase 3 note: check the Redmi for leftover test settings~~ RESOLVED 2026-11-28 (Task 1: all defaults).

## Phone session (Task 1, 100.104.73.89:43695)

Run 2026-11-28 on the Redmi Note 10T 5G with `export ADB_SERIAL=100.104.73.89:43695` for every command (emulator-5554 and two other phones were attached and not touched). Every command ran from the repo root. Nothing was rerun, and each gate passed on its first run. Tasks 2 and 3 were not run (real hands, user decisions).

**Step 1, device facts:** model `M2103K19I`, abilist `arm64-v8a,armeabi-v7a,armeabi`, Android 13 / SDK 33, MIUI V140 (MIUI 14), SoC MT6833, 1080x2400. Recorded in STATE.md.

**Settings before the session:** font_scale 1.0; window / transition / animator scale 1.0 / 1.0 / 1.0; low_power 0; accelerometer_rotation 0; app locale of the debug package `[en]`. So the Phase 3 "leftover test setting" note was clean.

| Step | Command | Output line | Result |
|---|---|---|---|
| 2 | `bash tools/device-lock.sh bash tools/gradlew.sh :app:installDebug :app:installRelease` | `Installing APK 'app-release.apk, app-release.dm' on 'M2103K19I - 13' for :app:release` / `Installed on 1 device.` / `BUILD SUCCESSFUL in 3m 46s`; lastUpdateTime debug 15:31:12, release 15:32:19 | PASS (update installs, nothing uninstalled) |
| 3 | verify command 4 (release onboarding, `BUDGET_MS=100000 bash tools/cold-start.sh`, `bash tools/cold-start.sh --compile`, `check-jank.sh idle 10`), exit 0 | fresh: `run 1: 406 ms` … `run 5: 347 ms` (406, 353, 391, 342, 347), `median 353 ms over 5 runs (budget 100000 ms)` | recorded |
| 3 | same run, after `--compile` (`arm64: [status=speed-profile] [reason=cmdline]`) | 311, 269, 293, 292, 305 ms, `median 293 ms over 5 runs (budget 2000 ms)` | **PASS** (< 2000 ms) |
| 3 | same run, Home idle 10 s | `jank OK: 0 of 619 frames missed the deadline (frame deadline missed)`; p50 22 ms, p90 23 ms, p99 24 ms, missed vsync 0 | **PASS** (0.00% ≤ 5%) |
| 4 | verify command 5 (`check-jank.sh swipe 3` on the release intro), exit 0 | `jank OK: 2 of 302 frames missed the deadline (frame deadline missed)`; p50 22 ms, p90 27 ms, p99 27 ms | **PASS** (0.66% ≤ 5%). No intro pager fix needed (the debug build measured 37.5% in Phase 3) |
| 5 | `bash tools/device-lock.sh bash -c 'export PKG=app.palm.dev && bash tools/scan-open.sh --grant && bash tools/ui.sh expect "Place your palm inside the frame" && bash tools/check-scan-session.sh 120'`, exit 0 | `session: 120s frames=6096 missed=0 (0.00%, frame deadline missed) max_thermal=0 temp=32.8C->33.3C delta=0.5C anr=0` / `session OK: 0.00% frame deadline missed, max thermal 0, no ANR, app.palm.dev resumed` | **PASS** (no ANR, 0.00% ≤ 5%, thermal 0 ≤ 1, resumed) |

After the session, the release scanner still showed "Place your palm inside the frame" with no "couldn't start" text, and the crash buffer had 0 `palm` lines. The phone camera saw no hand (dark scene).

**Step 6, screenshots** (debug build, `tools/adb/shots/`, git-ignored, no hand in view). The app locale was switched with `cmd locale set-app-locales app.palm.dev.debug --locales hi|en` (API 33). Each screen was reached in English, and the locale was switched on that screen. Files: `phone-phase4-{en,hi}-{scanner,denied,permdenied}.png` and the same six with `-fs2`, 12 files in total. I reviewed hi-scanner-fs2 and hi-permdenied-fs2: the Devanagari is not clipped or truncated. At fs 2.0 the debug-only HUD chip overlaps the edge of the guidance chip, as on the emulator. `font_scale.orig` = `1.0` (re-read from the phone).

**Finding (resolved 2026-11-28, not an app bug, a0b8ac5):** on the Denied screen in Hindi, `ui.sh tap "कैमरे की अनुमति दें"` did nothing 3/3. ui.sh taps the first dump line that matches, and the Hindi title "स्कैन के लिए कैमरे की अनुमति दें" contains the button label and is listed first, so the walk tapped the title, not the button (English "Allow camera" is not part of "Allow the camera to scan", so en worked). Reproduced on emulator-5554 (API 35): the unanchored selector taps the title centre and starts nothing; `ui.sh tap "^कैमरे की अनुमति दें"` hits the button and starts GrantPermissionsActivity after `set-app-locales hi`, rotation and process recreation. On the phone the button also worked after the hi -> en relaunches. The in-app Language screen uses the same LocaleManager relaunch on API 33+ and exists only in onboarding. a0b8ac5 adds the anchoring note to ui.sh; no app change.

**Settings after the session (restored):** font_scale 1.0 (equal to `font_scale.orig`); app locale of the debug package `[en]`; animations, battery saver and rotation were never changed. CAMERA is granted for the debug package, and the debug app is left on the live scanner (Right hand, English) for Task 2. No appops or security setting was touched; MIUIOP 10021 was not needed for Task 1.

## Known Stubs

None. The HUD is debug-only tooling.

## Threat Flags

None beyond the plan's threat model. T-04-20 holds: the recorder is absent from the release dex and present in debug. T-04-22 holds: screenshots are git-ignored and taken with no hand in view. T-04-23 holds: font scale, rotation settings and app locale were restored, and CAMERA was left granted for the debug package. T-04-SC holds: no new dependency or download.

## Self-Check: PASSED

- FOUND: android/app/src/debug/java/app/palm/debug/ScanDebug.kt, android/app/src/release/java/app/palm/debug/ScanDebug.kt, tools/check-scan-session.sh
- FOUND commits: db185f4, 9ea6c60
- Acceptance greps: `class LandmarkRecorder` in debug only, `fun ScanDebugHud` in release, `ScanDebugHud` in ScannerContent, `am_anr` / `Thermal Status` / `temperature` / `deadline missed` in the script; 14 `phase4-*.png` files; font scale equals `font_scale.orig`

## Review fixes

Five code-review findings, all fixed in one commit (`fix(04-05): address review findings`) and rerun on emulator-5554:

1. **`num()` under `set -euo pipefail` (tools/check-scan-session.sh, tools/check-jank.sh).** When grep found no line, the command substitution failed and the script exited 1 with no output. That meant the missed-vsync fallback (Android < 12, i.e. the API 26-30 phones) and the "no frame stats" message could never run. Fix: `|| true` inside `num()` in both scripts. Self-check: the `num()` line was extracted from each script and run under `set -euo pipefail` against a gfxinfo with only `Total frames rendered: 120` and `Number Missed Vsync: 3`. It returned total 120 and fell back to missed 3. An empty gfxinfo gave empty values, and the script carried on to its check. Both scripts printed `parser OK`.
2. **`logcat -c` left the events buffer (check-scan-session.sh).** An old `am_anr` could fail a new session. Fix: `logcat -b all -c`. Rerun: the events buffer held 67 lines after the 120 s session, so only the session itself was counted.
3. **Unsupported 5.94% vs 5.91% comparison (this summary).** The comparison mixed a debug build with a release build. It was dropped (text above) and replaced with a debug-vs-debug measurement on the same emulator *(emulator, not a gate)*:

   | Debug build, emulator-5554 | Frames | Deadline missed |
   |---|---|---|
   | Scan session 120 s, no hand (rerun with the fixed script) | 6836 | 393 (**5.75%**), max_thermal 0, anr 0, resumed. Script verdict FAILED on > 5% |
   | Home idle 10 s, run 1 (`check-jank.sh idle 10`) | 576 | 33 (**5.73%**), p50 27 ms |
   | Home idle 10 s, run 2 | 571 | 35 (**6.13%**), p50 28 ms |

   On the same build, the scanner sits inside the idle-Home spread (5.73-6.13%). Both are above 5% because of the emulator's renderer, so this says nothing about the phone gate.
4. **Stale STATE position lines.** The Plan line now reads "04-01..04-04 complete, 04-05 partial (emulator part done, phone part pending)", and Last activity now names the 04-05 emulator part.
5. **Recording cancelled silently by collapsing the HUD (ScanDebug.kt).** `recording` is hoisted into `ScanDebugHud`, and the HUD chip is `clickable(enabled = !recording)`, so the panel and its coroutine scope stay alive for the whole 3 s capture. Rerun on the emulator: expand HUD → "Record 3 s" → tapped the HUD chip twice during "Recording…". The panel stayed open, and after 3 s it showed "no hand seen, nothing saved". After that the chip collapsed the panel normally.

Builds: `:app:installDebug` (update install). The emulator is left on the live debug scanner (Right hand) with CAMERA granted. The connected suite was not rerun, because no connected test covers the debug HUD and no other source changed.
