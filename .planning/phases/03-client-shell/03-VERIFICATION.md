---
phase: 03-client-shell
verified: 2026-11-27T13:05:00Z
status: gaps_found
score: 8/12 must-haves verified
covered_files: [".planning/phases/03-client-shell/03-01-PLAN.md", ".planning/phases/03-client-shell/03-01-SUMMARY.md", ".planning/phases/03-client-shell/03-02-PLAN.md", ".planning/phases/03-client-shell/03-02-SUMMARY.md", ".planning/phases/03-client-shell/03-03-PLAN.md", ".planning/phases/03-client-shell/03-03-SUMMARY.md", ".planning/phases/03-client-shell/03-04-PLAN.md", ".planning/phases/03-client-shell/03-04-SUMMARY.md", ".planning/phases/03-client-shell/03-05-PLAN.md", "android/app/build.gradle.kts", "android/app/src/androidTest/java/app/palm/feature/handselect/HandSelectTest.kt", "android/app/src/androidTest/java/app/palm/feature/home/HomeTest.kt", "android/app/src/androidTest/java/app/palm/feature/onboarding/LanguageSwitchTest.kt", "android/app/src/androidTest/java/app/palm/feature/onboarding/OnboardingFlowTest.kt", "android/app/src/debug/java/app/palm/debug/DebugEntries.kt", "android/app/src/debug/java/app/palm/showcase/BackendStatus.kt", "android/app/src/debug/java/app/palm/showcase/ShowcaseScreen.kt", "android/app/src/main/AndroidManifest.xml", "android/app/src/main/java/app/palm/MainActivity.kt", "android/app/src/main/java/app/palm/MainViewModel.kt", "android/app/src/main/java/app/palm/core/data/ProfileRepository.kt", "android/app/src/main/java/app/palm/core/designsystem/Effects.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneBackground.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneTheme.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneTokens.kt", "android/app/src/main/java/app/palm/core/designsystem/components/Buttons.kt", "android/app/src/main/java/app/palm/core/designsystem/components/Chips.kt", "android/app/src/main/java/app/palm/core/designsystem/components/ChoiceTile.kt", "android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt", "android/app/src/main/java/app/palm/core/designsystem/components/TextFields.kt", "android/app/src/main/java/app/palm/core/designsystem/components/TopBar.kt", "android/app/src/main/java/app/palm/core/locale/AppLocale.kt", "android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt", "android/app/src/main/java/app/palm/core/navigation/BottomSheetSceneStrategy.kt", "android/app/src/main/java/app/palm/core/navigation/Routes.kt", "android/app/src/main/java/app/palm/feature/handselect/HandSelectEntries.kt", "android/app/src/main/java/app/palm/feature/handselect/HandSelectScreen.kt", "android/app/src/main/java/app/palm/feature/handselect/ScanPlaceholderScreen.kt", "android/app/src/main/java/app/palm/feature/home/ComingSoonSheet.kt", "android/app/src/main/java/app/palm/feature/home/ExitDialog.kt", "android/app/src/main/java/app/palm/feature/home/Greeting.kt", "android/app/src/main/java/app/palm/feature/home/HeroCard.kt", "android/app/src/main/java/app/palm/feature/home/HomeEntries.kt", "android/app/src/main/java/app/palm/feature/home/HomeScreen.kt", "android/app/src/main/java/app/palm/feature/home/HomeViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthRules.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/IntroScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/LanguageScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/LanguageViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameRules.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/OnboardingEntries.kt", "android/app/src/main/res/drawable/hero_orbit.xml", "android/app/src/main/res/drawable/ic_alert.xml", "android/app/src/main/res/drawable/ic_arrow.xml", "android/app/src/main/res/drawable/ic_back.xml", "android/app/src/main/res/drawable/ic_check.xml", "android/app/src/main/res/drawable/ic_chevron_down.xml", "android/app/src/main/res/drawable/ic_close.xml", "android/app/src/main/res/drawable/ic_sparkle.xml", "android/app/src/main/res/drawable/illus_constellation_hand.xml", "android/app/src/main/res/drawable/illus_guide_orb.xml", "android/app/src/main/res/drawable/illus_heart_orbit.xml", "android/app/src/main/res/drawable/illus_horoscope_star.xml", "android/app/src/main/res/drawable/illus_sun_medallion.xml", "android/app/src/main/res/drawable/illus_tarot_pair.xml", "android/app/src/main/res/drawable/illus_zodiac_wheel.xml", "android/app/src/main/res/drawable/tip_blurry_dark.xml", "android/app/src/main/res/drawable/tip_fingers_closed.xml", "android/app/src/main/res/drawable/tip_good.xml", "android/app/src/main/res/drawable/tip_jewellery_bent.xml", "android/app/src/main/res/drawable/tip_too_far.xml", "android/app/src/main/res/values-hi/strings.xml", "android/app/src/main/res/values-hi/strings_home.xml", "android/app/src/main/res/values-hi/strings_onboarding.xml", "android/app/src/main/res/values/strings.xml", "android/app/src/main/res/values/strings_home.xml", "android/app/src/main/res/values/strings_onboarding.xml", "android/app/src/main/res/values/themes.xml", "android/app/src/main/res/xml/locale_config.xml", "android/app/src/release/java/app/palm/debug/DebugEntries.kt", "android/app/src/test/java/app/palm/MainViewModelTest.kt", "android/app/src/test/java/app/palm/core/data/ProfileRepositoryTest.kt", "android/app/src/test/java/app/palm/core/designsystem/NocturneSkyTest.kt", "android/app/src/test/java/app/palm/core/navigation/PushFromTest.kt", "android/app/src/test/java/app/palm/core/navigation/StartRouteTest.kt", "android/app/src/test/java/app/palm/feature/home/GreetingTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/BirthRulesTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/BirthViewModelTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/LanguageViewModelTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/NameRulesTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/NameViewModelTest.kt", "android/app/src/test/java/app/palm/testing/FakeAppLocale.kt", "android/app/src/test/java/app/palm/testing/FakeProfileRepository.kt", "android/app/src/test/java/app/palm/testing/MainDispatcherRule.kt", "android/gradle/libs.versions.toml", "tools/check-jank.sh", "tools/check-motion.sh", "tools/cold-start.sh", "tools/device-lock.sh", "tools/test_gen_tokens.py", "tools/ui.sh"]
covered_digest: "v3:sha256:a6ba29206073379b36b06915f5127e88ff43bed7f4161df9d055df28fd449b62"
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "STATE.md flags the Claude-written Hindi strings for native-speaker review before release and records the Phase 3 gate results (03-05 must-have; D-06)"
    status: failed
    reason: "03-05 Task 2 step 7 was never done. STATE.md has no 'native-speaker' line, no Phase 3 cold-start or jank results, and no note on which locale path the hardware covered. Its last edit is the 03-04 docs commit 896299b."
    artifacts:
      - path: ".planning/STATE.md"
        issue: "grep -i 'native-speaker' gives 0 matches; there are no Phase 3 cold-start or jank decision lines"
    missing:
      - "Blockers/Concerns: '[Phase 3]: all Hindi strings (values-hi) are Claude-written; native-speaker review needed before release (D-06)'"
      - "Correct the hardware note for the reference phone: Redmi Note 10T 5G is API 33, so the LocaleManager path is verified there and the API 26-32 AppCompat path is not (the plan text has this backwards)"
      - "Decision lines: AppCompat 1.8.0 + autoStoreLocales, the cold-start medians with the gated variant, and the jank metric (deadline-missed share)"
  - truth: "The en/hi screenshot walk of S01-S07 (font scale 1.0 and 2.0, Hindi Home at 1.3) exists for the user's Figma sign-off (03-05 must-have; feeds Task 3)"
    status: failed
    reason: "There are no phase3-*.png files anywhere in the repo, and tools/adb/shots/ holds only Phase 1 and debug shots. The user cannot approve 03-05 Task 3 against Figma without this set, so the approval that is 'pending with the user' has nothing to review yet."
    artifacts:
      - path: "tools/adb/shots/"
        issue: "0 phase3-*.png files (the plan gate needs at least 30); font_scale.orig was never written"
    missing:
      - "Run 03-05 Task 2 step 6 on the reference phone with font_scale saved and restored, then hand the set to the user for Task 3"
human_verification:
  - test: "CHECKPOINT (fresh install). On the Redmi Note 10T 5G the release package app.palm.dev is NOT installed, so the first install shows a MIUI prompt. Run: export ADB_SERIAL=adb-8pucytpny5v8ijkj-HhmHVz._adb-tls-connect._tcp; bash tools/device-lock.sh bash tools/gradlew.sh :app:installRelease (or adb -s $ADB_SERIAL install -r -t -g android/app/build/outputs/apk/release/app-release.apk) while the user taps Install. Then onboard it once (03-05 Task 1 verify 1), and run BUDGET_MS=100000 PKG=app.palm.dev bash tools/cold-start.sh, then PKG=app.palm.dev bash tools/cold-start.sh --compile"
    expected: "The profile-compiled median of 5 am start -W TotalTime runs is under 2000 ms. The fresh-install median is recorded beside it."
    why_human: "Needs a tap on the phone for the fresh install of a new package id. ROADMAP SC2 (cold start < 2 s on a release build) cannot be measured on the debug package."
  - test: "No visible jank on the release build (after the checkpoint above): PKG=app.palm.dev bash tools/check-jank.sh idle 10 on Home; reset, Continue in English, then PKG=app.palm.dev bash tools/check-jank.sh swipe 3 on the intro; also check-motion off/on/saver with PKG=app.palm.dev"
    expected: "At most 5% of frames miss the deadline in both windows. Off and saver are static, and on animates."
    why_human: "On the debug build (debuggable, JIT only) Home idle passed at 25/514 frames (4.86%), but the three intro swipes FAILED at 105/280 frames (37.5%) deadline-missed (p50 27 ms, p90 38 ms). The phone dropped off adb before a repeat could run. Only the release-type build can settle ROADMAP SC4 'without visible jank'. If it also fails, this becomes a gap (look at the pager page content, glows and recomposition during the swipe)."
  - test: "03-05 Task 3: the user compares the phase3-en-* and phase3-hi-* screenshots with Figma S01-S07 (Ej98dV7fZQUhRwUyfQIlyJ, page '3 · Screens'), watches the sky on the phone, and confirms the A7 cold-start gate (profile-compiled median) and the A10 jank threshold (at most 5% deadline-missed)"
    expected: "The user replies 'approved' or lists issues. The answers are recorded in 03-05-SUMMARY.md and STATE.md."
    why_human: "Visual fidelity against Figma, Devanagari clipping and the feel of the motion are judgement calls. It is blocked by the missing screenshot gap above."
  - test: "Hindi persists across a restart on the reference phone: ui.sh reset, launch, tap हिन्दी, Continue (हिंदी में आगे बढ़ें), finish onboarding, am force-stop, then launch"
    expected: "Splash goes straight to Home with the Hindi greeting (for example 'शुभ संध्या, Asha') and no Language screen"
    why_human: "In this run I checked restart persistence of name and onboarding on the Redmi Note 10T in English only. Hindi after a restart was last seen on the vivo V2225 (03-01). The phone went offline before I could repeat it here. (It is a quick scripted check, not a judgement call.)"
---

# Phase 3: Client Shell Verification Report

**Phase Goal:** A user can open the app, onboard once, land on a Home hub over the animated cosmic background, and pick which hand to scan, with everything persisted across restarts.
**Verified:** 2026-11-27T13:05:00Z
**Status:** gaps_found
**Re-verification:** No (initial verification)
**Device:** Redmi Note 10T 5G (M2103K19I, API 33, 1080x2400, wireless serial adb-8pucytpny5v8ijkj-HhmHVz._adb-tls-connect._tcp). The other three attached phones were not touched.

MVP-mode note: `user-story.validate` returns `valid=false` for this goal, because it is not in "As a ..., I want to ..., so that ...." form. 03-05-PLAN already records this. Verification went ahead goal-backward against the ROADMAP success criteria, because the orchestrator asked for it explicitly. Run `/gsd-mvp-phase 3` if a user-story framing is wanted.

## User Flow Coverage

| Step | Expected | Evidence | Status |
|------|----------|----------|--------|
| Open the app | System splash held until the first profile read, then Language (first run) or Home (returning) | `MainActivity` `setKeepOnScreenCondition { vm.start.value == null }`; `MainViewModel` -> `startRoute`; `AppNavDisplay` replaces `[Splash]`. StartRouteTest and MainViewModelTest pass. On the device after `pm clear` the app opened on "Choose your language" | VERIFIED |
| Onboard once | Language -> 3 intro pages -> name -> birth -> Home; later Back never returns into onboarding | OnboardingFlowTest 2/2 and LanguageSwitchTest 1/1 pass on the phone. My ui.sh walk: English, Next, Next, Get started, "A" (Continue did nothing), "Asha", Female, January 15 2000, "I don't know my birth time" (time disabled, note shown), Continue -> "Good evening, Asha" | VERIFIED |
| Land on Home over the sky | Greeting, hero, 7-tile grid, coming-soon sheet, exit confirm, animated sky | HomeTest 4/4 on the phone. Back on Home showed "Leave [AppName]?" with Stay and Leave, and Stay kept Home. check-motion on/off/saver all OK | VERIFIED, except jank (see truth 6) |
| Pick which hand | Left/Right, 1 good + 4 bad tips, the hand goes on to Scan | HandSelectTest 1/1. Walk: Right selected by default, all 5 tips shown, picked Left -> "Scanner arrives in the next update / Hand: Left hand" | VERIFIED |
| Persisted across restarts | Kill and relaunch go straight to Home with the profile intact | `am force-stop` then launch -> "Good evening, Asha", and the Language and Name screens were absent (debug TotalTime 1266 ms). ProfileRepositoryTest `profileSurvivesANewStoreInstance` passes | VERIFIED for name and onboarding; Hindi after a restart is in Human Verification |

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1: first-launch splash -> language (UI switches) -> 3 intro pages -> name (at least 2 letters, optional gender) -> birth date with optional time or "I don't know" -> Home | VERIFIED | Device tests and the walk above. NameRulesTest 17, BirthRulesTest 6, Name/Birth/LanguageViewModelTest 19, all green |
| 2 | SC2a: kill and relaunch go splash -> Home with the profile intact, onboarding is not shown again, and the profile is excluded from backup | VERIFIED | Device relaunch walk; `profileSurvivesANewStoreInstance`. aapt2 on the release APK shows allowBackup=false, fullBackupContent=false, dataExtractionRules (excludes root/file/database/sharedpref/external) and localeConfig |
| 3 | SC2b: cold start to first frame is under 2 s on a release build on the reference phone | ? UNCERTAIN | `app.palm.dev` is not installed on the phone (a fresh install needs a MIUI tap, so this is a checkpoint). Debug relaunch TotalTime was 1266 ms, which is a hint only. tools/cold-start.sh exists and is correct; no measurement is recorded anywhere |
| 4 | SC3: time-of-day greeting with name, hero, grid; non-M1 tiles open "coming soon"; Back asks before leaving | VERIFIED | HomeTest 4/4 (`showsHeaderHeroAndAllTiles`, `palmTileOpensHandSelection`, `otherTileOpensComingSoonSheetOverHome`, `backOnHomeAsksBeforeLeaving` asserts activity DESTROYED after Leave). GreetingTest passes; "Good evening" at 18:2x IST |
| 5 | SC4a: the layered sky animates; it is static with system animations off and under battery saver | VERIFIED | Debug build: `check-motion.sh off` gave identical hashes, `on` gave different hashes, and `saver` gave identical hashes (same rest hash as off). NocturneSkyTest 5/5 |
| 6 | SC4b: onboarding and Home show the sky without visible jank on the reference phone | ? UNCERTAIN | Debug build: Home idle 10 s, 25/514 deadline-missed (4.86%), PASS. Intro swipe x3, 105/280 (37.5%), FAIL vs the 5% gate. The debug build is debuggable and JIT-only, so it is not representative. The release build was not measured, and the phone dropped before a repeat |
| 7 | SC4c: the scan route opts out of the sky | VERIFIED | On Scan(LEFT) with animations on, two hashes of rows 300..1400 taken 2 s apart were identical, and the pixels at (4,800) and (1070,1800) are #05040E. `Scan.showsBackground = false` feeds `NocturneBackground(animate=...)` in AppNavDisplay |
| 8 | SC5: from the hero the user picks left or right, sees 1 good + 4 bad tips, and the hand goes on to the scanner | VERIFIED | HandSelectTest plus the walk ("Hand: Left hand") |
| 9 | 03-05: the release APK has no showcase classes and no network security config, and keeps allowBackup=false | VERIFIED | `assembleRelease` exit 0; 0 `Lapp/palm/showcase/` matches in the dex; no networkSecurityConfig; the release `DebugEntries` has a null long-press target and no entries |
| 10 | 03-05: every Phase 3 instrumented test plus AppDatabaseTest passes together on the reference phone | VERIFIED | `connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`: BUILD SUCCESSFUL, 9 tests in 5 classes, 0 failures, no tests="0". Both packages are still installed |
| 11 | 03-05: STATE.md has the Hindi native-speaker review flag and the gate results | FAILED | Absent (see gaps) |
| 12 | 03-05: the en/hi S01-S07 screenshot set exists for the Figma sign-off | FAILED | 0 phase3-*.png files (see gaps) |

**Score:** 8/12 truths verified (0 present-but-behavior-unverified; 2 uncertain, routed to human/checkpoint; 2 failed)

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| core/data/ProfileRepository.kt | VERIFIED | DataStore (Okio storage), tolerant read, IOException -> empty profile, Hilt module and entry point |
| core/navigation/Routes.kt, AppNavDisplay.kt | VERIFIED | All keys, `showsBackground`, `replaceAll`, `pushFrom`, `popFrom`, `startRoute`; one sky behind NavDisplay |
| MainActivity.kt, MainViewModel.kt | VERIFIED | Splash hold, start route, locale mirror sync |
| core/locale/AppLocale.kt | VERIFIED | `setApplicationLocales`, `initialLanguage` rule |
| core/designsystem/NocturneBackground.kt, NocturneTheme.kt | VERIFIED | Draw-phase clock; `reduceMotion` = ANIMATOR_DURATION_SCALE 0 or isPowerSaveMode |
| feature/onboarding/* | VERIFIED | Pager intro, NameRules, BirthRules (UTC), the save-failure path |
| feature/home/* | VERIFIED | Greeting, hero, 7 tiles in D-12 order, ComingSoon sheet via BottomSheetSceneStrategy, ExitDialog |
| feature/handselect/* | VERIFIED | Right by default, 5 tips, Scan(hand) placeholder on BgCamera |
| debug/release DebugEntries.kt | VERIFIED | Showcase only in debug |
| tools/cold-start.sh, check-jank.sh, check-motion.sh | VERIFIED (exist, substantive) | check-motion and check-jank ran in this verification; cold-start not run (no release install) |
| android/app/build.gradle.kts release signingConfig | VERIFIED | Debug key with the REL-03 shortcut comment |
| .planning/STATE.md (03-05 additions) | MISSING content | See gaps |
| tools/adb/shots/phase3-*.png | MISSING | See gaps |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| MainActivity | MainViewModel | `setKeepOnScreenCondition { vm.start.value == null }` | WIRED |
| MainViewModel | ProfileRepository | `repo.profile.first()` -> `startRoute` | WIRED |
| AppNavDisplay | Routes | `LaunchedEffect` `replaceAll(start)` from Splash | WIRED |
| LanguageViewModel | AppLocale | `setApplicationLocales` | WIRED (LanguageSwitchTest) |
| OnboardingEntries | Routes | `pushFrom` chain, Birth `replaceAll(Home)` | WIRED |
| NameViewModel / BirthViewModel | NameRules / ProfileRepository | `checkName`, `saveBirth` + `completeOnboarding` | WIRED |
| NocturneBackground | NocturneTheme | `LocalNocturne.current.reduceMotion` | WIRED (motion checks) |
| AppNavDisplay | NocturneBackground | `animate = top route showsBackground` | WIRED (Scan hash check) |
| HandSelectEntries | Routes | `pushFrom(HandSelect, Scan(hand))` | WIRED (the hand shows on Scan) |
| HomeEntries | BottomSheetSceneStrategy | ComingSoon entry metadata `bottomSheet()` | WIRED (HomeTest sheet over Home) |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| HomeScreen greeting | `profile.firstName` | HomeViewModel <- DataStore `profile` flow | Yes ("Asha" after relaunch) | FLOWING |
| ScanPlaceholderScreen | `hand` | `Scan(hand)` nav key from HandSelect state | Yes ("Left hand") | FLOWING |
| Start route | `onboardingComplete` | DataStore first read | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| JVM unit suite | `bash tools/gradlew.sh :app:testDebugUnitTest` | exit 0, 80 tests in 14 classes, 0 failures | PASS |
| Instrumented suite | `:app:connectedDebugAndroidTest -P...leaveApksInstalledAfterRun=true` | 9/9 pass, no tests="0" | PASS |
| Motion off / on / saver (debug) | `tools/check-motion.sh off/on/saver` | OK / OK / OK; settings restored (animator 1.0, low_power 0, battery reset) | PASS |
| Scan opt-out | hash 300..1400 twice, pixel 4,800 | equal; #05040E | PASS |
| Home idle jank (debug) | `tools/check-jank.sh idle 10` | 25/514 deadline-missed (4.86%) | PASS (borderline, debug) |
| Intro swipe jank (debug) | `tools/check-jank.sh swipe 3` | 105/280 (37.5%) | FAIL on debug; release not measured |
| Release APK contents | assembleRelease + dex grep + aapt2 | clean | PASS |
| Release cold start | `PKG=app.palm.dev tools/cold-start.sh --compile` | not run: package not installed (fresh install = checkpoint) | SKIP |

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| FND-03 | Layered animated sky, static with animations off, absent on camera | NEEDS HUMAN | Static/animated/opt-out verified on the device; "no visible jank" unsettled (debug swipe 37.5%) |
| ONB-01 | Branded splash -> onboarding or Home | SATISFIED | Truths 1-2 |
| ONB-02 | Pick en/hi on first run | SATISFIED | LanguageSwitchTest on API 33 (LocaleManager path). The API 26-32 AppCompat path is NOT verified on hardware |
| ONB-03 | 3 intro pages, Next / Get started | SATISFIED | OnboardingFlowTest, walk |
| ONB-04 | First name at least 2 letters, optional gender | SATISFIED | NameRulesTest, walk |
| ONB-05 | Birth date, optional time, "I don't know" | SATISFIED | BirthRulesTest, OnboardingFlowTest, walk |
| ONB-06 | Once only; survives restarts; excluded from backup | SATISFIED | Relaunch walk, repository test, aapt2 |
| HOME-01 | Time-of-day greeting with name | SATISFIED | GreetingTest, walk |
| HOME-02 | Hero and grid; non-M1 tiles say coming soon | SATISFIED | HomeTest |
| HOME-04 | Back on Home asks to confirm | SATISFIED | HomeTest, walk |
| SCAN-01 | Left/right + good + 4 bad tips | SATISFIED | HandSelectTest, walk |

No orphaned requirements: every ID mapped to Phase 3 in REQUIREMENTS.md is claimed by a plan.

### Anti-Patterns Found

None blocking. There are no TBD, FIXME, XXX, TODO or HACK markers in the 96 files changed since 98cbbc3. Intentional stubs: ScanPlaceholderScreen (Phase 4 replaces it) and the coming-soon tiles (D-12).

### Human Verification Required

1. **CHECKPOINT: release fresh install + cold start.** The user taps Install on the phone while `bash tools/device-lock.sh bash tools/gradlew.sh :app:installRelease` runs (package `app.palm.dev`). Then onboard once and run `tools/cold-start.sh` without and with `--compile`. Expected: the compiled median is under 2000 ms.
2. **Release-build jank and motion.** Run `check-jank idle 10`, `check-jank swipe 3` and `check-motion off/on/saver` with `PKG=app.palm.dev`. Expected: at most 5% deadline-missed. The debug swipe result (37.5%) is a real warning sign.
3. **03-05 Task 3 sign-off.** Screenshots vs Figma, the sky on the phone, and the A7/A10 gate choices. It is blocked by gap 2.
4. **Hindi after restart on the reference phone.** A quick scripted re-check.

### Gaps Summary

The product code for the phase goal is present, wired and proven on the reference phone. The flow, persistence, Home, hand choice, the sky's motion/static behaviour and the scan opt-out all pass, and the 9/9 instrumented and 80/80 JVM tests are green. What is missing is the 03-05 phase-gate work: Task 2 was committed only as scripts (41580e2, 44c4cce). Its on-device outputs were never produced. There is no STATE.md Hindi review flag or gate record (gap 1), and no screenshot set for the user's Figma review (gap 2). Both close by finishing 03-05 Task 2 on the release build after the fresh-install checkpoint. The release cold start and the release jank numbers are still unmeasured. The debug intro-swipe result (37.5% deadline-missed) means SC4 "without visible jank" may turn into a real gap once it is measured on the release build.

Device state at the end: the phone dropped off adb (wireless) during a repeat of the intro swipe check, after all setting changes had already been restored. animator_duration_scale was 1.0, low_power 0, the battery override reset and font_scale 1.0 (never changed), all checked before the drop. Both `app.palm.dev.debug` and `app.palm.dev.debug.test` are still installed. The debug app was left with a cleared profile, at the intro (onboarding incomplete).

---

_Verified: 2026-11-27T13:05:00Z_
_Verifier: Claude (gsd-verifier)_
