---
phase: 05-result-against-the-fixture
verified: 2026-11-28T09:55:00Z
status: human_needed
score: 5/5 must-haves verified
covered_files:
  - .planning/phases/05-result-against-the-fixture/05-01-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-01-SUMMARY.md
  - .planning/phases/05-result-against-the-fixture/05-02-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-02-SUMMARY.md
  - .planning/phases/05-result-against-the-fixture/05-03-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-03-SUMMARY.md
  - .planning/phases/05-result-against-the-fixture/05-04-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-04-SUMMARY.md
  - .planning/phases/05-result-against-the-fixture/05-05-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-05-SUMMARY.md
  - .planning/phases/05-result-against-the-fixture/05-06-PLAN.md
  - .planning/phases/05-result-against-the-fixture/05-06-SUMMARY.md
  - android/app/src/main/AndroidManifest.xml
  - android/app/src/main/java/app/palm/PalmApp.kt
  - android/app/src/main/java/app/palm/core/db/AppDatabase.kt
  - android/app/src/main/java/app/palm/core/navigation/Routes.kt
  - android/app/src/main/java/app/palm/core/reading/FixtureVariant.kt
  - android/app/src/main/java/app/palm/core/reading/PalmReadingRepository.kt
  - android/app/src/main/java/app/palm/core/reading/ReadingModule.kt
  - android/app/src/main/java/app/palm/core/reading/ReadingRules.kt
  - android/app/src/main/java/app/palm/core/reading/ReadingStore.kt
  - android/app/src/main/java/app/palm/feature/home/HomeViewModel.kt
  - android/app/src/main/java/app/palm/feature/home/LastReadingCard.kt
  - android/app/src/main/java/app/palm/feature/invalidphoto/InvalidPhotoScreen.kt
  - android/app/src/main/java/app/palm/feature/result/LineLegend.kt
  - android/app/src/main/java/app/palm/feature/result/LineRules.kt
  - android/app/src/main/java/app/palm/feature/result/OverlayMath.kt
  - android/app/src/main/java/app/palm/feature/result/PalmPhoto.kt
  - android/app/src/main/java/app/palm/feature/result/ResultActions.kt
  - android/app/src/main/java/app/palm/feature/result/ResultEntries.kt
  - android/app/src/main/java/app/palm/feature/result/ResultScreen.kt
  - android/app/src/main/java/app/palm/feature/result/ResultViewModel.kt
  - android/app/src/main/java/app/palm/feature/result/share/Share.kt
  - android/app/src/main/java/app/palm/feature/result/share/ShareCard.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanEntries.kt
  - android/app/src/main/java/app/palm/feature/scan/ScanReadyViewModel.kt
  - android/app/src/main/res/xml/share_paths.xml
covered_digest: "v3:sha256:046bf2b1c218254b1cf43e56ef4b394db60846cf3d747f9c3e757cdba4b4c939"
behavior_unverified: 0
overrides_applied: 0
human_verification:
  - test: "Halo alpha decision: HALO_ALPHA in feature/result/PalmPhoto.kt is 0.55 (BgDeep, 9 dp); locked decision D-10 says about 30%. Look at tools/adb/shots/phase5-*.png (light and dark skin, 3:4 / 1:1 / 9:16)."
    expected: "User replies 'approve 0.55' or asks for 0.30 (one constant)."
    why_human: "Deviation from a locked user decision; only the user can accept it."
  - test: "Visual sign-off of the 42 walk screenshots in tools/adb/shots/phase5-*.png: lines sit on the photo, legend chips match cards, all_none shows no overlay/legend and the couldn't-trace note, present_not_drawn says Not clearly visible, each invalid reason has its own icon/title/tip, Hindi has no clipped matras, nothing clipped at font scale 2.0."
    expected: "Reply 'approved' or list what is wrong."
    why_human: "Visual quality and Hindi rendering judgement; automated tests already cover placement pixels, text presence and layout bounds."
  - test: "Phone session (MIUI): open a reading, tap Share, pick WhatsApp or Gmail."
    expected: "The 1080 x 1350 card arrives with 'My palm reading from [AppName]'; MIUI sheet opens without resolver error; Back returns to Result with Share idle."
    why_human: "Needs the physical MIUI phone and a real share target; emulator chooser already verified."
  - test: "Android 8.0/8.1 device: tap Share on a reading."
    expected: "Card PNG is not blank (LayerSnapshotV22 path); if blank, ship the D-21 Canvas fallback."
    why_human: "No API 26-27 system image on this machine."
---

# Phase 5: Result Against the Fixture Verification Report

**Phase Goal:** The user sees a believable Palm / Love / Career reading with the detected lines drawn over their own photo, saved on the device and shareable, built and verified against fixture data before the real backend is wired.
**Verified:** 2026-11-28T09:55:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

All 5 roadmap success criteria hold in the code and were confirmed by tests run in this verification (JVM + instrumented on emulator-5554) and an end-to-end chain on emulator-5554. What remains is the user's decision on the halo alpha deviation (D-10), the visual sign-off that 05-VALIDATION.md hands to verify-work, and two items that need a physical phone.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP SC) | Status | Evidence |
|---|-------|--------|----------|
| 1 | With fixture data the result opens on Palm / Love / Career tabs; lines drawn over the user's photo with colour + text legend, correct on 3 aspects and for every trace_source (llm/template = approximate, none = card without overlay) | VERIFIED | `ResultScreen.kt` `ResultTabs` (PrimaryTabRow, rememberSaveable) + Palm/Love/Career tab bodies; `PalmPhoto.kt` draws through `fitRect`/Catmull-Rom; `LineRules.isDrawn()` excludes `none`/<2 points/`present=false`; `badge()` = Approximate for drawn, Not clearly visible otherwise; `LineLegend` = colour dot + localised name chip. Tests run now: PalmOverlayTest (4: pixel colours at mapped points on 3 aspects + letterboxed, none draws nothing), ResultScreenTest (6 incl. tabs, aTallPhotoStaysAboveItsLegend, noDrawnLinesShowTheNoLinesNote), OverlayMathTest, LineRulesTest |
| 2 | A card per detected line (Heart, Head, Life, Fate, Sun) with label and interpretation; entertainment disclaimer on every reading at readable contrast | VERIFIED | `PalmTab` -> `orderedLines(...).forEach { LineCard(it) }`; disclaimer placed above tabs in `Found()` (visible on all tabs) and on `ShareCard.kt:109`. Contrast TextMuted #B4ACDB: 8.19:1 on sky top #1A1240, 9.22:1 on bg.base, 9.43:1 on bg.deep (computed here). ResultScreenTest#tabsShowTheirCardsAndTheDisclaimer asserts the disclaimer on each tab |
| 3 | For each of 7 unusable-photo reasons: reason + retake tip + retake button; not saved, not counted | VERIFIED | `InvalidPhotoScreen.reasonCopy` exhaustive `when` (7 entries, no else); `ScanReadyViewModel.read()` not-ok branch calls `holder.clear()` only, never `store.save`; `showInvalid` puts InvalidPhoto in place of ScanReady so Retake/Back land on Scan(hand). Tests: ReasonCopyTest, ScanReadyViewModelTest (10), InvalidPhotoTest (3, ran now). E2E now: invalid_bent shows "Photo not accepted" / "Your hand is bent" / tip / Retake photo / Back to Home, no disclaimer; readings count 55 -> 55, cache/scan 0 |
| 4 | Reading (photo + JSON) saved on device, opens after kill + relaunch; Home reopens last reading | VERIFIED | `ReadingStore.save` moves capture to `filesDir/readings/<uuid>.jpg` + Room upsert (DB v2, destructive fallback from v1 only); `HomeViewModel.lastReading` from `store.latest()`; Home `onOpenReading` pushes `Result(id)`. Tests now: ReadingStoreRoomTest (2), AppDatabaseTest (2, incl. v1 -> v2), HomeLastReadingTest (2). E2E now: valid read 54 -> 55 files, cache/scan 0; `am force-stop`, relaunch, tap "Your last reading" -> Result with disclaimer, "Photo unavailable" absent |
| 5 | Share as branded image card via system share sheet; Scan again from result; "Ask [Guide]" coming-soon | VERIFIED | `ResultActions` -> `ShareCapture` -> `writeSharePng` (cacheDir/share) -> `shareChooser(shareIntent(shareUri(...)))`; FileProvider `exported=false`, only `cache-path share/`. Scan again pushes HandSelect; Ask [Guide] pushes `ComingSoon(Feature.ASK_GUIDE)`. Tests now: ShareTest (6: 1080 x 1350 PNG with content, provider refuses filesDir/readings, intent carries URI + read grant, tap opens the system chooser), ResultScreenTest#theTabSurvivesRecreationAndActionsLandRight (Scan again -> "Which hand should we read?", Ask [Guide] -> "Coming soon") |

**Score:** 5/5 truths verified (0 present, behavior-unverified)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `core/reading/PalmReadingRepository.kt` + `ReadingModule.kt` | Seam + fixture impl, one @Binds | VERIFIED | Fixture stamped with fresh UUID, capture hand/size, locale, time; `debugFixtureVariant()` release twin returns null |
| `core/reading/ReadingStore.kt` | Save/delete photo + row | VERIFIED | UUID guard, atomic move with copy fallback, insert failure deletes photo, canonical-path containment |
| `core/db/AppDatabase.kt` + `schemas/.../2.json` | v2 entity, index on createdAt | VERIFIED | version 2, `fallbackToDestructiveMigrationFrom(true, 1)` |
| `feature/result/*` | S10 Result | VERIFIED | Wired via `resultEntries` in AppNavDisplay |
| `feature/invalidphoto/InvalidPhotoScreen.kt` | 7 reasons | VERIFIED | Wired via `scanEntries` |
| `feature/result/share/*`, manifest provider, `share_paths.xml` | Share card + intent | VERIFIED | Share dir emptied in `PalmApp.onCreate` |
| `feature/home/LastReadingCard.kt`, `HomeViewModel` | Reopen card | VERIFIED | Live from `latest()` |
| `assets/fixtures/palm_reading.{en,hi,invalid}.json` | Fixtures | VERIFIED | FixturesTest drift guard vs `contracts/` |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| ScanReady "Read my palm" | PalmReadingRepository -> ReadingStore.save | `ScanReadyViewModel.read()` | WIRED |
| ScanReady (saved) | Result(id) | `replaceAll(Home, Result(id))` in ScanEntries | WIRED |
| ScanReady (invalid) | InvalidPhoto(reason, hand) | `showInvalid` | WIRED |
| Result | Room row + photo | `ResultViewModel` -> `store.byId` -> `PhotoDecoder` | WIRED |
| Home card | Result(id) | `HomeEntries.onOpenReading` | WIRED |
| Result Share | system chooser | ResultActions -> ShareCapture -> FileProvider | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| ResultScreen | reading + photo | Room row JSON (strict decode + ReadingRules) + filesDir JPEG | Yes (fixture stamped per capture, by design for this phase) | FLOWING |
| LastReadingCard | id/hand/date/thumbnail | `store.latest()` | Yes | FLOWING |

### Behavioral Spot-Checks (run in this verification)

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| JVM suite + debug APK | `bash tools/gradlew.sh :app:testDebugUnitTest :app:assembleDebug` | exit 0; 32 suites, 198 tests, 0 failures, 1 skip (Phase 4 HandGuidanceTest recorded-fixtures assumption); tasks UP-TO-DATE from the identical-input 14:45 run, no source changed since | PASS |
| Phase 5 instrumented classes | `connectedDebugAndroidTest` (ResultScreenTest, PalmOverlayTest, ShareTest, HomeLastReadingTest, InvalidPhotoTest, ReadingStoreRoomTest, AppDatabaseTest), APKs left installed | 25/25 pass, no suite with tests="0" | PASS |
| Valid read saves, kill + reopen | result-open.sh --aspect 9:16 --variant mixed; force-stop; launch; tap Your last reading | readings 54 -> 55, cache/scan 0, Result reopened with photo | PASS |
| Invalid read saves nothing | result-open.sh --variant invalid_bent; Back to Home | readings 55 -> 55, cache/scan 0, Home still shows last reading | PASS |

Full `connectedDebugAndroidTest` was not re-run (05-06 ran it green); only the Phase 5 classes were.

### Probe Execution

Step 7c: not applicable (no `probe-*.sh` declared for this phase).

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| RES-01 | Palm / Love / Career tabs | SATISFIED | Truth 1 |
| RES-02 | Lines over own photo + colour/text legend | SATISFIED | Truth 1 |
| RES-03 | Card per line with label + interpretation | SATISFIED | Truth 2 |
| RES-04 | Entertainment disclaimer on every reading | SATISFIED | Truth 2 (Result + share card) |
| RES-05 | Reading saved, survives restarts | SATISFIED | Truth 4 |
| RES-06 | Share as branded image card | SATISFIED | Truth 5 (emulator); MIUI share is a phone item |
| RES-07 | Scan again; Ask [Guide] coming soon | SATISFIED | Truth 5 |
| READ-03 | 7 reasons with retake tip, no free scan used | SATISFIED | Truth 3; the counter itself is Phase 6, constraint recorded (increment only in `ReadingStore.save`) |
| HOME-03 | Reopen last reading from Home | SATISFIED | Truth 4 |

No orphaned requirements: all 9 IDs mapped to Phase 5 are claimed by plans.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| ReadingStore.kt | save | `shortcut:` comment (crash between move and insert leaves a private orphan photo) | Info | Documented; `deleteAll` sweeps orphans |
| PalmPhoto.kt | 61 | `HALO_ALPHA = 0.55f` vs locked D-10 ~30% | Warning | User decision (human item 1) |

No TBD/FIXME/XXX markers in Phase 5 files.

### Human Verification Required

### 1. Halo alpha (D-10 deviation)
**Test:** Look at `tools/adb/shots/phase5-*.png`; HALO_ALPHA is 0.55 in `PalmPhoto.kt`, D-10 says about 30%.
**Expected:** Approve 0.55 or ask for 0.30.
**Why human:** Deviation from a locked user decision.

### 2. Visual sign-off of the walk screenshots
**Test:** Review the 42 `phase5-*.png` (variants x en/hi, 3 aspects, both skins, font scale 2.0, reopen, share sheet).
**Expected:** Lines on the photo, legend matches cards, all_none/present_not_drawn correct, invalid reasons distinct, no clipped Hindi, nothing clipped at fs 2.0. Spot-checked here: `phase5-en-all_template.png` (9:16, five lines, legend, note, disclaimer, tabs) and `phase5-hi-all_none.png` (no overlay, no legend, couldn't-trace note, Hindi disclaimer) look right.
**Why human:** Visual and language judgement.

### 3. Share on the MIUI phone (phone session)
**Test:** Open a reading, tap Share, pick WhatsApp or Gmail.
**Expected:** The 1080 x 1350 card arrives with "My palm reading from [AppName]"; Back returns to Result with Share idle.
**Why human:** Needs the physical phone.

### 4. Share capture on Android 8.0/8.1 (phone session)
**Test:** Tap Share on an API 26-27 device.
**Expected:** Card not blank; otherwise ship the D-21 Canvas fallback.
**Why human:** No API 26-27 image available.

Also open (not blocking): native-speaker review of the Hindi strings and `palm_reading.hi.json`, Figma visual QA (both tracked in STATE.md / 05-06-SUMMARY).

### Gaps Summary

No gaps. Every success criterion and all 9 requirements are implemented, wired and confirmed by tests run in this verification. Status is human_needed only for the halo alpha decision, the screenshot sign-off and two phone-only share checks.

Side effect on the device: the end-to-end chain added one fixture reading to the debug install on emulator-5554 (55 readings now). No setting was changed.

---

_Verified: 2026-11-28T09:55:00Z_
_Verifier: Claude (gsd-verifier)_
