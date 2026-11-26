# PRD: AI Palm Reading & Astrology App (clean-room rebuild of "Taqde" v1.0.8)

| | |
|---|---|
| Source studied | `AI+Palm+Reading_1.0.8_APKPure.xapk` (APKPure). Package `com.ai.palmreading.scanpalmistry.horoscope`, store name **Taqde**, v1.0.8 (code 8), minSdk 30, targetSdk 37 |
| Base APK SHA-256 | `941ab39da9e5ceab823f202a3b7bbfb3f60d167109bfdcb9fb2eb430cf29d9cb` |
| Method | Static inspection only: REA `decode_android_resources` (Evidence `ev_3e5d75de…50ba5`), the decoded manifest, string tables (49 locales), and the class, API-path and config-key names in the DEX string pools. The app was **not run** and its code was **not decompiled** (REA's JADX provider isn't configured). Anything about runtime behaviour is marked *inferred* |
| Clean-room rule | This PRD describes features, flows and business logic. The rebuild writes its **own** copy, tarot/zodiac texts, art, persona name and brand. It does not reuse the original's strings, assets, backend endpoints or API keys |
| Date | 2026-11-25 |

Confidence tags: **[O]** observed in the artifact · **[I]** inferred from names or strings · **[A]** our assumption or design choice.

---

## 1. Product summary

A spiritual and entertainment app built around one hook: **"point your camera at your palm and AI reads your love, career and life lines."** Several cheap-to-serve content modules sit around that hook to drive daily return visits: tarot, horoscopes, a Vedic birth chart, a moon sign, a "soul energy" profile, a love-compatibility test, a daily lucky report, and an LLM chat with a mystic persona.

> **On-device check (§16):** on a fresh install in India, the paywall never appeared. Every gate was a rewarded ad, and interstitials fired on almost every navigation. **Decision (2026-11-26): the rebuild ships with no paywall and no in-app purchases.** Monetisation is ads only; rewarded ads are the only gate.

The original makes money from aggressive ad monetisation (AppLovin MAX mediation, with app-open, interstitial, native, rewarded and lock-screen placements). Its code also contains a subscription paywall, but it never appeared on device and **is out of scope for the rebuild**. Retention runs on an unusually heavy **notification and lock-screen engine**.

**Core loop:** Install → onboarding (language, name, birth data) → **palm scan** → result (Palm / Love / Career tabs) → "Ask AI about this" chat → rewarded-ad gates → daily notification brings the user back to the daily report, tarot or horoscope.

**Target user [I]:** mass-market users in South Asia, SEA and LatAm who are interested in astrology. Evidence: Vedic and Indian astrology terms (Lagna, Rasi, Dasha, Rahu/Ketu, Sanskrit sign names like Mesha and Kumbha), North and South Indian chart styles, and 49 UI locales including hi, bn, ta, te, mr, gu, kn, ml, or, ur, my, km and in.

---

## 2. Goals and non-goals

**Goals**
1. A palm reading in under 30 s from app open, with a visual overlay of the detected lines.
2. At least 8 engagement modules that can be gated for monetisation.
3. Day-1 retention through the daily report and scheduled notifications.
4. Revenue from ads (ARPDAU), with rewarded ads gating repeat use.
5. Localise to the top 10 locales at launch and 40+ later.

**Non-goals (v1)**
- Face reading. The backend host name suggests "palm-face-reading" **[I]**, but the client has no face feature.
- Social features, accounts and cloud sync. The original has no login flow; the Google/Facebook sign-in strings come from SDKs **[I]**.
- iOS, which is phase 2.
- Copying the original's dark patterns (see §11).

---

## 3. Screen inventory

Screen IDs come from the original's navigation destinations (`com.ai.palmai.domain.model.nav.Dest.*`) **[O]**. Purposes are taken from string tables **[O]** and naming **[I]**.

| ID | Screen (original route) | Purpose | Key components | States to build |
|---|---|---|---|---|
| S01 | Splash (`ScrSplash`) | Init SDKs and remote config, show app-open or splash ad | logo, loader, ad container | cold, warm, ad timeout, offline |
| S02 | Language (`ScrLanguage`, `ScrLanguageAlt`) | Pick UI language on first run (A/B variant "Alt") | radio list, native ad, Next | first run, from settings |
| S03 | Onboarding (`ScrOnboard`) | 3 intro pages (`onboard_1..3`) | pager, native ad above Next | per page, ad loaded or not |
| S04 | Info: Name (`ScrInfoName`) | Collect first name and gender | text field, gender chips (Male/Female/Other) | empty, filled, validation |
| S05 | Info: Birth (`ScrInfoBirth`) | Collect DOB (and time) | date/time picker | empty, filled |
| S06 | Home (`ScrHome`) | Hub: time-of-day greeting with name, "What's new" carousel, feature tiles, exit dialog | greeting, feature cards, NEW badge, native ads top and bottom | first visit, returning, exit confirm |
| S07 | Hand selection (`ScrHandSelect`) | Choose left or right hand, show photo requirements and a standard example | hand toggle, example image, requirement note | — |
| S08 | Hand scan (`ScrHandScan`) | Live camera with real-time hand guidance, capture or gallery | CameraX preview, frame overlay, live guidance chips, flash, gallery, shutter | permission denied, camera unavailable, palm detected, capture failed |
| S09 | Generating (`ScrGenerating`) | Waiting screen while the AI analyses (palm or astrology) | animated loader, rotating copy, interstitial/native slot | loading, server busy, invalid photo |
| S10 | Hand result (`ScrHandResult`) | Palm reading with 3 tabs: **Palm / Love / Career** | photo with line overlay, line cards (Heart, Head, Life, Fate, Sun), insights, traits, share, scan again, Ask AI, report | rewarded unlock, invalid photo |
| S11 | Chat (`ScrChat`) | AI chat: greeting, suggested questions, "scan my palm" card | message list, streaming bubble, input, suggestion chips, read aloud (TTS), report answer | empty, streaming, quota hit, blocked, busy, unavailable |
| S12 | Chat conversation (`ScrChatConversation`) | Resume a saved conversation | as S11 | — |
| S13 | Chat history (`ScrChatHistory`) | List and delete past conversations | list, swipe or delete | empty state with CTA |
| S14 | Tarot (`ScrTarot`) | Pick a topic: Love, Luck, Business, Financial, Health, Protection | topic grid | free draws left, locked |
| S15 | Tarot pick (`ScrTarotPick`) | Choose 1 face-down card from a spread | card fan, flip animation | — |
| S16 | Tarot card (`ScrTarotCard`) | Revealed card with name and keywords | card art, keywords | — |
| S17 | Tarot explain (`ScrTarotExplain`) | Meaning, what to do, what to avoid | sections, draw again | rewarded gate [I] |
| S18 | Horoscopes (`ScrHoroscopes`) | Choose zodiac sign (12) | sign grid with date ranges | — |
| S19 | Horoscope result (`ScrHoroResult`) | Daily horoscope text, with on-device translation | text, "translating…", "showing original English" | language pack downloading, translation unavailable, free count exhausted |
| S20 | Astrology (`ScrAstrology`) | Vedic astrology hub: my chart or another person's | profile selector | no profile |
| S21 | Astro info (`ScrAstroInfo`) | Enter name, birth date and time, birth place | form | validation |
| S22 | Astro place (`ScrAstroPlace`) | Search birth place (geocoding) | search field, results | no places found, offline |
| S23 | Vedic result (`ScrVedicResult`) | Rasi chart, Lagna, Moon sign, planetary details (degree, sign), current Dasha | chart graphic, planet table | — |
| S24 | Style chart (`ScrStyleChart`) | Switch chart style: North Indian / South Indian / Western | segmented control, chart renderer | — |
| S25 | Moon sign (`ScrMoonSign`) | Moon-sign profile: nature, needs, reactions, relationships, inner world and self-care | long-form sections | all free |
| S26 | Soul energy (`ScrSoulEnergy`) | Vedic sun-sign personality: core, mind and emotions, love, career, strengths, challenges, balance tip | sections, bullet lists | all free |
| S27 | Love test (`ScrLoveTest`) | Enter your name and your partner's | 2 text fields, CTA | validation "enter both names" |
| S28 | Love checking (`ScrLoveChecking`) | Fake progress/suspense screen (ad slot) | animation | — |
| S29 | Love result (`ScrLoveResult`) | Score % with one of 7 bands, entertainment disclaimer | score ring, band title, test again, share | — |
| S30 | Daily report (`ScrDailyReport`) | Lucky number(s), lucky colour(s), strength, weakness, share | cards, share | — |
| S32 | Settings (`ScrSetting`) | Language, rate, share, privacy, terms, licenses, version | list | — |
| S33 | Licenses (`ScrLicenses`) | OSS notices (Astronomy Engine and Checker Framework under MIT) | text | — |
| S34 | Web (`ScrWeb`) | In-app webview for privacy policy and terms | webview | offline |
| S35 | Uninstall intercept (`ScrUninstall`) | "Wait! Before you go": promotes features the user hasn't tried | feature list, Try now, Uninstall | — |
| S36 | Lock-screen activity (`LockScreenActivity`) | Full-screen card shown over the lock screen (daily, love or palm teaser) | card, CTA | — **see §11, not recommended** |
| S37 | Rating dialog | Satisfaction prompt → store review or feedback form | stars, reasons, free text | don't show again |
| S38 | Report-content dialog | Flag an AI reading or chat answer | reasons, note, send | thanks |

**Counts:** 35 navigable screens, the lock-screen activity, 2 dialogs (rating and report) and the exit dialog.

---

## 4. User flows

Click counts are for the happy path and are the targets to beat.

```
F01 First run → first palm reading                         (~9 taps)
    S01 → S02 language → S03 onboarding ×3 → S04 name/gender → S05 DOB → S06 home
    → S07 hand select → S08 scan (live guidance → "Palm detected, tap to scan") → S09 → S10
    edge: camera denied → gallery fallback; invalid photo → "Photo not accepted" with reason
          (blurry, too far, jewelry, bent, fingers too tight, incomplete, no palm lines); server busy.
    monetisation: native ads on S02/S03; interstitial after onboarding (inter_after_ob);
          native_scan on S08/S09; free_hand_scan_count gate → rewarded ad.

F02 Palm result → AI chat
    S10 "Ask AI" → S11 (palm context injected) → stream answer → follow-ups
    edge: chat quota reached ("come back tomorrow"); answer blocked; busy; unavailable.
    monetisation: interstitial every N sent messages (chat_inter_every).

F03 Chat from home
    S06 "Ask now" → S11 greeting + suggested questions → user asks → persona may ask to scan
    palm ("wants to read your palm" card) → F01 scan → back to chat with palm context.

F04 Tarot daily pull                                       (4 taps)
    S06 → S14 topic → S15 pick 1 card → S16 reveal → S17 explanation (do / avoid)
    edge: free_tarot_draw_count exhausted → rewarded ad.
    retention: "One card left to flip" notification if the user leaves before reveal.

F05 Horoscope
    S06 → S18 pick sign → S19 result (auto-translate to UI language via on-device model)
    edge: free_horoscope_count exhausted; translation model downloading or unavailable.

F06 Vedic birth chart
    S06 → S20 → S21 name + DOB/time → S22 birth place search → S09 generating → S23 chart
    → S24 switch style. "Other person's astrology" stores a second profile.

F07 Moon sign / soul energy
    S06 "What's new" → S25 or S26 (derived from birth data) → all sections free.

F08 Love test                                              (4 taps)
    S06 → S27 two names → S28 checking (ad) → S29 score + band → share / test again.

F09 Daily report
    Notification "Your lucky number today is…" or S06 → S30 → share.

F11 Rating
    After a positive moment (rate_success_trigger) or on home (rate_home_session):
    satisfied → store review; not satisfied → reason list → feedback.

F12 Uninstall intercept
    App-info / long-press intent → S35 lists untried features → Try now / Uninstall.
```

---

## 5. Feature specification (by module)

### 5.1 Palm scanning and reading (the core feature)

**Client-side real-time guidance [O/I].** The original bundles MediaPipe Tasks (`gesture_recognizer.task` at 8.4 MB, hand-landmarker classes) and CameraX. Live guidance states:
- back of hand shown → "turn your palm toward the camera"
- fist or closed hand → "open your hand and spread your fingers"
- hand too small → "move closer"
- motion → "hold still"
- valid → "Palm detected, tap to scan" (enables the shutter or auto-captures)

Flash toggle, gallery import, left/right hand selection (`HandSide`).

**Server-side analysis [O].** The image is uploaded to a hosted model served through a **Gradio** API (`GradioCallRequest`, `GradioFileData`, `GradioEvent`, path `gradio_api/call/api_palm/{eventId}`). That means the upload returns an event id and the result is fetched as an event stream. The response model (`PalmApiResponse`/`PalmApiResult`) contains:
- `PalmApiPalm`: overall summary and traits
- `PalmApiLine[]` with `PalmApiLineTrace`: **polyline coordinates per line**, used to draw the overlay on the photo, plus `PalmApiImageSize` for scaling
- `PalmApiLove`: how you love, ideal partner, challenges in love
- `PalmApiCareer`: professional strengths, professional challenges, ideal industries
- validation verdicts: blurry or dark, too far, jewelry, bent, fingers too tight, incomplete, no palm lines

**Lines covered:** Heart, Head, Life, Fate and Sun. Each card shows a title and the AI's interpretation (`"%1$s · %2$s"` format: line name and a short label).

**Persistence [O]:** `SavedPalmReading` holds the reading id, image path, hand side, timestamp, lines, love and career data. The last reading feeds the chat context and the push copy.

**Rebuild spec**

| Requirement | Detail |
|---|---|
| R-PALM-1 | On-device hand detection with MediaPipe Hand Landmarker. Thresholds: hand bbox ≥ 35% of frame width, palm-facing (landmark z / handedness check), fingers spread (angle between index and pinky MCP vectors), motion below a set px/frame for 500 ms |
| R-PALM-2 | Capture at least 1080 px on the short side, JPEG q85, strip EXIF/GPS before upload |
| R-PALM-3 | Server: (a) a quality gate (blur via Laplacian variance, brightness, jewelry and occlusion classifier); (b) palm-line segmentation (a U-Net-style model trained on our own labelled dataset, outputting polylines per line class); (c) an LLM that turns line geometry features (length, depth or continuity, curvature, breaks, forks, start and end zones) into structured JSON text for the Palm, Love and Career sections |
| R-PALM-4 | Response contract: `{reading_id, image_size{w,h}, lines[{type, label, trace[[x,y]...], interpretation}], palm{summary, traits[]}, love{how_you_love, ideal_partner, challenges}, career{strengths, challenges, ideal_industries[]}, verdict{ok, reason}}` |
| R-PALM-5 | Async job: POST upload → `job_id`; then SSE or poll for the result. Time out at 45 s with a friendly "busy" state |
| R-PALM-6 | Readings are stored locally (Room). The image is deleted server-side after processing (privacy promise) |
| R-PALM-7 | Readings are **deterministic per image hash** for 24 h (cache), so a re-scan of the same photo doesn't contradict itself |
| R-PALM-8 | Share the reading as an image card with app branding |

### 5.2 AI chat with a mystic persona

**What was observed [O]:**
- Backend `…/v1/ai_llm/profiles/personal_reading_chat`, a server-side **profile/persona** abstraction (`AiLlmProfile`, `AiLlmPersona`).
- Streaming: `ChatDelta` chunks and `ChatDone`, which carries follow-up suggestions and flags.
- `ChatRequest` = {conversation id, message, locale, `ChatContext`, history `ChatTurn[]`, `ClientContext`}.
- `ChatContext` = {`ChatUserInfo` (name, gender, DOB), `PalmContext` (6 fields: the last palm reading's line summaries)}.
- `ClientContext` = app version, device, locale and timezone. *Inferred:* its 14 fields include an install id and a usage counter.
- Errors: `quota` (daily limit), `blocked` (safety), `busy`, `unavailable`.
- UI pieces: suggested starter questions (career, heart line, marriage, soulmate, true love), a "Read your palm instantly" card inside the chat, read aloud (Android TTS), and per-answer reporting with reasons (inaccurate, inappropriate, off-topic, offensive, other).
- History: local `ChatHistoryStore` with conversations titled "Conversation N" and delete support.

**Rebuild spec**

| Requirement | Detail |
|---|---|
| R-CHAT-1 | Our own persona (**rename**: the original uses a named sage). The system prompt covers warm, mystical, non-fatalistic tone; **no medical, legal or financial directives**; no death or illness predictions; self-harm escalation to helplines; never claim certainty; and an "entertainment" framing |
| R-CHAT-2 | Inject the user profile and the latest palm reading as context. Keep the last 20 turns, summarising older ones server-side |
| R-CHAT-3 | SSE streaming. First token in under 1.5 s at p50 |
| R-CHAT-4 | Daily quota per install: free N/day (fair-use cap). A server-side counter keyed by install id plus Play Integrity token |
| R-CHAT-5 | Moderation pre- and post-filter. A blocked answer returns `blocked` and the UI shows the rephrase message |
| R-CHAT-6 | The assistant may emit a structured action `request_palm_scan`, which renders the scan card |
| R-CHAT-7 | Up to 3 follow-up suggestions returned in `done` |
| R-CHAT-8 | Report answer → `POST /report` (`ReportRequest`: reading or message id, reason, note, locale, app version) |

### 5.3 Tarot

- **[O]** A full **78-card deck**, each card with a name, description and upright meaning. Six topics (Love, Luck, Business, Financial, Health, Protection), each with its own reading title. Single-card pull with a "what to do / what to avoid" explanation, keywords, and Draw again.
- **[O]** `free_tarot_draw_count` and `tarot_topic_count` config, plus the "one card left to flip" re-engagement notification.
- **Rebuild:** write our own texts for 78 cards × 6 topics, generated once by an LLM and edited by a human, then **bundled offline**. Card art must be original or public domain (the 1909 Rider–Waite–Smith deck is public domain in the US; check other markets). Draw with a seeded RNG (per day, per topic) so the same day gives the same card, then reroll on "Draw again" (gated).

### 5.4 Horoscopes

- **[O]** 12 signs with date ranges. Text comes from a free third-party horoscope API. On-device ML Kit Translation turns it into the UI language, with "downloading language pack", "translating" and "showing original English" states. Gated by `free_horoscope_count`.
- **Rebuild:** don't depend on an unofficial free API. Generate daily, weekly and monthly horoscopes per sign per locale **once a day server-side** with an LLM (12 × L calls/day, which is trivial cost) and serve them from a CDN as JSON. Keep on-device translation only as a fallback.

### 5.5 Vedic astrology (birth chart)

- **[O]** Computed **on device** with Astronomy Engine (MIT, listed in the app's licenses). Planets: Sun, Moon, Mars, Mercury, Jupiter, Venus, Saturn, Rahu, Ketu and the Lagna (ascendant), each with sign and degree (`%d°%02d'`). Rasi chart and current **Dasha** (Vimshottari). Chart styles North Indian, South Indian and Western (`ChartStyle`). Profiles for "my" chart and another person's (`BirthProfile`, `BirthLocation`). Birth-place search via the Open-Meteo geocoding API.
- **Rebuild:** the same approach works and is cheap: Astronomy Engine (MIT) plus a sidereal (Lahiri ayanamsa) conversion. Implement whole-sign houses from the Lagna and Vimshottari dasha from the Moon's nakshatra. Use a geocoder with a commercial-use licence (Open-Meteo's free tier is non-commercial, so for a commercial app use their paid API or GeoNames/Photon self-hosted) **[A]**. Resolve timezone from coordinates (tz database) for the historical UTC offset.

### 5.6 Moon sign and soul energy (static personality content)

- **[O]** Moon sign has 12 signs × 6 sections (nature, needs, reactions, relationship, inner world, self-care ritual, with the "feels most at home with" compatibility in the relationship section). Soul energy has 12 Vedic sun signs (Sanskrit names) × 7 sections (core, mind and emotions, love, career, strengths, challenges, balance tip) and a "Vedic sun sign · date range" header.
- **Rebuild:** derive the Moon sign from the Vedic chart engine (needs birth time; fall back to noon with a lower-accuracy note). Write our own content, about 12 × 13 sections at 120–200 words each, bundled and localised.

### 5.7 Love test (name compatibility)

- **[O]** Two names → "checking" suspense screen → score with one of 7 bands (from "Written in the stars" down to "Opposite orbits", in our own wording). Has an entertainment disclaimer. Config `love_score_mode` and `love_name_gender` **[I]**: a deterministic score mode, possibly gender-aware.
- **Rebuild:** `score = 40 + (hash(normalize(a)+"|"+normalize(b)) mod 60)`, made **order-independent** by sorting the names first and deterministic so re-tests match. Map score to 7 bands. Optionally blend in zodiac compatibility when both DOBs are known.

### 5.8 Daily report

- **[O]** Lucky number(s), lucky colour(s) from a 32-colour palette, strength, weakness, share text, and the daily notification.
- **Rebuild:** `seed = hash(user_dob + local_date)`. Pick 1–3 numbers (1–99) and 1–2 colours, and strength and weakness from the sign's trait list. The result is the same all day, changes at local midnight, and works offline.

### 5.9 Home, onboarding, settings

- Greeting by time of day (morning, afternoon, evening, optionally with the name). A "What's new" carousel (Ask AI, Vedic chart with the user's Lagna and Moon sign, daily report). Feature grid and exit confirmation dialog.
- Onboarding: language → 3 pages → name and gender → DOB. Settings: language, rate, share app, privacy, terms, licenses, version.

### 5.10 Notifications and retention engine

**Observed channels and types [O]**, each with its own remote toggle (`enable_noti_*`):

| Type | Trigger (inferred) | Copy theme | Keep in rebuild? |
|---|---|---|---|
| Daily | scheduled each morning | "Your lucky number today is…" | **Yes** |
| Shortcut | periodic | "Got 5 seconds? Show us your palm" | Yes (frequency cap) |
| Pinned (ongoing) | persistent quick-access with tiles (palm, tarot, horoscope) | "What do you want to know today?" | Optional, opt-in only |
| Kill-app | user swiped the app away mid-scan | "Your palm has a secret… something unusual in your heart line" | **Rewrite honestly**: only if a scan actually finished |
| Win-back | ~7 days inactive | "Your stars have shifted" | Yes |
| Click-ads / unfinished tarot | left before revealing the card | "One card left to flip" | Yes (once) |
| Lock-screen full-screen | `USE_FULL_SCREEN_INTENT`, `showWhenLocked`, `turnScreenOn`, time slots (`noti_lockscreen_due_slot`), rotating daily, love and palm cards, and `enable_lock_screen_ad` | "Your palm knows a secret" | **No** (policy risk, §11) |

Also: `BootReceiver` (reschedules after reboot), `RECEIVE_BOOT_COMPLETED`, FCM push for remote campaigns, and `noti_schedule_anchor` for the schedule base time.

**Rebuild:** use WorkManager for local schedules with at most 2 notifications per day, quiet hours from 22:00 to 08:00 local, plus FCM for campaigns. Every type gets a remote-config toggle and a frequency cap.

### 5.11 Feedback, rating, reporting

- Rating flow (S37) with session and trigger config (`rate_home_session`, `rate_success_trigger`, `rating_disabled`). Negative reasons: palm inaccurate, hand scan doesn't work, generic AI answers, irrelevant horoscope, slow, **too many ads**, not working, other. A positive rating routes to the Play in-app review.
- Content-report dialog for readings: inaccurate, hurtful, inappropriate, risky to follow, other.

### 5.12 Localisation

- **[O]** 49 locales. Horoscope text is translated on device.
- **Rebuild:** Android string resources, with the LLM prompts carrying `locale`. Bundled static content (tarot, moon, soul) is translated through a TMS. Launch tier: en, hi, es, pt-BR, id, bn, ta, te, ar, vi.

---

## 6. Business logic (rules engine)

| # | Rule | Source |
|---|---|---|
| BL-1 | **Free usage counters** per feature: `free_hand_scan_count`, `free_tarot_draw_count`, `free_horoscope_count`, with chat on a server daily quota. Counters are local (DataStore) and mirrored server-side for chat and palm, which cost money | [O] keys |
| BL-2 | When a counter hits 0: offer a **rewarded ad to unlock one use** (`reward_unlock`). No paywall | [O] device |
| BL-4 | **Interstitial frequency**: after onboarding (`inter_after_ob`), on back navigation (`inter_back`), in-app taps (`inter_inapp_tap`), every N chat messages (`chat_inter_every`), with a global display delay (`inter_display_delay`) and a splash interval (`inter_splash_interval`) | [O] keys |
| BL-5 | **High/normal floor waterfall**: many placements have a `_high` twin (e.g. `native_scan_high`, `inter_splash_high`). The app tries a high-eCPM ad unit first, then falls back to the normal one | [O] keys, [I] behaviour |
| BL-6 | **App-open ads** on cold and warm start, with cold/hot load timeouts, a cache and a forced close-button delay (`aoa_*`) | [O] |
| BL-9 | **Rating gating**: ask only after a success moment, never twice in a session, stop after "Don't show again" | [O] |
| BL-10 | **Deterministic outputs**: daily report, tarot of the day and love score must stay stable within the day or name pair (trust) | [A] |
| BL-11 | **Notification schedule** anchored to the first open, with slotting and the last-type rotation stored so the same type isn't repeated | [O] keys |
| BL-12 | **Language A/B** (`language_alt`, with submit and after delays) and **onboarding A/B** through remote config and A/B testing (Firebase ABT is present) | [O] |

---

## 7. Revenue model

### 7.1 Observed monetisation stack [O]
- **Ads:** AppLovin MAX mediation, Google AdMob/GAM, Pangle (ByteDance), Mintegral and Vungle/Liftoff SDKs are bundled (others are listed only as adapter names), along with Meta Audience Network classes. UMP consent (GDPR/TCF). The ad config ships encrypted (`assets/ads_config.dat`) and is refreshed remotely. Ad-revenue tracking is on (`ad_revenue_enable`).
- **IAP:** Play Billing code and paywall strings exist, but no paywall was shown on device. **Not rebuilt.**
- **Attribution and analytics:** AppsFlyer (installs and ad revenue for UA optimisation), Facebook App Events, Firebase Analytics, Remote Config, A/B Testing, Crashlytics and Performance.

### 7.2 Ad placements (from config names)

| Format | Placements |
|---|---|
| App open | cold and warm start (`aoa_*`, `open_ad`, `inter_appopen`) |
| Interstitial | splash, after onboarding, back, in-app tap, every N chat messages |
| Native (in-feed) | language, language alt, onboarding pages 1–3 and "above Next", info and DOB, name pick, home top and bottom, scan, loading, success or result, exit dialog, welcome back, storage |
| Native full-screen | full splash, full-screen onboarding, resume (welcome back) |
| Rewarded | unlock one use, recover, "enhance" (reward_unlock, reward_recover, reward_enhance) |
| Lock-screen | `enable_lock_screen_ad` (**do not replicate**) |

### 7.3 Proposed monetisation for the rebuild [A]

**Ads only. No paywall, no subscriptions, no in-app purchases** (decided 2026-11-26).

| Feature | Free use | Repeat use |
|---|---|---|
| Palm scan | 1 full reading | 1 rewarded ad per extra scan |
| AI chat | N messages/day (server quota) | interstitial every 3rd message (as the original); rewarded ad for +N messages |
| Tarot | 1 draw/day per topic | 1 rewarded ad per extra draw (single ad, not a pod of 3) |
| Horoscope | own sign daily | 1 rewarded ad per other sign |
| Vedic chart, moon sign, soul energy, love test, daily report | free | free (banner or native ad on screen) |

**Ad caps:** at most 1 interstitial per 90 s, none during the first reading of the first session, no interstitial on every Back press, and no auto-redirect ad formats.

**KPIs:** D1 retention ≥ 35%, D7 ≥ 12%, ad ARPDAU $0.03–0.08 in tier-2/3 geos, rewarded-ad completion ≥ 70%, LTV > 1.3× CPI before scaling UA.

**Unit economics, per palm reading [A]:** segmentation inference about $0.002 on a serverless GPU, LLM text about $0.002–0.01, so under $0.015 per reading. Chat costs about $0.0005–0.003 per message with a small model. Gating with rewarded ads (eCPM $5–20) covers the cost of free users.

---

## 8. Data model (inferred → proposed)

Entity names follow the original's domain models **[O]**. Fields are partly **[I]** and partly **[A]**.

```
UserProfile (local)      id, name, gender(male|female|other), dob, birth_time?, birth_place_id?,
                         zodiac_sign, locale, created_at
BirthProfile             id, owner(self|other), name, dob, time?, location(BirthLocation)
BirthLocation            name, country, lat, lon, timezone        (from geocoding result)
SavedPalmReading         id, image_uri, image_hash, hand_side(left|right), created_at,
                         palm{summary, traits[]}, lines[PalmLine], love{…}, career{…}
PalmLine                 type(heart|head|life|fate|sun), label, interpretation, trace[[x,y]], image_size
Conversation             id, title, created_at, updated_at, turns[ChatMessage], result_context?
ChatMessage              id, role(user|assistant|system), text, created_at, reported(bool), suggestions[]
TarotDraw                id, topic(love|luck|business|financial|health|protection), card_id, date, rerolls
HoroscopeCache           sign, date, locale, text, source_lang
VedicChart (derived)     profile_id, lagna, planets[{planet, sign, degree, house, nakshatra}], dasha{maha, antar, until}
DailyReport (derived)    date, lucky_numbers[], lucky_colors[], strength, weakness
UsageCounter             feature, date, used, limit
Report                   target_type(reading|chat), target_id, reason, note, locale, app_version
NotificationState        type, last_shown_at, slot, enabled
```

**Server tables (Postgres):** `installs` (install_id, integrity_verdict, locale, first_seen), `usage_daily`, `palm_jobs`, `chat_conversations` (optional; privacy allows a "no server history" mode), `reports`, `content_horoscopes`.

---

## 9. Technical architecture (proposed)

| Layer | Choice | Why |
|---|---|---|
| Android client | Kotlin, Jetpack Compose, Navigation-Compose (typed destinations), Hilt, Room, DataStore, WorkManager, CameraX, MediaPipe Tasks, OkHttp/SSE, kotlinx.serialization, Coil 3 | matches what the original proved works, and is all standard |
| Astro engine | Astronomy Engine (Kotlin port, MIT) + our sidereal and dasha code | offline and free |
| Translation fallback | ML Kit on-device translation | as the original |
| Backend API | Cloud Run or Fly.io (TypeScript/Fastify or Python/FastAPI) | stateless, scales to zero |
| Palm model | own segmentation model served on Modal, Replicate or Hugging Face Inference Endpoints, via an async job API | GPU on demand |
| LLM | Claude Haiku-class model for chat and reading text, with a larger model for monthly content generation | cost and quality |
| DB / cache | Postgres (Neon or Supabase) + Redis (quotas) | |
| Ads | AppLovin MAX (or AdMob mediation) with UMP consent | |
| Analytics / attribution | Firebase (Analytics, Remote Config, A/B, Crashlytics), AppsFlyer or Adjust | |
| Abuse | Play Integrity on quota-bearing endpoints, rate-limited per install | protects LLM and GPU cost |

**API surface (ours)**

```
POST /v1/palm/jobs                 multipart image, hand_side, locale  → {job_id}
GET  /v1/palm/jobs/{id}/events     SSE: progress | result | error(reason)
POST /v1/chat/stream               {conversation_id, message, locale, context, history} → SSE delta/done/error
GET  /v1/horoscope/{sign}?date&locale&period
POST /v1/reports
GET  /v1/config                    feature flags (or Firebase Remote Config)
```

---

## 10. Non-functional requirements

- **Performance:** cold start under 2 s to first frame, excluding ads. Ad timeout 4 s on splash, never blocking longer. Palm result p50 under 12 s and p95 under 30 s. Chat first token under 1.5 s.
- **Offline:** tarot, daily report, love test, moon sign, soul energy and the Vedic chart all work offline. Palm, chat and horoscope show offline states.
- **Privacy:** palm images are processed and deleted within 24 h, with no face or biometric templates stored. The privacy policy states that hand images are sent for analysis. Data-safety form: photos, app activity, device IDs (ads). GDPR/UMP consent and a COPPA age gate (13+).
- **Accessibility:** TalkBack labels, dynamic type, 4.5:1 contrast, read-aloud for chat.
- **Quality:** Crashlytics crash-free rate ≥ 99.5%. ANR rate under 0.47% (Play threshold).

---

## 11. Policy, legal and ethical requirements

The original does several things the rebuild should **not** copy:

1. **Lock-screen full-screen activity and lock-screen ads.** `USE_FULL_SCREEN_INTENT` is restricted on Android 14+ to calling and alarm apps. Showing marketing over the lock screen and `enable_lock_screen_ad` break Google Play's disruptive-ads and notification policies, and could get the app suspended. **Drop it.**
2. **Misleading notifications.** The "kill-app" push says something unusual was found in your heart line even when no scan finished. **Only send result notifications for real results.**
3. **Uninstall interception screen.** It's allowed only as an in-app screen, never by blocking or hiding uninstall. Keep it soft or drop it.
4. **Ad density.** Natives on almost every screen plus interstitials on back navigation risk policy strikes and the "too many ads" rating reason the app itself lists. Rebuild with frequency caps: no more than 1 interstitial per 90 s, and none during the first session's first reading.
5. **Claims.** Every reading surface shows an "for entertainment purposes" disclaimer. No health, financial or legal predictions. Tarot "Health" and "Financial" topics stay in reflective, non-directive language.
6. **Clean room.** Don't reuse the original's name, persona, copy, images, ad-unit IDs, API keys or backend endpoints. The original's own backend services and the third-party horoscope API it uses are not ours to call.
7. **Licences.** Astronomy Engine (MIT: include the notice). Tarot art must be original or public domain. MediaPipe model (Apache-2.0).

---

## 12. Analytics event plan

`onboard_step{n}`, `onboard_completed`, `palm_scan_start`, `palm_guidance_state{state}`, `palm_upload`, `palm_result{ok|reason}`, `palm_tab{palm|love|career}`, `chat_sent`, `chat_api_failed{code}`, `tarot_draw{topic}`, `horoscope_view{sign}`, `vedic_generate`, `love_test`, `daily_report_view`, `ad_show{format, placement}`, `ad_revenue{value, network}`, `rate_value{n}`, `report_sent{target, reason}`, `noti_shown{type}`, `noti_open{type}`.

All the names above except the ones marked as ours mirror event keys seen in the original **[O]**: `chat_sent`, `chat_api_failed`, `palm_upload`, `palm_api_failed`, `rate_value`, `love_test`, `daily_report`, `hand_scan`.

---

## 13. What cannot be cloned (skip list)

| Item | Reason |
|---|---|
| Their trained palm-line model and prompts | owned by them, served from their backend. We train our own |
| Their persona name and personality copy | brand asset. Create our own |
| All tarot, moon-sign, soul-energy and horoscope texts | copyrighted copy. Write our own |
| Ad-unit IDs, AppLovin SDK key, AdMob app ID, AppsFlyer key | belong to their accounts |
| Their user base, ratings and store listing | not transferable |
| Lock-screen ad system | policy-violating (§11) |
| Exact price points | not in the APK (fetched from Play at runtime) |

---

## 14. Build plan and sizing

**Size: L** (roughly one quarter for a team of 2–3). The client is M on its own. The palm-line model is the long pole.

| Phase | Scope | Weeks |
|---|---|---|
| 0 | Brand, persona, content writing kick-off (tarot 78×6, moon 12×6, soul 12×7) | 1–2 (runs in parallel) |
| 1 Vertical slice | Onboarding → scan with live guidance → **LLM-only reading** (vision LLM on the photo, no custom model yet) → result tabs | 3 |
| 2 Engagement | Tarot, daily report, love test, horoscope pipeline, home | 2 |
| 3 Astrology | Vedic engine, 3 chart styles, moon sign, soul energy, geocoding | 2 |
| 4 Chat | SSE chat, persona, quotas, history, report | 1.5 |
| 5 Monetisation | MAX ads with caps, rewarded unlocks, UMP | 1 |
| 6 Retention | Notification engine (5 compliant types), rating flow, remote config, A/B | 1 |
| 7 Palm model v1 | Labelled dataset (~3–5k palms, own or licensed), segmentation model, line overlay | 4–6 (parallel from week 2) |
| 8 Hardening | Localisation, accessibility, performance, policy review, store listing | 1.5 |

**The three hardest parts:**
1. A palm-line detection model that draws believable overlays. It needs a dataset you're allowed to use.
2. Ad tuning (caps against ARPDAU and ratings) without policy strikes.
3. A correct Vedic chart (sidereal maths, timezone history, dasha periods).

**Fast-start shortcut [A]:** v1 can skip the custom model. Send the photo to a vision-capable LLM that returns approximate line polylines and the reading JSON. Accuracy and overlay quality will be lower. Upgrade to the trained model once the retention numbers hold.

---

## 15. Open questions

1. Brand name, persona and visual direction (needed before `/replica-brand`).
2. Target launch geos, which drive the locale list and price tiers.
3. ~~Ads vs IAP~~ decided: ads only, no paywall (2026-11-26).
4. Do we store chat history server-side (for cross-device later) or keep it local only?
5. Palm dataset source: collect with consent, buy, or LLM-only v1?

---

## 16. On-device verification (2026-11-25)

**Setup:** Xiaomi Redmi (M2006C3MII), Android 11, India locale, Play Store install of v1.0.8, fresh user. About 35 minutes of manual exploration driven over adb. Test profile: "Alex", Other, 1 Jan 1995 10:00, Mumbai. No purchases, no ad CTAs tapped on purpose, no photos captured or uploaded.

### 16.1 What changed versus the static PRD

| Topic | Static assumption | Observed on device |
|---|---|---|
| **Paywall** | Paywall is the main gate (§6 BL-2, §7) | **Never shown.** Not after onboarding, not on any gate, and there's no premium or restore entry in home or settings. Every gate is a **rewarded ad**. Billing code exists, but the paywall is remotely switched off for this user or market. The rebuild drops the paywall entirely (ads only) |
| Free limits | Counters per feature | **Palm scan:** after the first visit to the scanner (even without capturing), the Scan button shows an **"AD"** tag, meaning a rewarded ad per scan. **Horoscope:** 1 free, then "Test again · AD". **Tarot:** the 2nd reading triggered a **rewarded pod of up to 3 video ads** ("Ad 1 of 3 · Reward in 38s"). **Chat:** no quota hit after 6 messages |
| Chat interstitial | `chat_inter_every` = N | **N = 3.** A full-screen interstitial fires as message 3 and message 6 are sent |
| Love test | Deterministic, order-independent (our design) | **Confirmed in the original:** Alex+Sam = 80% "A beautiful match" twice, Sam+Alex = 80%, and changing genders didn't change it. The score depends only on the names |
| Moon sign | Derived from the Vedic chart | **Inconsistent in the original:** the Moon Sign screen says **Capricorn** (tropical), the Vedic chart says Moon in **Sagittarius 12°46′** (sidereal Lahiri). The rebuild must label the zodiac system per screen or use one system |
| Soul energy | Locked premium sections | **All 7 sections free** (accordion, one open at a time). Sign = Vedic sun sign (Sagittarius, "16 Dec – 13 Jan") |
| Moon sign sections | 6 sections, some locked | **5 sections, all free:** Emotional Nature, Emotional Needs, Instinctive Reactions, Relationship Style, Inner Personality |
| Vedic chart | Full feature | Confirmed and fully free: style picker (North India / South India / Western) → **Generate** → chart, "Current Dasha: Moon" (Mahadasha only), and a 10-row planet table (Lagna + 9 grahas, degree and sign). Values are correct for sidereal |

### 16.2 Verified flows and screens

- **Onboarding:** Language list (native ad) → 3 intro pages → **interstitial** → Name + gender (Male / Female / Other) → DOB wheel + birth-time wheel → **Start** → Home with a **"What's new"** bottom sheet (Ask AI, today's daily report preview, Vedic chart). The sheet **re-appears on most returns to home**.
- **Home** (top to bottom): greeting "Good evening, Alex", title "Palmistry Reader", menu icon; hero card "AI Chat Palm · Ask now"; native ad; "Explore your destiny" grid: Hand Analysis, Tarot, Astrology (NEW), Moon Sign (NEW), Daily report, Horoscopes, Love test.
- **Settings** (from the home menu): Language, Rate, Share, Privacy Policy, Terms of Service, Open source licenses, Version 1.0.8. No premium or restore entry.
- **Hand Analysis:** Hand selection (left/right) with a "Photo Requirement" standard example and 4 bad-example chips (Incomplete, Tighten, Bending, Jewelry) → Scan → camera permission → scanner with frame, a "Place your palm inside the frame" hint, and Gallery / shutter / Flash buttons. The shutter is greyed out until a palm is detected.
- **Tarot:** topic grid (Luck, Financial, Health, Love, and 2 more below) → face-down card fan → card reveal (e.g. "The Moon", "Two of Wands") → **"Read the explanation"** shows a **full-screen interstitial** before the text.
- **Horoscopes:** 12-sign grid with date ranges, select then **Check now** (banner ad). Result: date, "Character trait", body text, **Test again · AD**, report flag, home icon.
- **Love test:** name and gender per person (your name prefilled) → result with a heart %, band title, 1-line description, report flag, home icon, and a **collapsible native ad** covering the lower half.
- **Daily report:** "Today", 3 lucky numbers, 2 lucky colours, Strength (6 traits) and Weakness (6 traits) from the **sun sign** (Capricorn), and a Share tip. Stable across the session.
- **AI chat:** loading "…" bubble → greeting naming the user and persona ("Hello Alex! I am Agastya…") → "Read your palm instantly · Scan my palm" card → suggested-question carousel → replies within about 3 s, rendered as a whole message (no visible streaming at adb sampling), each followed by **3 follow-up chips**. The first answer also shows an **"Agastya wants to read your palm · SCAN"** card. Answers hedge ("While I cannot predict…"). The history icon opens **Chat history** with "Conversation 1", the last message preview and a timestamp.

### 16.3 Ad behaviour observed (counted)

| Where | Format | Network seen |
|---|---|---|
| Every cold start | Full-screen app-open/splash, 10 s countdown then Close or Skip | AdMob |
| Language screen, home, onboarding | Native card | AdMob |
| After onboarding | Interstitial | AdMob |
| Opening Hand Analysis | Interstitial pod "Ad 1 of 2 / 2 of 2" | Meta Audience Network |
| **Almost every Back navigation** | Interstitial | Meta, AdMob or AppLovin |
| Tarot "Read the explanation" | Interstitial (video, playable) | AppLovin |
| Tarot 2nd reading | Rewarded pod, up to 3 ads | AdMob |
| Horoscope grid, Vedic chart, moon sign, soul energy | Bottom banner | AdMob, Meta |
| Love result | Collapsible native, about 40% of screen | AdMob |
| Chat | Interstitial every 3rd message | AdMob |
| Rating dialog | Shown after **every result screen** (horoscope, love, daily, soul energy) and on 3rd launch | — |

**Problems seen in the original** (the rebuild must avoid them):
1. **AppLovin full-screen ads never became closable** on this device: no close control in the accessibility tree, Back ignored, 4 out of 4 times. The app had to be force-stopped. That's a broken-ad experience and a likely cause of the "too many ads" ratings.
2. **Ads auto-opened the Google Play install sheet** with no deliberate click ("You will be taken to the website after the ad"). This is an auto-redirect ad format and a policy risk.
3. The rating prompt appears after every result, which breaks Google's in-app review guidance (don't prompt repeatedly).

**Implication for the rebuild:** the original is effectively an **ad-monetised app** in this market. Interstitials sit on nearly every navigation, rewarded ads gate every repeat use, and banners and natives fill the screens. The rebuild follows the same ads-only model (§7.3), with ad caps and none of the three problems above.

### 16.4 Still not verified

- The palm-reading result itself (needs a real palm photo; deliberately not captured).
- Chat daily quota (not hit in 6 messages).
- Tarot free count beyond 1. The order was confirmed, but the reveal ad blocked further testing.
- Notifications (the permission was never requested during the session, and the 24 h schedules were not observed).

---

## Appendix A: Evidence ledger

| Finding | Evidence | Type |
|---|---|---|
| Package, version, SDKs, permissions | XAPK `manifest.json`; REA decode `ev_3e5d75de…50ba5` (manifest) | observed |
| 36 navigation destinations | DEX class names `com.ai.palmai.domain.model.nav.Dest.Scr*` | observed |
| Domain models (palm, chat, astrology, report, horoscope) | DEX class names `com.ai.palmai.domain.model.*` | observed |
| Palm backend is Gradio, `gradio_api/call/api_palm/{eventId}` | DEX string pool | observed |
| Chat backend profile path `v1/ai_llm/profiles/personal_reading_chat` | DEX string pool | observed |
| Third-party horoscope API and Open-Meteo geocoding | DEX URL strings | observed |
| Astronomy Engine on device | `licenses_*` strings | observed |
| MediaPipe gesture/hand models, CameraX, ML Kit Translate | `assets/gesture_recognizer.task`, Firebase component registrars, native libs | observed |
| Ad networks and placements | manifest activities, `lib/arm64-v8a/*`, config-key strings | observed (keys); inferred (exact behaviour) |
| Free counters, chat interstitial cadence, notification toggles | config-key strings (`free_*`, `chat_inter_every`, `enable_noti_*`) | observed names, inferred semantics |
| Paywall plans and benefits | `paywall_*` strings | observed (out of scope) |
| Prices, ad frequencies, counter values | not in the APK (remote config and Play) | **unknown** |

**Not verified:** runtime behaviour, actual remote-config values, prices, and server response shapes beyond class names. Running the app on a device (Artemis) or configuring JADX for REA would close these gaps.
