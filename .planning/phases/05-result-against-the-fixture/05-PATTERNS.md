# Phase 5: Result Against the Fixture - Pattern Map

**Mapped:** 2026-11-28
**Files analyzed:** 30 (new + modified)
**Analogs found:** 27 / 30

All paths below are git-tracked (`git ls-files android tools`). Base: `android/app/src/` unless stated.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `main/.../core/db/AppDatabase.kt` (edit: v2 entity, DAO, fallback) | model/config | CRUD | itself (lines 21-58) | exact |
| `main/.../core/reading/PalmReadingRepository.kt` | service interface | request-response | `core/photo/PhotoProcessor.kt` (interface + `PhotoModule` 177-186) | role-match |
| `main/.../core/reading/FixturePalmReadingRepository.kt` | service | file-I/O (assets) | `core/vision/HandDetector.kt` (asset-backed impl + `@Binds` 66-70) | role-match |
| `main/.../core/reading/FixtureVariant.kt` + `FixtureChoice` seam | utility (pure) | transform | `core/vision/HandGuidance.kt` (pure, JVM-tested) | role-match |
| `main/.../core/reading/ReadingRules.kt` | utility (pure) | transform/validation | `feature/onboarding/NameRules.kt`, `BirthRules.kt` | role-match |
| `main/.../core/reading/ReadingStore.kt` | service | file-I/O + CRUD | `core/photo/ScanResultHolder.kt` (atomic move) | exact |
| `main/.../core/reading/ReadingModule.kt` | config (DI) | - | `core/locale/AppLocale.kt` 38-42 (`@Binds`) + `core/photo/PhotoProcessor.kt` 177-186 (`@Provides`) | exact |
| `main/.../core/navigation/Routes.kt` (edit: `Result`, `InvalidPhoto`, vararg `replaceAll`) | route | - | itself (23-51) | exact |
| `main/.../feature/scan/ScanReadyViewModel.kt` (edit: read action) | VM | request-response | `feature/scan/ScanViewModel.kt` (processing/ready/consume, `io` dispatcher seam) | exact |
| `main/.../feature/scan/ScanReadyScreen.kt` (edit: enable button, loading, inline error) | component | - | itself (84-169) | exact |
| `main/.../feature/scan/ScanEntries.kt` (edit: navigation on verdict) | route | - | itself | exact |
| `main/.../feature/result/OverlayMath.kt` (`fitRect`, map, `catmullRom`) | utility (pure) | transform | `core/vision/ViewMapping.kt` + `core/photo/CropMath.kt` | exact |
| `main/.../feature/result/LineRules.kt` | utility (pure) | transform | `core/vision/HandGuidance.kt` | role-match |
| `main/.../feature/result/PalmOverlay.kt` | component (Canvas) | - | `feature/scan/ScanReadyScreen.kt` 113-140 (photo + Canvas), `feature/scan/PalmGuideOverlay.kt` | exact |
| `main/.../feature/result/ResultScreen.kt` (tabs, cards, legend, disclaimer, actions) | component | - | `feature/scan/ScanReadyScreen.kt` (layout), `feature/home/HeroCard.kt` (cards) | role-match |
| `main/.../feature/result/ResultViewModel.kt` | VM | CRUD (read Flow) | `feature/home/HomeViewModel.kt` (`stateIn`) + `ScanReadyViewModel.kt` (IO decode) | role-match |
| `main/.../feature/result/ResultEntries.kt` | route | - | `feature/home/HomeEntries.kt` | exact |
| `main/.../feature/result/ShareCard.kt` / `Share.kt` | component + utility | file-I/O | `feature/home/HeroCard.kt` (branding, `drawWithContent`) | partial |
| `main/.../feature/invalidphoto/InvalidPhotoScreen.kt` | component | - | `feature/scan/ScanFallbackScreens.kt` (exhaustive `when` -> `NocturneStateBlock`) | exact |
| `main/.../feature/home/HomeViewModel.kt`, `HomeScreen.kt`, new `LastReadingCard.kt` | VM/component | CRUD (read Flow) | `HomeViewModel.kt`, `HeroCard.kt` | exact |
| `main/.../PalmApp.kt` (edit: clear `cacheDir/share`) | config | file-I/O | itself (12-16) | exact |
| `main/AndroidManifest.xml` + `main/res/xml/share_paths.xml` | config | - | `res/xml/data_extraction_rules.xml` (xml res pattern) | partial |
| `main/assets/fixtures/palm_reading.{en,hi,invalid}.json` | fixture | - | `contracts/palm_reading*.json` | exact |
| `main/res/values{,-hi}/strings_result.xml` | config | - | `res/values/strings_scan.xml` (per-feature split) | exact |
| `debug/.../debug/FixtureLab.kt` + `release/.../debug/FixtureLab.kt`; `DebugEntries.kt` x2 (signature `debugEntries(backStack)`), `AppNavDisplay.kt` 72, `ShowcaseScreen.kt` | debug component | - | `debug|release/.../DebugEntries.kt`, `release/.../ScanDebug.kt` | exact |
| `app/build.gradle.kts` (edit: `fixtures.dir` system property) | config | - | line 47 `contracts.dir` | exact |
| Unit tests (`OverlayMathTest`, `LineRulesTest`, `FixtureTest`, `ReadingRulesTest`, `InvalidReasonCopyTest`, `ScanReadyViewModelTest`, `ResultViewModelTest`) + fakes (`FakePalmReadingRepository`, `FakeReadingStore`/decoder) | test | - | `test/.../contract/PalmReadingDtoTest.kt`, `test/.../feature/scan/ScanViewModelTest.kt`, `test/.../testing/FakePhotoProcessor.kt`, `core/photo/CropMathTest.kt` | exact |
| Instrumented (`AppDatabaseTest` edit + v1->v2, `ReadingStoreTest`, `ResultScreenTest`, `InvalidPhotoTest`, Home last-reading, share intent) | test | - | `androidTest/.../core/db/AppDatabaseTest.kt`, `androidTest/.../feature/home/HomeTest.kt` | exact |
| `tools/result-open.sh` (or lab walk script) | device script | - | `tools/scan-open.sh` (+ `_device.sh`, `ui.sh`) | exact |
| `ReadingEntryPoint` (instrumented seeding) | config | - | `core/data/ProfileRepository.kt` 149-153 `ProfileEntryPoint` | exact |
| Share card GraphicsLayer capture, FileProvider intent, Room v1 raw-SQL fixture | - | - | none in repo | no analog (use RESEARCH Patterns 5-6) |

## Pattern Assignments

### `core/db/AppDatabase.kt` (edit)
Keep the file layout (entity, DAO, `@Database`, `DbModule` in one file). Builder today (lines 53-57):
```kotlin
Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("palm.db").absolutePath)
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()
```
Add `.fallbackToDestructiveMigrationFrom(true, 1)`, bump `version = 2`, replace `insert`/`count` with RESEARCH Pattern 5 DAO (`upsert` REPLACE, `byId(): Flow`, `latest(): Flow`, `delete`, `all`). Update the stale comment at 21-22. `AppDatabaseTest` (androidTest, 4-field entity, `insert`/`count`) breaks at compile time: fix in the same plan.

### `core/reading/ReadingStore.kt` (file-I/O + CRUD)
**Analog:** `core/photo/ScanResultHolder.kt`. Copy the move + cleanup idiom (lines 27-37):
```kotlin
if (!dir.isDirectory && !dir.mkdirs()) throw IOException("cannot create $dir")
try {
    Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
} catch (e: IOException) {
    tmp.delete()
    throw e
}
```
Constructor takes `dir: File` (plain class, provided in the module like `holder(...)` = `ScanResultHolder(File(context.cacheDir, "scan"))`, PhotoProcessor.kt:183-185), so JVM tests use `TemporaryFolder` exactly as `ScanResultHolderTest`. KDoc "Blocking I/O" convention; or make functions `suspend` + `withContext(IO)`. Validate UUID id before `File(dir, "$id.jpg")` (RESEARCH Pitfall 3). Never store `capture.landmarks`.

### `core/reading/ReadingModule.kt` (DI)
`@Binds` analog, `core/locale/AppLocale.kt` 38-42:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class LocaleModule {
    @Binds
    abstract fun appLocale(impl: AppCompatLocale): AppLocale
}
```
`@Provides @Singleton` for the store (dir from `@ApplicationContext`) goes in a companion `object` or a second `object` module, as `PhotoModule` does. Entry point for tests: copy `ProfileEntryPoint` (ProfileRepository.kt 149-153).

### `core/reading/PalmReadingRepository.kt` / `FixturePalmReadingRepository.kt`
Interface shape as `PhotoProcessor` (`suspend fun process(source): ProcessResult`); impl `@Inject constructor`. Asset source as a `(String) -> String` lambda (RESEARCH Pitfall 7) so JVM tests read `System.getProperty("fixtures.dir")`, mirroring `PalmReadingDtoTest.contract()` (lines 17-20):
```kotlin
private fun contract(name: String): String {
    val dir = requireNotNull(System.getProperty("contracts.dir")) { "contracts.dir system property not set" }
    return File(dir, name).readText()
}
```
build.gradle.kts line 47 to extend: `unitTests.all { it.systemProperty("contracts.dir", rootProject.file("../contracts").absolutePath) }`.

### `FixtureChoice` seam + `FixtureLab.kt` (debug/release twins)
Release-null pattern, `release/.../debug/DebugEntries.kt`:
```kotlin
val debugHomeLongPress: NavKey? = null
@Suppress("UnusedReceiverParameter")
fun EntryProviderScope<NavKey>.debugEntries() = Unit
```
Debug twin registers `entry<Showcase> { ShowcaseScreen(statusViewModel = hiltViewModel()) }` with a `@Serializable data object ... : NavKey`. Add `FixtureLab` key the same way; change both twins to `debugEntries(backStack: NavBackStack<NavKey>)` and update `core/navigation/AppNavDisplay.kt:72`. Release no-op composables follow `release/.../ScanDebug.kt` (`@Suppress("UNUSED_PARAMETER") ... = Unit`). Lab puts the JPEG via `ScanResultHolder.put(jpeg, w, h, hand, landmarks, PhotoOrigin.GALLERY)`.

### `core/navigation/Routes.kt` (edit)
```kotlin
@Serializable
data class Scan(val hand: HandSide) : Route { override val showsBackground: Boolean get() = false }
fun NavBackStack<NavKey>.replaceAll(key: NavKey) { add(key); subList(0, size - 1).clear() }
fun NavBackStack<NavKey>.pushFrom(from: NavKey, to: NavKey) { if (lastOrNull() == from) add(to) }
```
New `@Serializable data class Result(val readingId: String) : Route`, `InvalidPhoto(val reason: InvalidReason, val hand: HandSide) : Route` (default sky on). Add vararg `replaceAll` overload next to the existing one; extend `test/.../navigation/PushFromTest.kt`.

### `feature/scan/ScanReadyViewModel.kt` + `ScanReadyScreen.kt` + `ScanEntries.kt` (edit)
VM state pattern from `ScanViewModel` (consumed one-shot signal, testable `io` dispatcher; test sets `apply { io = main.dispatcher }`):
```kotlin
assertTrue(vm.ui.value.ready)
vm.consumeReady()
assertFalse(vm.ui.value.ready) // consumed once, so a recomposition does not navigate again
```
Existing cleanup idiom to keep (ScanReadyViewModel 49-51): `viewModelScope.launch(Dispatchers.IO + NonCancellable) { holder.clear() }`. Replace `BitmapFactory.decodeFile` in `init` (36-42) with an injected decoder seam (RESEARCH Pitfall 6). Screen: line 160 `NocturneButton(..., onClick = {}, enabled = false)` becomes the read action; block `retake`/`BackHandler` (87-91) while in flight (Pitfall 2). Entry today: `entry<ScanReady> { ScanReadyScreen(onRetake = { backStack.popFrom(ScanReady) }) }`; add `onResult = { id -> backStack.replaceAll(Home, Result(id)) }`, `onInvalid = { r, h -> ... }`.

### `feature/result/OverlayMath.kt`, `LineRules.kt` (pure)
Analogs: `core/vision/ViewMapping.kt` (+ `ViewMappingTest`), `core/photo/CropMath.kt` (top-level pure funs, KDoc one-liners, e.g. lines 40-45 `sampleSizeFor`). Reuse `sampleSizeFor(w, h, maxEdge)` for the Home thumbnail. Code per RESEARCH Patterns 1-3; no Android types.

### `feature/result/PalmOverlay.kt`
Analog `ScanReadyScreen.kt` 113-140: photo box with `aspectRatio(capture.width / capture.height)`, `heightIn(max = window * 0.6f)` via `LocalWindowInfo.current.containerSize`, `clip(PhotoShape)` + hairline border, `Image(ContentScale.Fit)` + sibling `Canvas(Modifier.fillMaxSize())`, one `semantics { contentDescription = ... }`. Phase 5 uses 0.55 and `fitRect` instead of raw `size` mapping. Colours `NocturneColors.PalmHeart/Head/Life/Fate/Sun`; reduce motion via `LocalNocturne.current.reduceMotion` (HeroCard 62, 102). Draw-only animation reads inside `drawWithContent`/draw scope to avoid recomposition (HeroCard 120-129 comment: per-frame layout changes blocked uiautomator idling).

### `feature/result/ResultScreen.kt`
Scaffold from `ScanReadyScreen` 102-112:
```kotlin
Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
    .padding(start = NocturneSpace.S16, end = NocturneSpace.S16, bottom = NocturneSpace.S16)) {
    NocturneTopBar(stringResource(R.string.scan_ready_title), onBack = retake)
    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(top = NocturneSpace.S24)) { ... }
```
Text via `val type = LocalNocturne.current.type` (`type.titleM`, `type.caption`, `type.bodyL`); muted copy `NocturneColors.TextMuted`. Legend/traits: `NocturneChip(text, dotColor = ...)` (Chips.kt 26-31). Buttons: `NocturneButton(..., style = NocturneButtonStyle.Ghost)` for secondary. "Couldn't open" state = `PhotoGone` pattern (ScanReadyScreen 59-81) with `NocturneStateBlock`. Ask Guide: `backStack.pushFrom(key, ComingSoon(Feature.ASK_GUIDE))` (sheet already registered, HomeEntries 24-26).

### `feature/result/ResultViewModel.kt`
`HomeViewModel` pattern:
```kotlin
val profile: StateFlow<Profile?> = repo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
```
Map `dao.byId(id)` -> decoded state. Route arg via assisted inject (RESEARCH Pattern 7; first in repo) or `load(id)` from `LaunchedEffect`.

### `feature/result/ResultEntries.kt`
Copy `homeEntries(backStack)` shape (HomeEntries 15-27); register in `AppNavDisplay` next to `scanEntries`.

### `feature/invalidphoto/InvalidPhotoScreen.kt`
Analog `ScanFallbackScreens.kt` 36-89: top bar, centred `Box(weight(1f))` with scrolling column, exhaustive `when (screen)` each mapping to `NocturneStateBlock(painterResource(icon), NocturneColors.StatusWarning, title, body, primary = ... to ..., secondary = ...)`. Put the reason->(icon, title, tip) mapping in a pure `when (reason)` returning `@StringRes`/`@DrawableRes` ints so it is JVM-testable for all 7.

### Home last-reading card (`HomeViewModel.kt`, `HomeScreen.kt`, `LastReadingCard.kt`)
Add `val latest = store.latest().stateIn(...)` as above. Card styling from `HeroCard.kt` (RoundedCornerShape(NocturneRadius.Lg), `NocturneBorderWidth` border, `NocturneSpace` paddings, `Row` + `Column(weight(1f))`). Hand label via existing `feature/handselect/handLabel(hand)`.

### `PalmApp.kt` (edit)
```kotlin
// The previous process's capture never survives a restart (D-12); one directory listing
scanResults.clear()
```
Add the share dir deletion the same way (directory listing only; no DB on main).

### Tests
- JVM VM tests: `ScanViewModelTest` 24-37 (`MainDispatcherRule(StandardTestDispatcher())`, `TemporaryFolder`, real `ScanResultHolder(dir)`, `runTest(main.dispatcher)` + `advanceUntilIdle()`, `noFiles()` check for "saves nothing").
- Fakes: `testing/FakePhotoProcessor.kt` (`CompletableDeferred` `next` + recorded calls) -> `FakePalmReadingRepository` (also proves double-tap = one call).
- Drift/strict decode: `PalmReadingDtoTest` (`private val strict = Json`, `assertEquals(Json.parseToJsonElement(text), strict.encodeToJsonElement(...))`, reason substitution at 44-49 to derive the 7 invalid variants).
- Instrumented DB: `AppDatabaseTest` (in-memory builder + `BundledSQLiteDriver`, `runBlocking`, `try/finally db.close()`).
- Compose UI: `HomeTest` (`createEmptyComposeRule`, `ActivityScenario<MainActivity>`, `EntryPointAccessors` + `ProfileEntryPoint` seeding, `setEnglish`, `performScrollTo`).

### `tools/*.sh` device script
Copy `tools/scan-open.sh` header: `set -euo pipefail`, `ROOT=...`, `source "$ROOT/tools/_device.sh"`, `PKG="${PKG:-app.palm.dev.debug}"`, `A=(adb -s "$ADB_SERIAL")`, `UI=(bash "$ROOT/tools/ui.sh")`, onboarding fallback with `ui.sh launch/expect/tap/reset`.

## Shared Patterns
- **DI:** `@Module @InstallIn(SingletonComponent::class)`; interfaces via `@Binds` (AppLocale.kt 38-42), concrete file-backed classes via `@Provides @Singleton` with `@ApplicationContext` dirs (PhotoProcessor.kt 177-186). KSP only.
- **I/O:** blocking work on `Dispatchers.IO`; cleanup that must survive a pop uses `Dispatchers.IO + NonCancellable` (ScanReadyViewModel 49-51). Catch `IOException` explicitly.
- **Navigation:** every transition through `pushFrom`/`popFrom`/`replaceAll` guards (Routes.kt 37-51); entries are `EntryProviderScope<NavKey>.xxxEntries(backStack)`.
- **UI:** `LocalNocturne.current.type` styles, `NocturneColors`/`NocturneSpace`/`NocturneRadius` tokens only, `stringResource` from a per-feature `strings_*.xml` with a `values-hi` twin, `safeDrawing` insets + `S16` side padding, state screens via `NocturneStateBlock`.
- **Debug/release twins:** same package `app.palm.debug`, same signatures, release is `= Unit`/`null`.
- **Privacy:** never log readings, traces, image bytes; landmarks never persisted.

## No Analog Found
| File | Reason |
|---|---|
| Share capture (`rememberGraphicsLayer` + `record` + `toImageBitmap`, HARDWARE->ARGB copy) | No off-screen render in repo; use RESEARCH Pattern 6, gate on emulator |
| FileProvider manifest entry + `share_paths.xml` + `ACTION_SEND` intent | No provider/share yet; RESEARCH Pattern 6, authority `${context.packageName}.share` |
| Room v1 raw-SQL fixture for v1->v2 open test | No migration test yet; RESEARCH Pattern 5 (identity hash from `schemas/.../1.json`) |
| `ResultViewModel` assisted injection | First route-arg VM; alternative `load(id)` keeps existing patterns |

## Metadata
**Analog search scope:** `android/app/src/{main,debug,release,test,androidTest}`, `tools/`
**Files scanned:** ~25 read, 150 listed
**Pattern extraction date:** 2026-11-28
