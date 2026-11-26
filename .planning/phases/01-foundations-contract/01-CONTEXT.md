# Phase 1: Foundations & Contract - Context

**Gathered:** 2026-11-26
**Status:** Ready for planning

<domain>
## Phase Boundary

The project builds, installs and runs on the physical test phone (Redmi M2006C3MII, Android 11, connected over wireless adb) in the Nocturne theme; the phone reaches the local FastAPI backend's health endpoint over `adb reverse`; and client and server share one frozen, versioned palm-reading contract. Requirements: FND-01, FND-02, FND-04, FND-05. No product screens (onboarding, home, scan, result) are built here — those are Phases 3–5.

</domain>

<decisions>
## Implementation Decisions

The user asked Claude to decide all gray areas ("use your best approach instead of discussing"). Decisions below are Claude's, grounded in research/SUMMARY.md.

### What Phase 1 shows on the phone
- **D-01:** The app opens on a single scrollable **Nocturne showcase screen**: all colour roles as swatches, the 11 text styles, primary / secondary / ghost buttons (incl. the AD chip variant), one feature card, one chip, the 5 palm-line overlay colours, a simple animated star background (static when system animations are off), and a backend status chip.
- **D-02:** The backend status chip shows Connected / Unreachable with the base URL and a Retry button (no Logcat-only check), so `adb reverse` problems can be diagnosed on the phone.
- **D-03:** After Phase 3 the showcase becomes a debug-only screen (debug source set, reached from a hidden debug entry); it is not shipped in release builds.
- **D-04:** The Phase 1 background is a lightweight placeholder (a few drifting/twinkling star layers drawn in the draw phase). The full 6-layer layered background (FND-03) is Phase 3 work; build it as a reusable composable so Phase 3 extends rather than replaces it.

### Fonts & Hindi text
- **D-05:** Bundle fonts as resources (no downloadable Google Fonts — offline-first, predictable on low-end phones): Cormorant Garamond SemiBold (display) and Manrope Regular/Medium/SemiBold/Bold (UI), all OFL. Use static TTFs, subset to Latin + Latin-Ext to keep APK size down.
- **D-06:** Hindi/Devanagari: bundle **Noto Sans Devanagari** (Regular/Medium/SemiBold/Bold, OFL) as the fallback for the Manrope roles, and **Noto Serif Devanagari** SemiBold for the display role, so Hindi never renders as tofu boxes. Per 01-RESEARCH.md, implement the per-glyph fallback with `android.graphics.Typeface.CustomFallbackBuilder` (API 29+) wrapped as a Compose `FontFamily(Typeface)`; on API 26–28 the system Devanagari font is the fallback. The showcase screen includes one Hindi sample line per text style to prove it.
- **D-07:** Text styles come from the generated tokens (no hand-copied sizes); line heights must stay auto/unclipped for Devanagari (taller ascenders/matras).

### App identity (Claude's discretion)
- **D-08:** Kotlin package / `namespace` = `app.palm` (internal, freely renameable). `applicationId` = `app.palm.dev` for now, defined once in Gradle with a debug suffix `.debug`. — **Reversibility:** one-way once uploaded to Play — the real applicationId must be chosen with the brand before the first Play upload (REL-03, v2); until then it is reversible. Flag in STATE.md as a pre-release decision.
- **D-09:** Display name "[AppName]" in `strings.xml` (single resource to rename); launcher icon = the [Guide] orb illustration on Nocturne bg as an adaptive icon (foreground/background/monochrome layers) generated from the Figma illustration.

### Dev setup & secrets (Claude's discretion)
- **D-10:** Monorepo layout at repo root: `android/`, `backend/`, `contracts/`, `tools/` (existing `tools/adb` kept). Reference material (`replica/`, `_extract/`, xapk) untouched; `_extract/` and the xapk stay out of builds.
- **D-11:** Anthropic key lives in `backend/.env` (`ANTHROPIC_API_KEY`), git-ignored, with a committed `.env.example`. Phase 1's `/healthz` must work without a key (it reports `anthropic_key: configured|missing` but does not call Claude). The user supplies the key before Phase 2.
- **D-12:** One-command scripts in `tools/` (bash, quoting the space in `E:/Palm Reading`): `dev-reverse.sh` (adb reverse tcp:8000), `run-backend.sh` (uv, Python 3.13, uvicorn on 127.0.0.1:8000, no --reload during job tests), `install-debug.sh` (gradlew installDebug + launch). Wireless adb is the default (the Redmi is already paired over TLS); scripts detect the single connected device and fail clearly when none or several are attached.
- **D-13:** The scaffold task verifies the Android SDK has platforms android-36/37 and build-tools 36.x (01-RESEARCH.md reports them present with licences accepted; an earlier check of `%LOCALAPPDATA%\Android\Sdk\platforms` showed only 31/33 — resolve by checking `local.properties`' `sdk.dir`) and installs them with `sdkmanager` only if missing.

### Contract
- **D-14:** `contracts/palm_reading.v1.schema.json` + `contracts/palm_reading.sample.json` (+ one invalid-photo sample per reason or at least one) are the single source; Kotlin DTOs and Pydantic models are written to match and a test on each side parses the samples unchanged. Contract per research SUMMARY sign-offs: normalised 0..1 `[x,y]` traces, `trace_source` = `llm|template|none`, `image_size` = size sent to Claude, `verdict.ok=false` + 7-value reason enum for unusable photos, `schema_version: 1`. — **Reversibility:** costly — once Phases 2/5 build on it, changing field shapes touches both sides; bump `schema_version` instead of mutating v1.

### Claude's Discretion
- All areas above were delegated. Planner/executor may refine details (exact showcase layout, script names, font subsetting tool) without asking, as long as D-01..D-14 hold.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project scope and decisions
- `.planning/PROJECT.md` — product, core value, constraints, key decisions
- `.planning/REQUIREMENTS.md` — FND-01, FND-02, FND-04, FND-05 wording
- `.planning/ROADMAP.md` §Phase 1 — goal and 4 success criteria
- `.planning/STATE.md` — locked decisions and blockers (toolchain build gate)

### Research
- `.planning/research/SUMMARY.md` — resolved conflicts (Nav3/Room 3, poll contract, upload/contract sign-offs), Phase 1 deliverables and exit gate
- `.planning/research/STACK.md` — pinned versions, AGP 9 built-in Kotlin, version catalog, minSdk/targetSdk/compileSdk, fallback set
- `.planning/research/ARCHITECTURE.md` — single `:app` module package-by-feature, `core/designsystem` + `tools/gen_tokens.py`, dev networking (`127.0.0.1`, debug-only cleartext), contract shape
- `.planning/research/PITFALLS.md` — Pitfalls 11 (Play/targetSdk/edge-to-edge), 12 (phone→Windows backend networking), 26 (path with a space)

### Design
- `design-system/nocturne.tokens.json` — DTCG tokens (colour, dimension, typography, shadow, gradient, motion) to generate `NocturneTokens.kt` from
- Figma "[AppName] – UI" https://www.figma.com/design/Ej98dV7fZQUhRwUyfQIlyJ — page "1 · Foundations" (colour/type/effects/icons/emblems/illustrations), "2 · Components" (buttons, chips, cards), "5 · Motion" (background layers)

### Product spec
- `replica/PRD.md` §5.1 (R-PALM-4 response contract, amended by research sign-offs), §9 (architecture/API surface), §10 (NFRs)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `design-system/nocturne.tokens.json`: source for generated Compose theme tokens.
- `tools/adb/*.sh` (`back.sh`, `chat.sh`, `relaunch.sh`): existing adb helper scripts from the reference-app study; new dev scripts sit beside them in `tools/`.

### Established Patterns
- No app code exists yet (greenfield). `_extract/` is the decompiled reference APK — must not be copied from (clean-room rule).

### Integration Points
- Environment: Windows 11, JDK 17, Android SDK at `%LOCALAPPDATA%\Android\Sdk` (platforms 31/33 only; build-tools up to 36.1.0), `uv` 0.11.21, Python 3.14 system (pin 3.13 via uv), Node 24. Test phone: Redmi M2006C3MII (Android 11) over wireless adb.

</code_context>

<specifics>
## Specific Ideas

- The showcase screen doubles as the visual QA target for the design system: compare against Figma page "1 · Foundations" and "2 · Components".
- Hindi sample lines on the showcase prove the font fallback chain on the real phone.

</specifics>

<deferred>
## Deferred Ideas

- Full 6-layer animated background with parallax/tilt — Phase 3 (FND-03).
- Final applicationId and brand name/icon — before first Play upload (REL-03, v2).

</deferred>

---

*Phase: 01-foundations-contract*
