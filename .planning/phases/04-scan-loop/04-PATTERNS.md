# Phase 4: Scan Loop - Pattern Map

**Mapped:** 2026-11-27
**Files analyzed:** 27 (new + modified)
**Analogs found:** 20 / 27 (all analogs are git-tracked; base `android/app/src/` unless noted)

## File Classification

Package layout from 04-RESEARCH.md "Recommended Project Structure": `feature/scan`, `core/vision`, `core/photo`.

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `main/.../core/navigation/Routes.kt` (MOD: add `ScanReady`) | route | n/a | same file, `Scan` (l.23-26) | exact |
| `main/.../feature/handselect/HandSelectEntries.kt` (MOD: drop Scan line) | route | n/a | same file l.18 | exact |
| `main/.../core/navigation/AppNavDisplay.kt` (MOD: add `scanEntries(backStack)`) | route | n/a | same file l.66-70 | exact |
| `main/.../feature/handselect/ScanPlaceholderScreen.kt` (DELETE) | component | - | - | - |
| `main/.../feature/scan/ScanEntries.kt` | route | n/a | `feature/handselect/HandSelectEntries.kt` | exact |
| `main/.../feature/scan/ScanScreen.kt` | component | event-driven | `feature/handselect/ScanPlaceholderScreen.kt` (shell) + `HandSelectScreen.kt` | role-match |
| `main/.../feature/scan/ScanFallbackScreens.kt` | component (stateless) | request-response | `ScanPlaceholderScreen.kt` | exact |
| `main/.../feature/scan/ScanReadyScreen.kt` | component | n/a | `ScanPlaceholderScreen.kt` | exact |
| `main/.../feature/scan/ScanViewModel.kt` | ViewModel | event-driven | `feature/onboarding/NameViewModel.kt` | exact |
| `main/.../feature/scan/CameraPermission.kt` | utility (pure reducer) | transform | `feature/onboarding/NameRules.kt` | role-match |
| `main/.../core/vision/HandFrame.kt` | model | n/a | `core/data/ProfileRepository.kt` `Profile` (l.40-48) | role-match |
| `main/.../core/vision/HandGuidance.kt` (+ `GuidanceConfig`) | utility (pure) | transform | `feature/onboarding/NameRules.kt` | role-match |
| `main/.../core/vision/GuidanceStabilizer.kt` | utility (pure, stateful) | event-driven | `NameRules.kt` (style only) | partial |
| `main/.../core/vision/HandDetector.kt` (interface + MediaPipe IMAGE impl + Hilt module) | service | request-response | `core/data/ProfileRepository.kt` (interface + impl + `@Module`) | role-match |
| `main/.../core/vision/LiveHandTracker.kt` | service (Android) | streaming | none | no analog |
| `main/.../core/photo/CropMath.kt` | utility (pure) | transform | `NameRules.kt` | role-match |
| `main/.../core/photo/PhotoProcessor.kt` | service | file-I/O / transform | `core/network/HealthClient.kt` (`withContext(Dispatchers.IO)` l.32) | partial |
| `main/.../core/photo/ScanResultHolder.kt` | store (@Singleton) | file-I/O | `ProfileRepository.kt` (`@Singleton`, `@ApplicationContext`, l.130-140) | role-match |
| `main/AndroidManifest.xml` (MOD: CAMERA + `uses-feature camera.any required=false`) | config | - | same file | exact |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` (MOD: camera, mediapipe, exif; `noCompress "task"`) | config | - | same files (`androidResources` l.35-38) | exact |
| `main/assets/hand_landmarker.task` + `main/assets/licenses/MediaPipe-Apache-2.0.txt` | asset | - | `main/assets/licenses/*-OFL.txt` | exact |
| `debug/.../debug/` record-landmarks hook (+ release twin) | debug tool | file-I/O | `debug/.../DebugEntries.kt` + `release/.../DebugEntries.kt` | exact |
| `test/.../core/vision/HandGuidanceTest.kt`, `GuidanceStabilizerTest.kt`, `core/photo/CropMathTest.kt`, `feature/scan/CameraPermissionTest.kt`, `ScanViewModelTest.kt` | test | - | `NameRulesTest.kt`, `NameViewModelTest.kt`, `PalmReadingDtoTest.kt` (fixture file loading) | exact |
| `test/resources/landmarks/*.json` | fixture | - | none (contracts/ samples via `contracts.dir`) | partial |
| `androidTest/.../core/photo/PhotoProcessorTest.kt`, `feature/scan/ScanFallbackTest.kt` + `androidTest/assets/photos/*.jpg` | test | - | `androidTest/.../handselect/HandSelectTest.kt` | role-match |
| `tools/gen_landmark_fixtures.py`, `tools/gen_test_photos.py` (+ `--check`) | generator script | batch | `tools/gen_tokens.py` + `tools/test_gen_tokens.py` | exact |
| `tools/check-scan-session.sh` | device script | batch | `tools/check-jank.sh` + `tools/_device.sh` | exact |

## Pattern Assignments

### `Routes.kt` (MOD) - add ScanReady
Analog l.23-26. Copy the opt-out shape; ScanReady needs no args (capture comes from `ScanResultHolder`, D-01). Decide sky on/off per UI-SPEC.
```kotlin
@Serializable
data class Scan(val hand: HandSide) : Route {
    override val showsBackground: Boolean get() = false
}
```
Use `pushFrom`/`popFrom` (l.40-46) for every navigation action (double-tap guard). Tests in `test/.../core/navigation/PushFromTest.kt`.

### `feature/scan/ScanEntries.kt`
Analog `HandSelectEntries.kt` l.11-19 (whole file). Move the `entry<Scan>` line here and register `scanEntries(backStack)` next to `handSelectEntries(backStack)` in `AppNavDisplay.kt` l.66-70.
```kotlin
fun EntryProviderScope<NavKey>.handSelectEntries(backStack: NavBackStack<NavKey>) {
    entry<HandSelect> { HandSelectScreen(onBack = { backStack.popFrom(HandSelect) }, onContinue = { hand -> backStack.pushFrom(HandSelect, Scan(hand)) }) }
    entry<Scan> { key -> ScanPlaceholderScreen(hand = key.hand, onBack = { backStack.popFrom(key) }) }
}
```
ViewModel: `hiltViewModel()` inside the entry (see `debug/DebugEntries.kt`: `ShowcaseScreen(statusViewModel = hiltViewModel())`); Nav3 entry-scoped via `rememberViewModelStoreNavEntryDecorator` already in `AppNavDisplay.kt`.

### `ScanScreen.kt`, `ScanFallbackScreens.kt`, `ScanReadyScreen.kt`
Analog `ScanPlaceholderScreen.kt` l.26-55: opaque `NocturneColors.BgCamera`, `windowInsetsPadding(WindowInsets.safeDrawing)`, `NocturneTopBar(title, onBack)`, `LocalNocturne.current.type`, `NocturneSemantic.*` colors, `NocturneSpace.*`, all copy via `stringResource(R.string....)` with en + `values-hi`. Reuse `handLabel(hand)` (defined in handselect package; make it `internal`/move if needed). Buttons/chips from `core/designsystem/components/Buttons.kt`, `Chips.kt` - do not build new ones. Fallback screens stateless (`state`, lambdas, `modifier: Modifier = Modifier`) so the Compose test drives them directly. Preview: `AndroidView { PreviewView }` - no analog.

### `ScanViewModel.kt`
Analog `NameViewModel.kt` l.17-72.
```kotlin
data class NameUiState(val name: String = "", ..., val saveFailed: Boolean = false)

@HiltViewModel
class NameViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(NameUiState())
    val state: StateFlow<NameUiState> = _state.asStateFlow()
    fun save(next: () -> Unit) { viewModelScope.launch { try { ...; next() } catch (_: IOException) { _state.update { it.copy(saveFailed = true) } } } }
```
Conventions: immutable `data class XUiState` with defaults; `_state.update { it.copy(...) }`; navigation passed as a `next: () -> Unit` callback, not an event flow; expected failures caught narrowly and surfaced as a state flag. Inject `HandDetector` interface + `ScanResultHolder` so the JVM test can fake them.

### `CameraPermission.kt`, `HandGuidance.kt`, `CropMath.kt` (pure)
Analog `NameRules.kt` l.1-38: top-level `const val`s, a result `enum class`, one top-level pure function, KDoc citing the decision ID (`(D-09)`), no Android imports. Thresholds go in a `data class GuidanceConfig(...)` with defaults (D-07) instead of loose consts.
```kotlin
enum class NameCheck { OK, EMPTY, TOO_SHORT, TOO_LONG, BAD_CHARS }
/** First-name rule (D-09): ... */
fun checkName(raw: String): NameCheck { ... }
```

### `GuidanceStabilizer.kt`
No stateful pure analog. Use a plain class fed `(raw, timestampMs)` returning the shown state (RESEARCH Pattern 3); same no-Android, KDoc-with-D-ID style.

### `HandFrame.kt`
Analog `Profile` data class (`ProfileRepository.kt` l.40-48): plain `data class` with defaults, Kotlin/JDK types only (floats/lists, no `NormalizedLandmark`).

### `HandDetector.kt` / `ScanResultHolder.kt` (DI)
Analog `ProfileRepository.kt`: interface (l.51-58) + `@Inject constructor` impl (l.85) + Hilt module (l.130-140):
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    @Singleton
    fun store(@ApplicationContext context: Context): DataStore<Preferences> = ...
```
`ScanResultHolder` can be `@Singleton class ScanResultHolder @Inject constructor(@ApplicationContext ctx: Context)` (no module needed). Background work as in `HealthClient.kt` l.32: `suspend fun ... = withContext(Dispatchers.IO) { ... }` (use `Default` for bitmap CPU work).

### `PhotoProcessor.kt`
Partial analog `HealthClient.kt` (suspend + `withContext`, `internal fun causeOf` for testable mapping l.51). Return a sealed result (Ok / NoHand / Unsupported) rather than throwing, mirroring `NameCheck`. Never log bytes/landmarks (D-12).

### Debug record-landmarks control
Analog `debug/.../DebugEntries.kt` + `release/.../DebugEntries.kt`: same symbol in both source sets, release twin is a no-op/null.
```kotlin
/** Release twin of the debug file: no showcase, no Home long-press target (D-03). */
val debugHomeLongPress: NavKey? = null
```

### Manifest / Gradle / assets
`AndroidManifest.xml`: add `<uses-permission android:name="android.permission.CAMERA" />` beside INTERNET, plus `<uses-feature android:name="android.hardware.camera.any" android:required="false" />` (Pitfall 9). `build.gradle.kts` `androidResources {}` l.35-38: add `noCompress += "task"`. Catalog: add version refs + `module =` entries like l.9-38 (`androidx-activity-compose = { module = "...", version.ref = "activity" }`). License text next to existing `assets/licenses/*-OFL.txt`.

### JVM tests
- Pure: `NameRulesTest.kt` style - one-line `@Test fun name() = assertEquals(expected, f(input))`, header KDoc citing the research pitfall/table.
- ViewModel: `NameViewModelTest.kt` - `@get:Rule val main = MainDispatcherRule()` (`test/.../testing/MainDispatcherRule.kt`), fakes in `test/.../testing/` (copy `FakeProfileRepository` shape for `FakeHandDetector`, with a `fail...` flag).
- Fixtures: `PalmReadingDtoTest.kt` l.14-20 reads files via a system property set in `build.gradle.kts` `testOptions.unitTests.all { it.systemProperty(...) }` (l.43-46). For `app/src/test/resources/landmarks/` prefer classpath `javaClass.getResource("/landmarks/x.json")` (no gradle change); decode with kotlinx `Json`.

### Instrumented tests
Analog `HandSelectTest.kt` l.34-70: `@RunWith(AndroidJUnit4::class)`, `createEmptyComposeRule()` + `ActivityScenario` or `createComposeRule()` for stateless fallback screens, `setEnglish(instrumentation)` and locale reset in `@After`, `waitUntilAtLeastOneExists(hasText(...), 10_000)`. Hilt objects via `EntryPointAccessors.fromApplication(..., XEntryPoint::class.java)` (see `ProfileEntryPoint`). Photo assets from `InstrumentationRegistry.getInstrumentation().context.assets`.

### `tools/gen_landmark_fixtures.py`, `tools/gen_test_photos.py`
Analog `tools/gen_tokens.py` l.1-20 + `tools/test_gen_tokens.py`:
```python
"""Generate ... Usage: python -I tools/gen_tokens.py [--check]
--check regenerates in memory and exits 1 if a committed output differs.
Stdlib only. ..."""
ROOT = Path(__file__).resolve().parent.parent
```
Run with `python -I`; a `generate()` pure function returning text so tests compare in memory; tests `sys.path.insert(0, ...)` then `unittest`. Seeded RNG; `gen_test_photos.py` uses Pillow from `backend/.venv` (state that in the docstring).

### `tools/check-scan-session.sh`
Analog `tools/check-jank.sh` l.1-49 (whole file): header usage + gate + Env comment block, `set -euo pipefail`, `ROOT=...`, arg regex validation exit 2, `source "$ROOT/tools/_device.sh"` (gives `ADB_SERIAL`), `A=(adb -s "$ADB_SERIAL")`, `PKG="${PKG:-app.palm.dev.debug}"`, the `num()` gfxinfo parser and deadline-missed/vsync fallback, `FAILED:`/`OK:` messages. Change `MIN_FRAMES` to the low floor (RESEARCH Pitfall 7) and add logcat ANR + `dumpsys thermalservice` / `battery` polling. Wrap device usage with `tools/device-lock.sh` as Phase 3 scripts do.

## Shared Patterns
- **Decision IDs in KDoc/comments** (`(D-07)`) on every non-obvious rule - all analogs.
- **Strings**: every user-visible text in `res/values/strings.xml` and `res/values-hi/strings.xml`; no literals in composables.
- **Design system only**: `NocturneColors/Semantic/Space`, `LocalNocturne.current.type`, components from `core/designsystem/components`.
- **Navigation guards**: `pushFrom`/`popFrom` only.
- **DI**: Hilt `@HiltViewModel` + `@Inject constructor`; `@Singleton` app-scoped services; interfaces where a JVM fake is needed.
- **Errors**: catch narrow expected exceptions, expose as UI state; no crash paths, no logging of user data.
- **Pure logic split**: Android-free files for anything unit-tested (Rules.kt pattern), Android wrappers kept thin.

## No Analog Found
| File | Role | Reason |
|---|---|---|
| `core/vision/LiveHandTracker.kt` | service, streaming | No camera/MediaPipe code yet; use RESEARCH "Live analyzer" + "Landmarker options" |
| CameraX `LifecycleCameraController` + `AndroidView(PreviewView)` in `ScanScreen.kt` | component | Use RESEARCH Pattern 1 and Pattern 4 |
| Permission launcher / `LifecycleEventEffect(ON_RESUME)` | component | Use RESEARCH Pattern 5 |
| Bounded decode in `PhotoProcessor.kt` | service | Use RESEARCH Pattern 6 |
| `GuidanceStabilizer.kt` | stateful pure | Use RESEARCH Pattern 3 |
| `test/resources/landmarks/` loading | fixture | First classpath resource in the project |
| `ScanResultHolder` cacheDir file | store | No cacheDir use yet; keep to one file, delete on Retake and app start (D-12) |

## Metadata
**Analog search scope:** `android/app/src/{main,debug,release,test,androidTest}`, `tools/`
**Files scanned:** ~20 read, ~100 listed
**Note:** orchestrator's padded phase said "03"; file written as `04-PATTERNS.md` per the stated output path.
