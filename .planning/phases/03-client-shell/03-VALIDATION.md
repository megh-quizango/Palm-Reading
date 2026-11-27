---
phase: "3"
slug: "client-shell"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: true
wave_0_complete: false
created: "2026-11-27"
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Source: `03-RESEARCH.md` § Validation Architecture.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4.13.2 (JVM unit) + androidx.test runner 1.7.0 / ext-junit 1.3.0 (instrumented, on the Redmi) + Compose ui-test-junit4 1.12.1 (BOM); Turbine + kotlinx-coroutines-test for ViewModels |
| **Config file** | `android/app/build.gradle.kts` (`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`); test deps added in Wave 0 |
| **Quick run command** | `bash tools/gradlew.sh :app:testDebugUnitTest` |
| **Full suite command** | `bash tools/gradlew.sh :app:testDebugUnitTest :app:connectedDebugAndroidTest` (Redmi M2006C3MII, API 30, attached) |
| **Estimated runtime** | ~60 s quick (warm Gradle daemon); ~5 min full incl. instrumented (estimate, measure in Wave 0) |

---

## Sampling Rate

- **After every task commit:** Run `bash tools/gradlew.sh :app:testDebugUnitTest`
- **After every plan wave:** Run `bash tools/gradlew.sh :app:testDebugUnitTest :app:connectedDebugAndroidTest` on the Redmi
- **Before `/gsd-verify-work`:** Full suite green + `tools/cold-start.sh` + `tools/check-jank.sh` + `tools/check-motion.sh off` / `on` on the release build + ARTEMIS/adb walk of S01-S07 in en and hi
- **Max feedback latency:** 60 seconds (unit suite)

---

## Per-Task Verification Map

Task IDs are `{plan}-T{n}` (task number inside the plan). Every phone command runs inside `tools/device-lock.sh` (created in 03-01 Task 2) because 03-03 and 03-04 share the one phone in wave 2. Instrumented runs are also checked for `tests="0"` (Phase 1: a refused install reports success with no tests).

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 03-01-T2 | 03-01 | 1 | ONB-01, ONB-02, ONB-06 | T-03-03 | Unreadable profile routes to Language | device script (tracer) | `bash tools/device-lock.sh bash -c 'bash tools/install-debug.sh && bash tools/ui.sh reset && bash tools/ui.sh launch && ... tap "हिन्दी" ... expect "\[AppName\]" ... && bash tools/ui.sh launch && ... && bash tools/ui.sh absent "अपनी भाषा चुनें\|Choose your language"'` (full text in 03-01 Task 2) — first run, Hindi switch in place, Home, relaunch straight to Home | ❌ W0 (tools/ui.sh, tools/device-lock.sh created by this task) | ⬜ pending |
| 03-01-T3 | 03-01 | 1 | ONB-01, ONB-06, HOME-01 | T-03-03 | Corrupt file / IOException read as an empty profile | unit | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.data.ProfileRepositoryTest' --tests 'app.palm.core.navigation.StartRouteTest' --tests 'app.palm.MainViewModelTest' --tests 'app.palm.feature.onboarding.LanguageViewModelTest' --tests 'app.palm.feature.home.GreetingTest'` — disk round trip through a second store, start route, initial language, D-11 windows | ❌ W0 | ⬜ pending |
| 03-01-T3 | 03-01 | 1 | ONB-02 | — | N/A | instrumented (API 30 AppCompat path) | `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.onboarding.LanguageSwitchTest` — Hindi visible after the recreate, stored language HI | ❌ W0 | ⬜ pending |
| 03-01-T3 | 03-01 | 1 | ONB-06 | T-03-01 | allowBackup=false, extraction rules, localeConfig, autoStoreLocales in the built APK | static (aapt2) | `"$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/aapt2" dump xmltree --file AndroidManifest.xml android/app/build/outputs/apk/debug/app-debug.apk` piped to greps for `allowBackup(0x01010280)=false`, `dataExtractionRules`, `localeConfig`, `autoStoreLocales` | ✅ (Phase 1 rules) | ⬜ pending |
| 03-01-T3 | 03-01 | 1 | ONB-01..06 (VMs) | T-03-02 | No logging of profile values | static | `test -f .../core/data/ProfileRepository.kt && ! grep -rnE 'android\.util\.Log\|Log\.[dviwe]\(' .../core/data .../feature .../MainViewModel.kt` | ❌ W0 | ⬜ pending |
| 03-01-T3 | 03-01 | 1 | ONB-01 (D-01 Back on Language) | — | N/A | device script | `bash tools/device-lock.sh bash -c '... expect "Choose your language\|अपनी भाषा चुनें" && bash tools/ui.sh back && bash tools/ui.sh absent "^activity: app\.palm"'` | ❌ W0 | ⬜ pending |
| 03-02-T1 | 03-02 | 1 | FND-03 | T-03-06 | Sky timing helpers and rest-state constants (pure functions; the no-clock / static claim is proven on the device in 03-04-T1 and 03-05-T2) | unit | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.designsystem.NocturneSkyTest'` — counts, alpha floor 0.25, one sparkle and one shooting star at a time, 8-12 s gaps, rest state | ❌ W0 | ⬜ pending |
| 03-02-T2 | 03-02 | 1 | FND-03 (type scale, D-07) | — | N/A | unit (Python) | `python -I tools/test_gen_tokens.py`; `python -I tools/gen_tokens.py --check` | ✅ | ⬜ pending |
| 03-02-T3 | 03-02 | 1 | FND-03 / SCAN-01 / HOME-02 art | T-03-07 | Own art only (clean room) | build + static | `bash tools/gradlew.sh :app:assembleDebug`; presence of the 20 drawables and total size < 150 KB | ❌ W0 | ⬜ pending |
| 03-03-T1 | 03-03 | 2 | ONB-03, ONB-04 | T-03-09 | Name allow-list and 30-char cap before the DataStore write | unit + instrumented (tracer) | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.onboarding.NameRulesTest' --tests 'app.palm.feature.onboarding.LanguageViewModelTest'`; `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.onboarding.OnboardingFlowTest` | ❌ W0 | ⬜ pending |
| 03-03-T2 | 03-03 | 2 | ONB-04, ONB-05, ONB-06 | T-03-10, T-03-11 | Birth bounds 1900-01-01..today, UTC-only conversion, no profile values in logs or keys | unit + instrumented + device script | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.onboarding.*'`; OnboardingFlowTest as above; interrupted-onboarding prefill via `tools/ui.sh` (full text in 03-03 Task 2) | ❌ W0 | ⬜ pending |
| 03-04-T1 | 03-04 | 2 | SCAN-01, FND-03 (opt-out, 03-02 sky on device) | T-03-12, T-03-06 | Only HandSide in the Scan key; sky static (no clock) with animations off and animating with them on | instrumented (tracer) + device script | `bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.handselect.HandSelectTest` — Right default, 1 good + 4 bad tips, Scan shows the hand, Scan pixel = bg.camera; `bash tools/device-lock.sh bash -c 'bash tools/install-debug.sh && bash tools/ui.sh reset && bash tools/ui.sh launch && bash tools/ui.sh expect "Choose your language\|अपनी भाषा चुनें" && bash tools/check-motion.sh off && bash tools/check-motion.sh on'` | ❌ W0 / ✅ check-motion.sh | ⬜ pending |
| 03-04-T2 | 03-04 | 2 | HOME-01, HOME-02 | — | N/A | instrumented | `... -Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.home.HomeTest` — greeting with name, 7 tiles, Palm tile to HandSelect, Tarot sheet over Home | ❌ W0 | ⬜ pending |
| 03-04-T3 | 03-04 | 2 | HOME-04, D-03 | T-03-13 | Showcase absent from the release APK | instrumented + static | `... class=app.palm.feature.home.HomeTest,app.palm.feature.handselect.HandSelectTest`; `bash tools/gradlew.sh :app:assembleRelease :app:assembleDebug && ! unzip -p android/app/build/outputs/apk/release/app-release-unsigned.apk 'classes*.dex' \| grep -aq 'Lapp/palm/showcase/'` | ❌ W0 | ⬜ pending |
| 03-05-T1 | 03-05 | 3 | ONB-06 / D-16 (cold start) | T-03-15 | Debug-signed release only for timing (REL-03) | device script (tracer) | `bash tools/device-lock.sh bash -c 'BUDGET_MS=100000 PKG=app.palm.dev bash tools/cold-start.sh && PKG=app.palm.dev bash tools/cold-start.sh --compile'` — median of 5 < 2000 ms | ❌ W0 (tools/cold-start.sh) | ⬜ pending |
| 03-05-T2 | 03-05 | 3 | FND-03 | T-03-06, T-03-16, T-03-17 | Sky static (no clock) with animations off and under battery saver on the release build; release APK without showcase or cleartext; phone settings (animator scale, low_power, battery override, font scale) restored | device script + static | `PKG=app.palm.dev bash tools/check-motion.sh off` / `on` / `saver`; `PKG=app.palm.dev bash tools/check-jank.sh idle 10` and `swipe 3`; Scan hash equality + pixel `#05040E`; full `:app:connectedDebugAndroidTest`; release APK dex/manifest checks (full text in 03-05 Task 2) | ✅ check-motion.sh / ❌ W0 check-jank.sh | ⬜ pending |
| 03-05-T3 | 03-05 | 3 | all (visual) | — | N/A | manual (blocking-human checkpoint) | — (screenshots in `tools/adb/shots/phase3-*.png`) | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Catalog + `android/app/build.gradle.kts`: turbine, kotlinx-coroutines-test, ui-test-junit4 (androidTest, with `platform(bom)`), ui-test-manifest (debug) — after the dependency-download consent checkpoint (D-18) — 03-01 Task 1 + Task 2
- [ ] `android/app/src/test/java/app/palm/testing/MainDispatcherRule.kt`, `FakeProfileRepository.kt`, `FakeAppLocale.kt` — shared fixtures — 03-01 Task 3
- [ ] Unit tests: `StartRouteTest`, `ProfileRepositoryTest`, `MainViewModelTest`, `LanguageViewModelTest`, `GreetingTest` (03-01 Task 3); `NocturneSkyTest` (03-02 Task 1); `NameRulesTest` (03-03 Task 1); `BirthRulesTest`, `NameViewModelTest`, `BirthViewModelTest` (03-03 Task 2)
- [ ] Instrumented: `LanguageSwitchTest` (03-01 Task 3), `OnboardingFlowTest` (03-03), `HandSelectTest` (03-04 Task 1), `HomeTest` (03-04 Tasks 2-3) — `createEmptyComposeRule()` + `ActivityScenario` with the real `PalmApp` Hilt graph (no hilt-android-testing); the profile is reset and seeded through the main-source `ProfileEntryPoint` (the Hilt singleton DataStore caches in memory, so deleting the file is not enough) and the app locale is set on the main thread before launch
- [ ] `tools/ui.sh`, `tools/device-lock.sh` (03-01 Task 2); `tools/cold-start.sh` (03-05 Task 1); `tools/check-jank.sh`, `PKG` and the `saver` (battery saver) mode in `tools/check-motion.sh` (03-05 Task 2)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| On-device visual QA of S01-S07 against UI-SPEC / Figma (cosmic sky layers, typography, Devanagari rendering, illustrations) | FND-03, ONB-01..05, HOME-01, HOME-02, SCAN-01 | Visual fidelity and look cannot be asserted by unit / Compose tests; Figma not available in every session | ARTEMIS or adb screenshot walk of S01-S07 in `en` and `hi` on the Redmi; compare with `03-UI-SPEC.md` and Figma frames |
| Splash hand-off feel (no flash, branded splash, correct route on first run vs returning) | ONB-01 | Timing / visual transition on real hardware | Cold-launch after `pm clear` (first run, expect Language) and after onboarding (expect Home); watch for white flash |
| Cold-start timing review | D-16 (PRD §10 < 2 s to first frame) | Needs a release build on the physical device; numbers vary by device state | Run `tools/cold-start.sh` on the release build, record median of N launches, confirm < 2000 ms |
| Background motion smoothness / jank by eye | FND-03 | Script gives frame stats; perceived smoothness on a low-end phone needs a look | With animations on, watch Home and onboarding sky for stutter; toggle system animations off and confirm static sky |
| Locale switch on API 33+ (LocaleManager path) | ONB-02 | No API 33+ device available; Redmi is API 30 (AppCompat compat path only) | Optional: run `LanguageSwitchTest` on an API 33+ emulator later; otherwise note as unverified |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (the two checkpoints, 03-01 Task 1 consent and 03-05 Task 3 visual sign-off, are human gates)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s (unit suite; instrumented runs are per-task, not per-commit)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
