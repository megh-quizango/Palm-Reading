---
phase: 01-foundations-contract
verified: 2026-11-26T12:00:00Z
status: passed
score: 43/43 must-haves verified
covered_files: [".gitattributes",".gitignore",".planning/phases/01-foundations-contract/01-01-PLAN.md",".planning/phases/01-foundations-contract/01-01-SUMMARY.md",".planning/phases/01-foundations-contract/01-02-PLAN.md",".planning/phases/01-foundations-contract/01-02-SUMMARY.md",".planning/phases/01-foundations-contract/01-03-PLAN.md",".planning/phases/01-foundations-contract/01-03-SUMMARY.md",".planning/phases/01-foundations-contract/01-04-PLAN.md",".planning/phases/01-foundations-contract/01-04-SUMMARY.md",".planning/phases/01-foundations-contract/01-05-PLAN.md",".planning/phases/01-foundations-contract/01-05-SUMMARY.md","README.md","android/app/build.gradle.kts","android/app/schemas/app.palm.core.db.AppDatabase/1.json","android/app/src/androidTest/java/app/palm/core/db/AppDatabaseTest.kt","android/app/src/debug/AndroidManifest.xml","android/app/src/debug/res/xml/network_security_config.xml","android/app/src/main/AndroidManifest.xml","android/app/src/main/assets/licenses/CormorantGaramond-OFL.txt","android/app/src/main/assets/licenses/Manrope-OFL.txt","android/app/src/main/assets/licenses/NotoSansDevanagari-OFL.txt","android/app/src/main/assets/licenses/NotoSerifDevanagari-OFL.txt","android/app/src/main/java/app/palm/MainActivity.kt","android/app/src/main/java/app/palm/PalmApp.kt","android/app/src/main/java/app/palm/contract/PalmReadingDto.kt","android/app/src/main/java/app/palm/core/db/AppDatabase.kt","android/app/src/main/java/app/palm/core/designsystem/Effects.kt","android/app/src/main/java/app/palm/core/designsystem/NocturneBackground.kt","android/app/src/main/java/app/palm/core/designsystem/NocturneFonts.kt","android/app/src/main/java/app/palm/core/designsystem/NocturneTheme.kt","android/app/src/main/java/app/palm/core/designsystem/NocturneTokens.kt","android/app/src/main/java/app/palm/core/designsystem/components/Buttons.kt","android/app/src/main/java/app/palm/core/designsystem/components/Chips.kt","android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt","android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt","android/app/src/main/java/app/palm/core/network/HealthClient.kt","android/app/src/main/java/app/palm/showcase/BackendStatus.kt","android/app/src/main/java/app/palm/showcase/ShowcaseScreen.kt","android/app/src/main/res/drawable/ic_launcher_foreground.xml","android/app/src/main/res/drawable/ic_launcher_monochrome.xml","android/app/src/main/res/font/cormorant_garamond_semibold.ttf","android/app/src/main/res/font/manrope_bold.ttf","android/app/src/main/res/font/manrope_medium.ttf","android/app/src/main/res/font/manrope_regular.ttf","android/app/src/main/res/font/manrope_semibold.ttf","android/app/src/main/res/font/noto_sans_devanagari_bold.ttf","android/app/src/main/res/font/noto_sans_devanagari_medium.ttf","android/app/src/main/res/font/noto_sans_devanagari_regular.ttf","android/app/src/main/res/font/noto_sans_devanagari_semibold.ttf","android/app/src/main/res/font/noto_serif_devanagari_semibold.ttf","android/app/src/main/res/mipmap-anydpi/ic_launcher.xml","android/app/src/main/res/values/nocturne_colors.xml","android/app/src/main/res/values/strings.xml","android/app/src/main/res/values/themes.xml","android/app/src/main/res/xml/data_extraction_rules.xml","android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt","android/app/src/test/java/app/palm/core/designsystem/EffectsTest.kt","android/app/src/test/java/app/palm/core/network/HealthCauseTest.kt","android/build.gradle.kts","android/gradle.properties","android/gradle/libs.versions.toml","android/gradle/wrapper/gradle-wrapper.jar","android/gradle/wrapper/gradle-wrapper.properties","android/gradlew","android/gradlew.bat","android/settings.gradle.kts","backend/.env.example","backend/.python-version","backend/app/__init__.py","backend/app/config.py","backend/app/main.py","backend/app/schemas/__init__.py","backend/app/schemas/palm.py","backend/pyproject.toml","backend/scripts/export_schema.py","backend/tests/test_contract.py","backend/tests/test_health.py","backend/uv.lock","contracts/palm_reading.invalid_photo.sample.json","contracts/palm_reading.sample.json","contracts/palm_reading.v1.schema.json","tools/_device.sh","tools/check-motion.sh","tools/dev-reverse.sh","tools/fonts.sh","tools/gen_tokens.py","tools/gradlew.sh","tools/install-debug.sh","tools/run-backend.sh","tools/screen.py","tools/smoke-phone.sh","tools/test_device.sh","tools/test_gen_tokens.py"]
covered_digest: "v3:sha256:2947bd21058694897167ba7c6cb4c10d32113d11ec290705f1d0bffd9b79c588"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 43/43
  gaps_closed:
    - "Prohibition FND-01 clean-room (test tier, no wired enforcement): resolved by the user on 2026-11-26 (one-off _extract/ comparison accepted, 01-UAT.md test 1)"
    - "Prohibition FND-02 no runtime font fetch (test tier, half enforced): resolved by the user on 2026-11-26 (grep result accepted, 01-UAT.md test 2)"
    - "MVP-mode discrepancy: Mode: mvp removed from Phase 1 in ROADMAP.md (commit a34edd7, 01-UAT.md test 3)"
  gaps_remaining: []
  regressions: []
---

# Phase 1: Foundations & Contract Verification Report

**Phase Goal:** The project builds, installs and runs on the physical phone with the Nocturne theme, the phone can reach the local backend, and client and server share one frozen, versioned palm-reading contract.
**Verified:** 2026-11-26T12:00:00Z (17:30 IST)
**Status:** passed
**Re-verification:** Yes. The previous report (d97ac87, human_needed, no gaps) went stale because commit 18b9edc changed the contract model, the manifest and the device script. Its three human items are now closed in 01-UAT.md (a34edd7).

**Mode:** standard goal-backward. ROADMAP.md no longer gives Phase 1 `Mode: mvp`, so MVP-mode verification does not apply.

## Goal Achievement

### Roadmap Success Criteria (the contract)

Every check below was run by this verifier between 17:20 and 17:30 IST on 2026-11-26, after 18b9edc.

| # | Success criterion | Status | Evidence |
|---|---|---|---|
| SC1 | One documented command from a path with a space installs and launches the debug app on the phone, on the target toolchain or the documented fallback | VERIFIED | `bash tools/install-debug.sh` from `E:/Palm Reading` printed "Installed on 1 device", then started `app.palm.dev.debug/app.palm.MainActivity`. `dumpsys package` shows `lastUpdateTime=2026-11-26 17:27:24`, so the fresh build is the one installed. The toolchain did not change in 18b9edc, and `libs.versions.toml` is not in the diff |
| SC2 | The screen uses the Nocturne theme generated from tokens with no hand-copied values, edge-to-edge, targetSdk 36 / minSdk 26 | VERIFIED | `gen_tokens.py --check`: up to date. `test_gen_tokens.py`: OK. aapt2 xmltree on the fresh debug and release APKs: `minSdkVersion=26`, `targetSdkVersion=36`. Smoke matched `bg.base`, `palm.heart`, `Display XL`, `Primary`, `Feature card` and `हृदय रेखा`. No themed .kt file changed since the user-approved visual check |
| SC3 | `GET /healthz` (Python 3.13) answers from the phone over adb reverse at 127.0.0.1, the app shows it, and cleartext is allowed in debug only | VERIFIED | Host `curl 127.0.0.1:8000/healthz` returned `{"status":"ok",...}`. `adb reverse --list` shows `tcp:8000 tcp:8000`. `smoke-phone.sh` matched "Backend: Connected" and "http://127.0.0.1:8000" on the Redmi (M2006C3MII). The debug APK has `networkSecurityConfig`; the release APK still has none |
| SC4 | `contracts/` holds a versioned schema and a sample that Kotlin and Pydantic parse unchanged; coordinates 0-1; `trace_source` on every line; an unusable photo is `verdict.ok=false` with one of 7 reasons | VERIFIED | `uv run pytest -q`: 39 passed (the earlier 33 plus 6 new rejection cases: ok without lines, ok missing head, "yes" for bool, "960" for int, "0.5" for float, `true` for schema_version). Re-running `export_schema.py` rewrote `contracts/palm_reading.v1.schema.json` byte-identically (git shows no diff), so the committed schema matches the strict models. Kotlin `:app:testDebugUnitTest --rerun` (results written 17:26): PalmReadingDtoTest 4/4 against the real `contracts/` files (the `contracts.dir` system property points at `../contracts`), so the unchanged samples still decode and re-encode exactly. The sample lists heart, head, life, fate and sun, so it meets the new ok-true rule |

### Plan must-have truths (merged; roadmap wording kept where they overlap)

| Plan | Truths | Status | Evidence in this round |
|---|---|---|---|
| 01-01 | 8 | 8 VERIFIED | Build and install pass (SC1). Hilt, Nav3, Room 3 and the toolchain files are not in the 18b9edc diff. The on-device AppDatabaseTest result from the previous round still stands, because no db or toolchain file changed |
| 01-02 | 7 | 7 VERIFIED | Strict validation (`ConfigDict(extra="forbid", strict=True)`), `model_validate_json` on the wire and the bool guard on `schema_version` are in `palm.py`. Each invariant has a rejection case, and all of them pass. Nothing in `backend/app` calls `model_validate` in Python mode, so strict mode breaks no caller |
| 01-03 | 8 | 8 VERIFIED | Connected on the phone (SC3). `tools/test_device.sh`: 6 ok, including the new "zero devices under set -e" case. Under `set -e`, `grep -c . \|\| true` now lets the zero-device message print. HealthCauseTest 9/9. Retry and unreachable behaviour: the code is unchanged since the previous on-device run |
| 01-04 | 8 | 8 VERIFIED | Tokens up to date, fonts `--verify` OK (10 static fonts, 4 OFL texts), smoke matched the token and type rows |
| 01-05 | 8 | 8 VERIFIED | Smoke matched Primary and Feature card. EffectsTest 3/3. Components, background and icon are unchanged since the user's visual approval |

**Score:** 43/43 truths verified (4 roadmap SCs + 39 plan truths). None is present but behavior-unverified.

### Prohibitions (ADR-550)

| Plan | Prohibition (tier) | Disposition |
|---|---|---|
| 01-01 | FND-01 clean-room: no reference package id, no files copied from `_extract/` or the xapk (test) | **Resolved by the user.** 01-UAT.md test 1: on 2026-11-26 the user accepted the verifier's one-off comparison (only the empty-file blob matched _extract/; the reference id appears only in the pre-existing helper `tools/adb/relaunch.sh`). 18b9edc adds only first-party files |
| 01-02 | FND-05: no landmarks, image bytes or image fingerprint in the v1 contract (test) | VERIFIED. The wired test `test_privacy_schema_has_no_image_or_landmark_fields` passes in the 39 |
| 01-04 | FND-02: no font without its OFL text in the APK, no runtime font fetch (test) | **Resolved by the user.** 01-UAT.md test 2 accepted the grep result for the no-fetch half. The OFL half is enforced: `fonts.sh --verify` passed again this round |

### Changes since the previous report (18b9edc)

| Change | Verified how | Result |
|---|---|---|
| Strict contract model, `model_validate_json`, ok-true needs heart/head/life, bool `schema_version` rejected | pytest 39 passed; schema re-export gives no diff; Kotlin 4/4 on the unchanged samples | OK |
| `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules=@xml/data_extraction_rules` | aapt2 shows all three in both debug and release APKs. `dumpsys package` flags on the phone: `[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]`, with no `ALLOW_BACKUP` | OK |
| `_device.sh`: zero devices under `set -e`, unauthorized hint | `test_device.sh` 6 ok | OK (the unauthorized branch has no stub case, see Info) |

### Required Artifacts

All artifacts verified in the previous round still exist. The only new file, `android/app/src/main/res/xml/data_extraction_rules.xml`, is referenced by the manifest and compiled into both APKs (resource `@0x7f0f0000`).

### Key Link Verification

The previous round's links are unchanged. 18b9edc touches none of the link endpoints except `palm.py`, `test_contract.py` and `contracts/palm_reading.v1.schema.json`. The model-to-schema link was re-proven by the byte-identical export, and the test-to-sample link by the 39 passing tests.

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|---|---|---|---|---|
| BackendStatusRow | `BackendStatus` | HealthClient `GET BASE_URL/healthz`, then adb reverse, then FastAPI on :8000 | Yes ("Backend: Connected" against the running backend) | FLOWING |
| ShowcaseScreen swatches and type | `NocturneColors.ByPath`, `type.byName` | Generated from tokens (`--check` up to date) | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|---|---|---|---|
| Backend tests | `uv run pytest -q` | 39 passed, 1 warning (Starlette deprecation) | PASS |
| Schema drift | `uv run python scripts/export_schema.py`, then `git status` | contracts/ unchanged | PASS |
| JVM unit tests | `bash tools/gradlew.sh :app:testDebugUnitTest --rerun :app:assembleDebug :app:assembleRelease` | rc=0; 4+3+9 tests, 0 failures | PASS |
| Manifest, both builds | aapt2 `dump xmltree` and `dump badging` | backup attributes present; minSdk 26, targetSdk 36; release has no networkSecurityConfig | PASS |
| Device script | `bash tools/test_device.sh` | 6 ok | PASS |
| Tokens and fonts | `gen_tokens.py --check`, `test_gen_tokens.py`, `fonts.sh --verify` | up to date, OK, OK | PASS |
| Full stack on the phone | `SMOKE_SCROLLS=10 bash tools/smoke-phone.sh "Backend: Connected" "http://127.0.0.1:8000" "bg.base" "palm.heart" "Display XL" "Primary" "Feature card" "हृदय रेखा"` | `smoke OK` | PASS |
| Backup flag on the device | `adb shell dumpsys package app.palm.dev.debug` | no ALLOW_BACKUP | PASS |

### Probe Execution

There are no `scripts/*/tests/probe-*.sh` probes in this project, and no plan declares one, so Step 7c does not apply.

### Requirements Coverage

| Requirement | Source plans | Status | Evidence |
|---|---|---|---|
| FND-01 | 01-01, 01-03 | SATISFIED | SC1 and the 01-01 truths. The clean-room prohibition was closed by the user (UAT 1) |
| FND-02 | 01-04, 01-05 | SATISFIED | SC2 and the 01-04/01-05 truths. The font prohibition was closed by the user (UAT 2) |
| FND-04 | 01-01, 01-03, 01-05 | SATISFIED | SC3 and the 01-03 truths |
| FND-05 | 01-02 | SATISFIED | SC4 and the 01-02 truths |

No requirement is orphaned. REQUIREMENTS.md maps only FND-01/02/04/05 to Phase 1.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| files changed in 18b9edc | - | TBD/FIXME/XXX/TODO/HACK | none found | - |
| backend/app/schemas/palm.py | 130 | The new ok-true line rule and strictness live only in Pydantic; the Kotlin DTO does not repeat them | Info | Fine for v1: the server produces readings, and the JSON shape is unchanged (only the description text moved). Note it if the client ever validates readings itself |
| tools/_device.sh | 12 | The unauthorized hint has no stub case in test_device.sh | Info | It only changes a message; the zero-device path it sits in is tested |
| res/drawable/ic_launcher_foreground.xml | - | Vector orb stands in for the Figma [Guide] art | Info | Known stub, approved (D-09) |
| android/gradle/libs.versions.toml | 5 | Compose BOM 2026.09.00, one line behind the plan | Info | Documented in STATE.md |

## Human Verification Required

None. The three items from the previous report are closed in 01-UAT.md (3/3 passed, commit a34edd7), and the 01-05 visual check was approved earlier. No file the user approved visually changed in 18b9edc.

## Gaps Summary

No gaps. The four roadmap success criteria and all 39 plan truths still hold after the review fixes. The verifier re-ran the backend, Kotlin, tool and on-phone checks itself instead of relying on the orchestrator's report.

Device state after verification: the debug app is reinstalled and shows "Backend: Connected". The adb reverse rule is unchanged (`tcp:8000 tcp:8000`). The animation setting was not touched, and the backend on :8000 was never stopped.

---

_Verified: 2026-11-26T12:00:00Z_
_Verifier: Claude (gsd-verifier)_
