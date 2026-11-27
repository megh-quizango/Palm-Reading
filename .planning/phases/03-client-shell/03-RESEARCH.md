# Phase 3: Client Shell - Research

**Researched:** 2026-11-27
**Domain:** Android client shell: splash, per-app language, DataStore profile, Navigation 3 flow, Compose animated background, M3 date/time pickers, cold start and jank measurement
**Confidence:** HIGH for versions and platform APIs (Google Maven / Maven Central metadata and official docs read this session); MEDIUM for Nav3 back-handling interplay and DataStore-on-JVM test behaviour (verify in Wave 0)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1; Phase 3 planning started at the user's "Push and plan Phase 3"). The user also asked for speed. Decisions below are Claude's, grounded in `replica/PRD.md` §3-5.9/5.12, the Figma file and the Phase 1 code.

#### Navigation and flow
- **D-01:** Navigation 3 (Phase 1 stack). Routes are `@Serializable` `NavKey`s: Splash, Language, Intro, Name, Birth, Home, HandSelect, Scan(hand) (placeholder until Phase 4), ComingSoon(feature), plus the debug-only Showcase. The back stack is a `SnapshotStateList` owned by the root; finishing onboarding replaces the whole stack with `[Home]` so Back never returns into onboarding. Back inside onboarding steps goes to the previous step; Back on Language (first step) leaves the app.
- **D-02:** Splash uses `androidx.core:core-splashscreen` with `setKeepOnScreenCondition` until the first DataStore read finishes, then routes: onboarded → Home, else → Language. No artificial delay; no ads, no remote config (PRD S01 SDK init is out of M1).
- **D-03:** The Phase 1 showcase moves to the debug source set (Phase 1 D-03), reachable from a hidden debug-only entry (long-press on the Home greeting). Release builds do not contain it.

#### Profile and persistence (ONB-06)
- **D-04:** One `ProfileRepository` over DataStore Preferences: `language` (en|hi), `firstName`, `gender` (male|female|other|unset), `birthDate` (ISO `LocalDate`), `birthTime` (ISO `LocalTime` or absent) + `birthTimeUnknown` flag, `onboardingComplete`. Exposed as a `Flow<Profile>`; writes are suspend functions. Onboarding steps save as the user advances (so a kill mid-onboarding resumes at the right step is NOT required: an incomplete onboarding restarts at Language with the saved values pre-filled). Backup exclusion already holds (Phase 1 manifest + data_extraction_rules).
- **D-05:** `java.time` natively (minSdk 26). Birth date: not in the future, not before 1900-01-01; birth time optional with an "I don't know my birth time" toggle that clears and disables it.

#### Language (ONB-02)
- **D-06:** All user-visible text lives in `strings.xml` with `values-hi/strings.xml` (Claude-written Hindi, flagged in STATE.md for native-speaker review before release). Picking a language switches the UI immediately and persists. Mechanism is the researcher's call, with this preference order: platform per-app locales (`LocaleManager`, API 33+) plus a back-compat path for API 26-32 that does not block the main thread at cold start. If the only sound back-compat path is `androidx.appcompat` `AppCompatDelegate.setApplicationLocales` with `autoStoreLocales`, use it (it also persists the choice) and keep DataStore `language` in sync. The device default picks the initial selection (hi if the system language is Hindi, else en).
- **D-07:** Devanagari rendering uses the Phase 1 font fallback chain; layouts must not clip taller Devanagari lines (no fixed heights on text containers).

#### Onboarding screens (ONB-01..05)
- **D-08:** Intro = a `HorizontalPager` of 3 pages (illustration + title + body) with page indicator, "Next" and on the last page "Get started"; swiping works too. Copy is our own (palm reading, daily astrology, the [Guide] persona), not the reference app's.
- **D-09:** Name: first name trimmed, valid when it has at least 2 letters (Unicode letters, so Devanagari names pass; spaces, hyphen and apostrophe allowed between letters; max 30 chars). Inline error text, Continue disabled until valid. Gender: optional single-select chips Male / Female / Other (tap again to clear).
- **D-10:** Birth: Material 3 `DatePicker` and `TimePicker` dialogs styled with Nocturne tokens; the screen shows the chosen values as large tappable fields. "Continue" needs a date; time is optional. Finishing sets `onboardingComplete` and goes to Home.

#### Home (HOME-01, 02, 04)
- **D-11:** Greeting by local time: 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-20:59 evening, else night, with the first name ("Good evening, Asha"). Pure, unit-tested function.
- **D-12:** Layout: greeting, then the palm-scan hero card (primary CTA → HandSelect), then a 2-column feature grid: Palm reading (live), Tarot, Horoscope, Birth chart, Love test, Daily report, Ask [Guide]. Non-M1 tiles open a ComingSoon bottom sheet (feature name, short line, "OK"). No "What's new" carousel in M1 (needs astrology data). No ad slots yet (Phase 6 adds the stub ad interfaces).
- **D-13:** Back on Home opens a Nocturne-styled confirm dialog ("Leave [AppName]?" Stay / Leave); Leave finishes the activity.

#### Background (FND-03)
- **D-14:** Extend the Phase 1 `NocturneBackground` (do not replace it) to the 6-layer star field from Figma page "5 · Motion" (layer list and timing from the "Nocturne / Motion" variables). Animated values are read only in draw (no recomposition per frame). Static when system animations are off (`ANIMATOR_DURATION_SCALE == 0` / reduce motion). Each route declares whether it shows the background; Scan (and later Generating) opt out. No visible jank on the Redmi: frame pacing checked on device (gfxinfo / JankStats or Macrobenchmark-free measurement).

#### Hand selection (SCAN-01)
- **D-15:** HandSelect: Left / Right segmented choice (default Right), one "good photo" example and 4 "bad photo" examples (too far, blurry or dark, fingers closed, jewellery or bent hand) as our own illustrations (vector drawables traced from our Figma art, never reference-app assets), a short requirements note, and "Continue" → Scan(hand). The chosen hand is the Scan route's argument; Phase 4 builds the scanner behind it. The placeholder Scan screen shows the hand and "Scanner arrives in the next update" (debug wording is fine), with no background.

#### Performance and verification
- **D-16:** Cold start to first frame < 2 s measured on a release-type build on the Redmi (`adb shell am start -W`, TotalTime, median of 5 cold starts). A non-minified, debug-signed `benchmark`/release variant is acceptable for M1; R8 release is REL-03. If over budget, the fix (baseline profile, lazy init) is part of this phase.
- **D-17:** Tests: pure Kotlin unit tests for greeting, name validation, birth validation and the routing decision; ViewModel tests with Turbine; Room-free DataStore tests with a temp file; instrumented Compose UI tests for the onboarding flow and restart persistence where cheap; on-device check of the full flow on the Redmi (ARTEMIS or adb UI scripts per user rules).

#### Dependencies (download consent)
- **D-18:** New Android artifacts expected: `androidx.datastore:datastore-preferences` 1.3.0 and `androidx.core:core-splashscreen` 1.2.0 (both already pinned in `.claude/CLAUDE.md`), possibly `androidx.appcompat` (D-06) and test libs (Turbine 1.2.1, MockK 1.14.12, coroutines-test 1.11.0, compose ui-test). The user approved "Gradle artifacts from Google Maven and Maven Central" in Phase 1, but each new artifact is listed in a `checkpoint:human-verify` consent task at the start of the first plan that adds it, so the user sees exactly what is downloaded. No other new downloads.

### Claude's Discretion
- Package layout under `app.palm` (e.g. `feature/onboarding`, `feature/home`, `feature/handselect`, `core/data`), screen composition details, copy wording, illustration style within Nocturne, exact animation curves (from tokens), and test split, as long as D-01..D-18 hold.

### Deferred Ideas (OUT OF SCOPE)
- "What's new" carousel, settings screen, rating, notifications — later milestones.
- Ads on S02/S03 and interstitial after onboarding — Phase 6 stubs, real SDK later (MON-01).
- Birth place — astrology milestone (user decision 2026-11-26).
- Final brand name, persona name and applicationId — before first Play upload (REL-03).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FND-03 | Content screens show the layered animated cosmic background; static when system animations are off; absent on camera and generating screens | Pattern 5 (one root sky behind `NavDisplay`, `sky` flag on the route key, draw-phase reads), Pitfalls 6-8, jank measurement in Validation |
| ONB-01 | Branded splash that hands off to onboarding (first run) or Home (returning) | Pattern 2 (core-splashscreen theme + `setKeepOnScreenCondition` on an activity-scoped ViewModel), Pattern 3 (replace `[Splash]`) |
| ONB-02 | Pick app language (English, Hindi) on first run | Pattern 1 (AppCompat 1.8.0 `setApplicationLocales` + `autoStoreLocales`, `AppCompatActivity`, `locale_config.xml`, `localeFilters`) |
| ONB-03 | Page through 3 intro screens with Next / Get started | Foundation `HorizontalPager` (already in the BOM), Code Examples |
| ONB-04 | First name (>= 2 letters) and optional gender | Pitfall 4 (Devanagari combining marks are not `isLetter()`), `NameRules` test table |
| ONB-05 | Birth date and optional birth time with "I don't know my birth time" | Pattern 6 (M3 1.4.0 `DatePickerDialog` / `TimePickerDialog`, UTC millis), Pitfall 5 |
| ONB-06 | Onboarding shown once; profile survives restarts (DataStore, excluded from backup) | Pattern 4 (`ProfileRepository`), backup facts verified in manifest + `data_extraction_rules.xml`, DataStore two-instance disk test |
| HOME-01 | Time-of-day greeting with name | Pure `greetingFor(LocalTime)` (D-11 windows) |
| HOME-02 | Hero card and feature grid; non-M1 tiles open coming soon | `LazyVerticalGrid(GridCells.Fixed(2))` or two `Row`s, ComingSoon decision (Pattern 3) |
| HOME-04 | Back on Home asks to confirm leaving | `BackHandler` inside the Home entry + `LocalActivity.finish()` (Pattern 3, Pitfall 3) |
| SCAN-01 | Choose left/right hand and see photo tips (1 good + 4 bad) before scanning | `Scan(hand: HandSide)` key reusing the contract enum, M3 `SingleChoiceSegmentedButtonRow` |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Client stack fixed: Kotlin, Compose (BOM-managed versions only, never pin single Compose artifacts), Hilt with KSP (never kapt), DataStore 1.3.0, Coil, kotlinx.serialization; Navigation 3 1.2.2 chosen over typed Nav2. [VERIFIED: .claude/CLAUDE.md]
- Pinned in CLAUDE.md for this phase: `androidx.core:core-splashscreen` 1.2.0, DataStore (Preferences) 1.3.0, `androidx.profileinstaller` 1.4.1 and `benchmark-macro-junit4` 1.5.0 only "in the hardening phase". Test stack: JUnit 4.13.2, MockK 1.14.12, Turbine 1.2.1, `kotlinx-coroutines-test` 1.11.0. [VERIFIED: .claude/CLAUDE.md]
- Do not use: `org.jetbrains.kotlin.android` plugin, kapt, `android.disallowKotlinSourceSets=false`, Accompanist, LiveData, Gson/Moshi, `material-icons-extended`, `core-library-desugaring` (minSdk 26 has `java.time`), string-route Nav2. [VERIFIED: .claude/CLAUDE.md]
- minSdk 26 / targetSdk 36 / compileSdk 37; performance budget cold start < 2 s to first frame; must run well on mid/low-end phones. [VERIFIED: .claude/CLAUDE.md; android/app/build.gradle.kts:12-16]
- Privacy: no biometric templates stored; profile stays on device. Policy: entertainment disclaimer on readings (not this phase). Secrets: none in the app. [VERIFIED: .claude/CLAUDE.md]
- Clean room: never copy from `_extract/`. Illustrations are our own (D-15). [VERIFIED: 03-CONTEXT.md]
- Device testing: explore with ARTEMIS / adb before authoring device tests, dynamic locators first, coordinates as fallback; ask the user if several devices are connected (only one is: Redmi M2006C3MII). [VERIFIED: user rules artemis.md; `adb devices -l` this session]
- GSD workflow: edits happen inside `/gsd-execute-phase`. [VERIFIED: .claude/CLAUDE.md]

## Summary

Phase 3 builds seven screens and one repository on a stack that is already green (AGP 9.5.3, Kotlin 2.4.21, Hilt 2.60.1, Nav3 1.2.2, Compose BOM 2026.09.00 = Compose 1.12.1 / material3 1.4.0). The only real design choice is per-app language. The official docs say plainly: for Compose apps that need an in-app picker on Android 12 and lower, use `AppCompatDelegate.setApplicationLocales`, and "the activity hosting your Compose UI must extend `AppCompatActivity`… Otherwise, setting the app locale won't work". On API 33+ the same call delegates to `LocaleManager` and syncs with system settings. The custom-storage alternative also needs the locale "before `onCreate`", so any back-compat path reads something synchronously at startup. `autoStoreLocales` reads one tiny file. **Use AppCompat 1.8.0 with `autoStoreLocales=true`** (D-06's fallback clause applies), make `MainActivity` an `AppCompatActivity`, and move `Theme.Palm` onto an AppCompat parent. [CITED: developer.android.com/guide/topics/resources/app-languages]

Splash, routing and persistence fit together in one way. `installSplashScreen()` runs before `super.onCreate`. An activity-scoped `MainViewModel` reads `ProfileRepository.profile.first()` once. `setKeepOnScreenCondition { vm.start.value == null }` holds the system splash until that read returns. The Nav3 back stack starts as `[Splash]` and is replaced with `[Language]` or `[Home]`. Because the ViewModel survives configuration changes, the locale-change recreate on the Language screen does not re-block drawing. The background becomes one root instance behind `NavDisplay`, switched off by a `sky` flag on the top route key, so screen transitions never restart the star clock and the Scan placeholder runs no animation.

Measurement needs a non-debuggable build. Debuggable builds skip the merged library baseline profiles and run Compose in interpreted/JIT-heavy mode. The smallest option is to sign the existing `release` build type with the debug key (D-16 allows this) and measure with `adb shell am start -S -W` (median of 5) and `adb shell dumpsys gfxinfo <pkg>`. No app-specific baseline profile or Macrobenchmark unless the number is over budget.

**Primary recommendation:** AppCompat 1.8.0 + core-splashscreen 1.2.0 + DataStore 1.3.0. One `MainViewModel` decides the start route, the Nav3 stack is replaced rather than navigated, one root `NocturneBackground(sky = …)` sits behind `NavDisplay`, and the release build type (debug-signed, not minified) is used for the cold-start and jank gates.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Splash hold + start-route decision | Client: Activity + activity-scoped ViewModel | Client storage (DataStore) | Must survive config change; splash API lives on the Activity |
| App language | Client: AppCompat / platform `LocaleManager` | DataStore mirror (`language`) | Platform owns resources config; DataStore copy is for later API `locale` fields |
| Profile persistence | Client storage: DataStore Preferences (`filesDir/datastore`) | — | Local only; excluded from backup by existing rules |
| Validation (name, birth date/time, greeting) | Client: pure Kotlin functions | ViewModels | Unit-testable without Android |
| Navigation, Back, exit confirm | Client: root composable owning Nav3 stack | Home entry `BackHandler` | D-01: root owns `SnapshotStateList` |
| Animated background | Client: root composable, draw phase | Route key `sky` flag | One clock, no per-screen restarts |
| Hand choice carried to scanner | Client: Nav3 key argument `Scan(hand)` | — | Survives process death via saved back stack |
| Backend | — (not called in this phase) | — | CONTEXT domain |

## Standard Stack

### Core (new in this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.appcompat:appcompat` | **1.8.0** (stable, 2026-08-12) | `AppCompatDelegate.setApplicationLocales` back-compat to API 23, `AppCompatActivity` | Official recommendation for Compose in-app language pickers below API 33 [VERIFIED: Google Maven metadata `<release>1.8.0`; CITED: app-languages guide]. AAR manifest `minSdkVersion="23"` [VERIFIED: AAR manifest read this session] |
| `androidx.core:core-splashscreen` | **1.2.0** (stable) | S01 splash on API 26-36 with one API | [VERIFIED: Google Maven `<release>1.2.0`; pinned in CLAUDE.md] |
| `androidx.datastore:datastore-preferences` | **1.3.0** (stable, 2026-10-21) | Profile store | [VERIFIED: Google Maven `<release>1.3.0`; CITED: datastore release notes] |

### Test (new)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `app.cash.turbine:turbine` | 1.2.1 | Flow/StateFlow assertions in VM tests | `testImplementation` [VERIFIED: Maven Central `<release>1.2.1`] |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | `runTest`, `StandardTestDispatcher`, `Dispatchers.setMain` | `testImplementation` [VERIFIED: Maven Central `<release>1.11.0`] |
| `androidx.compose.ui:ui-test-junit4` | via BOM (1.12.1) | Instrumented Compose UI tests | `androidTestImplementation(platform(bom))` + this [VERIFIED: compose-bom-2026.09.00.pom lists `ui-test-junit4` 1.12.1] |
| `androidx.compose.ui:ui-test-manifest` | via BOM (1.12.1) | Host `ComponentActivity` for `createComposeRule()` | `debugImplementation` [VERIFIED: same BOM POM] |

**Not needed (do not add):** MockK (fakes over an interface are smaller: `ProfileRepository` is the only collaborator), Robolectric 4.18.1 (instrumented tests run on the connected Redmi; Robolectric downloads ~100+ MB android-all jars), JankStats / `androidx.metrics` (gfxinfo is enough), `profileinstaller` (already transitive via Compose UI 1.12.1 [VERIFIED: ui-android-1.12.1.pom lists `profileinstaller`]), Macrobenchmark / baseline-profile plugin (only if cold start misses budget, D-16), `navigation3` bottom-sheet artifact (none exists; see Pattern 3).

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| AppCompat `setApplicationLocales` | `LocaleManager` (33+) + custom `attachBaseContext` wrapping for 26-32 | Two code paths, still a synchronous read before `onCreate` (docs: "provide the stored locales before `onCreate`"), and no AndroidX support. Not smaller |
| AppCompat | Compose-only override (`CompositionLocalProvider(LocalContext/LocalConfiguration …)` from a DataStore value) | No recreate, no new dependency, but unofficial. Breaks non-Compose strings (activity label, future notifications, toasts) and does not sync with Android 13+ system "App languages". Rejected |
| Release build type signed with debug key | New `benchmark` build type | A third build type needs its own copy of the debug/release-split source files (D-03). The release type already exists |
| `PreferenceDataStoreFactory.create` | `DataStore.Builder` (new public in 1.3.0) | The Builder is new and takes a `CoroutineContext`. The factory is the documented, widely used path. Either works; take the factory |

**Installation (version catalog additions):**
```toml
[versions]
appcompat = "1.8.0"
splashscreen = "1.2.0"
datastore = "1.3.0"
turbine = "1.2.1"
coroutines = "1.11.0"

[libraries]
androidx-appcompat = { module = "androidx.appcompat:appcompat", version.ref = "appcompat" }
androidx-core-splashscreen = { module = "androidx.core:core-splashscreen", version.ref = "splashscreen" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
```

## Package Legitimacy Audit

The phase uses the Maven ecosystem. The `package-legitimacy` seam covers npm/PyPI/crates only, so it was not run. Every artifact below is published under the `androidx.*` (Google Maven, Google-owned), `app.cash.*` (Cash App) or `org.jetbrains.*` (JetBrains) groups and was read from the registry's `maven-metadata.xml` this session.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| androidx.appcompat:appcompat 1.8.0 | Google Maven | line since 2018; 1.8.0 2026-08-12 | n/a (Google Maven has no counts) | android.googlesource.com/platform/frameworks/support | OK (first-party) | Approved, consent checkpoint |
| androidx.core:core-splashscreen 1.2.0 | Google Maven | since 2021 | n/a | same | OK | Approved, consent checkpoint |
| androidx.datastore:datastore-preferences 1.3.0 | Google Maven | since 2020; 1.3.0 2026-10-21 | n/a | same | OK | Approved, consent checkpoint |
| androidx.compose.ui:ui-test-junit4 / ui-test-manifest 1.12.1 | Google Maven | BOM-managed | n/a | same | OK | Approved (ui-test-manifest 1.12.1 already in the Gradle cache) |
| app.cash.turbine:turbine 1.2.1 | Maven Central | 1.2.1 2025-06-11 | n/a | github.com/cashapp/turbine | OK | Approved, consent checkpoint |
| org.jetbrains.kotlinx:kotlinx-coroutines-test 1.11.0 | Maven Central | 1.11.0 2026-05 | n/a | github.com/Kotlin/kotlinx.coroutines | OK | Approved, consent checkpoint |

**Transitive artifacts the consent task should name** (from POMs read this session; Gradle resolves to the highest requested):
- appcompat 1.8.0 → `appcompat-resources` 1.8.0 (new; 1.7.1 is cached), `fragment` 1.5.4 (cached), `drawerlayout` 1.0.0 (cached), `cursoradapter` 1.0.0 (cached), `emoji2-views-helper` 1.2.0 (cached), `resourceinspection-annotation` 1.0.1, `jspecify` 1.0.0. [VERIFIED: appcompat-1.8.0.pom]
- core-splashscreen 1.2.0 → `appcompat-resources` 1.7.0 (resolves up to 1.8.0). [VERIFIED: core-splashscreen-1.2.0.pom]
- datastore-preferences 1.3.0 → `datastore-preferences-android`, `datastore-android`, `datastore-core-android`, `datastore-preferences-core-android`, `datastore-core-okio-jvm`, `datastore-preferences-proto`, `datastore-preferences-external-protobuf`, `okio-jvm` 3.17.0, `atomicfu-jvm` 0.28.0, `androidx.tracing:tracing-android` 2.0.1 (all 1.3.0 for datastore). Only 1.1.2 datastore is cached. [VERIFIED: datastore-*-1.3.0.pom]
- ui-test-junit4 1.12.1 → `ui-test-android`, `espresso-core` 3.5.0, `androidx.test:core` 1.5.0, `monitor` 1.6.1, `kotlinx-coroutines-test-jvm` 1.9.0 (androidTest only). [VERIFIED: ui-test-junit4-android-1.12.1.pom]

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
 Launcher tap
     │
     ▼
 MainActivity : AppCompatActivity
   attachBaseContext ── AppCompat (API<33: reads stored locale file, applies config;
     │                              API>=33: platform per-app locale already applied)
   installSplashScreen()  ──► system/compat splash (Theme.Palm.Starting, bg = nocturne_bg_base)
   super.onCreate()
     │
     ▼
 MainViewModel (activity scope, survives recreate)
   init: start = ProfileRepository.profile.first() ──► DataStore file (filesDir/datastore/profile.preferences_pb)
     │   start == null ──► keepOnScreenCondition = true (no frame drawn yet)
     ▼   start = Home | Language
 Root composable
   NocturneTheme ─► NocturneBackground(sky = top.sky) ─► NavDisplay(backStack)
     │                      │ draw-phase star clock (off when reduceMotion or sky=false)
     │                      ▼
     │  backStack: [Splash] ──replace──► [Language] → Intro → Name → Birth ──replace──► [Home]
     │                                                                                     │
     │        Home: greeting(LocalTime, name) · hero card · grid ── non-M1 tile ──► ComingSoon sheet
     │              BackHandler ──► exit dialog ──Leave──► activity.finish()
     │              hero CTA ──► HandSelect(Left|Right, tips) ──Continue──► Scan(hand) [sky=false]
     ▼
 Language screen tap ──► AppCompatDelegate.setApplicationLocales(hi|en) ──► activity recreate
                         └► ProfileRepository.setLanguage() (DataStore mirror)
 Each onboarding Continue ──► suspend ProfileRepository.save…() ──► DataStore edit
```

### Recommended Project Structure
```
android/app/src/main/java/app/palm/
├── MainActivity.kt            # AppCompatActivity: splash, edge-to-edge, setContent
├── MainViewModel.kt           # start route (null until first DataStore read)
├── core/data/                 # Profile.kt, ProfileRepository.kt (interface + DataStore impl), DataStoreModule.kt
├── core/locale/AppLanguage.kt # current()/set() over AppCompatDelegate, system default
├── core/navigation/           # Routes.kt (sealed Route : NavKey, sky flag), AppNavDisplay.kt
├── core/designsystem/         # NocturneBackground.kt extended (sky param, 6 layers)
├── feature/onboarding/        # Language, Intro, Name, Birth screens + VMs; NameRules.kt, BirthRules.kt
├── feature/home/              # HomeScreen, HomeViewModel, Greeting.kt, ComingSoon sheet, ExitDialog
└── feature/handselect/        # HandSelectScreen, ScanPlaceholderScreen
android/app/src/debug/java/app/palm/showcase/   # moved ShowcaseScreen, BackendStatus + DebugEntries.kt
android/app/src/release/java/app/palm/debug/    # DebugEntries.kt no-op (same signature)
android/app/src/main/res/values-hi/strings.xml
android/app/src/main/res/xml/locale_config.xml
```
`HealthClient` stays in `core/network` (main): Phase 6 needs OkHttp there. Only `BackendStatus.kt` and `ShowcaseScreen.kt` move. [VERIFIED: grep this session: only `showcase/BackendStatus.kt` imports `HealthClient`; `AppNavDisplay.kt:11,28` imports/uses `ShowcaseScreen`]

### Pattern 1: Per-app language with AppCompat (D-06)
**What:** `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))` from the Language screen's click handler. On API 33+ it delegates to `LocaleManager`. On 26-32 AppCompat stores the tag (when `autoStoreLocales` is on) and recreates AppCompat activities. [CITED: app-languages guide, "Setting the locale re-creates the Activity by default"; "Call this on the main thread as it may require Activity.restart()"]

Required changes:
1. `MainActivity : AppCompatActivity()`. The docs caution: "If you're using Compose with `setApplicationLocales`, you must extend your activity from `AppCompatActivity`. Otherwise, setting the app locale won't work." [CITED]
2. `Theme.Palm` parent changes from `android:Theme.Material.NoActionBar` [VERIFIED: res/values/themes.xml:3] to `Theme.AppCompat.NoActionBar`. Keep `<item name="android:windowBackground">@color/nocturne_bg_base</item>` [VERIFIED: themes.xml:5]. AppCompatActivity throws without an AppCompat theme. [ASSUMED, well-known behaviour]
3. Manifest service, verbatim from the docs [CITED]:
```xml
<service
  android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
  android:enabled="false"
  android:exported="false">
  <meta-data android:name="autoStoreLocales" android:value="true" />
</service>
```
   The docs note that this "causes a blocking read on the main thread and might cause a StrictMode diskRead and diskWrite violation". It reads one small file once per process. Accept it and confirm through the cold-start gate. [CITED]
4. System "App languages" listing (API 33+): add `res/xml/locale_config.xml` with `<locale android:name="en"/>` and `<locale android:name="hi"/>`, plus `android:localeConfig="@xml/locale_config"` on `<application>`. Use the manual file, not `generateLocaleConfig`. AGP's generator "uses the resources in the res folders of your app modules and any library module dependencies", and AppCompat/Material3 ship dozens of `values-xx` folders, so the generated list would show every language. [CITED: app-languages guide]
5. `android { androidResources { localeFilters += listOf("en", "hi") } }` strips library translations for other languages from the APK. It is the AGP 8.8+ replacement for `resourceConfigurations`. [CITED: developer.android.com/build/releases/gradle-plugin-api-updates]
6. Initial selection (D-06): before any app locale is set, `AppCompatDelegate.getApplicationLocales().isEmpty` and `LocalConfiguration.current.locales[0]` is the system locale, so preselect `hi` when its `language == "hi"`, else `en`. After the user picks, read `AppCompatDelegate.getApplicationLocales()[0]`. AppCompat 1.7+ makes `Locale.getDefault()` return the system locale after a cold start, so do not use it as "the app language". [CITED: appcompat release notes 1.7.x]
7. Keep DataStore `language` in sync. Write it in the same click handler. At startup, if AppCompat's locale (the source of truth, which also reflects Android 13+ Settings changes) differs from DataStore, overwrite DataStore.

`NocturneTheme` already derives `devanagari` from `LocalConfiguration.current.locales[0].language == "hi"` [VERIFIED: NocturneTheme.kt:137], so the recreate re-runs the Devanagari-safe type styles with no theme change. Replace the comment on NocturneTheme.kt:136.

### Pattern 2: Splash held on the first DataStore read (D-02)
```kotlin
// MainActivity.kt (shape)
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()            // must precede super.onCreate
        enableEdgeToEdge(SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { vm.start.value == null }
        setContent { NocturneTheme { AppNavDisplay(start = vm.start.collectAsStateWithLifecycle().value) } }
    }
}
```
```xml
<!-- res/values/themes.xml -->
<style name="Theme.Palm.Starting" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/nocturne_bg_base</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.Palm</item>
</style>
```
The manifest `<application android:theme>` (or the activity theme) changes to `@style/Theme.Palm.Starting` [VERIFIED current value: AndroidManifest.xml:14 `android:theme="@style/Theme.Palm"`]. The theme parent, the attribute names and the `installSplashScreen()`-before-`super.onCreate()` rule come from the migration guide [CITED: developer.android.com/develop/ui/views/launch/splash-screen/migrate]. `ic_launcher_foreground` is the existing adaptive-icon foreground [VERIFIED: res/drawable listing]. The executor checks that it fits the splash icon safe zone on the Redmi screenshot.

`MainViewModel`: `val start: StateFlow<Route?> = flow { emit(if (repo.profile.first().onboardingComplete) Home else Language) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)`. It is activity-scoped, so after the locale recreate `start` is already non-null and the keep-condition does not block the new window's first draw.

core-splashscreen 1.2.0 changes worth knowing: an `isLightTheme` attribute was added, and the default light navigation bar now "respects day/night theme" on API 33+. Check the Redmi screenshot and set `isLightTheme=false` only if the bar icons come out dark. A 1.0.0-beta02 fix covers rendering on MIUI dark mode. [CITED: developer.android.com/jetpack/androidx/releases/core, Core-Splashscreen section]

### Pattern 3: Navigation 3 flow, Back, sheet and dialog
- **Keys:** `@Serializable sealed interface Route : NavKey { val sky: Boolean get() = true }`. Use `data object`s for Splash, Language, Intro, Name, Birth, Home, HandSelect, `data class Scan(val hand: HandSide) : Route { override val sky get() = false }` and `data class ComingSoon(val feature: Feature) : Route`. Reuse the contract enum `enum class HandSide { @SerialName("left") LEFT, @SerialName("right") RIGHT }` [VERIFIED: PalmReadingDto.kt:72-73] so Phase 4 sends it unchanged.
- **Stack:** keep `rememberNavBackStack(Splash)` plus the two decorators already in place [VERIFIED: AppNavDisplay.kt:19-26]. The library "expects that your back stack will be a snapshot-state backed `List<T>`" [CITED: navigation-3/basics].
- **Replace, don't push:** `fun NavBackStack<NavKey>.replaceAll(key: NavKey) { add(key); removeRange(0, size - 1) }`. Use it for Splash → first screen and Birth → Home. Adding first means the list is never empty. Only replace when the top is `Splash`, so a stack restored after process death (for example `[Home, HandSelect]`) is kept.
- **onBack:** `onBack = { backStack.removeLastOrNull() }` as today. With `[Language]` alone, the system Back finishes the activity, which is D-01's "Back on Language leaves the app".
- **Exit confirm on Home (D-13):** inside the Home entry, `var ask by rememberSaveable { mutableStateOf(false) }`, `BackHandler(enabled = !ask) { ask = true }`, and an M3 `AlertDialog` whose Leave button calls `LocalActivity.current?.finish()`. The innermost enabled handler wins [CITED: developer.android.com/guide/navigation/custom-back]. On Home the stack has one entry, so `NavDisplay` has nothing to pop. Nav3 1.2's use of NavigationEvent for back dispatch has not been verified against an inner `BackHandler` [ASSUMED]. Wave 0 test: Espresso `pressBack()` on Home shows the dialog.
- **ComingSoon (D-01 + D-12):** Nav3 1.2.2 has no built-in bottom-sheet strategy. Only `DialogSceneStrategy` (with `DialogKey` metadata) and a sample `AnimatedBottomSheetSample`/OverlayScene exist [CITED: navigation3 release notes 1.1.0-alpha05, 1.2.0-alpha03]. `sceneStrategies` takes a `List<SceneStrategy>` since 1.1.0 [CITED]. Recommendation: keep `ComingSoon(feature)` as the D-01 key and add a small `BottomSheetSceneStrategy` (an `OverlayScene` that hosts M3 `ModalBottomSheet` and calls `onBack` on dismiss, with `equals`/`hashCode` per the scene lint), selected by entry metadata. If that costs more than ~40 lines in practice, the planner may instead hold the sheet as Home UI state (`ModalBottomSheet` handles its own Back). Record the deviation from D-01 if so.
- **Transitions:** set `transitionSpec`, `popTransitionSpec` and `predictivePopTransitionSpec` explicitly to a fade of `NocturneDurations.Standard` (300 ms) [VERIFIED: NocturneTokens.kt:154 `const val Standard = 300`] so the library defaults don't matter. Entries must not paint opaque backgrounds, or the root sky disappears.
- **Per-entry ViewModels:** `hiltViewModel()` inside an entry is already scoped to that entry by `rememberViewModelStoreNavEntryDecorator()` [VERIFIED: AppNavDisplay.kt:25,28].

### Pattern 4: ProfileRepository over DataStore (D-04)
```kotlin
@Module @InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides @Singleton
    fun profileStore(@ApplicationContext c: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(produceFile = { c.preferencesDataStoreFile("profile") })
}
```
Exactly one instance per file per process (DataStore throws when two are active on the same file) [ASSUMED, documented DataStore rule]. Keys: `stringPreferencesKey("language")`, `("first_name")`, `("gender")`, `("birth_date")` (ISO `LocalDate.toString()`), `("birth_time")` (ISO `LocalTime`), `booleanPreferencesKey("birth_time_unknown")`, `("onboarding_complete")`. Map `IOException` in `data` to `emptyPreferences()` (the standard `catch` pattern). Writes go through `edit {}`. Setting "I don't know" removes `birth_time` and sets the flag in one `edit`.

Backup: `android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:fullBackupContent="false"` [VERIFIED: AndroidManifest.xml:8-10], and the rules exclude `domain="file"` for both `cloud-backup` and `device-transfer` [VERIFIED: data_extraction_rules.xml:6,13]. DataStore and AppCompat's locale record both live under `filesDir`, so nothing new is needed. Re-run the merged-manifest check after the manifest edits.

### Pattern 5: Root sky with per-route opt-out (FND-03, D-14)
Change the signature to `NocturneBackground(modifier, sky: Boolean = true, content)`. When `sky` is false (or `reduceMotion`), skip `rememberInfiniteTransition` and the star draw, and paint a solid `bgCamera`. `NocturneExtras.bgCamera` exists already. Call it once at the root: `NocturneBackground(sky = (backStack.lastOrNull() as? Route)?.sky ?: true) { NavDisplay(...) }`. Never wrap `NavDisplay` in different parents conditionally, because that would rebuild the nav subtree and lose state. Keep the existing rules: animated `State` read only inside `drawBehind` and content in its own `graphicsLayer` [VERIFIED: NocturneBackground.kt:66-80 and file body]. `reduceMotion` is read from `ANIMATOR_DURATION_SCALE` with a `1f` default [VERIFIED: NocturneTheme.kt:133-135].

6 layers: the Figma "5 · Motion" layer list could not be read in this session (no Figma tool). The executor must read it from the Figma file before coding. Budget per frame on the Redmi (60 Hz, 16.6 ms): at most ~150-200 `drawCircle` calls, no `Brush`/`Shader` allocation inside `drawBehind` (pre-build gradients with `remember`), glows as a pre-rendered `ImageBitmap` sprite drawn with `drawImage(alpha)`, and no `Modifier.blur`, which is a no-op below API 31 and the Redmi is API 30 [ASSUMED, well-known RenderEffect limit]. Durations come from the token values `AmbientFast = 2400`, `Ambient = 4000`, `AmbientSlow = 40000` [VERIFIED: NocturneTokens.kt:157-159]. Easings `GentleSine` / `Standard` [VERIFIED: NocturneTokens.kt:182,185].

### Pattern 6: Birth date/time pickers (D-05, D-10)
- `rememberDatePickerState(initialSelectedDateMillis, yearRange = 1900..LocalDate.now().year, selectableDates = object : SelectableDates { override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMidnightMillis })` inside `DatePickerDialog(onDismissRequest, confirmButton, dismissButton) { DatePicker(state) }`.
- Convert with `Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()` and back with `date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()`. Never use the system zone (see Pitfall 5).
- Time: `rememberTimePickerState(initialHour, initialMinute, is24Hour = DateFormat.is24HourFormat(context))` with `TimePickerDialog`. material3 1.4.0 "Added an `TimePickerDialog` API that can be used for `TimePicker`, `TimeInput` or to have a switchable version" [CITED: compose-material3 release notes 1.4.0]. The BOM resolves material3 to 1.4.0 [VERIFIED: compose-bom-2026.09.00.pom]. The exact parameter list and opt-in annotation were not checked [ASSUMED]. The executor reads the signature in the IDE/sources jar.
- Colours come from `MaterialTheme.colorScheme`, which is already the Nocturne dark scheme. Override only what Figma differs on via `DatePickerDefaults.colors(...)` / `TimePickerDefaults.colors(...)`. Month names follow the activity configuration, so Hindi works after the AppCompat recreate [ASSUMED].

### Anti-Patterns to Avoid
- **Calling `setApplicationLocales` during composition or together with a navigation push:** call it from the tap only. Navigate on a separate "Continue" so the recreate never races a stack change.
- **Holding the start route in an Activity field:** after the locale recreate the field resets and `setKeepOnScreenCondition` blocks drawing again. Use the ViewModel.
- **One `NocturneBackground` per screen:** each instance starts its own clock, so stars jump on every transition and Scan cannot opt out cleanly.
- **`Char.isLetter()` as the whole name rule:** rejects Hindi names (Pitfall 4).
- **A second DataStore instance in tests or in two modules on the same file.**
- **Reading the device time zone for DatePicker millis.**

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Per-app locale + persistence + Android 13 settings sync | `attachBaseContext` config wrapping, own locale file | AppCompat `setApplicationLocales` + `autoStoreLocales` | Handles API 33 delegation, recreate, storage, system settings |
| Splash on API 26-30 | Splash activity or a Compose splash screen | core-splashscreen | Same look on all versions, no extra activity, no double splash on 12+ |
| Pager with page indicator | Custom swipe gesture | Foundation `HorizontalPager` + `rememberPagerState { 3 }` | Fling, accessibility, state saving |
| Date and time selection | Custom wheels | M3 `DatePickerDialog`, `TimePickerDialog` | Locale formatting, a11y, keyboard input mode |
| Segmented Left/Right | Two toggled buttons | M3 `SingleChoiceSegmentedButtonRow` / `SegmentedButton` | Selection semantics for TalkBack |
| Frame timing | In-app FPS counter | `adb shell dumpsys gfxinfo <pkg>` | HWUI's own per-frame data, no app code |
| Cold-start timing | `System.nanoTime` logs | `adb shell am start -S -W` TotalTime | Platform-measured launch to first frame |

**Key insight:** every hard part here (locale back-compat, splash back-compat, pickers, pager) has an AndroidX component. The only custom code is the rules (name, birth, greeting, routing) and the sky drawing.

## Runtime State Inventory

Not a rename/refactor phase. One moved component: the Showcase moves from `main` to `debug`. Nothing at runtime stores the old location: no stored data, no services, no OS registrations, no secrets. Build artifacts: none beyond a normal rebuild. The first install of the AppCompat build on the Redmi starts with no stored app locale, which is expected.

## Common Pitfalls

### Pitfall 1: AppCompat theme crash after switching to `AppCompatActivity`
**What goes wrong:** `IllegalStateException: You need to use a Theme.AppCompat theme`. **Why:** `Theme.Palm` is `android:Theme.Material.NoActionBar` [VERIFIED: themes.xml:3]. **How to avoid:** parent `Theme.AppCompat.NoActionBar`, and `postSplashScreenTheme` points to it. **Warning sign:** crash on first launch of the new build.

### Pitfall 2: Splash blocks drawing again after the language recreate
**What goes wrong:** a blank or held screen for a moment after picking Hindi. **Why:** on recreate, a keep-condition that reads state which reset with the Activity stays `true`. **How to avoid:** `start` lives in an activity-scoped ViewModel (Pattern 2). **Warning sign:** a visible pause on language tap on the Redmi (API 30 compat path).

### Pitfall 3: Back on Home exits without the dialog
**What goes wrong:** with a one-entry stack the activity finishes. **Why:** no enabled inner handler, or a handler registered outside the entry. **How to avoid:** `BackHandler` inside the Home entry content. Instrumented test with `Espresso.pressBack()` plus an on-device gesture-nav check (MIUI gestures). **Warning sign:** the test sees the activity in the DESTROYED state.

### Pitfall 4: Devanagari names fail "at least 2 letters"
**What goes wrong:** names like "अंकित" or "रवि" are rejected or count wrong. **Why:** matras and anusvara/virama are combining marks (`Character.getType` `NON_SPACING_MARK` / `COMBINING_SPACING_MARK`), not letters. ZWJ/ZWNJ (U+200D/U+200C) are format characters. [ASSUMED: Unicode general categories, standard knowledge] **How to avoid:** a name is valid when, after trim and with length ≤ 30, every code point is a letter, a mark (Mn/Mc/Me) following a letter or mark, ZWJ/ZWNJ inside a word, or one of space / `-` / `'` between letters, and the count of `Character.isLetter` code points is ≥ 2. Test table: "Asha" ok, "A" no, "रवि" ok, "अंकित" ok, "Mary-Jane" ok, "O'Neil" ok, "Al  " ok after trim, "-Al" no, "A1" no, emoji no, 31 chars no.

### Pitfall 5: Birth date shifts by a day
**What goes wrong:** LatAm users (UTC-3..-6) see the previous day stored. **Why:** M3 DatePicker millis are UTC midnight. Converting with `ZoneId.systemDefault()` moves them west of UTC. [ASSUMED: documented M3 behaviour] **How to avoid:** convert with `ZoneOffset.UTC` both ways. Unit-test the conversion with `TimeZone.setDefault(America/Sao_Paulo)`.

### Pitfall 6: Animation recomposes every frame or allocates in draw
**What goes wrong:** jank and heat on the Redmi. **Why:** reading `State.value` in composition, or building a `Brush`/`Path` inside `drawBehind`. **How to avoid:** keep the Phase 1 pattern, pre-build brushes and sprites, cap star count. Confirm with gfxinfo on the release build. **Warning sign:** Layout Inspector recomposition counts rising on Home.

### Pitfall 7: Measuring on a debug build
**What goes wrong:** cold start 2-5x worse and jank that is not real. **Why:** debuggable APKs disable ART optimisations, and AGP does not merge library baseline profiles into them [ASSUMED: AGP/ART behaviour]. **How to avoid:** measure on the non-minified release build signed with the debug key. **Warning sign:** a number that changes a lot between debug and release.

### Pitfall 8: Sideloaded release build without compiled profiles
**What goes wrong:** the first measurement is slow and later ones are fast. **Why:** ProfileInstaller (transitive via Compose) writes the embedded profile on first launch, but ART compiles it only on the next background dexopt [ASSUMED]. **How to avoid:** after install, launch once, run `adb shell cmd package compile -m speed-profile -f app.palm.dev`, then do 5 timed cold starts. Report both the "fresh install" and "profile-compiled" medians. Gate D-16 on the profile-compiled median, since that is what a Play install with library profiles approximates. **Confirm the gate choice with the user.**

### Pitfall 9: DataStore JVM test fails with "Method … not mocked"
**What goes wrong:** a local unit test of `ProfileRepository` throws from `android.os.Trace`. **Why:** datastore-core-android 1.3.0 now depends on `androidx.tracing:tracing-android` 2.0.1 [VERIFIED: datastore-core-android-1.3.0.pom]. Whether it touches `android.os.Trace` on JVM was not tested [ASSUMED]. **How to avoid:** if it fails, add `testOptions { unitTests.isReturnDefaultValues = true }`, or move the DataStore test to `androidTest` (the Redmi is attached). **Warning sign:** a `RuntimeException: Method isEnabled in android.os.Trace not mocked`.

### Pitfall 10: Temp file for the DataStore test
**What goes wrong:** `TemporaryFolder.newFile("profile.preferences_pb")` creates an empty file, and DataStore may read it as corrupt. The file must also end in `.preferences_pb`. [ASSUMED] **How to avoid:** `File(tmp.root, "profile.preferences_pb")` (not created).

## Code Examples

### Greeting (D-11)
```kotlin
enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }
fun dayPart(t: LocalTime): DayPart = when (t.hour) {
    in 5..11 -> DayPart.MORNING
    in 12..16 -> DayPart.AFTERNOON
    in 17..20 -> DayPart.EVENING
    else -> DayPart.NIGHT
}
// UI: stringResource(R.string.greeting_evening, name)  ->  "Good evening, %1$s"
```
Boundaries to test: 04:59 → NIGHT, 05:00 → MORNING, 11:59 → MORNING, 12:00 → AFTERNOON, 16:59, 17:00, 20:59, 21:00 → NIGHT.

### Start route (pure, D-17 "routing decision")
```kotlin
fun startRoute(p: Profile): Route = if (p.onboardingComplete) Home else Language
```

### DataStore disk round trip (ONB-06 without a device)
```kotlin
@Test fun profileSurvivesNewStoreInstance() = runTest {
    val file = File(tmp.root, "profile.preferences_pb")
    val scopeA = TestScope(UnconfinedTestDispatcher(testScheduler) + Job())
    DataStoreProfileRepository(PreferenceDataStoreFactory.create(scope = scopeA, produceFile = { file }))
        .saveName("Asha", Gender.FEMALE)
    scopeA.cancel()                                   // release the file before reopening
    val repoB = DataStoreProfileRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
    assertEquals("Asha", repoB.profile.first().firstName)
}
```

### Cold start (D-16)
```bash
PKG=app.palm.dev   # release build type, no suffix [VERIFIED: build.gradle.kts:14,26-28]
for i in 1 2 3 4 5; do
  adb -s "$ADB_SERIAL" shell am start -S -W -n "$PKG/app.palm.MainActivity" | tr -d '\r' | awk '/TotalTime/{print $2}'
  sleep 2
done | sort -n | sed -n 3p   # median of 5
```

### Jank (D-14)
```bash
adb -s "$ADB_SERIAL" shell dumpsys gfxinfo app.palm.dev reset
sleep 10   # Home idle with the sky animating (then repeat for an intro-pager swipe run)
adb -s "$ADB_SERIAL" shell dumpsys gfxinfo app.palm.dev | grep -E "Total frames rendered|Janky frames|50th|90th|99th"
```

### Release build signed for device measurement (smallest variant)
```kotlin
release {
    signingConfig = signingConfigs.getByName("debug") // shortcut: debug-signed for on-device timing, replace with the upload key at REL-03
    buildConfigField("String", "BASE_URL", "\"https://api.invalid\"")
}
```
Debug-only Showcase (D-03): `src/debug/.../DebugEntries.kt` and `src/release/.../DebugEntries.kt` define the same `fun EntryProviderScope<NavKey>.debugEntries()` and `val debugHomeLongPress: NavKey?` (null in release). Main code never references `showcase.*` directly.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `resourceConfigurations` | `androidResources.localeFilters` | AGP 8.8 | Use `localeFilters` on AGP 9.5.3 |
| `NavDisplay(sceneStrategy = a then b)` | `sceneStrategies = listOf(...)` | Nav3 1.1.0 | Use the list form |
| Untyped Nav3 metadata maps | `NavMetadataKey` metadata DSL | Nav3 1.1.0 | Use it for the sky/sheet flags if metadata is used |
| Custom LocaleHelper wrappers | AppCompat per-app locales + Android 13 `LocaleManager` | AppCompat 1.6.0 / API 33 | Single call; appears in system settings |
| `DataStoreFactory` only | `DataStore.Builder` also public | DataStore 1.3.0 | Optional; the factory stays fine |

**Deprecated/outdated:** `androidx.compose.material.icons` is "no longer recommended" and no longer a material3 dependency (1.4.0) [CITED]. Use vector drawables, as CLAUDE.md already says.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | AppCompatActivity requires an AppCompat-descendant theme (crash otherwise) | Pattern 1, Pitfall 1 | Low: visible on first launch |
| A2 | A `BackHandler` inside the Home entry intercepts Back with a one-entry Nav3 1.2.2 stack | Pattern 3, Pitfall 3 | HOME-04 fails; fallback is a root-level `BackHandler(enabled = top == Home)` |
| A3 | Devanagari matras/anusvara are Unicode marks, not letters | Pitfall 4 | Hindi names rejected; covered by the test table |
| A4 | M3 DatePicker millis are UTC midnight | Pattern 6, Pitfall 5 | Off-by-one dates in negative-offset zones |
| A5 | `TimePickerDialog` parameter list / opt-in in material3 1.4.0 | Pattern 6 | Minor compile fix |
| A6 | DataStore 1.3.0 tracing dependency may break local JVM tests | Pitfall 9 | Move test to androidTest or `isReturnDefaultValues` |
| A7 | ProfileInstaller profiles compile only after background dexopt on sideloaded installs; gate D-16 on the profile-compiled median | Pitfall 8 | Gate choice needs user confirmation |
| A8 | Debuggable builds don't merge library baseline profiles | Pitfall 7 | Measurement on the wrong variant |
| A9 | `Modifier.blur` is a no-op below API 31 | Pattern 5 | Glow looks different on the Redmi |
| A10 | Jank pass threshold: Janky frames < 5% and 90th percentile ≤ 16 ms on Home idle (10 s) on the release build | Validation | Needs the user's OK as the "no visible jank" bar |
| A11 | Hindi month names in DatePicker follow the AppCompat-applied configuration | Pattern 6 | English month names under Hindi UI |
| A12 | Figma "5 · Motion" defines exactly 6 layers usable within ~200 draw calls | Pattern 5 | Layer budget rework |

## Open Questions

1. **Figma "5 · Motion" layer list and the S01-S07 frames.**
   - What we know: 6 layers plus "Nocturne / Motion" variables. Duration tokens are already generated.
   - What's unclear: layer contents (nebula? shooting star? parallax/tilt was mentioned in Phase 1 deferrals) and counts.
   - Recommendation: the first background task reads the Figma page (Figma tool in the executor session, or the user exports a frame). Drop tilt/sensor parallax: D-14 does not require it.
2. **ComingSoon as a route vs Home UI state.** Recommendation in Pattern 3: keep the route and add a small sheet scene strategy, with a recorded fallback.
3. **API 33+ path untested on hardware.** The Redmi is API 30 (back-compat path). No API 33+ device or AVD was seen. Recommendation: accept it for M1 (AppCompat delegates to the platform), or run the instrumented language test on an emulator if the user installs one.
4. **Cold-start gate variant (A7) and jank threshold (A10):** confirm with the user at the end-of-phase human verify.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| adb | install, cold start, gfxinfo, UI tests | ✓ | 1.0.41 | — |
| Redmi M2006C3MII (API 30) over wireless adb | all on-device checks | ✓ (`device`, one device) | Android 11 | — |
| JDK | Gradle | ✓ | 17.0.17 | — |
| Android SDK platform android-37.0 / build-tools 36.0.0, 37.0.0 | compileSdk 37 | ✓ | — | — |
| Python | `tools/screen.py`, `check-motion.sh` | ✓ | 3.14.0 | — |
| Gradle cache: appcompat 1.7.1, ui-test-manifest 1.12.1, fragment, drawerlayout, cursoradapter, emoji2-views-helper, espresso-core 3.6.1/3.7.0 | build | ✓ (partial) | — | New downloads listed in the audit need consent (D-18) |
| API 33+ device/emulator | LocaleManager path | ✗ | — | Rely on AppCompat delegation; optional emulator later |
| Figma access | 6-layer spec, S01-S07 visuals | not in this session | — | Executor session tool or user export |

**Missing dependencies with no fallback:** none blocking.
**Missing dependencies with fallback:** API 33+ device; Figma (needed before the background and illustration tasks).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 (JVM) + androidx.test runner 1.7.0 / ext-junit 1.3.0 (instrumented, on the Redmi) + Compose ui-test-junit4 1.12.1 (BOM) |
| Config file | `android/app/build.gradle.kts` (`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` [VERIFIED: build.gradle.kts:19]) |
| Quick run command | `bash tools/gradlew.sh :app:testDebugUnitTest` |
| Full suite command | `bash tools/gradlew.sh :app:testDebugUnitTest :app:connectedDebugAndroidTest` (Redmi attached) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ONB-01 | `startRoute(profile)` Home vs Language; splash replaced, never in back stack | unit + instrumented | `:app:testDebugUnitTest --tests '*StartRouteTest'`; `connectedDebugAndroidTest` `OnboardingFlowTest` | ❌ Wave 0 |
| ONB-02 | Pick Hindi → Hindi string visible after recreate; DataStore `language=hi` | instrumented (API 30 compat path) | `connectedDebugAndroidTest --tests '*LanguageSwitchTest'` | ❌ Wave 0 |
| ONB-03 | Next ×2 → "Get started" on page 3; swipe works | instrumented Compose | `OnboardingFlowTest` | ❌ Wave 0 |
| ONB-04 | Name rule table (Latin, Devanagari, punctuation, length); gender toggle clears | unit | `--tests '*NameRulesTest'` | ❌ Wave 0 |
| ONB-05 | Birth bounds (1900-01-01..today), UTC conversion under a negative TZ, "don't know" clears time | unit | `--tests '*BirthRulesTest'` | ❌ Wave 0 |
| ONB-06 | Disk round trip through a second DataStore instance; true process restart lands on Home with data | unit + device script | `--tests '*ProfileRepositoryTest'`; `am force-stop` + relaunch + UI dump / ARTEMIS | ❌ Wave 0 |
| ONB-06 | Backup excluded | static | grep merged manifest for `allowBackup="false"` and `dataExtractionRules` | ✅ (Phase 1 rules) |
| HOME-01 | `dayPart` boundaries; greeting has the name | unit | `--tests '*GreetingTest'` | ❌ Wave 0 |
| HOME-02 | Non-M1 tile opens ComingSoon; Palm tile/hero → HandSelect | instrumented Compose | `HomeTest` | ❌ Wave 0 |
| HOME-04 | `pressBack()` on Home shows dialog; Stay keeps; Leave finishes | instrumented | `HomeTest` | ❌ Wave 0 |
| SCAN-01 | Default Right; choose Left → Scan placeholder shows Left; 1 good + 4 bad tips present | instrumented Compose | `HandSelectTest` | ❌ Wave 0 |
| FND-03 | Static with animations off, animating with them on; no sky on Scan; jank threshold (A10) | device script | `bash tools/check-motion.sh off` / `on` (needs PKG param for release), new `tools/check-jank.sh` | ✅ check-motion.sh (debug pkg hard-coded) / ❌ check-jank.sh |
| D-16 | Cold start median < 2000 ms | device script | new `tools/cold-start.sh` | ❌ Wave 0 |
| VM behaviour | Onboarding VMs prefill from profile and save on Continue | unit (Turbine + fake repo + `Dispatchers.setMain`) | `--tests '*ViewModelTest'` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `bash tools/gradlew.sh :app:testDebugUnitTest`
- **Per wave merge:** full suite incl. `connectedDebugAndroidTest` on the Redmi
- **Phase gate:** full suite green + `cold-start.sh` + `check-jank.sh` + `check-motion.sh off|on` on the release build + ARTEMIS/adb walk of S01-S07 in en and hi, before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] Catalog + `build.gradle.kts`: turbine, coroutines-test, ui-test-junit4 (androidTest, with `platform(bom)`), ui-test-manifest (debug), after the consent checkpoint
- [ ] `test/.../MainDispatcherRule.kt`, `FakeProfileRepository.kt`
- [ ] Unit test files: `StartRouteTest`, `NameRulesTest`, `BirthRulesTest`, `GreetingTest`, `ProfileRepositoryTest`, VM tests
- [ ] `androidTest/.../OnboardingFlowTest`, `LanguageSwitchTest`, `HomeTest`, `HandSelectTest` (use `createAndroidComposeRule<MainActivity>()`; the real `PalmApp` Hilt graph works with the default runner, so no hilt-android-testing is needed. Clear the profile with `targetContext` file deletion in `@Before` only before the activity starts, or add a debug-only reset hook)
- [ ] `tools/cold-start.sh`, `tools/check-jank.sh`; parameterize `PKG` in `tools/check-motion.sh` (currently `PKG=app.palm.dev.debug`)

## Security Domain

### Applicable ASVS Categories (Level 1)

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | no accounts in M1 |
| V3 Session Management | no | — |
| V4 Access Control | no | single exported launcher activity only [VERIFIED: AndroidManifest.xml:15-23] |
| V5 Input Validation | yes | `NameRules` (length, allowed code points), birth bounds; validate before writing DataStore |
| V6 Cryptography | no | profile is app-private plaintext; `datastore-tink` exists if ever needed, not in M1 |
| V8 Data Protection | yes | backup/device-transfer exclusion (verified), never log name/DOB/time, no PII in nav keys beyond `HandSide` |
| V14 Configuration | yes | Showcase (with backend status) absent from release (D-03); release debug-signed only for measurement (comment + REL-03) |

### Known Threat Patterns for an Android Compose client

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Profile PII leaked via cloud backup / device transfer | Information disclosure | `allowBackup=false` + `data_extraction_rules` excluding `file` (in place) |
| PII in logcat (name, birth date) | Information disclosure | No `Log` of profile values; grep in code review |
| Debug screen shipped in release | Elevation / disclosure | Source-set split; check that `ShowcaseScreen` is absent from the release APK (`apkanalyzer dex packages` or a grep of the build intermediates) |
| Oversized or control-character name input | Tampering | 30-char cap, code-point allow-list |

## Sources

### Primary (HIGH confidence)
- Google Maven `maven-metadata.xml`: appcompat (1.8.0), core-splashscreen (1.2.0), datastore-preferences (1.3.0), profileinstaller (1.4.1), compose ui-test-junit4/manifest, material3, androidx.test, uiautomator, navigation3-ui (1.2.2); POMs for appcompat 1.8.0, core-splashscreen 1.2.0, datastore-*-1.3.0, compose-bom 2026.09.00, ui-android 1.12.1, ui-test-junit4-android 1.12.1; appcompat 1.8.0 AAR manifest (minSdk 23)
- Maven Central metadata: turbine 1.2.1, mockk 1.14.12, kotlinx-coroutines-test 1.11.0, robolectric 4.18.1
- https://developer.android.com/guide/topics/resources/app-languages (AppCompat requirement, autoStoreLocales snippet and blocking-read note, locale_config, generateLocaleConfig scope)
- https://developer.android.com/develop/ui/views/launch/splash-screen/migrate (theme, installSplashScreen order, keep-on-screen)
- https://developer.android.com/guide/navigation/navigation-3/basics and /custom-layouts; https://developer.android.com/jetpack/androidx/releases/navigation3 (1.1.0 `sceneStrategies` list, DialogKey, OverlayScene, 1.2.x notes)
- https://developer.android.com/jetpack/androidx/releases/datastore (1.3.0 notes); .../releases/core (Core-Splashscreen section); .../releases/appcompat (1.8.0, 1.7 locale notes); .../releases/compose-material3 (1.4.0 TimePickerDialog)
- Repo files read this session: AndroidManifest.xml, themes.xml, data_extraction_rules.xml, build.gradle.kts, libs.versions.toml, MainActivity.kt, AppNavDisplay.kt, NocturneTheme.kt, NocturneBackground.kt, NocturneTokens.kt, PalmReadingDto.kt, tools/*.sh

### Secondary (MEDIUM confidence)
- https://developer.android.com/build/releases/gradle-plugin-api-updates via search summary (`localeFilters` replaces `resourceConfigurations` in AGP 8.8)
- https://developer.android.com/guide/navigation/custom-back via search summary (innermost `BackHandler` wins)

### Tertiary (LOW confidence)
- Slack thread on Nav3 alpha back-handling issues (fixed in later alphas); not relied on

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Every version was read from registry metadata today, and the transitive sets come from POMs.
- Architecture: HIGH for language/splash/DataStore (official docs); MEDIUM for Nav3 back interplay and the sheet strategy.
- Pitfalls: MEDIUM. Several are known platform behaviour tagged [ASSUMED], each with a cheap test.

**Research date:** 2026-11-27
**Valid until:** 2026-12-27 (Nav3 1.3 and Compose 1.13 are moving; re-check if the BOM is raised to 2026.11.01)

**Process note:** the research-plan / research-store / classify-confidence / package-legitimacy seams were not run (Maven ecosystem, no search providers enabled in config). Tiers are by source type. One appcompat AAR was fetched only to read its manifest and was then deleted from the scratchpad. Nothing was installed and nothing was run on the phone apart from `adb devices -l`.
