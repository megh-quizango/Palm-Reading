---
phase: 01-foundations-contract
plan: 02
subsystem: contract
status: complete
tags: [contract, pydantic, json-schema, kotlinx-serialization, fnd-05]
requires:
  - "01-01: backend uv project (pydantic 2.14.4, pytest 9.2.0) and Android Gradle project (kotlinx-serialization 1.13.0, junit 4.13.2)"
provides:
  - "backend/app/schemas/palm.py: SCHEMA_VERSION, ContractModel, Meta, ImageSize, Verdict, PalmLine, PalmSummary, Love, Career, PalmReading (+ Literal aliases HandSide, LineType, TraceSource, InvalidReason, Coord, Point)"
  - "backend/scripts/export_schema.py: export() + CLI writing contracts/palm_reading.v1.schema.json"
  - "contracts/palm_reading.v1.schema.json, palm_reading.sample.json, palm_reading.invalid_photo.sample.json"
  - "app.palm.contract: PalmReadingDto, MetaDto, ImageSizeDto, VerdictDto, PalmLineDto, PalmSummaryDto, LoveDto, CareerDto, HandSide, LineType, TraceSource, InvalidReason"
  - "android testOptions system property contracts.dir"
affects: [phase-2-backend, phase-5-result-screen]
tech-stack:
  added: []
  patterns:
    - "Pydantic models are the contract author; schema is generated and drift-tested by parsed-JSON equality"
    - "Kotlin DTOs have no defaults and are decoded with the strict default Json, so a missing or extra key fails a test"
key-files:
  created:
    - backend/app/schemas/__init__.py
    - backend/app/schemas/palm.py
    - backend/scripts/export_schema.py
    - backend/tests/test_contract.py
    - contracts/palm_reading.v1.schema.json
    - contracts/palm_reading.sample.json
    - contracts/palm_reading.invalid_photo.sample.json
    - android/app/src/main/java/app/palm/contract/PalmReadingDto.kt
    - android/app/src/test/java/app/palm/contract/PalmReadingDtoTest.kt
  modified:
    - android/app/build.gradle.kts
key-decisions:
  - "PalmReading v1 frozen: meta.generated_at is a timezone-aware datetime (AwareDatetime), emitted as ...Z; Kotlin keeps it a String"
  - "Contract invariants live in Pydantic model_validators (PalmLine trace rules, PalmReading verdict rules); the Kotlin DTOs check shape only"
requirements-completed: [FND-05]
duration: 4 min
started: 2026-11-26T10:44:24Z
completed: 2026-11-26
plan_head_before: 42617acb083993fd2857a41850e61cf4e2c66496
plan_head_after: acf53b26575c9d935c0d3cc39e5794dece393a13
actuals:
  tokens: 6474     # chars/4 over added lines in the 10 changed files (includes the generated schema JSON)
  tasks: 2
  commits: 3
coverage:
  - deliverable: "Both samples parse unchanged through Pydantic (model_dump == file) and the committed schema equals the export"
    human_judgment: false
    verification:
      - kind: command
        ref: "uv run --directory backend pytest -q tests/test_contract.py (27 passed)"
        status: pass
  - deliverable: "Invariant breaks, 7 reason codes and the privacy walk"
    human_judgment: false
    verification:
      - kind: command
        ref: 'uv run --directory backend pytest -q tests/test_contract.py -k "reason or invariant or privacy" (23 passed)'
        status: pass
  - deliverable: "Strict Kotlin decode of both samples, unchanged re-encode, 7 reasons, extra-key rejection"
    human_judgment: false
    verification:
      - kind: command
        ref: 'bash tools/gradlew.sh :app:testDebugUnitTest --tests "app.palm.contract.*" (tests=4 failures=0)'
        status: pass
---

# Phase 1 Plan 02: Palm Reading v1 Contract Summary

**PalmReading v1 is frozen. Pydantic models (extra=forbid, [x, y] tuple points bounded 0..1, model validators for the verdict and trace rules) are the authoring source. `contracts/` holds the exported 2020-12 schema (`$id palm_reading.v1`, `prefixItems` points) and two samples, a valid reading and an unusable photo. Both samples round-trip unchanged through Pydantic and through a strict kotlinx decoder.**

## Performance

- Duration: about 4 min (2026-11-26T10:44:24Z to 10:47:25Z)
- Tasks: 2/2
- Files: 10 (9 created, 1 modified)

## Accomplishments

- `backend/app/schemas/palm.py`: the interfaces-block shape with every key required and `X | None` where nullable. The `PalmReading` docstring states the invariants, so they appear in the schema `description`.
- `backend/scripts/export_schema.py`: paths come from `__file__` and the file is written with LF. `export()` is reused by the drift test, so the test and the CLI cannot disagree.
- Valid sample: heart and head use `llm`, life and sun use `template`, and fate is `present: false` with `none` and an empty trace. The text is original and entertainment-toned. The invalid sample is `ok: false`, `reason: "jewelry"`, `lines: []`, with null sections.
- pytest (27 tests): both round trips, the schema drift check, the normalised points check, all 7 reasons, 15 invariant-break cases and the privacy walk. The privacy walk goes over every `properties` map, including `$defs`, and asserts it reaches them. Each of the 15 rejections was checked to fail on its own rule, not a neighbouring one.
- Kotlin (4 tests): strict decode of both samples, re-encode equal as `JsonElement`, all 7 reason strings decode to `InvalidReason.entries` in order, and an extra key throws `SerializationException`.

## Tracer feedback gate

Task 1 (tracer): interactive mode, `human_verify_mode` end-of-phase, automated-only verify. The re-run of both verify commands passed (pytest 3/3, Gradle tests=2 failures=0), so expansion continued with no checkpoint.

## TDD (Task 2)

- RED `81ede0a`: 10 of 27 tests failed (the model-validator invariants). The bound, extra-key, literal and enum cases already passed because of Task 1's field types.
- GREEN `acf53b2`: validators added and 27/27 pass. Re-exporting the schema changed nothing, because the invariant text was already in the Task 1 docstring.
- REFACTOR: none needed.

## Task Commits

1. **Task 1: v1 contract round-trips through Pydantic and strict Kotlin DTOs** - `cb89c49` (feat)
2. **Task 2 RED: failing invariant, reason-code and privacy tests** - `81ede0a` (test)
3. **Task 2 GREEN: model validators** - `acf53b2` (feat)

## Decisions Made

- `generated_at` uses `AwareDatetime`, so a naive timestamp is rejected. Pydantic dumps UTC as `Z`, which keeps the sample canonical.
- Kotlin `trace` is `List<List<Double>>`, as the plan specified. Pair length and 0..1 range are checked in tests, not by the type. Sample coordinates always have a decimal point, so the Double re-encode equals the file.
- The Kotlin test uses `Json` (Default) as the strict decoder. The lenient production client `Json` is left to Phase 5, as planned.

## Deviations from Plan

None. The plan was executed as written.

## Issues Encountered

- Git warns "LF will be replaced by CRLF" for the new files (`core.autocrlf` on this checkout). This is harmless: the drift and round-trip tests compare parsed JSON, not text, which is the reason that anti-pattern was called out.
- No phone install happened in this plan, so the STATE.md MIUI install blocker was left unchanged. It was not re-tested.

## Next Phase Readiness

Phase 2 can validate model replies with `PalmReading.model_validate`. The LLM-facing schema (no min/max) still has to be a separate model. Phase 5 can decode with the `app.palm.contract` DTOs and a lenient `Json { ignoreUnknownKeys = true }`. Any change to field shapes ships as schema_version 2.

## Self-Check: PASSED

- FOUND: all 9 created files and the modified build.gradle.kts
- FOUND: commits cb89c49, 81ede0a, acf53b2 (ancestors of HEAD); `git rev-list --count 42617ac..HEAD` = 3
- All Task 1 and Task 2 acceptance greps pass. The three verify commands exit 0 (27 passed, 23 passed with -k, Gradle tests=4 failures=0)
