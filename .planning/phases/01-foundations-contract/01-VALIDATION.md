---
phase: "1"
slug: "foundations-contract"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: draft
nyquist_compliant: false
wave_0_complete: false
created: "2026-11-26"
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | pytest 9.2.0 + FastAPI TestClient (backend); JUnit 4.13.2 JVM unit tests and one androidTest (Android); stdlib unittest (tools); on-device checks via adb (`tools/smoke-phone.sh`, `tools/screen.py`, `tools/check-motion.sh`) |
| **Config file** | `backend/pyproject.toml` `[tool.pytest.ini_options]` (created in 01-01 Task 1); `android/app/build.gradle.kts` `testOptions` (01-02 Task 1) |
| **Quick run command** | `uv run --directory backend pytest -q` |
| **Full suite command** | `uv run --directory backend pytest -q && bash tools/gradlew.sh :app:testDebugUnitTest :app:assembleDebug && python -I tools/gen_tokens.py --check && python -I tools/test_gen_tokens.py && bash tools/test_device.sh` |
| **Estimated runtime** | ~2 s backend; ~30-90 s Gradle (first build several minutes); on-device smoke ~30-60 s each |

---

## Sampling Rate

- **After every task commit:** the task's own `<automated>` commands (backend tasks: `uv run --directory backend pytest -q`; Android tasks: `bash tools/gradlew.sh :app:testDebugUnitTest`; theme tasks: `python -I tools/gen_tokens.py --check`)
- **After every plan wave:** full suite command above, plus `bash tools/smoke-phone.sh "Backend: Connected"` when a phone is attached
- **Before `/gsd-verify-work`:** full suite green and the on-device checks in the map below green
- **Max feedback latency:** ~2 s backend, ~90 s Gradle (Gradle cannot meet 1 s; accepted)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | FND-01, FND-04 | T-01-01, T-01-02, T-01-SC | .env ignored before first backend file; backend bound to 127.0.0.1; pins locked | e2e (device) | `bash tools/smoke-phone.sh "Backend: Connected"` | ❌ created in task | ⬜ pending |
| 1-01-02 | 01 | 1 | FND-01, FND-04 | T-01-03, T-01-04 | release merged manifest has no plain-HTTP config; release BASE_URL https://api.invalid | instrumented + build | `source tools/_device.sh && bash tools/gradlew.sh :app:connectedDebugAndroidTest` + aapt2/manifest checks | ❌ created in task | ⬜ pending |
| 1-02-01 | 02 | 2 | FND-05 | T-01-08 | strict decoding on both sides | unit | `uv run --directory backend pytest -q tests/test_contract.py` and `bash tools/gradlew.sh :app:testDebugUnitTest --tests "app.palm.contract.*"` | ❌ created in task | ⬜ pending |
| 1-02-02 | 02 | 2 | FND-05 | T-01-06, T-01-07 | no hand-geometry/image fields; invariants reject malformed readings | unit | `uv run --directory backend pytest -q tests/test_contract.py -k "reason or invariant or privacy"` | ❌ created in task | ⬜ pending |
| 1-03-01 | 03 | 2 | FND-04 | T-01-12 | single-flight Retry | unit + e2e (device) | `SMOKE_NO_REVERSE=1 SMOKE_RETRY_EXPECT="Backend: Connected" bash tools/smoke-phone.sh "Backend: Unreachable" "Connection refused" "127\.0\.0\.1:8000" "Retry"` | ❌ created in task | ⬜ pending |
| 1-03-02 | 03 | 2 | FND-04, FND-01 | T-01-09, T-01-10, T-01-11 | /healthz never returns the key; .env untracked; absolute env path | unit | `uv run --directory backend pytest -q tests/test_health.py` and `bash tools/test_device.sh` | ❌ created in task | ⬜ pending |
| 1-04-01 | 04 | 3 | FND-02 | — | N/A | unit + device | `python -I tools/test_gen_tokens.py`, `python -I tools/gen_tokens.py --check`, `tools/screen.py pixel` checks | ❌ created in task | ⬜ pending |
| 1-04-02 | 04 | 3 | FND-02 | T-01-13, T-01-14, T-01-15 | pinned font sources; OFL shipped | structure + device | `bash tools/fonts.sh --verify` and `SMOKE_SCROLLS=8 bash tools/smoke-phone.sh ...` | ❌ created in task | ⬜ pending |
| 1-05-01 | 05 | 4 | FND-02, FND-04 | — | N/A | unit + e2e (device) | `bash tools/gradlew.sh :app:testDebugUnitTest --tests "app.palm.core.designsystem.*"` (gradient angles, RESEARCH Q3) and `SMOKE_SCROLLS=10 bash tools/smoke-phone.sh "Backend: Connected" "Primary" ...` | ❌ created in task | ⬜ pending |
| 1-05-02 | 05 | 4 | FND-02 | T-01-16 | phone animation setting restored | device | `bash tools/check-motion.sh off` / `on` + human-check | ❌ created in task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Greenfield: no separate Wave 0. Each task creates the test it is verified by, in the same task:

- [ ] pytest + httpx pinned and configured (01-01 Task 1)
- [ ] `tools/smoke-phone.sh` on-device harness (01-01 Task 1; extended in 01-03 and 01-04)
- [ ] `android/app/src/androidTest/java/app/palm/core/db/AppDatabaseTest.kt` (01-01 Task 2)
- [ ] `backend/tests/test_contract.py`, `android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt`, `contracts/*.sample.json` (01-02)
- [ ] `backend/tests/test_health.py`, `android/app/src/test/java/app/palm/core/network/HealthCauseTest.kt`, `tools/test_device.sh` (01-03)
- [ ] `tools/test_gen_tokens.py`, `tools/screen.py --self-test`, `tools/fonts.sh --verify` (01-04)
- [ ] `android/app/src/test/java/app/palm/core/designsystem/EffectsTest.kt`, `tools/check-motion.sh` (01-05)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Showcase matches the tokens/Figma; Hindi has no tofu, joined conjuncts, unclipped matras; launcher orb icon | FND-02 | Glyph shaping and visual fidelity cannot be asserted from the UI tree or a pixel probe | 01-05 Task 2 `<human-check>` (the single phase-end visual check), using tools/adb/shots/showcase-*.png |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 1s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** {pending / approved YYYY-MM-DD}
