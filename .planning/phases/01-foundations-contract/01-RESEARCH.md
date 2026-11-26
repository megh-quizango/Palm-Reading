# Phase 1: Foundations & Contract - Research

**Researched:** 2026-11-26
**Domain:** Android Gradle scaffold (AGP 9 built-in Kotlin) + Compose design-token theme + phone-to-PC dev networking + FastAPI skeleton + shared JSON contract
**Confidence:** MEDIUM-HIGH. Environment facts were probed on this machine and phone in this session. Library APIs come from official docs (the research seam rates `webfetch` LOW by default; the sources themselves are developer.android.com and Maven metadata). The toolchain combination itself is unproven until the gate build runs.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

The user asked Claude to decide all gray areas ("use your best approach instead of discussing"). Decisions below are Claude's, grounded in research/SUMMARY.md.

#### What Phase 1 shows on the phone
- **D-01:** The app opens on a single scrollable **Nocturne showcase screen**: all colour roles as swatches, the 11 text styles, primary / secondary / ghost buttons (incl. the AD chip variant), one feature card, one chip, the 5 palm-line overlay colours, a simple animated star background (static when system animations are off), and a backend status chip.
- **D-02:** The backend status chip shows Connected / Unreachable with the base URL and a Retry button (no Logcat-only check), so `adb reverse` problems can be diagnosed on the phone.
- **D-03:** After Phase 3 the showcase becomes a debug-only screen (debug source set, reached from a hidden debug entry); it is not shipped in release builds.
- **D-04:** The Phase 1 background is a lightweight placeholder (a few drifting/twinkling star layers drawn in the draw phase). The full 6-layer layered background (FND-03) is Phase 3 work; build it as a reusable composable so Phase 3 extends rather than replaces it.

#### Fonts & Hindi text
- **D-05:** Bundle fonts as resources (no downloadable Google Fonts — offline-first, predictable on low-end phones): Cormorant Garamond SemiBold (display) and Manrope Regular/Medium/SemiBold/Bold (UI), all OFL. Use static TTFs, subset to Latin + Latin-Ext to keep APK size down.
- **D-06:** Hindi/Devanagari: bundle **Noto Sans Devanagari** (Regular/Medium/SemiBold/Bold, OFL) as the fallback for the Manrope roles, and **Noto Serif Devanagari** SemiBold for the display role, via Compose `FontFamily` fallback chains, so Hindi never renders as tofu boxes. The showcase screen includes one Hindi sample line per text style to prove it.
- **D-07:** Text styles come from the generated tokens (no hand-copied sizes); line heights must stay auto/unclipped for Devanagari (taller ascenders/matras).

#### App identity (Claude's discretion)
- **D-08:** Kotlin package / `namespace` = `app.palm` (internal, freely renameable). `applicationId` = `app.palm.dev` for now, defined once in Gradle with a debug suffix `.debug`. — **Reversibility:** one-way once uploaded to Play — the real applicationId must be chosen with the brand before the first Play upload (REL-03, v2); until then it is reversible. Flag in STATE.md as a pre-release decision.
- **D-09:** Display name "[AppName]" in `strings.xml` (single resource to rename); launcher icon = the [Guide] orb illustration on Nocturne bg as an adaptive icon (foreground/background/monochrome layers) generated from the Figma illustration.

#### Dev setup & secrets (Claude's discretion)
- **D-10:** Monorepo layout at repo root: `android/`, `backend/`, `contracts/`, `tools/` (existing `tools/adb` kept). Reference material (`replica/`, `_extract/`, xapk) untouched; `_extract/` and the xapk stay out of builds.
- **D-11:** Anthropic key lives in `backend/.env` (`ANTHROPIC_API_KEY`), git-ignored, with a committed `.env.example`. Phase 1's `/healthz` must work without a key (it reports `anthropic_key: configured|missing` but does not call Claude). The user supplies the key before Phase 2.
- **D-12:** One-command scripts in `tools/` (bash, quoting the space in `E:/Palm Reading`): `dev-reverse.sh` (adb reverse tcp:8000), `run-backend.sh` (uv, Python 3.13, uvicorn on 127.0.0.1:8000, no --reload during job tests), `install-debug.sh` (gradlew installDebug + launch). Wireless adb is the default (the Redmi is already paired over TLS); scripts detect the single connected device and fail clearly when none or several are attached.
- **D-13:** Install the missing Android SDK packages (platforms;android-36 and android-37, build-tools to match) with `sdkmanager` as part of the scaffold task; only `android-31`/`33` are installed today.

#### Contract
- **D-14:** `contracts/palm_reading.v1.schema.json` + `contracts/palm_reading.sample.json` (+ one invalid-photo sample per reason or at least one) are the single source; Kotlin DTOs and Pydantic models are written to match and a test on each side parses the samples unchanged. Contract per research SUMMARY sign-offs: normalised 0..1 `[x,y]` traces, `trace_source` = `llm|template|none`, `image_size` = size sent to Claude, `verdict.ok=false` + 7-value reason enum for unusable photos, `schema_version: 1`. — **Reversibility:** costly — once Phases 2/5 build on it, changing field shapes touches both sides; bump `schema_version` instead of mutating v1.

### Claude's Discretion
- All areas above were delegated. Planner/executor may refine details (exact showcase layout, script names, font subsetting tool) without asking, as long as D-01..D-14 hold.

### Deferred Ideas (OUT OF SCOPE)
- Full 6-layer animated background with parallax/tilt — Phase 3 (FND-03).
- Final applicationId and brand name/icon — before first Play upload (REL-03, v2).
</user_constraints>

> **Correction to D-13 (environment fact, not a decision change):** the SDK is already complete. `platforms;android-36`, `platforms;android-36.1`, `platforms;android-37.0`, `build-tools;36.0.0` and `build-tools;36.1.0` are installed, and `licenses/android-sdk-license` exists [VERIFIED: probed `%LOCALAPPDATA%/Android/Sdk/platforms/*/package.xml` this session: `localPackage path="platforms;android-36"`, `localPackage path="platforms;android-36.1"`, `localPackage path="platforms;android-37.0"`, `localPackage path="build-tools;36.1.0"`]. CONTEXT.md line 84 says "platforms 31/33 only", which is out of date. D-13's intent (the SDK packages the build needs are present) is met already. The scaffold task only verifies them and runs no `sdkmanager --install`, so there is no license prompt to hand to the user.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| FND-01 | Developer can build and install the debug app on the test phone from the command line (Kotlin 2.4 / AGP 9.5 / Hilt / KSP / Room 3 / Navigation 3 toolchain builds green; fallbacks documented) | Gradle scaffold + KGP classpath bump (Pattern 1), gate procedure with ordered fallbacks (Pattern 2), wrapper bootstrap verified in a path with a space, `install-debug.sh` device detection (Pattern 8) |
| FND-02 | Every screen uses the Nocturne theme generated from `design-system/nocturne.tokens.json` (colours, type scale, shapes, glows) with edge-to-edge layout at targetSdk 36, minSdk 26 | Token-to-Kotlin mapping table (Pattern 3), M3 ColorScheme/Typography/Shapes mapping + `LocalNocturne` (Pattern 4), glyph-level Devanagari fallback via `Typeface.CustomFallbackBuilder` (Pattern 5), font sourcing/instancing/subsetting (Pattern 6), `enableEdgeToEdge(SystemBarStyle.dark(...))` (Pattern 7) |
| FND-04 | The phone can reach the local FastAPI backend's health endpoint over `adb reverse` (debug-only cleartext to 127.0.0.1; Python 3.13 pinned) | `adb reverse` over wireless TLS adb **verified end to end on this Redmi** (Environment), debug-source-set network security config (Pattern 9), uv project pinned to 3.13 + `/healthz` (Pattern 10) |
| FND-05 | Client and server share a versioned palm-reading contract (JSON schema + sample) with normalised 0–1 trace coordinates and a `trace_source` per line | Contract shape + invariants (Pattern 11), schema generated from Pydantic with a drift test, strict-decoder Kotlin test against the same files (Validation Architecture) |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

From `./.claude/CLAUDE.md` (GSD-generated) and the user's global rules:

- **Secrets:** the Anthropic API key lives only on the server (env var), never in the app. The repo is **public** (`.gitignore` says "this repo is public"), so `.env` must be git-ignored before the first backend commit.
- **Clean-room:** nothing from `_extract/` (the decompiled reference APK) may be copied. The old package `com.ai.palmreading...` in `tools/adb/relaunch.sh` is the reference app's id. New scripts must not reuse it.
- **Privacy/policy:** strip EXIF, the server deletes images, entertainment disclaimer. None of this is exercised in Phase 1, but the contract must leave room for it (the disclaimer is owned by the client, not the contract).
- **Stack (client):** Kotlin, Compose, Hilt, Room, DataStore, kotlinx.serialization, OkHttp. Navigation 3 replaces PRD "Navigation-Compose typed" per the STATE.md roadmap decision.
- **Workflow:** file changes go through GSD commands (`/gsd-execute-phase`).
- **Ponytail (global):** smallest complete change. No abstractions "for later". New non-trivial logic gets one small test.
- **ARTEMIS (global):** verify on-device behaviour with ARTEMIS / adb instead of assuming. If more than one device is connected, ask the user which serial to use. Use `adb devices -l` to diagnose.
- **Gemini key:** never print `GEMINI_API_KEY`. Not used in this phase.
- **Downloads:** the orchestrator says any download needs explicit user consent per session rules. That covers the font TTFs, fontTools, uv's managed CPython 3.13, and Gradle's first dependency resolution (AGP 9.5.3, Kotlin 2.4.21 and the AndroidX artifacts are **not** in `~/.gradle` yet).

## Summary

Phase 1 is mostly plumbing, but three details decide whether it goes smoothly.

1. **The toolchain gate is real.** AGP 9 has a runtime dependency on KGP 2.2.10 [CITED: developer.android.com/build/releases/agp-9-0-0-release-notes]. Declaring the Compose or serialization plugin at 2.4.21 does not lift the compiler. The documented way to raise it is a root `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") } }`. Without that line the Compose compiler plugin (2.4.21) and the Kotlin compiler (2.2.10) disagree and the build fails before any Hilt or Room question comes up.
2. **The known risk is Hilt's Kotlin-metadata reader.** dagger-compiler 2.60.1 depends on `org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.21` [VERIFIED: Maven Central dagger-compiler-2.60.1.pom]. If it fails to read Kotlin 2.4 metadata, the cheapest fix is to put `kotlin-metadata-jvm:2.4.21` (exists [VERIFIED: Maven Central metadata]) on the `ksp` classpath. That comes before the documented fallback (Kotlin 2.3.21 / Room 2.8.5 / Navigation-Compose 2.10.2).
3. **D-06's wording "Compose FontFamily fallback chains" does not do what D-06 intends.** A Compose `FontFamily(Font(...), Font(...))` list chooses fonts by weight and style and covers load failures. It does not fall back glyph by glyph. The only API with true glyph fallback is Android's `Typeface.CustomFallbackBuilder` (API 29+) [CITED: developer.android.com/reference/android/graphics/Typeface.CustomFallbackBuilder], wrapped with Compose's `FontFamily(android.graphics.Typeface)` [CITED: Compose `androidx.compose.ui.text.font` package summary]. On API 26-28 the platform's system Devanagari fallback still prevents tofu. The Redmi is API 30 [VERIFIED: `adb shell getprop ro.build.version.sdk` → `30`], so the bundled Noto path is what the phone exercises.

The networking path is already proven. `adb reverse` works over this phone's **wireless TLS** adb connection, end to end, to a server bound to PC `127.0.0.1` (output pasted under Environment Availability). The SDK is already complete (see the correction above). The Gradle 9.6.0 distribution is already in `~/.gradle/wrapper/dists`, and `gradle wrapper` works in a directory with a space once a stub `settings.gradle.kts` exists. Without that stub it fails (verified, see Pitfall 2). The contract should be authored as Pydantic models and exported to `contracts/palm_reading.v1.schema.json`, with a pytest drift check. Kotlin DTOs are tested with a **strict** `Json` decoder against the same sample files, so any field drift fails a test on both sides.

**Primary recommendation:** make task 1 the toolchain gate. Scaffold the exact Gradle set with real Hilt, Room 3 and Nav3 code (one `@HiltAndroidApp`, one `@HiltViewModel`, one `@Database`, one `NavKey`) and run `assembleDebug`. Walk the ordered fallback ladder in Pattern 2 if it fails, and record which set won in STATE.md before any theme or contract work.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Build/toolchain (Gradle, KSP, Hilt, Room 3, Nav3) | Build system (`android/`) | — | Compile-time only. Proves the stack before features exist. |
| Design tokens → Kotlin | Dev tooling (`tools/gen_tokens.py`) | Client (`core/designsystem`) | Generation runs on the PC. Its output is committed Kotlin source. The app never parses JSON tokens at runtime. |
| Theme, fonts, edge-to-edge, showcase | Client (Compose UI) | — | Pure presentation. |
| Backend health probe + status chip | Client (ViewModel + OkHttp) | API (`/healthz`) | The phone is the diagnostic surface (D-02). The server only reports its own state. |
| Phone → PC transport | Dev tooling (`adb reverse`, scripts) | — | Not product code. Debug-only cleartext config confines it. |
| Secret presence check | API/Backend (pydantic-settings) | — | The key never leaves the server. `/healthz` reports only `configured\|missing`. |
| Reading contract (shape + invariants) | API/Backend (Pydantic = authoring source) | Client (DTOs) + `contracts/` files | The server produces readings, so its validator is the authority. The committed schema and sample files are the shared artifact both sides test against. |

## Standard Stack

Versions are the STACK.md pins, restricted to what Phase 1 actually needs. Each was re-checked to exist in its registry this session.

### Core (Android)
| Library / Tool | Version | Purpose | Why Standard |
|---|---|---|---|
| Android Gradle Plugin | 9.5.3 | Build, built-in Kotlin | Pinned (9.6.0 is alpha) [VERIFIED: Google Maven metadata, `9.5.3` present, latest `9.6.0-alpha06`] |
| Gradle wrapper | 9.6.0 | Build | Already cached at `~/.gradle/wrapper/dists/gradle-9.6.0-bin` [VERIFIED: probe] |
| Kotlin (KGP + compose + serialization plugins) | 2.4.21 | Language, Compose compiler | [VERIFIED: Maven Central `kotlin-gradle-plugin` and `compose-compiler-gradle-plugin` list 2.4.21] |
| KSP | 2.3.14 | Hilt + Room processing | [VERIFIED: Maven Central, latest = 2.3.14] |
| Hilt / Dagger | 2.60.1 (+ `androidx.hilt` 1.4.0) | DI | 2.59 added AGP 9 support for the Hilt Gradle plugin, which requires Gradle 9.1+ [CITED: github.com/google/dagger/releases] [VERIFIED: hilt-android, hilt-compiler, hilt-android-gradle-plugin 2.60.1 present] |
| Room 3 | 3.0.3 + `androidx.sqlite:sqlite-bundled` 2.7.1 | DB (gate proof now, readings in Phase 5) | room3-runtime 3.0.3 depends on sqlite 2.7.1 [VERIFIED: room3-runtime-android-3.0.3.pom]. Plugin `androidx.room3` [VERIFIED: plugin marker 3.0.3 present] |
| Navigation 3 | 1.2.2 + `lifecycle-viewmodel-navigation3` 2.11.1 | Nav skeleton (one key) | [VERIFIED: Google Maven metadata] |
| Compose BOM | 2026.11.01 (ui 1.13, material3 1.5.0) | UI | [VERIFIED: Google Maven metadata] |
| activity-compose | 1.13.0 | `setContent`, `enableEdgeToEdge` | [VERIFIED: present] |
| lifecycle-runtime-compose / viewmodel-compose | 2.11.1 | `collectAsStateWithLifecycle` | [VERIFIED: present] |
| hilt-lifecycle-viewmodel-compose | 1.4.0 | `hiltViewModel()` inside Nav3 entries | [VERIFIED: present; POM depends on hilt-android 2.59, lifecycle-viewmodel-compose 2.11.0] |
| kotlinx-serialization-json | 1.13.0 | Contract DTOs + Nav3 keys | [VERIFIED: present; POM stdlib 2.2.20, so it also works if Kotlin falls back to 2.3.21] |
| OkHttp | 5.5.1 | `/healthz` probe (Phase 6 reuses it) | [VERIFIED: present] |
| core-ktx | 1.19.1 | Extensions | [VERIFIED: present] |
| JUnit | 4.13.2 | JVM unit tests (contract DTOs) | [VERIFIED: present] |

### Core (Backend)
| Library | Version | Purpose | Note |
|---|---|---|---|
| Python (uv-managed) | 3.13 (latest available 3.13.14) | Runtime | Not installed yet. `uv python list` shows `cpython-3.13.14 <download available>` [VERIFIED: probe]. |
| fastapi | 0.165.0 | `/healthz` | `requires_python >=3.11` [VERIFIED: PyPI JSON] |
| uvicorn[standard] | 0.54.0 | ASGI server | [VERIFIED: PyPI] |
| pydantic | 2.14.4 | Contract models (transitive via fastapi, pin explicitly) | [VERIFIED: PyPI] |
| pydantic-settings | 2.16.3 | `.env` → settings | [VERIFIED: PyPI] |
| pytest (dev) | 9.2.0 | Tests | [VERIFIED: PyPI] |
| httpx (dev) | 0.28.1 | `TestClient` dependency | [VERIFIED: PyPI] |

**Not in Phase 1** (ponytail): `anthropic`, `pillow`, `python-multipart`, `ruff`, DataStore, CameraX, MediaPipe, Coil, coroutines-test, MockK. Each arrives with the phase that uses it.

### Dev-time tool
| Tool | Version | Purpose | Note |
|---|---|---|---|
| fontTools (`fonttools` on PyPI) | **4.68.0** | Instance variable fonts to static weights, then subset | Pin 4.68.0 (2026-10-27). Not 4.69.1, which was published 2026-11-25, one day before this research [VERIFIED: PyPI release dates]. Run once via `uvx --from fonttools==4.68.0`, outside the backend venv. |

### Alternatives Considered (the documented fallback set)
| Instead of | Fallback | When |
|---|---|---|
| Kotlin 2.4.21 | Kotlin 2.3.21 (all three Kotlin pins + KGP classpath) [VERIFIED: present] | Kotlin-metadata or KSP errors that survive the `kotlin-metadata-jvm` override |
| Room 3.0.3 (`androidx.room3`) | Room 2.8.5 (`androidx.room`, plugin `androidx.room`, `room { schemaDirectory(...) }`) [VERIFIED: present] | Room 3 KSP/runtime failure. Never mix the two. |
| Navigation 3 1.2.2 | navigation-compose 2.10.2 + hilt-navigation-compose 1.4.0 [VERIFIED: both present] | Nav3 compile or runtime failure |

**Installation (Phase 1 only):**
```bash
# android: no install. Gradle resolves on first build (needs network + user consent)
cd "E:/Palm Reading/android" && ./gradlew --version

# backend
cd "E:/Palm Reading/backend"
uv python pin 3.13            # writes .python-version; uv downloads CPython 3.13.x on first sync
uv add "fastapi==0.165.0" "uvicorn[standard]==0.54.0" "pydantic==2.14.4" "pydantic-settings==2.16.3"
uv add --dev "pytest==9.2.0" "httpx==0.28.1"
```

## Package Legitimacy Audit

The seam was run with `gsd-tools query package-legitimacy check --ecosystem pypi ...`. Every PyPI package came back **SUS**. The only reasons given were `too-new` (the latest release is recent) and `unknown-downloads` (the seam gets no download counts from PyPI). No package was flagged for a missing repo, a postinstall script or deprecation. Each source repo is the canonical upstream org.

| Package | Registry | Latest published | Downloads | Source Repo | Verdict | Disposition |
|---|---|---|---|---|---|---|
| fastapi 0.165.0 | PyPI | 2026-11-18 | unknown (seam) | github.com/fastapi/fastapi | [SUS] too-new, unknown-downloads | Flagged. Keep (STACK pin). Batch into one human-verify checkpoint. |
| uvicorn 0.54.0 | PyPI | 2026-09-25 | unknown | github.com/Kludex/uvicorn | [SUS] unknown-downloads | Flagged (same checkpoint) |
| pydantic 2.14.4 | PyPI | 2026-11-19 | unknown | github.com/pydantic/pydantic | [SUS] too-new | Flagged (same checkpoint) |
| pydantic-settings 2.16.3 | PyPI | 2026-11-13 | unknown | github.com/pydantic/pydantic-settings | [SUS] too-new | Flagged (same checkpoint) |
| pytest 9.2.0 | PyPI | 2026-10-27 | unknown | github.com/pytest-dev/pytest | [SUS] too-new | Flagged (same checkpoint) |
| httpx 0.28.1 | PyPI | 2024-12-06 | unknown | github.com/encode/httpx | [SUS] unknown-downloads | Flagged (same checkpoint) |
| fonttools 4.68.0 | PyPI | 4.69.1 published 2026-11-25 | unknown | github.com/fonttools/fonttools | [SUS] too-new (the 4.69.x line) | Flagged. **Pin 4.68.0**, not the 1-day-old 4.69.1. |

Maven artifacts (Google Maven / Maven Central) were not run through the seam, which supports npm/pypi/crates. Their existence was confirmed from `maven-metadata.xml` on the official repositories, and the groupIds are the official `androidx.*`, `com.google.dagger`, `org.jetbrains.*`, `com.squareup.okhttp3`.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** all 7 PyPI packages above, from heuristics only. The planner should add **one** `checkpoint:human-verify` before the first `uv add` / `uvx` that lists all of them. It can be the same checkpoint that asks consent for downloads (fonts, CPython 3.13, Gradle deps).

## Architecture Patterns

### System Architecture Diagram

```
 design-system/nocturne.tokens.json ──► tools/gen_tokens.py ──► android/.../core/designsystem/NocturneTokens.kt (committed)
 google/fonts variable TTFs ──► fontTools instancer + subset ──► android/app/src/main/res/font/*.ttf (committed, + OFL.txt)
                                                                   │
 [Developer] ── tools/install-debug.sh ──► detect 1 device ──► ./gradlew :app:installDebug (ANDROID_SERIAL) ──► am start
                                                                   │
                                                                   ▼
 [Phone: Redmi API 30] MainActivity (enableEdgeToEdge dark) ──► NocturneTheme ──► Nav3 NavDisplay ──► Showcase screen
                                                                                                   │
                                                          BackendStatusViewModel (Hilt) ── OkHttp GET BuildConfig.BASE_URL/healthz
                                                                                                   │  http://127.0.0.1:8000 (debug cleartext only)
                                                                                                   ▼
 tools/dev-reverse.sh ──► adb reverse tcp:8000 tcp:8000 (over wireless TLS adb) ──► PC 127.0.0.1:8000
                                                                                                   │
 tools/run-backend.sh ──► uv run (Py 3.13) uvicorn app.main:app --host 127.0.0.1 ──► /healthz ──► Settings(.env) ──► {"status":"ok","anthropic_key":"configured|missing"}

 contracts/palm_reading.v1.schema.json  ◄── exported from ── backend/app/schemas/palm.py (Pydantic, authoring source)
 contracts/palm_reading.sample.json  ──► pytest (Pydantic parse, drift check)
 contracts/palm_reading.invalid_photo.sample.json ──► JVM unit test (strict kotlinx Json decode of DTOs)
```

### Recommended Project Structure
```
android/
  settings.gradle.kts  build.gradle.kts  gradle.properties  gradlew  gradlew.bat  gradle/{wrapper/,libs.versions.toml}
  local.properties                     # git-ignored; sdk.dir (ANDROID_HOME is NOT set on this PC)
  app/
    build.gradle.kts  schemas/         # Room exported schema (committed)
    src/main/AndroidManifest.xml
    src/main/java/app/palm/
      PalmApp.kt  MainActivity.kt
      core/designsystem/  NocturneTokens.kt (GENERATED)  NocturneTheme.kt  NocturneFonts.kt  StarBackground.kt  components/
      core/navigation/    Keys.kt  AppNavDisplay.kt
      core/network/       NetworkModule.kt  HealthClient.kt
      core/db/            AppDatabase.kt  ReadingEntity.kt  ReadingDao.kt  DbModule.kt
      contract/           PalmReadingDto.kt
      showcase/           ShowcaseScreen.kt  BackendStatusViewModel.kt
    src/main/res/{font/, values/strings.xml, values/themes.xml, mipmap-anydpi/ic_launcher.xml, drawable/}
    src/debug/AndroidManifest.xml  src/debug/res/xml/network_security_config.xml
    src/test/java/app/palm/contract/PalmReadingDtoTest.kt
backend/
  pyproject.toml  uv.lock  .python-version  .env.example  (.env git-ignored)
  app/{__init__.py, main.py, config.py, schemas/__init__.py, schemas/palm.py}
  scripts/export_schema.py
  tests/{test_health.py, test_contract.py}
contracts/  palm_reading.v1.schema.json  palm_reading.sample.json  palm_reading.invalid_photo.sample.json
tools/  gen_tokens.py  test_gen_tokens.py  fonts.sh  dev-reverse.sh  run-backend.sh  install-debug.sh  _device.sh  adb/ (existing)
```

`showcase/` lives in `src/main` for Phase 1. Moving it to `src/debug` is Phase 3 work (D-03).

### Pattern 1: Gradle scaffold for AGP 9 built-in Kotlin
**What:** a single `:app` module. The root `buildscript` raises KGP to 2.4.21. No `org.jetbrains.kotlin.android` plugin, no kapt.
**Key facts:**
- AGP 9.0 "has a runtime dependency on Kotlin Gradle plugin (KGP) 2.2.10". To go higher: `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:KGP_VERSION") } }` [CITED: agp-9-0-0-release-notes].
- With built-in Kotlin, "you don't need to set `kotlin.compilerOptions.jvmTarget` because its value defaults to `android.compileOptions.targetCompatibility`" [CITED: developer.android.com/build/migrate-to-built-in-kotlin].
- kapt is incompatible. Room 3 is KSP-only [CITED: Room 3 release notes].
- AGP 9 flipped `android.useAndroidx`, `android.builtInKotlin`, `android.newDsl`, `android.uniquePackageNames` to `true` and `android.defaults.buildfeatures.resvalues` to `false` [CITED: agp-9-0-0-release-notes]. Turn BuildConfig on explicitly (`buildFeatures { buildConfig = true }`).
- `android.sdk.defaultTargetSdkToCompileSdkIfUnset` is now `true` [CITED: same]. Always set `targetSdk = 36` explicitly, or the app silently targets 37.
(Full files in Code Examples.)

### Pattern 2: Toolchain gate with an ordered fallback ladder
Change **one** variable per step and record each outcome in STATE.md.
1. `./gradlew :app:assembleDebug` on the target set, with real annotated code: `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel` + `hiltViewModel()` inside a Nav3 `entry<>`, one Room 3 `@Database`/`@Entity`/`@Dao`, `@Serializable` NavKey and DTOs. An empty project proves nothing, because KSP processing is what breaks.
2. If Hilt/Dagger fails with a Kotlin metadata error (e.g. "Provided Metadata instance has version 2.4.0, while maximum supported version is 2.3.0"): add `ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")` and rebuild [ASSUMED: that the override resolves it; the dependency fact is VERIFIED from the dagger-compiler POM].
3. Still failing on Kotlin/KSP: set `kotlin = "2.3.21"` (KGP classpath + compose + serialization plugins together). KSP 2.3.14 stays.
4. Room 3 failing: switch to Room 2.8.5 (`androidx.room:room-runtime`, `ksp(room-compiler)`, plugin `androidx.room`, `room { schemaDirectory("$projectDir/schemas") }`, imports `androidx.room.*`) [ASSUMED: Room 2 DSL name `room {}`, as shown in the KMP doc snippet].
5. Nav3 failing: switch to `navigation-compose` 2.10.2 with `@Serializable` route objects and `hilt-navigation-compose` 1.4.0.
6. When green, run `installDebug` on the phone and confirm a launch (the success criterion also requires *install and launch*, not just a build).

### Pattern 3: DTCG tokens → `NocturneTokens.kt` (`tools/gen_tokens.py`, stdlib only)
Walk the JSON recursively. A node is a token when it has `$value`, and tokens sit at different depths: `typography.Caption` is a leaf at depth 2, while `typography.Display.XL` is at depth 3. Resolve `{a.b.c}` references.

| DTCG `$type` | Example in file (verbatim) | Kotlin output |
|---|---|---|
| color `#RRGGBB` | `"base": { "$value": "#0B0820", "$type": "color" }` [VERIFIED: nocturne.tokens.json:6-9] | `val BgBase = Color(0xFF0B0820)` |
| color `#RRGGBBAA` (CSS order) | `"color": "#6A4CF573",` [VERIFIED: nocturne.tokens.json:351] | `Color(0x736A4CF5)`. Alpha moves to the front, because Compose is ARGB. |
| dimension (px = dp per `$description`) | `"2": { "$value": "2px", "$type": "dimension" }` [VERIFIED: :136-139]; `"pill": { "$value": "999px",` [VERIFIED: :194-196] | `val Space2 = 2.dp` (digit-leading keys need a prefix); pill → map to `RoundedCornerShape(50)` in the theme, not 999.dp |
| typography | Overline: `"fontWeight": 700, "fontSize": "12px", "lineHeight": "16px", "letterSpacing": "0.12em"` [VERIFIED: :335-343]; `"fontFamily": "{font.family.display}",` [VERIFIED: :231] | `TextStyle(fontFamily = <role>, fontWeight = FontWeight(700), fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.12.em)` |
| shadow | `"Primary": ... "color": "#6A4CF573", "offsetX": "0px", "offsetY": "8px", "blur": "24px", "spread": "0px"` [VERIFIED: :348-356] | Emit a plain `NocturneShadow(color, offsetX, offsetY, blur, spread)` data class. The theme converts it to Compose `Shadow(radius = blur, spread, color, offset = DpOffset(...))`. Keep the generated file independent of the shadow API. |
| gradient | `"foil-border"`: stops `"#C9BCFFBF"` @0, `"#F2C66D8C"` @0.45, `"#A99BFF1F"` @1 [VERIFIED: :446-462] | `arrayOf(0f to Color(0xBFC9BCFF), 0.45f to ..., 1f to ...)` colour stops. Direction is not in the tokens; angles are fixed in Open Questions 3 (RESOLVED). |
| duration | `"instant": { "$value": "100ms", "$type": "duration" }` [VERIFIED: :495-497] | `const val Instant = 100` (ms) |
| cubicBezier | `"standard": { "$value": [0.2, 0, 0, 1], "$type": "cubicBezier" }` [VERIFIED: :599-606] | `val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)` |
| semantic refs | `"primary": { "$value": "{color.brand.primary}", "$type": "color" }` [VERIFIED: :533-535] | `val Primary = BrandPrimary` |

- Names: kebab-case to PascalCase (`card-hi` → `SurfaceCardHi`). Prefix digit-leading keys (`space.2` → `Space2`).
- The generated file starts with `// GENERATED by tools/gen_tokens.py from design-system/nocturne.tokens.json — do not edit`. Add a `--check` flag that exits 1 when the committed file differs from a fresh run.
- Font families: tokens carry only names (`"display": { "$value": "Cormorant Garamond"` / `"ui": { "$value": "Manrope"` [VERIFIED: :214-224]). Emit an enum or role key. The hand-written `NocturneFonts.kt` maps each role to resource fonts.
- **The 11 text styles** [VERIFIED: :226-345]: Display XL/L/M/S, Title M, Body L/M, Label L/M, Caption, Overline.

### Pattern 4: `NocturneTheme` = M3 dark scheme + `LocalNocturne` extras
- Build the `darkColorScheme(...)` from the `semantic` group: primary/onPrimary, secondary/onSecondary, tertiary = accent (gold)/onAccent, background/onBackground (foreground), surface = background, surfaceContainer = card, surfaceVariant = muted (card-hi), onSurfaceVariant = muted-foreground, outline = border, error = destructive/onDestructive, scrim = surface.scrim. Dark only, no dynamic colour.
- M3 `Typography`: displayLarge = Display XL, displayMedium = Display L, displaySmall = Display M, headlineSmall = Display S, titleMedium = Title M, bodyLarge = Body L, bodyMedium = Body M, labelLarge = Label L, labelMedium = Label M, bodySmall = Caption, labelSmall = Overline. All 11 are also exposed by token name in `LocalNocturne.current.type`.
- M3 `Shapes(extraSmall = xs 4, small = sm 8, medium = md 16, large = lg 24, extraLarge = xl 32)`.
- `staticCompositionLocalOf<NocturneExtras>` holds: palm.* overlay colours, status success/warning, bg.deep/raised/camera, focus ring, gradients (as brushes), glows (as NocturneShadow), durations, easings, `reduceMotion: Boolean`.
- `reduceMotion` = `Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f`. On this phone the setting is unset (`settings get global animator_duration_scale` → `null` [VERIFIED: probe]), so the default must be 1f.
- **D-07 (Devanagari line height):** when the active locale's script is Devanagari, derive styles with `lineHeight = TextUnit.Unspecified` (font-metric height, which never clips matras). Leave Latin styles at token heights. Prove it with the showcase Hindi lines on the phone. [ASSUMED: that token line heights clip Devanagari at e.g. Body M 14/20; the phone check decides.]

### Pattern 5: Glyph-level Devanagari fallback (D-06)
- Build one Android `Typeface` per (role, weight) with `Typeface.CustomFallbackBuilder(manropeFamily).addCustomFallback(notoSansDevaFamily).setSystemFallback("sans-serif").setStyle(FontStyle(weight, FontStyle.FONT_SLANT_UPRIGHT)).build()`. "The text renderer tries to see if it can render a character with the first font and if that font does not support the character, try next one" [CITED: Typeface.CustomFallbackBuilder reference, API 29].
- Wrap it with `androidx.compose.ui.text.font.FontFamily(typeface: android.graphics.Typeface)` [CITED: Compose font package summary] and set it as each TextStyle's `fontFamily`. Set `fontSynthesis = FontSynthesis.None` so Compose does not fake bold on top [ASSUMED].
- Pairs needed: ui400/500/600/700 (Manrope + Noto Sans Devanagari of the same weight); display600 (Cormorant Garamond SemiBold + Noto Serif Devanagari SemiBold).
- API 26-28 fallback (`Build.VERSION.SDK_INT < 29`): plain `FontFamily(Font(R.font.manrope_semibold, FontWeight.SemiBold), ...)`. Devanagari then falls back to the platform system font. That is a different look, but no tofu [ASSUMED: system Devanagari present on API 26-28 stock/OEM builds].
- Build the typefaces once (an `object` with `lazy`, or in `PalmApp`), not per recomposition.

### Pattern 6: Font sourcing → static, subset TTFs (D-05)
- **google/fonts ships only variable fonts for all four families** [VERIFIED: GitHub API listing]: `CormorantGaramond[wght].ttf` (1,195,560 B), `Manrope[wght].ttf` (164,700 B), `NotoSansDevanagari[wdth,wght].ttf` (641,944 B), `NotoSerifDevanagari[wdth,wght].ttf` (757,692 B), each with `OFL.txt`. Axes: Cormorant wght 300-700, Manrope wght 200-800, Noto wdth 62.5-100 and wght 100-900 [VERIFIED: METADATA.pb]. Noto Sans/Serif Devanagari subsets include `devanagari`, `latin` and `latin-ext` [VERIFIED: METADATA.pb].
- Pipeline (`tools/fonts.sh`, run once, outputs committed):
  1. Download the 4 TTFs + OFL.txt from a **pinned google/fonts commit SHA** (raw.githubusercontent.com/google/fonts/<sha>/ofl/...) into an empty scratch dir. **User consent checkpoint first.**
  2. Instance: `uvx --from fonttools==4.68.0 fonttools varLib.instancer -o manrope_semibold.ttf "Manrope[wght].ttf" wght=600` (Noto: `wght=600 wdth=100`).
  3. Subset: `uvx --from fonttools==4.68.0 pyftsubset in.ttf --output-file=out.ttf --unicodes=<ranges> --layout-features='*' --name-IDs='*'`. Latin + Latin-Ext for Cormorant/Manrope. For Noto: Devanagari `U+0900-097F,U+1CD0-1CF9,U+200C-200D,U+20B9,U+25CC,U+A830-A839,U+A8E0-A8FF` plus basic Latin punctuation/digits [ASSUMED: ranges per Google Fonts' devanagari subset definition].
  4. Name files `res/font/[a-z0-9_]+.ttf` (e.g. `manrope_semibold.ttf`, `noto_sans_devanagari_semibold.ttf`). Commit the OFL texts (e.g. `android/app/src/main/assets/licenses/` or `design-system/fonts/`).
- `--layout-features='*'` is mandatory for Devanagari. Dropping GSUB/GPOS breaks conjuncts and matra placement.

### Pattern 7: Edge-to-edge at targetSdk 36
- In `MainActivity.onCreate`: `enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT), navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))` before `setContent`. The default `enableEdgeToEdge()` "adapts to the system's light or dark theme" [CITED: developer.android.com/develop/ui/compose/system/setup-e2e]. On a phone in light mode that gives dark icons on the Nocturne background, which are invisible.
- `android:windowSoftInputMode="adjustResize"` on the activity [CITED: same].
- The window theme (`themes.xml`, parent `android:Theme.Material.NoActionBar`) sets `android:windowBackground` to `#0B0820` so cold start shows no white flash.
- Showcase: a full-bleed star background drawn behind the bars, with content in a `LazyColumn` using `contentPadding = WindowInsets.safeDrawing.asPaddingValues()`. Do not pad the background itself.

### Pattern 8: Dev scripts (bash, Git Bash on Windows)
- `tools/_device.sh` (sourced): if `ADB_SERIAL` is set, use it (same variable as the existing `tools/adb/*.sh`). Otherwise take the lines of `adb devices` whose second column is `device`. Exactly 1 → export `ADB_SERIAL` and `ANDROID_SERIAL`. 0 → "no device: pair wireless debugging / check `adb devices -l`". More than 1 → list them and exit 1. Per the user rules, ask which one rather than guessing.
- `install-debug.sh`: `cd "$ROOT/android" && ANDROID_SERIAL="$ADB_SERIAL" ./gradlew :app:installDebug`, then `adb -s "$ADB_SERIAL" shell am start -n app.palm.dev.debug/app.palm.MainActivity`. Create `local.properties` if missing (`sdk.dir=C:/Users/<user>/AppData/Local/Android/Sdk`, derived from `$LOCALAPPDATA`) [ASSUMED: AGP's installDebug honours `ANDROID_SERIAL`].
- `dev-reverse.sh`: `adb -s "$ADB_SERIAL" reverse --remove-all; adb -s "$ADB_SERIAL" reverse tcp:8000 tcp:8000; adb -s "$ADB_SERIAL" reverse --list`. Idempotent. Re-run after every adb reconnect (the reverse rule dies with the adb session).
- `run-backend.sh`: `uv run --directory "$ROOT/backend" uvicorn app.main:app --host 127.0.0.1 --port 8000` (no `--reload`).
- Set `ROOT="$(cd "$(dirname "$0")/.." && pwd)"` and **always quote** `"$ROOT"` (space in path). Prefix any `adb shell` argument that begins with `/` with `MSYS_NO_PATHCONV=1`. Git Bash otherwise rewrites `/sdcard/x` to `C:/Program Files/Git/sdcard/x` [ASSUMED: standard MSYS behaviour; the existing scripts avoid it by calling adb from Python].

### Pattern 9: Debug-only cleartext to 127.0.0.1
- For apps targeting API 28+, the default `base-config` is `cleartextTrafficPermitted="false"` [CITED: developer.android.com/privacy-and-security/security-config]. That default depends on **targetSdk**, so with targetSdk 36 it holds on every device the app runs on, including API 26-27. Release therefore needs nothing extra.
- `src/debug/res/xml/network_security_config.xml`: `<domain-config cleartextTrafficPermitted="true">` with `<domain includeSubdomains="false">127.0.0.1</domain>`, `localhost`, `10.0.2.2`. Numerical loopback IPs are valid domain entries (the doc's localhost section lists "a numerical IP address ... for example, `127.0.0.1`").
- `src/debug/AndroidManifest.xml`: `<application android:networkSecurityConfig="@xml/network_security_config"/>`. The manifest merger adds the attribute in debug only.
- `buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8000\"")` in debug. Release gets a placeholder (`"https://api.invalid"`) so no release code path points at loopback.
- Use `127.0.0.1`, never `localhost`. On Windows `localhost` can resolve to `::1` while uvicorn listens on IPv4 [CITED: project research ARCHITECTURE.md "Local Dev Networking"].

### Pattern 10: Backend skeleton
- `uv init --app backend`, then `uv python pin 3.13`, with `requires-python = ">=3.13,<3.14"` in `pyproject.toml`. Commit `uv.lock` and `.python-version`.
- `app/config.py`: `class Settings(BaseSettings)` with `model_config = SettingsConfigDict(env_file=BACKEND_DIR / ".env", extra="ignore")` and `anthropic_api_key: SecretStr | None = None`. Use an **absolute** env_file path (`Path(__file__).resolve().parents[1] / ".env"`). The scripts may be run from the repo root, and a relative `.env` would then silently be "missing".
- `/healthz` returns `{"status": "ok", "anthropic_key": "configured" | "missing", "schema_version": 1}`. An empty string counts as missing. Never echo the key or its length. No Claude call.
- `.env.example` is committed containing only `ANTHROPIC_API_KEY=`.
- Bind to `127.0.0.1` only. That avoids any Windows Firewall prompt and any LAN exposure, and adb reverse reaches loopback (verified).

### Pattern 11: Contract v1 (Pydantic is the author, `contracts/` holds the shared artifacts)
Fields (from ARCHITECTURE.md "Public contract PalmReading v1", lines 271-283, and PRD R-PALM-4):
- `schema_version: Literal[1]`, `reading_id: UUID`, `hand_side: "left"|"right"`, `meta {model, prompt_version, generated_at (RFC 3339), locale}`, `image_size {w>0, h>0}` (size sent to Claude), `verdict {ok, reason}`, `lines[]`, `palm {summary, traits[]}`, `love {how_you_love, ideal_partner, challenges}`, `career {strengths, challenges, ideal_industries[]}`.
- `lines[]`: `type` heart|head|life|fate|sun, `label`, `present`, `trace: list[[x,y]]` with each coordinate 0..1, `trace_source` llm|template|none, `interpretation`.
- Reason enum, verbatim from the in-repo source: `reason enum: blurry_or_dark | too_far | jewelry | bent | fingers_tight | incomplete | no_palm_lines` [VERIFIED: .planning/research/ARCHITECTURE.md:258]. The code is `jewelry` (US spelling), not REQUIREMENTS' prose "jewellery".
- `trace_source` values: `// llm | template | none` [VERIFIED: ARCHITECTURE.md:279].
- **Invariants (Pydantic `model_validator`, documented in the schema `description`):** `ok=true` ⇒ `reason=null` and palm/love/career present. `ok=false` ⇒ `reason` set, `lines=[]`, and palm/love/career `null`. `trace_source="none"` ⇔ `trace=[]`. `present=false` ⇒ `trace=[]`. No duplicate line `type`.
- `meta.locale` is a recommendation, not in ARCHITECTURE's sketch. It costs nothing now and would be a schema change later, because readings are cached per language (Claude's discretion under D-14).
- The public v1 schema may use `minimum`/`maximum`. The *LLM-facing* schema in Phase 2 may not (no min/max, no maxItems, no oneOf per ARCHITECTURE Pattern 6). Keep them separate models.
- Pydantic `model_config = ConfigDict(extra="forbid")` on contract models makes the sample test catch stray fields.
- Export: `scripts/export_schema.py` writes `{"$schema": "https://json-schema.org/draft/2020-12/schema", "$id": "palm_reading.v1", **PalmReading.model_json_schema()}` with `indent=2`. Pydantic emits 2020-12-style schemas (`prefixItems` for tuples) [ASSUMED].
- The sample exercises every variant: heart/head `llm`, life `template`, sun `template`, fate `present:false` + `none` + `[]`. The invalid sample is `ok:false` with `reason:"jewelry"`. Tests also loop all 7 reasons by mutating the invalid sample in memory.

### Anti-Patterns to Avoid
- **Applying `org.jetbrains.kotlin.android` or kapt.** It errors under built-in Kotlin [CITED: migrate-to-built-in-kotlin].
- **Setting `android.disallowKotlinSourceSets=false` or `android.builtInKotlin=false` to "get it building".** That hides the break the gate exists to find [CITED: STACK.md].
- **Hand-copying any token value into Kotlin.** Success criterion 2 forbids it. Even the M3 mapping must reference generated constants.
- **A gate build of an empty project.** Without annotated Hilt/Room classes, KSP never runs and the gate is meaningless.
- **Comparing schema files as text in the drift test.** `.gitattributes` only normalises `*.sh`, so a CRLF checkout breaks a text compare. Compare `json.load()` objects.
- **Using `usesCleartextTraffic="true"` in the main manifest.** It ships cleartext in release.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---|---|---|---|
| Glyph fallback Latin→Devanagari | Splitting strings by script into spans | `Typeface.CustomFallbackBuilder` + `FontFamily(Typeface)` | Platform shaping handles mixed runs, ZWJ/ZWNJ and conjuncts |
| Variable → static font weights | Shipping variable fonts and custom axis code | fontTools `varLib.instancer` | Correct named-instance output. D-05 asks for static. |
| Font subsetting | Dropping glyphs manually | `pyftsubset --layout-features='*'` | Keeps GSUB/GPOS needed for Devanagari |
| `.env` parsing | `os.environ` + a hand parser | pydantic-settings `SettingsConfigDict(env_file=...)` | Types, SecretStr redaction |
| JSON Schema | Hand-writing the schema | `PalmReading.model_json_schema()` + drift test | The schema cannot drift from the validator |
| Device shadows / glows | Blurred bitmaps or a custom RenderEffect | `Modifier.dropShadow(shape, Shadow(radius, spread, color, offset))` [CITED: compose shadows doc] | Compose 1.9+ API, in BOM 2026.11 (ui 1.13) |
| Phone→PC transport | LAN IP + firewall rules | `adb reverse tcp:8000 tcp:8000` | Verified over wireless TLS adb. No firewall, no IP drift. |
| Gradle wrapper | Downloading the distribution by hand | `gradle wrapper --gradle-version 9.6.0` (system Gradle 9.9.1 via scoop) | Verified working. Dist already cached. |

**Key insight:** every hand-rolled piece here (fallback, subsetting, schema) has a well-tested platform or tool path. Phase 1's own code should be glue only: the token generator, the theme mapping, the scripts, and the models.

## Runtime State Inventory

Not a rename/refactor/migration phase (greenfield). Omitted.

## Common Pitfalls

### Pitfall 1: Compose plugin 2.4.21 with compiler 2.2.10
**What goes wrong:** the build fails with a Compose-compiler / Kotlin version mismatch, or Kotlin 2.4 features are rejected.
**Why:** AGP 9's built-in Kotlin uses KGP 2.2.10 unless raised [CITED: agp-9-0-0-release-notes].
**How to avoid:** root `buildscript { dependencies { classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") } }`. Keep the compose and serialization plugin versions equal to it.
**Warning signs:** "Compose Compiler requires Kotlin version ..." or KGP 2.2.10 in `./gradlew buildEnvironment`.

### Pitfall 2: `gradle wrapper` refuses an empty directory
**What goes wrong:** `Directory '...\wrap test\android' does not contain a Gradle build.` [VERIFIED: ran Gradle 9.9.1 in a scratch dir with a space, this session].
**How to avoid:** write `settings.gradle.kts` containing only `rootProject.name = "palm"`, run `gradle wrapper --gradle-version 9.6.0 --distribution-type bin`, *then* write the real build files. If the AGP plugins are already declared, system Gradle 9.9.1 would configure them, which forces downloads and possibly incompatibility. Verified: after the stub, `./gradlew --version` printed "Welcome to Gradle 9.6.0!" from the path with a space.

### Pitfall 3: `ANDROID_HOME` is not set
**What goes wrong:** "SDK location not found".
**Why:** the env var is empty on this PC [VERIFIED: probe `ANDROID_HOME=` `ANDROID_SDK_ROOT=`].
**How to avoid:** `install-debug.sh` writes a git-ignored `android/local.properties` with `sdk.dir=` (forward slashes) when it is missing.

### Pitfall 4: targetSdk silently becomes 37
**Why:** AGP 9 defaults targetSdk to compileSdk when unset [CITED: agp-9-0-0-release-notes].
**How to avoid:** set `targetSdk = 36` explicitly and assert it with `aapt2 dump badging` or the merged manifest in verification.

### Pitfall 5: Light system bars on a dark-only app
**What goes wrong:** status bar icons are invisible when the phone is in light theme.
**How to avoid:** `SystemBarStyle.dark(Color.TRANSPARENT)` for both bars (Pattern 7).

### Pitfall 6: "FontFamily fallback" that does not fall back
**What goes wrong:** Hindi renders in the system font, or someone "fixes" it by putting Noto first, which breaks Latin.
**How to avoid:** Pattern 5. The showcase Hindi line per style is the proof (D-06).

### Pitfall 7: Subsetting strips Devanagari shaping
**What goes wrong:** broken conjuncts, matras detached from consonants.
**How to avoid:** `--layout-features='*'`. Eyeball "क्षत्रिय", "हृदय रेखा" and "ज्ञान" on the phone.

### Pitfall 8: Devanagari clipped by fixed token line heights
**How to avoid:** Pattern 4 / D-07. Use an unspecified line height for Devanagari-script locales.

### Pitfall 9: `.env` read relative to the working directory
**What goes wrong:** `/healthz` says `missing` although `backend/.env` exists, because the server was started from the repo root.
**How to avoid:** use an absolute `env_file` (Pattern 10), and cover it with a test.

### Pitfall 10: The adb reverse rule vanishes
**What goes wrong:** the chip shows Unreachable after the phone reconnects, sleeps long enough to drop wireless adb, or the adb server restarts.
**How to avoid:** `dev-reverse.sh` is idempotent. The status chip's Unreachable text names the likely cause ("connection refused → run tools/dev-reverse.sh / start backend"). Map exceptions: `ConnectException` → refused, `SocketTimeoutException` → timeout, `UnknownServiceException` containing "CLEARTEXT" → network config missing.

### Pitfall 11: Testing loopback with toybox `nc` closes too early
**What goes wrong:** `printf ... | nc 127.0.0.1 PORT` returns nothing, because toybox nc exits on stdin EOF before the response arrives [VERIFIED: first attempt in this session returned empty].
**How to avoid:** `(printf 'GET /healthz HTTP/1.0\r\n\r\n'; sleep 2) | nc 127.0.0.1 8000` [VERIFIED: returned `HTTP/1.0 200 OK`].

### Pitfall 12: Generated-token naming collisions and leading digits
`space.2` and `radius.xs` cannot become `2` or `xs` identifiers in one flat scope. Use prefixed objects (`NocturneSpace.S2` or `Space2`). Emit colours as `Color(0xAARRGGBB)` literals. Kotlin picks the `Color(Int)` overload for values up to `0x7FFFFFFF` and `Color(Long)` above it, and both read ARGB, so a single format works for every alpha.

### Pitfall 13: Shadow name collision
`androidx.compose.ui.graphics.Shadow` (the text shadow) and the drop-shadow `Shadow` type used by `Modifier.dropShadow` have the same simple name [ASSUMED: drop-shadow `Shadow` lives in `androidx.compose.ui.graphics.shadow`]. Generated code should use `NocturneShadow` and convert it in one hand-written place.

## Code Examples

### `android/settings.gradle.kts`
```kotlin
// Source: Room 3 / Nav3 docs require google() + mavenCentral(); Hilt plugin marker is on Maven Central
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "palm"
include(":app")
```

### `android/build.gradle.kts` (root)
```kotlin
// Source: developer.android.com/build/releases/agp-9-0-0-release-notes ("Upgrade to a higher KGP version")
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21") // keep == libs.versions.kotlin
    }
}
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room3) apply false
}
```
[ASSUMED: `libs` accessors are not used inside `buildscript {}` here; a literal string avoids catalog-in-buildscript edge cases. Keep it in sync by hand.]

### `android/gradle/libs.versions.toml` (Phase 1 subset of the STACK.md catalog)
```toml
[versions]
agp = "9.5.3"
kotlin = "2.4.21"
ksp = "2.3.14"
composeBom = "2026.11.01"
hilt = "2.60.1"
androidxHilt = "1.4.0"
room3 = "3.0.3"
sqlite = "2.7.1"
nav3 = "1.2.2"
lifecycle = "2.11.1"
activity = "1.13.0"
core = "1.19.1"
serialization = "1.13.0"
okhttp = "5.5.1"
junit = "4.13.2"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "core" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-foundation = { module = "androidx.compose.foundation:foundation" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-compiler", version.ref = "hilt" }
androidx-hilt-lifecycle-viewmodel-compose = { module = "androidx.hilt:hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-sqlite-bundled = { module = "androidx.sqlite:sqlite-bundled", version.ref = "sqlite" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
junit = { module = "junit:junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room3 = { id = "androidx.room3", version.ref = "room3" }
```

### `android/app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room3)
}

android {
    namespace = "app.palm"                       // D-08
    compileSdk { version = release(37) }         // STACK.md; platforms;android-37.0 is installed
    defaultConfig {
        applicationId = "app.palm.dev"           // D-08 (pre-release, reversible until first Play upload)
        minSdk = 26
        targetSdk = 36                           // explicit: AGP 9 would default it to compileSdk
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8000\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://api.invalid\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17   // Kotlin jvmTarget follows this under built-in Kotlin
    }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.all { it.systemProperty("contracts.dir", rootProject.file("../contracts").absolutePath) } }
}

room3 { schemaDirectory("$projectDir/schemas") }   // required with the plugin [CITED: Room 3 release notes]

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    // Gate fallback step 2 only, if Hilt fails to read Kotlin 2.4 metadata:
    // ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
}
```
[ASSUMED: `compileSdk { version = release(37) }` resolves the installed `platforms;android-37.0`. If AGP asks for `platforms;android-37`, try plain `compileSdk = 37` before anything else.]

### `android/gradle.properties`
```properties
org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8
kotlin.code.style=official
# AGP 9 defaults (useAndroidX, builtInKotlin, newDsl) are already true. Do not add opt-outs.
```

### Nav3 + Hilt ViewModel (verbatim API shape from the Nav3 docs)
```kotlin
// Source: developer.android.com/guide/navigation/navigation-3/save-state
@Serializable data object Showcase : NavKey

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
            entry<Showcase> { ShowcaseScreen(viewModel = hiltViewModel()) }
        },
    )
}
```
[ASSUMED: `hiltViewModel` import is `androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel` in androidx.hilt 1.4.0.]

### Room 3 minimal entity (gate proof; Phase 5 owns its final shape)
```kotlin
// Source: Room 3 release notes / KMP Room doc (suspend-only DAOs, SQLiteDriver required)
@Entity data class ReadingEntity(@PrimaryKey val id: String, val schemaVersion: Int, val json: String, val createdAt: Long)
@Dao interface ReadingDao { @Query("SELECT COUNT(*) FROM ReadingEntity") suspend fun count(): Int }
@Database(entities = [ReadingEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() { abstract fun readings(): ReadingDao }
// Hilt module: Room.databaseBuilder<AppDatabase>(context, context.getDatabasePath("palm.db").absolutePath)
//   .setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).build()
```
[ASSUMED: Android-only Room 3 does not need `@ConstructedBy`. The KMP doc requires it with an `expect object`, and the Android ProGuard rule `-keep class * extends androidx.room3.RoomDatabase { <init>(); }` implies reflective lookup on Android. If KSP demands it, follow the compiler message.]

### Edge-to-edge
```kotlin
// Source: developer.android.com/develop/ui/compose/system/setup-e2e (SystemBarStyle.dark: dark-only app)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { NocturneTheme { AppNavDisplay() } }
    }
}
```

### Devanagari glyph fallback (API 29+)
```kotlin
// Source: Typeface.CustomFallbackBuilder reference (API 29) + Compose FontFamily(android.graphics.Typeface)
@RequiresApi(29)
private fun fallbackTypeface(res: Resources, latin: Int, deva: Int, weight: Int): Typeface {
    fun fam(id: Int) = android.graphics.fonts.FontFamily.Builder(
        android.graphics.fonts.Font.Builder(res, id).setWeight(weight).build()).build()
    return Typeface.CustomFallbackBuilder(fam(latin))
        .addCustomFallback(fam(deva))
        .setSystemFallback("sans-serif")
        .setStyle(android.graphics.fonts.FontStyle(weight, android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT))
        .build()
}
// ui600 = androidx.compose.ui.text.font.FontFamily(fallbackTypeface(res, R.font.manrope_semibold, R.font.noto_sans_devanagari_semibold, 600))
```

### Debug network config
```xml
<!-- android/app/src/debug/res/xml/network_security_config.xml -->
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
<!-- android/app/src/debug/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:networkSecurityConfig="@xml/network_security_config" />
</manifest>
```

### Backend settings + healthz + test
```python
# backend/app/config.py
from pathlib import Path
from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

BACKEND_DIR = Path(__file__).resolve().parents[1]

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=BACKEND_DIR / ".env", extra="ignore")
    anthropic_api_key: SecretStr | None = None

# backend/app/main.py
from fastapi import FastAPI
from app.config import Settings

def create_app(settings: Settings | None = None) -> FastAPI:
    s = settings or Settings()
    app = FastAPI()

    @app.get("/healthz")
    def healthz() -> dict:
        key = s.anthropic_api_key.get_secret_value() if s.anthropic_api_key else ""
        return {"status": "ok", "anthropic_key": "configured" if key.strip() else "missing", "schema_version": 1}

    return app

app = create_app()

# backend/tests/test_health.py
from fastapi.testclient import TestClient
from app.config import Settings
from app.main import create_app

def test_missing_key():
    r = TestClient(create_app(Settings(_env_file=None, anthropic_api_key=None))).get("/healthz")
    assert r.json()["anthropic_key"] == "missing"

def test_configured_key_never_echoed():
    r = TestClient(create_app(Settings(_env_file=None, anthropic_api_key="sk-test-123"))).get("/healthz")
    assert r.json()["anthropic_key"] == "configured" and "sk-test-123" not in r.text
```
Note that `Settings(...)` also reads real OS env vars. Tests that need "missing" must pass the value explicitly, as above, rather than rely on absence.

### Kotlin DTO strict test (shape)
```kotlin
// android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt
private val strict = Json { ignoreUnknownKeys = false }          // drift detector; production uses ignoreUnknownKeys = true
private fun contract(name: String) = File(System.getProperty("contracts.dir"), name).readText()

class PalmReadingDtoTest {
    @Test fun sampleParsesUnchanged() {
        val r = strict.decodeFromString<PalmReadingDto>(contract("palm_reading.sample.json"))
        assertEquals(1, r.schemaVersion)
        assertTrue(r.lines.all { l -> l.trace.all { it.size == 2 && it.all { c -> c in 0.0..1.0 } } })
    }
    @Test fun invalidPhotoSampleParses() {
        val r = strict.decodeFromString<PalmReadingDto>(contract("palm_reading.invalid_photo.sample.json"))
        assertFalse(r.verdict.ok); assertNotNull(r.verdict.reason); assertTrue(r.lines.isEmpty())
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|---|---|---|---|
| `kotlin-android` plugin + `kotlinOptions {}` | AGP built-in Kotlin + `kotlin { compilerOptions {} }` | AGP 9.0 | No KGP plugin in module builds. KGP version raised via buildscript classpath. |
| kapt for Hilt/Room | KSP only | Room 3.0 (KSP-only), AGP 9 (kapt incompatible) | `ksp(...)` everywhere |
| `androidx.room` + SupportSQLite | `androidx.room3` + `SQLiteDriver` required, suspend DAOs | Room 3.0.0, 2026-07-01 | `setDriver(BundledSQLiteDriver())` |
| Nav2 `NavHost` graph | Nav3 owned `SnapshotStateList` back stack + `NavDisplay` | Nav3 1.x stable | Keys are `@Serializable NavKey` |
| `Modifier.shadow(elevation)` only | `Modifier.dropShadow` / `innerShadow` with `Shadow(radius, spread, color, offset)` | Compose 1.9 era | Token glows map directly |
| Edge-to-edge opt-in | Enforced at targetSdk 35+; opt-out removed at 36 | Android 15/16 | `enableEdgeToEdge` + insets from day one |

**Deprecated/outdated:** `androidx.compose.compiler` artifact (dead). `Typeface(context, fontFamily)` Compose helper (deprecated in favour of `FontFamily.Resolver`) [CITED: Compose font package summary].

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Forcing `kotlin-metadata-jvm:2.4.21` onto `ksp` fixes a Hilt metadata-version error | Pattern 2 | One extra fallback step. Falls through to Kotlin 2.3.21. |
| A2 | `compileSdk { version = release(37) }` resolves the installed `android-37.0` package | Code: app build | Build asks for an SDK package. Use `compileSdk = 37`, or let AGP auto-install (licenses already accepted). |
| A3 | Room 3 on Android-only needs no `@ConstructedBy` | Code: Room | KSP error tells you. Add the annotation per its message. |
| A4 | `hiltViewModel` lives at `androidx.hilt.lifecycle.viewmodel.compose` | Code: Nav3 | Import fix only |
| A5 | AGP `installDebug` honours `ANDROID_SERIAL` | Pattern 8 | Script installs on the wrong or all devices. Fallback: `adb -s "$S" install -r app-debug.apk`. |
| A6 | Token line heights clip Devanagari | Pattern 4 / D-07 | Only affects whether the unspecified-line-height branch is needed. Phone check decides. |
| A7 | `FontSynthesis.None` prevents fake bold on a pre-weighted Typeface | Pattern 5 | Slightly heavier glyphs. Visual check. |
| A8 | System Devanagari font exists on API 26-28 devices | Pattern 5 | Possible tofu on old OEM ROMs (not the test phone). Would need a Compose-only locale switch. |
| A9 | Google Fonts' Devanagari subset unicode ranges | Pattern 6 | Missing rare glyphs. Widen the ranges. |
| A10 | Compose shadow type is `androidx.compose.ui.graphics.shadow.Shadow`; Figma "blur" ≈ Compose `radius` | Pattern 3, Pitfall 13 | Import fix / glow looks 2x soft or hard. Visual check against Figma. |
| A11 | Pydantic `model_json_schema()` emits 2020-12-style schema | Pattern 11 | `$schema` URL mismatch only |
| A12 | MSYS rewrites leading-`/` adb arguments in Git Bash | Pattern 8 | Script paths break. `MSYS_NO_PATHCONV=1` is harmless either way. |
| A13 | Gradle builds from `E:/Palm Reading` (space) without AAPT2/KSP issues | Pitfall/Env | Only the wrapper was verified in a spaced path. The gate build is the real test. |

## Open Questions (RESOLVED)

1. **Downloads consent** — RESOLVED: the user approved all four download groups in chat on 2026-11-26: Gradle artifacts (AGP 9.5.3, Kotlin 2.4.21, AndroidX/Hilt/Room 3 from Google Maven and Maven Central), CPython 3.13.x via uv, the google/fonts files from GitHub, and fontTools from PyPI. No up-front checkpoint; 01-01 Task 1 replaces the human legitimacy checkpoint with an automated PyPI source-repo check against the Package Legitimacy Audit table, and 01-04 Task 2 pins fontTools 4.68.0 and one google/fonts commit.
   - Background: the Gradle cache has AGP up to 9.3.1, Kotlin up to 2.4.10 and Hilt 2.60.1 only [VERIFIED: `~/.gradle/caches/modules-2` listing]; the fonts are ~2.8 MB.
2. **Launcher icon from Figma (D-09)** — RESOLVED: the user approved a placeholder vector [Guide] orb adaptive icon (background `nocturne_bg_base`, gold orb foreground, monochrome layer) on 2026-11-26; the real Figma asset replaces it later. 01-05 Task 2 builds the three-layer adaptive icon so the swap is asset-only.
   - Background: the Figma MCP server needs authentication (`/mcp`) in this session, so the orb cannot be exported automatically.
3. **Gradient directions** — RESOLVED: decided now, in CSS `linear-gradient` degrees (0° points up, angles turn clockwise; the gradient line passes through the centre with length |w·sin a| + |h·cos a|, so the corners get the first and last stops):

   | Token | Angle | Direction (first stop -> last stop) |
   |---|---|---|
   | `gradient.primary` | 90° | left -> right |
   | `gradient.hero` | 135° | top-left -> bottom-right |
   | `gradient.sky` | 180° | top -> bottom |
   | `gradient.card` | 135° | top-left -> bottom-right |
   | `gradient.foil-border` | 135° | top-left -> bottom-right |
   | `gradient.hero-aurora` | 135° | top-left -> bottom-right |

   The tokens carry stops only, so these angles live in one hand-written place: `NocturneGradientAngles` in `core/designsystem/Effects.kt` (01-05 Task 1, with a JVM test of the angle-to-endpoint mapping). Visual QA against Figma "1 · Foundations" stays in the 01-05 phase-end human check; a mismatch is a one-constant change.
4. **Font weights actually needed** — RESOLVED: as confirmed. D-05/D-06 list ui 400/500/600/700 + display 600, and the tokens use exactly 400, 500, 600 and 700 for ui and 600 for display [VERIFIED: typography block]. No change needed.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|---|---|---|---|---|
| JDK | Gradle/AGP | ✓ | Temurin 17.0.17, JAVA_HOME set | — |
| System Gradle (to bootstrap the wrapper) | FND-01 | ✓ | 9.9.1 (scoop) | — |
| Gradle 9.6.0 distribution | FND-01 | ✓ cached | `~/.gradle/wrapper/dists/gradle-9.6.0-bin` | — |
| Android SDK platforms 36 / 36.1 / 37.0 | compile/target | ✓ | Pkg.Revision 2/1/2 | — |
| Build-tools 36.0.0 / 36.1.0 | AGP 9 (needs ≥36.0.0) | ✓ | — | — |
| SDK licenses | auto-install if needed | ✓ | `android-sdk-license` present | — |
| cmdline-tools sdkmanager | D-13 (only if a package is missing) | ✓ | 23.0 | — |
| `ANDROID_HOME` | Gradle SDK lookup | ✗ | unset | `local.properties sdk.dir` (script writes it) |
| adb / platform-tools | install, reverse | ✓ | 1.0.41 / 37.0.1 (37.0.3 available, optional) | — |
| Test phone | FND-01/02/04 | ✓ | `adb-QG55IJKZHQF6OF6L-aT7pYW._adb-tls-connect._tcp device product:cattail model:M2006C3MII`, API 30, 720x1600 @ 320 dpi | — |
| uv | backend | ✓ | 0.11.21 | — |
| Python 3.13 | FND-04 pin | ✗ (download available: cpython-3.13.14) | — | `uv python install 3.13` (needs consent). 3.12.13 is installed but does not satisfy the pin. |
| Gradle deps (AGP 9.5.3, Kotlin 2.4.21, AndroidX) | FND-01 | ✗ not cached | — | first build downloads (needs consent) |
| Network to Google Maven / Maven Central / PyPI / GitHub | all downloads | ✓ (metadata fetched this session) | — | — |
| Figma MCP | D-09 icon export | ✗ needs auth | — | user exports the asset manually |

**adb reverse over wireless TLS adb: positive end-to-end test (this session).** Python `http.server` bound to PC `127.0.0.1:18765`, `adb reverse tcp:18765 tcp:18765`, then from the phone:
```
$ adb -s adb-QG55IJKZHQF6OF6L-aT7pYW._adb-tls-connect._tcp shell '(printf "GET /healthz HTTP/1.0\r\n\r\n"; sleep 2) | nc 127.0.0.1 18765'
HTTP/1.0 200 OK
Server: SimpleHTTP/0.6 Python/3.14.0
...
{"status":"ok"}
```
The rule was removed afterwards (`adb reverse --remove tcp:18765`).

**Missing dependencies with no fallback:** none. Everything missing can be downloaded after consent.
**Missing dependencies with fallback:** `ANDROID_HOME` (local.properties), Figma (manual export / placeholder icon).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework (Android) | JUnit 4.13.2 JVM unit tests via AGP (`src/test`) |
| Framework (Backend) | pytest 9.2.0 + FastAPI `TestClient` (httpx 0.28.1) |
| Framework (Tooling) | Python stdlib `unittest` for `tools/test_gen_tokens.py` |
| Config file | none yet. Wave 0 creates `backend/pyproject.toml` `[tool.pytest.ini_options] testpaths = ["tests"]`. |
| Quick run command | `cd "E:/Palm Reading/backend" && uv run pytest -q` (~2 s) |
| Full suite command | `cd "E:/Palm Reading/backend" && uv run pytest -q && cd ../android && ./gradlew :app:testDebugUnitTest :app:assembleDebug && cd .. && python -I tools/gen_tokens.py --check && python -I -m unittest tools/test_gen_tokens.py` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| FND-01 | Target toolchain compiles with real Hilt/Room3/Nav3/KSP code | build | `cd android && ./gradlew :app:assembleDebug` (first run takes minutes; later ~30-60 s) | ❌ Wave 0 |
| FND-01 | Debug app installs and launches on the phone from one command | smoke (device) | `tools/install-debug.sh && adb -s "$ADB_SERIAL" shell pidof app.palm.dev.debug` | ❌ Wave 0 |
| FND-01 | Script fails clearly with 0 or 2+ devices | unit-ish (script) | `ADB_SERIAL= PATH=<stub adb> tools/install-debug.sh` exits 1 with a message; or a manual check with a second device absent | manual-only (needs a fake adb). Accept a manual check. |
| FND-02 | Generated tokens are up to date (no hand-copied values) | unit | `python -I tools/gen_tokens.py --check` | ❌ Wave 0 |
| FND-02 | Hex8 → ARGB, px → dp, em, ms, cubicBezier, refs resolved, 11 text styles emitted | unit | `python -I -m unittest tools/test_gen_tokens.py` | ❌ Wave 0 |
| FND-02 | targetSdk 36 / minSdk 26 in the built APK | build check | `"$LOCALAPPDATA/Android/Sdk/build-tools/36.1.0/aapt2" dump badging android/app/build/outputs/apk/debug/app-debug.apk \| grep -E "sdkVersion:'26'\|targetSdkVersion:'36'"` | ❌ Wave 0 |
| FND-02 | Showcase renders Nocturne, edge-to-edge, Hindi lines without tofu or clipping | on-device visual | `tools/adb/shot.sh showcase` + ARTEMIS `mobile_get_device_state` screenshot, compared with Figma "1 · Foundations" / "2 · Components" | manual-only (visual QA) |
| FND-02 | Stars static with animations off | on-device | `adb shell settings put global animator_duration_scale 0` → relaunch → two screenshots 2 s apart are identical → restore with `settings delete global animator_duration_scale` (the original value is unset/null) | manual/scripted |
| FND-04 | `/healthz` reports missing/configured without leaking the key | unit | `uv run pytest -q tests/test_health.py` | ❌ Wave 0 |
| FND-04 | `.env` found regardless of cwd | unit | `uv run pytest -q tests/test_health.py::test_env_file_absolute` | ❌ Wave 0 |
| FND-04 | Phone reaches `/healthz` over adb reverse; chip says Connected | on-device | `tools/run-backend.sh &` → `tools/dev-reverse.sh` → launch → `ADB_SERIAL=... python -I tools/adb/ui.py \| grep -i "connected"` | ❌ Wave 0 |
| FND-04 | Chip says Unreachable when the backend is stopped or the reverse removed | on-device | `adb reverse --remove-all` → tap Retry → `ui.py \| grep -i unreachable` | ❌ Wave 0 |
| FND-04 | Release build has no cleartext config | build check | `./gradlew :app:processReleaseMainManifest` then `! grep -r networkSecurityConfig android/app/build/intermediates/merged_manifest*/release` | ❌ Wave 0 |
| FND-04 | Python pinned to 3.13 | check | `cd backend && uv run python -c "import sys; assert sys.version_info[:2]==(3,13)"` | ❌ Wave 0 |
| FND-05 | Sample + invalid sample parse with Pydantic (`extra="forbid"`) | unit | `uv run pytest -q tests/test_contract.py` | ❌ Wave 0 |
| FND-05 | All 7 reasons accepted; invariants reject ok=false with lines, trace out of 0..1, source/trace mismatch | unit | `uv run pytest -q tests/test_contract.py -k "reason or invariant or range"` | ❌ Wave 0 |
| FND-05 | Committed schema equals the generated schema (drift) | unit | `uv run pytest -q tests/test_contract.py::test_schema_file_matches_models` | ❌ Wave 0 |
| FND-05 | Kotlin DTOs decode both samples with a strict Json | unit | `cd android && ./gradlew :app:testDebugUnitTest --tests "app.palm.contract.*"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `uv run pytest -q` (backend tasks), `python -I tools/gen_tokens.py --check` (theme tasks), `./gradlew :app:testDebugUnitTest` (Android tasks).
- **Per wave merge:** full suite command above.
- **Phase gate:** full suite green, plus the on-device checks (install+launch, Connected chip, Unreachable chip, showcase screenshot reviewed) before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] `backend/tests/test_health.py`, `backend/tests/test_contract.py`, pytest config in `backend/pyproject.toml`
- [ ] `tools/test_gen_tokens.py` (stdlib unittest)
- [ ] `android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt` + `testOptions` system property `contracts.dir`
- [ ] `contracts/palm_reading.sample.json`, `contracts/palm_reading.invalid_photo.sample.json`
- [ ] Framework install: `uv add --dev pytest==9.2.0 httpx==0.28.1` (after the consent checkpoint)

## Security Domain

`security_enforcement: true`, ASVS level 1.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---|---|---|
| V2 Authentication | no | No auth surface in Phase 1. The backend is loopback-only. |
| V3 Session Management | no | — |
| V4 Access Control | no (loopback bind is the control) | `--host 127.0.0.1` only. Never `0.0.0.0` in scripts. |
| V5 Input Validation | yes | Pydantic `extra="forbid"` + model validators on the contract. kotlinx strict decoding in tests. |
| V6 Cryptography | no | No crypto in Phase 1 (the TLS-to-Anthropic call starts in Phase 2) |
| V7 Error handling & logging | yes | `/healthz` never logs or returns key material. `SecretStr` repr is redacted. |
| V8 Data protection | yes | `.env` git-ignored before the first commit. Repo is public. |
| V9 Communications | yes | Cleartext only in the debug source set, only for loopback hosts. Release falls back to the targetSdk-36 default (cleartext off). |
| V14 Configuration | yes | No secrets in the APK or BuildConfig. Release `BASE_URL` is a placeholder. Debug manifest overlay only. |

### Known Threat Patterns for this stack
| Pattern | STRIDE | Standard Mitigation |
|---|---|---|
| Anthropic key committed to a public repo | Information disclosure | `.gitignore` gets `backend/.env` (and `.env`) in the first backend task. `.env.example` has an empty value. A pre-commit check: `git ls-files \| grep -E '(^\|/)\.env$'` must be empty. |
| `/healthz` leaks key or length | Information disclosure | Return only `configured\|missing`. Test asserts the key string is absent from the body. |
| Backend exposed on LAN/Wi-Fi | Spoofing / DoS (future cost abuse) | Bind 127.0.0.1. adb reverse needs no LAN exposure (verified). |
| Cleartext shipped in release | Tampering / information disclosure | Debug-only `networkSecurityConfig`. Merged-manifest check in verification. |
| Secrets baked into BuildConfig | Information disclosure | Only `BASE_URL` in BuildConfig. Never an API key (PROJECT constraint). |
| Supply chain (fresh PyPI releases) | Tampering | Exact pins + `uv.lock` committed. fonttools pinned to 4.68.0, not 4.69.1 (1 day old). Human-verify checkpoint for the SUS list. |
| Downloaded font files | Tampering | Download into an empty scratch dir from a pinned google/fonts commit. Process with `uvx` from outside that dir (the user rule on untrusted downloads). Commit only the processed TTFs + OFL. |
| `.gitignore` gaps | Information disclosure / repo bloat | Add `android/.gradle/`, `android/build/`, `android/app/build/`, `android/local.properties`, `android/.kotlin/`, `backend/.venv/`, `backend/.env`, `*.jks`/`*.keystore`, `__pycache__/` (already there) |

## Sources

### Primary (official docs fetched this session, tagged [CITED]; the seam rates the `webfetch` provider LOW)
- developer.android.com/build/migrate-to-built-in-kotlin: kotlin-android removal, kapt incompatibility, `compilerOptions`, jvmTarget default, source sets
- developer.android.com/build/releases/agp-9-0-0-release-notes: KGP 2.2.10 runtime dependency and buildscript upgrade snippet, property default flips, targetSdk default
- developer.android.com/jetpack/androidx/releases/room3: 3.0.3 stable, plugin `androidx.room3`, `room3 { schemaDirectory }`, SQLiteDriver required
- developer.android.com/kotlin/multiplatform/room: `BundledSQLiteDriver`, `setQueryCoroutineContext`, sqlite 2.7.1, ProGuard rule, `@ConstructedBy` (KMP)
- developer.android.com/guide/navigation/navigation-3/{get-started,basics,save-state}: artifacts, `NavKey`, `rememberNavBackStack`, `NavDisplay`, entry decorators
- developer.android.com/develop/ui/compose/graphics/draw/shadows: `dropShadow`/`Shadow(radius, spread, color, offset)`
- developer.android.com/reference/android/graphics/Typeface.CustomFallbackBuilder: API 29, glyph fallback semantics
- developer.android.com/reference/kotlin/androidx/compose/ui/text/font/package-summary: `FontFamily(android.graphics.Typeface)`
- developer.android.com/develop/ui/compose/system/setup-e2e: `enableEdgeToEdge`, `adjustResize`, contrast note
- developer.android.com/privacy-and-security/security-config: cleartext defaults by targetSdk, domain-config, loopback IPs
- github.com/google/dagger/releases: 2.59 AGP 9 support (Gradle 9.1+), 2.60 Kotlin 2.3.21

### Primary (tool-verified this session, tagged [VERIFIED])
- Google Maven / Maven Central `maven-metadata.xml` and POMs for every pin above; dagger-compiler 2.60.1 → kotlin-metadata-jvm 2.3.21; room3-runtime-android 3.0.3 → sqlite 2.7.1
- PyPI JSON API (versions, requires_python, release dates); gsd-tools package-legitimacy seam
- GitHub API: google/fonts `ofl/{cormorantgaramond,manrope,notosansdevanagari,notoserifdevanagari}` listings + METADATA.pb
- Local probes: SDK `package.xml`, JDK, Gradle, uv, Python list, `~/.gradle` cache, `adb devices -l`, device props, adb reverse end-to-end, `gradle wrapper` in a spaced path

### Secondary
- Qiita (Daiji256), "Compose per-character font fallback": confirms FontFamily lists do not fall back per glyph; CustomFallbackBuilder + AndroidFont approach
- Project research: `.planning/research/{STACK,ARCHITECTURE,PITFALLS,SUMMARY}.md`

### Tertiary
- kotlinlang Slack / forum threads on Compose glyph fallback (corroboration only)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH for existence and pins (registry-verified). MEDIUM for the combination, which is unproven until the gate build.
- Architecture/patterns: MEDIUM-HIGH. Official docs for every API. A few import paths and DSL forms are [ASSUMED] and logged.
- Environment: HIGH. Probed directly, including an end-to-end adb reverse test on the real phone.
- Pitfalls: HIGH for the probed ones (wrapper, nc, ANDROID_HOME, SDK state). MEDIUM for the font and line-height ones, which the showcase verifies on the phone.

**Research date:** 2026-11-26
**Valid until:** ~2026-12-10 for versions (AGP 9.6 and Room 3.1 are in alpha; fastapi releases weekly). ~30 days for the patterns.
