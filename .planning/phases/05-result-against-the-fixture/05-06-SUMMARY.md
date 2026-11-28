---
phase: 05-result-against-the-fixture
plan: 06
subsystem: testing
tags: [emulator, gate, i18n, font-scale, share, validation]
status: complete

requires:
  - phase: 05-result-against-the-fixture
    provides: "05-01..05-05: Result, InvalidPhoto, Home last reading, share card, fixture variants, debug Fixture lab, tools/result-open.sh, ui.sh hold / scroll-to"
provides:
  - "tools/result-walk.sh: every fixture variant x en/hi with screenshots, tabs, font scale 2.0 backstops, kill + reopen, share sheet; restores font scale and app locale"
  - "tools/res_pattern.py: Android string resource -> literal ERE for ui.sh expect"
  - "Fix: a photo taller than the 55% cap narrows instead of overflowing (ResultScreen)"
  - "05-VALIDATION.md signed off (nyquist_compliant true, wave_0_complete true)"
affects: [phase-6-backend, gsd-verify-work]

actuals:
  tokens: 8800
  tasks: 2
  commits: 3
plan_head_before: 74bc01c1970de218a27ee625970d95158a535256
plan_head_after: 6ad69ddfdc949151e502e14af335d221cee0988f

tech-stack:
  added: []
  patterns:
    - "Device scripts read expected UI text from res/values{,-hi} through res_pattern.py instead of hard-coding it"
    - "On Windows, pipe Python output through tr -d '\\r' before mapfile (Python writes CRLF)"

key-files:
  created:
    - tools/result-walk.sh
    - tools/res_pattern.py
  modified:
    - tools/ui.sh
    - android/app/src/main/java/app/palm/feature/result/ResultScreen.kt
    - android/app/src/androidTest/java/app/palm/feature/result/ResultScreenTest.kt
    - .planning/phases/05-result-against-the-fixture/05-VALIDATION.md

key-decisions:
  - "res_pattern.py escapes only ERE metacharacters, not re.escape: re.escape also escapes - and #, which GNU grep warns about as stray backslashes"
  - "ui.sh scroll-to drags slowly (1000 ms, no fling) and allows 20 drags: at font scale 2.0 a 300 ms fling skipped lab tiles, and the long_text reading needs more than 8 drags to reach Share"
  - "The photo box uses fillMaxWidth().wrapContentWidth().heightIn(max).aspectRatio(ratio): with fillMaxWidth alone, aspectRatio could not satisfy the height cap and overflowed"

requirements-completed: [RES-01, RES-02, RES-03, RES-04, RES-05, RES-06, RES-07, READ-03, HOME-03]

duration: 86min
completed: 2026-11-28
---

# Phase 5 Plan 06: Phase 5 emulator gate Summary

**On emulator-5554, every JVM and instrumented suite passes and the release build assembles. All 14 fixture variants were walked in English and Hindi, with 42 screenshots covering three aspect ratios, both skin tones, font scale 2.0, kill + reopen and the share sheet. The walk found a 9:16 photo overflowing the legend, which is now fixed and has a regression test.**

## Performance

- Duration: about 86 min (2026-11-28T08:15Z to 09:41Z). Most of it was device time: two full instrumented runs and three walks of about 25 min each.
- Tasks: 2 of 2
- Files: 6 (2 created, 4 modified)

## Accomplishments

- **Task 1:** `:app:testDebugUnitTest :app:assembleRelease` exit 0. The full `:app:connectedDebugAndroidTest` run is green: 13 classes present, none with `tests="0"` (see the flake note). Static gates hold: no lab symbol in main or release, the only log call in reading/result is the debug aspect warning, en/hi key parity for strings_result/scan/home, and the version catalog is unchanged. VALIDATION per-task rows are green and `wave_0_complete: true`.
- **Task 2:** `tools/result-walk.sh` passes end to end (`walk OK: 42 phase5 screenshots`). Each valid variant opened on "Your palm reading" / "आपकी हस्तरेखा रीडिंग". Each invalid variant opened on "Photo not accepted" / "फ़ोटो स्वीकार नहीं हुई" with its own reason title. Love and Career tabs work for mixed. The font scale 2.0 shots cover mixed, long_text, invalid_jewelry and the Home card in both languages. After force-stop + relaunch, "Your last reading" reopened the newest reading. Share opened the system chooser with the card thumbnail and "My palm reading from [AppName]". Afterwards, font scale was 1.0 and the app locale [en] (both restored by the trap).
- res_pattern acceptance: the en `invalid_no_palm_lines_title` pattern matches "We couldn't see your palm lines". The hi keys print without UnicodeEncodeError, and a missing key exits 1 naming the key.

## Task Commits

1. **Task 1: suites, release, static gates, VALIDATION statuses:** `1268431` (test)
2. **Fix found by the walk: 9:16 photo overflow:** `057a726` (fix)
3. **Task 2: walk, res_pattern, ui.sh scroll-to, sign-off:** `6ad69dd` (feat)

## Screenshots (local, git-ignored, tools/adb/shots/)

`phase5-{en,hi}-{mixed,all_llm,all_template,all_none,present_not_drawn,hindi,long_text,invalid_blurry_or_dark,invalid_too_far,invalid_jewelry,invalid_bent,invalid_fingers_tight,invalid_incomplete,invalid_no_palm_lines}.png`, `phase5-{en,hi}-mixed-{love,career}.png`, `phase5-{en,hi}-{mixed,long_text,invalid_jewelry,home}-fs2.png`, `phase5-reopen.png`, `phase5-share-sheet.png` (42 files). Aspect = [3:4, 1:1, 9:16][i % 3] and skin = [light, dark][i % 2] over the variant order above.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] A 9:16 photo overflowed onto the top bar and covered the legend**
- **Found during:** Task 2 (first full walk, `phase5-en-all_template.png`, `phase5-en-hindi.png`)
- **Issue:** The photo box used `fillMaxWidth().heightIn(max = 55%).aspectRatio(ratio)`. fillMaxWidth sets the minimum width, so aspectRatio could not meet the height cap and measured 996 x 1609 px in a 1320 px slot. The box ran under the top bar and drew over the legend chips and the "Lines are drawn approximately" note.
- **Fix:** Added `wrapContentWidth()` before `heightIn`. A tall photo now narrows and stays centred. Added `ResultScreenTest.aTallPhotoStaysAboveItsLegend`, which seeds a 720 x 1280 reading and asserts the box ends above the legend, is at most 55% of the window and is 9:16. It failed before the fix (box 42,296,1038,1905 over chip top 1711) and passes after. ResultScreenTest, PalmOverlayTest, ShareTest and HomeLastReadingTest are green after the fix, and so are the JVM suite and release build.
- **Files:** ResultScreen.kt, ResultScreenTest.kt
- **Commit:** `057a726`

**2. [Rule 3 - Blocking] ui.sh scroll-to skipped list items at font scale 2.0**
- **Found during:** Task 2 (fs2 step: `scroll-to \(long_text\)` ended past the tile)
- **Fix:** The swipe is now a 1000 ms drag with no fling, and the cap went from 8 to 20 drags, because the long_text reading needed more than 8 to reach Share at fs2. The change is shared with result-open.sh, so it is slower but no longer skips items.
- **Commit:** `6ad69dd`

**3. [Rule 1 - Bug] CRLF in patterns read through mapfile**
- **Found during:** Task 2 (first walk: `expect Your palm reading` failed on a correct screen)
- **Fix:** `| tr -d '\r'` on the res_pattern.py output in result-walk.sh (Windows Python writes CRLF).
- **Commit:** `6ad69dd`

### Environment note (not a code change)

- The first full instrumented run failed HandSelectTest and HomeLastReadingTest.aDeletedReadingIsNoLongerOpened with "No compose hierarchies found". At that time another session's app (`com.meghquizango.pagewise`) held the foreground on the shared emulator, and that app does not use `tools/device-lock.sh`. The immediate full rerun was green with all 13 classes. This is recorded in 05-VALIDATION.md.

## Human check (for /gsd-verify-work)

Decide the halo alpha first. HALO_ALPHA in PalmPhoto.kt is 0.55 (BgDeep), while D-10 says about 30%. Approve 0.55 or ask for 0.30. Then open `tools/adb/shots/phase5-*.png` and confirm:
- lines sit on the photo, with the halo readable on light and dark skin and on 3:4, 1:1 and 9:16
- legend chips match the line cards
- all_none shows no overlay or legend and shows the "couldn't trace" note
- present_not_drawn says "Not clearly visible"
- the disclaimer shows on every tab
- each invalid reason has its own icon, title and tip
- Hindi has no clipped matras
- nothing is clipped at font scale 2.0
- a share target receives the 1080 x 1350 card with the disclaimer (the emulator chooser offers Quick Share, Google, Print, Drive, Maps)

Reply "approved" or list what is wrong.

Observation for that check: a Hindi-content reading viewed with the English UI (the reopened long_text reading in `phase5-share-sheet.png`) shows English badges such as "Approximate" next to Hindi text. This is expected, because content follows the reading's locale and chrome follows the app language. Flagged so it is a conscious approval.

## Deferred to the phone session

- **Share on the MIUI phone:** open a reading, tap Share, pick WhatsApp or Gmail, and check that the 1080 x 1350 card arrives with "My palm reading from [AppName]". Check that the MIUI sheet opens (no resolver error) and that Back returns to Result with Share idle.
- **API 26-27 share capture path (LayerSnapshotV22):** there is no system image. On any Android 8.0/8.1 device, tap Share and check that the card is not blank. If it is blank, ship the D-21 Canvas fallback.
- **Overlay contrast on a real light-skinned palm:** once Phase 6 traces follow the real palm.
- **Native review of the Hindi strings and the Hindi fixture** (`strings_result.xml` hi, `palm_reading.hi.json`).
- **Figma visual QA:** S10, Love, Career and the invalid frames against the Nocturne file.
- **Large screens / tablets:** not covered (phone emulator only).

## Known Stubs

None added by this plan.

## Threat Flags

None. T-05-18: the screenshots are under git-ignored tools/adb/shots (`git check-ignore` passes) and show only generated lab photos. T-05-19: font scale and app locale are saved and restored by an EXIT trap, verified at 1.0 / [en] afterwards. T-05-SC: nothing was installed and the catalog is unchanged.

## Self-Check: PASSED

- FOUND: tools/result-walk.sh, tools/res_pattern.py, 05-VALIDATION.md (nyquist_compliant true, wave_0_complete true, 13 green rows)
- FOUND commits: 1268431, 057a726, 6ad69dd (ancestors of HEAD)
- 42 phase5 screenshots, git-ignored; emulator font_scale 1.0, app locale [en]
