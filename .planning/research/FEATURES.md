# Feature Research

**Domain:** AI palm-reading and astrology entertainment app (Android, free, ads-only; India / SEA / LatAm)
**Researched:** 2026-11-26
**Confidence:** MEDIUM. The PRD and the reference-app device check (HIGH, first-hand) drive most of this. External competitor evidence is thin (iOS listings and third-party review aggregators only, MEDIUM-LOW); I could not pull Google Play listings or install counts for named competitors.

Inputs: `replica/PRD.md`, `replica/features.csv`, `.planning/PROJECT.md`. Out of scope already decided and not re-argued here: paywall/IAP, lock-screen (S36), face reading, accounts, iOS.

## Feature Landscape

### Table Stakes (Users Expect These)

Missing any of these and the app feels like a broken fortune-teller or gets reported/uninstalled.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| Live camera palm scan with on-screen guidance (frame, hand-detected state, flash, gallery) | The category hook. Every palm app opens with "put your hand in the frame" | HIGH | M1. CameraX + MediaPipe Hand Landmarker; guidance states in PRD 5.1. Gallery fallback is also the camera-denied path |
| Photo-quality rejection with a reason | Users shoot bad photos; silent garbage readings read as "fake" | MEDIUM | M1. 7 reasons from PRD. Two layers: on-device (too far, back of hand, fist, motion) and LLM `verdict` (blur, jewelry, bent, tight, incomplete, no lines) |
| Reading result with Palm / Love / Career sections | Love and career are the two questions people actually want answered | MEDIUM | M1. R-PALM-4 JSON. Content is the product; tone must be specific, not horoscope-generic |
| Detected lines drawn over the user's own photo | Proof the app "saw" the palm. Without it, the reading feels like canned text | HIGH | M1. Vision-LLM polylines are approximate; budget for misaligned overlays and a "tap line card to highlight line" interaction that tolerates error. Trained model is a later milestone |
| Heart / Head / Life / Fate / Sun line cards | Standard palmistry vocabulary | LOW | M1. Cards driven by the same JSON |
| Entertainment disclaimer on every reading | Store policy plus user trust; also protects against medical/financial claims | LOW | M1. Static footer on S10 and later S19/S29 |
| Daily horoscope per zodiac sign | Present in nearly every palm/astrology app; the daily-return driver | MEDIUM | Later. Server-generated once per sign/locale/day, served as JSON (PRD 5.4) |
| Daily lucky number / colour report | Cheapest daily-habit loop; popular in India/SEA | LOW | Later. Seeded from DOB + date, offline |
| Tarot daily draw | Expected alongside astrology in this category | MEDIUM | Later. 78 cards x 6 topics bundled; content writing is the cost, not code |
| Love / name compatibility test | Viral, shareable, cheap | LOW | Later. Deterministic, order-independent hash (verified in reference app) |
| Birth chart (kundli / Vedic) with Lagna, Moon, planets, dasha | India is the primary market. A "Vedic" app without it is not credible there | HIGH | Later. Astronomy Engine + Lahiri; timezone history and geocoding are the risk |
| Local language UI (Hindi first, then bn/ta/te, es, pt-BR, id) | Target users are often not English-first; readings in the UI language | MEDIUM | M1 needs en + hi minimum plus `locale` passed to the LLM; full tier later |
| Onboarding capturing name, gender, DOB (and birth time) | Personalisation of greeting, horoscope sign and chart | LOW | M1 (S02-S05). Birth time optional with an explicit "I don't know" |
| Camera-permission, offline and server-busy states | Mid/low-end phones, flaky data in tier-2/3 | MEDIUM | M1. Not optional for the "reliable on real phones" core value |
| Saved past readings the user can reopen | Users expect to revisit "my reading" and compare hands | LOW | PRD stores readings (Room) but has **no screen** to list them. See Gaps |
| Share reading as an image card | Main organic growth channel for this category (WhatsApp in India/LatAm) | MEDIUM | M1 should-have. Render a branded card with photo, overlay and one line of copy; strip nothing personal beyond what the user chose |
| Report-this-reading control | Google Play AI-generated-content policy requires in-app flagging for AI content | LOW | Not strictly in M1 slice scope but the S10 button should ship with M1 (PRD S38). Medium confidence on exact policy scope; the policy text covers chatbots and generative content |
| Ad caps that never trap the user (closable ads, no auto-redirect) | Reference app failed here (unclosable AppLovin ads, auto-opened Play sheet); "too many ads" is the top uninstall reason | MEDIUM | Later (ads milestone) but the caps and no-ads-in-first-reading rule must be designed into M1 interfaces |
| Privacy statement and consent before photo upload | Hand image leaves the device; India DPDP Act, GDPR/UMP, Play data-safety form | LOW | M1. One-line plain-language notice at S07/S08 plus link to policy |

### Differentiators (Competitive Advantage)

Aligned with the Core Value (palm scan moment). Do not try to differentiate on everything.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Under-30-second scan-to-reading with live guidance and no paywall wall | Competitors (iOS listings observed) gate readings behind subscriptions and aggressive paywalls; "expensive subscriptions" and "misleading free advert" are the top complaint clusters in aggregator summaries | HIGH | This is the whole M1 bet. Latency budget p50 < 12 s, p95 < 30 s |
| Stable readings for the same photo (24 h image-hash cache) | A recurring complaint is "scanned the same hand twice and got different results". Determinism builds trust | LOW | PRD R-PALM-7. Cheap: hash + server cache. Pull into M1 |
| Overlay drawn on the user's own photo, tied to line cards | Most competitors show a generic hand diagram | HIGH | M1 with vision LLM; upgraded by trained model later |
| Palm reading feeds chat context ("Ask about this reading") | Turns a one-shot scan into a conversation and a retention loop | MEDIUM | Needs saved reading JSON first; chat milestone |
| Chat persona that can request a palm scan (`request_palm_scan`) | Cross-links the two heaviest modules | MEDIUM | Chat milestone; depends on palm flow being reusable as a destination |
| Vedic chart feeding moon sign and soul energy from one engine | Reference app was inconsistent (tropical Moon sign vs sidereal chart). One engine and one labelled zodiac system is a quiet trust win | MEDIUM | Astrology milestone. Label zodiac system on every screen |
| Honest notifications (only real results) and capped, quiet-hours scheduling | Direct contrast to deceptive "your palm has a secret" pushes | LOW | Retention milestone |
| Local-language readings generated natively (not translate-after) | Higher perceived accuracy for hi/es/pt users | MEDIUM | Pass `locale` to the vision prompt in M1; no extra infra |
| Offline-capable daily modules (tarot, daily report, love test, chart) | Works on bad connections, no data cost | LOW | Falls out of the bundled/seeded design |
| Richer palmistry beyond five lines (hand type/element, mounts, marriage/children lines, finger shapes) | Competing palm apps market these; cheap to add as extra fields in the LLM JSON | MEDIUM | Not in the PRD. Candidate for post-M1 once the base reading holds retention. See Gaps |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Paywall / subscription / lifetime / restore | Standard revenue lever in this category | Already decided out: ads only. Subscription cost is the top review complaint cluster; also adds receipt verification scope | Rewarded ad per extra scan; free everything else |
| Lock-screen activity and lock-screen ads | Reference app used it for re-engagement | `USE_FULL_SCREEN_INTENT` restricted on Android 14+; disruptive-ads policy risk; suspension risk | Capped honest notifications |
| Fake "found something unusual in your palm" notifications | Strong click-through | Deceptive; Play policy risk; destroys trust | Notify only when a real reading/daily item exists |
| Uninstall-intercept screen (S35) | Win-back for would-be churners | Fragile, near-policy edge, low value for the effort | Skip; use win-back notification |
| Interstitial on every Back press, rating prompt after every result | Reference app did both | Top cause of "too many ads" ratings; Google review-prompt guidance forbids repeat prompts | 1 interstitial per 90 s cap, none in first reading, rating once after a success moment |
| Rewarded-ad pods of 3 (tarot) | Higher revenue per unlock | Feels punishing in low-income markets; completion rate falls | One ad per unlock |
| Live human astrologer / paid consultation marketplace | Big in India (talk-to-astrologer apps) | Needs payments, supply, moderation; contradicts ads-only and a small team | AI persona chat only |
| Medical, legal, financial, death or pregnancy predictions | Users ask for them; they feel "powerful" | Policy and harm risk; the PRD forbids directives | Reflective, non-directive language; refuse and redirect in the system prompt |
| Face reading, accounts/login, social feed, cloud sync | "Everyone has it" | PRD non-goals; each adds backend, privacy and moderation cost | Local-only storage in v1 |
| Storing raw palm images server-side or building biometric templates | Would help model training | Privacy and legal exposure (DPDP/GDPR); contradicts the "deleted after processing" promise | Delete after job; collect training data only with explicit opt-in consent, later |
| Custom palm-segmentation model in M1 | Better overlays | Long pole (dataset, training, serving); PRD says LLM first | Vision-LLM polylines now, model after retention validates |
| Copying the reference app's persona, copy, art, endpoints, keys | Fast | Clean-room rule; legal | Own persona, own content |

## Feature Dependencies

```
Onboarding (S02-S05: language, name, gender, DOB/time)
    └──feeds──> Home greeting / zodiac sign / daily report seed / horoscope default sign

Hand select (S07) ──> Live scan (S08) ──> Generating (S09) ──> Palm result (S10)
    S08 requires: CameraX + MediaPipe + camera-permission/gallery fallback
    S09 requires: backend palm job API (upload -> job id -> SSE/poll) + Claude vision JSON
    S10 requires: R-PALM-4 JSON incl. line polylines + image_size + verdict

Palm result (saved reading, Room)
    ├──feeds──> Saved-readings list (gap) ──> Share card
    ├──feeds──> Chat context (PalmContext)  ──enhances──> Chat persona
    └──feeds──> Win-back / "reading ready" notifications

Birth place geocoder + tz database ──> Vedic chart engine (Astronomy Engine, Lahiri)
    ├──> Lagna / Moon sign / Dasha display
    ├──> Chart styles (North / South / Western)
    ├──> Moon sign profile (needs Moon sign; birth time optional, noon fallback)
    └──> Soul energy profile (needs Vedic sun sign)

Zodiac sign (from DOB) ──> Daily report seed, horoscope default, love-test blend (optional)

Free-usage counters (DataStore + server mirror for palm/chat)
    └──requires──> Ad abstraction (interface in M1, SDK later) ──> Rewarded unlock per feature

Ad SDK + UMP consent ──requires──> Ad caps rules (1 inter / 90 s, none in first reading)

Notification engine ──requires──> WorkManager + opted-in permission (Android 13+) + real content to point at

Chat quota ──requires──> Play Integrity + install id
```

### Dependency Notes

- **Palm result requires backend job API and the vision JSON contract:** the client cannot be built against a stub that differs from R-PALM-4; freeze the schema (including `verdict.reason` enum) at the start of M1.
- **Saved reading is the integration point for everything downstream:** chat, share, notifications and the history list all read it. Define the Room entity once (PRD section 8 `SavedPalmReading`/`PalmLine`).
- **Moon sign and soul energy depend on the Vedic engine, not on static content alone:** fix the zodiac-system labelling decision before writing 12x13 sections of copy.
- **Rewarded gates depend on the ad interface, not on an SDK:** M1 should ship the interface, counters and a stub that "grants" instantly, so adding AdMob/MAX later does not change call sites.
- **First-reading-no-ads rule conflicts with the free-count gate:** the first scan must be free and ad-free; the counter starts gating from scan 2.

## MVP Definition

### Launch With (Milestone 1: palm-scan vertical slice)

Already scoped in PROJECT.md. Research verdict per item and small additions:

- [ ] Onboarding S01-S05 persisted locally, with birth time optional - keeps scope; add "I don't know my birth time"
- [ ] Hand select S07 with 4 bad-example chips and a one-line photo-upload privacy notice
- [ ] Live scan S08: guidance states, gallery import, flash, permission-denied and camera-unavailable states
- [ ] Generating S09 with busy/offline/timeout (45 s) states
- [ ] Backend palm job API + Claude vision reading JSON (lines, love, career, verdict)
- [ ] Palm result S10: Palm/Love/Career tabs, overlay, line cards, invalid-photo screen with 7 reasons, disclaimer
- [ ] Image-hash result cache (R-PALM-7) - tiny, pulls trust forward; add to M1
- [ ] Reports/flag control on the reading (S38) - cheap and policy-aligned; add to M1
- [ ] Saved reading in Room, EXIF stripped, server deletes image
- [ ] Ad interface + rewarded-gate stub; no ads in a first-time user's first reading
- [ ] Home hub with "coming soon" tiles
- [ ] Share card - include only if the slice has slack; otherwise first item after M1

### Add After Validation (v1.x)

- [ ] Saved-readings list screen - trigger: readings exist in Room and users return to the app
- [ ] Share image card - trigger: first organic-sharing signal
- [ ] Daily report, love test, tarot, horoscope pipeline - trigger: M1 slice works on device (these carry D1/D7 retention)
- [ ] Vedic chart + chart styles, moon sign, soul energy - trigger: engagement milestone done
- [ ] AI chat with persona, quotas, history, TTS - trigger: palm context stored
- [ ] Ads (MAX/AdMob + UMP + caps), rating flow, notifications, localisation tier-1 - trigger per PRD phases 5-8
- [ ] Extended palmistry fields (hand type, mounts, marriage line) - trigger: retention numbers hold

### Future Consideration (v2+)

- [ ] Trained palm-line segmentation model - needs dataset and consent plan
- [ ] Other-person chart profiles, pinned quick-access notification - could-have in features.csv
- [ ] Kundli matching (Gun Milan) and panchang - high-value India feature, large content/calculation scope
- [ ] iOS - phase 2

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Live scan with guidance | HIGH | HIGH | P1 |
| Vision-LLM reading JSON + tabs | HIGH | MEDIUM | P1 |
| Line overlay on photo | HIGH | HIGH | P1 |
| Photo rejection reasons | HIGH | MEDIUM | P1 |
| Entertainment disclaimer + privacy notice | MEDIUM | LOW | P1 |
| Image-hash result cache | MEDIUM | LOW | P1 |
| Report-reading control | MEDIUM | LOW | P1 |
| Saved reading (Room) | MEDIUM | LOW | P1 |
| Saved-readings list | MEDIUM | LOW | P2 |
| Share card | HIGH | MEDIUM | P2 |
| Daily report, love test | HIGH | LOW | P2 |
| Tarot | HIGH | MEDIUM | P2 |
| Horoscope pipeline | HIGH | MEDIUM | P2 |
| Vedic chart + moon sign + soul energy | HIGH (India) | HIGH | P2 |
| AI chat | HIGH | MEDIUM | P2 |
| Ads + caps + UMP | HIGH (revenue) | MEDIUM | P2 |
| Notifications (compliant) | MEDIUM | MEDIUM | P2 |
| Extended palmistry fields | MEDIUM | MEDIUM | P3 |
| Kundli matching, panchang | HIGH (India) | HIGH | P3 |
| Trained palm model | HIGH | HIGH | P3 |

**Priority key:** P1 = M1 / must have for launch of the slice; P2 = next milestones; P3 = later.

## Competitor Feature Analysis

Evidence is limited. Observed: the reference app (first-hand on device) and iOS App Store listings plus review aggregators for the palm category (MEDIUM-LOW). No Google Play data or install counts were obtained.

| Feature | Reference app (Taqde, device-verified) | Other palm/astro apps (store listings, aggregators) | Our approach |
|---------|----------------------------------------|------------------------------------------------------|--------------|
| Palm scan | Live guidance, overlay, 3 tabs; AD tag on scan after first visit | AI scan in seconds; free tier limited, unlimited behind subscription | Same hook, no subscription; 1 free ad-free, rewarded ad for more |
| Daily horoscope / zodiac compatibility | Horoscope with "Test again · AD" | Bundled with palm reading (daily horoscope, zodiac compatibility, biorhythm) | Sign horoscope later; zodiac compat blends into love test |
| Chat | Persona chat, interstitial every 3rd message | AI astrology chat in some apps; human palmist consults in others | AI persona with palm context; no human marketplace |
| Vedic chart | Free, 3 styles, dasha | Not observed in the palm-first apps checked | Later milestone; strongest India differentiator |
| Monetisation | Ads, rewarded pods, uncapped interstitials | Subscriptions and paywalls dominate complaints | Ads-only with strict caps |
| Trust | Inconsistent Moon sign; unclosable ads | Complaints: generic/vague readings, different result on re-scan, "fake/scam", privacy concerns | Deterministic cache, honest copy, privacy notice |

## Gaps: what users of competing apps clearly expect that the PRD lacks

1. **No screen for past readings.** `SavedPalmReading` is stored but S01-S38 has no "My readings" list. Needed for returning users, comparing left/right hand, and as the entry point for share/chat. Add (P2; reopen last reading from Home in M1 as the minimum).
2. **Left vs right hand comparison / "both hands".** Palmistry convention reads both hands; PRD offers one hand per scan with no compare view. Low-cost add later.
3. **Palmistry education ("what does the heart line mean").** Line cards show only the AI interpretation. A short static glossary per line is cheap and builds credibility.
4. **Share card is "should" but undefined for other modules** (daily report, love, tarot). Viral loops live here; spec once, reuse.
5. **Consent and data-rights UX for the photo:** no "delete my readings/data" action in Settings (S32), no explicit consent copy at scan time. Add for DPDP/GDPR posture and Play data-safety honesty.
6. **Age gate:** PRD section 10 mentions a COPPA 13+ gate but no screen or flow. Fold into onboarding S04.
7. **Home-screen widget (daily lucky number/horoscope) and Android 13+ notification permission pre-prompt:** common retention levers absent from the PRD.
8. **Kundli matching (Gun Milan), panchang/auspicious times, numerology:** staples of India-market astrology apps that the PRD does not list. Flag for post-v1 scoping rather than M1.
9. **Extended palmistry content** (hand type, mounts, marriage/children lines): competing palm apps emphasise these; PRD covers only five lines. Cheap as extra JSON fields; decide after M1.
10. **Reading regeneration policy:** users will re-scan wanting a "better" answer. Cache by image hash (R-PALM-7) plus a clear message when the photo is the same, to avoid contradiction complaints.
11. **Invalid-photo recovery:** PRD lists reasons but not "retake with this tip" per reason. Map each of the 7 reasons to one corrective hint and a retake CTA; this directly protects the Core Value.

## Sources

- `E:/Palm Reading/replica/PRD.md`, `features.csv`, `.planning/PROJECT.md` (HIGH, first-party; PRD section 16 is on-device verified)
- [PalmHD: Palm Reader (App Store)](https://apps.apple.com/app/id1324828998) and related App Store palm-reader listings (MEDIUM-LOW; iOS, bundling of horoscope/compatibility, subscription tiers)
- Review aggregators [JustUseApp Palmist](https://justuseapp.com/en/app/973861216/palmist/reviews), [MWM Palmistry && Horoscope](https://mwm.ai/apps/palmistry-horoscope/1464654656), [MWM Palm Reader](https://mwm.ai/it/apps/palm-reader-palmistry-life/1468487172) (LOW; automated sentiment: expensive subscriptions, misleading free ads, vague/generic and inconsistent readings, privacy concerns)
- [Google Play AI-Generated Content policy](https://support.google.com/googleplay/android-developer/answer/14094294) (MEDIUM-HIGH; in-app reporting/flagging requirement for AI-generated content)

---
*Feature research for: AI palm-reading and astrology Android app*
*Researched: 2026-11-26*
