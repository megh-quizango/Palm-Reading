---
phase: 03-client-shell
plan: 03
subsystem: android-client
status: complete
tags: [android, compose, onboarding, datepicker, timepicker, horizontalpager, datastore, validation]
requires:
  - phase: 03-01
    provides: ProfileRepository (saveName, saveBirth, completeOnboarding), routes Language/Intro/Name/Birth/Home + replaceAll, LanguageViewModel, FakeProfileRepository, tools/ui.sh, tools/device-lock.sh
  - phase: 03-02
    provides: illus_constellation_hand, illus_zodiac_wheel, illus_guide_orb, ic_alert, ic_chevron_down, Phase 3 type scale, NocturneBackground at the root
provides:
  - IntroScreen (3-page HorizontalPager, PageIndicator, Next / Get started)
  - NameScreen + NameViewModel + NameRules (checkName, NameCheck, NAME_MAX = 30)
  - BirthScreen + BirthViewModel + BirthRules (MIN_BIRTH_DATE, isValidBirthDate, utcMillisToDate, dateToUtcMillis)
  - NocturneTextField, NocturneSelectField, nocturneTextFieldColors, NocturneChoiceTile
  - strings_onboarding.xml (en, hi)
  - LanguageViewModel.continueTo(next) without the completeOnboarding parameter; only the birth step completes onboarding
affects: [03-04, 03-05]
tech-stack:
  added: []
  patterns: [code-point allow-list name rule with marks only after letters, picker millis converted only via ZoneOffset.UTC, error revealed only on focus loss or Done, IOException -> saveFailed keeps the user on the step, prefill from profile.first() at ViewModel init]
key-files:
  created:
    - android/app/src/main/java/app/palm/feature/onboarding/{IntroScreen,NameScreen,NameViewModel,NameRules,BirthScreen,BirthViewModel,BirthRules}.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/{TextFields,ChoiceTile}.kt
    - android/app/src/main/res/values/strings_onboarding.xml
    - android/app/src/main/res/values-hi/strings_onboarding.xml
    - android/app/src/test/java/app/palm/feature/onboarding/{NameRulesTest,BirthRulesTest,NameViewModelTest,BirthViewModelTest}.kt
    - android/app/src/androidTest/java/app/palm/feature/onboarding/OnboardingFlowTest.kt
  modified:
    - android/app/src/main/java/app/palm/feature/onboarding/OnboardingEntries.kt
    - android/app/src/main/java/app/palm/feature/onboarding/LanguageViewModel.kt
    - android/app/src/test/java/app/palm/feature/onboarding/LanguageViewModelTest.kt
key-decisions:
  - "Only the birth step completes onboarding; LanguageViewModel.continueTo lost its completeOnboarding flag and Language now pushes Intro"
  - "The time picker sits in a BasicAlertDialog, not a material3 time-picker dialog wrapper, to avoid an experimental API"
  - "Back in the instrumented test uses Instrumentation.sendKeyDownUpSync(KEYCODE_BACK), because the pressBack helper is only on the androidTest runtime classpath (no dependency added)"
requirements-completed: [ONB-03, ONB-04, ONB-05, ONB-06]
coverage:
  - id: D1
    description: "Language -> 3 intro pages (Next and swipe) -> name + gender -> birth date -> Home, with the profile saved"
    requirement: ONB-03
    verification:
      - kind: automated_ui
        ref: "OnboardingFlowTest#firstRunReachesHomeWithProfileSaved (Redmi Note 10T, API 33)"
        status: pass
    human_judgment: false
  - id: D2
    description: "First-name rule (2+ Unicode letters incl. Devanagari marks, single separators, max 30) and optional toggleable gender"
    requirement: ONB-04
    verification:
      - kind: unit
        ref: "NameRulesTest, NameViewModelTest"
        status: pass
    human_judgment: false
  - id: D3
    description: "Birth date 1900-01-01..today without a day shift, optional time, 'I don't know my birth time' disables the time field and shows the note"
    requirement: ONB-05
    verification:
      - kind: unit
        ref: "BirthRulesTest (America/Sao_Paulo, Asia/Kolkata), BirthViewModelTest"
        status: pass
      - kind: automated_ui
        ref: "OnboardingFlowTest#timeUnknownAndBackWalkTheStepsInOrder"
        status: pass
    human_judgment: false
  - id: D4
    description: "Save as you go: relaunch after an interrupted onboarding starts at Language and the name step shows the saved name; Back walks Birth -> Name -> Intro -> Language"
    requirement: ONB-06
    verification:
      - kind: automated_ui
        ref: "tools/ui.sh interrupted-onboarding run (Task 2 verify 3), exit 0"
        status: pass
      - kind: automated_ui
        ref: "OnboardingFlowTest#timeUnknownAndBackWalkTheStepsInOrder"
        status: pass
    human_judgment: false
  - id: D5
    description: "Large font scale and open keyboard keep Continue reachable; intro art, pager motion and picker colours match UI-SPEC"
    verification: []
    human_judgment: true
    rationale: "Visual and overflow checks are not automated in this plan; the 03-05 hardening pass covers font scale on the device"
metrics:
  duration: 135min
  completed: 2026-11-27
  tasks: 2
  files: 20
estimate:
  tokens: 80000
actuals:
  tokens: 24300
  tasks: 2
  commits: 3
plan_head_before: 522ba7d14e925e76620b9e89718eaf2bf573a7d4
plan_head_after: e2d36befc6b6814bb247339e5980bc9a0242df84
---

# Phase 3 Plan 03: Onboarding intro pager, name + gender, birth date/time Summary

**A new user now goes from Language through a 3-page intro pager to name and optional gender, then birth date with an optional time or "I don't know". Each step is saved as they go, and they land on Home. On the Redmi Note 10T, a relaunch after an interrupted onboarding brings back the saved name, and Back walks the steps in order.**

## Performance

- Duration: about 135 min of wall clock, from 2026-11-27 14:09 to 16:24 IST, over four executor resumes. Most of that time the phone was blocked by MIUI install and pop-up restrictions. The code itself took a small part of it.
- Tasks: 2 of 2
- Files: 20 (16 Android sources and tests, 2 string files, the red-evidence JSON and this summary)

## Task Commits

| Task | Name | Commit | Notes |
|------|------|--------|-------|
| 1 | Tracer: Language -> Intro -> Name -> Birth -> Home | fbe29cf (feat) | NameRulesTest and LanguageViewModelTest green |
| 2 (RED) | Failing tests for prefill, error reveal, time unknown, save failures | a8e3e33 (test) | 6 assertion failures, recorded in 03-03-red-evidence.json |
| 2 (GREEN) | Birth time / I don't know, error reveal, save failures, prefill | e2d36be (feat) | onboarding.* 42 tests green, full unit suite 77/77 |

## Verification

- `bash tools/gradlew.sh :app:testDebugUnitTest`: 77/77 at e2d36be, with no 03-01, 03-02 or Phase 1 regressions.
- `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.feature.onboarding.*'`: green.
- OnboardingFlowTest on the Redmi Note 10T (serial 8pucytpny5v8ijkj, API 33), run through `tools/device-lock.sh` with `ADB_SERIAL` set and `-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true`: BUILD SUCCESSFUL. The result XML has tests="2", failures="0" and errors="0" (`firstRunReachesHomeWithProfileSaved` 5.3 s, `timeUnknownAndBackWalkTheStepsInOrder` 5.1 s). The tests="0" guard passed.
- The interrupted-onboarding `tools/ui.sh` run exited 0: after the kill, the relaunch started at Language and the name step showed "Asha".
- All acceptance greps pass, for both tasks. That includes `HorizontalPager`, `NON_SPACING_MARK`, `ZoneOffset.UTC`, `replaceAll(Home)`, `add(Intro)`, equal en/hi string counts of 30 or more, at least 13 NameRulesTest assertions, `America/Sao_Paulo`, `BasicAlertDialog`, `Role.Checkbox`, `revealError`, `saveFailed` and `KEYCODE_BACK`. The negative grep found no `Log.` calls in feature/onboarding.
- Tracer feedback gate (interactive, end-of-phase, automated verify): it passed and the plan expanded.

## Decisions Made

- Only the birth step completes onboarding. Language Continue pushes Intro, and Birth Continue calls `replaceAll(Home)`.
- The time picker uses `BasicAlertDialog` rather than an experimental time-picker dialog wrapper.
- Back in the instrumented test uses `sendKeyDownUpSync(KEYCODE_BACK)`, so no dependency was added (D-18).

## Deviations from Plan

None in the code. Process notes:
- The device was the Redmi Note 10T 5G (M2103K19I, API 33), not the Redmi named in the plan. It is API 33, so it exercises the API 33+ LocaleManager language path. The API 26-32 appcompat back-compat path was not verified on hardware.
- The previous executors' work-in-progress (the Task 2 green edits) was kept unchanged and committed as e2d36be.
- Every Gradle connected run passed `leaveApksInstalledAfterRun=true`. On MIUI a fresh install needs a tap on the phone, so neither package was ever uninstalled. Profile state was reset with `pm clear` and `tools/ui.sh reset` instead.

## Issues Encountered

- MIUI blocked adb installs until the user turned on "USB debugging (Security settings)".
- MIUI then stopped the instrumented activity launch (MIUIOP 10021, "Display pop-up windows while running in the background"), and OnboardingFlowTest hung. After the user set that permission to Allow, the run took 18 s and passed.

## Device State

This executor changed no phone settings. Checked at the end: window and transition animation scales 1.0, system font_scale 1.0, low_power 0, battery not unplugged (USB powered: true). Both app.palm.dev.debug and app.palm.dev.debug.test are still installed. The vivo and the older Redmi were not touched.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-03-09 is covered by the checkName allow-list and the 30-character cap. T-03-10 is covered by selectableDates, isValidBirthDate and UTC-only conversion. For T-03-11, routes stay data objects and nothing is logged. T-03-SC: no new artifacts were added.

## Next Phase Readiness

The first-run journey (ROADMAP Phase 3 criterion 1) holds on the reference phone. 03-04 (Home hub, hand picker) and 03-05 (hardening: font scale, battery saver, release build) can build on these components and strings.

## Self-Check: PASSED

- All created files listed above exist.
- Commits fbe29cf, a8e3e33 and e2d36be are ancestors of HEAD. plan_head_before..HEAD counts 3 commits.
