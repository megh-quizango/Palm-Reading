---
phase: 03-client-shell
verified: 2026-11-27T15:04:33Z
status: human_needed
score: 10/12 must-haves verified
covered_files: [".planning/phases/03-client-shell/03-01-PLAN.md", ".planning/phases/03-client-shell/03-01-SUMMARY.md", ".planning/phases/03-client-shell/03-02-PLAN.md", ".planning/phases/03-client-shell/03-02-SUMMARY.md", ".planning/phases/03-client-shell/03-03-PLAN.md", ".planning/phases/03-client-shell/03-03-SUMMARY.md", ".planning/phases/03-client-shell/03-04-PLAN.md", ".planning/phases/03-client-shell/03-04-SUMMARY.md", ".planning/phases/03-client-shell/03-05-PLAN.md", ".planning/phases/03-client-shell/03-05-SUMMARY.md", "android/app/build.gradle.kts", "android/app/src/androidTest/java/app/palm/feature/handselect/HandSelectTest.kt", "android/app/src/androidTest/java/app/palm/feature/home/HomeTest.kt", "android/app/src/androidTest/java/app/palm/feature/onboarding/LanguageSwitchTest.kt", "android/app/src/androidTest/java/app/palm/feature/onboarding/OnboardingFlowTest.kt", "android/app/src/debug/java/app/palm/debug/DebugEntries.kt", "android/app/src/debug/java/app/palm/showcase/BackendStatus.kt", "android/app/src/debug/java/app/palm/showcase/ShowcaseScreen.kt", "android/app/src/main/AndroidManifest.xml", "android/app/src/main/java/app/palm/MainActivity.kt", "android/app/src/main/java/app/palm/MainViewModel.kt", "android/app/src/main/java/app/palm/core/data/ProfileRepository.kt", "android/app/src/main/java/app/palm/core/designsystem/Effects.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneBackground.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneTheme.kt", "android/app/src/main/java/app/palm/core/designsystem/NocturneTokens.kt", "android/app/src/main/java/app/palm/core/designsystem/components/Buttons.kt", "android/app/src/main/java/app/palm/core/designsystem/components/Chips.kt", "android/app/src/main/java/app/palm/core/designsystem/components/ChoiceTile.kt", "android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt", "android/app/src/main/java/app/palm/core/designsystem/components/TextFields.kt", "android/app/src/main/java/app/palm/core/designsystem/components/TopBar.kt", "android/app/src/main/java/app/palm/core/locale/AppLocale.kt", "android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt", "android/app/src/main/java/app/palm/core/navigation/BottomSheetSceneStrategy.kt", "android/app/src/main/java/app/palm/core/navigation/Routes.kt", "android/app/src/main/java/app/palm/feature/handselect/HandSelectEntries.kt", "android/app/src/main/java/app/palm/feature/handselect/HandSelectScreen.kt", "android/app/src/main/java/app/palm/feature/handselect/ScanPlaceholderScreen.kt", "android/app/src/main/java/app/palm/feature/home/ComingSoonSheet.kt", "android/app/src/main/java/app/palm/feature/home/ExitDialog.kt", "android/app/src/main/java/app/palm/feature/home/Greeting.kt", "android/app/src/main/java/app/palm/feature/home/HeroCard.kt", "android/app/src/main/java/app/palm/feature/home/HomeEntries.kt", "android/app/src/main/java/app/palm/feature/home/HomeScreen.kt", "android/app/src/main/java/app/palm/feature/home/HomeViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthRules.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/BirthViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/IntroScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/LanguageScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/LanguageViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameRules.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameScreen.kt", "android/app/src/main/java/app/palm/feature/onboarding/NameViewModel.kt", "android/app/src/main/java/app/palm/feature/onboarding/OnboardingEntries.kt", "android/app/src/main/res/drawable/hero_orbit.xml", "android/app/src/main/res/drawable/ic_alert.xml", "android/app/src/main/res/drawable/ic_arrow.xml", "android/app/src/main/res/drawable/ic_back.xml", "android/app/src/main/res/drawable/ic_check.xml", "android/app/src/main/res/drawable/ic_chevron_down.xml", "android/app/src/main/res/drawable/ic_close.xml", "android/app/src/main/res/drawable/ic_sparkle.xml", "android/app/src/main/res/drawable/illus_constellation_hand.xml", "android/app/src/main/res/drawable/illus_guide_orb.xml", "android/app/src/main/res/drawable/illus_heart_orbit.xml", "android/app/src/main/res/drawable/illus_horoscope_star.xml", "android/app/src/main/res/drawable/illus_sun_medallion.xml", "android/app/src/main/res/drawable/illus_tarot_pair.xml", "android/app/src/main/res/drawable/illus_zodiac_wheel.xml", "android/app/src/main/res/drawable/tip_blurry_dark.xml", "android/app/src/main/res/drawable/tip_fingers_closed.xml", "android/app/src/main/res/drawable/tip_good.xml", "android/app/src/main/res/drawable/tip_jewellery_bent.xml", "android/app/src/main/res/drawable/tip_too_far.xml", "android/app/src/main/res/values-hi/strings.xml", "android/app/src/main/res/values-hi/strings_home.xml", "android/app/src/main/res/values-hi/strings_onboarding.xml", "android/app/src/main/res/values/strings.xml", "android/app/src/main/res/values/strings_home.xml", "android/app/src/main/res/values/strings_onboarding.xml", "android/app/src/main/res/values/themes.xml", "android/app/src/main/res/xml/locale_config.xml", "android/app/src/release/java/app/palm/debug/DebugEntries.kt", "android/app/src/test/java/app/palm/MainViewModelTest.kt", "android/app/src/test/java/app/palm/core/data/ProfileRepositoryTest.kt", "android/app/src/test/java/app/palm/core/designsystem/NocturneSkyTest.kt", "android/app/src/test/java/app/palm/core/navigation/PushFromTest.kt", "android/app/src/test/java/app/palm/core/navigation/StartRouteTest.kt", "android/app/src/test/java/app/palm/feature/home/GreetingTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/BirthRulesTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/BirthViewModelTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/LanguageViewModelTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/NameRulesTest.kt", "android/app/src/test/java/app/palm/feature/onboarding/NameViewModelTest.kt", "android/app/src/test/java/app/palm/testing/FakeAppLocale.kt", "android/app/src/test/java/app/palm/testing/FakeProfileRepository.kt", "android/app/src/test/java/app/palm/testing/MainDispatcherRule.kt", "android/gradle/libs.versions.toml", "tools/adb/ui.py", "tools/check-jank.sh", "tools/check-motion.sh", "tools/cold-start.sh", "tools/device-lock.sh", "tools/test_gen_tokens.py", "tools/ui.sh"]
covered_digest: "v3:sha256:abc553e1e92d39c5a417517ff74322ece3caccf2809706dcf5823e831e51ecd0"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 8/12
  gaps_closed:
    - "STATE.md flags the Claude-written Hindi strings for native-speaker review before release and records the Phase 3 gate results (03-05 must-have; D-06)"
    - "The en/hi screenshot walk of S01-S07 (font scale 1.0 and 2.0, Hindi Home at 1.3) exists for the user's Figma sign-off (03-05 must-have; feeds Task 3)"
  gaps_remaining: []
  regressions: []
human_verification:
  - test: "Hardware performance gate, next phone session (reference phone, release build app.palm.dev): onboard once, then BUDGET_MS=100000 PKG=app.palm.dev bash tools/cold-start.sh and PKG=app.palm.dev bash tools/cold-start.sh --compile"
    expected: "The profile-compiled median of 5 am start -W TotalTime runs is under 2000 ms, with the fresh-install median recorded beside it (ROADMAP SC2, D-16)"
    why_human: "Only the x86_64 emulator was available. It gave 500 ms fresh and 495 ms compiled, which says nothing about a mid/low-end ARM phone. Hardware gate deferred, not passed."
  - test: "Hardware jank gate, next phone session: PKG=app.palm.dev bash tools/check-jank.sh idle 10 on Home, then reset, Continue in English and PKG=app.palm.dev bash tools/check-jank.sh swipe 3 on the intro"
    expected: "At most 5% of frames miss the deadline in both windows (ROADMAP SC4 'without visible jank', A10)"
    why_human: "Emulator numbers (host GPU) are not a gate: Home idle 24/581 (4.13%), intro swipe 12/248 (4.84%). The only hardware number is the debug-build intro swipe on the Redmi, 105/280 (37.5%), so the release swipe on the phone is a real risk. If it is over 5%, it becomes a gap (intro pager)."
  - test: "03-05 Task 3: the user compares tools/adb/shots/phase3-en-* and phase3-hi-* (43 shots, emulator, git-ignored) with Figma S01-S07 (page '3 · Screens'), watches the sky move, and confirms the A7 cold-start gate (profile-compiled median) and the A10 jank threshold (at most 5% deadline-missed)"
    expected: "The user replies 'approved' or lists issues. The answer goes into 03-05-SUMMARY.md and STATE.md."
    why_human: "Visual fidelity against Figma, Devanagari rendering and the feel of the motion are judgement calls, and the gate choices are the user's decision."
---

# Phase 3: Client Shell Verification Report

**Phase Goal:** A user can open the app, onboard once, land on a Home hub over the animated cosmic background, and pick which hand to scan, with everything persisted across restarts.
**Verified:** 2026-11-27T15:04:33Z
**Status:** human_needed
**Re-verification:** Yes, after gap closure (previous: gaps_found, 8/12)
**Device:** Android emulator emulator-5554 only (AVD launcher_test, sdk_gphone64_x86_64, API 35, 1080x2400, 420 dpi), standing in for the reference phone on the functional checks. No physical phone was connected. Performance numbers from it are labelled "emulator, not a gate".

MVP-mode note: the goal is not in user-story form (`user-story.validate` is false), as 03-05-PLAN records. Verification went ahead goal-backward against the ROADMAP success criteria because the orchestrator asked for it.

## What changed since the previous report

- ec4dd89 added the STATE.md lines and 03-05-SUMMARY.md, and finished 03-05 Task 2 on the emulator. That commit also changed HeroOrb to draw-only transforms (instead of graphicsLayer) and made `tools/adb/ui.py` delete the old dump before each new one.
- cda7140 added `rasterizedBehind` (Effects.kt). HeroCard and FeatureCard now paint their static gradients into one cached bitmap.
- Because product code changed, I re-ran the unit suite, the instrumented suite and every functional check myself on the current HEAD build. I did not rely on the summary's numbers.

## User Flow Coverage

| Step | Expected | Evidence (this run, emulator, release build unless noted) | Status |
|------|----------|----------|--------|
| Open the app | Splash, then Language on a first run or Home when returning | After `pm clear` the app opened on "अपनी भाषा चुनें" / "Choose your language". Relaunches went straight to Home | VERIFIED |
| Onboard once | Language -> 3 intro pages -> name -> birth -> Home | Hindi walk: हिन्दी, हिंदी में आगे बढ़ें, आगे, आगे, शुरू करें, "Asha", 15 जनवरी 2000, तारीख़ सेट करें, मुझे अपने जन्म का समय नहीं पता (note "कोई बात नहीं..." shown), आगे बढ़ें -> "शुभ संध्या, Asha". English walk: English, Continue in English, Next, Next, Get started, "A" (Continue not enabled), "Asha", January 15 2000, Set date, Continue -> "Read your palm", "Asha". OnboardingFlowTest 2/2 and LanguageSwitchTest 1/1 pass | VERIFIED |
| Land on Home over the sky | Greeting, hero, grid, coming-soon, exit confirm, animated sky | HomeTest 4/4. On Home two hashes 2 s apart differ. check-motion on/off/saver all OK | VERIFIED (hardware jank is a human item) |
| Pick which hand | Left/Right, 1 good + 4 bad tips, the hand goes on to Scan | HandSelectTest 1/1. Hindi hand screen shows बायाँ हाथ / दायाँ हाथ, अच्छी फ़ोटो, इनसे बचें; Continue -> "स्कैनर अगले अपडेट में आएगा" | VERIFIED |
| Persisted across restarts | Force-stop and relaunch go straight to Home with language and name intact | After `am force-stop app.palm.dev` and a relaunch (TotalTime 462 ms), Home showed "शुभ संध्या, Asha" and "हथेली स्कैन करें", and the Language and Name screens were absent. ProfileRepositoryTest `profileSurvivesANewStoreInstance` passes | VERIFIED (this closes the earlier "Hindi after restart" human item) |

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC1: first launch splash -> language (UI switches) -> 3 intro pages -> name (at least 2 letters, optional gender) -> birth date with optional time or "I don't know" -> Home | VERIFIED | The Hindi and English walks above, plus 9/9 instrumented and 80/80 JVM tests (NameRules, BirthRules, ViewModel tests) |
| 2 | SC2a: kill and relaunch go splash -> Home with the profile intact, onboarding not shown again, profile excluded from backup | VERIFIED | Hindi force-stop relaunch above. aapt2 on the current release APK shows allowBackup=false, fullBackupContent=false, dataExtractionRules, localeConfig and no networkSecurityConfig |
| 3 | SC2b: cold start to first frame under 2 s on a release build on the reference phone | ? UNCERTAIN (human: hardware gate deferred) | Emulator, not a gate: fresh 447/500/552/478/507, median 500 ms. Compiled (dexopt `x86_64: [status=speed-profile]`) 495/479/472/514/547, median 495 ms. No hardware measurement exists |
| 4 | SC3: time-of-day greeting with name, hero, grid; non-M1 tiles open "coming soon"; Back asks before leaving | VERIFIED | HomeTest 4/4 (incl. `otherTileOpensComingSoonSheetOverHome`, `backOnHomeAsksBeforeLeaving`). The greeting is "शुभ संध्या" in the evening slot |
| 5 | SC4a: the layered sky animates, and is static with animations off and under battery saver | VERIFIED | Release: check-motion off OK (equal hashes), on OK (different), saver OK (equal; the rest hash matches the off hash). Settings were restored by the trap |
| 6 | SC4b: onboarding and Home show the sky without visible jank on the reference phone | ? UNCERTAIN (human: hardware gate deferred) | Emulator, not a gate: Home idle 10 s, 24/581 deadline-missed (4.13%). Intro swipe x3, 12/248 (4.84%). The only hardware data point is the debug intro swipe on the Redmi, 37.5% |
| 7 | SC4c: the scan route opts out of the sky | VERIFIED | On Scan, two hashes of rows 300..1400 taken 2 s apart are equal (48b9aaa3...), with pixels (4,800) and (1070,1800) = #05040E. Home hashes 2 s apart differ (control) |
| 8 | SC5: pick left or right from the hero, see 1 good + 4 bad tips, the hand goes on to the scanner | VERIFIED | HandSelectTest plus the Hindi walk |
| 9 | 03-05: release APK has no showcase classes and no network security config, and keeps allowBackup=false | VERIFIED | 0 `Lapp/palm/showcase/` matches in the current release dex; manifest as in truth 2 |
| 10 | 03-05: every Phase 3 instrumented test plus AppDatabaseTest passes together | VERIFIED | `connectedDebugAndroidTest` on emulator-5554 (result file `TEST-launcher_test(AVD) - 15.xml`): AppDatabaseTest 1, HandSelectTest 1, HomeTest 4, LanguageSwitchTest 1, OnboardingFlowTest 2. 9/9, 0 failures, no tests="0" |
| 11 | 03-05: STATE.md flags the Hindi strings for native-speaker review and records the gate results | VERIFIED (gap closed) | STATE.md line 146 is the native-speaker flag (D-06), line 147 covers the locale paths (API 33+ verified, API 26-32 unverified), and lines 129-130 and 148-149 hold the cold-start/jank decisions, the emulator-only numbers, the hardware deferral and the pending Task 3 |
| 12 | 03-05: the en/hi S01-S07 screenshot set exists for the Figma sign-off | VERIFIED (gap closed) | 43 phase3-*.png files (21 en, 22 hi, including fs2 and Hindi Home fs1.3), git-ignored. I opened phase3-hi-s06-home-fs2 (Devanagari is intact and the orb is hidden at the large font scale) and phase3-en-s07-hand-tips (Right selected, 1 good + 4 bad tips). font_scale = 1.0 = font_scale.orig. The approval itself is a human item |

**Score:** 10/12 truths verified (0 present but behavior-unverified; 2 uncertain, routed to the deferred hardware session)

### Required Artifacts

| Artifact | Status | Details |
|----------|--------|---------|
| tools/cold-start.sh | VERIFIED | Ran in this verification: PKG required, `sleep 12`, dexopt filter printed, median gate |
| tools/check-jank.sh | VERIFIED | Ran idle and swipe; deadline-missed metric, minimum frame count |
| tools/check-motion.sh | VERIFIED | `PKG="${PKG:-app.palm.dev.debug}"`, off/on/saver modes, restores low_power and battery in the trap |
| android/app/build.gradle.kts | VERIFIED | Release is debug-signed with the REL-03 shortcut comment; installRelease and `adb install -r` work |
| core/designsystem/Effects.kt `rasterizedBehind` | VERIFIED | `drawWithCache` builds one ImageBitmap per size and blits it in `onDrawBehind`; it is skipped for zero size. Used by HeroCard and FeatureCard (after `clip`, so clipped to the shape) |
| feature/home/HeroCard.kt HeroOrb | VERIFIED | `drawWithContent { rotate/scale }` reads the animation in the draw phase. The motion "on" check still sees change, and the "off"/"saver" checks see none |
| .planning/STATE.md (03-05 lines) | VERIFIED | See truth 11 |
| tools/adb/shots/phase3-*.png | VERIFIED (local, git-ignored) | 43 files |
| Earlier 03-01..03-04 artifacts | VERIFIED (regression) | Unchanged except the files above. All suites green |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| tools/cold-start.sh | build.gradle.kts release variant | `am start -S -W` TotalTime on app.palm.dev | WIRED (ran) |
| tools/check-jank.sh | NocturneBackground sky | gfxinfo "Frame deadline missed" on Home and the intro | WIRED (ran) |
| MainActivity / MainViewModel / AppNavDisplay | ProfileRepository | splash hold -> `startRoute` -> `replaceAll` | WIRED (relaunch goes to Home) |
| LanguageViewModel | AppLocale | `setApplicationLocales` | WIRED (Hindi UI switch and Hindi after restart) |
| AppNavDisplay | NocturneBackground | `animate` = top route `showsBackground` | WIRED (Scan static, Home animating) |
| HandSelectEntries | Scan(hand) | `pushFrom` | WIRED |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|----------|------|--------|-----------|--------|
| Home greeting | profile first name + language | DataStore via HomeViewModel | "शुभ संध्या, Asha" after a restart | FLOWING |
| Start route | onboardingComplete | DataStore first read | Home after a restart, Language after `pm clear` | FLOWING |
| Scan placeholder | hand | `Scan(hand)` nav key | HandSelectTest | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| JVM unit suite | `tools/gradlew.sh :app:testDebugUnitTest --rerun` | 80 tests in 14 classes, 0 failures | PASS |
| Instrumented suite | `:app:connectedDebugAndroidTest` (ADB_SERIAL=emulator-5554) | 9/9, no tests="0" | PASS |
| Hindi onboarding + restart | ui.sh walk, `am force-stop`, launch | Home in Hindi with "Asha", no Language/Name | PASS |
| Motion off / on / saver (release) | `PKG=app.palm.dev tools/check-motion.sh` | OK / OK / OK | PASS |
| Scan opt-out (release) | screen.py hash 300..1400 twice, pixels | equal, #05040E | PASS |
| Release APK contents | dex grep + aapt2 | clean | PASS |
| Cold start (release) | `tools/cold-start.sh` and `--compile` | 500 ms / 495 ms medians | EMULATOR, NOT A GATE |
| Jank Home idle 10 s (release) | `tools/check-jank.sh idle 10` | 24/581 (4.13%), p50 26 ms, p90 36 ms | EMULATOR, NOT A GATE |
| Jank intro swipe x3 (release) | `tools/check-jank.sh swipe 3` | 12/248 (4.84%), p50 26 ms, p90 34 ms | EMULATOR, NOT A GATE |

### Probe Execution

No `scripts/*/tests/probe-*.sh` exist and no plan declares any probe. Not applicable.

### Requirements Coverage

| Requirement | Description | Status | Evidence |
|-------------|-------------|--------|----------|
| FND-03 | Layered animated sky, static with animations off, absent on camera | NEEDS HUMAN (hardware jank only) | Motion and opt-out verified; "no visible jank" awaits the phone |
| ONB-01 | Branded splash -> onboarding or Home | SATISFIED | Truths 1-2 |
| ONB-02 | Pick en/hi on first run | SATISFIED | LanguageSwitchTest and the Hindi walk on API 35 (LocaleManager path). The API 26-32 AppCompat path is NOT verifiable on this emulator and has no device; STATE.md records it as unverified |
| ONB-03 | 3 intro pages, Next / Get started | SATISFIED | Both walks, OnboardingFlowTest |
| ONB-04 | First name of at least 2 letters, optional gender | SATISFIED | NameRulesTest; "A" left Continue disabled in the walk |
| ONB-05 | Birth date, optional time, "I don't know" | SATISFIED | Hindi walk showed the note; BirthRulesTest |
| ONB-06 | Once only; survives restarts; excluded from backup | SATISFIED | Restart walk, aapt2 |
| HOME-01 | Time-of-day greeting with name | SATISFIED | GreetingTest, walk |
| HOME-02 | Hero and grid; non-M1 tiles say coming soon | SATISFIED | HomeTest |
| HOME-04 | Back on Home asks to confirm | SATISFIED | HomeTest |
| SCAN-01 | Left/right + good + 4 bad tips | SATISFIED | HandSelectTest, walk |

No orphaned requirements.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| (changed files: Effects.kt, FeatureCard.kt, HeroCard.kt, ui.py, cold-start/check-jank/check-motion) | - | no TBD/FIXME/XXX/TODO/HACK | - | none |
| .planning/STATE.md | 26, 32-33 | "Current focus: Phase 3 (planned)" and "Last activity: Phase 3 planned" are stale | Info | Housekeeping only; the orchestrator updates the position at phase completion |

Intentional stubs: ScanPlaceholderScreen (Phase 4) and the coming-soon tiles (D-12).

### Human Verification Required

1. **Hardware cold-start gate (next phone session).** On the reference phone, install the release build, onboard once, then run `PKG=app.palm.dev bash tools/cold-start.sh` (with BUDGET_MS=100000) and `PKG=app.palm.dev bash tools/cold-start.sh --compile`. Expected: the compiled median is under 2000 ms. Emulator: 495 ms, not a gate.
2. **Hardware jank gate (next phone session).** Run `check-jank idle 10` on Home and `check-jank swipe 3` on the intro with PKG=app.palm.dev. Expected: at most 5% deadline-missed. Emulator: 4.13% / 4.84%, not a gate. The debug swipe on the Redmi was 37.5%, so this is the most likely place for a real gap.
3. **03-05 Task 3 sign-off.** The user compares the 43 phase3 screenshots with Figma S01-S07, watches the sky, and confirms the A7 and A10 gate choices.

### Gaps Summary

Both earlier gaps are closed. STATE.md now has the Hindi native-speaker flag, the locale-path note and the gate decision lines, and the 43-shot en/hi screenshot set exists. The "Hindi after restart" item is now verified on the emulator. No regressions came from the HeroOrb or rasterizedBehind changes: 80/80 JVM tests and 9/9 instrumented tests pass, and motion, saver and Scan opt-out are all OK on the current release build. What is left is not a code gap. It is the hardware performance gates (SC2 cold start, SC4 jank), deferred to the next phone session because x86 emulator numbers are not meaningful, and the user's Task 3 visual and gate sign-off.

Emulator state at the end: animator_duration_scale null, transition/window 1.0, low_power 0, no battery override, font_scale 1.0, as at the start. app.palm.dev (current HEAD release build) is onboarded in English as "Asha". The instrumented run uninstalled its debug and test packages afterwards, so only app.palm.dev is installed.

---

_Verified: 2026-11-27T15:04:33Z_
_Verifier: Claude (gsd-verifier)_
