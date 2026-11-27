---
phase: 03-client-shell
plan: 04
subsystem: android-client
status: complete
tags: [android, compose, navigation3, home, bottom-sheet, back-handler, hand-selection, debug-showcase]
requires:
  - phase: 03-01
    provides: routes Home/HandSelect/Scan/ComingSoon, NavBackStack.pushFrom, ProfileRepository, tools/ui.sh, tools/device-lock.sh
  - phase: 03-02
    provides: NocturneBackground at the root with a showsBackground flag per route, Phase 3 type scale, illustrations and duotone icons, tools/check-motion.sh
  - phase: 03-03
    provides: onboarding that lands on Home with the saved name
provides:
  - HomeScreen (greeting with name, title, HeroCard, "Explore your destiny" 7-tile grid, exit confirm, first-entry stagger)
  - HeroCard (breathing orb + orbit, orb hidden at font scale >= 1.5), NocturneButtonStyle.OnHero
  - ComingSoonSheet + BottomSheetSceneStrategy (Nav3 OverlayScene hosting an M3 ModalBottomSheet)
  - ExitDialog (Stay / Leave)
  - HandSelectScreen (Right by default, Left/Right cards, 1 good + 4 bad photo tips) and ScanPlaceholderScreen (opaque bg.camera, sky paused)
  - NocturneTopBar, FeatureCard art/Soon/arrow slots, NocturneChip Phase 3 padding, NocturneButton 0.97 press scale
  - NavBackStack.popFrom(from) guard
  - app.palm.debug debugEntries / debugHomeLongPress (showcase in src/debug only; release has empty stand-ins)
  - strings_home.xml (en, hi)
affects: [03-05, 04-palm-scan]
tech-stack:
  added: []
  patterns: [sheet routes rendered by a Nav3 OverlayScene strategy over the previous entry, sky animate flag taken from the top route's showsBackground, popFrom guard so double Back cannot pop two screens, debug-only features behind a source-set split with a release no-op of the same API]
key-files:
  created:
    - android/app/src/main/java/app/palm/feature/home/{HeroCard,ComingSoonSheet,ExitDialog}.kt
    - android/app/src/main/java/app/palm/feature/handselect/{HandSelectScreen,ScanPlaceholderScreen,HandSelectEntries}.kt
    - android/app/src/main/java/app/palm/core/navigation/BottomSheetSceneStrategy.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/TopBar.kt
    - android/app/src/main/res/values/strings_home.xml
    - android/app/src/main/res/values-hi/strings_home.xml
    - android/app/src/debug/java/app/palm/debug/DebugEntries.kt
    - android/app/src/release/java/app/palm/debug/DebugEntries.kt
    - android/app/src/androidTest/java/app/palm/feature/home/HomeTest.kt
    - android/app/src/androidTest/java/app/palm/feature/handselect/HandSelectTest.kt
  modified:
    - android/app/src/main/java/app/palm/feature/home/{HomeScreen,HomeEntries}.kt
    - android/app/src/main/java/app/palm/core/navigation/{AppNavDisplay,Routes}.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/{Buttons,FeatureCard,Chips}.kt
    - android/app/src/test/java/app/palm/core/navigation/PushFromTest.kt
    - android/app/src/{main -> debug}/java/app/palm/showcase/{ShowcaseScreen,BackendStatus}.kt (moved)
key-decisions:
  - "Coming-soon is a real ComingSoon(feature) route rendered by a Nav3 OverlayScene strategy that hosts an M3 ModalBottomSheet over Home"
  - "The sky's animate flag is the top route's showsBackground, so Scan (bg.camera) pauses it"
  - "Added NavBackStack.popFrom(from), the pop twin of pushFrom, so a double tap on Back or OK pops one screen only"
  - "The exit-confirm BackHandler stays inside HomeScreen; on the phone it wins over NavDisplay, so research A2's root-handler fallback was not needed"
  - "The showcase lives in src/debug; src/release ships empty debugEntries/debugHomeLongPress with the same signatures"
requirements-completed: [HOME-01, HOME-02, HOME-04, SCAN-01, FND-03]
coverage:
  - id: D1
    description: "Home shows greeting with name, title, hero and the 7 tiles in D-12 order"
    requirement: HOME-01
    verification:
      - kind: automated_ui
        ref: "HomeTest#showsHeaderHeroAndAllTiles (Redmi Note 10T, API 33)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Palm tile and hero open hand selection; every other tile opens the coming-soon sheet over Home"
    requirement: HOME-02
    verification:
      - kind: automated_ui
        ref: "HomeTest#palmTileOpensHandSelection, HomeTest#otherTileOpensComingSoonSheetOverHome"
        status: pass
    human_judgment: false
  - id: D3
    description: "Back on Home opens the Leave dialog (Stay keeps Home, Leave finishes); Back with the sheet open closes only the sheet"
    requirement: HOME-04
    verification:
      - kind: automated_ui
        ref: "HomeTest#backOnHomeAsksBeforeLeaving"
        status: pass
    human_judgment: false
  - id: D4
    description: "Hand selection defaults to Right, 1 good + 4 bad tips, Continue opens Scan carrying the hand"
    requirement: SCAN-01
    verification:
      - kind: automated_ui
        ref: "HandSelectTest#heroOpensHandSelectionAndScanCarriesTheHand"
        status: pass
    human_judgment: false
  - id: D5
    description: "Scan opts out of the sky (opaque bg.camera, animation paused); the sky still runs and honours reduce motion on Home"
    requirement: FND-03
    verification:
      - kind: automated_ui
        ref: "tools/check-motion.sh off and on on the debug build (Task 1 verify 3)"
        status: pass
    human_judgment: false
  - id: D6
    description: "Hero orb hidden at font scale >= 1.5, 30-char name and Hindi wrap, hero art placement, press scale and stagger feel"
    verification: []
    human_judgment: true
    rationale: "Visual and long-text checks are not automated here; the 03-05 hardening pass covers font scale and Hindi on the device"
metrics:
  duration: 82min
  completed: 2026-11-27
  tasks: 3
  files: 24
estimate:
  tokens: 95000
actuals:
  tokens: 20800
  tasks: 3
  commits: 3
plan_head_before: da2735661d411b85c36e00c9b5be62bac4b019f2
plan_head_after: ffec636
---

# Phase 3 Plan 04: Home hub, coming-soon sheet, exit dialog, hand selection, scan placeholder Summary

**Home is now the real hub: a greeting with the user's name, the palm-scan hero and a 7-tile grid. The hero and the Palm tile lead to hand selection (Right by default, with 1 good and 4 bad photo tips) and then to a Scan placeholder that carries the hand and pauses the sky. The other six tiles open a coming-soon bottom sheet over Home. Back on Home asks "Leave [AppName]?". The Phase 1 showcase is reachable only in debug builds, by long-pressing the greeting. All 5 instrumented tests pass on the Redmi Note 10T.**

## Performance

- Duration: about 82 min of wall clock, from 2026-11-27 16:31 IST (base da27356) to about 18:02 IST, over three executor runs. Most of that time went to the phone: it dropped off USB twice, and MIUI blocked the test's activity start until the user set MIUIOP 10021 to Allow.
- Tasks: 3 of 3
- Files: 24 (21 Android sources and tests, 2 string files, 1 unit test)

## Task Commits

| Task | Name | Commit | Notes |
|------|------|--------|-------|
| 1 | Tracer: Home hero -> hand selection with photo tips -> Scan(hand), sky paused | 1f0f423 (feat) | HandSelectTest 1/1 on the phone; check-motion off/on passed in the second run |
| 2 | 7-tile feature grid, coming-soon bottom sheet over Home | c9243a9 (feat) | HomeTest written; on-device run completed in the final run |
| 3 | Exit confirm on Back, press scale, Home entry stagger, debug-only showcase | ffec636 (feat) | Showcase moved to src/debug, release stand-ins |

## Verification

- Instrumented run on the Redmi Note 10T 5G (M2103K19I, API 33, wireless-debugging serial), started from the phone's home screen with MIUIOP(10021) at allow: `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.home.HomeTest,app.palm.feature.handselect.HandSelectTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true` gave BUILD SUCCESSFUL. The result XML shows HomeTest tests="4" failures="0" errors="0" and HandSelectTest tests="1" failures="0" errors="0". The tests="0" guard passed.
  - HomeTest: `showsHeaderHeroAndAllTiles`, `palmTileOpensHandSelection`, `otherTileOpensComingSoonSheetOverHome`, `backOnHomeAsksBeforeLeaving`.
  - HandSelectTest: `heroOpensHandSelectionAndScanCarriesTheHand`.
- Task 1 verify 3 passed in the second executor run: install-debug, `ui.sh reset`, launch, expect Language, then `check-motion.sh off` and `on`. The same run reported that every Task 2 and Task 3 adb and ui.sh verify passed, including the assemble debug, androidTest and release builds and the check that the release APK has no `Lapp/palm/showcase/` class.
- `PushFromTest#doubleBackPopsOnce` covers the popFrom guard.
- Tracer feedback gate (interactive, end-of-phase, automated verify): passed, and the plan expanded.
- Device state at the end: animator, transition and window scales are 1.0, font_scale is 1.0 and low_power is 0. Both packages are still installed.

## Decisions Made

See key-decisions in the frontmatter. In short: a real ComingSoon route rendered as an Overlay sheet, the sky's animate flag taken from the top route, the popFrom guard, the exit-confirm BackHandler kept local because it wins on the phone, and the showcase split into source sets.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Added NavBackStack.popFrom(from)**
- **Found during:** Task 1
- **Issue:** A double tap on the top bar Back (or on the sheet's OK) could pop two entries and leave Home, the same race that pushFrom already guards on the push side.
- **Fix:** Added `popFrom(from)` in Routes.kt, which pops only while `from` is on top. Used by the HandSelect, Scan and sheet exits. Unit test `PushFromTest#doubleBackPopsOnce` added.
- **Files modified:** android/app/src/main/java/app/palm/core/navigation/Routes.kt, android/app/src/test/java/app/palm/core/navigation/PushFromTest.kt (neither is in the plan's files_modified)
- **Commit:** 1f0f423

### Process notes

- The device was the Redmi Note 10T 5G (M2103K19I, API 33), not the Redmi named in the plan. It is API 33, so the API 33+ LocaleManager language path can be checked on hardware. The API 26-32 appcompat back-compat path was not checked on hardware.
- The phone dropped off USB twice, at the first screencap after `check-motion.sh off`. Later runs used wireless debugging. The orchestrator restored animator_duration_scale to 1.0 after the trap could not run.
- MIUI's MIUIOP(10021) ("start in background") had reverted to ignore and hung the first HomeTest run. The user set it to Allow on the phone. The executor never changed it.
- Every Gradle connected run passed `leaveApksInstalledAfterRun=true`, and neither package was ever uninstalled (on MIUI a fresh install needs a tap on the phone).
- Research A2 (moving the exit-confirm BackHandler to a root handler in AppNavDisplay) was not needed: `backOnHomeAsksBeforeLeaving` passes with the handler in HomeScreen.

## Known Stubs

- `ScanPlaceholderScreen` ("Scanner arrives in the next update" plus the chosen hand) is intentional, as the plan specifies. Phase 4 (palm scan) replaces it with the CameraX + MediaPipe scanner.
- The six non-palm tiles open the coming-soon sheet on purpose (D-12). Their features come in later milestones.

## Next Phase Readiness

03-05 (hardening: font scale, Hindi, cold start) can run on top of this. Phase 4 plugs the scanner into the `Scan(hand)` route that already exists.

## Self-Check: PASSED
