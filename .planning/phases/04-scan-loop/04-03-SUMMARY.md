---
phase: 04-scan-loop
plan: 03
subsystem: android-client
tags: [permissions, camera, fallback, compose, i18n]
status: complete
requires:
  - "04-01 frozen interfaces: CameraPermissionGate signature, CameraProblem, NocturneStateBlock, NocturneTopBar, scan_* strings"
provides:
  - "feature/scan/CameraPermission.kt: CamPerm, cameraPerm, GateScreen, gateScreen (pure)"
  - "feature/scan/CameraPermissionGate.kt: request on entry, rationale after the answer, ON_RESUME re-check, Settings deep link, camera problem + retry"
  - "feature/scan/ScanFallbackScreens.kt: ScanFallbackScreen for Requesting, Denied, PermanentlyDenied, NoCamera, CameraError"
  - "strings_scan_permission.xml (en + hi): scan_perm_blocked_title, scan_perm_blocked_body, scan_perm_open_settings, scan_camera_error_body, scan_camera_retry"
affects: [04-04, 04-05]
tech-stack:
  added: []
  patterns:
    - "Permission state from the real request answer + shouldShowRequestPermissionRationale only (no stored 'has asked' flag)"
key-files:
  created:
    - android/app/src/main/java/app/palm/feature/scan/CameraPermission.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanFallbackScreens.kt
    - android/app/src/main/res/values/strings_scan_permission.xml
    - android/app/src/main/res/values-hi/strings_scan_permission.xml
    - android/app/src/test/java/app/palm/feature/scan/CameraPermissionTest.kt
    - android/app/src/androidTest/java/app/palm/feature/scan/ScanFallbackTest.kt
  modified:
    - android/app/src/main/java/app/palm/feature/scan/CameraPermissionGate.kt
decisions:
  - "Gate keeps a saveable 'asked' flag so a recreated activity does not launch a second permission request while the first is in flight"
  - "Requesting renders the Scan top bar (title + Back) on bg.camera, per the plan, instead of the 04-01 lone back button"
metrics:
  duration: "about 5 min (02:38 to 02:43 UTC)"
  completed: 2026-11-28
actuals:
  tokens: 4350
  tasks: 2
  commits: 2
plan_head_before: fe8c293aabadf3e7eb85a884e2db4be41b253cf1
plan_head_after: 8dd2c630e50807cac1e038ef5925d1a99858caea
---

# Phase 4 Plan 03: Camera Permission Gate and Fallbacks Summary

**CAMERA is requested on entering Scan; the answer plus the rationale flag picks "Allow the camera to scan" (ask again) or "Camera access is off" (Open settings to this app's details page); a resume re-check switches to the scanner after Settings; no back camera and camera-open failures land on "Camera not available" with gallery (and a retry that rebinds). Every state offers "Choose from gallery" and Back.**

## What was built

- **Task 1** (`54ef4c1`): pure `cameraPerm` / `gateScreen` reducers (10 JVM tests), the full `CameraPermissionGate` (frozen signature kept), stateless `ScanFallbackScreen`, five new strings in English and Hindi (UI-SPEC text verbatim; Hindi is Claude-written, covered by the existing native-review concern).
- **Task 2** (`8dd2c63`): `ScanFallbackTest` (6 Compose tests: one per state, Requesting has only Back, Back + heading semantics in every state).

## Approvals recorded

- 04-01 Task 1 download consent: approved by the user in chat on 2026-11-28 (Approve all; telemetry disclosure accepted). The additional resolved artifacts (featurecombinationquery 1.7.0, media3 1.9.0, guava 33.3.1-android and the listed version shifts) were approved the same day after the 04-01 checkpoint. This plan added no dependency and downloaded nothing (T-04-SC).
- 04-01 Task 4 (real palm on the phone) stays deferred to the 04-05 phone session by the user's choice (listed in 04-01-SUMMARY items 1-7).

## Verification (emulator-5554, API 35)

| Check | Result |
|---|---|
| `:app:testDebugUnitTest --tests app.palm.feature.scan.CameraPermissionTest` | 10 tests, 0 failures |
| `:app:assembleDebug` | exit 0 |
| Task 1 greps (6+ tests, pure file, gate has ACTION_APPLICATION_DETAILS_SETTINGS / ON_RESUME / shouldShowRequestPermissionRationale, aapt2 manifest CAMERA and no storage/media permission, en = hi keys, 5 strings, verticalScroll and no maxLines) | all pass |
| `:app:connectedDebugAndroidTest` ScanFallbackTest (leaveApksInstalled, tests="0" guard) | 6 tests, 0 failures, guard clean |
| Device walk: install -> `scan-open.sh --revoke` -> system dialog -> Don't allow -> "Allow camera" + "Choose from gallery" -> Allow camera -> Don't allow -> "Open settings" + "Choose from gallery" -> Open settings (screen left the app) -> `pm grant` -> Back -> live scanner | exit 0 (regex `Don.{1,3}t allow`, see deviations) |
| Regression: HandSelectTest on the emulator (gate on the granted path) | 1 test, 0 failures |

CAMERA for app.palm.dev.debug is left **granted** on the emulator after the walk (`granted=true`; the USER_SET/USER_FIXED flags from the two refusals remain, as for a real user; `scan-open.sh --revoke` clears them). The emulator is on MainActivity with the live scanner.

## Deferred to the 04-05 phone session

Not faked, not marked passed. Run with `ADB_SERIAL=<phone serial>` exported; on MIUI confirm MIUIOP 10021 is Allow and the phone is unlocked on the home screen first.

1. **ScanFallbackTest on the phone:** `rm -rf android/app/build/outputs/androidTest-results/connected && bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.scan.ScanFallbackTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected`. Expected: exit 0, 6 tests passed.
2. **Permission walk on the phone (MIUI dialog wording and Settings page may differ):** `bash tools/device-lock.sh bash -c 'bash tools/install-debug.sh && bash tools/scan-open.sh --revoke && bash tools/ui.sh tap "Don.{1,3}t allow|^Deny" && bash tools/ui.sh expect "Allow camera" "Choose from gallery" && bash tools/ui.sh tap "Allow camera" && bash tools/ui.sh tap "Don.{1,3}t allow|^Deny" && bash tools/ui.sh expect "Open settings" "Choose from gallery" && bash tools/ui.sh tap "Open settings" && bash tools/ui.sh absent "Open settings" && source tools/_device.sh && adb -s "$ADB_SERIAL" shell pm grant app.palm.dev.debug android.permission.CAMERA && bash tools/ui.sh back && bash tools/ui.sh expect "Place your palm inside the frame|hand guide couldn.t start"'`. Expected: exit 0; CAMERA left granted. If MIUI's dialog uses other button text (e.g. "Deny"/"Reject"), widen the tap regex only.
3. **Real Settings toggle (human, phone):** on "Camera access is off" tap Open settings, turn Camera on in the app's permission page by hand, press Back. Expected: the live scanner appears without a tap.
4. **Camera in use (human, phone, optional):** open the scanner, switch to another camera app that holds the camera, return. Expected: "Camera not available" with "Another app may be using the camera..." body; close the other app, tap "Try camera again": the live scanner rebinds. (Not forced on the emulator: it would need another camera client or a device-policy change.)
5. Carried from 04-01 / 04-02: their "Deferred to the 04-05 phone session" lists, unchanged.

## Deviations from Plan

**1. [Rule 1 - Bug] Plan's "Don.t allow" regex cannot match the system dialog**
- **Found during:** Task 2 device walk (already known from 04-01 deviation 2)
- **Issue:** Android uses a curly apostrophe (3 bytes); `.` matches one byte in the byte locale.
- **Fix:** ran the walk with `Don.{1,3}t allow|^Deny`. No code change.
- **Commit:** none (verify command only)

**2. [Rule 2 - Correctness] Single in-flight permission request**
- **Found during:** Task 1
- **Issue:** `LaunchedEffect(Unit)` re-runs after activity recreation; with only `granted`/`requested` saved, a recreation while the dialog was up would launch a second request.
- **Fix:** saveable `asked` flag set when the entry request is launched.
- **Files modified:** CameraPermissionGate.kt
- **Commit:** 54ef4c1

**3. [Layout] Centring via a weighted Box around the scroll column** instead of literal flexible spacers inside the scroll (weights do not work inside `verticalScroll`); same result: centred when it fits, scrolls at large font scale. The outer `safeDrawing` padding supplies the navigation inset under the 24 dp bottom padding.

**4. [Scope] Device checks ran on the emulator, not the phone** (user's choice "emulator now, phone later"); phone checks listed above.

## Known Stubs

None in this plan's files.

## Threat Flags

None beyond the plan's threat model. T-04-11 (aapt2 manifest check), T-04-12 (Uri.fromParts with this package only), T-04-13 (gallery on every fallback, retry, Back; ScanFallbackTest), T-04-14 (CAMERA granted back at the end of the walk) and T-04-SC (no new artifact) hold.

## Self-Check: PASSED

- All created files exist; commits 54ef4c1 and 8dd2c63 are ancestors of HEAD.
