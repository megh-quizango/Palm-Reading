---
phase: 01-foundations-contract
reviewed: 2026-11-26T11:33:58Z
depth: standard
files_reviewed: 58
files_reviewed_list:
  - .gitignore
  - .gitattributes
  - README.md
  - android/build.gradle.kts
  - android/settings.gradle.kts
  - android/gradle.properties
  - android/gradle/libs.versions.toml
  - android/gradle/wrapper/gradle-wrapper.properties
  - android/app/build.gradle.kts
  - android/app/src/main/AndroidManifest.xml
  - android/app/src/debug/AndroidManifest.xml
  - android/app/src/debug/res/xml/network_security_config.xml
  - android/app/src/main/res/values/themes.xml
  - android/app/src/main/res/values/strings.xml
  - android/app/src/main/res/mipmap-anydpi/ic_launcher.xml
  - android/app/src/main/res/drawable/ic_launcher_foreground.xml
  - android/app/src/main/java/app/palm/MainActivity.kt
  - android/app/src/main/java/app/palm/PalmApp.kt
  - android/app/src/main/java/app/palm/contract/PalmReadingDto.kt
  - android/app/src/main/java/app/palm/core/db/AppDatabase.kt
  - android/app/src/main/java/app/palm/core/navigation/AppNavDisplay.kt
  - android/app/src/main/java/app/palm/core/network/HealthClient.kt
  - android/app/src/main/java/app/palm/showcase/BackendStatus.kt
  - android/app/src/main/java/app/palm/showcase/ShowcaseScreen.kt
  - android/app/src/main/java/app/palm/core/designsystem/Effects.kt
  - android/app/src/main/java/app/palm/core/designsystem/NocturneBackground.kt
  - android/app/src/main/java/app/palm/core/designsystem/NocturneFonts.kt
  - android/app/src/main/java/app/palm/core/designsystem/NocturneTheme.kt
  - android/app/src/main/java/app/palm/core/designsystem/NocturneTokens.kt
  - android/app/src/main/java/app/palm/core/designsystem/components/Buttons.kt
  - android/app/src/main/java/app/palm/core/designsystem/components/Chips.kt
  - android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt
  - android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt
  - android/app/src/test/java/app/palm/core/designsystem/EffectsTest.kt
  - android/app/src/test/java/app/palm/core/network/HealthCauseTest.kt
  - android/app/src/androidTest/java/app/palm/core/db/AppDatabaseTest.kt
  - backend/.env.example
  - backend/pyproject.toml
  - backend/app/config.py
  - backend/app/main.py
  - backend/app/schemas/palm.py
  - backend/scripts/export_schema.py
  - backend/tests/test_contract.py
  - backend/tests/test_health.py
  - contracts/palm_reading.sample.json
  - contracts/palm_reading.invalid_photo.sample.json
  - tools/_device.sh
  - tools/check-motion.sh
  - tools/dev-reverse.sh
  - tools/fonts.sh
  - tools/gen_tokens.py
  - tools/gradlew.sh
  - tools/install-debug.sh
  - tools/run-backend.sh
  - tools/screen.py
  - tools/smoke-phone.sh
  - tools/test_device.sh
  - tools/test_gen_tokens.py
findings:
  critical: 0
  warning: 9
  info: 9
  total: 18
status: issues_found
---

# Phase 1: Code Review Report

**Reviewed:** 2026-11-26T11:33:58Z
**Depth:** standard
**Files Reviewed:** 58 (generated `contracts/palm_reading.v1.schema.json` is covered by `test_committed_schema_matches_models`)
**Status:** issues_found

## Summary

Scope: phase 1 commits `4b8e8d1..HEAD` (`git diff --name-only 54d9084..HEAD`), minus `.planning/`, fonts, licence texts, `gradle-wrapper.jar`, `gradlew(.bat)`, `uv.lock` and the Room schema JSON. `backend/.env` was not read.

Security checks that came back clean:
- **Cleartext does not reach release builds.** `network_security_config.xml` and its manifest hook exist only in `src/debug`. The main manifest sets no `usesCleartextTraffic`, and targetSdk 36 blocks cleartext by default. Release `BASE_URL` is `https://api.invalid`.
- **No secrets committed.** `git grep` over HEAD finds only the `sk-test-123` test fixture. `.gitignore` covers `backend/.env`, `.env`, `*.jks` and `*.keystore`.
- **`/healthz` never echoes the key.** It returns only `configured`/`missing`, `SecretStr` is used, and tests cover blank keys, the OS env leak and the absolute `.env` path. It does show anyone who calls it whether a key is set (IN-01).
- **The backend binds to 127.0.0.1 only.** The adb scripts quote serials and pass `-s` everywhere.

I ran these locally: `pytest` (33 pass), `test_gen_tokens.py` (14 pass) and `screen.py --self-test` (pass). I also probed the contract and the generator by hand to confirm WR-01, WR-04, WR-05 and WR-08.

There are no blockers. The main problems:
- The device picker fails silently on the zero-device path under `set -e`, and its own test cannot see this.
- Gradle can skip the contract drift test as UP-TO-DATE after a sample changes.
- The Pydantic contract accepts more than the JSON schema it publishes.
- Two cases in `smoke-phone.sh` can kill a process it did not start, or tap the wrong spot.

## Warnings

### WR-01: `_device.sh` exits silently with zero devices under `set -e`; its test cannot catch it

**File:** `tools/_device.sh:10` (test gap: `tools/test_device.sh:20-24`)
**Issue:** `_dev_count="$(printf '%s' "$_dev_list" | grep -c .)"` runs inside scripts that use `set -euo pipefail` (`dev-reverse.sh`, `install-debug.sh`, `smoke-phone.sh`, `check-motion.sh`). With an empty list, `grep -c` prints `0` and exits 1. The assignment returns 1, so `set -e` aborts the caller before the `no device: ...` message prints. I reproduced it: a stub adb with no devices gives rc=1 and no output. This breaks D-12 ("fail clearly when none ... are attached"). An attached but `unauthorized` phone hits the same path. `test_device.sh` runs `source` inside `bash -c` without `set -e` and in a `&&`/`||` context, so it reports `ok zero devices` while the real scripts fail.
**Fix:**
```bash
_dev_count="$(printf '%s' "$_dev_list" | grep -c . || true)"
```
Also run the test harness the way the real scripts do: `bash -c 'set -euo pipefail; ...; source "$1"; echo rc=0 ...' || echo "rc=$?"`, so the zero-device case has to show the message under `set -e`.

### WR-02: The Kotlin contract drift test can be skipped as UP-TO-DATE after a contract sample changes

**File:** `android/app/build.gradle.kts:38-41`
**Issue:** `unitTests.all { it.systemProperty("contracts.dir", ...) }` registers only the path string as a task input. Gradle does not fingerprint the files under `contracts/`. If someone edits `palm_reading.sample.json` (or the invalid sample) without touching Kotlin, `:app:testDebugUnitTest` stays UP-TO-DATE (or comes from the build cache) and the drift detector (FND-05) never runs.
**Fix:**
```kotlin
unitTests.all {
    val contracts = rootProject.file("../contracts")
    it.systemProperty("contracts.dir", contracts.absolutePath)
    it.inputs.dir(contracts).withPropertyName("contracts").withPathSensitivity(PathSensitivity.RELATIVE)
}
```

### WR-03: App data, including the future readings DB, is in cloud backup and device transfer by default

**File:** `android/app/src/main/AndroidManifest.xml:6-11`
**Issue:** No `android:allowBackup`, `android:fullBackupContent` or `android:dataExtractionRules` is set, so backup defaults to on. `palm.db` (`AppDatabase.kt:54`) stores whole reading JSON. Later phases will add DataStore with name, gender and DOB. All of that would go to Google Drive backup and D2D transfer without anyone deciding it should, which conflicts with the project's privacy stance. Setting the flag now is cheap; changing it after release is not.
**Fix:** Set `android:allowBackup="false"`, or add `android:dataExtractionRules="@xml/data_extraction_rules"` (API 31+) plus `android:fullBackupContent` (API 26-30) that exclude `database/palm.db` and the DataStore files. Record the choice in CONTEXT.

### WR-04: The Pydantic contract accepts values the published JSON schema rejects

**File:** `backend/app/schemas/palm.py:31-32`
**Issue:** `ContractModel` uses lax mode. I checked these against the real sample:
- `"present": "yes"` becomes `True`.
- `"ok": "true"` becomes `True`.
- `"w": "960"` becomes `960`.
- `"schema_version": true` becomes `1`.

`contracts/palm_reading.v1.schema.json` declares boolean, integer and `const 1`, so the server enforces a looser contract than the one it publishes. In Phase 2 this model validates Claude output (`messages.parse`). Model output of the wrong type will pass silently and get coerced, which defeats the "invariants enforced by model validators" goal. It also means Python and Kotlin disagree on what counts as valid input.
**Fix:** Use `model_config = ConfigDict(extra="forbid", strict=True)` and validate with `PalmReading.model_validate_json(text)`. Strict JSON mode still accepts UUID and datetime strings. If you keep dict validation in the tests, mark only the scalars strict instead: `StrictBool`, `StrictInt`, `Annotated[float, Strict(), Field(ge=0, le=1)]`, and `schema_version: Literal[1]` with `strict=True`. Add a BROKEN case for each coercion.

### WR-05: A successful reading with zero lines (or no heart/head/life line) passes the contract

**File:** `backend/app/schemas/palm.py:109-127`
**Issue:** `_verdict_rules` requires palm, love and career when `verdict.ok` is true, but nothing about `lines`. A reading with `ok: true` and `lines: []` validates (confirmed). So does one with only a `sun` line. The core value is "detected lines drawn over the user's own photo", so a model reply that drops every line would reach the client as a successful reading with nothing to draw. The PalmLine docstring and the PalmReading invariant list say nothing either way, so the contract does not show whether this is intended.
**Fix:** If ok readings must carry the core lines, add this to `_verdict_rules`:
```python
if self.verdict.ok and not {"heart", "head", "life"} <= set(types):
    raise ValueError("verdict.ok true requires heart, head and life lines")
```
Add a matching `ok_true_no_lines` BROKEN case. If an empty line set is allowed, say so in the docstring and the schema description so Phase 5 handles it.

### WR-06: `smoke-phone.sh` cleanup can force-kill a process it did not start

**File:** `tools/smoke-phone.sh:19-28, 31-37`
**Issue:** `ensure_backend` sets `STARTED_PID` as soon as it launches `run-backend.sh`. Say port 8000 is already held by something that does not answer `/healthz` (another dev server, or a hung uvicorn). The new uvicorn fails to bind and exits, and after 60 s the script exits. `cleanup` then runs `port_pid` and `taskkill //F //T` on whatever owns 127.0.0.1:8000, which is the foreign process and its whole process tree.
**Fix:** Before launching, refuse if the port is already taken: `[ -z "$(port_pid)" ] || { echo "port 8000 busy but /healthz does not answer" >&2; exit 1; }`. Alternatively, record the PID that owns the port after our own `/healthz` succeeds and only `taskkill` that PID.

### WR-07: With `SMOKE_RETRY_EXPECT` and `SMOKE_SCROLLS>0`, the script taps Retry at stale coordinates

**File:** `tools/smoke-phone.sh:67-80, 92-95`
**Issue:** When every regex matches, the loop `break`s before the scroll-back on line 79, so the screen is left scrolled down by `SCROLLS × 2/3` of its height. `XY` is then taken from the first (top-of-page) dump in `$DUMP`, and `input tap $XY` lands wherever that position now is. That is not Retry; it could be another button. The retry check then times out or passes for the wrong reason.
**Fix:** Before extracting `XY`, scroll back to the top (`for _ in $(seq "$SCROLLS"); do swipe $((SH / 6)) $((SH * 5 / 6)) 100; done`) and take a fresh `DUMP="$(ui_dump)"`.

### WR-08: The token generator emits same-object forward references that compile to transparent colour

**File:** `tools/gen_tokens.py:134-141` (also `159-168` for non-colour refs)
**Issue:** A reference becomes the target's qualified name, in file order. If `semantic.a` references `semantic.b` and `b` is declared later, the output is `object NocturneSemantic { val A = NocturneSemantic.B; val B = ... }` (reproduced). Kotlin only rejects unqualified forward references. The qualified read compiles and returns the uninitialised backing field, which is `0` (transparent) for the `Color` value class and `0` for durations. You get a silent wrong colour, not a build failure. The current tokens only reference `color.*`, so this is latent until someone adds a semantic-to-semantic alias.
**Fix:** In `color()`, resolve the reference chain all the way to a `color.*` token, which is always in `NocturneColors`, a different object. For example, follow `target()` until the path starts with `color`, then emit that place. Or raise `TokenError` when the target is in the same object and appears later. Add a test for the alias-chain case.

### WR-09: Changing a token font weight compiles cleanly and crashes the app at launch

**File:** `android/app/src/main/java/app/palm/core/designsystem/NocturneFonts.kt:36-48` (called from `NocturneTheme.kt:141`)
**Issue:** `fontRes` calls `error(...)` for any (role, weight) pair without a bundled file. Typography weights come from `design-system/nocturne.tokens.json` through the generator. Changing, say, Title M to 800 builds fine, `gen_tokens --check` passes, and `fonts.sh --verify` passes. Then `NocturneTheme` throws during the first composition and every screen crashes. Nothing cross-checks the generated specs against the bundled fonts.
**Fix:** Add a JVM unit test that runs over `NocturneTypeSpecs.ByName.values` and checks each `(role, weight)` resolves. To make that testable, expose `fontRes` as `internal`. Alternatively, have `gen_tokens.py` check typography weights against the set of fonts `fonts.sh` builds.

## Info

### IN-01: `/healthz` is public and FastAPI's docs routes are on

**File:** `backend/app/main.py:8-14`
**Issue:** It is fine on 127.0.0.1. Once the backend is on Cloud Run, anyone can call `/healthz` and learn whether the key is configured, and `/docs`, `/redoc` and `/openapi.json` show the API surface.
**Fix:** Before deploying, use `FastAPI(docs_url=None, redoc_url=None, openapi_url=None)` outside dev, and gate the `anthropic_key` field behind a dev flag.

### IN-02: Generator edge cases escape `TokenError` or emit invalid Kotlin

**File:** `tools/gen_tokens.py:217, 186`
**Issue:** A `fontFamily` value containing `"` is written unescaped (`Ui("Man"rope")`, reproduced), which produces invalid Kotlin. A non-numeric `fontWeight` raises a raw `ValueError` traceback instead of the promised "exit 1, token path in the message".
**Fix:** Write `json.dumps(v)` for the Kotlin string literal, and wrap `int(...)` and `float(...)` in a `TokenError` that names the token path.

### IN-03: The Kotlin DTO does not enforce the shape of trace points

**File:** `android/app/src/main/java/app/palm/contract/PalmReadingDto.kt:47`
**Issue:** `trace: List<List<Double>>` decodes `[[0.1]]` or `[[2.0, 3.0, 4.0]]` without error. The "strict" drift detector only checks keys. Phase 5's renderer must not index `p[1]` without checking the point.
**Fix:** Before drawing in Phase 5, validate each point (size 2, values in 0..1) or use a custom serializer for points.

### IN-04: Release build config is unfinished, and the README misstates the release failure

**File:** `android/app/build.gradle.kts:26-28`, `README.md:44`
**Issue:** Release has no `isMinifyEnabled` or `shrinkResources` and no signing config, and it still launches the showcase, which probes `https://api.invalid` (expected until D-03). The README says a release build shows "Plain HTTP blocked". It actually shows "Host not found", because release uses HTTPS to `api.invalid`.
**Fix:** Correct the README row. Track R8 and signing for the hardening phase.

### IN-05: `causeOf` maps every `SocketException` to "start tools/run-backend.sh"

**File:** `android/app/src/main/java/app/palm/core/network/HealthClient.kt:58-59`
**Issue:** `NoRouteToHostException` and `PortUnreachableException` are subclasses of `SocketException`, so they get the "No response" hint. That is misleading once the URL is not loopback.
**Fix:** Match only `SocketException` whose message contains "reset" or "closed", and send the rest to the generic branch.

### IN-06: `dev-reverse.sh` removes every adb reverse rule on the device

**File:** `tools/dev-reverse.sh:6`
**Issue:** `--remove-all` also deletes reverse rules set by other tools.
**Fix:** Use `adb reverse --remove tcp:8000 2>/dev/null || true` before adding the rule.

### IN-07: `check-motion.sh` hard-codes pixel rows 120..1400

**File:** `tools/check-motion.sh:33-35`
**Issue:** The rows suit one phone. A screen shorter than 1401 px raises `ValueError`, and on a tall screen most of the screen is never compared.
**Fix:** Get the height from `wm size`, or let `screen.py hash` take fractions of the screen height.

### IN-08: Kotlin version is duplicated, and the backend test client is deprecated

**File:** `android/build.gradle.kts:4`, `backend/tests/test_health.py:2`
**Issue:** The KGP classpath `2.4.21` is a second copy of `libs.versions.toml` `kotlin`, kept in sync only by a comment. Pytest also warns that Starlette's httpx-based `TestClient` is deprecated (`install httpx2`).
**Fix:** Use `classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")` and plan the test-client migration.

### IN-09: Empty strings pass for prose and meta fields

**File:** `backend/app/schemas/palm.py:36-39, 63`
**Issue:** `interpretation`, `meta.model`, `meta.locale` and the summary fields accept `""` (confirmed). `locale` is documented as BCP-47 but nothing checks it.
**Fix:** Add `Field(min_length=1)` to the prose fields, and a simple pattern such as `^[a-z]{2,3}(-[A-Za-z0-9]{2,8})*$` for `locale`.

---

_Reviewed: 2026-11-26T11:33:58Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
