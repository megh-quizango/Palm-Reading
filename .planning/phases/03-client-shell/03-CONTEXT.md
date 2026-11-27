# Phase 3: Client Shell - Context

**Gathered:** 2026-11-27
**Status:** Ready for planning

<domain>
## Phase Boundary

The Android app gets its first real screens: splash (S01), language (S02), 3 intro pages (S03), name + gender (S04), birth date/time (S05), Home hub (S06) and hand selection with photo tips (S07), all over the layered animated cosmic background, with the profile persisted across restarts. Requirements: FND-03, ONB-01..06, HOME-01, HOME-02, HOME-04, SCAN-01. No camera, scan, generating or result screens (Phases 4-6); the hand-selection "Continue" lands on a placeholder scan route that receives the chosen hand. No ads (ADS-* are Phase 6, and a first-time user sees none anyway). The backend is not called in this phase.

</domain>

<decisions>
## Implementation Decisions

The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1; Phase 3 planning started at the user's "Push and plan Phase 3"). The user also asked for speed. Decisions below are Claude's, grounded in `replica/PRD.md` §3-5.9/5.12, the Figma file and the Phase 1 code.

### Navigation and flow
- **D-01:** Navigation 3 (Phase 1 stack). Routes are `@Serializable` `NavKey`s: Splash, Language, Intro, Name, Birth, Home, HandSelect, Scan(hand) (placeholder until Phase 4), ComingSoon(feature), plus the debug-only Showcase. The back stack is a `SnapshotStateList` owned by the root; finishing onboarding replaces the whole stack with `[Home]` so Back never returns into onboarding. Back inside onboarding steps goes to the previous step; Back on Language (first step) leaves the app.
- **D-02:** Splash uses `androidx.core:core-splashscreen` with `setKeepOnScreenCondition` until the first DataStore read finishes, then routes: onboarded → Home, else → Language. No artificial delay; no ads, no remote config (PRD S01 SDK init is out of M1).
- **D-03:** The Phase 1 showcase moves to the debug source set (Phase 1 D-03), reachable from a hidden debug-only entry (long-press on the Home greeting). Release builds do not contain it.

### Profile and persistence (ONB-06)
- **D-04:** One `ProfileRepository` over DataStore Preferences: `language` (en|hi), `firstName`, `gender` (male|female|other|unset), `birthDate` (ISO `LocalDate`), `birthTime` (ISO `LocalTime` or absent) + `birthTimeUnknown` flag, `onboardingComplete`. Exposed as a `Flow<Profile>`; writes are suspend functions. Onboarding steps save as the user advances (so a kill mid-onboarding resumes at the right step is NOT required: an incomplete onboarding restarts at Language with the saved values pre-filled). Backup exclusion already holds (Phase 1 manifest + data_extraction_rules).
- **D-05:** `java.time` natively (minSdk 26). Birth date: not in the future, not before 1900-01-01; birth time optional with an "I don't know my birth time" toggle that clears and disables it.

### Language (ONB-02)
- **D-06:** All user-visible text lives in `strings.xml` with `values-hi/strings.xml` (Claude-written Hindi, flagged in STATE.md for native-speaker review before release). Picking a language switches the UI immediately and persists. Mechanism is the researcher's call, with this preference order: platform per-app locales (`LocaleManager`, API 33+) plus a back-compat path for API 26-32 that does not block the main thread at cold start. If the only sound back-compat path is `androidx.appcompat` `AppCompatDelegate.setApplicationLocales` with `autoStoreLocales`, use it (it also persists the choice) and keep DataStore `language` in sync. The device default picks the initial selection (hi if the system language is Hindi, else en).
- **D-07:** Devanagari rendering uses the Phase 1 font fallback chain; layouts must not clip taller Devanagari lines (no fixed heights on text containers).

### Onboarding screens (ONB-01..05)
- **D-08:** Intro = a `HorizontalPager` of 3 pages (illustration + title + body) with page indicator, "Next" and on the last page "Get started"; swiping works too. Copy is our own (palm reading, daily astrology, the [Guide] persona), not the reference app's.
- **D-09:** Name: first name trimmed, valid when it has at least 2 letters (Unicode letters, so Devanagari names pass; spaces, hyphen and apostrophe allowed between letters; max 30 chars). Inline error text, Continue disabled until valid. Gender: optional single-select chips Male / Female / Other (tap again to clear).
- **D-10:** Birth: Material 3 `DatePicker` and `TimePicker` dialogs styled with Nocturne tokens; the screen shows the chosen values as large tappable fields. "Continue" needs a date; time is optional. Finishing sets `onboardingComplete` and goes to Home.

### Home (HOME-01, 02, 04)
- **D-11:** Greeting by local time: 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-20:59 evening, else night, with the first name ("Good evening, Asha"). Pure, unit-tested function.
- **D-12:** Layout: greeting, then the palm-scan hero card (primary CTA → HandSelect), then a 2-column feature grid: Palm reading (live), Tarot, Horoscope, Birth chart, Love test, Daily report, Ask [Guide]. Non-M1 tiles open a ComingSoon bottom sheet (feature name, short line, "OK"). No "What's new" carousel in M1 (needs astrology data). No ad slots yet (Phase 6 adds the stub ad interfaces).
- **D-13:** Back on Home opens a Nocturne-styled confirm dialog ("Leave [AppName]?" Stay / Leave); Leave finishes the activity.

### Background (FND-03)
- **D-14:** Extend the Phase 1 `NocturneBackground` (do not replace it) to the 6-layer star field from Figma page "5 · Motion" (layer list and timing from the "Nocturne / Motion" variables). Animated values are read only in draw (no recomposition per frame). Static when system animations are off (`ANIMATOR_DURATION_SCALE == 0` / reduce motion). Each route declares whether it shows the background; Scan (and later Generating) opt out. No visible jank on the Redmi: frame pacing checked on device (gfxinfo / JankStats or Macrobenchmark-free measurement).

### Hand selection (SCAN-01)
- **D-15:** HandSelect: Left / Right segmented choice (default Right), one "good photo" example and 4 "bad photo" examples (too far, blurry or dark, fingers closed, jewellery or bent hand) as our own illustrations (vector drawables traced from our Figma art, never reference-app assets), a short requirements note, and "Continue" → Scan(hand). The chosen hand is the Scan route's argument; Phase 4 builds the scanner behind it. The placeholder Scan screen shows the hand and "Scanner arrives in the next update" (debug wording is fine), with no background.

### Performance and verification
- **D-16:** Cold start to first frame < 2 s measured on a release-type build on the Redmi (`adb shell am start -W`, TotalTime, median of 5 cold starts). A non-minified, debug-signed `benchmark`/release variant is acceptable for M1; R8 release is REL-03. If over budget, the fix (baseline profile, lazy init) is part of this phase.
- **D-17:** Tests: pure Kotlin unit tests for greeting, name validation, birth validation and the routing decision; ViewModel tests with Turbine; Room-free DataStore tests with a temp file; instrumented Compose UI tests for the onboarding flow and restart persistence where cheap; on-device check of the full flow on the Redmi (ARTEMIS or adb UI scripts per user rules).

### Dependencies (download consent)
- **D-18:** New Android artifacts expected: `androidx.datastore:datastore-preferences` 1.3.0 and `androidx.core:core-splashscreen` 1.2.0 (both already pinned in `.claude/CLAUDE.md`), possibly `androidx.appcompat` (D-06) and test libs (Turbine 1.2.1, MockK 1.14.12, coroutines-test 1.11.0, compose ui-test). The user approved "Gradle artifacts from Google Maven and Maven Central" in Phase 1, but each new artifact is listed in a `checkpoint:human-verify` consent task at the start of the first plan that adds it, so the user sees exactly what is downloaded. No other new downloads.

### Claude's Discretion
- Package layout under `app.palm` (e.g. `feature/onboarding`, `feature/home`, `feature/handselect`, `core/data`), screen composition details, copy wording, illustration style within Nocturne, exact animation curves (from tokens), and test split, as long as D-01..D-18 hold.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/ROADMAP.md` §Phase 3 (goal, 5 success criteria, UI hint)
- `.planning/REQUIREMENTS.md` (FND-03, ONB-01..06, HOME-01/02/04, SCAN-01)
- `.planning/research/SUMMARY.md`, `STACK.md`, `ARCHITECTURE.md`, `PITFALLS.md` (Nav3, DataStore, edge-to-edge, low-end phone notes)
- `.claude/CLAUDE.md` (pinned versions, what not to use)
- `.planning/phases/01-foundations-contract/01-CONTEXT.md` and the Phase 1 code: `android/app/src/main/java/app/palm/core/designsystem/*` (Nocturne theme, tokens, `NocturneBackground`, `Effects.kt`, components), `core/navigation/AppNavDisplay.kt`, `MainActivity.kt`, `showcase/*`
- `design-system/nocturne.tokens.json`
- Figma "[AppName] – UI" (file key `Ej98dV7fZQUhRwUyfQIlyJ`): pages "3 · Screens" (S01-S07 frames), "2 · Components", "5 · Motion" (6-layer star field, Nocturne / Motion variables), "1 · Foundations" (illustrations, icons)
- `replica/PRD.md` §3 (S01-S07), §4 (onboarding flow), §5.9, §5.12, §10 (cold start < 2 s)
- Clean room: never copy from `_extract/`

</canonical_refs>

<code_context>
## Existing Code Insights

- Single `:app` module, package `app.palm`, Hilt app (`PalmApp`), `MainActivity` hosts `AppNavDisplay` (Nav3) which today shows only the showcase. Room 3 `AppDatabase` exists (readings, Phase 5). `HealthClient` + `BackendStatus` live in the showcase.
- Design system: generated `NocturneTokens.kt` (colours, type specs by name), `NocturneTheme`, `NocturneFonts` (Devanagari fallback), `Effects.kt` (glows, gradient brushes), components `Buttons.kt` (primary/secondary/ghost + AD chip), `Chips.kt`, `FeatureCard.kt`; `NocturneBackground` (seeded 60-star placeholder, static under reduce motion).
- Dev tools: `tools/install-debug.sh`, `tools/gradlew.sh`, `tools/smoke-phone.sh`, `tools/screen.py`, `tools/check-motion.sh`; phone is the Redmi M2006C3MII (Android 11) over wireless adb; MIUI install prompt is remembered.
- Compose BOM is held at 2026.09.00 (compileSdk 37.0) until platforms;android-37.2 is installed.

</code_context>

<specifics>
## Specific Ideas

- Tracer first: Splash → Language → Home with the profile persisted is the thinnest end-to-end slice; then fill in intro, name, birth, home grid, hand select, and the background.
- Visual QA against the Figma S01-S07 frames on the real phone (screenshots via `tools/screen.py`).

</specifics>

<deferred>
## Deferred Ideas

- "What's new" carousel, settings screen, rating, notifications — later milestones.
- Ads on S02/S03 and interstitial after onboarding — Phase 6 stubs, real SDK later (MON-01).
- Birth place — astrology milestone (user decision 2026-11-26).
- Final brand name, persona name and applicationId — before first Play upload (REL-03).

</deferred>

---

*Phase: 03-client-shell*
