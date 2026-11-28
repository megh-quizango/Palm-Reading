---
phase: 05-result-against-the-fixture
plan: 04
subsystem: ui
tags: [compose, graphicslayer, fileprovider, share, emulator]

requires:
  - phase: 05-result-against-the-fixture
    provides: "05-01: ResultActions(reading, photo, ...) seam, PalmPhoto/PhotoState, LineRules, strings_result share keys, seedReading/ReadingEntryPoint; 05-02: static PalmPhoto (revealKey null)"
provides:
  - "ShareCard (432 x 540 dp at density 2.5 = 1080 x 1350 px) and ShareCapture (off-screen GraphicsLayer record)"
  - "writeSharePng, shareUri, shareIntent, shareChooser"
  - "Non-exported FileProvider ${applicationId}.share with cache-path share/ only"
  - "Share button with Preparing / failure states in ResultActions"
  - "cacheDir/share emptied at app start"
affects: [05-05, 05-06]

actuals:
  tokens: 7800
  tasks: 2
  commits: 2
plan_head_before: 660c0f21a4db90808f135a788cc4a3ccb3a445d9
plan_head_after: 122579c682be6eff1488d662854ab3f6fb147a35

tech-stack:
  added: []
  patterns:
    - "Off-screen capture: rememberGraphicsLayer + drawWithContent { layer.record { drawContent() } } without drawLayer, in a Box(size(0.dp)); capture after a recorded flag via snapshotFlow, then toImageBitmap"
    - "Compose tests that render 0-size content must wrap it in fillMaxSize: ComponentActivity.setContent uses WRAP_CONTENT, and a 0 x 0 window is never drawn"
    - "Leaving and returning from another app's activity in a test: poll dumpsys topResumedActivity and ActivityScenario.state; query compose only once the activity is resumed again"

key-files:
  created:
    - android/app/src/main/java/app/palm/feature/result/share/ShareCard.kt
    - android/app/src/main/java/app/palm/feature/result/share/Share.kt
    - android/app/src/main/res/xml/share_paths.xml
    - android/app/src/androidTest/java/app/palm/feature/result/share/ShareTest.kt
  modified:
    - android/app/src/main/java/app/palm/feature/result/ResultActions.kt
    - android/app/src/main/AndroidManifest.xml
    - android/app/src/main/java/app/palm/PalmApp.kt

key-decisions:
  - "RESEARCH A1 answered on emulator-5554 (API 35, LayerSnapshotV28): recording a GraphicsLayer from a node that is never drawn on screen works; the Compose GraphicsLayer path shipped, the D-21 Canvas fallback was not needed"
  - "Share state is plain remember (not rememberSaveable): a saved Preparing would re-share by itself after process recreation, and sharing must start only on the tap"
  - "ShareCapture is a Box sibling of the action column, so the column's 8 dp spacing never counts the 0-size host"
  - "With the photo Unavailable the card hides both the legend chips and the 'Lines are drawn approximately' note (UI-SPEC says legend hidden; a note about lines without a photo would mislead)"

patterns-established:
  - "Share files: one name overwritten through a .tmp + Files.move(REPLACE_EXISTING, ATOMIC_MOVE), directory emptied in PalmApp.onCreate"

requirements-completed: [RES-06, RES-04]

coverage:
  - id: D1
    description: "Off-screen card renders 1080 x 1350 (photo Ready, Unavailable, 600-char summary), PNG decodes 1080 x 1350 with >= 50 colours on a 20 px grid, two writes leave only reading.png, host 0 x 0 and no card text in semantics"
    requirement: RES-06
    verification:
      - kind: integration
        ref: "connectedDebugAndroidTest ShareTest render cases (3) on emulator-5554, tests=\"0\" guard"
        status: pass
    human_judgment: false
  - id: D2
    description: "Send intent and chooser carry EXTRA_STREAM, EXTRA_TEXT, ClipData and FLAG_GRANT_READ_URI_PERMISSION; URI is content://<packageName>.share and readable; filesDir/readings throws; provider exported=false, grantUriPermissions=true"
    requirement: RES-06
    verification:
      - kind: integration
        ref: "connectedDebugAndroidTest ShareTest intent + provider cases (2) on emulator-5554"
        status: pass
    human_judgment: false
  - id: D3
    description: "In app: seeded reading from the Home card, tap Share -> reading.png 1080 x 1350 appears, the system chooser becomes the top activity, Back returns to Result with the button reading 'Share'"
    requirement: RES-06
    verification:
      - kind: integration
        ref: "connectedDebugAndroidTest ShareTest#tappingShareWritesTheCardAndOpensTheShareSheet on emulator-5554"
        status: pass
      - kind: e2e
        ref: "install-debug + result-open.sh + ui.sh scroll-to/tap Share + expect intentresolver activity + run-as ls cache/share = reading.png + back (emulator-5554)"
        status: pass
    human_judgment: false
  - id: D4
    description: "Card carries the disclaimer (3 lines max, never cut) and the branded footer"
    requirement: RES-04
    verification:
      - kind: other
        ref: "Pulled reading.png from the emulator and inspected: sky, orb + [AppName], photo with Heart/Head/Life/Sun overlay, legend, note, summary, traits, disclaimer, footer"
        status: pass
    human_judgment: false
  - id: D5
    description: "Sharing into a real app (WhatsApp/Instagram) on the MIUI phone; API 26-27 capture path (LayerSnapshotV22)"
    requirement: RES-06
    verification:
      - kind: other
        ref: "Phone session / 05-06"
        status: pending
    human_judgment: true
    rationale: "No physical phone in this run and no API 26-27 system image on this machine"

duration: 14min
completed: 2026-11-28
status: complete
---

# Phase 5 Plan 04: Share as a branded image card Summary

**Tapping Share on Result now renders an off-screen 1080 x 1350 branded card with Compose GraphicsLayer at a fixed density of 2.5 and font scale 1.0. The card has the sky, orb and [AppName], the photo with the same overlay, the legend, the summary, traits, disclaimer and footer. It is written as one PNG in cacheDir/share and opened in the system share sheet through a non-exported FileProvider with a per-URI read grant. Verified on emulator-5554.**

## Performance

- **Duration:** about 14 min (07:46 to 08:00 UTC)
- **Started:** 2026-11-28T07:46:00Z
- **Completed:** 2026-11-28T08:00:22Z
- **Tasks:** 2 of 2
- **Files:** 7 changed (+535 / -10)

## Accomplishments

- Task 1: `ShareCard` follows the UI-SPEC section 4 layout. Its maxLines are the only ones in Phase 5: title 2, legend note 2, summary 3 with weight(1f), so it yields space first, traits 1, disclaimer 3, footer 1. `ShareCapture` records the card into `rememberGraphicsLayer()` from a `Box(size(0.dp))` and never calls drawLayer. It waits for a `recorded` flag (snapshotFlow), then one frame, then `toImageBitmap()`. `writeSharePng` copies the HARDWARE bitmap to ARGB_8888, writes PNG 100 to `reading.png.tmp` and atomically moves it over `reading.png`.
- Task 2: `shareUri` builds the authority from `context.packageName + ".share"`. `shareIntent` sets ACTION_SEND, image/png, EXTRA_STREAM, EXTRA_TEXT, ClipData and the read grant, and `shareChooser` adds the same ClipData and grant to the chooser. The manifest has a `FileProvider` with `exported=false` and `grantUriPermissions=true`. `share_paths.xml` has a single `<cache-path name="share" path="share/"/>`. ResultActions shows Share as a Secondary button: Preparing… disables it, and a failure caption in StatusError appears with a polite live region and clears on the next tap. IOException, IllegalArgumentException and ActivityNotFoundException all lead to the failure state. `PalmApp.onCreate` deletes the files in `cacheDir/share`.

## Task Commits

1. **Task 1: Off-screen card and PNG** - `ef55d2e` (feat)
2. **Task 2: Share button, provider, intent, cleanup** - `122579c` (feat)

## Verification run

- `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...ShareTest` on emulator-5554: 3/3 after Task 1, then 6/6 after Task 2. The tests="0" guard held both times, and the APKs were left installed.
- Device chain exit 0: `install-debug.sh && result-open.sh && ui.sh scroll-to "^Share  \(" && ui.sh tap "^Share  " && ui.sh expect "activity: .*(...intentresolver)" && run-as ls cache/share = reading.png && ui.sh back`.
- `:app:assembleDebug :app:compileReleaseKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest` exit 0.
- All acceptance greps pass; `libs.versions.toml` is unchanged (no download, D-25; FileProvider comes from the pinned core 1.19.1).
- Screenshots checked: the pulled card PNG and Result after returning from the share sheet ("Share" idle between Scan again and Ask [Guide]).

## Capture path shipped

- **Compose GraphicsLayer** (RESEARCH A1 confirmed on API 35 / LayerSnapshotV28). The D-21 Canvas fallback was not built.
- **API 26-27 (LayerSnapshotV22, ImageReader) is unverified**: no system image here. This is the VALIDATION manual row.

## TDD Gate Compliance

- Task 1: RED was observed: all 3 render cases timed out with no capture. The cause was the test's own window: `ComponentActivity.setContent` wraps content, so a 0 x 0 window is never drawn. Wrapping the test host in `fillMaxSize` made them GREEN, and the 0-size `Box` host in ShareCapture stayed as planned. The semantics case then failed on the unmerged tree and passed on the merged tree (the tree accessibility reads).
- Task 2: the intent, URI and provider cases were written before Share.kt existed (compile RED, not run separately). The in-app case went through three red runs (see deviations) before GREEN.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Test-only: the render host needed a full-size window**
- **Found during:** Task 1
- **Issue:** Under `createAndroidComposeRule<ComponentActivity>`, setContent uses WRAP_CONTENT, so a tree whose only child is 0 x 0 gives a 0 x 0 ComposeView that Android never draws. The capture waited forever.
- **Fix:** In ShareTest, `Box(fillMaxSize) { Box(testTag) { ShareCapture } }`. No production change: on Result the ComposeView fills the screen.
- **Commit:** ef55d2e

**2. [Rule 1 - Test robustness] The semantics check reads the merged tree**
- clearAndSetSemantics hides the card's texts from the merged tree, which is what accessibility uses. The unmerged tree still lists them by design.

**3. [Rule 1 - Test robustness] Waiting for the real chooser before Back**
- **Issue:** Sending Back as soon as MainActivity paused closed the API 35 chooser (`intentresolver/.ChooserActivityLauncher`, which is the sheet itself) before it showed. Querying compose while Result was still stopped also threw "No compose hierarchies".
- **Fix:** Poll `dumpsys activity activities` until topResumedActivity matches chooser/resolver/intentresolver, then send Back through a shell `input keyevent`, then wait until `scenario.state` is RESUMED before looking for "Share". Strings come from the target context, not the rule activity.
- **Commit:** 122579c

**4. [Plan regex] ui.sh tap pattern**
- The plan's `tap "^Share  \("` can never match: ui.sh appends its own `(x,y)` match, so the pattern needs two parentheses (the 05-01 finding). The tap uses `"^Share  "`, and scroll-to keeps `"^Share  \("`.

## Known Stubs

None. ResultActions now uses `reading` and `photo`, which closes 05-01's intentional stub.

## Deferred to the phone session

- **Share into a real app on the MIUI phone:** open Result (the lab or a real scan), tap Share, pick WhatsApp or Gmail, and check that the 1080 x 1350 card arrives with "My palm reading from [AppName]". Also check that the MIUI share sheet opens (not a resolver error) and that Back returns to Result with "Share" idle.
- **API 26-27 capture path:** on any Android 8.0/8.1 device, tap Share and check that the card is not blank. If it is blank, ship the D-21 Canvas fallback.

## Environment note

- Another session's app (`com.meghquizango.pagewise`) was in the foreground on emulator-5554 and was reinstalled during this run. Our runs held `tools/device-lock.sh`, but that app does not use the lock. No test of ours was affected once the window-size issue was fixed.

## Threat Flags

None. T-05-12: the provider is not exported, its only path is cache-path share/, and the test asserts that filesDir/readings throws. T-05-13: one file overwritten atomically and emptied at start, asserted after two writes. T-05-14: a PNG from Bitmap.compress carries no EXIF. All are mitigated as planned.

## Self-Check: PASSED

- Files exist: ShareCard.kt, Share.kt, share_paths.xml, ShareTest.kt
- Commits on HEAD: ef55d2e, 122579c
