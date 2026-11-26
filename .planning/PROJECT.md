# [AppName] — AI Palm Reading & Astrology (Android)

## What This Is

A clean-room Android app for mass-market users in India, South-East Asia and Latin America who enjoy palmistry and astrology. The hook is "point your camera at your palm and AI reads your love, career and life lines", surrounded by cheap-to-serve daily modules (tarot, horoscope, Vedic chart, moon sign, soul energy, love test, daily report) and a chat with a mystic persona ("[Guide]"). It is a rebuild of the features and flows of a reference app (studied in `replica/PRD.md`) with our own brand, copy, art and backend.

## Core Value

The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds. If everything else is rough, this must feel magical and work reliably.

## Business Context

- **Customer**: free users on mid/low-end Android phones in tier-2/3 markets (South Asia first)
- **Revenue model**: ads only — native/banner/interstitial with strict caps, rewarded ads as the only gate on repeat use; no paywall, no IAP
- **Success metric**: D1 retention ≥ 35%, D7 ≥ 12%, ad ARPDAU $0.03–0.08 (PRD §7.3)
- **Strategy notes**: `replica/PRD.md` (§1, §7, §16)

## Requirements

### Validated

(None yet — ship to validate)

### Active

Milestone 1 — PRD phase 1 vertical slice ("a real reading on a real phone"):

- [ ] Android app shell in Kotlin + Jetpack Compose with the Nocturne theme (tokens from `design-system/nocturne.tokens.json`), typed navigation, animated cosmic background
- [ ] Onboarding: splash, language, 3 intro pages, name + gender, birth date/time (S01–S05) persisted locally
- [ ] Home hub (S06) with greeting, hero card and feature grid (non-milestone tiles show "coming soon")
- [ ] Hand selection with photo tips (S07)
- [ ] Live hand scan (S08) with CameraX + MediaPipe Hand Landmarker guidance states: back of hand, open hand, too far, hold still, palm detected; gallery import; camera denied / unavailable states
- [ ] Generating screen (S09) while the backend works, with busy / offline states
- [ ] FastAPI backend (local for milestone 1) with async palm job API: upload → job id → SSE/poll result; Claude vision returns the PRD R-PALM-4 reading JSON including approximate line polylines and a photo-quality verdict
- [ ] Palm result (S10): Palm / Love / Career tabs, line overlay on the photo, line cards, invalid-photo screen with the 7 reasons, entertainment disclaimer
- [ ] Readings stored locally (Room); photo stripped of EXIF before upload; server deletes the image after processing
- [ ] Ad slots and rewarded gate stubbed behind an interface (no ad SDK yet); no ads during a first-time user's first reading

Later milestones (from PRD §14): tarot, daily report, love test, horoscope pipeline; Vedic engine, chart styles, moon sign, soul energy; AI chat with persona, quotas, history; AppLovin MAX / AdMob with caps + UMP consent; notifications and rating; trained palm-line model; localisation, accessibility and release hardening.

### Out of Scope

- Paywall, subscriptions, in-app purchases — decided 2026-11-26: ads only (PRD §7.3)
- Lock-screen activity and lock-screen ads (S36) — Play policy risk (PRD §11)
- Misleading "your palm has a secret" notifications — only real results get result notifications (PRD §11)
- Face reading, accounts, social features, cloud sync — PRD non-goals for v1
- iOS — phase 2
- Reusing anything of the reference app's name, persona, copy, art, endpoints or keys — clean-room rule
- Custom palm-line segmentation model in milestone 1 — v1 uses a vision LLM (PRD §14 fast-start); model comes later

## Context

- Reference app studied statically and on device: `replica/PRD.md` (screens S01–S38, flows F01–F12, business rules, ad behaviour, on-device findings §16), `replica/features.csv`, reference screenshots in `replica/screens/`.
- Design is complete in Figma "[AppName] – UI" (https://www.figma.com/design/Ej98dV7fZQUhRwUyfQIlyJ): Nocturne direction, 45 colour/dimension variables + motion variables, 11 text styles, 54 duotone icons, 13 gradient emblems, layered illustrations with "(anim: …)" layers, 24 component sets, 119 screen frames with states, a clickable F01/F02 prototype and a Motion spec page. Tokens exported to `design-system/nocturne.tokens.json`.
- Brand name and persona are placeholders: `[AppName]`, `[Guide]`.
- Dev machine: Windows 11, JDK 17, Android SDK at `%LOCALAPPDATA%\Android\Sdk`, Python 3.14, Node 24, adb scripts in `tools/adb`. A physical Android phone is available for on-device testing (ARTEMIS MCP can drive it).
- The decompiled reference APK lives in `_extract/` and must not be copied from.

## Constraints

- **Tech stack (client)**: Kotlin, Jetpack Compose, Navigation-Compose (typed), Hilt, Room, DataStore, CameraX, MediaPipe Tasks Hand Landmarker, OkHttp + SSE, kotlinx.serialization, Coil — PRD §9, proven by the reference app
- **Tech stack (server)**: Python FastAPI; Claude vision for the palm reading JSON (Haiku-class for chat later); runs locally in milestone 1, Cloud Run later
- **Platform**: minSdk to be confirmed in research (reference app uses 30); must run well on mid/low-end phones
- **Performance**: palm result p50 < 12 s, p95 < 30 s; cold start < 2 s to first frame (PRD §10)
- **Privacy**: strip EXIF/GPS before upload; server deletes palm images after processing; no biometric templates stored
- **Policy**: entertainment disclaimer on every reading; no medical/legal/financial directives; ad caps from PRD §7.3
- **Secrets**: Anthropic API key only on the server (env var), never in the app

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Ads only, no paywall/IAP | On-device check showed the reference monetises purely with ads; simpler build | — Pending |
| Kotlin + Jetpack Compose client | PRD §9 stack, matches what the reference proved | — Pending |
| FastAPI + Claude backend | Vision LLM gives a v1 reading without a custom model (PRD §14 fast-start) | — Pending |
| Backend local first, Cloud Run later | Faster iteration for milestone 1; deploy when the slice works | — Pending |
| Nocturne visual direction | Chosen from 3 directions on 2026-11-26 | — Pending |
| Milestone 1 = PRD phase 1 slice, real reading on device | Proves the core value end to end before breadth | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-11-26 after initialization*
