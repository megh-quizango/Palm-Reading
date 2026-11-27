---
phase: 03-client-shell
plan: 01
subsystem: android-client
status: complete
tags: [android, compose, navigation3, datastore, appcompat, splash, locale, tracer]
requires: [01-01, 01-05]
provides:
  - ProfileRepository / DataStoreProfileRepository / createProfileStore / ProfileModule / ProfileEntryPoint
  - AppLocale / AppCompatLocale / LocaleModule / initialLanguage
  - Route keys (Splash, Language, Intro, Name, Birth, Home, HandSelect, Scan, ComingSoon), Feature, replaceAll, startRoute
  - MainViewModel.start, AppNavDisplay(start), onboardingEntries, homeEntries
  - LanguageScreen/LanguageViewModel, HomeScreen/HomeViewModel, DayPart/dayPart/greetingRes
  - Test fixtures MainDispatcherRule, FakeProfileRepository, FakeAppLocale
  - tools/ui.sh, tools/device-lock.sh
affects: [03-02, 03-03, 03-04, 03-05]
tech-stack:
  added: [androidx.appcompat 1.8.0, core-splashscreen 1.2.0, datastore-preferences 1.3.0, turbine 1.2.1, kotlinx-coroutines-test 1.11.0, compose ui-test-junit4/ui-test-manifest (BOM)]
  patterns: [activity-scoped start route held by the system splash, AppCompat per-app locale as source of truth, one root NocturneBackground behind NavDisplay, one entry-builder file per feature]
key-files:
  created:
    - android/app/src/main/java/app/palm/core/data/ProfileRepository.kt
    - android/app/src/main/java/app/palm/core/locale/AppLocale.kt
    - android/app/src/main/java/app/palm/core/navigation/Routes.kt
    - android/app/src/main/java/app/palm/MainViewModel.kt
    - android/app/src/main/java/app/palm/feature/onboarding/{OnboardingEntries,LanguageScreen,LanguageViewModel}.kt
    - android/app/src/main/java/app/palm/feature/home/{HomeEntries,HomeScreen,HomeViewModel,Greeting}.kt
    - android/app/src/main/res/values-hi/strings.xml
    - android/app/src/main/res/xml/locale_config.xml
    - android/app/src/test/java/app/palm/testing/{MainDispatcherRule,FakeProfileRepository,FakeAppLocale}.kt
    - android/app/src/test/java/app/palm/{MainViewModelTest,core/data/ProfileRepositoryTest,core/navigation/StartRouteTest,feature/onboarding/LanguageViewModelTest,feature/home/GreetingTest}.kt
    - android/app/src/androidTest/java/app/palm/feature/onboarding/LanguageSwitchTest.kt
    - tools/ui.sh
    - tools/device-lock.sh
  modified:
    - android/gradle/libs.versions.toml
    - android/app/build.gradle.kts
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/res/values/themes.xml
    - android/app/src/main/res/values/strings.xml
    - android/app/src/main/java/app/palm/MainActivity.kt
    - android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt
    - android/app/src/main/java/app/palm/core/designsystem/Effects.kt
    - android/app/src/main/java/app/palm/core/designsystem/NocturneTheme.kt (comment only)
key-decisions:
  - "Profile DataStore is built on OkioStorage (same preferences_pb format): the default FileStorage's File.renameTo fails on every write after the first on a Windows JVM test host"
  - "LanguageSwitchTest starts by tapping English: on API 33+ the locale set in @Before reaches the process asynchronously"
  - "The vivo V2225 (API 35) is the reference phone for this plan; the Redmi was also attached, so every device command ran with ADB_SERIAL=10BD7J2TAS000Z5"
metrics:
  duration: 16min
  completed: 2026-11-27
  tasks: 3
  files: 33
estimate:
  tokens: 100000
actuals:
  tokens: 16500
  tasks: 3
  commits: 2
plan_head_before: 98cbbc3922537c6340d0494a430163ea19a44d48
plan_head_after: 8457834f4f0f6b254228b7925eaa1fa78572542a
---

# Phase 3 Plan 01: Splash -> Language -> Home tracer Summary

The system splash waits for the first DataStore read. Language switches the whole UI to Hindi in place through the AppCompat per-app locale. Continue saves the profile and opens Home with a time-of-day greeting, and after a force-stop the app goes straight to Home in Hindi. All of this was proven on the vivo V2225 (API 35).

## Tasks

| Task | Name | Commit | Notes |
|------|------|--------|-------|
| 1 | Consent for new Gradle artifacts (D-18) | none | Approved by the user in chat on 2026-11-27 ("Approve all"), exactly the listed artifacts plus transitives |
| 2 | Tracer: first run -> Language -> Home, relaunch -> Home | 1c8d7ab | assembleDebug green; device flow passed end to end |
| 3 | Tests: profile, routing, language rules, Back on Language | 8457834 | 37 JVM tests green; LanguageSwitchTest tests=1 failures=0 |

## Verification (all run on the vivo V2225, serial 10BD7J2TAS000Z5)

- `bash tools/gradlew.sh :app:assembleDebug`: exit 0
- Tracer device flow: reset -> launch -> "Choose your language" / English / हिन्दी -> tap हिन्दी -> "अपनी भाषा चुनें" + "हिंदी में आगे बढ़ें" -> Continue -> "[AppName]" + Hindi greeting -> relaunch -> Home, no Language: exit 0. It ran again after the Task 3 storage change and passed. A screenshot shows "नमस्ते," and [AppName] over the star sky.
- Tracer feedback gate: verify re-ran end to end and passed, so the plan continued to Task 3.
- `:app:testDebugUnitTest` targeted (5 classes) and full: exit 0, 37 tests in total
- `connectedDebugAndroidTest` LanguageSwitchTest: exit 0, tests="1" failures="0", no tests="0" file. The final run started from a Hindi per-app locale, which is the case that failed before the fix.
- Back on Language: `ui.sh absent "^activity: app\.palm"` passed (the app left the foreground)
- aapt2 on the built APK: allowBackup=false, dataExtractionRules, localeConfig and autoStoreLocales are all present
- Acceptance greps: all pass. `ui.sh nope` exits 2, and the device lock is released after use.
- Cold start TotalTime (debug build, so only a rough figure): 1540 ms first launch, 801 ms relaunch

## Hardware note

The vivo is API 35, so the API 33+ LocaleManager path was verified on hardware. The API 26-32 AppCompat back-compat path (autoStoreLocales storage and the AppCompat recreate) was **not** verified on hardware in this plan.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `removeRange` is not accessible on NavBackStack**
- Found during: Task 2 (compile)
- Fix: `replaceAll` uses `subList(0, size - 1).clear()`, which keeps the same add-first semantics
- Commit: 1c8d7ab

**2. [Rule 3 - Blocking] DataStore JVM tests failed on Windows ("Unable to rename .tmp")**
- Found during: Task 3
- Issue: DataStore's FileStorage replaces the file with File.renameTo, which fails on Windows when the target exists. On the JVM test host every write after the first failed. `createWithPath` still routes to FileStorage on Android.
- Fix: `createProfileStore` builds the store with `OkioStorage(FileSystem.SYSTEM, PreferencesSerializer)` (datastore-core-okio and okio are approved transitive artifacts). The on-disk format is unchanged and it is still the one construction path. The test opens the first store on a `CoroutineScope` with its own `Job` and uses `cancelAndJoin`, because cancelling a `TestScope` never completes, so the file was never released.
- Verified on the phone afterwards with the tracer flow (including restart persistence) and LanguageSwitchTest
- Commit: 8457834

**3. [Rule 1 - Bug] LanguageSwitchTest was flaky when the app locale was already Hindi**
- Found during: Task 3 (the first run failed waiting for "Choose your language")
- Issue: on API 33+ `setApplicationLocales("en")` in @Before reaches the process asynchronously, and the activity started in Hindi without being recreated
- Fix: the test waits for the screen and taps English first, then checks Hindi -> English. The assertion was not weakened.
- Commit: 8457834

**4. [Plan inconsistency] Acceptance grep counts 6, not 5**
- `grep -cE '^(appcompat|splashscreen|datastore|turbine|coroutines) ='` also matches the library alias `turbine = {...}`. The plan names that alias. All 5 version lines are present.

**5. [Environment] Two phones attached**
- The Redmi (wireless) was also listed by adb, so `_device.sh` would refuse. Every device command exported `ADB_SERIAL=10BD7J2TAS000Z5`, which `_device.sh` and `gradlew.sh` already support. The Redmi was not touched.

Other notes:
- `ProfileModule` is an `object` with two `@Provides @Singleton` methods (store and repository).
- `LanguageScreen` takes `onContinue`, and the Language entry wires `continueTo(completeOnboarding = true) { replaceAll(Home) }`, so 03-03 changes only the entry.
- Only the title, subtitle and rows on Language scroll. The CTA stays pinned above the navigation inset.
- The first Gradle run re-downloaded the already-used Gradle 9.6.0 wrapper distribution (the local cache had been cleared). No new tool was involved.
- connectedDebugAndroidTest uninstalls the app at the end. A third-party vivo "gallery" app then shows an install/uninstall overlay, and it can cover the screen right after a test run. This matters for later ui.sh runs that follow an instrumented run.

## Device state

No phone settings were changed (no animation scale, battery, font scale or dumpsys changes). The only extra command was `cmd locale set-app-locales app.palm.dev.debug --locales hi`, which reproduced the flaky case and applies only to this app. The debug app is left installed, onboarded, in Hindi.

## Known Stubs

- `HomeScreen` shows the header only. The greeting uses `firstName`, which stays empty until 03-03 adds the Name step, so it currently reads "नमस्ते," / "Good afternoon, ". This is intentional: 03-03 adds the name and 03-04 adds the hero, grid, sheet and exit dialog.
- `homeEntries(backStack)` does not use `backStack` yet (03-04 adds navigation).
- The Hindi strings were written by Claude and need native-speaker review (already flagged in UI-SPEC).

## Threat Flags

None. No new exported components (the AppCompat service has exported=false and enabled=false), no logging of profile values, and the backup exclusions are confirmed on the built APK.

## Self-Check: PASSED
