# Walking Skeleton — [AppName] (AI Palm Reading & Astrology, Android)

**Phase:** 1
**Generated:** 2026-11-26

## Capability Proven End-to-End

> On the physical Redmi, the debug app built and installed by one command opens the Nocturne showcase screen and shows that the local FastAPI backend at `127.0.0.1:8000` answered `GET /healthz` over `adb reverse`; client and server parse the same versioned palm-reading sample.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework (client) | Kotlin 2.4.21, AGP 9.5.3 built-in Kotlin (no standalone Kotlin Android plugin), Gradle 9.6.0, Compose BOM 2026.09.00 (Compose 1.12.1, material3 1.4.0) with Material 3 as primitives only. Built green on the target set in plan 01-01 with no fallback-ladder step; the BOM is held below 2026.11.01 because Compose 1.13 needs compileSdk 37.1+ and only platforms;android-37.0 is installed (move up after installing android-37.2). Documented fallback (unused): Kotlin 2.3.21 / Room 2.8.5 / navigation-compose 2.10.2. | STACK.md pins; AGP 9 is the current line; the fallback ladder is pre-approved in ROADMAP |
| SDK levels | minSdk 26, targetSdk 36 (explicit), compileSdk 37 | Play requires 36 from 2026-08-31; 26 gives `java.time` and reaches Android 8-10 phones in the target markets |
| DI | Hilt 2.60.1 + androidx.hilt 1.4.0 via KSP 2.3.14 (no other annotation processor) | KSP-only under AGP 9 built-in Kotlin |
| Navigation | Navigation 3 1.2.2: `@Serializable` `NavKey`s, an owned back stack, `NavDisplay` with saveable-state and ViewModel-store entry decorators, `hiltViewModel()` inside entries | Roadmap decision; owned back stack suits onboarding resets and scan -> generating -> result |
| Data layer | Room 3.0.3 with `BundledSQLiteDriver`, suspend DAOs, schema exported to `android/app/schemas/`; a reading is one row holding the JSON document (`ReadingEntity`). DataStore (Preferences) arrives in Phase 3 | Room 3 is the maintained line; JSON-document rows avoid four tables for data that is always read whole |
| Networking | OkHttp 5.5.1; `BuildConfig.BASE_URL` = `http://127.0.0.1:8000` in debug (reached through `adb reverse tcp:8000 tcp:8000`), `https://api.invalid` in release; plain HTTP allowed only by the debug source set's network security config for loopback hosts | No LAN exposure, no firewall prompt, no IP drift; release never points at loopback |
| Backend | FastAPI 0.165.0 + uvicorn 0.54.0 on CPython 3.13 managed by uv (`requires-python >=3.13,<3.14`, `uv.lock` committed); `create_app()` factory; pydantic-settings reading an absolute `backend/.env`; bound to `127.0.0.1` only | Factory lets tests inject settings; absolute env path works from any working directory |
| Contract | Pydantic models in `backend/app/schemas/palm.py` are the authoring source; `contracts/palm_reading.v1.schema.json` is exported from them (drift-tested); `contracts/*.sample.json` are the shared fixtures; Kotlin DTOs in `app.palm.contract` decode them with a strict `Json` in tests. `schema_version: 1`; normalised 0-1 `[x,y]` traces; `trace_source` = `llm` / `template` / `none`; unusable photo = `verdict.ok=false` + one of 7 reason codes | Server produces readings, so its validator is the authority; both sides test the same bytes. Shape changes bump `schema_version` (D-14, costly to reverse) |
| Design system | `design-system/nocturne.tokens.json` -> `tools/gen_tokens.py` -> committed `NocturneTokens.kt` + `res/values/nocturne_colors.xml` (`--check` guards drift); `NocturneTheme` maps tokens onto an M3 dark scheme and exposes the rest through `LocalNocturne`; bundled static OFL fonts (Cormorant Garamond, Manrope, Noto Sans/Serif Devanagari) with glyph fallback through `Typeface.CustomFallbackBuilder` on API 29+ | No hand-copied token values; offline, predictable fonts; Hindi never renders as tofu |
| Auth | None in milestone 1. The Anthropic key lives only in `backend/.env` (git-ignored); `/healthz` reports `configured` / `missing` and never the key | PROJECT constraint: secrets only on the server |
| Deployment target | Local: phone over wireless adb + PC backend. Cloud Run is a later milestone (REL-04) | Milestone 1 is a local vertical slice |
| Directory layout | Monorepo: `android/` (single `:app` module, package-by-feature under `app.palm`: `core/designsystem`, `core/navigation`, `core/network`, `core/db`, `contract`, `showcase`), `backend/` (`app/`, `app/schemas/`, `scripts/`, `tests/`), `contracts/`, `tools/` (dev scripts beside the existing `tools/adb/`). `replica/`, `_extract/` and the xapk stay out of every build | D-10; one module until a second developer or slow builds justify more |

## Stack Touched in Phase 1

- [x] Project scaffold: Gradle wrapper, version catalog, `:app` with Hilt/KSP/Room 3/Nav3; uv backend with pytest; JVM unit tests and one instrumented test (plan 01-01; the first JVM unit and pytest tests land with the contract and health plans)
- [x] Routing: Navigation 3 `Showcase` key with a Hilt ViewModel entry (plan 01-01)
- [x] Database: Room 3 insert + read-by-id on the phone in `AppDatabaseTest` (plan 01-01 Task 2)
- [ ] UI wired to the API: backend status chip calling `GET /healthz`, with Retry (plans 01-01, 01-03)
- [x] Local full-stack run command (below) (plan 01-01)

## Local Full-Stack Run

| Step | Command (from the repo root, Git Bash) |
|---|---|
| Start the backend (Python 3.13, 127.0.0.1:8000) | `bash tools/run-backend.sh` |
| Forward the phone's 127.0.0.1:8000 to the PC (re-run after any adb reconnect) | `bash tools/dev-reverse.sh` |
| Build, install and launch the debug app on the single connected phone | `bash tools/install-debug.sh` |
| All of the above plus a UI assertion (used by verification) | `bash tools/smoke-phone.sh "Backend: Connected"` |
| Any Gradle task (writes `android/local.properties` if missing) | `bash tools/gradlew.sh <task>` |

## Out of Scope (Deferred to Later Slices)

- Onboarding, Home, hand selection, DataStore profile (Phase 3)
- Full 6-layer animated background with parallax (Phase 3, FND-03); Phase 1 ships the reusable `NocturneBackground` star layers only
- Moving the showcase into the debug source set behind a hidden entry (Phase 3, D-03)
- CameraX, MediaPipe, image processing (Phase 4)
- Anthropic SDK, palm job API, cache, rate limits, eval harness, LLM-facing schema (Phase 2)
- Result screen, line overlay, saved readings, the lenient client `Json` (`ignoreUnknownKeys`) and the "update the app" state for a newer `schema_version` (Phases 5-6)
- `schema_version` in `/healthz` (add when a client consumes it)
- Final brand name, applicationId and launcher artwork from Figma (before the first Play upload, REL-03)
- Release signing, R8, Cloud Run deploy (REL-*)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: Backend turns a palm photo + hand side + landmarks into a validated, cached `PalmReading v1` through an async job API (backend only)
- Phase 3: User onboards once, lands on Home over the animated cosmic background, and picks a hand
- Phase 4: User gets live hand guidance and captures or imports an upload-ready palm photo
- Phase 5: User sees the Palm / Love / Career result with the line overlay from fixture data, saved and shareable
- Phase 6: Real palm to real reading in under 30 s, with generating/error states and the stubbed rewarded gate
