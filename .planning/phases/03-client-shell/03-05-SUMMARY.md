---
phase: 03-client-shell
plan: 05
subsystem: android-client
tags: [release-build, cold-start, jank, motion, screenshots, phase-gate]
status: complete
requires: [03-01, 03-02, 03-03, 03-04]
provides:
  - debug-signed release variant app.palm.dev for on-device timing (REL-03 swaps the key)
  - tools/cold-start.sh, tools/check-jank.sh, PKG-aware tools/check-motion.sh (off|on|saver)
  - 43 phase3-*.png screenshots (en/hi, font scale 1.0/2.0, Hindi Home 1.3), git-ignored, for the Figma sign-off
affects: [STATE.md Decisions and Blockers/Concerns]
tech-stack:
  added: []
  patterns:
    - "Draw-phase transforms (drawWithContent + rotate/scale) for ambient loops, not graphicsLayer, so semantics bounds do not change every frame"
key-files:
  created:
    - tools/cold-start.sh
    - tools/check-jank.sh
    - .planning/phases/03-client-shell/03-05-SUMMARY.md
  modified:
    - android/app/build.gradle.kts
    - tools/check-motion.sh
    - tools/adb/ui.py
    - android/app/src/main/java/app/palm/feature/home/HeroCard.kt
    - android/app/src/main/java/app/palm/core/designsystem/Effects.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt
    - .planning/STATE.md
decisions:
  - "Cold start gated on the profile-compiled median (A7), fresh-install median reported beside it; confirmed by the user 2026-11-27 (Task 3)"
  - "Jank gated on the gfxinfo 'Frame deadline missed' share <= 5% (A10); confirmed by the user 2026-11-27 (Task 3)"
  - "HeroOrb animation uses draw-only transforms so uiautomator (and accessibility services) see an idle Home"
metrics:
  duration: "about 45 min (review-fix session)"
  completed: 2026-11-27
actuals:
  tokens: 9000
  tasks: 2
  commits: 4
plan_head_before: 896299bf6b7aeacd5c2655aea8e3a8caf0c1c7ce
plan_head_after: "the fix(03-05) review-fix commit that adds this file (previous plan head cda71404224c818edbce50c23207cf05e7b0a659)"
---

# Phase 3 Plan 05: Phase gate on a release build Summary

The debug-signed release build onboards and runs the full functional gate on the emulator: motion off/on/saver, the Scan opt-out, the 9 instrumented tests, a clean release APK and 43 en/hi screenshots. The release cold-start and jank numbers are recorded as emulator-only. The user approved Task 3; the hardware performance gate is still deferred to the next phone session.

## Device

All Task 1 and Task 2 checks ran on the Android emulator `emulator-5554` (AVD launcher_test, sdk_gphone64_x86_64, API 35, 1080x2400, 420 dpi), chosen by the user because the Redmi Note 10T keeps dropping off adb. It stood in for the reference phone on the functional checks. Performance numbers from it are **emulator, not a gate** (x86 with a host/software GPU).

## Task 1: release build and cold start

- `android/app/build.gradle.kts`: the release build is signed with the debug key and carries the REL-03 shortcut comment (41580e2).
- `tools/cold-start.sh` (41580e2): refuses to run without PKG (exit 2), `--compile` waits `sleep 12` for ProfileInstaller, then compiles speed-profile and prints the dexopt filter.
- Install and onboarding (verify 1): `installRelease` succeeded. Onboarding reached Home ("Good evening, Asha"). The final `expect` first failed on a stale uiautomator dump (see Review fixes) and passed after the fix.
- Cold start (emulator, not a gate):

| Variant | Runs (ms) | Median |
|---|---|---|
| Fresh install (BUDGET_MS=100000) | 601, 565, 597, 536, 510 | 565 ms |
| Profile-compiled (`--compile`, dexopt `x86_64: [status=speed-profile]`) | 569, 565, 610, 545, 560 | 565 ms |

No startup fix was needed and no `baseline-prof.txt` was added. The Redmi hardware gate (< 2000 ms compiled median) is deferred to the next phone session.

## Task 2: the whole phase on the device

| Check | Result |
|---|---|
| check-motion off (PKG=app.palm.dev) | OK, static with animations off |
| check-motion on | OK, animating |
| check-motion saver | OK, static under battery saver (low_power restored to 0, battery override reset) |
| check-jank idle 10 on Home (release) | 29 of 578 frames deadline-missed (5.02%), p50 27 / p90 36 / p99 53 ms. The script exits 1 at the 5% line. **Emulator, not a gate** |
| Scan opt-out | Two hashes of rows 300..1400 taken 2 s apart are equal; pixel (4,800) = #05040E. PASS |
| check-jank swipe 3 on the intro (release) | 9 of 251 frames deadline-missed (3.59%), p50 26 / p90 34 / p99 48 ms. **Emulator, not a gate** |
| connectedDebugAndroidTest (no filter) | AppDatabaseTest 1, HandSelectTest 1, HomeTest 4, LanguageSwitchTest 1, OnboardingFlowTest 2: 9/9 pass, no tests="0" |
| assembleRelease APK | No `Lapp/palm/showcase/` in dex, allowBackup=false, no networkSecurityConfig. PASS |
| JVM unit tests (testDebugUnitTest) | 80 tests in 14 classes, 0 failures |
| Settings after the run | animator_duration_scale null, transition/window 1.0, low_power 0, font_scale 1.0 (= font_scale.orig), battery not unplugged: all as before |

Language path: the emulator is API 35, so this run covers the API 33+ LocaleManager path again. The API 26-32 AppCompat path has no device and is still unverified (recorded in STATE.md).

Hardware gate deferred: on the debug build on the Redmi, the intro swipe measured 105/280 (37.5%). The release swipe on the emulator is 3.59%, but that does not settle hardware. If the Redmi release swipe is still over 5%, the intro pager is the next fix.

### Screenshots (tools/adb/shots/, git-ignored, 43 files)

English: phase3-en-s01-splash, -s02-language, -s03-intro1, -s03-intro2, -s03-intro3, -s04-name, -s05-birth-date, -s05-birth-unknown, -s06-home, -s06-home-grid, -s06-tarot-sheet, -s06-exit-dialog, -s07-hand, -s07-hand-tips, -s07-scan; at font scale 2.0: -s04-name-fs2, -s05-birth-fs2, -s06-home-fs2, -s06-home-grid-fs2, -s07-hand-fs2, -s07-hand-tips-fs2.

Hindi: phase3-hi-s02-language, -s03-intro1, -s03-intro2, -s03-intro3, -s04-name, -s05-birth-date, -s05-birth-unknown, -s06-home, -s06-home-grid, -s06-tarot-sheet, -s06-exit-dialog, -s07-hand, -s07-hand-tips, -s07-scan; at font scale 2.0: -s04-name-fs2, -s05-birth-fs2, -s06-home-fs2, -s06-home-grid-fs2, -s07-hand-fs2, -s07-hand-tips-fs2; at 1.3: -s06-home-fs1.3, -s06-home-grid-fs1.3.

Spot-checked: the Hindi Home at 2.0, the Hindi tips at 2.0 and the Hindi birth screen. Devanagari rendered with no clipped matras, and text wrapped without truncation. S01 was taken 0.25 s after `am start` from the launcher (a capture with no delay showed the previous app).

### STATE.md

Added the native-speaker flag for the Hindi strings (D-06). Added the locale-path note: API 33+ verified, API 26-32 AppCompat unverified. This corrects the plan's "API 33+ unverified, Redmi API 30" wording, since the reference phones are API 33/35. Also added: the AppCompat 1.8.0 + autoStoreLocales decision, the cold-start gate variant with the emulator medians, the jank metric with the emulator numbers, the hardware-gate deferral and the pending Task 3 sign-off.

## Task 3: user sign-off (APPROVED 2026-11-27)

The user reviewed the emulator screenshots (S02-S07 en, Hindi Home and intro, Hindi Home at font scale 2.0) against Figma S01-S07 and replied "Approved, push". That reply also accepts the proposed gates: A7 cold start is gated on the profile-compiled median, with the fresh-install median reported beside it; A10 jank is gated at <= 5% deadline-missed frames. Both hardware gate runs are still deferred to the next phone session (see STATE.md Blockers).

## Deviations from Plan

- **Device:** the emulator replaced the Redmi at the user's request. Performance gates are recorded but not passed (see above).
- **[Rule 1/3] stale uiautomator dump and Home never idle:** see Review fixes.
- **Screenshot walk:** this was a scratchpad script (not committed, as the plan lists no walk script). The walk waits for the Name screen before typing, because a first run typed into the page during the transition.

## Review fixes

Finding (major, .planning/STATE.md:116): Tasks 2 and 3 were not done. STATE.md had no native-speaker flag and no API-level line, 03-05-SUMMARY.md was missing, and no gate numbers or screenshots were recorded.

- Task 2 finished on the release build on emulator-5554: motion off/on/saver OK, Scan opt-out OK, 9/9 instrumented, release APK clean, 43 screenshots, settings restored. Cold start and jank are recorded as "emulator, not a gate", with the hardware gate deferred.
- STATE.md now has the `native-speaker` flag and the `API 33` / API 26-32 line, plus the decision lines.
- This SUMMARY was written. Task 3 was later approved by the user (2026-11-27).
- Fix exposed by the run: `HeroCard.kt` HeroOrb used `graphicsLayer { rotationZ / scaleX / scaleY }`. Each per-frame layer update calls `owner.onLayoutChange`, which floods accessibility content-changed events. As a result `uiautomator dump` failed with "could not get idle state" on Home, and the same flood hits any running accessibility service. The orb now uses `drawWithContent { rotate/scale { drawContent() } }`, which is draw-only and does not recompose. The motion check still animates. HomeTest passes 4/4.
- Fix exposed by the run: `tools/adb/ui.py` re-read the previous `/sdcard/ui.xml` when a dump failed, so `expect` reported a stale screen. It now deletes the file before each dump.
- Not changed: the intro pager. The release swipe measured 3.59% on the emulator, and there is no hardware evidence that a fix is needed yet.

## Known Stubs

None added. ScanPlaceholderScreen and the coming-soon tiles are intentional (Phase 4 / D-12).

## Self-Check: PASSED

- tools/cold-start.sh, tools/check-jank.sh, tools/check-motion.sh exist; 43 phase3-*.png and font_scale.orig exist locally.
- Commits 41580e2, 44c4cce and cda7140 are ancestors of HEAD.
