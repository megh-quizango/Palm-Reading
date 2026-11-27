# Phase 3: Client Shell - Pattern Map

**Mapped:** 2026-11-27
**Files analyzed:** 24 (new + modified)
**Analogs found:** 20 / 24

All paths are relative to `android/app/src/` unless noted. All analogs are git-tracked (checked with `git ls-files android`). Package root `app.palm`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `main/.../MainActivity.kt` (modify: AppCompatActivity, splash) | activity | event-driven | itself | exact |
| `main/.../MainViewModel.kt` | viewmodel | request-response | `main/.../showcase/BackendStatus.kt` (BackendStatusViewModel) | role-match |
| `main/.../core/navigation/Routes.kt` | config (NavKeys) | n/a | `main/.../core/navigation/AppNavDisplay.kt` lines 14-15 | exact |
| `main/.../core/navigation/AppNavDisplay.kt` (modify) | route host | event-driven | itself | exact |
| `main/.../core/data/Profile.kt` | model | n/a | `main/.../contract/PalmReadingDto.kt` (`HandSide` enum line ~72) | partial |
| `main/.../core/data/ProfileRepository.kt` | repository/service | CRUD (DataStore) | `main/.../core/db/AppDatabase.kt` (DAO suspend API) | role-match |
| `main/.../core/data/DataStoreModule.kt` | DI module | n/a | `main/.../core/db/AppDatabase.kt` lines 48-58 (`DbModule`) | exact |
| `main/.../core/locale/AppLanguage.kt` | utility | request-response | none | none |
| `main/.../core/designsystem/NocturneBackground.kt` (modify: 6 layers, `sky` param) | component | streaming (animation) | itself | exact |
| `main/.../core/designsystem/NocturneTheme.kt` (modify comment line ~136) | config | n/a | itself | exact |
| `main/.../feature/onboarding/LanguageScreen.kt` + VM | screen + viewmodel | CRUD | `showcase/BackendStatus.kt` + `showcase/ShowcaseScreen.kt` | role-match |
| `main/.../feature/onboarding/IntroScreen.kt` | screen | n/a | `showcase/ShowcaseScreen.kt` | role-match |
| `main/.../feature/onboarding/NameScreen.kt` + VM | screen + viewmodel | CRUD | same as Language | role-match |
| `main/.../feature/onboarding/BirthScreen.kt` + VM | screen + viewmodel | CRUD | same as Language | role-match |
| `main/.../feature/onboarding/NameRules.kt`, `BirthRules.kt` | utility (pure) | transform | `core/network/HealthClient.kt` `causeOf` (pure fn tested by `HealthCauseTest`) | role-match |
| `main/.../feature/home/HomeScreen.kt` + `HomeViewModel.kt` | screen + viewmodel | request-response | `ShowcaseScreen.kt` + `FeatureCard.kt` | role-match |
| `main/.../feature/home/Greeting.kt` | utility (pure) | transform | `causeOf` pattern | role-match |
| `main/.../feature/home/ComingSoonSheet.kt`, `ExitDialog.kt` | component | event-driven | `core/designsystem/components/FeatureCard.kt` (token styling) | partial |
| `main/.../feature/handselect/HandSelectScreen.kt` | screen | event-driven | `ShowcaseScreen.kt` | role-match |
| `main/.../feature/scan/ScanPlaceholder.kt` | screen | n/a | `ShowcaseScreen.kt` | role-match |
| `debug/java/app/palm/showcase/*` (moved ShowcaseScreen, BackendStatus) + `debug/.../DebugEntries.kt` | screen / nav | n/a | existing `main/.../showcase/*` (git mv) | exact |
| `release/java/app/palm/debug/DebugEntries.kt` | nav stub | n/a | none (same signature as debug) | none |
| `main/res/values/strings.xml`, `values-hi/strings.xml`, `xml/locales_config.xml` | config | n/a | existing `main/res/values/strings.xml` (`R.string.app_name`) | role-match |
| `test/.../GreetingTest.kt`, `NameRulesTest.kt`, `BirthRulesTest.kt`, `StartRouteTest.kt`, `ProfileRepositoryTest.kt`, VM tests | test | n/a | `test/java/app/palm/core/network/HealthCauseTest.kt` | exact |
| `androidTest/.../OnboardingFlowTest.kt` | instrumented test | n/a | `androidTest/java/app/palm/core/db/AppDatabaseTest.kt` | role-match |
| `app/build.gradle.kts`, `gradle/libs.versions.toml` (modify) | config | n/a | themselves (`testImplementation(libs.junit)` line 65) | exact |

## Pattern Assignments

### `core/navigation/Routes.kt` + `AppNavDisplay.kt`
**Analog:** `main/java/app/palm/core/navigation/AppNavDisplay.kt` (whole file, 31 lines)

Key shape (lines 14-30):
```kotlin
@Serializable
data object Showcase : NavKey

@Composable
fun AppNavDisplay() {
    val backStack = rememberNavBackStack(Showcase)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Showcase> { ShowcaseScreen(statusViewModel = hiltViewModel()) }
        },
    )
}
```
Conventions: keep both decorators; `hiltViewModel()` passed into the screen at the `entry<>` call (screens take the VM as a parameter, not created internally). Move keys into `Routes.kt` as `@Serializable sealed interface Route : NavKey { val sky: Boolean get() = true }`; `Scan(hand: app.palm.contract.HandSide)` reuses the contract enum (`PalmReadingDto.kt`: `@Serializable enum class HandSide { @SerialName("left") LEFT, @SerialName("right") RIGHT }`). Remove `import app.palm.showcase.ShowcaseScreen` (line 11); replace with `debugEntries()` from the source-set-split `DebugEntries.kt`. Wrap `NavDisplay` once in `NocturneBackground(sky = ...)` (RESEARCH Pattern 5).

### `MainActivity.kt` (modify)
**Analog:** itself:
```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dark-only app: light bar icons regardless of the system theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { NocturneTheme { AppNavDisplay() } }
    }
}
```
Keep the edge-to-edge block and comment. Add `installSplashScreen()` before `super.onCreate` and `setKeepOnScreenCondition { vm.startRoute.value == null }` (RESEARCH Pattern 2, Pitfalls 1-2: AppCompat theme parent needed if switching to `AppCompatActivity`).

### ViewModels (`MainViewModel`, `LanguageViewModel`, `NameViewModel`, `BirthViewModel`, `HomeViewModel`)
**Analog:** `main/java/app/palm/showcase/BackendStatus.kt`
```kotlin
sealed interface BackendStatus {
    data object Checking : BackendStatus
    data object Connected : BackendStatus
    data class Unreachable(val cause: String) : BackendStatus
}

@HiltViewModel
class BackendStatusViewModel @Inject constructor(private val health: HealthClient) : ViewModel() {
    private val _state = MutableStateFlow<BackendStatus>(BackendStatus.Checking)
    val state: StateFlow<BackendStatus> = _state.asStateFlow()
    ...
        probe = viewModelScope.launch { _state.value = when (val r = health.check()) { ... } }
}
```
And the UI side:
```kotlin
@Composable
fun BackendStatusRow(viewModel: BackendStatusViewModel, modifier: Modifier = Modifier) {
    val status by viewModel.state.collectAsStateWithLifecycle()
```
Conventions: `@HiltViewModel` + `@Inject constructor(repo: ProfileRepository)`; sealed-interface or data-class UI state in a private `MutableStateFlow` exposed via `asStateFlow()` (or `repo.profile.stateIn(viewModelScope, ...)`); writes in `viewModelScope.launch`; screens collect with `collectAsStateWithLifecycle()`. Short KDoc on non-obvious behaviour (see `retry()` single-flight comment).

### `core/data/DataStoreModule.kt`
**Analog:** `main/java/app/palm/core/db/AppDatabase.kt` lines 48-58
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DbModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("palm.db").absolutePath)
            ...build()
}
```
Copy the shape: `object DataStoreModule`, `@Provides @Singleton fun dataStore(@ApplicationContext context): DataStore<Preferences> = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("profile") }`. Bind the repository with `@Provides`/`@Binds` in the same module (one module, not two).

### `core/data/ProfileRepository.kt`
**Analog:** `ReadingDao` in `AppDatabase.kt` lines 31-41 (suspend write API, single file holding entity + DAO + module). Follow: one file may hold `Profile` data class + repository; suspend setters; `val profile: Flow<Profile>` mapped from `dataStore.data`. Constructor takes `DataStore<Preferences>` so the JVM test can pass a temp-file store (RESEARCH "DataStore disk round trip", Pitfalls 9-10).

### `core/designsystem/NocturneBackground.kt` (extend, do not replace)
**Analog:** itself, lines 32-90. Keep these rules verbatim:
```kotlin
private const val SEED = 20261126 // fixed so the sky is the same on every launch
...
// reduceMotion is fixed for the process, so this branch never flips within a composition
val clock = if (LocalNocturne.current.reduceMotion) null else rememberInfiniteTransition(label = "stars").let { t ->
    StarClock(
        twinkle = t.loop(NocturneDurations.Ambient, NocturneEasings.GentleSine, RepeatMode.Reverse),
        twinkleFast = t.loop(NocturneDurations.AmbientFast, NocturneEasings.GentleSine, RepeatMode.Reverse),
        drift = t.loop(NocturneDurations.AmbientSlow, LinearEasing, RepeatMode.Restart),
    )
}
Box(modifier.fillMaxSize().background(NocturneColors.BgBase).drawBehind {
    // Animated values are read only here, so a frame redraws without recomposing
```
Extend `Layers` (`StarLayer(count, minDp, maxDp, color, driftDp)`) from 3 to 6 entries using Figma "5 · Motion" values; add `sky: Boolean = true` param; when false also null the clock and paint `NocturneExtras.bgCamera`. Update the KDoc lines 66-70. No `Brush` allocation in `drawBehind` (pre-build with `remember`, as `FeatureCard.kt` does with `remember { nocturneBrush(...) }`).

### Screens (Language, Intro, Name, Birth, Home, HandSelect, ScanPlaceholder)
**Analog:** `main/java/app/palm/showcase/ShowcaseScreen.kt` lines 41-57
```kotlin
@Composable
fun ShowcaseScreen(statusViewModel: BackendStatusViewModel) {
    val s16 = NocturneSpace.S16
    val padding = WindowInsets.safeDrawing.add(WindowInsets(s16, s16, s16, s16)).asPaddingValues()
    NocturneBackground {
        LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(NocturneSpace.S24)) {
            item { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall) }
```
Copy: `WindowInsets.safeDrawing` + `NocturneSpace` padding (edge-to-edge), `NocturneSpace.*` spacing, `stringResource(...)` for every string (D-06; Showcase's literal strings like `"Retry"` are debug-only and must not be copied). Difference: screens must NOT wrap themselves in `NocturneBackground` (root owns it) and must not paint opaque backgrounds. Text styles from `LocalNocturne.current.type` (`titleM`, `bodyM`, `caption`); colours from `NocturneSemantic.*`. No fixed heights on text containers (D-07).

Components to reuse, not re-create:
- `NocturneButton(text, onClick, style = NocturneButtonStyle.Ghost|...)` — `components/Buttons.kt:35` (Continue/Next/Get started, dialog buttons).
- `NocturneChip(text, modifier, dotColor)` — `components/Chips.kt:26` (gender chips, hand choice can build on it).
- `FeatureCard(title, body, modifier, onClick)` — `components/FeatureCard.kt:29` (Home grid tiles; hero card can wrap/extend its glow + foil-border style lines 30-41).

### Pure rules (`Greeting.kt`, `NameRules.kt`, `BirthRules.kt`, start-route fn)
**Analog:** `causeOf(Throwable)` in `core/network/HealthClient.kt`, tested in `HealthCauseTest`. Top-level internal/public pure functions with explicit inputs (pass `LocalTime`/`LocalDate` "today" in, never read the clock inside) so tests are one-liners.

### Unit tests
**Analog:** `test/java/app/palm/core/network/HealthCauseTest.kt`
```kotlin
class HealthCauseTest {
    @Test fun timeout() = assertEquals("Timed out: is tools/run-backend.sh running?", causeOf(SocketTimeoutException("timeout")))
```
Convention: JUnit 4 (`org.junit.Test`, `org.junit.Assert.*`), expression-bodied one-line tests, same package as the code under test. VM tests add Turbine + `kotlinx-coroutines-test` (`Dispatchers.setMain`). Gradle: add next to `testImplementation(libs.junit)` in `app/build.gradle.kts:65`, versions in `gradle/libs.versions.toml`.

### Instrumented tests
**Analog:** `androidTest/java/app/palm/core/db/AppDatabaseTest.kt`
```kotlin
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    @Test
    fun insertThenReadById() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ...
        try { ... } finally { db.close() }
```
Compose UI tests add `createAndroidComposeRule<MainActivity>()` (new dependency `ui-test-junit4` via BOM).

### Debug/release source sets
Move `main/java/app/palm/showcase/{ShowcaseScreen,BackendStatus}.kt` to `debug/java/app/palm/showcase/` with `git mv` (no edits). `HealthClient.kt` stays in main. Add `DebugEntries.kt` in `debug/` and `release/` with identical signatures (RESEARCH line 433).

## Shared Patterns

- **DI:** `@AndroidEntryPoint` activity, `@HiltViewModel` + `@Inject constructor`, `object XModule` with `@Module @InstallIn(SingletonComponent::class)` (`AppDatabase.kt:48-58`).
- **State:** private `MutableStateFlow` + `asStateFlow()`; UI collects with `collectAsStateWithLifecycle()` (`BackendStatus.kt:95-96,119`).
- **Design tokens only:** `NocturneSpace`, `NocturneColors`, `NocturneSemantic`, `NocturneDurations`, `NocturneEasings`, `LocalNocturne.current.type`; no raw dp/colours except where `NocturneBackground.kt` already does star sizes.
- **Comments:** one line, explain the why (e.g. `// Unset on some phones, so the default must be 1f` in `NocturneTheme.kt`).
- **Reduce motion:** read `LocalNocturne.current.reduceMotion` (set in `NocturneTheme.kt` from `ANIMATOR_DURATION_SCALE`, default 1f); pager/transition animations should respect it too.
- **Locale:** `NocturneTheme.kt` derives `devanagari` from `LocalConfiguration.current.locales[0].language == "hi"`; language switch via AppCompat recreate needs no theme change, only the comment update.

## No Analog Found

| File | Role | Reason / source |
|---|---|---|
| `core/locale/AppLanguage.kt` | utility | No locale code yet; use RESEARCH Pattern 1 (`AppCompatDelegate.setApplicationLocales`, `autoStoreLocales`). |
| `release/.../DebugEntries.kt` | nav stub | First source-set split; RESEARCH line 433. |
| `feature/home/ComingSoonSheet.kt`, `ExitDialog.kt` | component | No sheets/dialogs yet; M3 `ModalBottomSheet`/`AlertDialog` with Nocturne tokens (style per `FeatureCard.kt`). |
| `res/xml/locales_config.xml` | config | New; RESEARCH Pattern 1. |
| Birth pickers | component | M3 `DatePicker`/`TimePicker`; RESEARCH Pattern 6 + Pitfall 5 (UTC millis to LocalDate). |

## Metadata

**Analog search scope:** `android/app/src/{main,test,androidTest}`, `android/app/build.gradle.kts`
**Files scanned:** 12
**Pattern extraction date:** 2026-11-27
