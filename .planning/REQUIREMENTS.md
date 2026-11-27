# Requirements: [AppName] — AI Palm Reading & Astrology

**Defined:** 2026-11-26
**Core Value:** The palm scan moment: live on-device hand guidance, then a believable Palm / Love / Career reading with the detected lines drawn over the user's own photo, in under 30 seconds.

## v1 Requirements (Milestone 1 — palm-scan vertical slice)

### Foundation

- [x] **FND-01**: Developer can build and install the debug app on the test phone from the command line (Kotlin 2.4 / AGP 9.5 / Hilt / KSP / Room 3 / Navigation 3 toolchain builds green; fallbacks documented)
- [x] **FND-02**: Every screen uses the Nocturne theme generated from `design-system/nocturne.tokens.json` (colours, type scale, shapes, glows) with edge-to-edge layout at targetSdk 36, minSdk 26
- [ ] **FND-03**: Content screens show the layered animated cosmic background; it is static when system animations are off and absent on camera and generating screens
- [x] **FND-04**: The phone can reach the local FastAPI backend's health endpoint over `adb reverse` (debug-only cleartext to 127.0.0.1; Python 3.13 pinned)
- [x] **FND-05**: Client and server share a versioned palm-reading contract (JSON schema + sample) with normalised 0–1 trace coordinates and a `trace_source` per line

### Onboarding

- [ ] **ONB-01**: User sees a branded splash on launch that hands off to onboarding (first run) or Home (returning)
- [ ] **ONB-02**: User can pick the app language (English, Hindi) on first run
- [ ] **ONB-03**: User can page through 3 intro screens with Next / Get started
- [ ] **ONB-04**: User can enter a first name (validated: at least 2 letters) and optionally choose a gender
- [ ] **ONB-05**: User can enter a birth date and optionally a birth time, with an "I don't know my birth time" option
- [ ] **ONB-06**: Onboarding is shown only once; the profile survives app restarts (DataStore, excluded from backup)

### Home

- [ ] **HOME-01**: User sees a time-of-day greeting with their name on Home
- [ ] **HOME-02**: User sees the hero card and feature grid; tiles outside milestone 1 open a "coming soon" state
- [ ] **HOME-03**: User can reopen their last palm reading from Home
- [ ] **HOME-04**: Pressing Back on Home asks to confirm leaving the app

### Scan

- [ ] **SCAN-01**: User can choose left or right hand and see photo tips (good example + 4 bad examples) before scanning
- [ ] **SCAN-02**: User is asked for camera permission; if denied they see an explanation with "Open settings" and "Choose from gallery"; if the camera is unavailable they see a gallery fallback
- [ ] **SCAN-03**: While scanning, user sees live guidance that reacts to back of hand, closed hand, too far, moving, and "palm detected" (stable, no flicker)
- [ ] **SCAN-04**: User can capture only when a palm is detected; user can toggle the flash
- [ ] **SCAN-05**: User can import a palm photo from the gallery; a photo with no hand is rejected on the device with a clear message
- [ ] **SCAN-06**: The captured or imported photo is rotated upright, cropped to the palm, resized to at most 1280 px, and stripped of EXIF/GPS before upload
- [ ] **SCAN-07**: The scanner runs smoothly on the reference low-end phone (2-minute session with no ANR, no visible jank, no overheating)

### Reading pipeline

- [ ] **READ-01**: User's photo, hand side and the 21 hand landmarks are sent to the backend as a job; the app polls for the result with a 45-second overall deadline
- [x] **READ-02**: The backend returns a validated reading (palm summary, traits, Heart/Head/Life/Fate/Sun lines with traces, love, career, verdict) produced by Claude vision; malformed output is retried once, then fails cleanly
- [ ] **READ-03**: When the photo is unusable, the user sees one of 7 reasons (blurry/dark, too far, jewellery, bent, fingers too tight, incomplete, no palm lines) with a retake tip, and the attempt does not use up their free scan
- [ ] **READ-04**: The same palm photo within 24 hours returns the same reading (server cache by visual fingerprint + hand + language + prompt version)
- [x] **READ-05**: The backend never writes the palm image to disk or logs; landmarks are used only for line validation and never stored
- [ ] **READ-06**: AI lines that fail validation against the hand landmarks are replaced by landmark-anchored template lines, and the overlay is labelled approximate
- [x] **READ-07**: Readings never contain death, illness, pregnancy, medical, legal or financial directives (prompt rules + server output filter)
- [ ] **READ-08**: The generating screen shows progress and handles busy, offline and timeout with retry
- [ ] **READ-09**: On the real phone a reading arrives in under 12 s typically (p50) and under 30 s at worst (p95)
- [ ] **READ-10**: The backend rate-limits each install and enforces a daily spend cap

### Result

- [ ] **RES-01**: User can switch between Palm, Love and Career tabs on the result
- [ ] **RES-02**: User sees the detected lines drawn over their own photo with a colour + text legend
- [ ] **RES-03**: User sees a card per detected line with a short label and interpretation
- [ ] **RES-04**: Every reading shows an entertainment disclaimer
- [ ] **RES-05**: The reading (photo + JSON) is saved on the device and survives app restarts
- [ ] **RES-06**: User can share the reading as a branded image card
- [ ] **RES-07**: User can start another scan from the result; "Ask [Guide]" shows a coming-soon state

### Monetisation stubs

- [ ] **ADS-01**: Ad slots and the rewarded-ad gate live behind interfaces with a stub implementation (including failure simulations), so a real ad SDK can drop in later without changing screens
- [ ] **ADS-02**: A first-time user sees no ads at all before and during their first reading
- [ ] **ADS-03**: From the second scan on, "Scan again" carries an AD chip and opens the "Watch a short ad to unlock" sheet (stubbed reward)

## v2 Requirements

### Required before public release

- **REL-01**: User can report a reading (reasons + note) — Google Play AI-generated-content policy
- **REL-02**: User sees a one-line photo-upload privacy notice (hand image + hand geometry sent for analysis, deleted after) and confirms they are 13+
- **REL-03**: Release build (R8) works end to end; MediaPipe native libs are 16 KB-page aligned; Data safety form drafted; internal + closed test tracks running
- **REL-04**: Backend deployed to Cloud Run with job store externalised or single-instance; Play Integrity on quota endpoints

### Engagement modules

- **ENG-01**: Tarot (78 cards × 6 topics, daily seeded draw, explanation, rewarded redraw)
- **ENG-02**: Daily report (lucky numbers/colours, strengths/weaknesses, share)
- **ENG-03**: Love test (deterministic order-independent score, 7 bands)
- **ENG-04**: Horoscope (server-generated daily per sign per locale, rewarded other signs)

### Astrology

- **AST-01**: Vedic chart (sidereal Lahiri, whole-sign houses, Vimshottari dasha) with North/South/Western styles
- **AST-02**: Birth place search + historical timezone
- **AST-03**: Moon sign and soul energy content, each labelled with its zodiac system

### Chat

- **CHAT-01**: AI chat with [Guide] persona, SSE streaming, palm-reading context, follow-up suggestions
- **CHAT-02**: Daily quota with rewarded top-up; blocked/busy/unavailable states; history; report answer

### Monetisation & retention

- **MON-01**: Real ad SDK (AppLovin MAX or AdMob) with UMP consent and PRD §7.3 caps
- **RET-01**: Notifications (max 2/day, quiet hours, honest result notifications)
- **RET-02**: Rating flow (once per success moment, never repeatedly)

### Later

- Trained palm-line segmentation model; localisation tier 1 (10 locales); accessibility audit; saved-readings list; both-hands compare

## Out of Scope

| Feature | Reason |
|---------|--------|
| Paywall, subscriptions, IAP | Decided 2026-11-26: ads only |
| Lock-screen activity and ads (S36) | Play policy risk (PRD §11) |
| Uninstall intercept screen (S35) | Near-policy edge, low value; use an honest win-back notification instead |
| Misleading "your palm has a secret" notifications | Only real results get result notifications |
| Face reading, accounts, social, cloud sync | PRD non-goals for v1 |
| iOS | Phase 2 product decision |
| Server-side storage of palm images | Privacy promise: processed in memory, deleted |
| Copying anything from the reference app (`_extract/`) | Clean-room rule |
| Birth place in onboarding | Deferred to the astrology milestone (user decision 2026-11-26) |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| FND-01 | Phase 1 | Complete |
| FND-02 | Phase 1 | Complete |
| FND-03 | Phase 3 | Pending |
| FND-04 | Phase 1 | Complete |
| FND-05 | Phase 1 | Complete |
| ONB-01 | Phase 3 | Pending |
| ONB-02 | Phase 3 | Pending |
| ONB-03 | Phase 3 | Pending |
| ONB-04 | Phase 3 | Pending |
| ONB-05 | Phase 3 | Pending |
| ONB-06 | Phase 3 | Pending |
| HOME-01 | Phase 3 | Pending |
| HOME-02 | Phase 3 | Pending |
| HOME-03 | Phase 5 | Pending |
| HOME-04 | Phase 3 | Pending |
| SCAN-01 | Phase 3 | Pending |
| SCAN-02 | Phase 4 | Pending |
| SCAN-03 | Phase 4 | Pending |
| SCAN-04 | Phase 4 | Pending |
| SCAN-05 | Phase 4 | Pending |
| SCAN-06 | Phase 4 | Pending |
| SCAN-07 | Phase 4 | Pending |
| READ-01 | Phase 6 | Pending |
| READ-02 | Phase 2 | Complete |
| READ-03 | Phase 5 | Pending |
| READ-04 | Phase 2 | Pending |
| READ-05 | Phase 2 | Complete |
| READ-06 | Phase 2 | Pending |
| READ-07 | Phase 2 | Complete |
| READ-08 | Phase 6 | Pending |
| READ-09 | Phase 6 | Pending |
| READ-10 | Phase 2 | Pending |
| RES-01 | Phase 5 | Pending |
| RES-02 | Phase 5 | Pending |
| RES-03 | Phase 5 | Pending |
| RES-04 | Phase 5 | Pending |
| RES-05 | Phase 5 | Pending |
| RES-06 | Phase 5 | Pending |
| RES-07 | Phase 5 | Pending |
| ADS-01 | Phase 6 | Pending |
| ADS-02 | Phase 6 | Pending |
| ADS-03 | Phase 6 | Pending |

**Coverage:**
- v1 requirements: 42 total
- Mapped to phases: 42
- Unmapped: 0

---
*Requirements defined: 2026-11-26*
*Last updated: 2026-11-26 after roadmap creation (traceability filled)*
