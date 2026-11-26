# API Coverage — Anthropic Messages API (Python SDK anthropic 2.14.0)

> Full coverage by default. Opt-outs are explicit, reasoned decisions. Phase 2 makes one single-turn Claude vision call per palm job (plans 02-01, 02-03, 02-05; real calls only in 02-08).

| capability | decision | reason |
|---|---|---|
| messages.parse structured output (output_format = Pydantic LlmReading) | INTEGRATE | |
| vision input (base64 JPEG image block, image before text) | INTEGRATE | |
| image transformations.oversized_image = error | INTEGRATE | |
| output_config.effort | INTEGRATE | |
| prompt caching (cache_control on the system block) | INTEGRATE | |
| stop_reason handling (refusal, max_tokens) | INTEGRATE | |
| usage accounting (input, output, cache creation, cache read tokens to cost) | INTEGRATE | |
| typed exceptions, client timeout and max_retries | INTEGRATE | |
| served model id (response.model into meta.model) | INTEGRATE | |
| server-side refusal fallback (fallbacks beta) | OPT-OUT | not needed yet: off per D-04 (amended) until the eval shows real refusals; revisited at the 02-09 sign-off |
| streaming | OPT-OUT | not needed: one non-streaming call with max_tokens 8000 per job; the client polls the job (D-01) |
| thinking configuration | OPT-OUT | not needed: thinking cannot be disabled on the 5.5 models; effort low is the only control (D-04) |
| sampling parameters (temperature, top_p, top_k) | OPT-OUT | explicitly out of scope: rejected on the current API version; determinism comes from the result cache (D-04, D-12) |
| tool use and tool runner | OPT-OUT | not needed: structured outputs give the JSON shape; forced tool_choice is rejected on Sonnet 5.5 |
| Message Batches | OPT-OUT | not needed: the reading path is interactive and latency-bound; the eval spend is small |
| Files API | OPT-OUT | explicitly out of scope: each palm image is used once and must not persist anywhere (D-08) |
| token counting endpoint | OPT-OUT | not needed: response usage is enough for the spend ledger |
| Models API | OPT-OUT | not needed: the model id is fixed by PALM_MODEL and the user sign-off |
| PDF documents and citations | OPT-OUT | not needed: no document input |
| web search, web fetch, code execution, memory, computer use, MCP connector | OPT-OUT | not needed: single-turn image reading |
| fast mode | OPT-OUT | not needed: Opus-tier only at premium price; Opus is not a default model for this app |
| compaction, message threads, context editing, mid-conversation system messages, task budgets | OPT-OUT | not needed: single-turn calls with a byte-stable system prompt |
| priority tier | OPT-OUT | not needed: not supported on the 5.5 models |
| Managed Agents, Admin API, Usage and Cost API | OPT-OUT | not needed: the daily spend cap is computed locally from usage (D-14) |
