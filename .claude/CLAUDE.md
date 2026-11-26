<!-- GSD:project-start source:PROJECT.md -->

## Project

**[AppName] — AI Palm Reading & Astrology (Android)**

A clean-room Android app for mass-market users in India, South-East Asia and Latin America who enjoy palmistry and astrology. The hook is "point your camera at your palm and AI reads your love, career and life lines", surrounded by cheap-to-serve daily modules (tarot, horoscope, Vedic chart, moon sign, soul energy, love test, daily report) and a chat with a mystic persona ("[Guide]"). It is a rebuild of the features and flows of a reference app (studied in `replica/PRD.md`) with our own brand, copy, art and backend.

**Core Value:** The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds. If everything else is rough, this must feel magical and work reliably.

### Constraints

- **Tech stack (client)**: Kotlin, Jetpack Compose, Navigation-Compose (typed), Hilt, Room, DataStore, CameraX, MediaPipe Tasks Hand Landmarker, OkHttp + SSE, kotlinx.serialization, Coil — PRD §9, proven by the reference app
- **Tech stack (server)**: Python FastAPI; Claude vision for the palm reading JSON (Haiku-class for chat later); runs locally in milestone 1, Cloud Run later
- **Platform**: minSdk to be confirmed in research (reference app uses 30); must run well on mid/low-end phones
- **Performance**: palm result p50 < 12 s, p95 < 30 s; cold start < 2 s to first frame (PRD §10)
- **Privacy**: strip EXIF/GPS before upload; server deletes palm images after processing; no biometric templates stored
- **Policy**: entertainment disclaimer on every reading; no medical/legal/financial directives; ad caps from PRD §7.3
- **Secrets**: Anthropic API key only on the server (env var), never in the app

<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->

## Technology Stack

## Headline recommendations

## Recommended Stack

### Core Technologies (Android client)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Android Gradle Plugin | **9.5.3** (stable; 9.6.0 is alpha) | Build | Supports API 37, JDK 17 minimum, built-in Kotlin. Needs Gradle >= 9.6.0 and build-tools 36.0.0 (both present or trivially available). |
| Gradle wrapper | **9.6.0** | Build | AGP 9.5's minimum and default. Gradle 9.9.2 is current, but 9.6.0 is the tested pairing; a newer Gradle has caused deprecation noise with AGP before. |
| Kotlin | **2.4.21** (declared via the `compose` and `serialization` plugin versions; 2.5.0 is only RC2) | Language | AGP 9.5 bundles KGP 2.2.10 as a floor; AGP lets you raise it by declaring a higher plugin version at the top level. Keep one Kotlin version for the Compose, serialization and KSP plugins. |
| KSP | **2.3.14** (`com.google.devtools.ksp`) | Annotation processing for Hilt + Room | KSP2 era versioning (independent of Kotlin). 2.3.10 added Kotlin 2.4 and AGP 9 built-in Kotlin fixes; 2.3.13 has a concurrency regression fixed in 2.3.14. AGP 9 guidance requires KSP >= 2.3.6. |
| Jetpack Compose BOM | **2026.11.01** (ui / foundation / runtime / animation 1.13.0, material3 1.5.0) | UI toolkit | Latest stable BOM. Use BOM-managed versions only; never pin individual Compose artifacts. |
| Compose compiler | Kotlin plugin `org.jetbrains.kotlin.plugin.compose` **2.4.21** | Compose compilation | Compose compiler ships with Kotlin now. The `androidx.compose.compiler` artifact (last 1.5.15, Aug 2024) is dead. |
| Navigation 3 | **`androidx.navigation3:navigation3-runtime` + `navigation3-ui` 1.2.2** | Typed navigation | **DECISION (deviates from PRD 9 "Navigation-Compose typed").** Nav3 is stable (1.2.2, Nov 18 2026) and is Google's current path for new Compose apps: back stack is a plain `SnapshotStateList` you own, which suits onboarding resets, scan to generating to result flows, and ad-gated screens. Its ViewModel decorator `lifecycle-viewmodel-navigation3` 2.11.1 is stable too. Conservative fallback with identical Hilt wiring: `androidx.navigation:navigation-compose` **2.10.2** with `@Serializable` route objects. |
| Hilt | **Dagger/Hilt 2.60.1** + `androidx.hilt` **1.4.0** | DI | AGP 9 guidance requires Hilt >= 2.59.2. Use `ksp(...)`, never kapt. `androidx.hilt:hilt-lifecycle-viewmodel-compose` for Nav3 (`hilt-navigation-compose` 1.4.0 if you pick Nav2). |
| Room | **Room 3: `androidx.room3:room3-runtime` / `room3-compiler` 3.0.3** | Local readings DB | **DECISION.** Room 3 went stable July 2026 (3.0.3 on Sep 9). Android-only app, so all its breaking changes (suspend-only DAOs, KSP only, `SQLiteDriver` required) are free for a greenfield project, and it is the line that gets new features. Fallback: Room 2.8.5 (`androidx.room`), same annotations, different package. Do not mix them. Plugin id `androidx.room3` with `room3 { schemaDirectory(...) }`; commit the schema JSON. Use `BundledSQLiteDriver` (androidx.sqlite) for consistent SQLite behaviour on old devices. |
| DataStore (Preferences) | **1.3.0** | Onboarding, name, gender, DOB, language, flags | Standard replacement for SharedPreferences. Preferences flavour is enough; no proto. |
| CameraX | **1.7.0** (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`) | Preview + analysis + capture | Stable since Nov 18 2026, minSdk 24 (verified in the AAR manifest). Compose-native `camera-compose` 1.7.0 is also stable, but `PreviewView` in PERFORMANCE mode (SurfaceView) is the lower-risk choice on low-end phones. |
| MediaPipe Tasks Vision | **`com.google.mediapipe:tasks-vision:1.2.3`** (Google Maven, not Maven Central) | Hand Landmarker | Latest (published 2026-11-11), minSdk 24, Apache-2.0. Pin it: Google's docs show `latest.release`, which makes builds non-reproducible. Model `hand_landmarker.task` (float16, 7,819,105 bytes) comes from `storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task`, also mirrored on Hugging Face `litert-community/MediaPipe-HandLandmarker-Full`. Bundle it in `assets/` so the scan works offline. |
| OkHttp | **5.5.1** (`okhttp`, `okhttp-sse`) | HTTP + SSE | One HTTP stack for upload, polling fallback, SSE (palm job events now, chat streaming later). `EventSource` takes any `Request`, so a POST chat stream works too. |
| kotlinx.serialization | **json 1.13.0** + Kotlin plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21 | JSON for API + typed routes | PRD choice; no reflection, R8-friendly. |
| kotlinx.coroutines | **1.11.0** (`-android`, `-test`) | Concurrency | Flow for camera analysis to UI state, SSE to Flow bridging. |
| Coil 3 | **3.6.3** (`coil-compose`; add `coil-network-okhttp` only when remote images appear) | Image display | M1 only shows local photos, so `coil-compose` alone is enough. Coil 3 is the maintained line. |

### Core Technologies (Backend)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| Python | **3.13 in the container; 3.14 locally is fine** (`requires-python = ">=3.12"`) | Runtime | Every pinned dependency supports >= 3.11/3.12. Use 3.13 for Cloud Run so no dependency depends on a 3.14 wheel. |
| FastAPI | **0.165.0** | API + SSE | Built-in SSE (`from fastapi.sse import EventSourceResponse, ServerSentEvent`) since 0.135.0: 15 s keep-alive pings, `Cache-Control: no-cache`, `X-Accel-Buffering: no` and Pydantic-typed events. Works for GET and POST. |
| Uvicorn | **0.54.0** (`uvicorn[standard]`) | ASGI server | Standard. One worker per Cloud Run instance. |
| Pydantic | **2.14.4** (+ `pydantic-settings` 2.16.3) | Response contract (R-PALM-4), Claude structured output, env config | The same `PalmReading` model drives FastAPI validation and `messages.parse(output_format=...)`. |
| Anthropic Python SDK | **2.14.0** (released 2026-11-24) | Claude vision + chat | Official SDK only; no raw HTTP, no OpenAI shims. |
| Pillow | **13.0.0** | Server-side re-encode, resize, quality gate | Re-encode to JPEG, cap long edge, blur check via `ImageFilter.Kernel` Laplacian + `ImageStat` variance. No numpy/OpenCV needed in M1. |
| python-multipart | **0.0.34** | Multipart upload parsing | Required by FastAPI `UploadFile`. |

### Supporting Libraries (Android)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.activity:activity-compose` | 1.13.0 | `setContent`, edge-to-edge, `PickVisualMedia` | Always. Use `ActivityResultContracts.PickVisualMedia` for gallery import (no storage permission). |
| `androidx.lifecycle:lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | 2.11.1 | `collectAsStateWithLifecycle`, ViewModel | Always. Do not use 2.12.0-alpha. |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | 2.11.1 | Nav3 entry-scoped ViewModels | Only if Nav3. |
| `androidx.core:core-ktx` | 1.19.1 | Extensions | Always. |
| `androidx.core:core-splashscreen` | 1.2.0 | S01 splash without a flash | Always; supports the "cold start < 2 s to first frame" budget. |
| `androidx.exifinterface:exifinterface` | 1.4.2 | Read orientation from gallery JPEGs below API 28 | minSdk 26 means API 26-27 need it. On 28+ `ImageDecoder` handles it. Re-encoding through `Bitmap.compress` is what actually strips EXIF/GPS. |
| `androidx.work:work-runtime-ktx` + `androidx.hilt:hilt-work` | 2.12.0 / 1.4.0 | Daily notifications, content refresh | **Not in milestone 1.** Add in the notifications milestone. |
| `androidx.profileinstaller:profileinstaller` | 1.4.1 | Baseline profile install | Add in the hardening phase for the cold-start target. |
| `androidx.benchmark:benchmark-macro-junit4` | 1.5.0 stable (1.6.0 is alpha) | Generate baseline profile | Hardening phase. |
| `androidx.compose.material3:material3` | via BOM (1.5.0) | Primitives only | Theme is the custom Nocturne token set; use M3 for scaffolding/semantics, not look. |
| Custom vector drawables | n/a | 54 duotone icons from Figma | Import as vectors. Skip `material-icons-extended`. |

### Supporting Libraries (Backend)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `httpx` | 0.28.1 | `TestClient`/async test client | Dev only. |
| `pytest` / `pytest-asyncio` | 9.2.0 / 1.5.0 | Tests | Dev. Mock the Anthropic client; never call the real API in CI. |
| `ruff` | 0.17.6 | Lint + format | Dev. |
| `structlog` | 26.1.0 | Structured logs | Optional; plain `logging` JSON is enough for M1. Never log image bytes or readings. |
| `redis` / Postgres driver | not pinned | Job store, quotas | **Later** (chat quotas, multi-instance Cloud Run). Not in M1. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| Gradle version catalog (`gradle/libs.versions.toml`) | Single source of versions | Snippet below. |
| `uv` (or pip + venv) | Python env + lockfile | Commit the lockfile. |
| JUnit 4.13.2, MockK 1.14.12, Turbine 1.2.1, `kotlinx-coroutines-test` 1.11.0, `mockwebserver3` 5.5.1 | Unit tests (hand-state classifier, ViewModels, SSE parsing) | The geometry classifier for guidance states must be pure Kotlin so it is unit-testable without a camera. |
| ARTEMIS MCP + physical phone | On-device verification | Per user rules: explore the real screens first; use it for scan-screen checks. |
| LeakCanary (debug only) | Camera/ImageProxy leak detection | Debug build only; pin at scaffold time (not verified here). |

## Version catalog (copy-ready)

# NO org.jetbrains.kotlin.android plugin: AGP 9 has built-in Kotlin.

## SDK levels: minSdk / targetSdk / compileSdk

| Setting | Value | Why | Confidence |
|---------|-------|-----|------------|
| `minSdk` | **26** (Android 8.0) | Hard floor from libraries is 24 (CameraX 1.7.0 and MediaPipe 1.2.3 AAR manifests both say 24; AndroidX default is 24 for ~99% of devices). 26 gives `java.time` natively (birth date/time, no core-library desugaring), notification channels, adaptive icons and background-limit semantics that match what WorkManager notifications will assume. The reference app's 30 is needlessly restrictive for tier-2/3 India/SEA where Android 8-10 phones are still common. | MEDIUM (a judgement call; check Play Console "Reach and devices" after first release, and drop to 24 only if > ~2% of target users are on API 24-25) |
| `targetSdk` | **36** (Android 16) | Play requires new apps and updates to target API 36 from 2026-08-31. API 37 is expected to be required around Aug 2027. Do not target 37 in M1: extra behaviour changes for no benefit. | HIGH |
| `compileSdk` | **37** | AGP 9.5 supports up to 37, and CameraX Compose artifacts require compileSdk 37 + AGP >= 9.2. `android-37.0` is installed locally. | HIGH |

## CameraX setup (Preview + ImageAnalysis + ImageCapture)

- **Controller or provider:** use `LifecycleCameraController` (camera-view) with `PreviewView` (`ImplementationMode.PERFORMANCE`) hosted in `AndroidView`. Enable `IMAGE_ANALYSIS or IMAGE_CAPTURE`. Controller binds Preview, handles rotation and gives you `setImageAnalysisAnalyzer(executor, analyzer)`. Drop to `ProcessCameraProvider` + `UseCaseGroup` only if you need something the controller lacks.
- **Same viewport for all use cases:** set a 4:3 `ResolutionSelector` (`AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY`) on all three so the on-screen guide box, analysis frame and captured photo share one field of view. Mismatched aspect ratios are the main cause of overlay drift.
- **ImageAnalysis:** `setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)` (no YUV conversion code; MediaPipe takes the bitmap directly), `STRATEGY_KEEP_ONLY_LATEST`, target about 640x480 (`ResolutionStrategy` with `FALLBACK_RULE_CLOSEST_LOWER`), dedicated single-thread executor. Close every `ImageProxy` in `finally`. Rotate the bitmap by `imageInfo.rotationDegrees` before `BitmapImageBuilder(...).build()`. Throttle to about 10-15 fps on low-end devices.
- **ImageCapture:** `CAPTURE_MODE_MINIMIZE_LATENCY`, JPEG, resolution selector aiming for >= 1080 short side (PRD R-PALM-2) with `FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER`; flash mode from the S08 toggle; capture to memory or app cache, decode with `inSampleSize`, bake rotation, then re-encode JPEG q85 with `Bitmap.compress` (this drops EXIF/GPS) before upload.
- **Permission/state handling:** request `CAMERA` only on S08; model denied / permanently denied / no back camera as explicit UI states (already in milestone scope).

## MediaPipe Hand Landmarker setup and guidance-state logic

- `HandLandmarker.createFromOptions` with `RunningMode.LIVE_STREAM`, `setNumHands(1)`, min detection/presence/tracking confidence 0.5 (defaults), `BaseOptions.setDelegate(Delegate.CPU)` (the GPU delegate is less predictable on low-end SoCs), `setModelAssetPath("hand_landmarker.task")`, `setResultListener`/`setErrorListener`. Feed frames with `detectAsync(mpImage, SystemClock.uptimeMillis())`; frames arriving while it is busy are dropped (acceptable). Create it off the main thread; `close()` it in `onDispose`.
- **Result fields:** `landmarks` (21 normalized points, x/y in [0,1], z relative to wrist), `worldLandmarks` (metres), `handedness` (label + score). Build all guidance states from these in a pure-Kotlin `HandGuidance` function (unit-testable):
- **Why not `gesture_recognizer.task`** (what the reference bundles, 8.4 MB): its Open_Palm/Closed_Fist classes do not distinguish palm from back of hand, so you would still need the geometry; it only adds a second model. Use it later only if fist detection proves unreliable.
- Gotcha: the Android guide never shows CameraX integration. The bridge is the RGBA analyzer above.

## Anthropic (server) usage

# palm_service.py  (shape only; verified against the claude-api skill, SDK 2.x)

- **Model IDs (cached 2026-11-03 in the claude-api skill):** `claude-opus-5-5` ($4/$20 per MTok), `claude-sonnet-5-5` ($2/$10), `claude-haiku-5-5` ($0.10/$0.50 up to 100K prompt). IDs are complete as written; never append date suffixes. Vision is supported on all three.
- **DECISION, palm model tier (MEDIUM):** default `claude-sonnet-5-5`; run a half-day bake-off against `claude-haiku-5-5` and `claude-opus-5-5` on 20-30 real palm photos, scoring p50/p95 latency (target p50 < 12 s, p95 < 30 s), polyline plausibility and cost. A long polyline JSON is output-token heavy, so latency, not input cost, is the risk. Haiku 5.5 is the fallback if Sonnet misses the p50. Opus 5.5 is a quality ceiling check only. Keep the model in an env var.
- **No sampling params.** `temperature`/`top_p`/`top_k` are rejected on the 5.x models (non-default values) and on every model once API version 2026-10-21 is used. Consequence for R-PALM-7 (deterministic per image): you cannot get it from `temperature=0`. Implement it as a server-side cache keyed by SHA-256 of the normalized image + hand + locale (24 h TTL).
- **No forced `tool_choice`** on Opus 5.5/Sonnet 5.5 (400). Structured output via `output_format`/`output_config.format` is the right mechanism anyway; do not build this on tool-calling.
- **Thinking:** do not pass `budget_tokens` or `{"type": "disabled"}` (400 on these models). Control cost/latency with `output_config.effort` (`low` for palm and chat; Haiku 5.5 defaults to `medium`, so set it).
- **Image sizing:** models >= 4.7 accept up to 2576 px long edge and 4,784 visual tokens, and coordinates the model returns map 1:1 to pixels of the image it saw. Downscale server-side to about 1200-1600 px long edge (about 1,500-2,500 visual tokens) before sending, and return `image_size` as the size **sent to Claude**; the Android client rescales polylines to its photo. 10 MB per image limit (base64) on the first-party API.
- **Refusals:** always check `stop_reason` (`refusal` is HTTP 200). The claude-api skill recommends enabling the server-side fallback (`betas=["server-side-fallback-2026-07-01"]`, `fallbacks="default"` on the beta messages endpoint) for 5.x models; for palm photos refusals should be rare, so treat it as an optional hardening item, but code the refusal branch from day one.
- **Prompt caching:** keep the system prompt and schema byte-stable and put image + per-request text after them; top-level `cache_control` is fine once the system prompt exceeds the model's minimum cacheable length. Check `usage.cache_read_input_tokens`.
- **Chat later:** `claude-haiku-5-5` via `client.messages.stream(...)`, relayed through `EventSourceResponse` as `delta`/`done`/`error` events (PRD 5.2).

## FastAPI async job pattern (milestone 1)

- `POST /v1/palm/jobs` (multipart: `image`, `hand_side`, `locale`): validate type/size (<= 8 MB, JPEG/PNG/WebP, Pillow `verify()` + dimensions), re-encode, SHA-256 for the cache, create `job_id` (UUID4), start the task, return `{job_id}` immediately (202).
- **Job runner = plain asyncio, no queue library:** `asyncio.create_task` kept in a module-level `set` (strong reference, discard on done), an `asyncio.Semaphore` capping concurrent Claude calls, `asyncio.timeout(45)` per R-PALM-5, `asyncio.Queue` per job feeding the SSE generator, dict job store with 10-minute TTL sweep.
- `GET /v1/palm/jobs/{id}/events` (SSE): yields `progress` / `result` / `error` events with `id=` set; supports `Last-Event-ID`. Also expose `GET /v1/palm/jobs/{id}` as a **poll fallback** (PRD allows SSE or poll; mobile networks and some proxies break SSE).
- **Privacy:** hold image bytes in memory only; drop the reference as soon as the Claude call returns (R-PALM-6). Never write palm images to disk, never log them.
- **Cloud Run caveat (later milestone):** in-memory jobs only work on a single instance. Run `--max-instances=1` initially, or move job state to Redis/Postgres when deploying. Set `--timeout` generously for SSE and enable HTTP/2-friendly settings. Do not add Celery/RQ/Arq; that is far more machinery than a 12-30 s job needs.

## Installation

# Android: no install step; Gradle resolves from google() and mavenCentral().

# Required repos in settings.gradle.kts: google(), mavenCentral()  (tasks-vision is on Google Maven)

# Gradle wrapper

# Backend

# Model asset (commit to app/src/main/assets/, ~7.8 MB, Apache-2.0)

## Alternatives Considered

| Recommended | Alternative | When to Use Alternative |
|-------------|-------------|-------------------------|
| Navigation 3 1.2.2 | Navigation-Compose 2.10.2 (typed `@Serializable` routes) | If the team wants zero deviation from PRD 9, or finds a Nav3 gap (e.g. a specific shared-element transition). Same Hilt/ViewModel story. |
| Room 3.0.3 | Room 2.8.5 | If a needed integration only exists for Room 2 (Paging/LiveData are available as `room3-paging`/`room3-livedata`, so unlikely), or the team wants maximum community examples. |
| `LifecycleCameraController` + `PreviewView` | `camera-compose` `CameraXViewfinder` (1.7.0 stable) | If PreviewView sizing/overlay alignment proves painful in Compose; try only after the scan screen works. |
| HandLandmarker + geometry | GestureRecognizer (`gesture_recognizer.task`) | Only if fist/open-hand classification from geometry is unreliable. |
| OkHttp + `okhttp-sse` | Ktor client SSE | Only if the project goes Kotlin Multiplatform. Otherwise it is a second HTTP stack. |
| OkHttp direct + small `ApiClient` (3-6 endpoints) | Retrofit 3.0.0 + `converter-kotlinx-serialization` | If the REST surface grows past about 8 endpoints (horoscope, reports, config); easy to add later, not needed for M1's two calls. |
| FastAPI built-in SSE | `sse-starlette` 3.7.1 | If you need its explicit client-disconnect handling/`ping` tuning that the built-in lacks; verify disconnect behaviour of the built-in in a test first. |
| In-memory asyncio jobs | Redis/Arq/Cloud Tasks | When going multi-instance on Cloud Run or when job durability matters. |
| Pillow Laplacian blur check | OpenCV (`opencv-python-headless` 5.1.0.95) | If the later server-side palm segmentation model needs it; it is a large dependency for a single variance calculation. |
| `claude-sonnet-5-5` palm default | `claude-haiku-5-5` / `claude-opus-5-5` | Haiku if latency misses target; Opus only as a quality benchmark or a "premium reading" tier. |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| `org.jetbrains.kotlin.android` plugin | Incompatible with AGP 9 built-in Kotlin (and with the new DSL). | Nothing; AGP compiles Kotlin. Keep only the `compose`, `serialization` plugins, KSP. |
| `android.disallowKotlinSourceSets=false` | Hides a build break; Google's own AGP 9 guidance says never to add it. | Fix the plugin that needs KotlinSourceSets (update KSP/Hilt). |
| kapt (`kotlin-kapt`) | Incompatible with built-in Kotlin. | KSP for Hilt and Room 3 (Room 3 is KSP-only). |
| Mixing `androidx.room` and `androidx.room3` | Two packages, two plugins; WorkManager 2.12 uses Room 2 internally, which is fine as it is a separate package, but your own code must pick one. | Room 3 only. |
| `com.google.mediapipe:tasks-vision:latest.release` | Unpinned; breaks reproducibility. | Pin `1.2.3`. |
| Legacy MediaPipe Solutions (`com.google.mediapipe:solution-core`, `hands`) | Deprecated, replaced by Tasks. | Tasks Vision Hand Landmarker. |
| TFLite/LiteRT hand model wired by hand, OpenCV Android, ML Kit hand APIs | Re-implements what the `.task` bundle gives you; ML Kit has no hand-landmark API. | MediaPipe Hand Landmarker. |
| MediaPipe GPU delegate as default | Less predictable on low-end SoCs; CPU is fast enough for 1 hand at 10-15 fps. | `Delegate.CPU`; make GPU an opt-in experiment. |
| CameraX default YUV analysis + manual conversion | Bug-prone, slower. | `OUTPUT_IMAGE_FORMAT_RGBA_8888`. |
| `Camera2Interop` V1 (`Extender`, `Camera2CameraControl`) | Deprecated in CameraX 1.7. | V2 factories if interop is ever needed. |
| `androidx.compose.compiler` artifact, Accompanist libs, LiveData, Gson/Moshi, Glide/Picasso, `material-icons-extended` | Dead, superseded, or heavy for no gain. | Compose compiler plugin, built-in APIs, kotlinx.serialization, Coil 3, custom vector icons. |
| String-route Navigation 2 (`composable("home/{id}")`) | Untyped. | Nav3 `NavKey`/`@Serializable` objects (or typed Nav2 if falling back). |
| `core-library-desugaring` | Unneeded at minSdk 26. | Native `java.time`. |
| `temperature`, `top_p`, `top_k`, `budget_tokens`, `thinking: {"type": "disabled"}`, forced `tool_choice`, assistant prefill | Return 400 on current models / API version. | `output_config.effort`, structured outputs, prompt wording, hash-keyed cache for determinism. |
| Raw `httpx`/`requests` to the Anthropic API, OpenAI-compat shims, LangChain | Official SDK is the supported path; extras add drift. | `anthropic` SDK. |
| Gradio (what the reference used), Celery/Redis queues for M1, WebSockets for job events | Wrong shape or too heavy. | FastAPI + asyncio job + SSE/poll. |
| Anthropic API key in the APK or any client config | Extractable; violates the project constraint. | Key only in server env; later Play Integrity + per-install rate limits. |
| Opus-tier model as the default palm/chat model | Cost and latency for a free, ad-funded app. | Sonnet 5.5 for palm (benchmark Haiku 5.5), Haiku 5.5 for chat. |
| x86/x86_64 ABIs in release | Inflates APK for no target users. | `armeabi-v7a`, `arm64-v8a`. |

## Stack Patterns by Variant

- Drop analysis resolution to 480x360, cap at 8-10 fps, and run landmarking only while the screen is resumed.
- Because MediaPipe drops busy frames in LIVE_STREAM, backpressure is already handled.
- Switch `PALM_MODEL` to `claude-haiku-5-5`, shrink the image to about 1,200 px long edge, shorten the schema (fewer polyline points, e.g. 8-12 per line), and emit `progress` events so the generating screen feels alive.
- Longer term: PRD phase "trained palm-line model" moves polylines out of the LLM and leaves the LLM only the prose.
- `--max-instances=1 --min-instances=1`, poll fallback enabled, no scale-to-zero during testing windows.
- Add core-library desugaring (`com.android.tools:desugar_jdk_libs`) for `java.time`; nothing else in the stack needs to change.

## Version Compatibility

| Package A | Compatible With | Notes |
|-----------|-----------------|-------|
| AGP 9.5.3 | Gradle >= 9.6.0, JDK 17, build-tools >= 36.0.0, API <= 37 | Built-in Kotlin on; KGP floor 2.2.10, raise by declaring plugins at 2.4.21. |
| Kotlin 2.4.21 | KSP 2.3.14 | KSP 2.3.10+ handles Kotlin 2.4 module names and AGP 9 built-in Kotlin. Min AGP for KSP 2.3.12+ is 8.12. |
| Hilt 2.60.1 / androidx.hilt 1.4.0 | AGP 9 (needs Hilt >= 2.59.2), KSP | `hilt-lifecycle-viewmodel-compose` 1.4.0 depends on hilt-android 2.59, resolved up to 2.60.1. **Risk:** Dagger's Kotlin-metadata reader vs Kotlin 2.4. Verify in the first scaffold build; if it fails, drop Kotlin to 2.3.21 (still supported by KSP 2.3.14 and Room 3.0.x). |
| Room 3.0.3 | KSP (required), Kotlin >= 2.2 | Libraries target Kotlin 2.2. Schema directory required with the Gradle plugin. |
| Compose BOM 2026.11.01 | Kotlin 2.4.x compose plugin, `camera-compose` 1.7.0 (compiled against Compose 1.11.2, resolves up) | CameraX Compose artifacts need compileSdk 37 and AGP >= 9.2. |
| CameraX 1.7.0 | minSdk 24, Java 11+ bytecode | Fine with JVM target 17. |
| MediaPipe tasks-vision 1.2.3 | minSdk 24; Guava + `androidx.annotation` transitive | Keep `.task` uncompressed in the APK. Hand Landmarker model is Apache-2.0 (PRD 11 licence note: include the notice). |
| OkHttp 5.5.1 | `okhttp-sse` same version; `mockwebserver3` 5.5.1 for tests | Keep all OkHttp artifacts on one version. |
| kotlinx-serialization-json 1.13.0 | Kotlin 2.4 serialization plugin | Plugin and runtime must stay in step. |
| Nav3 1.2.2 | `lifecycle-viewmodel-navigation3` 2.11.1 (stable), Hilt 1.4.0 | Do not use the 2.12.0-alpha lifecycle line; mixing with lifecycle 2.11.1 elsewhere keeps one version. |
| FastAPI 0.165.0 | Starlette 1.10.0 (transitive; do not pin), Pydantic 2.14.4, Python >= 3.11 | Let FastAPI resolve Starlette. |
| anthropic 2.14.0 | Python >= 3.10, `claude-sonnet-5-5` / `claude-haiku-5-5` / `claude-opus-5-5` | Use `output_config`, not the removed raw `output_format={...}` dict; passing a Pydantic class to `output_format=` on `parse()` is still the supported helper form. |

## Open risks to verify early (first Gradle/scan spike)

## Sources

- Android Jetpack release index (retrieved 2026-11-26, "last updated Nov 18 2026"): https://developer.android.com/jetpack/androidx/versions : HIGH (official)
- Google Maven metadata (`maven-metadata.xml`) for compose-bom, camera-*, room/room3, navigation, navigation3, lifecycle, hilt, core-splashscreen, exifinterface, mediapipe tasks-vision, AGP, profileinstaller; AAR manifest minSdk for tasks-vision 1.2.3 and camera-core 1.7.0; BOM 2026.11.01 POM : HIGH (primary artifacts)
- Maven Central metadata: kotlin-gradle-plugin, KSP plugin 2.3.14, hilt-android 2.60.1, okhttp / okhttp-sse 5.5.1, kotlinx-serialization-json 1.13.0, coroutines 1.11.0, coil 3.6.3, mockk, turbine, junit : HIGH
- AGP 9.5 release notes (compatibility table: Gradle 9.6.0, JDK 17, build tools 36.0.0, max API 37): https://developer.android.com/build/releases/gradle-plugin : HIGH
- AGP 9 migration guidance (KSP >= 2.3.6, Hilt >= 2.59.2, no kapt, never set `android.disallowKotlinSourceSets=false`): https://developer.android.com/build/migrate-to-built-in-kotlin and the AGP 9 upgrade skill : HIGH
- KSP releases (2.3.14, 2.3.10 Kotlin 2.4 fix, 2.3.12 min AGP 8.12, 2.3.13 regression): https://github.com/google/ksp/releases : HIGH
- Room 3.0 release notes: https://developer.android.com/jetpack/androidx/releases/room3 : HIGH
- CameraX 1.7.0 release notes: https://developer.android.com/jetpack/androidx/releases/camera : HIGH
- MediaPipe Hand Landmarker Android guide (options, result fields, `detectAsync`, mirrored-input note): https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker/android : HIGH; model file size verified via HTTP headers (7,819,105 bytes)
- Play target API requirement (API 36 from 2026-08-31): https://support.google.com/googleplay/android-developer/answer/11926878 and https://developer.android.com/google/play/requirements/target-sdk : HIGH
- FastAPI SSE docs (built-in since 0.135.0): https://fastapi.tiangolo.com/tutorial/server-sent-events/ : HIGH; disconnect behaviour not documented (flagged)
- PyPI JSON API for anthropic 2.14.0, fastapi 0.165.0, uvicorn 0.54.0, pydantic 2.14.4, pydantic-settings 2.16.3, pillow 13.0.0, python-multipart 0.0.34, sse-starlette 3.7.1, pytest 9.2.0, pytest-asyncio 1.5.0, httpx 0.28.1, ruff 0.17.6 : HIGH
- Claude API skill (bundled with Claude Code, models table cached 2026-11-03; sampling-parameter removal, forced `tool_choice` removal, `output_config.effort`, `messages.parse`, refusal fallbacks) and Anthropic Vision docs (resolution tiers, 10 MB limit, 1:1 coordinates on >= 4.7 models): https://platform.claude.com/docs/en/build-with-claude/vision : HIGH
- Android platform distribution dashboard did not return usable API-level shares; minSdk 26 rests on library floors (24), AndroidX's 99%-coverage default (24) and `java.time` availability : MEDIUM, verify with Play Console Reach and devices
- Not done: provider/cache seams from the tool strategy (`research-plan`, `research-store`, `classify-confidence`) were not run; findings come directly from primary sources and are tagged HIGH/MEDIUM by judgement.

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:
- `/gsd-fast` for a trivial task inline, with no subagents and no PLAN.md
- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
