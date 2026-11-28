---
phase: 05-result-against-the-fixture
plan: 02
subsystem: ui
tags: [compose, overlay, catmull-rom, tabs, material3, emulator]

requires:
  - phase: 05-result-against-the-fixture
    provides: "05-01 tracer: PalmPhoto/fitRect/LineLegend/ResultActions, ReadingStore, ResultViewModel, seedReading, FakeReadingDao/FakePhotoDecoder"
provides:
  - "OverlayMath: Cubic, catmullRom, capPoints (64), aspectMismatch (pure)"
  - "LineRules: Badge, badge(), badgeRes, orderedLines"
  - "PalmPhoto: BgDeep halo (HALO_ALPHA 0.55, 9 dp) under 3 dp Catmull-Rom lines, draw-in once per revealKey, photo fade; trailing revealKey: String? = null"
  - "ResultScreen: full S10 layout, segmented PrimaryTabRow tabs (rememberSaveable), notes, debug aspect warning"
  - "ResultCards: InfoCard, LineCard"
affects: [05-03, 05-04, 05-05, 05-06]

actuals:
  tokens: 14100
  tasks: 2
  commits: 2
plan_head_before: 73559a87fc35aecf86a4a292977949bd225ccc4e
plan_head_after: f30975a4bc26b535614e6a74782ff2d875bf8a00

tech-stack:
  added: []
  patterns:
    - "Overlay reveal: one Animatable read only inside drawWithCache/onDrawBehind; PathMeasure.getSegment trims halo and line together"
    - "JVM StateFlow tests: collect with backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)), because advanceUntilIdle skips background work"
    - "Compose pixel tests: setContent once per test, swap the scene through mutableStateOf"

key-files:
  created:
    - android/app/src/main/java/app/palm/feature/result/ResultCards.kt
    - android/app/src/test/java/app/palm/feature/result/OverlayMathTest.kt
    - android/app/src/test/java/app/palm/feature/result/LineRulesTest.kt
    - android/app/src/test/java/app/palm/feature/result/ResultViewModelTest.kt
    - android/app/src/androidTest/java/app/palm/feature/result/PalmOverlayTest.kt
    - android/app/src/androidTest/java/app/palm/feature/result/ResultScreenTest.kt
  modified:
    - android/app/src/main/java/app/palm/feature/result/OverlayMath.kt
    - android/app/src/main/java/app/palm/feature/result/LineRules.kt
    - android/app/src/main/java/app/palm/feature/result/PalmPhoto.kt
    - android/app/src/main/java/app/palm/feature/result/ResultScreen.kt
    - android/app/src/main/java/app/palm/feature/result/ResultViewModel.kt

key-decisions:
  - "Deviation from locked D-10 (user check in 05-06): HALO_ALPHA in PalmPhoto.kt is 0.55 (BgDeep, 9 dp) where D-10 says about 30% alpha, chosen from the UI-SPEC light-skin contrast measurement; one constant, the user approves 0.55 or asks for 0.30 in the 05-06 human check"
  - "ResultViewModel adds distinctUntilChanged after store.byId: Room re-emits byId on any table write, which re-decoded the photo and flashed the photo spinner"
  - "The photo fade is a BgDeep rect fading out over the image inside drawWithContent (draw-only, no recomposition), visually the same as alpha 0 to 1 on the BgDeep box"
  - "The selected-tab pill animates its brush alpha (animateFloatAsState, snap under reduce motion) instead of animateColorAsState, because the pill is a gradient brush"

patterns-established:
  - "PalmPhoto(revealKey = readingId) on Result; null (static, complete) for the share card and pixel tests"

requirements-completed: [RES-01, RES-02, RES-03, RES-04]

coverage:
  - id: D1
    description: "Lines are Catmull-Rom curves through every trace point with a dark halo, on the right pixels on 3:4, 1:1, 9:16 and a letterboxed box; nothing drawn for none; description names the drawn lines"
    requirement: RES-02
    verification:
      - kind: unit
        ref: "app.palm.feature.result.OverlayMathTest (9), LineRulesTest (4)"
        status: pass
      - kind: integration
        ref: "connectedDebugAndroidTest PalmOverlayTest (4) on emulator-5554, tests=\"0\" guard"
        status: pass
    human_judgment: false
  - id: D2
    description: "Tabs Palm / Love / Career with D-08 content, selection survives recreate, disclaimer reachable on every tab, line cards with Approximate / Not clearly visible, notes, missing-photo degradation, Scan again / Ask [Guide] / Back"
    requirement: RES-01
    verification:
      - kind: integration
        ref: "connectedDebugAndroidTest ResultScreenTest (5) on emulator-5554, tests=\"0\" guard"
        status: pass
      - kind: e2e
        ref: "install-debug + result-open.sh + ui.sh expect 'Your palm reading' 'Lines are drawn approximately' 'For entertainment only' (emulator-5554), plus screenshots of the photo/legend/tabs and the line cards"
        status: pass
    human_judgment: false
  - id: D3
    description: "Loading / Found(photo Loading) / Ready, Unavailable for undecodable or deleted photos, Missing for no row, bad JSON, extra key, rule violation and not-ok verdict; one decode per row"
    requirement: RES-03
    verification:
      - kind: unit
        ref: "app.palm.feature.result.ResultViewModelTest (6)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Halo alpha 0.55 vs D-10's about 30%, and the draw-in feel (930 ms for 5 lines, once per entry)"
    requirement: RES-02
    verification:
      - kind: other
        ref: "05-06 human check on the device"
        status: pending
    human_judgment: true
    rationale: "Locked-decision deviation and motion feel need the user's eye"

duration: 11min
completed: 2026-11-28
status: complete
---

# Phase 5 Plan 02: Full S10 Result Summary

**Result now draws the detected lines as smooth Catmull-Rom curves over a dark halo that draw in once per entry, followed by a legend with the approximate or no-lines note, the disclaimer, and segmented Palm / Love / Career tabs. The tab selection survives recreation. Each line has its own card with an "Approximate" or "Not clearly visible" badge. Missing photos and unreadable readings degrade cleanly. Verified on emulator-5554.**

## Performance

- **Duration:** 11 min (07:22 to 07:33 UTC)
- **Started:** 2026-11-28T07:22:43Z
- **Completed:** 2026-11-28T07:33:33Z
- **Tasks:** 2 of 2
- **Files:** 11 changed (+963 / -30)

## Accomplishments

- Task 1 (overlay): `catmullRom` (n-1 cubics ending on each next point; 2 points give a straight segment), `capPoints` (64, even sampling, first and last kept), `aspectMismatch` (symmetric, 2%). `Badge` / `badge()` / `badgeRes` / `orderedLines`. PalmPhoto draws a halo and then the line for each drawn line in contract order, with round caps and joins. Paths are built in `drawWithCache` from the decoded bitmap's fit rect. Once the photo fade ends, the reveal runs one Animatable over 4 x 120 + 450 ms with an EmphasizedDecelerate trim per line. A `rememberSaveable(revealKey)` "played" flag stops replays, and the lines are drawn complete under reduce motion or with a null key.
- Task 2 (screen): the UI-SPEC structure in order. The legend and notes show only once the photo is drawn (D-12), and the disclaimer always shows. `PrimaryTabRow` has a transparent container, an empty indicator and divider, a pill background with a hairline border, 48 dp tabs and a gradient pill inside the selected tab. Palm / Love / Career content follows D-08, with chip rows and the Ideal industries card hidden when empty and "Your lines" hidden with no lines. `LineCard` merges into one TalkBack node. A debug-only `Log.w` reports the two aspect numbers.

## Task Commits

1. **Task 1: Smoothed haloed lines that draw in once** - `1a3e9e6` (feat)
2. **Task 2: Tabs, cards, badges, notes and states** - `f30975a` (feat)

## Verification run

- `:app:testDebugUnitTest --tests OverlayMathTest --tests LineRulesTest` exit 0 (13 tests); `--tests ResultViewModelTest` exit 0 (6 tests)
- `:app:assembleDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest` exit 0
- `connectedDebugAndroidTest` PalmOverlayTest (4) and then ResultScreenTest (5) + PalmOverlayTest (4) on emulator-5554: all pass, the tests="0" guard holds
- `install-debug.sh && result-open.sh && ui.sh expect "Your palm reading" "Lines are drawn approximately" "For entertainment only"` exit 0
- Screenshots checked: four haloed curves on the lab palm, legend Heart/Head/Life/Sun, note, disclaimer, Palm pill selected. The cards show Approximate for Heart/Head/Life/Sun and Not clearly visible for Fate.
- `libs.versions.toml` unchanged; no new artifact (D-25)

## TDD Gate Compliance

- Task 1: RED came from the JVM compile failure (unresolved `catmullRom`, `Badge`, `orderedLines`, `badgeRes`), then GREEN (13/13). PalmOverlayTest was written before PalmPhoto changed, but it was first run after the change (pass), so its RED was not observed.
- Task 2: ResultViewModelTest RED on `theSameIdAndUnrelatedWritesDecodeOnce` (decoded 2, expected 1), then GREEN after `distinctUntilChanged`. ResultScreenTest was written before its first run, but that run came after the screen was implemented (pass), so its RED was not observed.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Unrelated Room writes re-decoded the Result photo**
- **Found during:** Task 2 (ResultViewModelTest)
- **Issue:** `store.byId(id)` re-emits on any write to the readings table, so `transformLatest` emitted Found(photo Loading) again and decoded again. The photo spinner flashed and the overlay reveal could restart.
- **Fix:** `.distinctUntilChanged()` after `flatMapLatest { store.byId(it) }`.
- **Files modified:** ResultViewModel.kt
- **Commit:** f30975a

### Locked-decision deviation (planned, for the user)

- HALO_ALPHA is 0.55 (BgDeep, 9 dp), while D-10 says about 30% alpha. The value comes from the UI-SPEC light-skin contrast measurement. It is a single constant, and the user approves 0.55 or asks for 0.30 in the 05-06 human check.

## Known Stubs

- `ResultActions` still ignores `reading` and `photo` (05-01's intentional stub; 05-04 adds Share). Not touched here.

## Deferred to the phone session

- Nothing in this plan needs a phone. The light-skin heart-line contrast backstop (a real light-skinned palm photo) and the reveal feel remain part of 05-06's device walk.

## Threat Flags

None. T-05-08 (64-point cap, clamping in `map`, rule-breaking rows render as Missing) and T-05-09 (debug-only log with two numbers) are mitigated as planned.

## Self-Check: PASSED

- Files exist: ResultCards.kt, OverlayMathTest.kt, LineRulesTest.kt, ResultViewModelTest.kt, PalmOverlayTest.kt, ResultScreenTest.kt
- Commits on HEAD: 1a3e9e6, f30975a
