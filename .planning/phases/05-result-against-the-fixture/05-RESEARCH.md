# Phase 5: Result Against the Fixture - Research

**Researched:** 2026-11-28
**Domain:** Android Compose result screen (photo overlay, tabs, share card), Room 3 persistence, Nav3 routing, fixture repository seam
**Confidence:** HIGH for APIs and in-repo facts (read from the source files and the cached AARs this session); MEDIUM for off-screen share-card capture (documented API, the off-screen placement is unverified on device)

## Summary

Phase 5 needs **no new Maven artifact**. Every API the decisions name is already on the classpath and was confirmed by `javap` on the cached AARs this session: Room 3.0.3 `fallbackToDestructiveMigrationFrom(boolean, int...)`, `OnConflictStrategy.REPLACE`, `@Upsert`, `@Index` and `Flow` DAO returns; Compose UI 1.12.1 `GraphicsLayer.record`/`toImageBitmap`, `rememberGraphicsLayer`, `PathMeasure.getSegment`; Material3 1.4.0 `PrimaryTabRow`/`Tab`/`TabRowDefaults.PrimaryIndicator`; androidx.core 1.19.1 `FileProvider.getUriForFile`; androidx.hilt 1.4.0 `hiltViewModel(creationCallback)` for assisted route arguments. D-25's consent checkpoint is therefore not needed.

The overlay maths are small and pure. `ContentScale.Fit` gives scale `s = min(dstW/srcW, dstH/srcH)`, centred. A normalised point maps to `rect.left + x*rect.width, rect.top + y*rect.height`. Catmull-Rom to cubic Bézier conversion is affine-invariant, so smoothing can run in normalised space and the control points can be mapped with the same rect. Catmull-Rom passes through every trace point, so a pixel test can sample the line colour exactly at the mapped trace points. The one real visual risk is contrast: the five palm line colours sit at only **1.03 to 1.50:1 against light skin** (computed this session), so the D-10 "glow" must be a **dark halo** under the coloured stroke, not a glow in the line's own colour.

The main correctness traps are not in the libraries. They are: (1) `readingId` comes from the server in Phase 6 and becomes a file name, so it must be validated as a UUID before `File(dir, "$id.jpg")` (path traversal); (2) the Back and Retake handlers on ScanReady call `holder.clear()`, which can race the file move during save; (3) on API 28+, `GraphicsLayer.toImageBitmap()` returns a **HARDWARE** bitmap (Compose uses `Bitmap.createBitmap(Picture)`), so copy it to `ARGB_8888` before `compress`; (4) the share card must be laid out under a fixed `Density(3f, fontScale = 1f)`, or the device density and font scale 2.0 change the 1080x1350 layout; (5) `ScanReadyViewModel` decodes with `BitmapFactory` in `init`, and that call throws "not mocked" in JVM tests, so the read flow needs a decode seam to be unit-testable.

**Primary recommendation:** Build the tracer in this order: `ReadingRules` + fixtures + drift test, then `ReadingEntity` v2 + `ReadingStore`, then the ScanReady read action, then the Result Palm tab with `PalmOverlay`, then Home reopen. Use only the existing stack, pure-Kotlin maths with JVM tests, and Compose `captureToImage` pixel tests on emulator-5554.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
The user asked Claude to decide gray areas ("use your best approach instead of discussing", standing since Phase 1). Decisions below are Claude's, grounded in `replica/PRD.md` §3 S10, §5.1 (R-PALM-4/6/8), §6 BL-1, §7.3, §8 `SavedPalmReading`, §10, §11.5, the frozen contract in `contracts/`, and the Phase 3/4 code.

#### Hand-off from ScanReady (fixture now, network in Phase 6)
- **D-01:** New interface `PalmReadingRepository { suspend fun read(capture: ProcessedCapture, locale: AppLanguage): PalmReadingDto }` (throws on failure). Phase 5 binds `FixturePalmReadingRepository` with one `@Binds` in a Hilt module (`core/reading/ReadingModule.kt`); Phase 6 swaps that single binding for the network implementation. Nothing else in Result, store or Home knows which implementation runs.
- **D-02:** The fixture repository loads a fixture from app assets, then stamps it for the real capture: a fresh UUID `reading_id`, `hand_side` from the capture, `image_size` = the processed photo's width x height, `meta.locale` and `meta.generated_at` = now. Traces stay as the fixture's normalised points (they will not follow the user's real palm lines; Phase 6 brings real traces). No artificial delay: the button shows its loading state while read + save run (the Generating screen is Phase 6).
- **D-03:** `ScanReady` keeps its route and layout (Phase 4 D-01); "Read my palm" becomes enabled. On a valid verdict: save (D-17), then `replaceAll` the stack to `[Home, Result(readingId)]` so Back from Result goes Home. On an invalid verdict: pop `ScanReady`, push `InvalidPhoto(reason, hand)` so Back and Retake both land on `Scan(hand)`. A double tap cannot start two reads (the VM ignores a second call while one runs). A repository exception shows a short inline error with Retry on ScanReady (Phase 6 designs the full error states).
- **D-04:** Fixtures live in `android/app/src/main/assets/fixtures/`: `palm_reading.en.json` (byte copy of `contracts/palm_reading.sample.json`), `palm_reading.hi.json` (a new Hindi reading written fresh for this project, same structure and trace sources as the en sample) and `palm_reading.invalid.json` (copy of the invalid sample). Other variants are derived in code from these (copy with changed `trace_source`/`trace`/`reason`). A unit test asserts the en/invalid fixtures equal the `contracts/` samples (drift guard, via the existing `contracts.dir` property) and that every fixture and every derived variant decodes strictly and obeys the schema rules (none exactly when trace is empty, llm/template have >= 2 points, points in 0..1, `present=false` implies empty trace). Default in every build: valid reading in the app language (hi fixture when the app is in Hindi, else en).

#### Debug fixture lab (every state reachable on the emulator)
- **D-05:** The emulator camera has no hand, so ScanReady cannot be reached by scanning there (04-05-SUMMARY). A debug-only "Fixture lab" screen in `src/debug` (opened from the Phase 1 Showcase, which the Home greeting long-press already opens) offers: a photo aspect (3:4, 1:1, 9:16; the photo is drawn in code: gradient + simple palm silhouette, no download, no real hand) and a fixture variant, then "Open ScanReady", which puts the generated JPEG into `ScanResultHolder` with dummy landmarks and pushes `ScanReady`. The same lab sets the variant used by the next real scan and has "Delete all readings".
- **D-06:** Variants (13): Valid · mixed sources (the sample: llm + template + one `none`), Valid · all llm, Valid · all template, Valid · all lines none (no overlay, legend hidden, every card "not clearly visible"), Valid · present but not drawn (`present=true`, `trace_source=none`), Valid · Hindi, and Invalid · each of the 7 reasons. The choice is an in-memory debug singleton read by the fixture repository through a small `FixtureChoice` seam whose release implementation always returns "default" (same release-null pattern as `debugHomeLongPress`).

#### Result screen layout (RES-01, RES-03)
- **D-07:** Route `Result(readingId: String)` (sky background on). Top bar: back (to Home) and title "Your palm reading". Scrolling content, top to bottom: the photo with overlay (D-09), the legend (D-11), the disclaimer (D-13), the tab row, the tab content, then the action row (D-20). The photo, legend and disclaimer are above the tabs, so they show on every tab.
- **D-08:** Tabs Palm / Love / Career (localised), a Nocturne-styled tab row (M3 `PrimaryTabRow` restyled with tokens), selected tab kept with `rememberSaveable`, swipe between tabs not required. Palm tab: summary card, traits as chips, then one line card per line in contract order (Heart, Head, Life, Fate, Sun). Love tab: three cards (How you love, Ideal partner, Challenges). Career tab: Strengths and Challenges cards plus Ideal industries as chips. Card titles for the sections are app strings (en/hi); the bodies are the reading text. A line card shows a colour dot in the line's colour, the server `label` as title (falling back to the localised line name if blank), the interpretation, and a badge: "Approximate" for `llm`/`template`, "Not clearly visible" when not drawn (`none` or `present=false`).

#### Overlay maths and look (RES-02)
- **D-09:** The photo is shown with `ContentScale.Fit` inside a box of the photo's own aspect ratio, full content width, height capped at about 55% of the window (letterboxed when capped). A pure-Kotlin `fitRect(srcW, srcH, dstW, dstH)` gives the drawn image rect; a trace point `[x, y]` maps to `rect.left + x * rect.width, rect.top + y * rect.height`. Traces are normalised to the image sent to the model (schema), which is the processed photo (same aspect), so `image_size` is not used for scaling; a debug-only log line flags an `image_size` aspect that differs from the photo's by more than 2%. Points are clamped to 0..1 before mapping.
- **D-10:** A line is drawn iff `trace_source != none` and the trace has >= 2 points. Smoothing: Catmull-Rom through the points converted to cubic Béziers (pure function, unit-tested; 2 points = a straight segment). Stroke 3 dp, round caps and joins, solid (every M1 line is approximate, so dashing would mark all of them), in the token colours `PalmHeart`, `PalmHead`, `PalmLife`, `PalmFate`, `PalmSun`, over a soft glow (same path, about 9 dp, about 30% alpha) so lines read on light and dark skin. The lines draw in once (staggered path trim, about 900 ms total) and are static under reduce motion (`LocalNocturne.current.reduceMotion`).
- **D-11:** Legend under the photo: one chip per drawn line, colour swatch + localised line name ("Heart", "Head", "Life", "Fate", "Sun"), so colour is never the only cue. When any drawn line is approximate, one muted note follows: "Lines are drawn approximately." Lines that are not drawn are not in the legend (their cards say so). The overlay has one content description listing the drawn lines for TalkBack.
- **D-12:** Missing or undecodable photo: the reading still opens, the photo box shows a "Photo unavailable" placeholder, no overlay, legend hidden; cards, tabs and actions work. Missing row or JSON that fails strict decode: a state block "We couldn't open this reading" with "Back to Home" (no crash).

#### Disclaimer (RES-04)
- **D-13:** Wording en: "For entertainment only. Palm reading is not a prediction and is not medical, legal or financial advice." (hi: a natural translation with the same meaning). Placed on Result under the legend, above the tabs (visible on every tab without switching), body-small in `TextMuted` on the sky/background (well above 4.5:1; verify with the contrast check the design skill uses), and on the share card. Not shown on the invalid-photo screen (no reading there).

#### Unusable photo (READ-03)
- **D-14:** Route `InvalidPhoto(reason: InvalidReason, hand: HandSide)` (sky background on). Layout: top bar "Photo not accepted", a `NocturneStateBlock` with a reason icon, the reason title and its retake tip, primary "Retake photo" (pops to `Scan(hand)`; the capture is cleared) and secondary "Back to Home". Copy (en; hi translated): blurry_or_dark "Your photo is too blurry or dark" / "Hold the phone steady in good light and let it focus."; too_far "Your hand is too far away" / "Move closer so your palm fills the frame."; jewelry "Jewellery is covering your palm" / "Take off rings and bracelets, then try again."; bent "Your hand is bent" / "Hold your hand flat and facing the camera."; fingers_tight "Your fingers are too close together" / "Spread your fingers a little apart."; incomplete "Part of your palm is missing" / "Fit your whole palm, wrist to fingertips, in the frame."; no_palm_lines "We couldn't see your palm lines" / "Use brighter light and keep your palm open and flat." Reason to string mapping is an exhaustive `when`, unit-tested for all 7.
- **D-15:** "Not counted as a completed scan" means concretely: an invalid verdict writes no Room row and no file in `filesDir`; the capture in `cacheDir/scan` is deleted when the user leaves InvalidPhoto (and at app start, as today); Home's last reading is unchanged. In this codebase a completed scan is exactly a successful `ReadingStore.save`. Phase 6's free-scan counter (PRD BL-1) must be incremented only in that save path, never on read or on an invalid verdict; this is recorded as a constraint for Phase 6.

#### Storage (RES-05, HOME-03)
- **D-16:** `ReadingEntity(id = reading_id PK, schemaVersion, json, photoFile, handSide, createdAt)` with an index on `createdAt`. `json` is the whole contract JSON (the source of truth; decoded strictly on read). `photoFile` is a file name relative to `filesDir/readings/`. DB version 2 with `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)`: v1 only ever existed on debug installs and no code path wrote rows (Phase 1 test only), so dropping it is safe, and it avoids crashing debug installs that cannot be uninstalled (MIUI lesson). Commit `schemas/.../2.json`; an instrumented test opens a v1 database and checks the v2 open succeeds. DAO: `upsert` (`REPLACE`, because Phase 6's 24 h per-image cache can return the same `reading_id`), `byId(id): Flow`, `latest(): Flow` (`ORDER BY createdAt DESC LIMIT 1`), `delete(id)`, `all()` (lab/tests).
- **D-17:** `ReadingStore.save(reading, capture)`: move `cacheDir/scan/capture.jpg` to `filesDir/readings/<readingId>.jpg` (atomic move, copy+delete fallback), then upsert the row; if the insert fails the moved file is deleted. Then the holder is cleared. `delete(id)` removes the row and the file. Retention: keep every reading, no cap (simplest that meets RES-05 and HOME-03; storage cap belongs with a history screen).
- **D-18:** Privacy: photos and readings stay in app-private storage (backup is already off: `allowBackup=false`, data-extraction rules), never logged (no reading text, traces or image bytes in logs, including debug), and deleting a reading deletes its photo. No delete UI in this phase; the lab's "Delete all readings" and tests exercise `delete`.
- **D-19:** Home: when `latest()` is non-null, a "Your last reading" card appears under the hero card: small photo thumbnail (decoded with `inSampleSize`), "Palm reading", hand and date (localised, `java.time`), chevron; tap pushes `Result(id)`. Hidden when there is none; the Palm tile still starts a scan. Reopening works after `am force-stop` + relaunch because everything is read from Room and `filesDir`.

#### Share, Scan again, Ask [Guide] (RES-06, RES-07)
- **D-20:** Action row at the bottom of Result: primary "Scan again" (pushes `HandSelect`; when the next reading saves, the stack is replaced as in D-03), secondary "Share" and "Ask [Guide]". "Ask [Guide]" opens the existing `ComingSoon(Feature.ASK_GUIDE)` bottom sheet (no new screen).
- **D-21:** Share card: a `ShareCard` composable at a fixed 1080 x 1350 px (4:5), Nocturne background, `[AppName]` wordmark and orb, the photo with the same overlay code and legend, "My palm reading", the palm summary and up to 3 traits, the disclaimer, and "Read your palm in [AppName]" (no store link yet). Rendered to a bitmap with Compose `GraphicsLayer` (`rememberGraphicsLayer` + `record` + `toImageBitmap`), composed only while sharing; if off-screen capture proves unreliable on the emulator, the fallback is drawing the card on an `android.graphics.Canvas` with the same maths (research decides, the look stays).
- **D-22:** Mechanism: `Bitmap.compress(PNG)` (writes no EXIF) to `cacheDir/share/reading.png`, `androidx.core.content.FileProvider` (already on the classpath via core-ktx) with authority `${applicationId}.share` and a `cache-path` limited to `share/`, `ACTION_SEND`, type `image/png`, `EXTRA_STREAM` + `ClipData`, `FLAG_GRANT_READ_URI_PERMISSION`, a one-line `EXTRA_TEXT`, through `Intent.createChooser`. The share directory is cleared at app start next to the scan directory. Sharing includes the user's own palm photo; it only ever happens on their tap.

#### Strings, tests, device, downloads
- **D-23:** Every new UI string in `values` and `values-hi` (new `strings_result.xml`); reading text comes from the reading's own locale and is not translated by the app. `[AppName]` and `[Guide]` stay placeholders.
- **D-24:** Tests. Unit: `fitRect` and point mapping for 3:4, 1:1, 9:16 and a letterboxed case; Catmull-Rom output; fixture drift + strict decode + schema rules for every variant (D-04/D-06); fixture stamping (id, hand, size, locale); draw/badge rules per `trace_source`; reason mapping for all 7; ScanReady VM (valid -> save + navigate, invalid -> no save, double tap, error); Result VM (found, missing row, bad JSON, missing photo). Instrumented: `ReadingStore` with an in-memory/file DB (save moves the photo, delete removes both, insert failure leaves no file), v1 -> v2 open; Compose UI tests for tab switching with the disclaimer visible on each tab, the overlay drawn at the expected pixels on the 3 aspect ratios (sample pixel colour at mapped trace points on a plain test photo), the invalid screen for each reason with Retake, Home last-reading card appearing and opening; share intent (content URI, grant flag, PNG decodes). On the emulator (emulator-5554, debug build; no phone needed): walk every lab variant in en and hi with screenshots, kill + relaunch then reopen from Home, share to a target in the system sheet.
- **D-25:** Downloads: none expected (FileProvider is in androidx.core, Room 3, Compose, kotlinx.serialization are present). If research finds one is needed, the first plan starts with a consent checkpoint naming each artifact before anything is fetched.

### Claude's Discretion
- Package layout (e.g. `core/reading` for repository/store/fixtures, `feature/result`, `feature/invalidphoto`), exact spacing, icons per reason, chip/badge styling, animation easing, the generated lab photo's drawing, Hindi phrasing, the exact Home card styling, and test split, as long as D-01..D-25 hold.

### Deferred Ideas (OUT OF SCOPE)
- Network repository, Generating screen, timeouts, busy/offline states, free-scan counter and rewarded gate (Phase 6; counter increments only on save, D-15).
- Reading history list, delete-reading UI, storage cap (later milestone).
- Chat with the last reading as context; "report this reading" flag from PRD S10; rating prompt after results.
- Traces that follow the real palm (Phase 6 backend / later segmentation model); landmark-anchored fixture traces were considered and skipped.
- Play store link on the share card (needs a published package).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| RES-01 | User can switch between Palm, Love and Career tabs on the result | M3 1.4.0 `PrimaryTabRow` + `Tab` verified; `rememberSaveable` index; Pattern 4 |
| RES-02 | User sees the detected lines drawn over their own photo with a colour + text legend | `fitRect` + Catmull-Rom (Patterns 1-2), dark halo for contrast (Pitfall 1), `PathMeasure.getSegment` trim, legend chips via existing `NocturneChip(dotColor=)` |
| RES-03 | User sees a card per detected line with a short label and interpretation | `LineType` order + draw/badge rule function (Pattern 3); `present=true` + `none` is a valid contract state |
| RES-04 | Every reading shows an entertainment disclaimer | TextMuted on sky 8.19-9.43:1 (computed), above the tabs; share card carries it too |
| RES-05 | The reading (photo + JSON) is saved on the device and survives app restarts | Room 3 v2 entity/DAO (verified APIs), `Files.move` ATOMIC same filesystem, raw-SQL v1 fixture for the migration test (Pattern 5) |
| RES-06 | User can share the reading as a branded image card | GraphicsLayer capture with fixed Density, HARDWARE to ARGB copy, FileProvider (verified in core 1.19.1), chooser (Pattern 6) |
| RES-07 | User can start another scan from the result; "Ask [Guide]" shows a coming-soon state | Existing `ComingSoon(Feature.ASK_GUIDE)` entry is global and opens over any route; `pushFrom(resultKey, ...)` |
| READ-03 | The user sees one of 7 reasons with a retake tip; the attempt does not use up the free scan | `InvalidReason` 7 values verified; no counter exists today (grep), so "not counted" = no `save`; clear the capture at the verdict |
| HOME-03 | User can reopen their last palm reading from Home | `latest(): Flow` in Room 3 (Flow returns allowed without suspend), thumbnail via the existing `sampleSizeFor` |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- Stack is fixed: Kotlin, Compose, Nav3 (`navigation3` 1.2.2), Hilt via KSP (never kapt), **Room 3 only** (`androidx.room3`, never mix with `androidx.room`), DataStore, kotlinx.serialization, Coil 3 only if remote images appear (not needed: local photos only).
- No `org.jetbrains.kotlin.android` plugin, no `android.disallowKotlinSourceSets=false`, no `material-icons-extended`, no Gson/Moshi/Glide, no Accompanist.
- Privacy: no biometric templates stored; readings and photos stay private; never log image bytes or reading text. (Phase 4 rule: landmarks never stored. The Room row must not contain the capture's `landmarks`.)
- Policy: entertainment disclaimer on every reading; no medical, legal or financial directives.
- Performance: cold start < 2 s, so `PalmApp.onCreate` work must stay a directory listing (no DB query on main).
- Secrets: no API key in the app (not touched in this phase).
- GSD workflow: edits only through GSD commands; clean room: never copy from `_extract/`.
- Task constraints for this run: no Gradle builds, no downloads, no commits, nothing installed on the phone.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Reading content (fixture now) | Client data layer (`core/reading` repository) | Backend (Phase 6) | D-01 seam; Phase 6 swaps one `@Binds` |
| Contract validation (schema rules) | Client data layer (`ReadingRules`) | Backend (already validates) | Trust boundary: Phase 6 data arrives from the network |
| Persistence (row + photo) | Device storage (Room 3 + `filesDir/readings`) | — | RES-05, private storage, backup off |
| Overlay geometry | Pure Kotlin (`core/reading` or `feature/result`) | Compose Canvas (draw only) | JVM-testable maths, UI only strokes paths |
| Tabs, cards, legend, disclaimer | Compose UI (`feature/result`) | — | Presentation only |
| Share card render + intent | Compose UI + Android framework (FileProvider, chooser) | — | Off-screen render, system share sheet |
| Unusable photo screen | Compose UI (`feature/invalidphoto`) | — | Stateless; reason arrives in the route |
| Last-reading card | Compose UI (Home) reading a DAO Flow | — | HOME-03 |
| Fixture lab | Debug source set only | — | D-05/D-06; release twin is a no-op |

## Standard Stack

Everything below is already declared in `android/gradle/libs.versions.toml` / `app/build.gradle.kts` (read this session). **Nothing to add.**

### Core
| Library | Version (in repo) | Purpose | Verified API used |
|---------|-------------------|---------|-------------------|
| `androidx.room3:room3-runtime` / `room3-compiler` (KSP) | 3.0.3 | Reading table | `RoomDatabase.Builder.fallbackToDestructiveMigrationFrom(boolean, int...)`, `OnConflictStrategy.REPLACE = 1`, `@Upsert`, `@Index`, `@Entity(indices=)`, Flow DAO returns [VERIFIED: javap on cached room3-runtime/room3-common 3.0.3; CITED: developer.android.com/jetpack/androidx/releases/room3 "DAO functions have to be suspending unless they are returning a reactive type, such as a `Flow`"] |
| `androidx.sqlite:sqlite-bundled` | 2.7.1 | Driver; raw v1 fixture in the test | `SQLiteDriver.open(String)`, `androidx.sqlite.execSQL(SQLiteConnection, String)`, `SQLiteConnection.close()` [VERIFIED: javap sqlite-android 2.7.1] |
| Compose BOM | 2026.09.00 (ui 1.12.1, foundation 1.12.1, material3 1.4.0) | UI | `GraphicsLayer.record(...)`, `suspend toImageBitmap()`, `rememberGraphicsLayer()`, `drawLayer`, `PathMeasure()`, `getSegment(start, stop, dst, startWithMoveTo)`, `PathEffect.dashPathEffect` [VERIFIED: javap ui-graphics/ui 1.12.1; BOM pom read] |
| Material3 | 1.4.0 | Tabs | `PrimaryTabRow(selectedTabIndex, modifier, containerColor, contentColor, indicator, divider, tabs)`, `Tab(selected, onClick, ..., text, icon, selectedContentColor, unselectedContentColor)`, `TabRowDefaults.PrimaryIndicator` [VERIFIED: javap material3-android 1.4.0] |
| `androidx.core:core` (via core-ktx) | 1.19.1 | FileProvider | `androidx.core.content.FileProvider.getUriForFile(Context, String, File)` [VERIFIED: javap core-1.19.1.aar] |
| `androidx.hilt:hilt-lifecycle-viewmodel-compose` | 1.4.0 | Route-argument VMs | `hiltViewModel(owner, key, creationCallback: (VMF) -> VM)` [VERIFIED: javap] |
| kotlinx-serialization-json | 1.13.0 | Strict decode/encode | default `Json` is strict, and the round-trip already passes `PalmReadingDtoTest` [VERIFIED: PalmReadingDtoTest.kt:14-41] |

### Supporting (already present)
| Library | Purpose |
|---------|---------|
| `androidx.compose.ui:ui-test-junit4` (+ ui-test-manifest debug) | `captureToImage()`, `toPixelMap()`, `createComposeRule`/`createEmptyComposeRule` |
| JUnit 4.13.2, Turbine 1.2.1, kotlinx-coroutines-test 1.11.0 | VM and store tests with fakes (`src/test/.../testing/Fake*`; the repo has no MockK) |
| `androidx.exifinterface` | Not needed here: photos are already EXIF-free processed JPEGs |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Raw-SQL v1 file in the migration test | `androidx.room3:room3-testing` `MigrationTestHelper` | New artifact plus a download checkpoint, for a one-version destructive fallback. Not worth it. |
| Building the share intent in a pure function and asserting its fields | espresso-intents | New artifact; asserting the built `Intent` covers the same contract |
| Compose GraphicsLayer capture | `android.graphics.Canvas` hand-drawn card (D-21 fallback) | Duplicates layout and text code; use it only if the capture fails on the emulator |
| `HorizontalPager` tabs | — | Swipe is not required (D-08); a pager inside a verticalScroll needs fixed heights. Skip it. |

**Installation:** none.

## Package Legitimacy Audit

No external packages are installed in this phase. Every artifact used is already pinned in `libs.versions.toml` and resolved in the local Gradle cache (`~/.gradle/caches/modules-2/files-2.1/...`, inspected this session).

| Package | Registry | Verdict | Disposition |
|---------|----------|---------|-------------|
| (none new) | — | — | — |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
D-25's consent checkpoint is not needed.

## Architecture Patterns

### System Architecture Diagram

```
 ScanReady ("Read my palm")
   |  capture = ScanResultHolder.latest (cacheDir/scan/capture.jpg, w, h, hand)
   v
 ScanReadyViewModel.read()  --(in flight: ignore 2nd tap, block Back/Retake)--
   |
   v
 PalmReadingRepository.read(capture, lang)      [Hilt @Binds -> Fixture now, Network in Phase 6]
   |  Fixture: assets/fixtures/*.json -> FixtureChoice variant -> derive -> stamp(id, hand, size, locale, now)
   v
 ReadingRules.check(dto)  -- violation --> inline error + Retry
   |
   +-- verdict.ok == false --> holder.clear() --> stack: pop ScanReady, push InvalidPhoto(reason, hand)
   |                                                Retake / Back --> Scan(hand);  Home --> Home
   |
   +-- verdict.ok == true --> ReadingStore.save(dto, capture)
                                 1. validate readingId (UUID)
                                 2. Files.move scan/capture.jpg -> files/readings/<id>.jpg (ATOMIC, fallback copy+delete)
                                 3. dao.upsert(ReadingEntity(json = encode(dto), photoFile, hand, createdAt))
                                    (on failure: delete moved file, rethrow)
                                 4. holder.clear()
                              --> stack: [Home, Result(id)]

 Result(id) --> ResultViewModel(id) --> dao.byId(id): Flow --> strict decode --> ReadingRules
               |  photo decode (IO) -> ImageBitmap or "unavailable"
               v
          PhotoWithOverlay(fitRect -> Catmull-Rom -> Path -> halo + stroke, trim anim)
          Legend · Disclaimer · PrimaryTabRow(Palm|Love|Career) · cards · actions
               |-- Scan again --> push HandSelect (next valid save -> [Home, Result(new)])
               |-- Ask [Guide] --> push ComingSoon(ASK_GUIDE) bottom sheet
               '-- Share --> compose ShareCard (Density 3f, fontScale 1f) -> GraphicsLayer.record
                             -> toImageBitmap -> copy ARGB_8888 -> PNG cacheDir/share/reading.png
                             -> FileProvider URI -> ACTION_SEND + ClipData + grant -> createChooser

 Home --> dao.latest(): Flow --> "Your last reading" card --> push Result(id)
 PalmApp.onCreate --> holder.clear() + delete cacheDir/share/*
```

### Recommended Project Structure
```
app/src/main/java/app/palm/
├── core/db/AppDatabase.kt          # ReadingEntity v2, ReadingDao, version 2 + fallback (edit)
├── core/reading/
│   ├── PalmReadingRepository.kt    # interface (D-01)
│   ├── FixturePalmReadingRepository.kt  # assets + variants + stamping
│   ├── FixtureVariant.kt           # 13 variants enum + derive() (pure)
│   ├── ReadingRules.kt             # schema-rule checker (pure, also the Phase 6 guard)
│   ├── ReadingStore.kt             # save/delete/latest/byId (file + DAO)
│   ├── ContractJson.kt             # val ContractJson = Json (strict, one instance)
│   └── ReadingModule.kt            # @Binds repository; @Provides store
├── feature/result/
│   ├── OverlayMath.kt              # fitRect, mapPoint, catmullRom (pure)
│   ├── LineRules.kt                # isDrawn, badge, colour, order (pure)
│   ├── PalmOverlay.kt              # Canvas drawing + trim animation
│   ├── ResultScreen.kt / ResultViewModel.kt / ResultEntries.kt
│   ├── ShareCard.kt / Share.kt     # capture + intent builder
├── feature/invalidphoto/InvalidPhotoScreen.kt  # + reasonCopy(InvalidReason) exhaustive when
├── assets/fixtures/palm_reading.{en,hi,invalid}.json
└── res/values{,-hi}/strings_result.xml, res/xml/share_paths.xml
app/src/debug/java/app/palm/debug/FixtureLab.kt   # lab screen, lab photo generator, FixtureChoice impl
app/src/release/java/app/palm/debug/FixtureLab.kt # release twin: default choice, no entries
```

### Pattern 1: Fit rect and point mapping (pure)
**What:** Reproduce what `Image(contentScale = Fit, alignment = Center)` does, so the Canvas draws on the same pixels.
```kotlin
// Source: ContentScale.Fit semantics (uniform scale, both dims <= destination) + Alignment.Center
data class FitRect(val left: Float, val top: Float, val width: Float, val height: Float)

fun fitRect(srcW: Int, srcH: Int, dstW: Float, dstH: Float): FitRect {
    val s = minOf(dstW / srcW, dstH / srcH)
    val w = srcW * s
    val h = srcH * s
    return FitRect((dstW - w) / 2f, (dstH - h) / 2f, w, h)
}

fun FitRect.map(x: Double, y: Double): Pair<Float, Float> =
    left + x.coerceIn(0.0, 1.0).toFloat() * width to top + y.coerceIn(0.0, 1.0).toFloat() * height
```
- Compute it from the **decoded bitmap's** width and height and the Canvas `size`. Do not compute it from `image_size` (D-09) and do not rely on the modifier chain. Then it is correct whether the box keeps the photo's aspect or is letterboxed by the 55 % cap.
- Test cases: 3:4 (960x1280 into 360x480 gives the full rect), 1:1, 9:16, and letterboxed (960x1280 into 360x300 gives w = 225, left = 67.5). `Alignment.Center` rounds the offset to whole pixels inside `Image`, so allow ±1 px in pixel tests [ASSUMED: rounding detail].
- Skip Compose `Offset`/`Rect` in the pure code if you want zero Android types. `Offset` lives in ui-geometry, which is pure Kotlin, but a plain data class keeps the unit tests clearly free of Android.

### Pattern 2: Catmull-Rom to cubic Bézier (pure, affine-invariant)
```kotlin
// Uniform Catmull-Rom (tension 0.5) through P0..Pn, endpoints duplicated; segment Pi->Pi+1:
// C1 = Pi + (Pi+1 - Pi-1) / 6,  C2 = Pi+1 - (Pi+2 - Pi) / 6
data class Cubic(val c1x: Float, val c1y: Float, val c2x: Float, val c2y: Float, val x: Float, val y: Float)

fun catmullRom(px: FloatArray, py: FloatArray): List<Cubic> {
    val n = px.size
    require(n >= 2 && py.size == n)
    return (0 until n - 1).map { i ->
        val i0 = maxOf(i - 1, 0); val i3 = minOf(i + 2, n - 1)
        Cubic(
            px[i] + (px[i + 1] - px[i0]) / 6f, py[i] + (py[i + 1] - py[i0]) / 6f,
            px[i + 1] - (px[i3] - px[i]) / 6f, py[i + 1] - (py[i3] - py[i]) / 6f,
            px[i + 1], py[i + 1],
        )
    }
}
```
- Two points give one cubic whose control points lie on the segment, so the result is a straight line, as D-10 requires.
- The mapping is affine (separate x and y scale plus an offset). Bézier curves are affine-invariant, so running `catmullRom` on the mapped pixel points and running it on normalised points then mapping give the same curve. Map first and draw (simplest).
- The curve passes through every trace point. Unit test: each segment's end equals the next point exactly. Pixel test: sample the colour at mapped trace points.
- Build the Compose `Path` in `drawWithCache`: `moveTo(p0)`, then `cubicTo(c1, c2, end)` per segment. Compose `Path` wraps `android.graphics.Path`, so keep it out of JVM tests.
- Cap the points drawn per line (for example 64). The schema has no `maxItems`, and Phase 6 data comes from the network.

### Pattern 3: Draw and badge rules (pure, one place)
```kotlin
fun PalmLineDto.isDrawn() = traceSource != TraceSource.NONE && trace.size >= 2 && present
// present=false always has an empty trace (schema), so `present` is belt-and-braces
fun PalmLineDto.badge(): Badge = if (isDrawn()) Badge.APPROXIMATE else Badge.NOT_VISIBLE // M1: llm and template are both approximate
val lineOrder = LineType.entries  // HEART, HEAD, LIFE, FATE, SUN; sort cards by ordinal, not list order
```
- The contract requires only heart, head and life when `ok`. Fate and Sun may be **absent** from `lines`. Render the cards present in the list, sorted by `LineType.ordinal`. Do not render placeholders for missing types (consistent with D-08's "one line card per line").
- Colour by type: `PalmHeart`, `PalmHead`, `PalmLife`, `PalmFate`, `PalmSun` [VERIFIED: NocturneTokens.kt:31-35 `val PalmHeart = Color(0xFFFF7A9A)` `val PalmHead = Color(0xFF7CC4FF)` `val PalmLife = Color(0xFF6EE7B7)` `val PalmFate = Color(0xFFF2C66D)` `val PalmSun = Color(0xFFFFA94D)`].

### Pattern 4: Tabs
```kotlin
var tab by rememberSaveable { mutableIntStateOf(0) }
PrimaryTabRow(
    selectedTabIndex = tab,
    containerColor = Color.Transparent,
    contentColor = NocturneColors.TextPrimary,
    indicator = { TabRowDefaults.PrimaryIndicator(Modifier.tabIndicatorOffset(tab, matchContentSize = true), color = NocturneColors.AccentGold) },
) { titles.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, style = type.labelL) },
    selectedContentColor = NocturneColors.TextPrimary, unselectedContentColor = NocturneColors.TextMuted) } }
when (tab) { 0 -> PalmTab(r); 1 -> LoveTab(r); else -> CareerTab(r) }
```
- `rememberSaveable` survives rotation and process death. Nav3 already installs `rememberSaveableStateHolderNavEntryDecorator` (AppNavDisplay.kt:57-60), so state is per entry.
- M3 `Tab` gives TalkBack the Tab role and the selected state, so no custom semantics are needed.
- The exact `tabIndicatorOffset(Int, Boolean)` overload on `TabIndicatorScope` was not inspected; the `PrimaryTabRow` indicator slot receives `TabIndicatorScope` [VERIFIED: javap signature `Function3<? super TabIndicatorScope, ...>`]. Use whatever that scope exposes [ASSUMED: the overload's name and parameters].

### Pattern 5: Room 3 v2 and the v1 -> v2 test without room3-testing
```kotlin
@Entity(tableName = "reading", indices = [Index("createdAt")])
data class ReadingEntity(@PrimaryKey val id: String, val schemaVersion: Int, val json: String,
                         val photoFile: String, val handSide: String, val createdAt: Long)

@Dao interface ReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(e: ReadingEntity)
    @Query("SELECT * FROM reading WHERE id = :id") fun byId(id: String): Flow<ReadingEntity?>
    @Query("SELECT * FROM reading ORDER BY createdAt DESC LIMIT 1") fun latest(): Flow<ReadingEntity?>
    @Query("DELETE FROM reading WHERE id = :id") suspend fun delete(id: String)
    @Query("SELECT * FROM reading") suspend fun all(): List<ReadingEntity>
}
// builder: .fallbackToDestructiveMigrationFrom(true, 1)   // positional; param names not in bytecode
```
- `handSide` as a `String` (`HandSide.name`) avoids a `@ColumnTypeConverter` (Room 3 renamed `@TypeConverter` to that) [CITED: room3 release notes 3.0.0-rc01]. Room may also map enums natively [ASSUMED]. A String column is the zero-risk choice.
- Migration test: write a v1 database file with the driver, then open it with the v2 builder:
```kotlin
// v1 SQL copied from schemas/app.palm.core.db.AppDatabase/1.json (createSql with ${TABLE_NAME} = reading, and setupQueries)
val path = context.getDatabasePath("v1-test.db").also { it.delete() }.path
BundledSQLiteDriver().open(path).let { c -> try {
    c.execSQL("CREATE TABLE IF NOT EXISTS `reading` (`id` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `json` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    c.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
    c.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd87abb872b81665c4e6776d2714c9889')")
    c.execSQL("INSERT INTO reading VALUES('old', 1, '{}', 1)")
    c.execSQL("PRAGMA user_version = 1")
} finally { c.close() } }
// then Room.databaseBuilder<AppDatabase>(context, path).setDriver(BundledSQLiteDriver()).fallbackToDestructiveMigrationFrom(true, 1).build()
// assert: all() is empty (dropped), upsert + byId of a v2 row works
```
[VERIFIED: schemas/app.palm.core.db.AppDatabase/1.json: createSql "CREATE TABLE IF NOT EXISTS `${TABLE_NAME}` (`id` TEXT NOT NULL, `schemaVersion` INTEGER NOT NULL, `json` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))", identityHash "d87abb872b81665c4e6776d2714c9889"]
- The v2 schema exports to `android/app/schemas/app.palm.core.db.AppDatabase/2.json`, next to the existing `1.json` (no flavours). Commit it.
- `AppDatabaseTest` builds `ReadingEntity` with 4 fields and calls `insert` and `count`. It breaks at compile time and must be updated in the same plan.

### Pattern 6: Share card capture and intent
```kotlin
// Compose only while sharing. Fixed density: 360x450 dp * 3 = 1080x1350 px on every phone, and font scale does not move text.
@Composable fun ShareCapture(reading: PalmReadingDto, photo: ImageBitmap?, onBitmap: (ImageBitmap) -> Unit) {
    val layer = rememberGraphicsLayer()
    Box(Modifier.size(0.dp)) {             // takes no space in the screen layout
        CompositionLocalProvider(LocalDensity provides Density(3f, fontScale = 1f)) {
            Box(Modifier.requiredSize(360.dp, 450.dp)
                .drawWithContent { layer.record { this@drawWithContent.drawContent() } } // record only, never drawLayer
            ) { ShareCard(reading, photo) }
        }
    }
    LaunchedEffect(Unit) { withFrameNanos { }; withFrameNanos { }; onBitmap(layer.toImageBitmap()) }
}
// IO: val bmp = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)  // API 28+ capture is HARDWARE
//     File(cacheDir, "share").mkdirs(); FileOutputStream(file).use { bmp.compress(PNG, 100, it) }
val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
val send = Intent(Intent.ACTION_SEND).apply {
    type = "image/png"
    putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text)
    clipData = ClipData.newRawUri(null, uri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.startActivity(Intent.createChooser(send, title))
```
- Base API: record + `toImageBitmap` [CITED: developer.android.com/develop/ui/compose/graphics/draw/modifiers "Write contents of a composable to a bitmap"].
- On API 28+, Compose's snapshot calls `Bitmap.createBitmap(Picture)` [VERIFIED: javap -c LayerSnapshotV28]. The result is "an immutable bitmap with a HARDWARE config" [CITED: developer.android.com/reference/android/graphics/Bitmap]. `copy(ARGB_8888, false)` is allowed: the doc only forbids `HARDWARE` + mutable. API 26-27 use `LayerSnapshotV22` (ImageReader).
- Recording without `drawLayer` in a zero-size parent depends on Compose still drawing nodes outside the parent bounds (Compose does no culling) [ASSUMED]. Gate it on the emulator first. If the capture comes back blank, use the D-21 Canvas fallback.
- Put the `ShareCapture` node **outside** the `verticalScroll` column (a sibling inside the screen's root `Box`), so the scroll does not measure it.
- Manifest provider: `android:name="androidx.core.content.FileProvider"`, `android:authorities="${applicationId}.share"`, `android:exported="false"`, `android:grantUriPermissions="true"`, `<meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/share_paths"/>`, and `share_paths.xml` = `<paths><cache-path name="share" path="share/"/></paths>` [ASSUMED: standard FileProvider XML from training; the class itself is VERIFIED].
- The debug applicationId is `app.palm.dev.debug` [VERIFIED: app/build.gradle.kts `applicationId = "app.palm.dev"`, `applicationIdSuffix = ".debug"`]. Always use `context.packageName`. A hard-coded authority breaks one of the two builds.
- Unit-test the intent: build it in `fun shareIntent(uri: Uri, text: String): Intent` and assert the action, type, `EXTRA_STREAM`, `clipData`, and the flag in an instrumented test. Do not launch the chooser in the test.

### Pattern 7: Route arguments into ViewModels
```kotlin
@HiltViewModel(assistedFactory = ResultViewModel.Factory::class)
class ResultViewModel @AssistedInject constructor(@Assisted val readingId: String, store: ReadingStore) : ViewModel() {
    @AssistedFactory interface Factory { fun create(readingId: String): ResultViewModel }
}
entry<Result> { key -> ResultScreen(hiltViewModel<ResultViewModel, ResultViewModel.Factory>(creationCallback = { it.create(key.readingId) }), ...) }
```
[VERIFIED: hilt-lifecycle-viewmodel-compose 1.4.0 has the `hiltViewModel(ViewModelStoreOwner, String, Function1<VMF, VM>)` overload.] No existing VM uses assisted injection, so this is the first one; Dagger 2.60.1 supports it [ASSUMED: assisted ViewModel support in Dagger 2.49+]. Alternative with zero new patterns: a `load(id)` call from a `LaunchedEffect`.

### Pattern 8: Navigation helpers
- `replaceAll(key)` takes one key [VERIFIED: Routes.kt:38-41 `fun NavBackStack<NavKey>.replaceAll(key: NavKey) { add(key); subList(0, size - 1).clear() }`]. Add `fun NavBackStack<NavKey>.replaceAll(vararg keys: NavKey) { addAll(keys); subList(0, size - keys.size).clear() }`. It keeps the never-empty invariant.
- Invalid: `if (lastOrNull() == ScanReady) { removeAt(size - 1); add(InvalidPhoto(reason, hand)) }`. Retake and Back on InvalidPhoto use `popFrom(key)`, and the stack then shows `Scan(hand)`.
- New routes must be `@Serializable` subtypes of `Route`. `InvalidReason` and `HandSide` are already `@Serializable` enums [VERIFIED: PalmReadingDto.kt `@Serializable enum class HandSide { @SerialName("left") LEFT, @SerialName("right") RIGHT }` and `@Serializable enum class InvalidReason { ... BLURRY_OR_DARK, TOO_FAR, JEWELRY, BENT, FINGERS_TIGHT, INCOMPLETE, NO_PALM_LINES }`].
- `ComingSoon` is registered with the BottomSheet strategy in `homeEntries` and opens over any top route (HomeEntries.kt:193-195). From Result: `backStack.pushFrom(key, ComingSoon(Feature.ASK_GUIDE))`.
- `debugEntries()` takes **no backStack** [VERIFIED: DebugEntries.kt (debug and release) `fun EntryProviderScope<NavKey>.debugEntries()`; AppNavDisplay.kt:72 `debugEntries()`]. The lab must push ScanReady, so change the signature to `debugEntries(backStack)` in **both** twins and in AppNavDisplay. The Showcase also needs a callback to open the lab (it takes only `statusViewModel` today).

### Anti-Patterns to Avoid
- **Using `image_size` to scale traces.** Traces are normalised to the sent image (schema `ImageSize.description`: "trace coordinates are relative to it"). Scale with the decoded bitmap and `fitRect`.
- **Smoothing with quadratic midpoints.** That curve does not pass through the trace points, so pixel tests at trace points fail. Use Catmull-Rom.
- **Glowing in the line's own colour on light skin.** Contrast stays near 1:1 (Pitfall 1).
- **Storing landmarks or EXIF.** `ProcessedCapture.landmarks` must not go into the row or the JSON.
- **Composing the share card in the scroll column or in the visible tree with `drawLayer`.** It shows on screen or shifts the layout.
- **Logging the reading JSON or traces "only in debug"**, which D-18 forbids. The D-09 aspect warning may log the two aspect numbers only.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Composable to bitmap | Manual `View.draw` / `ComposeView` attached off-window | `rememberGraphicsLayer` + `record` + `toImageBitmap` | Official API; handles hardware layers |
| Share URI | `file://` or a custom ContentProvider | `androidx.core.content.FileProvider` | `file://` throws `FileUriExposedException`; grants are per-URI |
| Path trimming animation | Arc-length sampling by hand | `PathMeasure.getSegment(0, len * f, dst, true)` | Verified in ui-graphics 1.12.1 |
| Tab semantics | Custom clickable Row | M3 `PrimaryTabRow`/`Tab` | Role, selected state, focus for free |
| DB change notification | Polling | Room `Flow` DAO returns | Room 3 invalidation tracker |
| Thumbnail sampling | New sampler | Existing `sampleSizeFor(w, h, maxEdge)` [VERIFIED: CropMath.kt:40-43] | Already tested (CropMathTest) |
| Atomic file replace | `File.renameTo` | `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)` as in `ScanResultHolder.put` [VERIFIED: ScanResultHolder.kt] | `renameTo` cannot replace on every platform (the repo's own comment) |
| JSON validation | Ad-hoc null checks scattered in UI | One `ReadingRules.check(dto): List<String>` used by the drift test, the repository and the Result VM | One place, reused as the Phase 6 trust-boundary guard |

**Key insight:** the dangerous parts of this phase are ordering and trust boundaries (file move vs clear, id as filename, hardware bitmaps), not drawing. Keep every rule in a pure function and test it on the JVM.

## Common Pitfalls

### Pitfall 1: Palm line colours vanish on light skin
**What goes wrong:** Heart, Head, Life, Fate and Sun reach only 1.03-1.50:1 against a light skin tone (#F1C27D) and 1.5-2.5:1 against white. Against a dark tone (#8D5524) they reach 2.5-4.0:1 [computed this session with the WCAG formula; skin hexes are illustrative, ASSUMED representative].
**How to avoid:** Draw a **dark halo** first (`BgDeep` or black at about 40-50 % alpha, stroke about 9 dp, round caps), then the 3 dp coloured line. Against the halo the colours keep their 7.9-12.9:1 (their contrast on `BgBase`), which meets the 3:1 non-text rule (WCAG 1.4.11) on any photo. This fills in the "soft glow" colour, which D-10 leaves unspecified.
**Warning signs:** Lab photo variants with a light gradient show faint lines.

### Pitfall 2: Back or Retake during save deletes the photo being moved
**What goes wrong:** `ScanReadyScreen` calls `viewModel.retake()`, which runs `holder.clear()` (NonCancellable, deletes every file in `cacheDir/scan`) on Back [VERIFIED: ScanReadyScreen.kt:87-91, ScanReadyViewModel.kt:122-124]. If this happens while `save` moves `capture.jpg`, the move fails or the row points to a missing file.
**How to avoid:** While a read is in flight, disable Retake and make the BackHandler a no-op (or let it cancel nothing). The read should run in `viewModelScope` with `NonCancellable` around save + navigate, or simply complete before the screen can pop.

### Pitfall 3: Path traversal through `reading_id`
**What goes wrong:** `File(readingsDir, "$readingId.jpg")` with a server-supplied id like `../shared_prefs/x` writes outside the folder. The fixture is safe; Phase 6 is not.
**How to avoid:** In `ReadingStore.save`, require `UUID.fromString(id).toString() == id.lowercase()` (the schema says `format: uuid`). Also derive `photoFile` only from a validated id. On read, resolve `File(dir, photoFile)` and check `canonicalPath.startsWith(dir.canonicalPath + File.separator)`.

### Pitfall 4: HARDWARE bitmap from the capture
**What goes wrong:** On API 28+, `toImageBitmap()` returns a HARDWARE bitmap. Pixel access fails on it, and the `compress` docs say nothing about HARDWARE.
**How to avoid:** `asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)` before `compress`, on `Dispatchers.IO` (the docs warn that compress "may take several seconds").

### Pitfall 5: Share card layout depends on the device
**What goes wrong:** Laid out in device dp and sp, a 1080 px card is 411 dp wide on a 420 dpi phone and 360 dp on another, and font scale 2.0 overflows the summary.
**How to avoid:** Wrap the card in `CompositionLocalProvider(LocalDensity provides Density(3f, fontScale = 1f))` and lay it out at 360x450 dp. Cap the summary with `maxLines` + ellipsis and take only 3 traits (D-21).

### Pitfall 6: JVM tests crash on `BitmapFactory` in `ScanReadyViewModel.init`
**What goes wrong:** The VM decodes the capture in `init` with `BitmapFactory.decodeFile` and catches only `IOException` [VERIFIED: ScanReadyViewModel.kt:105-118]. Unit tests run without `isReturnDefaultValues` [VERIFIED: build.gradle.kts testOptions], so the call throws `RuntimeException("Method ... not mocked")`.
**How to avoid:** Inject a tiny `PhotoDecoder` (`suspend fun decode(file: File): ImageBitmap?`) with an Android implementation, and use a fake in the tests. Or move the read flow into a separate class that the VM delegates to, and test that class.

### Pitfall 7: Fixture assets are not on the JVM test classpath
**What goes wrong:** `context.assets` is Android-only. JVM tests of `FixturePalmReadingRepository` and the drift test cannot open `src/main/assets`.
**How to avoid:** Give the repository a `(name: String) -> String` source (Android: `assets.open("fixtures/$name")`). In tests, read from a new system property next to `contracts.dir`: `it.systemProperty("fixtures.dir", file("src/main/assets/fixtures").absolutePath)` in `testOptions.unitTests.all` [VERIFIED: existing `contracts.dir` line in build.gradle.kts]. This is a build-script edit, not a new artifact.

### Pitfall 8: "Disclaimer visible on every tab" test fails due to scroll
**What goes wrong:** After switching to Career, `assertIsDisplayed()` on the disclaimer can fail if the user or test scrolled down to tap the tab.
**How to avoid:** For each tab: `performScrollTo()` on the disclaimer, then `assertIsDisplayed()`. Or assert `assertExists()` plus the fact that it sits outside the tab content (a single node).

### Pitfall 8b: The trim animation hides lines during pixel tests
**How to avoid:** In Compose tests, provide `LocalNocturne provides LocalNocturne.current.copy(reduceMotion = true)` (`NocturneExtras` is a data class [VERIFIED: NocturneTheme.kt:86-104 `data class NocturneExtras(... val reduceMotion: Boolean = false, val type: NocturneTextStyles)`]). Or advance `mainClock` past 900 ms.

### Pitfall 9: `connectedDebugAndroidTest` passes with zero tests
Known from Phase 1 and 4: keep the `! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected` guard, and clear the directory first.

### Pitfall 10: Instrumented tests wiping real data
The app-process test shares `filesDir` and the `palm.db` with the debug install on the emulator. Use `Room.inMemoryDatabaseBuilder` and a temp directory (`cacheDir/test-<uuid>`) for `ReadingStore` tests. For the Home test that uses the real graph, add a `ReadingEntryPoint` (same pattern as `ProfileEntryPoint` [VERIFIED: ProfileRepository.kt:149-153]) and delete only the ids the test created.

### Pitfall 11: An invalid verdict leaves the capture behind
D-15 says the capture is deleted "when the user leaves InvalidPhoto". InvalidPhoto shows no photo (D-14), so the simplest correct option is to `holder.clear()` in the ScanReady VM **before** navigating to InvalidPhoto. Then no InvalidPhoto ViewModel is needed and no exit path can forget the cleanup. This is a recommended refinement; the outcome is the same as D-15.

## Code Examples

### Fixture stamping (D-02)
```kotlin
fun PalmReadingDto.stamp(capture: ProcessedCapture, lang: AppLanguage, now: Instant): PalmReadingDto = copy(
    readingId = UUID.randomUUID().toString(),
    handSide = capture.hand,
    imageSize = ImageSizeDto(capture.width, capture.height),
    meta = meta.copy(locale = lang.tag, generatedAt = now.truncatedTo(ChronoUnit.SECONDS).toString()),
)
```
`AppLanguage` [VERIFIED: ProfileRepository.kt:36 `enum class AppLanguage(val tag: String) { EN("en"), HI("hi") }`]. Default `Json` encodes `"reason": null` (`explicitNulls` is on by default), and the existing round-trip test proves encode(decode(sample)) equals the sample [VERIFIED: PalmReadingDtoTest.kt:29].

### Schema rules (D-04), the single checker
```kotlin
fun ReadingRules.check(r: PalmReadingDto): List<String> = buildList {
    if (r.schemaVersion != 1) add("schema_version")
    if (r.verdict.ok) {
        if (r.verdict.reason != null || r.palm == null || r.love == null || r.career == null) add("ok shape")
        if (!r.lines.map { it.type }.containsAll(listOf(LineType.HEART, LineType.HEAD, LineType.LIFE))) add("required lines")
    } else if (r.verdict.reason == null || r.lines.isNotEmpty() || r.palm != null) add("invalid shape")
    if (r.lines.map { it.type }.toSet().size != r.lines.size) add("duplicate type")
    r.lines.forEach { l ->
        if ((l.traceSource == TraceSource.NONE) != l.trace.isEmpty()) add("${l.type} none<->empty")
        if (l.traceSource != TraceSource.NONE && l.trace.size < 2) add("${l.type} < 2 points")
        if (!l.present && l.trace.isNotEmpty()) add("${l.type} absent with trace")
        if (l.trace.any { it.size != 2 || it.any { c -> c !in 0.0..1.0 } }) add("${l.type} point range")
    }
}
```
[VERIFIED: schema PalmLine description "trace_source none exactly when trace is empty; present false implies an empty trace; llm and template traces have at least 2 points. Points are [x, y] in 0..1." and root description "verdict.ok true implies reason null, palm, love, career present and heart, head and life lines listed; verdict.ok false implies reason set, lines empty and palm, love, career null; no line type appears twice."]

### Generated lab photo (debug)
```kotlin
// Bitmap(w, h) + android.graphics.Canvas: LinearGradient background, a rounded-rect palm + 5 finger capsules in a skin tone,
// compress(JPEG, 85) -> holder.put(jpeg, w, h, hand, landmarks = List(21) { P3(0.5f, 0.5f, 0f) }, PhotoOrigin.GALLERY)
```
[VERIFIED: ScanResultHolder.kt `fun put(jpeg: ByteArray, width: Int, height: Int, hand: HandSide, landmarks: List<P3>, origin: PhotoOrigin)`; PhotoProcessor.kt:32 `enum class PhotoOrigin { CAMERA, GALLERY }`; HandFrame.kt:6 `data class P3(val x: Float, val y: Float, val z: Float)`.] Paint one light-skin and one dark-skin variant so Pitfall 1 is visible in the lab.

### PalmApp cleanup
```kotlin
scanResults.clear()
File(cacheDir, "share").listFiles()?.forEach { it.delete() }   // D-22; one listing, still main-thread cheap
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `androidx.room` + `@TypeConverter`, blocking DAOs | `androidx.room3`, `@ColumnTypeConverter`, suspend or Flow only | Room 3.0.0, Jul 2026 | DAO functions must be `suspend` or return `Flow` [CITED: room3 release notes] |
| `View.drawToBitmap` / `PixelCopy` for composables | `GraphicsLayer.record` + `toImageBitmap` | Compose 1.7 | Official path [CITED: Compose draw modifiers doc] |
| `file://` share URIs | FileProvider `content://` + grant | API 24 | Required |

**Deprecated/outdated:** `MigrationTestHelper` from `androidx.room:room-testing` (Room 2) cannot be used with Room 3. Do not add it.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Recording a GraphicsLayer from a node in a zero-size parent, without `drawLayer`, still records (Compose does not cull) | Pattern 6 | Blank share image: switch to the D-21 Canvas fallback; gate on the emulator first |
| A2 | `Intent.createChooser` keeps the target's ClipData and grant flag so receivers can read the URI | Pattern 6 | Targets fail to load the image: also set ClipData and the flag on the chooser intent |
| A3 | Standard FileProvider manifest and `cache-path` XML element names | Pattern 6 | Build-time or runtime error, caught by the share instrumented test |
| A4 | `TabIndicatorScope.tabIndicatorOffset(index, matchContentSize)` overload name | Pattern 4 | Compile error; use the scope's available modifier |
| A5 | Dagger 2.60.1 assisted ViewModels work with `hiltViewModel(creationCallback)` | Pattern 7 | Fall back to `load(id)` from `LaunchedEffect` |
| A6 | Representative skin hexes #F1C27D / #8D5524 for the contrast numbers | Pitfall 1 | Exact ratios differ; the conclusion (light skin is near 1:1) holds for any light tone |
| A7 | `Alignment.Center` offset rounding is at most 1 px | Pattern 1 | Pixel tests need a wider tolerance |
| A8 | PNG of a 1080x1350 card with a photo is about 1-3 MB | D-22 | Larger share payload; acceptable for chat apps |
| A9 | Hindi `DateTimeFormatter.ofLocalizedDate(MEDIUM)` output is readable with Latin digits | D-19 | Visual only |

## Open Questions

1. **Does the off-screen GraphicsLayer capture work on emulator-5554 (API 35) and on API 26-27?**
   - What we know: the API exists and uses `LayerSnapshotV28` (Picture) on API 28+ and `LayerSnapshotV22` below.
   - What's unclear: whether recording without `drawLayer` in a zero-size parent produces content, and the API 26-27 path. No API 26/27 system image is installed (`system-images`: android-24, 35, 36, 36.1, 37.0).
   - Recommendation: make "share PNG decodes, is 1080x1350 and is not a single colour" the first share-task gate on the emulator. Leave API 26-27 to the reference phone session (Android 11 = API 30, the V28 path).
2. **Orphan photo sweep.** A crash between the file move and the insert leaves a private orphan file. Recommendation: no startup sweep in Phase 5 (a `shortcut:` comment in `ReadingStore`). The lab's "Delete all readings" deletes the whole `readings/` directory.
3. **Figma S10 frames** are for the UI-SPEC step, not this research.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Emulator | All UI and instrumented tests | ✓ | emulator-5554, API 35, 1080x2400 @ 420 dpi | — |
| JDK | Gradle | ✓ | Temurin 17.0.17 | — |
| Android SDK platforms | compileSdk 37 | ✓ | android-37.0 (plus 31-36.1) | — |
| API 26-27 system image | LayerSnapshotV22 path | ✗ | — | Phone (API 30) covers V28 only; API 26-27 unverified |
| Python | `tools/screen.py`, `tools/ui.sh` (ui.py) | ✓ | 3.14.0 | — |
| Gradle cache with all artifacts | Offline build | ✓ | room3 3.0.3, compose 1.12.1, m3 1.4.0, core 1.19.1, hilt 1.4.0 found | — |

**Missing dependencies with no fallback:** none blocking.
**Missing dependencies with fallback:** API 26-27 image (accept as unverified, note in the summary).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + kotlinx-coroutines-test 1.11.0 + Turbine 1.2.1 (JVM); androidx.test runner 1.7.0 / ext-junit 1.3.0 + Compose ui-test-junit4 1.12.1 (instrumented) |
| Config file | `android/app/build.gradle.kts` (`testOptions.unitTests.all { systemProperty("contracts.dir", ...) }`; add `fixtures.dir`) |
| Quick run command | `bash tools/gradlew.sh :app:testDebugUnitTest --tests 'app.palm.core.reading.*' --tests 'app.palm.feature.result.*' --tests 'app.palm.feature.invalidphoto.*' --tests 'app.palm.feature.scan.*'` |
| Full suite command | `bash tools/gradlew.sh :app:testDebugUnitTest` then `ADB_SERIAL=emulator-5554 bash tools/device-lock.sh bash tools/gradlew.sh :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true && ! grep -rl 'tests="0"' android/app/build/outputs/androidTest-results/connected` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| RES-02 | `fitRect` + map for 3:4, 1:1, 9:16, letterboxed; clamp | unit | `--tests 'app.palm.feature.result.OverlayMathTest'` | ❌ Wave 0 |
| RES-02 | Catmull-Rom passes through points; 2 points straight | unit | same class | ❌ Wave 0 |
| RES-02 | Line colour at mapped trace points on 3 aspects; no overlay for all-none | Compose instrumented | `-Pandroid.testInstrumentationRunnerArguments.class=app.palm.feature.result.PalmOverlayTest` | ❌ Wave 0 |
| RES-02/03 | isDrawn/badge per trace_source incl. present+none; order by LineType | unit | `--tests 'app.palm.feature.result.LineRulesTest'` | ❌ Wave 0 |
| RES-01/04 | Tab switching; disclaimer present on each tab | Compose instrumented | `class=app.palm.feature.result.ResultScreenTest` | ❌ Wave 0 |
| RES-05 | Save moves photo, upsert, insert failure leaves no file, delete removes both, bad id rejected | unit (fake DAO + TemporaryFolder) + instrumented (Room in-memory) | `--tests 'app.palm.core.reading.ReadingStoreTest'`; `class=app.palm.core.reading.ReadingStoreRoomTest` | ❌ Wave 0 |
| RES-05 | v1 file opens as v2 (destructive) | instrumented | `class=app.palm.core.db.AppDatabaseTest` (update existing) | ✅ (must be edited) |
| RES-05/HOME-03 | Kill + relaunch, Home card reopens reading | instrumented + emulator script | `class=app.palm.feature.home.HomeLastReadingTest`; `adb shell am force-stop app.palm.dev.debug` + `bash tools/ui.sh launch` + `expect` | ❌ Wave 0 |
| RES-06 | Share intent fields; URI resolves; PNG decodes 1080x1350, not blank | instrumented | `class=app.palm.feature.result.ShareTest` | ❌ Wave 0 |
| RES-06 | Real chooser opens, a target receives the image | manual (emulator) | screenshot | — |
| RES-07 | Scan again pushes HandSelect; Ask [Guide] opens the coming-soon sheet | Compose instrumented | `class=app.palm.feature.result.ResultScreenTest` | ❌ Wave 0 |
| READ-03 | Reason to title/tip for all 7 (exhaustive) | unit | `--tests 'app.palm.feature.invalidphoto.ReasonCopyTest'` | ❌ Wave 0 |
| READ-03 | Invalid verdict: no save, holder cleared, InvalidPhoto pushed; Retake lands on Scan(hand) | unit (VM with fakes) + instrumented | `--tests 'app.palm.feature.scan.ScanReadyViewModelTest'`; `class=app.palm.feature.invalidphoto.InvalidPhotoTest` | ❌ Wave 0 |
| D-03 | Valid: save then navigate; double tap = one read; repo throws = error + retry | unit | `--tests 'app.palm.feature.scan.ScanReadyViewModelTest'` | ❌ Wave 0 |
| D-04/D-06 | en/invalid fixture == contracts; all 13 variants decode strictly and pass `ReadingRules` | unit | `--tests 'app.palm.core.reading.FixturesTest'` | ❌ Wave 0 |
| D-02 | Stamping id/hand/size/locale/time | unit | `--tests 'app.palm.core.reading.FixtureRepositoryTest'` | ❌ Wave 0 |
| D-12 | Result VM: found, missing row, bad JSON, missing photo | unit | `--tests 'app.palm.feature.result.ResultViewModelTest'` | ❌ Wave 0 |
| D-24 | Every lab variant in en and hi, font scale 2.0 | manual (emulator screenshots) | `adb shell settings put system font_scale 2.0` (restore 1.0) | — |

### Sampling Rate
- **Per task commit:** the quick run command (JVM only).
- **Per wave merge:** full JVM suite + the instrumented classes touched by the wave on emulator-5554.
- **Phase gate:** full suite green (with the tests="0" guard), plus the emulator walk of all 13 variants in en and hi, before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `build.gradle.kts`: add the `fixtures.dir` system property (Pitfall 7)
- [ ] `src/test/.../testing/FakeReadingDao.kt`, `FakePalmReadingRepository.kt`, `FakePhotoDecoder.kt`
- [ ] `src/androidTest/.../ReadingEntryPoint` usage (main source `@EntryPoint`) for Home and Result tests
- [ ] A plain test photo generated in code inside the overlay test (no new asset file)
- [ ] Update `AppDatabaseTest` for the v2 entity

## Security Domain

`security_enforcement: true`, ASVS level 1 [VERIFIED: .planning/config.json `"security_enforcement": true`, `"security_asvs_level": 1`].

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | yes | FileProvider `exported="false"`, `grantUriPermissions`, per-URI read grant, `cache-path` limited to `share/` |
| V5 Input Validation | yes | Strict `Json` decode + `ReadingRules.check` before save and render; UUID check on `reading_id`; point clamp + per-line point cap |
| V6 Cryptography | no | — (no crypto; never hand-roll) |
| V7 Error/Logging | yes | No reading text, traces, ids-to-paths or image bytes in logs (D-18) |
| V8 Data Protection | yes | `filesDir`/Room private, backup off [VERIFIED: AndroidManifest.xml `allowBackup="false"`, `fullBackupContent="false"`, data_extraction_rules excludes file/database], delete removes the photo |
| V12 Files | yes | Canonical-path check for `photoFile`; share dir cleared at start |

### Known Threat Patterns
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Path traversal via `reading_id` used as file name | Tampering | UUID round-trip validation; canonical-path containment |
| FileProvider exposing `filesDir/readings` | Information disclosure | Only `cache-path share/`; never `files-path`; provider not exported |
| Stale share PNG readable later | Information disclosure | Overwrite a single `reading.png`; clear `cacheDir/share` at app start |
| Malformed or huge JSON (Phase 6) crashing Result | Denial of service | Strict decode in try/catch giving the D-12 state; cap points per line |
| Reading text in logcat | Information disclosure | No `Log` of DTOs; the debug aspect warning logs numbers only |
| Landmarks persisted with the reading | Privacy | Never part of the DTO or the row |

## Sources

### Primary (HIGH confidence)
- In-repo files read this session: `AppDatabase.kt`, `PalmReadingDto.kt`, `ScanResultHolder.kt`, `Routes.kt`, `ScanEntries.kt`, `ScanReadyScreen.kt`, `ScanReadyViewModel.kt`, `HomeEntries.kt`, `HomeViewModel.kt`, `HomeScreen.kt`, `AppNavDisplay.kt`, `DebugEntries.kt` (debug + release), `ScanDebug.kt`, `PalmApp.kt`, `AndroidManifest.xml`, `data_extraction_rules.xml`, `NocturneTokens.kt`, `NocturneTheme.kt`, components, `ProfileRepository.kt`, `libs.versions.toml`, `app/build.gradle.kts`, `schemas/.../1.json`, `AppDatabaseTest.kt`, `PalmReadingDtoTest.kt`, the contracts (schema + 2 samples), PRD §5.1/§6/§8/§11
- javap on the cached AARs: room3-runtime/room3-common 3.0.3, sqlite-android 2.7.1, ui-graphics/ui 1.12.1, material3-android 1.4.0, core 1.19.1, hilt-lifecycle-viewmodel-compose 1.4.0; Compose BOM 2026.09.00 POM
- https://developer.android.com/jetpack/androidx/releases/room3 (Flow DAO rule, ColumnTypeConverter rename, destructive-migration defaults)
- https://developer.android.com/develop/ui/compose/graphics/draw/modifiers (composable to bitmap)
- https://developer.android.com/reference/android/graphics/Bitmap (createBitmap(Picture) gives HARDWARE; copy rules)
- https://developer.android.com/training/sharing/send (ACTION_SEND, EXTRA_STREAM, FileProvider, chooser)

### Secondary (MEDIUM)
- WCAG 2.x contrast computations run locally on token hexes

### Tertiary (LOW, flagged in the Assumptions Log)
- Off-screen record behaviour, chooser ClipData propagation, FileProvider XML names, tab indicator overload name

**Process note:** The GSD `research-plan`, `research-store` and `classify-confidence` seams and the graphify step were not run. All search providers are off in the config, no graph exists, and every claim was checked directly against the source files, the cached binaries or the official docs. The tiers above are assigned by that evidence.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Every API was confirmed in the exact cached artifact versions.
- Architecture: HIGH. It follows the existing code (Nav3 helpers, holder, entry points, debug twins).
- Pitfalls: HIGH for 2, 3, 6, 7, 10 (read from code); MEDIUM for 1, 4, 5 (docs plus computation); A1 is the main unknown.

**Research date:** 2026-11-28
**Valid until:** 2026-12-28 (stable stack, pinned versions)
