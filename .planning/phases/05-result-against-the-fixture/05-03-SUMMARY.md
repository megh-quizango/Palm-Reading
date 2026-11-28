---
phase: 05-result-against-the-fixture
plan: 03
subsystem: ui
tags: [compose, navigation3, invalid-photo, emulator]

requires:
  - phase: 05-result-against-the-fixture
    provides: "05-01: ReadState / ScanReadyViewModel read flow, ReadingStore, ReadingRules, strings_result invalid_* keys, popFrom/pushFrom/replaceAll, FakeReadingDao/FakePhotoDecoder/FakeAppLocale"
provides:
  - "InvalidPhoto(reason, hand) route, showInvalid (ScanReady -> InvalidPhoto in one stack edit), popTo"
  - "InvalidPhotoScreen, ReasonCopy, reasonCopy (exhaustive, UI-SPEC section 3 icons)"
  - "ReadState.Invalid + consumeInvalid; ScanReadyScreen onInvalid"
  - "ic_star, ic_eye drawables"
  - "FakePalmReadingRepository (CompletableDeferred, records calls and locales)"
affects: [05-05, 05-06, phase-6-backend]

actuals:
  tokens: 8000
  tasks: 2
  commits: 2
plan_head_before: 0a3d8a96a1dc2f7267f4f341420c3f070c372dab
plan_head_after: 30cc1883e6927b80c210199c4bf036a386ed434e

tech-stack:
  added: []
  patterns:
    - "Verdict handling order: ReadingRules first (any violation = Failed), then ok -> save, not ok -> delete capture + Invalid"

key-files:
  created:
    - android/app/src/main/java/app/palm/feature/invalidphoto/InvalidPhotoScreen.kt
    - android/app/src/main/res/drawable/ic_star.xml
    - android/app/src/main/res/drawable/ic_eye.xml
    - android/app/src/test/java/app/palm/feature/invalidphoto/ReasonCopyTest.kt
    - android/app/src/test/java/app/palm/feature/scan/ScanReadyViewModelTest.kt
    - android/app/src/test/java/app/palm/testing/FakePalmReadingRepository.kt
    - android/app/src/androidTest/java/app/palm/feature/invalidphoto/InvalidPhotoTest.kt
  modified:
    - android/app/src/main/java/app/palm/core/navigation/Routes.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanEntries.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyViewModel.kt
    - android/app/src/main/java/app/palm/feature/scan/ScanReadyScreen.kt
    - android/app/src/test/java/app/palm/core/navigation/PushFromTest.kt

key-decisions:
  - "Phase 6 constraint (D-15): the free-scan counter (PRD BL-1) increments only inside ReadingStore.save, never on read() and never on an invalid verdict"
  - "The capture is deleted at the invalid verdict (holder.clear() under NonCancellable), before InvalidPhoto opens, so no exit path from InvalidPhoto needs cleanup (RESEARCH Pitfall 11)"
  - "read() returns early once holder.latest is null, so a stray tap after an invalid verdict or a save cannot read a deleted capture"
  - "ScanReady treats Invalid as busy (buttons disabled) for the frame between the verdict and the navigation"

patterns-established:
  - "Stack edits that replace the top screen live as pure guarded NavBackStack functions in Routes.kt (showInvalid), unit-tested in PushFromTest; entries only call them"

requirements-completed: [READ-03]

coverage:
  - id: D1
    description: "Each of the 7 reasons maps to its UI-SPEC icon, distinct title and tip; screen shows 'Photo not accepted', title, tip, Retake, Back to Home, no disclaimer; Retake and top-bar back call onRetake, Back to Home calls onHome"
    requirement: READ-03
    verification:
      - kind: unit
        ref: "app.palm.feature.invalidphoto.ReasonCopyTest (2)"
        status: pass
      - kind: integration
        ref: "connectedDebugAndroidTest InvalidPhotoTest (3) on emulator-5554, tests=\"0\" guard"
        status: pass
    human_judgment: false
  - id: D2
    description: "showInvalid puts InvalidPhoto where ScanReady was (Back/Retake land on Scan(hand)), ignores other tops and late callbacks; popTo(Home) pops to Home, no-op when absent or on top"
    requirement: READ-03
    verification:
      - kind: unit
        ref: "app.palm.core.navigation.PushFromTest (9)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Invalid verdict: no row, no readings file, capture and holder.latest gone, earlier reading still latest; valid saves once; double tap reads once; Retake no-op while reading; failure / rule violation / failed save -> Failed, capture kept, retry reads again; locale HI and null->EN"
    requirement: READ-03
    verification:
      - kind: unit
        ref: "app.palm.feature.scan.ScanReadyViewModelTest (10)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Lab walk through an invalid fixture variant on the device (variants arrive with 05-05)"
    requirement: READ-03
    verification:
      - kind: e2e
        ref: "05-06 lab walk"
        status: pending
    human_judgment: false

duration: 5min
completed: 2026-11-28
status: complete
---

# Phase 5 Plan 03: Unusable-photo screen and ScanReady invalid branch Summary

**An unusable-photo verdict now deletes the capture, saves nothing, and replaces ScanReady with an InvalidPhoto screen showing the reason's icon (error tone), title and retake tip under "Photo not accepted", with Retake photo (back to Scan for the same hand) and Back to Home. All 7 reasons and every ScanReady read outcome are tested on the JVM, and the screen is tested on emulator-5554.**

## Performance

- **Duration:** about 5 min (07:37 to 07:42 UTC)
- **Started:** 2026-11-28T07:37:28Z
- **Completed:** 2026-11-28T07:42:00Z
- **Tasks:** 2 of 2
- **Files:** 12 changed (+509 / -9)

## Accomplishments

- Task 1: `InvalidPhoto(reason, hand)` route (sky on), `showInvalid` (adds InvalidPhoto, then removes the ScanReady below it, only when ScanReady is on top) and `popTo`. `reasonCopy` is one exhaustive `when` with no `else`. `InvalidPhotoScreen` follows the ScanFallback layout: top bar back = Retake, centred `NocturneStateBlock` in `StatusError`, and no disclaimer and no BackHandler (NavDisplay's pop lands on Scan(hand)). The `entry<InvalidPhoto>` is registered in `scanEntries`, so AppNavDisplay is unchanged. The ic_star and ic_eye vectors use the ic_alert format.
- Task 2: `ReadState.Invalid` and `consumeInvalid()`. `read()` now checks ReadingRules first (any violation = Failed). An ok verdict saves as before. A not-ok verdict runs `holder.clear()` under `io + NonCancellable` and becomes `Invalid(reason, capture.hand)`, and `store.save` is never called. ScanReadyScreen's `onInvalid` LaunchedEffect calls `showInvalid` and then consumes the state.

## Task Commits

1. **Task 1: Unusable-photo screen, routes, icons** - `ab82439` (feat)
2. **Task 2: ScanReady invalid branch** - `30cc188` (feat)

## Verification run

- `:app:testDebugUnitTest --tests ReasonCopyTest --tests PushFromTest` exit 0 (2 + 9 tests)
- `connectedDebugAndroidTest` InvalidPhotoTest on emulator-5554: 3/3 pass, the tests="0" guard holds (APKs left installed, no uninstall)
- `:app:testDebugUnitTest --tests ScanReadyViewModelTest` exit 0 (10 tests)
- `:app:assembleDebug :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileReleaseKotlin` exit 0, 30 suites with 0 failures
- `libs.versions.toml` unchanged; no download (D-25)

## TDD Gate Compliance

- Task 1: RED from the JVM compile failure (unresolved `InvalidPhoto`, `popTo`, `showInvalid`, `ReasonCopy`), then GREEN. InvalidPhotoTest was written before the screen, but its first run came after the screen existed (pass), so its RED was not observed.
- Task 2: RED from the compile failure (unresolved `ReadState.Invalid`, `consumeInvalid`), then GREEN 10/10.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Correctness] read() ignored once the capture is gone**
- **Found during:** Task 2
- **Issue:** After `consumeInvalid()` the state is Idle while the deleted capture is still in the ViewModel's `capture` val. A tap during the exit transition would have sent a deleted photo to the repository.
- **Fix:** `read()` returns when `holder.latest == null`, and ScanReady counts `Invalid` as busy. The invalid test asserts that a second read() makes no call.
- **Files modified:** ScanReadyViewModel.kt, ScanReadyScreen.kt
- **Commit:** 30cc188

**2. [Rule 1 - Test robustness] Disclaimer check reads the resource**
- InvalidPhotoTest checks that `result_disclaimer` is absent by reading the resource instead of a hard-coded English string, so it holds under any device locale.

## Known Stubs

None.

## Deferred to the phone session

- None. Nothing in this plan needs a phone. The on-device walk through the 7 invalid fixture variants is 05-06's lab walk, after 05-05 adds the variants.

## Threat Flags

None. T-05-10 (the capture is deleted for all 7 reasons, asserted) and T-05-11 (no save on the invalid branch, the counter constraint recorded) are mitigated as planned.

## Self-Check: PASSED

- Files exist: InvalidPhotoScreen.kt, ic_star.xml, ic_eye.xml, ReasonCopyTest.kt, ScanReadyViewModelTest.kt, FakePalmReadingRepository.kt, InvalidPhotoTest.kt
- Commits on HEAD: ab82439, 30cc188
