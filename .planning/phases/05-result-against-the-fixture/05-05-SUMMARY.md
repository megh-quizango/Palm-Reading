---
phase: 05-result-against-the-fixture
plan: 05
subsystem: ui
tags: [fixtures, debug-lab, hindi, contract, emulator]

requires:
  - phase: 05-result-against-the-fixture
    provides: "05-01: fixtureReading/stamp, ContractJson, ReadingRules, ReadingStore.deleteAll, FixtureLab NavKey + lab photo generator, result-open.sh, ui.sh scroll-to/hold; 05-03: InvalidPhoto route for the invalid variants"
provides:
  - "FixtureVariant (13 D-06 variants + LONG_TEXT) and the pure derive()"
  - "palm_reading.hi.json (fresh Hindi reading) and palm_reading.invalid.json (byte copy of the contract sample)"
  - "fixtureReading(source, variant, capture, lang, now): app-language default, HINDI keeps locale hi"
  - "debugFixtureVariant() twins: debug FixtureLabChoice, release always null"
  - "Full debug Fixture lab: aspect, skin, variant, pinned Open ScanReady + Delete all readings"
  - "tools/result-open.sh --aspect / --skin / --variant"
  - "fixtures.dir unit-test system property"
affects: [05-06, phase-6-backend]

actuals:
  tokens: 8500
  tasks: 2
  commits: 2
plan_head_before: a8c6548c6d8fc18ecc7e0addc15580b9a571a811
plan_head_after: a252185bda6dd98692edf135988d2c57999ee31b

tech-stack:
  added: []
  patterns:
    - "Variant seam: main code calls debugFixtureVariant(); the debug twin reads an in-memory object, the release twin returns null (same pattern as debugHomeLongPress)"
    - "fixtureReading decodes exactly one fixture per read and passes it to derive for all three arguments (derive reads only the one its variant needs)"

key-files:
  created:
    - android/app/src/main/java/app/palm/core/reading/FixtureVariant.kt
    - android/app/src/main/assets/fixtures/palm_reading.hi.json
    - android/app/src/main/assets/fixtures/palm_reading.invalid.json
    - android/app/src/debug/java/app/palm/debug/FixtureChoice.kt
    - android/app/src/release/java/app/palm/debug/FixtureChoice.kt
    - android/app/src/test/java/app/palm/core/reading/FixturesTest.kt
    - android/app/src/test/java/app/palm/core/reading/FixtureRepositoryTest.kt
  modified:
    - android/app/build.gradle.kts
    - android/app/src/main/java/app/palm/core/reading/PalmReadingRepository.kt
    - android/app/src/debug/java/app/palm/debug/FixtureLab.kt
    - tools/result-open.sh

key-decisions:
  - "The Delete all readings dialog closes only after store.deleteAll() finishes, so a script (or user) that sees the dialog gone knows the sweep is done; ui.sh launch uses am start -S, which would otherwise kill an in-flight delete"
  - "fixtures.dir is a second unitTests.all line instead of folding into the existing block, so the build diff is exactly one added line carrying fixtures.dir (acceptance invariant)"
  - "Variant tiles are labelled '<label> (<code>)' so result-open.sh selects them by code"

patterns-established:
  - "Device scripts select lab variants by the code in parentheses: ui.sh scroll-to '\\(CODE\\)  \\(' then tap '\\(CODE\\)  '"

requirements-completed: [RES-02, RES-03, READ-03]

coverage:
  - id: D1
    description: "en and invalid fixtures equal the contracts/ samples (parsed JSON); Hindi fixture has the en structure, traces, sources, locale hi; all 14 variants survive a strict round trip and pass ReadingRules from en and hi; each variant has its promised property; the 7 invalid variants cover InvalidReason.entries"
    requirement: RES-03
    verification:
      - kind: unit
        ref: "app.palm.core.reading.FixturesTest (4)"
        status: pass
    human_judgment: false
  - id: D2
    description: "Stamping: fresh lower-case UUIDs, capture hand and size, generated_at truncated to seconds with Z, fixture traces kept, no landmarks; lang picks en/hi; HINDI forces hi; INVALID_BENT reason"
    requirement: RES-02
    verification:
      - kind: unit
        ref: "app.palm.core.reading.FixtureRepositoryTest (3)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Lab on emulator: all_llm on 1:1 shows the Fate line; hindi on 9:16 dark shows the Hindi summary; Delete all readings (after confirm) removes Home's last reading"
    requirement: RES-02
    verification:
      - kind: e2e
        ref: "ADB_SERIAL=emulator-5554 device-lock: install-debug + result-open.sh --aspect 1:1 --variant all_llm + expect '^Fate  \\(' + result-open.sh --aspect 9:16 --skin dark --variant hindi + scroll-to <hi summary> + lab Delete -> absent 'Your last reading'"
        status: pass
    human_judgment: false
  - id: D4
    description: "Release ignores the lab: debugFixtureVariant release twin returns null; FixtureLabChoice absent from src/main and src/release"
    requirement: RES-02
    verification:
      - kind: other
        ref: ":app:compileReleaseKotlin exit 0 + acceptance greps"
        status: pass
    human_judgment: false
  - id: D5
    description: "Hindi fixture text (Claude-written)"
    verification:
      - kind: other
        ref: "Native-speaker review before release (STATE.md item)"
        status: pending
    human_judgment: true
    rationale: "Claude-written Hindi, as in Phases 3-4"

duration: 8min
completed: 2026-11-28
status: complete
---

# Phase 5 Plan 05: Fixture variants and the full debug lab Summary

**Every Phase 5 state can now be reached on the emulator. The repository reads the app-language fixture by default (Hindi in Hindi). A pure `FixtureVariant.derive` turns the en, hi and invalid fixtures into the 13 D-06 variants plus a long-text one. A drift and schema test pins the fixtures to `contracts/`. The debug Fixture lab picks the photo aspect (3:4, 1:1, 9:16), skin tone and variant, and can delete all readings. Release builds never see the lab choice. Verified on emulator-5554.**

## Performance

- **Duration:** about 8 min (08:04 to 08:12 UTC)
- **Started:** 2026-11-28T08:04:00Z
- **Completed:** 2026-11-28T08:12:04Z
- **Tasks:** 2 of 2
- **Files:** 11 changed (+500 / -17)

## Accomplishments

- Task 1 (test-first): `FixtureVariant` (14 entries with code and label). `derive` works as follows:
  - MIXED returns the app fixture and HINDI returns the hi fixture.
  - ALL_LLM and ALL_TEMPLATE draw every line with that source. An empty trace gets a 4-point `FATE_TRACE`.
  - ALL_NONE empties every trace and sets the source to none.
  - PRESENT_NOT_DRAWN changes only the heart line.
  - LONG_TEXT repeats the summary and every interpretation until each is at least 600 characters.
  - The 7 INVALID_x variants set their reason.

  `fixtureReading` takes the variant, decodes one fixture (app language, hi or invalid) and stamps it. HINDI keeps locale "hi". `FixturePalmReadingRepository` passes `debugFixtureVariant() ?: MIXED`. The Hindi fixture was written fresh, with the en structure, traces and sources. The invalid fixture is a byte copy of the contract sample.
- Task 2: The lab ViewModel takes `ReadingStore` and holds aspect, skin and the variant mirror. Choosing a variant writes `FixtureLabChoice.variant` immediately. The 05-01 palm generator now takes the size and skin tone, drawing the hand on the 960 x 1280 grid, scaled and centred. The pickers sit in a `weight(1f)` scrolling column, and the two buttons are pinned below it. The delete confirmation follows UI-SPEC section 6. `result-open.sh` accepts `--aspect`, `--skin` and `--variant`, checks the code against the 14 codes and prints the usage text on anything else.

## Task Commits

1. **Task 1: Variants, fixtures, language default, seam twins, tests** - `031f2fe` (feat)
2. **Task 2: Full debug lab and result-open.sh options** - `a252185` (feat)

## Verification run

- `:app:testDebugUnitTest --tests FixturesTest --tests FixtureRepositoryTest`: RED first, as a compile failure (`FixtureVariant` unresolved, `fixtureReading` arity). Then GREEN with 4 + 3 tests.
- `:app:compileReleaseKotlin :app:assembleDebug` exit 0 (the release `FixtureChoiceKt.class` was rebuilt).
- Emulator chain on emulator-5554 under device-lock exit 0: install-debug, then all_llm on 1:1 shows "Your palm reading" and the Fate line. Hindi on 9:16 dark reaches the Hindi summary. Lab Delete, then confirm, then Home shows "Read your palm" with no "Your last reading".
- Full `:app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` exit 0: 32 suites, 0 failures.
- Screenshots checked: the lab (pickers, two-per-row variant tiles, pinned buttons) and ScanReady with the dark 9:16 palm (720 x 1280 px).
- All acceptance greps pass, including the build.gradle.kts invariant (the only added line carries fixtures.dir). `libs.versions.toml` is unchanged and nothing was downloaded (D-25).

## TDD Gate Compliance

- Task 1: RED was observed as a compile failure on the new symbols and signature, then GREEN (7/7). No refactor commit.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Plan regex] ui.sh hold/tap patterns ending in `\(`**
- **Found during:** Task 2 verify
- **Issue:** As in 05-01 and 05-04, `hold "^\[AppName\]  \("` and `tap "^Delete  \("` can never match, because ui.sh appends its own `(x,y)` match.
- **Fix:** The verify chain used `"^\[AppName\]  "` and `"^Delete  "`. `expect "^Fate  \("` and `scroll-to` were kept as written, because those commands append nothing.

**2. [Rule 1 - Race] Delete closed the dialog before the sweep finished**
- **Issue:** Without a fix, the plan's chain would run `ui.sh launch` (`am start -S`, which force-stops the app) right after tapping Delete, and that could kill an in-flight `deleteAll`.
- **Fix:** The dialog now closes from `deleteAll`'s completion callback, and the chain waits with `ui.sh absent "Delete all readings\?"` before relaunching.
- **Files modified:** FixtureLab.kt
- **Commit:** a252185

**3. [Simplification] One fixture decode per read**
- The plan loads "only the ones the variant needs" through `source`. The code picks one file name by variant and passes the decoded fixture as all three `derive` arguments. This is behaviourally the same, since `derive` reads only the argument its variant uses.

## Known Stubs

None.

## Deferred to the phone session

- Nothing in this plan needs a phone. 05-06 does the end-to-end walk through the invalid variants into InvalidPhoto, and the walk through every variant in en and hi.

## Threat Flags

None. T-05-16: the release twin returns null, FixtureLabChoice exists only in src/debug, and compileReleaseKotlin passes. T-05-17: the fixtures are original text, and lab photos are generated at run time and never committed. T-05-SC: no package was installed.

## Self-Check: PASSED

- Files exist: FixtureVariant.kt, palm_reading.hi.json, palm_reading.invalid.json, debug and release FixtureChoice.kt, FixturesTest.kt, FixtureRepositoryTest.kt
- Commits on HEAD: 031f2fe, a252185

## Review fixes (post-phase)

- 70a51c5: `all_llm` and `all_template` drew the fate line but kept its "no clear fate line" text (en and hi). A line that was absent in the fixture now gets a short neutral present interpretation in the fixture's locale; lines already present keep their text. New `FixturesTest.aLineForcedPresentLosesItsAbsentTextInBothLocales`.
