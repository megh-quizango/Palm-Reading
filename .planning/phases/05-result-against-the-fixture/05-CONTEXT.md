# Phase 5: Result Against the Fixture - Context

**Gathered:** 2026-11-28
**Status:** Ready for planning

<domain>
## Phase Boundary

The Phase 4 `ScanReady` placeholder gets a working "Read my palm" button. It hands the processed capture to a `PalmReadingRepository` whose Phase 5 implementation returns fixture readings (contract v1 JSON) for the real processed photo. A valid reading is saved on the device (Room 3 row + photo in `filesDir`) and opens the S10 result: the photo with the detected lines drawn over it, a colour + text legend, Palm / Love / Career tabs, one card per line, an entertainment disclaimer, Share (branded image card through the system share sheet), Scan again and "Ask [Guide]" (coming soon). An unusable-photo verdict opens a reason screen with a retake tip and a Retake button and saves nothing. Home shows the last reading and reopens it after a kill and relaunch. Requirements: RES-01..RES-07, READ-03, HOME-03.

Not in this phase: upload, the network repository, the S09 Generating screen, timeouts and busy/offline states, the free-scan counter and the rewarded gate (all Phase 6); a reading history list, deleting readings from the UI, chat context (later milestones). No ads.

</domain>

<decisions>
## Implementation Decisions

The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1). Decisions below are Claude's, grounded in `replica/PRD.md` §3 S10, §5.1 (R-PALM-4/6/8), §6 BL-1, §7.3, §8 `SavedPalmReading`, §10, §11.5, the frozen contract in `contracts/`, and the Phase 3/4 code.

### Hand-off from ScanReady (fixture now, network in Phase 6)
- **D-01:** New interface `PalmReadingRepository { suspend fun read(capture: ProcessedCapture, locale: AppLanguage): PalmReadingDto }` (throws on failure). Phase 5 binds `FixturePalmReadingRepository` with one `@Binds` in a Hilt module (`core/reading/ReadingModule.kt`); Phase 6 swaps that single binding for the network implementation. Nothing else in Result, store or Home knows which implementation runs.
- **D-02:** The fixture repository loads a fixture from app assets, then stamps it for the real capture: a fresh UUID `reading_id`, `hand_side` from the capture, `image_size` = the processed photo's width x height, `meta.locale` and `meta.generated_at` = now. Traces stay as the fixture's normalised points (they will not follow the user's real palm lines; Phase 6 brings real traces). No artificial delay: the button shows its loading state while read + save run (the Generating screen is Phase 6).
- **D-03:** `ScanReady` keeps its route and layout (Phase 4 D-01); "Read my palm" becomes enabled. On a valid verdict: save (D-17), then `replaceAll` the stack to `[Home, Result(readingId)]` so Back from Result goes Home. On an invalid verdict: pop `ScanReady`, push `InvalidPhoto(reason, hand)` so Back and Retake both land on `Scan(hand)`. A double tap cannot start two reads (the VM ignores a second call while one runs). A repository exception shows a short inline error with Retry on ScanReady (Phase 6 designs the full error states).
- **D-04:** Fixtures live in `android/app/src/main/assets/fixtures/`: `palm_reading.en.json` (byte copy of `contracts/palm_reading.sample.json`), `palm_reading.hi.json` (a new Hindi reading written fresh for this project, same structure and trace sources as the en sample) and `palm_reading.invalid.json` (copy of the invalid sample). Other variants are derived in code from these (copy with changed `trace_source`/`trace`/`reason`). A unit test asserts the en/invalid fixtures equal the `contracts/` samples (drift guard, via the existing `contracts.dir` property) and that every fixture and every derived variant decodes strictly and obeys the schema rules (none exactly when trace is empty, llm/template have >= 2 points, points in 0..1, `present=false` implies empty trace). Default in every build: valid reading in the app language (hi fixture when the app is in Hindi, else en).

### Debug fixture lab (every state reachable on the emulator)
- **D-05:** The emulator camera has no hand, so ScanReady cannot be reached by scanning there (04-05-SUMMARY). A debug-only "Fixture lab" screen in `src/debug` (opened from the Phase 1 Showcase, which the Home greeting long-press already opens) offers: a photo aspect (3:4, 1:1, 9:16; the photo is drawn in code: gradient + simple palm silhouette, no download, no real hand) and a fixture variant, then "Open ScanReady", which puts the generated JPEG into `ScanResultHolder` with dummy landmarks and pushes `ScanReady`. The same lab sets the variant used by the next real scan and has "Delete all readings".
- **D-06:** Variants (13): Valid · mixed sources (the sample: llm + template + one `none`), Valid · all llm, Valid · all template, Valid · all lines none (no overlay, legend hidden, every card "not clearly visible"), Valid · present but not drawn (`present=true`, `trace_source=none`), Valid · Hindi, and Invalid · each of the 7 reasons. The choice is an in-memory debug singleton read by the fixture repository through a small `FixtureChoice` seam whose release implementation always returns "default" (same release-null pattern as `debugHomeLongPress`).

### Result screen layout (RES-01, RES-03)
- **D-07:** Route `Result(readingId: String)` (sky background on). Top bar: back (to Home) and title "Your palm reading". Scrolling content, top to bottom: the photo with overlay (D-09), the legend (D-11), the disclaimer (D-13), the tab row, the tab content, then the action row (D-20). The photo, legend and disclaimer are above the tabs, so they show on every tab.
- **D-08:** Tabs Palm / Love / Career (localised), a Nocturne-styled tab row (M3 `PrimaryTabRow` restyled with tokens), selected tab kept with `rememberSaveable`, swipe between tabs not required. Palm tab: summary card, traits as chips, then one line card per line in contract order (Heart, Head, Life, Fate, Sun). Love tab: three cards (How you love, Ideal partner, Challenges). Career tab: Strengths and Challenges cards plus Ideal industries as chips. Card titles for the sections are app strings (en/hi); the bodies are the reading text. A line card shows a colour dot in the line's colour, the server `label` as title (falling back to the localised line name if blank), the interpretation, and a badge: "Approximate" for `llm`/`template`, "Not clearly visible" when not drawn (`none` or `present=false`).

### Overlay maths and look (RES-02)
- **D-09:** The photo is shown with `ContentScale.Fit` inside a box of the photo's own aspect ratio, full content width, height capped at about 55% of the window (letterboxed when capped). A pure-Kotlin `fitRect(srcW, srcH, dstW, dstH)` gives the drawn image rect; a trace point `[x, y]` maps to `rect.left + x * rect.width, rect.top + y * rect.height`. Traces are normalised to the image sent to the model (schema), which is the processed photo (same aspect), so `image_size` is not used for scaling; a debug-only log line flags an `image_size` aspect that differs from the photo's by more than 2%. Points are clamped to 0..1 before mapping.
- **D-10:** A line is drawn iff `trace_source != none` and the trace has >= 2 points. Smoothing: Catmull-Rom through the points converted to cubic Béziers (pure function, unit-tested; 2 points = a straight segment). Stroke 3 dp, round caps and joins, solid (every M1 line is approximate, so dashing would mark all of them), in the token colours `PalmHeart`, `PalmHead`, `PalmLife`, `PalmFate`, `PalmSun`, over a soft glow (same path, about 9 dp, about 30% alpha) so lines read on light and dark skin. The lines draw in once (staggered path trim, about 900 ms total) and are static under reduce motion (`LocalNocturne.current.reduceMotion`).
- **D-11:** Legend under the photo: one chip per drawn line, colour swatch + localised line name ("Heart", "Head", "Life", "Fate", "Sun"), so colour is never the only cue. When any drawn line is approximate, one muted note follows: "Lines are drawn approximately." Lines that are not drawn are not in the legend (their cards say so). The overlay has one content description listing the drawn lines for TalkBack.
- **D-12:** Missing or undecodable photo: the reading still opens, the photo box shows a "Photo unavailable" placeholder, no overlay, legend hidden; cards, tabs and actions work. Missing row or JSON that fails strict decode: a state block "We couldn't open this reading" with "Back to Home" (no crash).

### Disclaimer (RES-04)
- **D-13:** Wording en: "For entertainment only. Palm reading is not a prediction and is not medical, legal or financial advice." (hi: a natural translation with the same meaning). Placed on Result under the legend, above the tabs (visible on every tab without switching), body-small in `TextMuted` on the sky/background (well above 4.5:1; verify with the contrast check the design skill uses), and on the share card. Not shown on the invalid-photo screen (no reading there).

### Unusable photo (READ-03)
- **D-14:** Route `InvalidPhoto(reason: InvalidReason, hand: HandSide)` (sky background on). Layout: top bar "Photo not accepted", a `NocturneStateBlock` with a reason icon, the reason title and its retake tip, primary "Retake photo" (pops to `Scan(hand)`; the capture is cleared) and secondary "Back to Home". Copy (en; hi translated): blurry_or_dark "Your photo is too blurry or dark" / "Hold the phone steady in good light and let it focus."; too_far "Your hand is too far away" / "Move closer so your palm fills the frame."; jewelry "Jewellery is covering your palm" / "Take off rings and bracelets, then try again."; bent "Your hand is bent" / "Hold your hand flat and facing the camera."; fingers_tight "Your fingers are too close together" / "Spread your fingers a little apart."; incomplete "Part of your palm is missing" / "Fit your whole palm, wrist to fingertips, in the frame."; no_palm_lines "We couldn't see your palm lines" / "Use brighter light and keep your palm open and flat." Reason to string mapping is an exhaustive `when`, unit-tested for all 7.
- **D-15:** "Not counted as a completed scan" means concretely: an invalid verdict writes no Room row and no file in `filesDir`; the capture in `cacheDir/scan` is deleted when the user leaves InvalidPhoto (and at app start, as today); Home's last reading is unchanged. In this codebase a completed scan is exactly a successful `ReadingStore.save`. Phase 6's free-scan counter (PRD BL-1) must be incremented only in that save path, never on read or on an invalid verdict; this is recorded as a constraint for Phase 6.

### Storage (RES-05, HOME-03)
- **D-16:** `ReadingEntity(id = reading_id PK, schemaVersion, json, photoFile, handSide, createdAt)` with an index on `createdAt`. `json` is the whole contract JSON (the source of truth; decoded strictly on read). `photoFile` is a file name relative to `filesDir/readings/`. DB version 2 with `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`: v1 only ever existed on debug installs and no code path wrote rows (Phase 1 test only), so dropping it is safe, and it avoids crashing debug installs that cannot be uninstalled (MIUI lesson). Commit `schemas/.../2.json`; an instrumented test opens a v1 database and checks the v2 open succeeds. DAO: `upsert` (`REPLACE`, because Phase 6's 24 h per-image cache can return the same `reading_id`), `byId(id): Flow`, `latest(): Flow` (`ORDER BY createdAt DESC LIMIT 1`), `delete(id)`, `all()` (lab/tests).
- **D-17:** `ReadingStore.save(reading, capture)`: move `cacheDir/scan/capture.jpg` to `filesDir/readings/<readingId>.jpg` (atomic move, copy+delete fallback), then upsert the row; if the insert fails the moved file is deleted. Then the holder is cleared. `delete(id)` removes the row and the file. Retention: keep every reading, no cap (simplest that meets RES-05 and HOME-03; storage cap belongs with a history screen).
- **D-18:** Privacy: photos and readings stay in app-private storage (backup is already off: `allowBackup=false`, data-extraction rules), never logged (no reading text, traces or image bytes in logs, including debug), and deleting a reading deletes its photo. No delete UI in this phase; the lab's "Delete all readings" and tests exercise `delete`.
- **D-19:** Home: when `latest()` is non-null, a "Your last reading" card appears under the hero card: small photo thumbnail (decoded with `inSampleSize`), "Palm reading", hand and date (localised, `java.time`), chevron; tap pushes `Result(id)`. Hidden when there is none; the Palm tile still starts a scan. Reopening works after `am force-stop` + relaunch because everything is read from Room and `filesDir`.

### Share, Scan again, Ask [Guide] (RES-06, RES-07)
- **D-20:** Action row at the bottom of Result: primary "Scan again" (pushes `HandSelect`; when the next reading saves, the stack is replaced as in D-03), secondary "Share" and "Ask [Guide]". "Ask [Guide]" opens the existing `ComingSoon(Feature.ASK_GUIDE)` bottom sheet (no new screen).
- **D-21:** Share card: a `ShareCard` composable at a fixed 1080 x 1350 px (4:5), Nocturne background, `[AppName]` wordmark and orb, the photo with the same overlay code and legend, "My palm reading", the palm summary and up to 3 traits, the disclaimer, and "Read your palm in [AppName]" (no store link yet). Rendered to a bitmap with Compose `GraphicsLayer` (`rememberGraphicsLayer` + `record` + `toImageBitmap`), composed only while sharing; if off-screen capture proves unreliable on the emulator, the fallback is drawing the card on an `android.graphics.Canvas` with the same maths (research decides, the look stays).
- **D-22:** Mechanism: `Bitmap.compress(PNG)` (writes no EXIF) to `cacheDir/share/reading.png`, `androidx.core.content.FileProvider` (already on the classpath via core-ktx) with authority `${applicationId}.share` and a `cache-path` limited to `share/`, `ACTION_SEND`, type `image/png`, `EXTRA_STREAM` + `ClipData`, `FLAG_GRANT_READ_URI_PERMISSION`, a one-line `EXTRA_TEXT`, through `Intent.createChooser`. The share directory is cleared at app start next to the scan directory. Sharing includes the user's own palm photo; it only ever happens on their tap.

### Strings, tests, device, downloads
- **D-23:** Every new UI string in `values` and `values-hi` (new `strings_result.xml`); reading text comes from the reading's own locale and is not translated by the app. `[AppName]` and `[Guide]` stay placeholders.
- **D-24:** Tests. Unit: `fitRect` and point mapping for 3:4, 1:1, 9:16 and a letterboxed case; Catmull-Rom output; fixture drift + strict decode + schema rules for every variant (D-04/D-06); fixture stamping (id, hand, size, locale); draw/badge rules per `trace_source`; reason mapping for all 7; ScanReady VM (valid -> save + navigate, invalid -> no save, double tap, error); Result VM (found, missing row, bad JSON, missing photo). Instrumented: `ReadingStore` with an in-memory/file DB (save moves the photo, delete removes both, insert failure leaves no file), v1 -> v2 open; Compose UI tests for tab switching with the disclaimer visible on each tab, the overlay drawn at the expected pixels on the 3 aspect ratios (sample pixel colour at mapped trace points on a plain test photo), the invalid screen for each reason with Retake, Home last-reading card appearing and opening; share intent (content URI, grant flag, PNG decodes). On the emulator (emulator-5554, debug build; no phone needed): walk every lab variant in en and hi with screenshots, kill + relaunch then reopen from Home, share to a target in the system sheet.
- **D-25:** Downloads: none expected (FileProvider is in androidx.core, Room 3, Compose, kotlinx.serialization are present). If research finds one is needed, the first plan starts with a consent checkpoint naming each artifact before anything is fetched.

### Claude's Discretion
- Package layout (e.g. `core/reading` for repository/store/fixtures, `feature/result`, `feature/invalidphoto`), exact spacing, icons per reason, chip/badge styling, animation easing, the generated lab photo's drawing, Hindi phrasing, the exact Home card styling, and test split, as long as D-01..D-25 hold.

</decisions>

<specifics>
## Specific Ideas

- MVP tracer first: lab photo -> ScanReady -> fixture -> save -> Result Palm tab with overlay -> kill/relaunch -> Home reopen. Then Love/Career tabs, legend/disclaimer polish, invalid screens, share, Hindi.
- The Figma "3 · Screens" page has a "Palm reading (S08-S10)" section; the UI researcher maps any S10 frames there and builds what is missing from tokens (Phase 4 UI-SPEC noted the invalid-photo screens are not in Figma).

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `.planning/ROADMAP.md` §Phase 5 (goal, 5 success criteria)
- `.planning/REQUIREMENTS.md` (RES-01..07, READ-03, HOME-03)
- `.planning/PROJECT.md`, `.planning/STATE.md` (Phase 4 status: 04-05 phone part still open; Phase 2 template mode)
- `contracts/palm_reading.v1.schema.json`, `contracts/palm_reading.sample.json`, `contracts/palm_reading.invalid_photo.sample.json` (trace rules, 7 reasons)
- `android/app/src/main/java/app/palm/contract/PalmReadingDto.kt` (strict DTOs, enums)
- `replica/PRD.md` §3 S10, §5.1 (R-PALM-4, R-PALM-6, R-PALM-8), §6 BL-1, §7.3, §8 `SavedPalmReading`, §10, §11.5
- `.planning/phases/04-scan-loop/04-CONTEXT.md` (D-01 ScanReady/holder, D-12 capture lifetime), `04-UI-SPEC.md`, `04-05-SUMMARY.md` (emulator limits)
- `.planning/phases/03-client-shell/03-CONTEXT.md`, `03-UI-SPEC.md` (Home, ComingSoon sheet, MIUI lessons, tools/ui.sh)
- `.claude/CLAUDE.md` (pinned stack, Room 3, privacy and policy constraints)
- Figma "[AppName] – UI" (file key `Ej98dV7fZQUhRwUyfQIlyJ`), page "3 · Screens" S10; `design-system/nocturne.tokens.json`
- Clean room: never copy from `_extract/`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `core/photo/ScanResultHolder` + `ProcessedCapture` (file in `cacheDir/scan`, size, hand, landmarks); `PalmApp` clears it at start.
- `core/db/AppDatabase.kt`: Room 3 `ReadingEntity` v1 (id, schemaVersion, json, createdAt), `BundledSQLiteDriver`, schema export at `android/app/schemas/`; comment says Phase 5 owns the final columns.
- Design system: `NocturneColors.Palm*` line colours, `NocturneButton`, `NocturneChip`, `NocturneStateBlock`, `NocturneTopBar`, `NocturneDialog`, `FeatureCard`, `Effects.kt` (`nocturneGlow`, `rasterizedBehind`), `LocalNocturne.reduceMotion`.
- `feature/home/ComingSoonSheet` handles `Feature.ASK_GUIDE`; `HeroCard`, `HomeViewModel` (add the latest-reading flow).
- `feature/scan/ScanReadyScreen` + `ScanReadyViewModel` (photo decode, gone state, retake).
- Tests: `contracts.dir` system property for unit tests, `src/test/.../testing/Fake*` fakes, `androidTest/assets/photos`, `AppDatabaseTest`.
- Debug hooks: `src/debug/.../DebugEntries.kt` (`debugHomeLongPress`, Showcase), `ScanDebug.kt`.

### Established Patterns
- Nav3 `Route` sealed interface with `showsBackground`, `pushFrom`/`popFrom`/`replaceAll` guards against double taps.
- Hilt `@Provides`/`@Binds` in `SingletonComponent`; blocking I/O on `Dispatchers.IO`; `NonCancellable` for cleanup on pop.
- Strings split per feature file in `values` / `values-hi`.

### Integration Points
- `ScanEntries.kt` (`ScanReady` entry gets the read action and navigation), `Routes.kt` (`Result`, `InvalidPhoto`), `HomeEntries.kt`/`HomeScreen.kt` (last-reading card), `PalmApp` (clear share dir), `AndroidManifest.xml` (FileProvider), `res/xml` (file paths).

</code_context>

<deferred>
## Deferred Ideas

- Network repository, Generating screen, timeouts, busy/offline states, free-scan counter and rewarded gate (Phase 6; counter increments only on save, D-15).
- Reading history list, delete-reading UI, storage cap (later milestone).
- Chat with the last reading as context; "report this reading" flag from PRD S10; rating prompt after results.
- Traces that follow the real palm (Phase 6 backend / later segmentation model); landmark-anchored fixture traces were considered and skipped.
- Play store link on the share card (needs a published package).

</deferred>

---

*Phase: 05-result-against-the-fixture*
*Context gathered: 2026-11-28*
