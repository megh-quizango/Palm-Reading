---
phase: "5"
slug: "result-against-the-fixture"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: true
wave_0_complete: true
created: "2026-11-28"
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 + kotlinx-coroutines-test 1.11.0 + Turbine 1.2.1 (JVM); androidx.test runner 1.7.0 / ext-junit 1.3.0 + Compose ui-test-junit4 1.12.1 (instrumented, emulator-5554) |
| **Config file** | `android/app/build.gradle.kts` (`testOptions.unitTests.all { systemProperty("contracts.dir", ...) }`; Wave 0 adds `fixtures.dir`) |
| **Quick run command** | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.reading.*' --tests 'app.palm.feature.result.*' --tests 'app.palm.feature.invalidphoto.*' --tests 'app.palm.feature.scan.*'` |
| **Full suite command** | `bash tools/gradlew.sh :app:testDebugUnitTest` then `ADB_SERIAL=emulator-5554 bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected` |
| **Estimated runtime** | ~90 seconds quick (JVM, warm daemon); ~6 minutes full (estimate, not measured) |

---

## Sampling Rate

- **After every task commit:** Run the quick run command (JVM only)
- **After every plan wave:** Run the full JVM suite plus the instrumented classes the wave touched on emulator-5554 (keep the `tests="0"` guard)
- **Before `/gsd-verify-work`:** Full suite must be green, plus the emulator walk of all 13 fixture variants in en and hi
- **Max feedback latency:** 120 seconds (JVM quick run)

---

## Per-Task Verification Map

Filled at planning time (2026-11-28). Waves: 05-01 (wave 1, tracer) -> 05-02, 05-03, 05-04, 05-05 (wave 2, disjoint files) -> 05-06 (wave 3, gate).
Class-only entries under "Automated Command" run with the quick command's Gradle task (`--tests`) or the connected task (`-Pandroid.testInstrumentationRunnerArguments.class=...`, comma-separated classes) on emulator-5554, always as `rm -rf android/app/build/outputs/androidTest-results/connected && ADB_SERIAL=emulator-5554 bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -P...class=... -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected`. The exact commands are in each plan's `<verify>`.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 05-01-T1 | 05-01 | 1 | RES-02, RES-04, RES-05, RES-07 (tracer) | T-05-02, T-05-03, T-05-06, T-05-07, T-05-SC | Row has no landmarks (2.json); no log calls in reading/result/home/lab; lab only in src/debug; Back/Retake blocked while saving; catalog unchanged; frozen wave-2 signatures present (frozen-symbol grep) | build + device (tracer) | `bash tools/gradlew.sh :app:assembleDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest`; emulator chain: `install-debug.sh` -> `result-open.sh` -> expect "Your palm reading" "^Your palm photo with lines drawn" (the photo box's description, only present when the photo decoded) "For entertainment only" -> `run-as ls cache/scan` empty and a UUID `.jpg` in `files/readings` -> `ui.sh back` -> expect "Read your palm" (Home) | ❌ W0 (result-open.sh and the ui.sh hold / scroll-to verbs are created by this task) | ✅ green |
| 05-01-T2 | 05-01 | 1 | RES-05 | T-05-01, T-05-02, T-05-05 | Lower-case UUID round trip before any file work; photo resolved only inside readings/ (canonical path); insert failure leaves no file; delete removes row + photo; v1 file opens as v2 (destructive); "landmarks" never in the row JSON | unit (FakeReadingDao + TemporaryFolder) + instrumented (Room in-memory) | `--tests 'app.palm.core.reading.ReadingStoreTest' --tests 'app.palm.core.reading.ReadingRulesTest' --tests 'app.palm.core.navigation.PushFromTest'`; connected `app.palm.core.db.AppDatabaseTest,app.palm.core.reading.ReadingStoreRoomTest` | ❌ W0 (TDD in task; AppDatabaseTest exists and is edited) | ✅ green |
| 05-01-T3 | 05-01 | 1 | HOME-03, RES-05 | T-05-03 | HomeLastReadingTest checks and deletes only the reading it created (shared install, Pitfall 10); no log calls in feature/home | instrumented (real graph) + device | connected `app.palm.feature.home.HomeLastReadingTest,app.palm.feature.home.HomeTest` (newest seeded reading on the card, tap opens it (marker text), Back to Home, after delete `latest()` is not the seeded id); emulator chain: `install-debug.sh` -> `result-open.sh` -> `am force-stop` -> `ui.sh launch` -> tap "Your last reading" -> expect "Your palm reading" "^Your palm photo with lines drawn" "For entertainment only" -> `ui.sh absent "Photo unavailable"` (fails when the photo did not decode after reopen); the empty case (no card) is proven by 05-05-T2 | ❌ W0 (TDD in task) | ✅ green |
| 05-02-T1 | 05-02 | 2 | RES-02, RES-03 | T-05-08 | Points clamped to 0..1; at most 64 points per line | unit + Compose instrumented | `--tests 'app.palm.feature.result.OverlayMathTest' --tests 'app.palm.feature.result.LineRulesTest'` (fitRect + map for 3:4, 1:1, 9:16, letterboxed; Catmull-Rom through points, 2 points straight; isDrawn / badge per trace_source incl. present+none; order by LineType); connected `app.palm.feature.result.PalmOverlayTest` (line colour at mapped trace points on 3 aspects + letterbox; no palm colour for all-none) | ❌ W0 (TDD in task; test photos generated in code) | ✅ green |
| 05-02-T2 | 05-02 | 2 | RES-01, RES-03, RES-04 (RES-07 actions exercised) | T-05-05, T-05-09 | Disclaimer on every tab (policy); bad JSON / rule violation / invalid verdict / missing row -> "couldn't open" state, no crash; debug aspect warning logs two numbers only | unit + Compose instrumented + device | `--tests 'app.palm.feature.result.ResultViewModelTest'`; connected `app.palm.feature.result.ResultScreenTest,app.palm.feature.result.PalmOverlayTest` (tabs, disclaimer per tab, recreate keeps the tab, Scan again -> HandSelect, Ask [Guide] -> coming-soon sheet, Back -> Home, notes, "Photo unavailable" with no legend chip and no approximate note, D-12); emulator `result-open.sh` -> expect "Lines are drawn approximately" "For entertainment only" | ❌ W0 (TDD in task) | ✅ green |
| 05-03-T1 | 05-03 | 2 | READ-03 | — | N/A | unit + Compose instrumented | `--tests 'app.palm.feature.invalidphoto.ReasonCopyTest' --tests 'app.palm.core.navigation.PushFromTest'` (7 reasons -> icon/title/tip, exhaustive; popTo; showInvalid replaces ScanReady with InvalidPhoto so popFrom lands on Scan(hand), other tops unchanged); connected `app.palm.feature.invalidphoto.InvalidPhotoTest` (each reason renders; Retake / Back to Home / top-bar back callbacks; no disclaimer) | ❌ W0 (TDD in task) | ✅ green |
| 05-03-T2 | 05-03 | 2 | READ-03 (D-03, D-15) | T-05-10, T-05-11 | Invalid verdict: no row, no file in readings/, capture deleted before InvalidPhoto, Home's last reading unchanged (free scan not used); failure keeps the capture | unit (VM with real holder + store over fakes) | `--tests 'app.palm.feature.scan.ScanReadyViewModelTest'` (valid saves once + id once; double tap = one read; 7 invalid reasons; IOException -> Failed + retry; rule violation; failed save); `bash tools/gradlew.sh :app:assembleDebug :app:testDebugUnitTest` | ❌ W0 (TDD in task) | ✅ green |
| 05-04-T1 | 05-04 | 2 | RES-06, RES-04 | T-05-13, T-05-14 | One overwritten `cacheDir/share/reading.png`; PNG via Bitmap.compress (no EXIF); hidden card excluded from semantics | Compose instrumented | connected `app.palm.feature.result.share.ShareTest` (capture 1080x1350 after the layer is recorded, awaited with `waitUntil`, never a frame count; PNG decodes and is not blank, single file after two writes, missing photo and long text still 1080x1350) | ❌ W0 (TDD in task; gates RESEARCH A1) | ✅ green |
| 05-04-T2 | 05-04 | 2 | RES-06 | T-05-12, T-05-13, T-05-15 | Only `cache-path share/`; provider not exported; per-URI read grant on send + chooser; a readings/ file cannot get a URI; share dir emptied at start | Compose instrumented + device | connected `app.palm.feature.result.share.ShareTest` (intent fields, URI authority + decode, readings path throws, ProviderInfo exported=false, in-app Share tap writes the PNG and the button recovers); emulator `result-open.sh` -> `ui.sh scroll-to "^Share"` -> tap -> expect chooser activity -> `run-as ls cache/share` = reading.png | ❌ W0 (TDD in task) | ✅ green |
| 05-05-T1 | 05-05 | 2 | RES-02, RES-03, READ-03 (D-02, D-04, D-06) | T-05-16, T-05-17, T-05-SC | Strict decode + ReadingRules for every variant; release twin of the variant seam returns null; only the fixtures.dir build line added | unit + build | `--tests 'app.palm.core.reading.FixturesTest' --tests 'app.palm.core.reading.FixtureRepositoryTest'` (en/invalid fixture == contracts; hi mirrors en; 14 variants valid with their property; stamping id/hand/size/locale/time, no landmarks); `bash tools/gradlew.sh :app:compileReleaseKotlin :app:assembleDebug` | ❌ W0 (TDD in task; adds the fixtures.dir property) | ✅ green |
| 05-05-T2 | 05-05 | 2 | D-05, D-06 (RES-02, READ-03 reachability) | T-05-16 | Lab and its choice object only in src/debug | build + device | `bash tools/gradlew.sh :app:assembleDebug :app:compileReleaseKotlin`; emulator `result-open.sh --aspect 1:1 --variant all_llm` -> expect "^Fate  \("; `--aspect 9:16 --skin dark --variant hindi` -> scroll-to the hi summary (captured with `python -I -X utf8`); lab "Delete all readings" (pinned bar outside the scrolling pickers) -> Home has no "Your last reading" | ✅ (result-open.sh from 05-01) | ✅ green |
| 05-06-T1 | 05-06 | 3 | all 9 | T-05-03, T-05-06, T-05-SC | No lab symbol in main/release; no log calls in reading/result except the debug aspect warning; en/hi key parity; catalog unchanged | unit + build + instrumented + static | `bash tools/gradlew.sh :app:testDebugUnitTest :app:assembleRelease`; full `:app:connectedDebugAndroidTest` with the zero-test guard and the 13-class presence loop; static gate command in 05-06 Task 1 | ✅ (all created by earlier plans) | ✅ green |
| 05-06-T2 | 05-06 | 3 | all 9 | T-05-18, T-05-19 | Screenshots git-ignored; font scale and app locale restored by a trap | device + human check | `ADB_SERIAL=emulator-5554 bash tools/device-lock.sh bash tools/result-walk.sh` + at least 40 `phase5-*.png` + `git check-ignore` + font scale not 2.0 afterwards; `tools/res_pattern.py` check (en invalid_no_palm_lines_title pattern matches "We couldn't see your palm lines" through `grep -E`; hi keys print without UnicodeEncodeError); `<human-check>` visual sign-off (including the halo alpha 0.55 vs D-10's ~30%) for `/gsd-verify-work` | ❌ W0 (result-walk.sh and res_pattern.py created by this task) | ✅ green (automated; human check pending) |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

05-06-T1 run (2026-11-28, emulator-5554): `:app:testDebugUnitTest :app:assembleRelease` exit 0; full `:app:connectedDebugAndroidTest` green on the second run (all 13 classes present, no `tests="0"`). The first run failed HandSelectTest and HomeLastReadingTest.aDeletedReadingIsNoLongerOpened with "No compose hierarchies found" while another session's app (com.meghquizango.pagewise) held the foreground on the shared emulator; neither failure reproduced. Per-task rows 05-01..05-05 are green from their plan summaries and this full run.

---

## Wave 0 Requirements

Each item is created by the task that first needs it (TDD tasks write their test class first):

- [x] `android/app/build.gradle.kts` — `fixtures.dir` system property for unit tests (Pitfall 7) — 05-05-T1 (the first task whose tests read the assets; 05-01's JVM tests read `contracts.dir`)
- [x] `android/app/src/test/java/app/palm/testing/FakeReadingDao.kt`, `FakePhotoDecoder.kt` — 05-01-T2 (shared by 05-02 and 05-03 in wave 2); `FakePalmReadingRepository.kt` — 05-03-T2
- [x] `ReadingEntryPoint` (main, `core/reading/ReadingModule.kt`) — 05-01-T1; androidTest `testing/TestReadings.kt` (`seedReading`) — 05-01-T2, used by HomeLastReadingTest (05-01-T3), ResultScreenTest, ShareTest
- [x] Plain test photos generated in code inside `PalmOverlayTest` and `ShareTest` (no new asset file) — 05-02-T1, 05-04-T1
- [x] `AppDatabaseTest` — compiles against v2 in 05-01-T1, gains the v1 -> v2 case in 05-01-T2
- [x] Every ❌ W0 class in the map is written first inside its own TDD task

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Real system share sheet opens and a target receives the image card | RES-06 | The system chooser and third-party targets are outside the app's instrumentation | On emulator-5554 open a reading, tap Share, confirm the chooser appears, pick a target (e.g. Files/Messages), confirm the 1080x1350 branded card with disclaimer arrives; screenshot |
| Overlay visually matches the palm (lines sit on the photo, halo readable, legend colours match cards) | RES-02, RES-03 | Visual alignment and legibility need human judgement beyond pixel probes | Walk all 13 fixture variants on emulator-5554 (debug lab); screenshot each; check line placement, dark halo contrast, legend chips vs line cards, all-none variant shows no overlay; approve the halo alpha (0.55, HALO_ALPHA in PalmPhoto.kt) or ask for D-10's 0.30 |
| Every lab variant in en and hi at font scale 2.0 (no clipping, disclaimer visible) | RES-01, RES-03, RES-04 (D-24) | Layout under large text and Hindi script is a visual check | `adb shell settings put system font_scale 2.0`, walk all variants in en and hi, screenshot; restore with `adb shell settings put system font_scale 1.0` |
| API 26-27 share capture path (`LayerSnapshotV22`) | RES-06 | No API 26-27 system image installed; phone (API 30) covers V28 only | Accept as unverified; record in the phase summary |

**05-06-T2 results (2026-11-28, emulator-5554, `tools/result-walk.sh`, 42 screenshots in `tools/adb/shots/phase5-*.png`, git-ignored):**

| Behavior | Emulator result | Still open |
|----------|-----------------|------------|
| Share sheet | `phase5-share-sheet.png`: the system chooser (com.android.intentresolver) opens with the card thumbnail and "My palm reading from [AppName]"; targets Quick Share, Google, Print, Drive, Maps | Picking a target and checking the received 1080 x 1350 PNG (human check); MIUI phone share (phone session) |
| Overlay / halo / legend | 14 variants x en/hi, every aspect with both skins; all_none shows no overlay, no legend and the "couldn't trace" note. The walk found the 9:16 photo box overflowing onto the top bar and the legend (fixed in `057a726`, regression test `ResultScreenTest.aTallPhotoStaysAboveItsLegend`) | Halo alpha 0.55 vs D-10 0.30 decision and line placement judgement (human check); real light-skin palm (phone session, Phase 6) |
| Font scale 2.0 en/hi | `phase5-{en,hi}-{mixed,long_text,invalid_jewelry,home}-fs2.png` captured; font scale and app locale restored by the trap (verified 1.0 / [en]) | Clipping judgement (human check) |
| API 26-27 capture path | Not run (no system image) | Accepted as unverified; phone-session list |

All four rows are run (or recorded as phone-session items) in 05-06-T2 through `tools/result-walk.sh` screenshots and its `<human-check>`; the walk covers the 13 D-06 variants plus the long-text variant (05-05) in en and hi.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 120s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** automated sign-off 2026-11-28 (05-06); the visual `<human-check>` (halo alpha, overlay, Hindi, font scale, share target) is collected by `/gsd-verify-work`
