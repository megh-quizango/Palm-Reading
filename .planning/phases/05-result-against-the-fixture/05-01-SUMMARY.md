---
phase: 05-result-against-the-fixture
plan: 01
subsystem: ui
tags: [room3, compose, navigation3, hilt, fixture, overlay, emulator]

requires:
  - phase: 04-scan-loop
    provides: ScanResultHolder / ProcessedCapture handed from Scan to ScanReady, ScanReady screen
provides:
  - Reading seam (PalmReadingRepository, fixture implementation with D-02 stamping), ContractJson, ReadingRules
  - ReadingStore (photo in filesDir/readings/<uuid>.jpg + Room v2 row), PhotoDecoder, DB v2 with destructive fallback from v1
  - Result route and screen (PalmPhoto overlay via fitRect, LineLegend, disclaimer, ResultActions)
  - Home "Your last reading" card (NocturneCard, LastReadingCard)
  - Debug Fixture lab, tools/result-open.sh, ui.sh hold / scroll-to
  - Test helpers for wave 2 (FakeReadingDao, FakePhotoDecoder + stubImageBitmap, androidTest seedReading)
affects: [05-02, 05-03, 05-04, 05-05, 05-06, phase-6-backend]

actuals:
  tokens: 24400
  tasks: 3
  commits: 3
plan_head_before: d9df898a7ec9289b1c1e242710e1601e46956612
plan_head_after: 314e18af55ee79f476d85d940b44ca29524a7320

tech-stack:
  added: []
  patterns:
    - "Reading seam: one @Binds in ReadingModule is the Phase 6 swap point"
    - "Store guards: lower-case UUID ids only, photos resolved only inside readings/ (canonical path), failed insert deletes the moved photo"
    - "Device scripts: ui.sh tap/hold regexes match the label only (ui.py appends '  (x,y)'; tap adds the coordinate match itself)"

key-files:
  created:
    - android/app/src/main/java/app/palm/core/reading/{ReadingRules,PalmReadingRepository,ReadingStore,ReadingModule}.kt
    - android/app/src/main/java/app/palm/core/photo/PhotoDecoder.kt
    - android/app/src/main/java/app/palm/feature/result/{OverlayMath,LineRules,PalmPhoto,LineLegend,ResultActions,ResultViewModel,ResultScreen,ResultEntries}.kt
    - android/app/src/main/java/app/palm/feature/home/LastReadingCard.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/Card.kt
    - android/app/src/debug/java/app/palm/debug/FixtureLab.kt
    - android/app/schemas/app.palm.core.db.AppDatabase/2.json
    - android/app/src/main/res/values{,-hi}/strings_result.xml
    - tools/result-open.sh
  modified:
    - android/app/src/main/java/app/palm/core/db/AppDatabase.kt
    - android/app/src/main/java/app/palm/core/navigation/{Routes,AppNavDisplay}.kt
    - android/app/src/main/java/app/palm/feature/scan/{ScanReadyViewModel,ScanReadyScreen,ScanEntries}.kt
    - android/app/src/main/java/app/palm/feature/home/{HomeViewModel,HomeScreen,HomeEntries}.kt
    - android/app/src/{debug,release}/java/app/palm/debug/DebugEntries.kt
    - tools/ui.sh

key-decisions:
  - "result-open.sh regexes drop the plan's trailing '  \\(' because ui.sh tap/hold already append the '(x,y)' match; with it the pattern needed two parentheses and never matched"
  - "ReadingStore.deleteAll deletes rows through the DAO directly (not delete(id)) so a stored non-UUID id cannot stop the sweep"
  - "FakePhotoDecoder's stub ImageBitmap is a java.lang.reflect.Proxy (width/height only) so it keeps compiling whatever members Compose adds to ImageBitmap"
  - "The saved row's handSide comes from the stamped reading (equal to capture.hand by stamp)"

patterns-established:
  - "Instrumented tests on the shared debug install seed through ReadingEntryPoint + seedReading and delete only their own ids"

requirements-completed: [RES-02, RES-04, RES-05, RES-07, HOME-03]

coverage:
  - id: D1
    description: "Lab photo -> Read my palm -> saved reading (Room row + filesDir/readings/<uuid>.jpg, cache/scan empty) -> Result with lines over the photo, legend and disclaimer -> Back lands on Home"
    requirement: RES-02
    verification:
      - kind: e2e
        ref: "ADB_SERIAL=emulator-5554 device-lock: install-debug + result-open.sh + ui.sh expect 'Your palm reading' '^Your palm photo with lines drawn' 'For entertainment only' + run-as ls checks + back -> 'Read your palm'"
        status: pass
    human_judgment: false
  - id: D2
    description: "Store safety: UUID ids, contained photo paths, insert-failure cleanup, delete/deleteAll, v1 database opens as v2"
    requirement: RES-05
    verification:
      - kind: unit
        ref: "app.palm.core.reading.ReadingStoreTest (9), ReadingRulesTest (3), PushFromTest (5)"
        status: pass
      - kind: integration
        ref: "connectedDebugAndroidTest AppDatabaseTest (2) + ReadingStoreRoomTest (2) on emulator-5554, tests=\"0\" guard"
        status: pass
    human_judgment: false
  - id: D3
    description: "Home 'Your last reading' card survives force-stop and reopens the reading with its photo"
    requirement: HOME-03
    verification:
      - kind: integration
        ref: "connectedDebugAndroidTest HomeLastReadingTest (2) + HomeTest on emulator-5554"
        status: pass
      - kind: e2e
        ref: "result-open.sh, am force-stop, ui.sh launch, tap 'Your last reading', expect photo cd + disclaimer, absent 'Photo unavailable'"
        status: pass
    human_judgment: false
  - id: D4
    description: "Disclaimer on Result and Scan again / Ask [Guide] actions"
    requirement: RES-04
    verification:
      - kind: e2e
        ref: "ui.sh expect 'For entertainment only' on Result (emulator-5554)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Hindi copy for every new string (Claude-written)"
    verification:
      - kind: other
        ref: "key-set diff en vs hi for strings_result.xml (56 keys), strings_scan.xml, strings_home.xml"
        status: pass
    human_judgment: true
    rationale: "Native-speaker review of Claude-written Hindi, as in Phases 3-4"

duration: 17min
completed: 2026-11-28
status: complete
---

# Phase 5 Plan 01: Result tracer, store and Home reopen Summary

**A debug lab photo becomes a stamped fixture reading on "Read my palm", saved as a Room v2 row plus filesDir/readings/<uuid>.jpg, opens on a Result screen with the Heart/Head/Life/Sun lines drawn over the photo, a legend and the disclaimer, and reopens from Home's "Your last reading" card after force-stop. Verified on emulator-5554.**

## Performance

- **Duration:** 17 min (07:00 to 07:17 UTC)
- **Started:** 2026-11-28T07:00:08Z
- **Completed:** 2026-11-28T07:17:12Z
- **Tasks:** 3 of 3
- **Files:** 47 changed (+2049 / -58), the accepted budget overrun

## Accomplishments

- Tracer (Task 1): reading seam with D-02 stamping, strict `ContractJson`, `ReadingRules`, `ReadingStore`, `PhotoDecoder`, DB v2 (`buildDatabase`, `fallbackToDestructiveMigrationFrom(true, 1)`, schema 2.json), `Result(readingId)` route, vararg `replaceAll`, ScanReady read flow (Reading / Failed / Saved states, Back and Retake ignored while reading, save + `holder.clear()` under `NonCancellable`), Result screen (PalmPhoto with the polyline overlay mapped through `fitRect`, LineLegend, disclaimer, summary and line texts, Scan again / Ask [Guide]), all 56 `strings_result` keys plus the 3 ScanReady keys in en and hi, debug Fixture lab from the Showcase, `ui.sh hold` / `scroll-to`, `tools/result-open.sh`. Every frozen wave-2 signature exists (frozen-symbol grep passes).
- Store hardening (Task 2, test-first): red on 3 guard tests, then green: lower-case UUID ids checked before any file work, atomic move with copy fallback, a failed insert deletes the moved photo, photos resolve only inside readings/, `delete` validates the id, `deleteAll` sweeps orphans. v1 -> v2 open test with the v1 identity hash. Shared fakes and `seedReading`.
- Home reopen (Task 3, test-first): red 2 of 2, then green: `NocturneCard`, `HomeViewModel.lastReading` (live from `latest()`, 168 px thumbnail), `LastReadingCard` (thumbnail or hand placeholder, "Right hand · Nov 28, 2026", one merged TalkBack node), Home pushes `Result(id)`.

## Task Commits

1. **Task 1: Tracer** - `2c0dde8` (feat)
2. **Task 2: Store hardening** - `ba0ba3a` (feat, tests + guards in one commit after the red run)
3. **Task 3: Home last-reading card** - `314e18a` (feat)

## Verification run

- `tools/gradlew.sh :app:assembleDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest` exit 0 (after Task 1 and again after Task 3)
- Task 1 emulator chain exit 0; Task 2 connected (AppDatabaseTest, ReadingStoreRoomTest) and Task 3 connected (HomeLastReadingTest, HomeTest) all pass with the tests="0" guard; Task 3 force-stop + reopen chain exit 0
- `libs.versions.toml` unchanged (no new artifact, D-25); schemas: 2.json added, 1.json unchanged
- Screenshots checked: Result shows the four drawn lines over the lab palm with the legend and disclaimer; Home shows the card under the hero

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] result-open.sh regexes could never match**
- **Found during:** Task 1 emulator verify
- **Issue:** The plan's `hold '^\[AppName\]  \('` and read-button `... \(` patterns end with a literal "(", and `ui.sh` tap/hold append `.*\([0-9]+,[0-9]+\)`, so the combined pattern needed two parentheses on a one-parenthesis line ("ui FAILED: hold").
- **Fix:** The patterns end at the two spaces before the coordinates; a script comment explains why.
- **Files modified:** tools/result-open.sh
- **Commit:** 2c0dde8

**2. [Rule 1 - Bug] deleteAll could stop on a bad stored id**
- **Found during:** Task 2
- **Issue:** Once `delete(id)` validates the id, `deleteAll` calling it would throw on any legacy or non-UUID row and leave the rest behind.
- **Fix:** `deleteAll` deletes rows through the DAO directly, then every file in readings/.
- **Files modified:** ReadingStore.kt
- **Commit:** ba0ba3a

**3. [Rule 3 - Blocking] `ui.sh tap|hold` shared case label**
- **Found during:** Task 1 acceptance
- **Issue:** The acceptance grep requires `hold)` on its own case line.
- **Fix:** A `find_xy` helper with separate `tap)` and `hold)` cases.
- **Commit:** 2c0dde8

### Environment note

During the first Task 1 run another app (`com.meghquizango.pagewise`) came to the front on emulator-5554 and an exit dialog appeared, apparently from another session using the same emulator outside `device-lock`. The retry passed. If emulator checks fail at random later, check what else is driving emulator-5554.

## Known Stubs

- `ResultActions` takes `reading` and `photo` but does not use them yet (`@Suppress("UNUSED_PARAMETER")`); 05-04 adds Share between the two buttons. This is intentional per the frozen interface.
- The Result body under the disclaimer is a plain summary plus line texts; 05-02 replaces it with tabs and cards (planned). The overlay is a solid polyline with no halo and no Catmull-Rom yet (05-02 owns PalmPhoto refinement).
- A not-ok verdict shows the ScanReady error instead of InvalidPhoto; 05-03 adds the route (planned).

## Deferred to the phone session

- None specific to this plan: everything ran on emulator-5554. The light-skin heart-line contrast backstop (UI-SPEC Color) is a real-photo check that belongs to 05-06's device walk.

## Threat Flags

None: the new surface (filesDir/readings, palm.db rows) is the plan's T-05-01..T-05-07; backup exclusions untouched.

## Self-Check: PASSED

- Files exist: ReadingStore.kt, ResultScreen.kt, LastReadingCard.kt, FixtureLab.kt, 2.json, result-open.sh, ReadingStoreTest.kt, HomeLastReadingTest.kt, TestReadings.kt
- Commits on HEAD: 2c0dde8, ba0ba3a, 314e18a
