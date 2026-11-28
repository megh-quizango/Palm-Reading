---
phase: "5"
slug: "result-against-the-fixture"
status: draft
shadcn_initialized: false
preset: none
created: "2026-11-28"
---

# Phase 5: Result Against the Fixture, UI Design Contract

> Visual and interaction contract for: the `ScanReady` read action (loading, inline error), the S10 Result (photo with the line overlay, legend, disclaimer, Palm / Love / Career tabs, line cards, Scan again / Share / Ask [Guide]), its loading / missing-photo / unreadable states, the unusable-photo screen for all 7 reasons, the branded share card image, the Home "Your last reading" card, and the debug-only Fixture lab. Written by gsd-ui-researcher, to be verified by gsd-ui-checker.

**The Figma MCP was not available in this session.** This researcher had no ToolSearch and no Figma tools, so no live `create_session`, `get_metadata`, `get_screenshot`, `get_design_context` or `get_variable_defs` call was made against file `Ej98dV7fZQUhRwUyfQIlyJ`. As in Phases 3 and 4, the Figma values were rebuilt from **our own `use_figma` build scripts of the 2026-11-26 design session**, kept in the local transcript `172eeb1d-…cb.jsonl`:
- Line 875: "3 · Screens", section "Palm reading (S08–S10)". It holds **S10 Hand result / Palm tab (Layout A)**, **Palm tab (Layout B)**, **Love tab**, **Career tab**, **Rewarded unlock** and **Invalid photo · <reason>** for all 7 reasons. (The Phase 4 UI-SPEC said the invalid-photo screens were not in Figma. They are; they were built in the same script.)
- Line 733: "2 · Components" batch 2: `Card` (Type=Line Heart…Sun, Type=Info), `Palm-line legend` (Line × Default/Selected/Dimmed).
- Line 709: `Segmented & tabs` (Type=Segmented for the palm result, Type=Tabs underline for horoscope).
- Line 1269: card restyle (Info and Line cards get `gradient.card` fill + `FoilBorder` stroke + a white sheen).
- Line 1303: "5 · Motion" rows "Palm overlay / Result reveal" and "S10 Palm result" sequence.
- Line 841: S06 Home frames. **No "last reading" card exists in Figma.** It is designed here from tokens and the FeatureCard pattern.
- Line 494: `Icon/*` paths (for the 2 new icons).
- **No share card exists in Figma.** It is designed here from tokens (D-21).

Manual Figma edits after 2026-11-26 are not captured. Every value below is mapped onto `design-system/nocturne.tokens.json` and the generated `NocturneTokens.kt`. A visual QA pass against the live frames is still owed (UI Considerations).

**Precedence:** the locked decisions D-01..D-25 in `05-CONTEXT.md` come first. Figma comes second, then the Phase 3 and 4 conventions (`03-UI-SPEC.md`, `04-UI-SPEC.md`). Every disagreement is listed in "Figma vs locked decisions".

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (shadcn does not apply: native Android, Jetpack Compose) |
| Preset | not applicable |
| Component library | In-repo "Nocturne" design system (`app.palm.core.designsystem`) on Material 3 primitives (`androidx.compose.material3` 1.4.0 via Compose BOM 2026.09.00, as pinned in `android/gradle/libs.versions.toml`) |
| Icon library | Custom vector drawables from Figma `Icon/*` (24 dp grid, 1.8 dp stroke, round caps and joins, `strokeColor` `@color/nocturne_text_primary`, tinted at use). Same format as `ic_alert.xml`. No `material-icons-extended` |
| Font | Cormorant Garamond SemiBold 600 (display), Manrope 500/600 (UI), Noto Devanagari fallback (`NocturneFonts`). All bundled |
| Tokens | `NocturneTokens.kt`, generated from `nocturne.tokens.json`. **Phase 5 adds no token and changes no token.** Alphas are applied with `.copy(alpha = …)` on token colours |
| Background | `NocturneBackground` (animated sky) **shows behind Result and InvalidPhoto** (both routes keep the default `showsBackground = true`, D-07, D-14). The share card draws its own static `NocturneBackground(animate = false)` |

---

## Component Inventory

Enumerated by `rg -U "@Composable\s*\n\s*fun \w+" android/app/src/main/java/app/palm/core/designsystem` — 13 public composables — app.palm.core.designsystem@90d5499 (in-repo, git HEAD) — 2026-11-28.
Material 3 primitives: resolved by `curl -s https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom | grep -A1 '<artifactId>material3</artifactId>'` — androidx.compose.material3:material3@1.4.0 — 2026-11-28. Any M3 1.4.0 composable may be used (Phase 5 needs `PrimaryTabRow`, `Tab`, `CircularProgressIndicator`, `Icon`, `Text`).
Filesystem cross-check: `ls android/app/src/main/java/app/palm/core/designsystem/components/*.kt | wc -l` = 8 files (Buttons, Chips, ChoiceTile, Dialog, FeatureCard, StateBlock, TextFields, TopBar). They hold 11 of the 13 composables; the other 2 are `NocturneTheme` and `NocturneBackground`. No mismatch.

This table is a **non-exhaustive** list of known-good components, not a closed allowlist.

### Existing (reuse as-is)

| Component | Import path | Phase 5 use |
|-----------|-------------|-------------|
| `NocturneTheme`, `LocalNocturne` (`type`, `reduceMotion`, `durations`, `easings`) | `app.palm.core.designsystem` | All text styles; every reduced-motion branch |
| `NocturneButton` + `NocturneButtonStyle` (Primary, Secondary, Ghost) | `…designsystem.components` | ScanReady read, Result actions, InvalidPhoto, missing state, lab. No change (no icon slot is added; see Figma table) |
| `NocturneChip` (`dotColor`, `textColor`) | `…designsystem.components` | Legend chips (with dot), line-card badges, traits and industries chips (no dot). No change |
| `NocturneTopBar` | `…designsystem.components` | Result ("Your palm reading"), InvalidPhoto ("Photo not accepted"), lab. No change |
| `NocturneStateBlock` | `…designsystem.components` | InvalidPhoto (7 reasons), Result "couldn't open". No change |
| `NocturneDialog` | `…designsystem.components` | Lab "Delete all readings?" confirmation only |
| `NocturneChoiceTile` | `…designsystem.components` | Lab aspect and variant pickers |
| `NocturneBackground(animate = false)` | `app.palm.core.designsystem` | Share card backdrop (static, no clock) |
| `nocturneBrush`, `NocturneGradientAngles`, `rasterizedBehind`, `nocturneGlow`, `NocturneBorderWidth` | `app.palm.core.designsystem` (Effects.kt) | Card fill + foil border; selected tab pill |
| `ComingSoonSheet` via `ComingSoon(Feature.ASK_GUIDE)` | `app.palm.feature.home` | "Ask [Guide]" from Result (D-20). No change |
| `FeatureCard`, `HeroCard`, `AdChip`, text fields | — | Not used by Phase 5 screens (Home grid unchanged; no ads) |

### New (Phase 5 builds these)

| Component | Location | Contract |
|-----------|----------|----------|
| `NocturneCard(modifier, onClick = null, content: ColumnScope.() -> Unit)` | `core/designsystem/components/Card.kt` | Figma `Card` Type=Info/Line after the 1269 restyle: `RoundedCornerShape(NocturneRadius.Lg)` (24 dp), clip, `rasterizedBehind { drawRect(nocturneBrush(NocturneGradients.Card, NocturneGradientAngles.Card)) }`, 1 dp border with `nocturneBrush(NocturneGradients.FoilBorder, NocturneGradientAngles.FoilBorder)`, inner padding 16 dp, `verticalArrangement = spacedBy(8 dp)`. When `onClick` is set: `clickable(role = Role.Button)`. No glow, no sheen (see Figma table). Used by Result info/line cards and the Home last-reading card |
| `PalmPhoto(photo: ImageBitmap?, state, lines, animate, modifier)` | `feature/result/PalmPhoto.kt` | Photo box + overlay + loading/unavailable placeholders (S10 section "Photo and overlay"). Shared by Result and ShareCard |
| `PalmLinesOverlay` (draw code) + pure `fitRect`, `mapPoint`, `catmullRomToBezier` | `feature/result/` (pure functions in a non-Compose file) | D-09, D-10 maths; unit-tested (D-24) |
| `LineLegend(lines, modifier)` | `feature/result/` | `FlowRow` of `NocturneChip(dotColor)` + approximate note (D-11) |
| `ResultTabs(selected, onSelect)` | `feature/result/` | M3 `PrimaryTabRow` restyled as the Figma segmented control (D-08) |
| `LineCard`, `InfoCard` | `feature/result/` | Built on `NocturneCard` |
| `ShareCard(reading, photo)` | `feature/result/share/` | Fixed 1080 × 1350 px image (D-21) |
| `LastReadingCard(...)` | `feature/home/` | Home entry (D-19), built on `NocturneCard` |
| `InvalidPhotoScreen` | `feature/invalidphoto/` | `NocturneTopBar` + `NocturneStateBlock` (D-14) |
| `FixtureLabScreen` | `src/debug/…` | Debug-only (D-05, D-06) |

---

## Spacing Scale

From `NocturneSpace`. **Phase 5 uses only 4, 8, 16, 24, 32 and 48 dp for padding, gaps and insets.** Figma's 6, 10, 12 and 14 dp gaps are rounded onto the scale (6 → 8, 10/12/14 → 16 or 8 as listed). `S2`, `S12` and `S20` exist as tokens but Phase 5 code must not use them.

| Token | Value | Usage in Phase 5 |
|-------|-------|------------------|
| S4 | 4 dp | Legend `FlowRow` vertical gap; line-card title-row vertical wrap gap; inset of the selected-tab pill inside its 48 dp tab |
| S8 | 8 dp | Card inner gaps (overline → body, title row → interpretation, body → chips); chip gaps; gap between stacked action buttons; photo → legend; legend → approximate note; title → body in the state block (existing); ScanReady inline error → button |
| S16 | 16 dp | Screen side padding; card inner padding; gap between cards; legend/note → disclaimer; disclaimer → tabs; tabs → first card; Home hero → last-reading card; thumbnail → text in the Home card; bottom padding above the navigation-bar inset |
| S24 | 24 dp | Gap between the last card and the action column; share card outer padding; state block internal gaps (existing); Result content top padding under the top bar |
| S32 | 32 dp | Space after the action column before the end of the scroll (on top of the 16 dp inset), so the last button never sits on the gesture bar |
| S48 | 48 dp | Minimum touch target (`NocturneSize.TouchMin`): tabs, back button, Home card min height floor |

**Component and art sizes (dimensions, not spacing; all multiples of 4):**
- Top bar 64 dp with a 48 dp back button (existing). Buttons 52 dp tall (existing). Tabs 48 dp tall; selected pill 40 dp (48 minus 4 dp inset each side).
- Photo box: full content width, height capped at 55% of the window height (D-09); radius 24 (`Lg`); 1 dp hairline border.
- Line dot in line cards: 12 dp circle (Figma "Line colour" 12). Legend dot: 8 dp (existing `NocturneChip` dot).
- Overlay strokes: line 3 dp, halo 9 dp (D-10; colour/alpha in Color).
- Home thumbnail 56 dp square, radius 16 (`Md`); arrow disc 32 dp with a 16 dp `ic_arrow` (FeatureCard pattern).
- State-block disc 96 dp with a 40 dp icon (existing). Spinners 48 dp.
- Share card canvas: 432 × 540 dp at density 2.5 = 1080 × 1350 px (see Share card).

---

## Typography

All styles come from `LocalNocturne.current.type`. Never set `fontSize` or `fontWeight` inline. **Phase 5 renders 3 sizes (26, 15, 13 sp) and 2 weights (500, 600).** No new styles. `Body M` (14/400), `Display XL/L/S` are not used.

| Size | Token | Size / line height | Weight | Used for |
|------|-------|--------------------|--------|----------|
| 26 | Display M | 26 / 32 sp | 600 (Cormorant) | Top-bar titles; "Your lines" section heading on the Palm tab; state-block titles (reason titles, "We couldn't open this reading"); share card wordmark and "My palm reading" |
| 15 | Title M | 15 / 22 sp | 600 (Manrope) | Line-card titles (server `label`); Home card title "Palm reading" |
| 15 | Body L | 15 / 22 sp | 500 (Manrope) | Info-card bodies (`text.primary`); line-card interpretations (`text.muted`); state-block bodies; share card summary |
| 15 | Label L | 15 / 20 sp | 600 (Manrope) | `NocturneButton` labels (existing) |
| 13 | Label M | 13 / 18 sp | 600 (Manrope) | Tab labels; legend chips; badges; traits / industries chips (all via `NocturneChip` or the tab) |
| 13 | Overline | 13 / 18 sp, 0.12 em | 600 (Manrope) | Info-card section labels ("Overview", "How you love", …) and the Home card overline "Your last reading", in `accent.gold`. Use `.devanagariSafe()` exactly as Home does |
| 13 | Caption | 13 / 18 sp | 500 (Manrope) | Disclaimer; approximate note; "no lines drawn" note; Home card meta (hand · date); ScanReady inline error; "Photo unavailable"; share card disclaimer, traits line and footer |

Rules:
- **Devanagari:** the `hi` locale already swaps the theme to `devanagariSafe()` (font-metric line height, 0 letter spacing). Text containers use `heightIn(min = …)` only, never a fixed height. **No `maxLines` anywhere on the Result, InvalidPhoto, ScanReady or Home screens**: reading text has no length limit in the contract, so it wraps and the page scrolls. The **only** truncation in Phase 5 is on the share card (fixed image), listed there.
- Reading text (summary, traits, interpretations, love/career bodies, server `label`) is rendered exactly as received, in the reading's own locale (D-23). App strings follow the app language.

---

## Color

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `gradient.sky` (#1A1240 → #0B0820 → #07051A) + stars, via the root `NocturneBackground` | Result and InvalidPhoto backdrop; share card backdrop (static) |
| Secondary (30%) | `gradient.card` (#2A2066 → #150F38, 135°) + `gradient.foil-border` 1 dp (`NocturneCard`); `surface.card-hi` #2A2160 (chips, badges, tab-row container at @0.8 of `surface.card`); the user's photo; `bg.deep` #07051A (photo letterbox bands) | Info / line / Home cards, chips, tab row, photo box |
| Accent (10%) | `accent.gold` #F2C66D | Only the elements listed below |
| Brand / interactive | `gradient.primary` (#5B3FE6 → #7A4EF2, 90°) + `GlowPrimary` | Primary button ("Scan again", "Read my palm", "Retake photo", "Back to Home" in the missing state); the **selected tab pill**. `brand.link` #A99BFF for spinners and the "Approximate" badge text |
| Palm lines | `palm.heart` #FF7A9A, `palm.head` #7CC4FF, `palm.life` #6EE7B7, `palm.fate` #F2C66D, `palm.sun` #FFA94D | **Reserved** for the overlay strokes, the legend dots and the line-card dots. Never for text or buttons |
| Status | `status.error` #FF8A8A; `status.warning` #FFD27A | Error: InvalidPhoto disc icon and border (Figma reason card tone), ScanReady inline error text, share-failure text. Warning: not used |
| Destructive | `status.error` #FF8A8A | No destructive button for users in Phase 5 (no delete UI, D-18). The debug lab's "Delete" dialog button uses the existing destructive dialog style |

**Accent (gold) reserved for:** (1) Info-card overlines (section labels) on the Result tabs; (2) the Home card overline "Your last reading"; (3) the Home card arrow-disc icon (same as `FeatureCard`); (4) on the share card only: the traits line and the footer "Read your palm in [AppName]"; (5) `focus.ring` for keyboard/D-pad focus. `palm.fate` happens to equal gold; it stays a line colour and is always paired with its "Fate" text label.

**Overlay halo (tuning inside D-10).** D-10 asks for "a soft glow (same path, about 9 dp, about 30% alpha) so lines read on light and dark skin". A glow in the line's own colour does not help on light skin, and 30% of a dark colour is too weak. The halo is therefore **`bg.deep` #07051A at 0.55 alpha, 9 dp, round caps/joins, drawn under each 3 dp line**. Measured (WCAG, non-text target 3:1):

| Line on … | raw skin | halo @0.30 | halo @0.55 (chosen) |
|-----------|----------|------------|----------------------|
| light skin #E8C9B0 | 1.03–1.58 | 1.25–2.03 | 2.53 (heart) / 3.28–4.10 (others) |
| mid skin #B98A6A | 1.23–1.99 | 2.21–3.58 | 3.81–6.18 |
| dark skin #5A4650 | 3.50–5.68 | 4.84–7.85 | 6.15–9.97 |

Heart on light skin stays below 3:1; the text legend carries the meaning (colour is never the only cue, D-11), and the on-device check is a backstop row. If the planner prefers the literal 30%, only the alpha constant changes.

**Contrast (WCAG 2.x, computed 2026-11-28):**

| Pair | Ratio | Verdict |
|------|-------|---------|
| text.muted on sky top #1A1240 / bg.base (disclaimer, notes) | 8.19 / 9.22 | pass (D-13 "well above 4.5:1") |
| text.primary on gradient.card ends (#2A2066 / #150F38) | 12.6 / ≥ 15 | pass |
| text.muted on gradient.card ends (interpretations) | 6.59 / 8.52 | pass |
| accent.gold on gradient.card #2A2066 (overlines) | 8.73 | pass |
| text.primary on surface.card-hi (legend chips) | 12.71 | pass |
| brand.link on surface.card-hi ("Approximate" badge) | 5.94 | pass |
| text.muted on surface.card-hi ("Not clearly visible", traits chips) | 6.64 | pass |
| white on gradient.primary ends (Primary buttons, selected tab) | 6.37 / 5.00 | pass |
| text.muted on surface.card @0.8 over sky (unselected tabs) | ≥ 7.8 | pass |
| palm.* dots on surface.card-hi / surface.card (non-text) | 5.73–9.28 / 6.80–11.02 | pass (≥ 3:1) |
| status.error on bg.base (inline errors, invalid disc icon) | 8.65 | pass |
| gradient.card vs surface.card-hi chip fill | 1.01 | Chips inside cards read by their text only; no state relies on the chip fill |

---

## Screens

### 1. ScanReady (existing screen, read action, D-03)

Layout unchanged from Phase 4 (top bar, photo with landmark dots, source line, meta line, two bottom buttons). Changes:

| State | Primary button | Retake button | Extra |
|-------|----------------|---------------|-------|
| Idle | "Read my palm" **enabled** (Primary) | "Retake photo" (Ghost) enabled | — |
| Reading (read + save running) | label "Reading your palm…", `enabled = false` | `enabled = false` | System Back is ignored while reading (the VM finishes or fails within the fixture call); the VM ignores a second read call |
| Error (repository threw) | label "Try again", enabled | enabled | A Caption line in `status.error`, start-aligned, 8 dp above the primary button: "Something went wrong while reading. Please try again." Cleared when "Try again" is tapped. `liveRegion = Polite` so TalkBack announces it once |
| Valid verdict | — | — | Save, then `replaceAll` to `[Home, Result(id)]` (no visual of its own) |
| Invalid verdict | — | — | Pop ScanReady, push `InvalidPhoto(reason, hand)` |

No artificial delay and no Generating screen (Phase 6). The disabled look is the existing 0.38 alpha.

### 2. Result (S10), route `Result(readingId)`, sky on

**Structure (D-07), one `verticalScroll` column, `windowInsetsPadding(safeDrawing)`, 16 dp side padding:**

```
NocturneTopBar("Your palm reading", back → pop to Home)       64 dp
  24 dp
PalmPhoto (full width, aspect = photo, max 55% window height)
  8 dp
LineLegend (FlowRow, only drawn lines)            [hidden if 0 drawn]
  8 dp
Approximate note (Caption, muted)                 [only if any drawn line is llm/template]
  -- or, when 0 lines are drawn and the photo exists: "no lines drawn" note (Caption, muted)
  16 dp
Disclaimer (Caption, text.muted, start-aligned)   [always]
  16 dp
ResultTabs (Palm | Love | Career)                 48 dp
  16 dp
Tab content (cards, 16 dp apart)
  24 dp
Action column: Scan again (Primary) / Share (Secondary) / Ask [Guide] (Ghost), full width, 8 dp apart
  [share error caption, 8 dp below, only after a failure]
  32 dp + navigation-bar inset
```

The photo, legend, note and disclaimer sit above the tabs, so they show on every tab (D-07, D-13).

**Photo and overlay (`PalmPhoto`, D-09, D-10, D-12):**
- Box: `fillMaxWidth()`, `heightIn(max = 0.55 × window height)`, `aspectRatio(photo.width / photo.height)` (before decode: `image_size.w / image_size.h`), clip `RoundedCornerShape(24 dp)`, 1 dp `border.hairline` @0.35, background `bg.deep` (visible only as letterbox bands when the height cap applies).
- Image: `ContentScale.Fit`. The drawn image rect comes from the pure `fitRect(srcW, srcH, dstW, dstH)`; a trace point `[x, y]` (clamped to 0..1) maps to `rect.left + x·rect.width, rect.top + y·rect.height`.
- A line is drawn iff `trace_source != none` and the trace has ≥ 2 points. Path: Catmull-Rom through the points as cubic Béziers (2 points = straight segment). Each line is drawn twice: halo (9 dp, `bg.deep` @0.55) then the line (3 dp, its `palm.*` colour), both `StrokeCap.Round`, `StrokeJoin.Round`, solid (no dash). Draw order: heart, head, life, fate, sun (contract order).
- States:

| State | Box content | Legend / note |
|-------|-------------|---------------|
| Loading (bitmap decoding) | `CircularProgressIndicator` 48 dp, `brand.link`, centred | hidden |
| Loaded, ≥ 1 line drawn | photo + overlay | legend + approximate note (if any llm/template) |
| Loaded, 0 lines drawn (all `none`, or `present=false`) | photo only | legend hidden; note "We couldn't trace your lines clearly on this photo." |
| Photo missing or undecodable (D-12) | `ic_gallery` 40 dp in `text.muted` above Caption "Photo unavailable" (`text.muted`), centred, at the `image_size` aspect | legend and both notes hidden |

**Legend (D-11):** `FlowRow(horizontalArrangement = spacedBy(8 dp), verticalArrangement = spacedBy(4 dp))` of `NocturneChip(text = localised line name, dotColor = palm colour)`, one per drawn line, in contract order. Non-interactive (no Selected/Dimmed state; see Figma table).

**Tabs (D-08):** M3 `PrimaryTabRow(selectedTabIndex)` restyled as the Figma segmented control:
- Row container: `fillMaxWidth()`, `containerColor = Color.Transparent`, `Modifier.background(surface.card @0.8, pill).border(1 dp, border.hairline @0.3, pill)`, `divider = {}`, `indicator = {}` (M3 draws the indicator above the tabs, which would cover the labels, so the selected look is drawn inside each tab instead).
- Each `Tab`: height 48 dp; inner `Box(Modifier.padding(4 dp).fillMaxSize().background(if selected gradient.primary else transparent, pill))`; label Label M, `text.on-primary` when selected, `text.muted` otherwise; `selectedContentColor` / `unselectedContentColor` set to the same.
- Selected index in `rememberSaveable` (survives rotation and process death). No swipe. Switching tabs does not scroll the page and does not replay the overlay animation (the photo is above the tabs).

**Palm tab:**
1. `InfoCard`: overline "Overview" (gold Overline), `palm.summary` (Body L, `text.primary`), then a `FlowRow` (8 dp gaps) of `NocturneChip(trait, textColor = text.muted)` for each trait. Chip row hidden when `traits` is empty.
2. Heading "Your lines" (Display M, `heading()` semantics), hidden when the reading has no lines.
3. One `LineCard` per line, in the order Heart, Head, Life, Fate, Sun (types not in the reading are skipped).

**`LineCard`** (`NocturneCard`):
- Title row: `FlowRow(spacedBy(8 dp) horizontal, spacedBy(4 dp) vertical, itemVerticalAlignment = CenterVertically)` holding `Row(12 dp dot in the line colour, 8 dp, title)` and the badge. The badge wraps under the title at large font scales.
- Title: server `label` (Title M, `text.primary`); if blank, the localised full name ("Heart line"…).
- Badge (`NocturneChip`, no dot): `trace_source` `llm` or `template` **and drawn** → "Approximate" (`brand.link` text); not drawn (`none`, or `present=false`, or < 2 points) → "Not clearly visible" (`text.muted` text). Every card has exactly one badge in M1.
- Interpretation: Body L, `text.muted`. Hidden if blank.
- The dot keeps its colour even when the line is not drawn (it identifies the line, and the badge says it is not drawn).

**Love tab:** three `InfoCard`s: "How you love" (`love.how_you_love`), "Ideal partner" (`love.ideal_partner`), "Challenges" (`love.challenges`).

**Career tab:** `InfoCard` "Strengths" (`career.strengths`), `InfoCard` "Challenges" (`career.challenges`), `InfoCard` "Ideal industries" with a `FlowRow` of `NocturneChip` (no body text; card hidden when the list is empty).

**Actions (D-20):** full-width column, always stacked (fits 360 dp at font scale 2.0 and in Hindi):
- "Scan again": Primary → push `HandSelect` (no AD chip in Phase 5).
- "Share": Secondary → share flow below. While the image is being made: label "Preparing…", `enabled = false`. On failure: Caption in `status.error` under the column, "Couldn't create the share image. Please try again.", `liveRegion = Polite`, cleared on the next tap.
- "Ask [Guide]": Ghost → `ComingSoon(Feature.ASK_GUIDE)` bottom sheet (existing art, "Coming soon" overline, title, body, OK).

**Whole-screen states:**

| State | What shows |
|-------|-----------|
| Loading (Room row not yet emitted) | Top bar + `CircularProgressIndicator` 48 dp `brand.link`, centred in the remaining space. No tabs, no actions |
| Found | The structure above |
| Photo missing / undecodable | The structure above with the photo placeholder (D-12); tabs, cards and actions work; Share still works (the card shows the placeholder in the photo slot) |
| Row missing, JSON fails strict decode, or `verdict.ok = false` / null sections | Top bar + `NocturneStateBlock(ic_alert, tone brand.link, "We couldn't open this reading", "It may have been removed from this phone. Start a new scan from Home.", primary "Back to Home")`. Back to Home pops to the Home entry. No crash |

### 3. Unusable photo, route `InvalidPhoto(reason, hand)`, sky on (D-14, D-15)

```
NocturneTopBar("Photo not accepted", back → same as Retake)
  scrollable, vertically centred:
  NocturneStateBlock(icon = reason icon, tone = status.error,
                     title = reason title, body = reason tip,
                     primary = "Retake photo" → pop to Scan(hand), capture cleared,
                     secondary = "Back to Home" (Ghost) → pop to Home, capture cleared)
```

System Back = Retake (lands on `Scan(hand)`, D-03). Nothing is saved; the cached capture is deleted when the user leaves (D-15).

| `reason` | Icon (24-grid, tinted `status.error`) | Title (en) | Tip (en) |
|----------|------|------------|----------|
| `blurry_or_dark` | `ic_flash` (exists) | Your photo is too blurry or dark | Hold the phone steady in good light and let it focus. |
| `too_far` | `ic_search` (exists) | Your hand is too far away | Move closer so your palm fills the frame. |
| `jewelry` | `ic_star` (**new**, Figma `star`) | Jewellery is covering your palm | Take off rings and bracelets, then try again. |
| `bent` | `ic_hand` (exists) | Your hand is bent | Hold your hand flat and facing the camera. |
| `fingers_tight` | `ic_hand` (exists) | Your fingers are too close together | Spread your fingers a little apart. |
| `incomplete` | `ic_gallery` (exists) | Part of your palm is missing | Fit your whole palm, wrist to fingertips, in the frame. |
| `no_palm_lines` | `ic_eye` (**new**, Figma `eye`) | We couldn't see your palm lines | Use brighter light and keep your palm open and flat. |

The reason → (icon, title, tip) mapping is one exhaustive `when` (unit-tested for all 7, D-14). Icons follow the Figma invalid-photo frames.

### 4. Share card (D-21, D-22)

A `ShareCard` composable rendered off-screen to a bitmap; never visible in the UI and excluded from semantics (`clearAndSetSemantics {}`).

- **Canvas:** `CompositionLocalProvider(LocalDensity provides Density(density = 2.5f, fontScale = 1f))`, `Modifier.requiredSize(432.dp, 540.dp)` = exactly **1080 × 1350 px**. Font scale is fixed at 1.0 because the card is an image, not UI.
- **Backdrop:** `NocturneBackground(animate = false)` (static sky and stars), full bleed.
- **Layout** (24 dp padding, content width 384 dp):

```
y 24   Header row, 40 dp: illus_guide_orb 40 dp, 8 dp, "[AppName]" (Display M, text.primary)
       16 dp
y 80   Body row, 264 dp:
         Photo box 176 × 264 dp (2:3), radius 16, bg.deep letterbox, photo Fit + same overlay code (static, no animation)
         16 dp
         Right column (192 dp): "My palm reading" (Display M, max 2 lines),
                                8 dp, legend chips stacked vertically (4 dp gaps, only drawn lines),
                                8 dp, approximate note (Caption, muted, max 2 lines) if any llm/template line
       16 dp
y 360  Remaining column (fills to the bottom padding):
         palm.summary (Body L, text.primary, weight(1f), max 3 lines, ellipsis)  — absorbs slack
         8 dp, traits joined with " · " (Caption, accent.gold, 1 line, ellipsis), up to 3 traits
         16 dp, disclaimer (Caption, text.muted, max 3 lines, ellipsis)
         8 dp, "Read your palm in [AppName]" (Caption, accent.gold, 1 line)
y 516  24 dp bottom padding
```

- Photo missing: the photo box shows the same "Photo unavailable" placeholder; the right column keeps the title, legend hidden.
- These are the **only** `maxLines` in Phase 5 (fixed-size image). The disclaimer must never be cut entirely: at most 3 lines with ellipsis, and the summary yields space first.
- **Output:** `Bitmap.compress(PNG)` to `cacheDir/share/reading.png`, FileProvider `${applicationId}.share`, `ACTION_SEND`, `image/png`, `EXTRA_STREAM` + `ClipData`, `FLAG_GRANT_READ_URI_PERMISSION`, `EXTRA_TEXT` = "My palm reading from [AppName]", via `Intent.createChooser(…, "Share your reading")`.

### 5. Home "Your last reading" (D-19)

Inserted under the hero card, 16 dp gap; the "Explore your destiny" header keeps its 24 dp gap below it. Hidden entirely when `latest()` is null (no empty-state card; the Palm tile and hero CTA already start a scan).

```
NocturneCard(onClick → push Result(id)), min height 88 dp
  Row(verticalAlignment = Center, spacedBy 16 dp):
    Thumbnail 56 × 56 dp, radius 16, ContentScale.Crop, decoded with inSampleSize (no overlay)
      placeholder while decoding / missing: surface.card-hi box with ic_hand 24 dp text.muted
    Column(weight 1, spacedBy 4 dp):
      "Your last reading"     Overline, accent.gold
      "Palm reading"          Title M, text.primary
      "<Right hand> · <date>" Caption, text.muted   (date: DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) in the app locale, java.time)
    Arrow disc 32 dp (surface.card-hi, gold→link stroke), ic_arrow 16 dp accent.gold  — decorative
```

The card joins Home's existing entry stagger with `index = 1` (same slot as the hero), so `ENTRY_END_MS` does not change. It updates live from Room (`latest()` Flow): after a save, returning to Home shows the new reading.

### 6. Fixture lab (debug only, D-05, D-06)

Not user-facing; English only, strings hardcoded in `src/debug` (like the Showcase). Opened from the Showcase.
- `NocturneTopBar("Fixture lab")`, scrolling column, 16 dp padding, 16 dp gaps.
- "Photo aspect": three `NocturneChoiceTile`s (3:4, 1:1, 9:16).
- "Variant": 13 `NocturneChoiceTile`s in the D-06 order (Valid · mixed sources, Valid · all llm, Valid · all template, Valid · all lines none, Valid · present but not drawn, Valid · Hindi, Invalid · each of the 7 reasons by code). The selection is the variant used by the next read (lab or real scan).
- Primary "Open ScanReady"; Ghost "Delete all readings" → `NocturneDialog` "Delete all readings?" / "Removes every saved reading and its photo on this phone." [Cancel] [Delete].

---

## Copywriting Contract

All new keys in `res/values/strings_result.xml` and `res/values-hi/strings_result.xml` (D-23), except the ScanReady and Home keys, which go in the existing `strings_scan.xml` / `strings_home.xml`. `[AppName]` and `[Guide]` stay literal placeholders. Hindi is Claude-written (needs native review, as in Phases 3-4).

| Element | Key | English | Hindi |
|---------|-----|---------|-------|
| Primary CTA (ScanReady) | `scan_ready_read` (**replace** the "(next update)" text) | Read my palm | मेरी हथेली पढ़ें |
| ScanReady reading | `scan_ready_reading` | Reading your palm… | आपकी हथेली पढ़ी जा रही है… |
| ScanReady error | `scan_ready_error` | Something went wrong while reading. Please try again. | पढ़ते समय कुछ गड़बड़ हो गई। कृपया फिर कोशिश करें। |
| ScanReady retry | `scan_ready_retry` | Try again | फिर कोशिश करें |
| Result title | `result_title` | Your palm reading | आपकी हस्तरेखा रीडिंग |
| Tabs | `result_tab_palm` / `_love` / `_career` | Palm / Love / Career | हथेली / प्रेम / करियर |
| Line names (legend) | `result_line_heart` … `_sun` | Heart / Head / Life / Fate / Sun | हृदय / मस्तिष्क / जीवन / भाग्य / सूर्य |
| Line full names (blank-label fallback) | `result_line_heart_full` … `_sun_full` | Heart line / Head line / Life line / Fate line / Sun line | हृदय रेखा / मस्तिष्क रेखा / जीवन रेखा / भाग्य रेखा / सूर्य रेखा |
| Badge, drawn approximately | `result_badge_approx` | Approximate | अनुमानित |
| Badge, not drawn | `result_badge_not_visible` | Not clearly visible | साफ़ नहीं दिखी |
| Approximate note | `result_legend_note` | Lines are drawn approximately. | रेखाएँ अनुमान से बनाई गई हैं। |
| Empty overlay note (0 lines drawn) | `result_no_lines_drawn` | We couldn't trace your lines clearly on this photo. | इस फ़ोटो पर हम आपकी रेखाएँ साफ़ नहीं बना पाए। |
| Disclaimer (D-13) | `result_disclaimer` | For entertainment only. Palm reading is not a prediction and is not medical, legal or financial advice. | केवल मनोरंजन के लिए। हस्तरेखा रीडिंग कोई भविष्यवाणी नहीं है, और न ही चिकित्सा, कानूनी या वित्तीय सलाह है। |
| Palm tab | `result_overview`, `result_your_lines` | Overview / Your lines | सारांश / आपकी रेखाएँ |
| Love tab | `result_love_how`, `result_love_partner`, `result_love_challenges` | How you love / Ideal partner / Challenges | आप कैसे प्यार करते हैं / आदर्श साथी / चुनौतियाँ |
| Career tab | `result_career_strengths`, `result_career_challenges`, `result_career_industries` | Strengths / Challenges / Ideal industries | खूबियाँ / चुनौतियाँ / आपके लिए सही क्षेत्र |
| Result actions | `result_scan_again`, `result_share`, `result_share_preparing`; Ask uses existing `feature_ask_guide` | Scan again / Share / Preparing… / Ask [Guide] | फिर से स्कैन करें / शेयर करें / तैयार हो रहा है… / [Guide] से पूछें |
| Share failure | `result_share_failed` | Couldn't create the share image. Please try again. | शेयर इमेज नहीं बन पाई। कृपया फिर कोशिश करें। |
| Share chooser / text | `result_share_chooser`, `result_share_text` | Share your reading / My palm reading from [AppName] | अपनी रीडिंग शेयर करें / [AppName] से मेरी हस्तरेखा रीडिंग |
| Share card | `share_card_title`, `share_card_footer` | My palm reading / Read your palm in [AppName] | मेरी हस्तरेखा रीडिंग / [AppName] में अपनी हथेली पढ़वाएँ |
| Photo unavailable | `result_photo_unavailable` | Photo unavailable | फ़ोटो उपलब्ध नहीं है |
| Error state (unreadable reading) | `result_missing_title`, `result_missing_body` | We couldn't open this reading / It may have been removed from this phone. Start a new scan from Home. | यह रीडिंग खुल नहीं पाई / हो सकता है यह इस फ़ोन से हट गई हो। होम से नया स्कैन शुरू करें। |
| Back to Home | `common_back_home` | Back to Home | होम पर लौटें |
| Invalid top bar | `invalid_title` | Photo not accepted | फ़ोटो स्वीकार नहीं हुई |
| Invalid retake | existing `scan_retake` | Retake photo | फ़ोटो फिर से लें |
| Reason titles | `invalid_<reason>_title` (7) | see InvalidPhoto table | धुंधली या अँधेरी: "आपकी फ़ोटो बहुत धुंधली या अँधेरी है" · दूर: "आपका हाथ बहुत दूर है" · गहने: "गहने आपकी हथेली को ढक रहे हैं" · मुड़ा: "आपका हाथ मुड़ा हुआ है" · उँगलियाँ: "आपकी उँगलियाँ बहुत सटी हुई हैं" · अधूरा: "हथेली का कुछ हिस्सा फ़ोटो में नहीं है" · रेखाएँ: "हमें आपकी हथेली की रेखाएँ नहीं दिखीं" |
| Reason tips | `invalid_<reason>_tip` (7) | see InvalidPhoto table | "अच्छी रोशनी में फ़ोन स्थिर रखें और फ़ोकस होने दें।" · "पास आएँ, ताकि हथेली पूरे फ़्रेम में आ जाए।" · "अँगूठियाँ और कंगन उतारें, फिर कोशिश करें।" · "हाथ सीधा और कैमरे की ओर खुला रखें।" · "उँगलियों को थोड़ा फैलाएँ।" · "कलाई से उँगलियों के सिरों तक पूरी हथेली फ़्रेम में रखें।" · "तेज़ रोशनी में हथेली खुली और सीधी रखें।" |
| Home card | `home_last_reading_overline`, title = existing `feature_palm`, `home_last_reading_meta` (`%1$s · %2$s`) | Your last reading / Palm reading / Right hand · 28 Nov 2026 | आपकी पिछली रीडिंग / हस्तरेखा / दायाँ हाथ · 28 नव॰ 2026 |
| Empty state (Home, no reading yet) | — | No card is shown; the hero "Scan my palm" CTA is the next step (D-19) | — |
| Destructive confirmation | (debug lab only, hardcoded) | Delete all readings: "Delete all readings?" / "Removes every saved reading and its photo on this phone." [Cancel] [Delete] | n/a (debug) |

**Content descriptions (en / hi):**

| Element | Key | English | Hindi |
|---------|-----|---------|-------|
| Photo with overlay | `result_photo_cd` (`%1$s` = localised drawn line names joined with ", ") | Your palm photo with lines drawn: %1$s | आपकी हथेली की फ़ोटो, बनाई गई रेखाएँ: %1$s |
| Photo, no lines drawn | `result_photo_cd_plain` | Your palm photo | आपकी हथेली की फ़ोटो |
| Home card (merged) | `home_last_reading_cd` (`%1$s` hand, `%2$s` date) | Your last reading, palm reading, %1$s, %2$s | आपकी पिछली रीडिंग, हस्तरेखा, %1$s, %2$s |

---

## Motion

Durations and easings come from `NocturneDurations` / `NocturneEasings` (also on `LocalNocturne.current`). Under reduce motion (`LocalNocturne.current.reduceMotion`: animator scale 0 or battery saver) every row below is static.

| Element | Motion | Duration / easing | Reduce motion |
|---------|--------|-------------------|---------------|
| Photo appears after decode | Fade 0 → 1 | `Quick` 200, `Standard` | Instant |
| Overlay draw-in (D-10; Figma "Palm overlay / Result reveal") | Each drawn line trims in along its path (halo and line together, `PathMeasure.getSegment`), in contract order, starting 120 ms apart; starts when the photo fade ends. 5 lines = 4 × 120 + 450 = 930 ms (D-10 "about 900 ms") | `Emphasized` 450 per line, `EmphasizedDecelerate` | All lines drawn fully at once |
| Overlay replay | Plays **once per Result entry** (a `rememberSaveable` "played" flag keyed by `readingId`). Not on tab switch, rotation, returning from the share sheet or process recreation | — | — |
| Tab selection | Selected pill colour crossfade (`animateColorAsState`) | `Quick` 200, `Standard` | Instant swap |
| Buttons | Existing press scale 0.97 | `Press` 150 (existing) | Existing (no scale) |
| Home last-reading card | Existing Home entry fade + 16 dp rise, `index = 1` | `Standard` 300, `EmphasizedDecelerate` (existing) | Existing (static) |
| Coming-soon sheet | Existing bottom-sheet scene | existing | existing |

Not built: legend chips "pop in", card rise stagger on the Result, and the card sheen shimmer (Figma "S10 Palm result" sequence and card "anim: shimmer"). One reveal on this screen is enough; see Figma table.

---

## Accessibility

- **Touch targets:** back button 48 dp, tabs 48 dp tall, buttons 52 dp, Home card ≥ 88 dp tall and full width. Legend chips, badges and trait chips are not interactive.
- **TalkBack:**
  - Photo box: one node with `result_photo_cd` (drawn lines listed by name) or `result_photo_cd_plain`; the overlay Canvas has no semantics of its own. Placeholder states read their visible text ("Photo unavailable") or the spinner's default progress semantics.
  - Each `LineCard` is `semantics(mergeDescendants = true)`, reading "Heart line, Approximate, <interpretation>". Colour dots are decorative (`contentDescription = null`).
  - "Your lines", top-bar titles and state-block titles are `heading()`.
  - Tabs: M3 `Tab` gives `Role.Tab` and selected state; labels are the visible text.
  - Home card: `clearAndSetSemantics { contentDescription = home_last_reading_cd; role = Button; onClick }`. Thumbnail and arrow disc are decorative.
  - ScanReady inline error and Result share error: `liveRegion = LiveRegionMode.Polite`.
  - The off-screen share card is `clearAndSetSemantics {}` so TalkBack never sees it.
- **Colour is never the only cue:** every drawn line has a text legend chip; every card names its line and states "Approximate" / "Not clearly visible" in text.
- **Contrast:** see Color. Every text pair ≥ 4.5:1; non-text dots ≥ 3:1; overlay lines on light skin are a backstop.
- **Font scaling (1.0 / 1.3 / 2.0):** everything sits in one vertical scroll; legend, badges and chips wrap (`FlowRow`); actions are always stacked; the photo cap (55% of the window) keeps content reachable. No fixed heights on text containers. The share card is fixed at font scale 1.0 by design.
- **Devanagari:** theme-level `devanagariSafe()` (font-metric line heights, 0 letter spacing); Overline gets `.devanagariSafe()` explicitly, as on Home. No `maxLines` outside the share card. Mixed-script reading text (hi reading in en app, or the reverse) renders through the same fallback font chain.
- **Reduce motion:** see Motion.

---

## Assets (vector drawables, our Figma art only)

Never reference-app assets; nothing from `_extract/`.

| Name | Source | Use |
|------|--------|-----|
| `ic_star` (**new**) | Figma `Icon/star`: `M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1-4.4-4.3 6.1-.9z` | Invalid reason `jewelry` |
| `ic_eye` (**new**) | Figma `Icon/eye`: `M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z` + `M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0` (circle cx 12 cy 12 r 3 as a path) | Invalid reason `no_palm_lines` |
| `ic_flash`, `ic_search`, `ic_hand`, `ic_gallery`, `ic_alert`, `ic_arrow` | existing | Reasons, photo placeholder, unreadable state, Home arrow disc |
| `illus_guide_orb` | existing | Share card header orb; coming-soon sheet (existing) |

Both new icons use the `ic_alert.xml` format (24 dp, viewport 24, 1.8 stroke, round caps and joins, `@color/nocturne_text_primary`, tinted at use). **Drawn in code, not drawables:** palm-line overlay and halo, the generated lab photo (gradient + palm silhouette), card fills and foil borders. **No new illustrations.**

---

## Figma vs locked decisions (decision wins)

| Item | Figma (2026-11-26 build) | Built in Phase 5 | Why |
|------|--------------------------|------------------|-----|
| Top-bar title | "Your reading" | "Your palm reading" | D-07 |
| Order | Tabs above the photo; disclaimer at the bottom | Photo, legend, disclaimer above the tabs | D-07, D-13 (visible on every tab) |
| Tab control | Custom segmented pill | M3 `PrimaryTabRow` restyled to the same segmented look (pill container, gradient selected pill, Label M) | D-08; Figma look kept |
| Share / Report | Icon buttons in the top bar (share, flag) | Share in the action column; no Report | D-20; "report this reading" deferred |
| Actions | Primary "Ask [Guide] about this" (sparkle icon), Secondary "Scan again" with AD chip | Primary "Scan again", Secondary "Share", Ghost "Ask [Guide]"; no AD chip; no button icons | D-20; no ads in Phase 5 (AD chip comes with the Phase 6 rewarded gate); `NocturneButton` has no icon slot and one is not added |
| Love / Career overlay focus | Legend Selected/Dimmed, non-focus lines at 25% | All drawn lines at full strength on every tab; legend has one state | D-11 (legend = drawn lines); simpler and the overlay does not change per tab |
| Disclaimer copy | "For entertainment. Not medical, legal or financial advice." | D-13 wording | D-13 |
| Line card title | "Heart line · Deep and curved" | Server `label` (fallback: localised full name) + badge | D-08 |
| Love card 3 title | "Challenges in love" | "Challenges" | D-08 |
| Card style | `gradient.card` + foil + white sheen (anim shimmer 6 s) | `gradient.card` + foil, no sheen, no shimmer | Many cards on one scroll; no animated chrome competing with the overlay reveal |
| Photo label chip | "Your right palm · today" over the photo | Not built | Not in D-07; hand and date are on the Home card |
| Layout B (floating bar, horizontal line-card rail) | Alternative frame | Not built | Layout A matches D-07 |
| Invalid photo | Dimmed photo with a red ✕, "We couldn't read this photo", reason card, "This didn't use your free scan.", "Retake photo" + "Choose another photo" | `NocturneStateBlock` with the reason icon in `status.error`, reason title + tip, "Retake photo" + "Back to Home" | D-14. The free-scan line returns in Phase 6 when the counter exists (D-15) |
| Reason copy | Figma's 7 titles/tips | D-14 copy | D-14 |
| Rewarded unlock frame | Sheet over the result | Not built | Phase 6 |
| Overlay halo | not specified | `bg.deep` @0.55, 9 dp (tuning within D-10's "about 30%") | Measured contrast on light skin (Color) |

---

## UI Considerations

Applicable state considerations resolved: 22 covered, 9 backstop, 5 unresolved.

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| loading | ScanReady read (interactive-control) | ✅ covered | While reading, the primary shows "Reading your palm…" disabled, Retake is disabled, a second tap starts nothing |
| error | ScanReady repository failure (interactive-control) | ✅ covered | Inline `status.error` caption (`scan_ready_error`) above the button, which becomes "Try again"; nothing is saved |
| loading | Result row load (media) | ✅ covered | Top bar + centred 48 dp `brand.link` spinner until the Room row emits |
| loading | Result photo decode (media) | ✅ covered | The photo box shows a spinner at the `image_size` aspect; legend hidden until the bitmap is drawn |
| error | Reading row missing / strict decode fails / invalid verdict stored (media) | ✅ covered | "We couldn't open this reading" state block with "Back to Home"; no crash (Copywriting `result_missing_*`) |
| error | Photo file missing or undecodable (media) | ✅ covered | "Photo unavailable" placeholder, no overlay, legend hidden; tabs, cards and actions work (D-12) |
| populated | Result, mixed sources sample (media) | ✅ covered | Heart, head, life, sun drawn with halo; legend lists those 4; note "Lines are drawn approximately."; Fate card shows "Not clearly visible" |
| empty | All lines `none` (media) | ✅ covered | Photo without overlay, legend hidden, note `result_no_lines_drawn`, every line card shows "Not clearly visible" |
| partial | `present=true` with `trace_source=none` (media) | ✅ covered | Not drawn, not in the legend, badge "Not clearly visible" |
| partial | Lines missing from the array / blank label / blank interpretation (list-collection) | ✅ covered | Missing types get no card; blank label falls back to the localised full name; blank interpretation hides that text; zero lines hides "Your lines" |
| empty | Empty `traits` / `ideal_industries` (list-collection) | ✅ covered | The chip row (Palm) or the "Ideal industries" card (Career) is hidden |
| zero-one-many | Legend chips 0 / 1 / 5 (list-collection) | ✅ covered | 0 → legend hidden; 1-5 → one `FlowRow`, wraps to a second row when needed |
| populated | Tabs (nav) | ✅ covered | Palm / Love / Career switch with the D-08 content; selection survives rotation (`rememberSaveable`); disclaimer visible on every tab |
| error | Invalid photo, each of 7 reasons (form) | ✅ covered | Reason icon, title and tip from the exhaustive mapping; "Retake photo" → `Scan(hand)`; nothing saved; capture deleted on leave |
| nav | Back from Result / InvalidPhoto (nav) | ✅ covered | Result back → Home (stack `[Home, Result]`); InvalidPhoto back = Retake → `Scan(hand)`; "Back to Home" pops to Home |
| loading | Share preparing (interactive-control) | ✅ covered | "Preparing…" disabled Share button; one image at a time |
| error | Share image failure (interactive-control) | ✅ covered | `result_share_failed` caption under the actions, polite live region, cleared on the next tap |
| populated | Ask [Guide] (nav) | ✅ covered | Opens the existing coming-soon sheet for `ASK_GUIDE` |
| empty | Home with no saved reading (list-collection) | ✅ covered | No last-reading card; hero CTA and Palm tile start a scan (D-19) |
| populated | Home after a save, and after force-stop + relaunch (list-collection) | ✅ covered | Card shows thumbnail, "Palm reading", hand · localised date; tap opens `Result(id)` from Room + `filesDir` |
| error | Home thumbnail missing (media) | ✅ covered | `ic_hand` placeholder in the thumbnail slot; the card still opens the reading (which then shows its own placeholder) |
| reduced-motion | Overlay reveal, photo fade, tab pill (static-content) | ✅ covered | All static under `reduceMotion`; overlay drawn complete |
| long-text | Long reading text on Result (static-content) | 🧪 backstop | No `maxLines` on Result; text wraps and the page scrolls. Check with a lab variant carrying a 600-character summary/interpretation at font scale 2.0 in en and hi (screenshot) |
| long-text | Share card truncation (static-content) | 🧪 backstop | Summary (≤ 3 lines) yields first; traits 1 line; disclaimer ≤ 3 lines, never fully cut. Verify the decoded PNG for the en sample, the hi fixture and a long-text variant (instrumented test decodes it; visual check on the emulator) |
| overflow | Result, InvalidPhoto, Home card at font scale 2.0 in Hindi (static-content, interactive-control) | 🧪 backstop | Badges wrap under titles, chips wrap, actions stacked, everything reachable by scroll. Emulator screenshots at 1.0 / 1.3 / 2.0 in en and hi |
| alignment | Overlay maths on 3:4, 1:1, 9:16 and the letterboxed case (media) | 🧪 backstop | `fitRect`/`mapPoint` unit tests plus the Compose pixel test (sample colours at mapped trace points on a plain photo), D-24 |
| contrast | Overlay lines on light and dark skin (media) | 🧪 backstop | Halo @0.55 keeps lines visible; heart on light skin is 2.53:1, carried by the text legend. Phone check with a real light-skinned palm photo once Phase 6 traces follow the palm |
| long-text | Devanagari in tabs, chips, badges, state blocks (static-content) | 🧪 backstop | No clipped matras or conjuncts (screenshot check as in Phases 3-4) |
| i18n | Every new string in `values-hi` (static-content) | 🧪 backstop | Lint `MissingTranslation` (or a unit check) fails on a missing Hindi key |
| a11y | TalkBack order and merged cards (static-content) | 🧪 backstop | Top bar → photo description → legend → disclaimer → tabs → cards (merged) → actions; share card never focused. Check with TalkBack on the emulator |
| motion | Overlay reveal plays once (media) | 🧪 backstop | Not replayed on tab switch, rotation or return from the share sheet; static under reduce motion (Compose test with the "played" flag + manual check) |
| unresolved | Hindi copy | ⚠ unresolved | All Hindi (strings and the hi fixture) is Claude-written; native review before release |
| unresolved | Figma visual QA | ⚠ unresolved | Values come from build scripts (MCP unavailable). Compare emulator screenshots with "S10 Hand result / Palm tab (Layout A)", Love, Career and the Invalid photo frames once Figma access works |
| unresolved | Share card off-screen capture | ⚠ unresolved | Recording a `GraphicsLayer` without drawing it on screen (and forcing a 1080 × 1350 px layout on any screen size) is for research to confirm; the D-21 Canvas fallback keeps this look |
| unresolved | Fixture traces vs the user's palm | ⚠ unresolved | Fixture traces do not follow the real palm (D-02); the overlay will look misplaced on real photos until Phase 6. Accepted for this phase |
| unresolved | Large screens and landscape | ⚠ unresolved | Android 16 ignores the portrait lock at sw ≥ 600 dp. The 55% photo cap and single column still work but were not designed for tablets. Assumed acceptable for M1 (phones only) |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (no shadcn or component registry on Android) | none | not applicable. No new Gradle artifact is expected (FileProvider is in androidx.core, `FlowRow` in Compose foundation, `PrimaryTabRow` in material3 1.4.0); any download goes through the D-25 consent checkpoint |

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS
- [ ] Dimension 2 Visuals: PASS
- [ ] Dimension 3 Color: PASS
- [ ] Dimension 4 Typography: PASS
- [ ] Dimension 5 Spacing: PASS
- [ ] Dimension 6 Registry Safety: PASS
- [ ] Dimension 7 Inventory Provenance: PASS

**Approval:** pending
