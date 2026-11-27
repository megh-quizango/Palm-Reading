---
phase: "3"
slug: "client-shell"
status: draft
shadcn_initialized: false
preset: none
created: "2026-11-27"
---

# Phase 3: Client Shell, UI Design Contract

> Visual and interaction contract for S01 splash, S02 language, S03 intro x3, S04 name + gender, S05 birth date/time, S06 Home (greeting, palm-scan hero, feature grid, coming-soon sheet, exit dialog), S07 hand selection and the placeholder Scan route. Written by gsd-ui-researcher, to be verified by gsd-ui-checker.

**Figma MCP was not available in this session.** The researcher had no ToolSearch or Figma tools, and Phase 1 had already found that the Figma MCP needs `/mcp` authentication. So no live `get_screenshot`, `get_design_context` or `get_variable_defs` calls were made against file `Ej98dV7fZQUhRwUyfQIlyJ`. To fill the gap, Figma values were rebuilt from the **Figma build scripts of the 2026-11-26 design session**: the `use_figma` code that created the frames, kept in the local Claude Code transcript `172eeb1d-…cb.jsonl`. That code covers page "1 · Foundations" (icons, line 493), "2 · Components" (lines 708 and 732, restyled at 1268), "3 · Screens" S01-S05 (line 769) and S06/S07 (line 840; art re-applied at 1288), "5 · Motion" (line 1302) and the 6-layer star field (line 1170). Those values are exact for what the scripts built. Any manual edit made in Figma after 2026-11-26 is **not** captured. Values were also checked against `design-system/nocturne.tokens.json`, the generated `NocturneTokens.kt`, `replica/PRD.md` and the Phase 1 code. A visual QA pass against the real Figma frames is still required (see UI Considerations).

**Precedence:** `03-CONTEXT.md` decisions D-01..D-18 come first. Figma comes second. The deviations are listed in "Figma vs locked decisions".

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (shadcn does not apply: native Android, Jetpack Compose) |
| Preset | not applicable |
| Component library | In-repo "Nocturne" design system (`app.palm.core.designsystem`) on Material 3 primitives (`androidx.compose.material3`, via Compose BOM 2026.09.00) |
| Icon library | Custom vector drawables from Figma "1 · Foundations" `Icon/*` (24 dp grid, 1.8 dp stroke, round caps and joins). No `material-icons-extended` (CLAUDE.md) |
| Font | Cormorant Garamond SemiBold 600 (display), Manrope 400/500/600/700 (UI), with glyph-level Noto Serif/Sans Devanagari fallback (`NocturneFonts`, Phase 1). All bundled, OFL |
| Tokens | `design-system/nocturne.tokens.json` turned into `NocturneTokens.kt` by `tools/gen_tokens.py`. Never hand-copy a token value. Art colours live in the art (see Color) |

---

## Component Inventory

Enumerated by `rg -U "@Composable\s*\n\s*fun \w+" android/app/src/main/java/app/palm/core/designsystem` — 6 public composables — app.palm.core.designsystem@c4b804d (in-repo, git HEAD) — 2026-11-27.
Material 3 primitives: resolved by `curl -s https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom | grep -A1 '<artifactId>material3</artifactId>'` — androidx.compose.material3:material3@1.4.0 (BOM 2026.09.00 pins 247 artifacts) — 2026-11-27. M3 is not inventoried as a table; any M3 composable in 1.4.0 may be used. The Gradle cache's material3 1.5.0 belongs to BOM 2026.11.01, which the project does not use yet.
Filesystem cross-check: `ls android/app/src/main/java/app/palm/core/designsystem/components/*.kt | wc -l` = 3 files (Buttons.kt, Chips.kt, FeatureCard.kt), which hold 4 of the 6 composables. The other 2 are `NocturneTheme` and `NocturneBackground`. No mismatch.

This table is a **non-exhaustive** list of known-good components, not a closed allowlist. Anything Material 3 or the design system exports may be used.

### Existing (reuse, extend in place)

| Component | Import path | Notes / Phase 3 change |
|-----------|-------------|------------------------|
| `NocturneTheme` | `app.palm.core.designsystem.NocturneTheme` | Root theme. **Change:** (a) `devanagari` must follow the *app* locale. With per-app locales `LocalConfiguration` already reflects it, so verify after D-06 lands. (b) `reduceMotion` = animator scale 0 **or** `PowerManager.isPowerSaveMode` at start (Figma "5 · Motion" rule: loops pause under battery saver). (c) `devanagariSafe()` also sets `letterSpacing = 0.em`, so the tracked Overline never splits Devanagari conjuncts. (d) `toTypography()` maps every M3 slot to a Phase 3 style (see Typography) |
| `NocturneBackground` | `app.palm.core.designsystem.NocturneBackground` | **Extend, do not replace (D-14):** Sky gradient base plus the 6 layers in "Background (FND-03)". Add `animate: Boolean = true`, so the root can pause it while an opt-out route is on top |
| `NocturneButton` + `NocturneButtonStyle` | `app.palm.core.designsystem.components.NocturneButton` | Primary / Secondary / Ghost, 52 dp min height, pill. **Change:** add `NocturneButtonStyle.OnHero` (fill `TextOnPrimary` #FFFFFF, label `BrandPrimaryDeep`, no glow) and a press scale (see Motion). No icon slot in Phase 3 |
| `NocturneChip` | `app.palm.core.designsystem.components.NocturneChip` | Non-interactive pill. Used as the tile "Soon" badge base (see S06). **Change:** padding `S12`/`S8` → `S8`/`S4` (Phase 3 spacing set) |
| `AdChip` | `app.palm.core.designsystem.components.AdChip` | Not used in Phase 3 (no ads until Phase 6) |
| `FeatureCard` | `app.palm.core.designsystem.components.FeatureCard` | **Change:** `body` becomes `String? = null`. Add `illustration: Painter? = null` (132 dp slot on top), `badge: String? = null` and the 32 dp arrow disc on the title row. Padding `S20` → the title-row padding in S06 (16/8/16/16). The existing showcase call `FeatureCard(title, body)` keeps compiling |
| `Modifier.nocturneGlow`, `nocturneBrush`, `NocturneGradientAngles`, `NocturneBorderWidth` (1 dp) | `app.palm.core.designsystem` (Effects.kt) | **Add** `NocturneSelectedBorderWidth = 2.dp` beside `NocturneBorderWidth`. The tokens define no border widths, so Effects.kt stays the one place for them |

### New in `core/designsystem/components` (used by 2+ screens)

| Component | File | Built on | Used by |
|-----------|------|----------|---------|
| `NocturneTextField` + `nocturneTextFieldColors()` | `TextFields.kt` | M3 `OutlinedTextField` (its 1 dp unfocused / 2 dp focused border matches Figma). Label rendered above, `supportingText` for helper/error, `isError` | S04 |
| `NocturneSelectField` | `TextFields.kt` | `Row` with the same shape, fill and border as the text field, plus `clickable(role = Role.Button)` | S05 date, S05 time |
| `NocturneChoiceTile` | `Chips.kt` | `Box` + `Modifier.selectable(role = Role.RadioButton)` inside `selectableGroup()` | S04 gender |
| `NocturneTopBar` | `TopBar.kt` | `Row`: 48 dp back `IconButton` + title | S07, Scan placeholder |

### Screen-local (one screen only, stays in its feature package)

`LanguageRow` (S02, M3 `RadioButton(onClick = null)` in a `selectable` row), `PageIndicator` (S03), `HeroCard` (S06), `ComingSoonSheet` (S06, M3 `ModalBottomSheet`), `ExitDialog` (S06, M3 `BasicAlertDialog` + Nocturne surface), `HandOptionCard` + `PhotoTips` (S07). Date/time pickers are M3 `DatePickerDialog` + `DatePicker` and M3 `TimePicker` in a `BasicAlertDialog`.

---

## Spacing Scale

From `NocturneSpace` (tokens `dimension.space`). **Phase 3 uses only 4, 8, 16, 24, 32 and 48 dp for padding, gaps and insets.** No spacing exceptions. The tokens S2, S12 and S20 still exist in `NocturneSpace` but Phase 3 code must not use them. Figma's 12, 14, 10, 6 and 20 dp values are rounded onto the scale (see "Figma vs locked decisions").

| Token | Value | Usage in Phase 3 |
|-------|-------|------------------|
| S4 | 4 dp | Overline-to-icon gap, helper-text gap under fields |
| S8 | 8 dp | Overline → title, list-row gap, label → field, gender-tile gap, page-dot gap, tip-column gap, hero text-column gap, hero column → orb gap, "Soon" badge inset, tile title-row end padding, row vertical padding, dialog button gap |
| S16 | 16 dp | **Screen horizontal padding** (360 dp frame → 328 dp content), section gap, bottom CTA padding, feature-grid gutter (both axes), hand-card gap and padding, tips card padding and gaps, tile title-row start/top/bottom padding, row horizontal padding |
| S24 | 24 dp | Top padding under the status bar on onboarding screens, title block → first control, **hero card inner padding**, dialog padding, sheet side padding |
| S32 | 32 dp | Space above the bottom CTA when content is short (minimum, a flexible spacer takes the rest) |
| S48 | 48 dp | Minimum touch target (`NocturneSize.TouchMin`) |

**Component and art sizes (dimensions, not spacing; all multiples of 4):**
- Controls: `NocturneSize.TouchMin` 48 dp, `Button` 52 dp, 56 dp minimum height for fields, rows and gender tiles (Figma "56 dp field"), 64 dp top bar and Home header, exit dialog width 312 dp.
- Icons: `Icon` 24 dp, small icons 16 and 12 dp, arrow disc 32 dp, hand-card selected circle 24 dp, tip status badges 20 dp, drag handle 32 x 4 dp, page dots 8 x 8 / 24 x 8 dp.
- Art: illustration slot 132 dp, tile art 112 dp, hero orb 104 dp, coming-soon art 96 dp, hand art 92 x 120 dp, intro art 160 x 208 / 176 / 160 dp, intro accent orb 48 dp, tip art 56 dp, intro dashed ring 300 dp, `hero_orbit` viewport 220 x 140 dp.
- Strokes: 1 dp default border, 2 dp selected/focused/error border and rings.
- Radii: `NocturneRadius.Md` 16 (fields, rows, tiles), `Lg` 24 (cards, hero, hand cards, tips card), `Xl` 32 (intro art panel, sheet top corners, dialogs via theme `extraLarge`), `Pill` (buttons, badges).

---

## Typography

All styles come from `LocalNocturne.current.type` (token text styles). Never set `fontSize` or `fontWeight` inline.

**Phase 3 renders exactly 4 sizes (32, 26, 15, 13 sp) and 2 weights (500, 600).** This counts everything on screen: the components, the M3 pickers, small print. The Phase 1 token scale had 7 sizes and 4 weights, so Phase 3 changes 4 token values (below). No token is added or removed.

| Size | Token | Size / line height | Weight | Used for |
|------|-------|--------------------|--------|----------|
| 32 | Display L | 32 / 38 sp | 600 (Cormorant) | Screen titles S02-S05, intro page titles, Home "[AppName]" title |
| 26 | Display M | 26 / 32 sp | 600 (Cormorant) | Hero title, Home section header, coming-soon feature name, exit dialog title, S07 top-bar title |
| 15 | Title M | 15 / 22 sp | 600 (Manrope) | Emphasis at body size: feature-tile titles, S07 question, "Good photo", S05 chosen date/time values, gender tile labels, scan placeholder title, time-dialog title |
| 15 | Label L | 15 / 20 sp | 600 (Manrope) | `NocturneButton` label, M3 `TextButton` label, hand-card labels |
| 15 | Body L | 15 / 22 sp | 500 (Manrope) | All body copy: subtitles, intro body, hero body, sheet/dialog body, language rows, text-field input, checkbox label, placeholders |
| 13 | Label M | 13 / 18 sp | 600 (Manrope) | Field labels above fields ("Your first name", "Birth date", "Gender"), "Avoid these", "Soon" badge |
| 13 | Overline | 13 / 18 sp, +0.12 em | 600 (Manrope) | "Step 1 of 2", Home greeting, hero overline, "Coming soon" |
| 13 | Caption | 13 / 18 sp | 500 (Manrope) | Helper/error text, hints, tip captions, notes, privacy line |

Hierarchy comes from family (Cormorant display vs Manrope UI), size, and colour (`text.primary` vs `text.muted`). At 15 sp the 600 vs 500 step separates titles from body.

**Token changes (edit `design-system/nocturne.tokens.json`, then regenerate `NocturneTokens.kt` with `tools/gen_tokens.py`; never hand-edit the .kt):**

| Token | Phase 1 | Phase 3 |
|-------|---------|---------|
| `typography.Title.M` | 17 / 24 / 700 | **15 / 22 / 600** |
| `typography.Label.L` | 16 / 20 / 700 | **15 / 20 / 600** |
| `typography.Caption` | 12 / 16 / 400 | **13 / 18 / 500** |
| `typography.Overline` | 12 / 16 / 700, +0.12 em | **13 / 18 / 600, +0.12 em** |

Update the `Overline` assertion in `tools/test_gen_tokens.py` (line 58) to `NocturneTypeSpec(NocturneFontRole.Ui, 600, 13f, 18f, 0.12f)`. Every needed font file is already bundled: `manrope_medium`, `manrope_semibold`, `cormorant_garamond_semibold`, `noto_sans_devanagari_medium/semibold`, `noto_serif_devanagari_semibold`. Keep `manrope_regular` and `manrope_bold`: Phase 3 does not render them, but removing them is not in scope.

**Not rendered in Phase 3:** Display XL (40), Display S (22) and Body M (14/400). `FeatureCard` only draws its `body` (Body M) when one is passed, and Phase 3 passes none. The debug-only showcase is exempt.

**M3 typography slots (`toTypography()` in NocturneTheme.kt).** Map every slot to a Phase 3 style, so the M3 pickers, fields and sheet cannot bring their default sizes back (for example, the TimePicker time selector reads `displayLarge`, which is mapped to Display XL today):

| M3 slots | Nocturne style |
|----------|----------------|
| `displayLarge`, `displayMedium` | `displayL` (32) |
| `displaySmall`, `headlineLarge`, `headlineMedium`, `headlineSmall` | `displayM` (26) |
| `titleLarge`, `titleMedium`, `titleSmall` | `titleM` (15/600) |
| `labelLarge` | `labelL` (15/600) |
| `bodyLarge`, `bodyMedium` | `bodyL` (15/500) |
| `labelMedium`, `labelSmall` | `labelM` (13/600) |
| `bodySmall` | `caption` (13/500) |

**Devanagari rules (D-07):**
- In the `hi` locale the whole theme uses `devanagariSafe()`: line height `Unspecified` (font metrics), letter spacing 0.
- In the `en` locale, three Devanagari surfaces still appear: the "हिन्दी" language row, any user-typed Devanagari name (greeting, name field) and Hindi strings never shown in English. Apply `.devanagariSafe()` to the language-row endonym, the greeting text and the name-field `textStyle`.
- No fixed heights on text containers: use `heightIn(min = …)` only. No `maxLines` on any Phase 3 text. Everything wraps.

---

## Color

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | `gradient.sky` (#1A1240 → #0B0820 @0.55 → #07051A, 180°) over `bg.base` #0B0820 | The full-bleed background on every content route, drawn once at the root by `NocturneBackground` |
| Secondary (30%) | `surface.card` #1C1645, `gradient.card` (#2A2066 → #150F38, 135°), `surface.card-hi` #2A2160, `bg.raised` #1A1240 | Feature tiles, tips card, fields, rows, gender tiles, hand cards, sheet and dialog containers (`bg.raised`) |
| Accent (10%) | `accent.gold` #F2C66D (semantic `accent`) | Only the elements listed below |
| Brand / interactive | `brand.primary` #6A4CF5, `gradient.primary` (#5B3FE6 → #7A4EF2, 90°), `brand.link` #A99BFF, `gradient.hero-aurora` | Primary buttons, selected states (gradient fill or `brand.link` 2 dp border), radio/checkbox selected, focused field border, page-indicator active dot, hero card fill |
| Destructive | `status.error` #FF8A8A | Field error border, error text and icon, the 20 dp "x" badges on bad photo tips. Never used on buttons: Phase 3 has no destructive action |
| Supporting status | `status.success` #6EE7B7, `status.warning` #FFD27A | Success: good-photo ring and check badge. Warning: not used |

**Accent (gold) reserved for:** (1) the step overlines "Step 1 of 2" / "Step 2 of 2" (S04, S05); (2) the hero overline text and its 16 dp sparkle icon (S06); (3) the 24 dp sparkle before the Home section header; (4) the feature-tile arrow icon and the gold end of its disc stroke; (5) the L5 near-sparkle tint in the background (as a token-derived pale gold); (6) `focus.ring` (2 dp, outside) for keyboard/D-pad focus where a custom indication is drawn. Not for buttons, not for selected states, not for body text.

**Art colours vs UI colours.** UI chrome uses tokens only. Figma used a few literal colours inside art and decoration that have no token. They map like this (art inside vector drawables keeps its own exported colours):

| Figma literal | Where | Phase 3 value |
|---------------|-------|---------------|
| #3D7BFF @0.25 | L1 nebula, third blob | `palm.head` #7CC4FF @0.25 |
| #B04BF0 @0.30 | L1 nebula, second blob | `gradient.hero-aurora` stop 1 (`NocturneGradients.HeroAurora[2]`) |
| #E9E2FF | L3 mid stars, L4 nodes | `text.primary` #F4F1FF |
| #C9BCFF | L4 constellation lines | `brand.link` #A99BFF |
| #FFF3CC | L5 sparkles | `lerp(AccentGold, TextOnPrimary, 0.7f)` |
| #3A2C8A → #1E1650 + radial #9C82FF @0.55 | Feature-tile illustration slot | vertical `surface.card-hi` → `bg.raised`, plus a radial `shadow.Glow.Orb` colour (#8B6CFF99) fading to transparent at 0.55 of the slot width, centred |
| #2A2160 → #120D33 | Intro art panel | `gradient.card` at 135° |
| #FF7A9A @0.45, #7CC4FF @0.25 radials | Hero card glows | `palm.heart` @0.45 (top-left), `palm.head` @0.25 (bottom-right) radials over `gradient.hero-aurora` |

**Contrast (WCAG 2.x, computed 2026-11-27):**

| Pair | Ratio | Verdict |
|------|-------|---------|
| text.primary on bg.base / bg.raised | 17.6 / 15.7 | pass |
| text.muted on bg.base / surface.card / card-hi | 9.2 / 7.9 / 6.6 | pass |
| white on gradient.primary ends (#5B3FE6 / #7A4EF2) | 6.4 / 5.0 | pass |
| brand.primary-deep on white (OnHero button) | 6.4 | pass |
| white on hero-aurora #3420A8 / #6A4CF5 / #B04BF0 | 11.0 / 5.3 / **4.1** | Hero text must stay in the left column, over the first ~60% of the gradient (see S06) |
| accent.gold on bg.base / hero #3420A8 / hero #6A4CF5 | 12.2 / 6.8 / **3.3** | Hero overline sits top-left, over #3420A8 only |
| text.on-accent on accent.gold | 11.1 | pass |
| status.error on bg.base / surface.card | 8.7 / 7.4 | pass |
| brand.link on surface.card (selected border, non-text) | 7.1 | pass (≥ 3:1) |
| text.muted @50% on bg.base (inactive page dot, non-text) | 3.05 | pass (≥ 3:1), do not go lower |
| border.hairline @40% on surface.card | 2.2 | Decorative only. No state may rely on it alone |

---

## Layout and screen chrome (all routes)

- **Edge-to-edge** (Phase 1 `enableEdgeToEdge`, light bar icons). The background draws behind both system bars. Content pads with `WindowInsets.safeDrawing` (status bar on top, navigation bar at the bottom). Screens with a text field also use `imePadding()`, so the bottom CTA stays above the keyboard.
- **One background for the whole app.** `NocturneBackground` sits once at the root, *behind* `NavDisplay`, and all entries are transparent. The star clock never restarts or jumps between onboarding steps.
  - Each route key declares `showsBackground` (D-14): true for Splash, Language, Intro, Name, Birth, Home, HandSelect and the ComingSoon overlay; **false for Scan**.
  - When the top key opts out, the root passes `animate = false`, so the clock is not composed, and the entry draws an opaque `bg.camera` #05040E fill.
- **Bottom CTA pattern (S02-S05, S07):** full-width `NocturneButton` Primary, 16 dp side padding, 16 dp above the navigation-bar inset. The content above it scrolls (`verticalScroll`) when it does not fit (small phones, font scale 2.0, keyboard open).
- **Back behaviour (D-01, D-13):**
  - Language leaves the app.
  - Intro goes to Language. Paging back inside the pager happens by swipe only. System Back on Intro pops the route; the pager keeps its saved page if re-entered.
  - Name goes to Intro, Birth goes to Name, HandSelect goes to Home, Scan goes to HandSelect.
  - On Home, Back opens the exit dialog. While the dialog or the coming-soon sheet is open, Back dismisses it.
  - Finishing onboarding replaces the stack with `[Home]`.

---

## Background (FND-03): the 6-layer star field

Source: Figma `Illustration/Star field` layer names (360 x 800 frame) and the "5 · Motion" table "Ambient background". All positions are fractions of the canvas (Figma x/360, y/800). All radii scale by `size.width / 360dp`.

| # | Layer | Content (Figma) | Animation | Timing |
|---|-------|-----------------|-----------|--------|
| 0 | Sky | `gradient.sky`, 180° (top → bottom) | none | — |
| 1 | Nebula | 3 radial blobs, each colour → transparent: `brand.primary` @0.45 at (310,150) rx 230 ry 190; hero-aurora #B04BF0 @0.30 at (30,520) rx 210 ry 180; `palm.head` @0.25 at (270,770) rx 200 ry 150 | Drift ±24 dp on a slow Lissajous path (x = sin θ, y = sin 2θ / 2); opacity 0.7 ↔ 1 | `AmbientSlow` 40 000 ms, `GentleSine` |
| 2 | Far stars | 55 dots, r 0.8 dp, `text.on-primary` (white), alpha 0.25-0.55, seeded positions | Twinkle in 4 phase groups between each star's alpha and 0.25; parallax 0.2x (drift amplitude 4 dp) | `Ambient` 4 000 ms, `GentleSine`, reverse |
| 3 | Mid stars | 26 dots, r 1.3 dp, `text.primary`, alpha 0.5-0.9 | Twinkle 0.5 ↔ 0.9; parallax 0.5x (drift 8 dp) | 3 000 ms (Figma layer value, no token), `GentleSine`, reverse |
| 4 | Constellations | Polyline A (228,228)(262,210)(298,226)(312,262)(286,286); polyline B (40,640)(70,618)(104,630); 1 dp `brand.link` lines; nodes r 2.0 / 1.8 dp `text.primary` | Line alpha 0.3 ↔ 0.6; nodes pulse with the same phase | 6 000 ms (Figma layer value), `GentleSine`, reverse |
| 5 | Near sparkles | 7 four-point sparkles, r 3.5-6.5 dp, pale gold (see Color) @0.9, each on a soft white radial glow r x 2.2 @0.35 | One at a time: scale 0.8 ↔ 1.15 + rotate 15°; parallax 1x (drift 16 dp) | `AmbientFast` 2 400 ms per sparkle, stagger 400 ms |
| 6 | Shooting star | Line (230,112) → (322,70), 1.6 dp, white gradient 0 → 0.95 toward a 2.2 dp head | Slide 120 dp along its line, fade in 100 ms (`Instant`) / out 400 ms; max 1 on screen | 900 ms travel, next one after a random 8-12 s |

Rules:
- **Parallax.** Phase 3 parallax is the drift-amplitude ratio between layers (0.2x / 0.5x / 1x on one shared 40 s orbit, as the Phase 1 code already does with 4 / 8 / 14 dp; Phase 3 sets the near layer to 16 dp). Drift amplitudes and slide distances are motion offsets, not layout spacing. Scroll-offset and device-tilt parallax (Figma "max 6 dp") are **not built** in Phase 3: they need a sensor listener or scroll plumbing to the root, which risks jank on low-end phones.
- **Draw phase only (D-14).** All animated values are read inside `drawWithCache { onDrawBehind { … } }`. Brushes, the sparkle `Path` and star lists are built once per size in the cache block. No recomposition per frame. The content stays in its own `graphicsLayer` (as in Phase 1).
- **Static rest state (reduce motion, power save, `animate = false`).** Every star at its own alpha, constellation alpha 0.45, nebula alpha 0.85, no drift, sparkles at scale 1, no shooting star.
- **Budget.** At most the background + one hero loop run per screen (Figma rule). On the Redmi, Home idle for 10 s and 3 intro swipes must show **≤ 5% janky frames** in `adb shell dumpsys gfxinfo app.palm` (reset first with `dumpsys gfxinfo app.palm reset`).

---

## Motion

Durations and easings come from `NocturneDurations` / `NocturneEasings` ("Nocturne / Motion" variables). The Figma "5 · Motion" table maps them to these uses.

| Element | Animation | Duration / easing | Reduced motion |
|---------|-----------|-------------------|----------------|
| Screen transition (NavDisplay `transitionSpec` / `popTransitionSpec`) | Enter: fade in + slide 24 dp from the end. Exit: fade out (pop mirrors the direction) | Enter `Standard` 300 + `EmphasizedDecelerate`; exit `Quick` 200 + `EmphasizedAccelerate` | `EnterTransition.None` / `ExitTransition.None` |
| Splash → first screen | `setOnExitAnimationListener`: splash view alpha 1 → 0, then `remove()` | `Quick` 200, `Standard` easing | Remove immediately |
| Intro "Next" | `animateScrollToPage` | `Emphasized` 450, `Standard` | `scrollToPage` (instant) |
| Page indicator | Active dot width 8 ↔ 24 dp, colour cross-fade | `Quick` 200, `Standard` | Instant |
| Home first entry (once per process) | Header fades in → hero rises 16 dp + fades → tiles fade + rise 16 dp with a 60 ms stagger in grid order | `Standard` 300, `EmphasizedDecelerate` | No entry animation |
| Hero card idle (the one hero loop) | Orb breathe scale 1 ↔ 1.04; orbit ring (`hero_orbit`) rotates 360° | Orb `Ambient` 4 000 `GentleSine` reverse; orbit `AmbientSlow` 40 000 linear | Static |
| Button / feature card press | Scale 1 → 0.97 while pressed (via `graphicsLayer` lambda) | `Press` 150, `Standard` | No scale (ripple/colour only) |
| Gender tile, hand card, language radio select | Selected fill/border cross-fade | `Quick` 200, `Standard` | Instant |
| Coming-soon sheet | M3 `ModalBottomSheet` default slide-up; scrim 0 → 60% | M3 default (close to `Emphasized` 450) | M3 default (accepted, see Considerations) |
| Exit dialog | Content scale 0.92 → 1 + fade | `Standard` 300, `Standard` | Fade only |
| Feature-tile idle float / sheen, button glow animation | **Not built** (Figma rule: one hero loop + background per screen) | — | — |

No flashing faster than 3 times per second. Twinkle alpha never goes below 0.25. Loops stop when the activity is not visible (the Compose frame clock pauses).

---

## Screens

### S01 Splash (ONB-01, D-02)

- **System splash only** (`androidx.core:core-splashscreen`).
  - New theme `Theme.Palm.Starting` (parent `Theme.SplashScreen`): `windowSplashScreenBackground` = `@color/nocturne_bg_base`, `windowSplashScreenAnimatedIcon` = `@drawable/ic_launcher_foreground` (the Phase 1 orb stand-in, adaptive-icon safe zone), `postSplashScreenTheme` = `@style/Theme.Palm`.
  - The manifest activity uses `Theme.Palm.Starting`.
- `installSplashScreen()` before `super.onCreate`. `setKeepOnScreenCondition { routing == null }` stays on until the first DataStore read returns. Then the start stack is `[Home]` (onboarded) or `[Language]`. No artificial delay, no spinner, no copy.
- The `Splash` NavKey (D-01) renders only the shared background with no content. Under the kept splash it should never be visible for more than one frame.
- States: cold start (as above); warm start (no splash shown by the system, the stack is restored); DataStore read error → treated as an empty profile → `[Language]`.
- Not built: Figma's composed splash (wordmark, tagline, spinner, "Reading the stars…"), the app-open ad and offline states (out of M1).

### S02 Language (ONB-02, D-06)

Layout, top to bottom (24 dp under the status bar, 16 dp sides):
1. Title "Choose your language" (Display L, `text.primary`, heading semantics).
2. 8 dp gap, then subtitle (Body L, `text.muted`).
3. 24 dp gap, then the list, 2 rows with 8 dp gap, inside `selectableGroup()`:
   - `LanguageRow`: min height 56 dp, radius Md 16, padding 8 dp vertical / 16 dp horizontal, row `selectable(role = Role.RadioButton)`.
   - Endonym label: "English" / "हिन्दी" (Body L, `text.primary`, `devanagariSafe()`; not translated, `translatable="false"`).
   - M3 `RadioButton(onClick = null)` at the end: selected colour `brand.link`, unselected `text.muted`.
   - Default row: no fill. Selected row: `surface.card` fill + 2 dp `brand.link` border.
4. Flexible spacer, then the bottom CTA `language_cta`: "Continue in English" when English is selected, "हिंदी में आगे बढ़ें" when Hindi is selected (the label re-renders with the locale). Always enabled, since a language is always selected.

Behaviour:
- Initial selection: the saved `language`, else `hi` if the system language is Hindi, else `en`.
- Tapping a row **applies the locale immediately** and persists it to DataStore. Title, subtitle and button re-render in the chosen language.
- If the chosen back-compat mechanism recreates the activity (AppCompat path on API 26-32), the user must land back on Language with the new selection: the back stack is saveable and `windowBackground` is `bg.base`, so there is no white flash. A one-time background restart is acceptable.
- If the AppCompat path is chosen, `Theme.Palm` must move to an AppCompat parent and `MainActivity` to `AppCompatActivity` with no visual change. That is the planner's call, per D-06.

States: first run (default selection). Hindi selected (whole UI in Hindi, Devanagari line heights). No empty or error state.

### S03 Intro x3 (ONB-03, D-08)

- `HorizontalPager`, 3 pages, swipe enabled. Each page is a column (16 dp sides, 24 dp top):
  1. **Art panel**: full width, radius Xl 32, fill `gradient.card` 135°, a 300 dp dashed ring (1 dp `brand.link` @0.3, dash 2/6) behind the art.
     - Height: takes the remaining space, `weight(1f)` with `heightIn(min = 160.dp, max = 340.dp)`.
     - Art (decorative, `contentDescription = null`):
       - Page 1: `illus_constellation_hand` 160 x 208 dp + `illus_guide_orb` 48 dp at top-end (24 dp inset).
       - Page 2: `illus_zodiac_wheel` 176 dp.
       - Page 3: `illus_guide_orb` 160 dp.
  2. 24 dp, then the title (Display L, centred, heading).
  3. 8 dp, then the body (Body L, `text.muted`, centred).
  4. When the title and body do not fit at large font scale, the panel shrinks to its 160 dp minimum, then the text block scrolls inside the page.
- **Bottom row** (outside the pager, 16 dp sides and bottom):
  - `PageIndicator` at the start: active dot 24 x 8 dp pill `brand.link`; inactive dots 8 x 8 dp `text.muted` @0.5; 8 dp gap. One `contentDescription` "Page 2 of 3"; the dots are not tappable.
  - Button at the end (Primary, wrap content): "Next" on pages 1-2, "Get started" on page 3 → Name. Both labels are locked by D-08.
- States: page 1/2/3. No empty or error state.

### S04 Name + gender (ONB-04, D-09)

Layout (24 dp top, 16 dp sides, `imePadding`, scrolls):
1. "Step 1 of 2" (Overline, `accent.gold`).
2. 8 dp, then the title "What should we call you?" (Display L, heading).
3. 24 dp, then `NocturneTextField`:
   - Label above: "Your first name" (Label M, `text.muted`).
   - Field: min height 56 dp, radius Md 16, container `surface.card`. Border: 1 dp `border.hairline` @0.4 (default) / 2 dp `brand.link` (focused) / 2 dp `status.error` (error).
   - Text: Body L, `text.primary`, `devanagariSafe()`. Cursor `brand.link`. Placeholder "e.g. Priya" in `text.muted`.
   - Helper below: Caption `text.muted`. When in error, replaced by a 16 dp `ic_alert` + error text in `status.error`.
   - IME: `KeyboardCapitalization.Words`, autocorrect off, `ImeAction.Done` (hides the keyboard). Input is capped at 30 characters (further characters are ignored).
4. 24 dp, then "Gender" (Label M, `text.muted`, same as the field label) and 8 dp.
5. A row of 3 `NocturneChoiceTile` (equal weight, 8 dp gap) inside `selectableGroup()`:
   - Min height 56 dp, radius Md 16, 8 dp padding. Label (Title M) centred and wrapping.
   - Default: `surface.card` fill + 1 dp hairline @0.4, label `text.primary`.
   - Selected: `gradient.primary` fill + 1 dp `brand.link` + `GlowPrimary`, label `text.on-primary`.
   - Tap again clears the choice. State description "Selected" / "Not selected".
6. 8 dp, then the hint (Caption, `text.muted`).
7. Bottom CTA "Continue": disabled at 38% alpha until the name is valid.

Validation (pure function, D-09): trimmed, ≥ 2 Unicode letters, only letters plus single spaces, hyphens or apostrophes *between* letters, ≤ 30 characters.

Error visibility: the error shows when the field is **non-empty and invalid** and either focus has left the field or Done was pressed. It clears as soon as the value becomes valid. An empty field never shows an error.

States:
- Empty (placeholder, Continue disabled).
- Filled valid (Continue enabled).
- Error: too short → `name_error_short`; disallowed characters → `name_error_chars`.
- Pre-filled from a saved incomplete onboarding (D-04).
- Save failure: `common_save_failed` shown as a Caption in `status.error` above the CTA; the user stays on the step.

### S05 Birth date and time (ONB-05, D-05, D-10)

Layout (24 dp top, 16 dp sides, scrolls):
1. "Step 2 of 2" (Overline, gold).
2. 8 dp, then "When were you born?" (Display L, heading).
3. 8 dp, then the body (Body L, muted).
4. 24 dp, then "Birth date" (Label M, muted), 8 dp, then the date `NocturneSelectField`:
   - Same shape, fill and border as the text field, min height 56 dp, trailing 24 dp `ic_chevron_down` in `text.muted`.
   - Value: Title M (15/600) `text.primary`, formatted `DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)` in the app locale ("12 March 1996" / "12 मार्च 1996").
   - Empty: placeholder "Choose a date" (Body L, `text.muted`).
5. 16 dp, then "Birth time (optional)" (Label M, muted), 8 dp, then the time field: `FormatStyle.SHORT`, following the device 12/24 h setting.
6. 8 dp, then the checkbox row "I don't know my birth time":
   - Min height 48 dp, `toggleable(role = Role.Checkbox)`.
   - M3 `Checkbox(onCheckedChange = null)`: checked `brand.primary`, checkmark `text.on-primary`, unchecked border `text.muted`. Label Body L.
   - When checked: clears the time, sets `birthTimeUnknown`, disables the time field (38% alpha, not clickable) and shows the note (Caption, `text.muted`) below.
7. Bottom CTA "Continue": enabled when a date is set. On tap it saves, sets `onboardingComplete` and replaces the stack with `[Home]`.

Pickers:
- **Date:** M3 `DatePickerDialog` + `DatePicker`.
  - `selectableDates` = 1900-01-01 … today (device zone). `yearRange = 1900..currentYear`. `showModeToggle = true` (calendar ↔ typed input).
  - Initial selection: the saved date; if none, no selection with the displayed month at January 2000.
  - Convert `selectedDateMillis` with `ZoneOffset.UTC` (M3 stores UTC midnight), never the device zone. Otherwise users west or east of UTC see an off-by-one day.
  - Buttons as M3 `TextButton` in `brand.link`: confirm "Set date" (`birth_date_confirm`, disabled until a day is selected), dismiss "Close calendar" (`birth_date_dismiss`, keeps the previous value). `DatePickerDefaults.colors`: container `bg.raised`; headline/title/day text `text.primary`; weekday and subhead `text.muted`; selected day/year `brand.primary` with `text.on-primary`; today outline `brand.link`; disabled days at 38%.
- **Time:** M3 `TimePicker` in a `BasicAlertDialog`.
  - Surface `bg.raised`, radius from the theme `extraLarge` (32), 24 dp padding. Title "Birth time" (Title M), 16 dp above the picker. `is24Hour = DateFormat.is24HourFormat(context)`.
  - Colours: clock dial `surface.card`, selector `brand.primary`, selected time-selector container `brand.primary` with `text.on-primary`, unselected `surface.card-hi` with `text.primary`.
  - Buttons as above: confirm "Set time" (`birth_time_confirm`), dismiss "Close clock" (`birth_time_dismiss`, keeps the previous value).

States: empty (Continue disabled); date only; date + time; time unknown (field disabled + note); pre-filled. Save failure as in S04. No validation error is reachable, because the picker enforces the range.

### S06 Home (HOME-01, HOME-02, HOME-04, D-11..D-13)

Scrolling column (`verticalScroll`; 7 tiles do not need a lazy list), 16 dp sides, bottom padding 24 dp + navigation inset:

1. **Header** (min 64 dp, 8 dp under the status bar):
   - Greeting (Overline, `text.muted`, `devanagariSafe()`).
   - "[AppName]" (Display L, heading).
   - No menu icon: there is no Settings screen in M1.
   - Long-press on the greeting opens the debug showcase in **debug builds only** (D-03). It has no visual affordance and no semantics action in release.
2. 16 dp, then the **Hero card**:
   - Full width, radius Lg 24, padding 24, `Row` with 8 dp gap.
   - Fill: `gradient.hero-aurora` 135° + the two radial glows (see Color). 1 dp white→transparent gradient border (alpha 0.55 → 0.05, 135°). `GlowPrimary` shadow.
   - Left column (`weight(1f)`, 8 dp gap):
     - Overline row: 16 dp `ic_sparkle` gold + "Palm reading" (Overline, gold). Top-left, over #3420A8.
     - Title "Read your palm" (Display M, `text.on-primary`).
     - Body (Body L, `text.on-primary`).
     - 8 dp, then `NocturneButton(style = OnHero)` "Scan my palm" → HandSelect. **The button is the only click target on the card** (52 dp tall).
   - Right: `illus_guide_orb` 104 dp with `hero_orbit` behind it (decorative, top-aligned).
   - At `fontScale ≥ 1.5` the orb and orbit are hidden, so the text column gets the full width.
   - All hero text stays inside the left column. Never place text over the right ~40% of the card (white on #B04BF0 is 4.1:1).
3. 24 dp, then the **Section header**: 24 dp `ic_sparkle` gold + 8 dp + "Explore your destiny" (Display M, heading).
4. 16 dp, then the **Feature grid** (rows of 2, 16 dp gutters on both axes, tile width (328 - 16) / 2 = 156 dp, each row `IntrinsicSize.Min` so tiles in a row share the same height):
   - Order: Palm reading, Tarot / Horoscope, Birth chart / Love test, Daily report / **Ask [Guide] full width**. The 7th tile spans both columns.
   - `FeatureCard`: radius Lg 24, `gradient.card` 135° fill, foil border (`gradient.foil-border`, 1 dp), `GlowCard`.
   - Illustration slot: 132 dp tall, art fitted in 112 dp, decorative.
   - Title row: padding start 16, top 16, end 8, bottom 16 dp, 8 dp gap; title (Title M, wraps) + 32 dp arrow disc (`surface.card-hi` fill, 1 dp gold → `brand.link` gradient stroke, 16 dp `ic_arrow` gold). The disc is decorative: the whole card is the click target, `Role.Button`.
   - Tiles other than Palm reading get the "Soon" badge: `NocturneChip` (Label M, `text.muted`) on a `surface.card-hi` pill ( 8 dp horizontal / 4 dp vertical padding), 8 dp inset top-start in the slot. It is also folded into the card's content description ("Tarot, coming soon").
   - Palm reading → HandSelect. Every other tile → `ComingSoon(feature)`.

| Tile | Art (vector) | Action |
|------|--------------|--------|
| Palm reading | `illus_constellation_hand` | HandSelect |
| Tarot | `illus_tarot_pair` | ComingSoon |
| Horoscope | `illus_horoscope_star` | ComingSoon |
| Birth chart | `illus_zodiac_wheel` | ComingSoon |
| Love test | `illus_heart_orbit` | ComingSoon |
| Daily report | `illus_sun_medallion` | ComingSoon |
| Ask [Guide] | `illus_guide_orb` | ComingSoon |

**ComingSoon overlay (D-01 route, D-12):**
- Drawn over Home (Home stays visible under the scrim). The planner picks how: an overlay scene, e.g. Nav3 `DialogSceneStrategy` hosting a `ModalBottomSheet`, or an equivalent.
- `ModalBottomSheet`: container `bg.raised`, top corners Xl 32, drag handle 32 x 4 dp `text.muted` @0.5, scrim `surface.scrim` @0.6, `Elevation.Sheet` look.
- Content (24 dp sides, 8 dp top, 24 dp bottom + nav inset, centred, 16 dp gaps):
  - The tile's art at 96 dp (decorative).
  - "Coming soon" (Overline, `text.muted`).
  - Feature name (Display M, heading).
  - Body (Body L, muted).
  - Primary "OK" full width → dismiss (label locked by D-12).
- Dismissed also by Back, scrim tap or drag.

**Exit dialog (D-13):**
- On Back at Home: `BasicAlertDialog`. Surface `bg.raised`, radius 32 (theme `extraLarge`), 1 dp `border.hairline` @0.35, padding 24, gaps 16, width 312 dp (`widthIn(max = 312.dp)`, fills a narrower screen).
- Title "Leave [AppName]?" (Display M). Body (Body L, muted).
- Buttons end-aligned in a `FlowRow` (8 dp gap, wraps to 2 lines at large font): `NocturneButton` **Primary "Stay"** (dismiss) and **Ghost "Leave"** (`activity.finish()`).
- Back or scrim tap = Stay.

Greeting (D-11): the bucket comes from local `LocalTime.now()`. 05:00-11:59 morning, 12:00-16:59 afternoon, 17:00-20:59 evening, else night. The name is always present, since onboarding guarantees it.

States:
- Returning/default (greeting + hero + grid).
- Each greeting bucket (4).
- Coming-soon open (6 features).
- Exit dialog open.
- Long name (30 chars, wraps to 2 lines).
- Hindi (all labels wrap, no truncation).
- No loading state: the profile is already read during splash routing, and the screen renders once the profile `Flow` emits.

### S07 Hand selection (SCAN-01, D-15)

- `NocturneTopBar` (64 dp): 48 dp circular back button (`surface.card` fill, 1 dp hairline @0.35, 24 dp `ic_back`, `contentDescription` "Back") + title "Hand reading" (Display M, heading). No help action.
- Scrolling content (16 dp sides, 16 dp gaps; Figma's 14 dp rounded to the token):
  1. "Which hand should we read?" (Title M).
  2. **Hand choice** (D-15 "Left / Right segmented choice, default Right", drawn as Figma's two cards): `Row`, 2 equal cards, 16 dp gap, `selectableGroup()`.
     - Each `HandOptionCard`: `selectable(role = Role.RadioButton)`, min 48 dp, radius Lg 24, padding 16, 8 dp gap, content centred.
     - Art: `illus_constellation_hand` 92 x 120 dp, mirrored (`scaleX = -1`) for Left.
     - Label row: "Left hand" / "Right hand" (Label L), 8 dp gap. Selected adds a 24 dp `gradient.primary` circle with a 16 dp `ic_check` in `text.on-primary`.
     - Unselected: `surface.card` fill + 1 dp hairline @0.35. Selected: vertical `surface.card-hi` → `surface.card` fill, 2 dp `brand.link` border, `GlowCard`.
     - Semantics merged: "Right hand, selected".
  3. Hint (Caption, muted).
  4. **Photo tips card**: `surface.card`, radius Lg 24, 1 dp hairline @0.3, padding 16, 16 dp gaps.
     - Good row: `tip_good` 56 dp in a 2 dp `status.success` ring with a 20 dp success check badge (12 dp `ic_check`, top-end), 16 dp gap, then a column (4 dp gap): "Good photo" (Title M) + body (Caption, muted).
     - "Avoid these" (Label M, muted).
     - Bad row: 4 equal columns with 8 dp gaps, each a 56 dp `surface.card-hi` disc with the tip art + a 20 dp `status.error` badge with a 12 dp `ic_close` in `text.on-accent` (top-end), then 8 dp and the caption below (Caption, `text.primary`, centred, wraps, no max lines).
     - Order: Too far, Blurry or dark, Fingers closed, Jewellery or bent.
     - Each cell merges semantics into one node: "Avoid: Too far". The good row merges as "Good photo: Whole palm in the frame, fingers open, good light."
  5. Requirement note (Caption, muted).
  6. Privacy line (Caption, muted).
- Bottom CTA "Continue" → `Scan(hand)`, always enabled. The default is **Right** each time the screen opens; the choice is not persisted.
- States: Right selected (default); Left selected; Hindi (captions wrap to 2-3 lines; tip cells top-aligned). The repeat-scan "AD" variant is Phase 6.

### Scan placeholder (D-15)

- **No background** (`showsBackground = false`, opaque `bg.camera`). `NocturneTopBar` with back + title "Hand reading".
- Centred column: "Scanner arrives in the next update" (Title M) + "Hand: Right" / "Hand: Left" (Body L, muted).
- Back → HandSelect. Strings go through resources too (debug wording is allowed, hardcoded text is not).

---

## Copywriting Contract

Brand "[AppName]" and persona "[Guide]" stay literal placeholders (single `app_name` resource; `[Guide]` stays inline until REL-03). All strings live in `res/values/strings.xml` and `res/values-hi/strings.xml` (D-06). The Hindi is Claude-written and is flagged for native-speaker review in STATE.md. Placeholders are positional (`%1$s`, `%1$d`).

| Element | Copy |
|---------|------|
| Primary CTA (phase) | "Scan my palm" (Home hero → hand selection). S02 "Continue in English" / "हिंदी में आगे बढ़ें". Picker actions "Set date" / "Close calendar" and "Set time" / "Close clock" |
| Locked labels (not verb + noun, kept by decision) | "Continue" on S04, S05, S07 (D-09, D-10, D-15); "Next" and "Get started" on S03 (D-08); "OK" on the coming-soon sheet (D-12); "Stay" / "Leave" in the exit dialog (D-13) |
| Empty state heading | S04 empty name: placeholder "e.g. Priya" + helper "Used only to personalise your readings."; Continue disabled |
| Empty state body | S05 no date yet: "Choose a date" in the date field; Continue disabled until a date is picked |
| Error state | Name: "Enter at least 2 letters." / "Use letters only. Spaces, - and ' are fine." Save failure: "Couldn't save. Please try again." (stays on the step, Continue retries) |
| Destructive confirmation | None in Phase 3. Leaving the app is a confirm, not destructive: "Leave [AppName]?" / "Your profile stays saved on this phone. Come back any time." [Stay] (primary) [Leave] |
| Coming soon | "Coming soon" + feature name + "We're still working on this one. Your palm reading is ready today." [OK] |

### String table (keys, English, Hindi)

| Key | English | Hindi |
|-----|---------|-------|
| `common_continue` | Continue | आगे बढ़ें |
| `common_next` (intro pages 1-2, D-08) | Next | आगे |
| `common_ok` (coming-soon sheet only, D-12) | OK | ठीक है |
| `common_back` (content description) | Back | वापस |
| `common_save_failed` | Couldn't save. Please try again. | सेव नहीं हो सका। कृपया फिर से कोशिश करें। |
| `common_selected` / `common_not_selected` | Selected / Not selected | चुना गया / नहीं चुना गया |
| `language_title` | Choose your language | अपनी भाषा चुनें |
| `language_subtitle` | Read [AppName] in English or Hindi. | [AppName] को अंग्रेज़ी या हिंदी में इस्तेमाल करें। |
| `language_cta` | Continue in English | हिंदी में आगे बढ़ें |
| `language_name_en` / `language_name_hi` (`translatable="false"`) | English / हिन्दी | English / हिन्दी |
| `intro_1_title` | Your palm, your story | आपकी हथेली, आपकी कहानी |
| `intro_1_body` | Show your palm to the camera and get a reading of your heart, head and life lines. | कैमरे को अपनी हथेली दिखाइए और पाइए अपनी हृदय रेखा, मस्तिष्क रेखा और जीवन रेखा की रीडिंग। |
| `intro_2_title` | Your stars, day by day | आपके सितारे, दिन-ब-दिन |
| `intro_2_body` | Horoscope, tarot and your birth chart are on the way. Your birth date is all they need. | राशिफल, टैरो और आपकी जन्म कुंडली जल्द आ रहे हैं। इनके लिए बस आपकी जन्म तिथि चाहिए। |
| `intro_3_title` | Meet [Guide] | [Guide] से मिलिए |
| `intro_3_body` | Soon you can ask [Guide] about love, work and what lies ahead. | जल्द ही आप [Guide] से प्यार, काम और आने वाले कल के बारे में पूछ सकेंगे। |
| `intro_get_started` | Get started | शुरू करें |
| `intro_page_cd` | Page %1$d of %2$d | पेज %1$d, कुल %2$d |
| `onboarding_step` | Step %1$d of %2$d | चरण %1$d / %2$d |
| `name_title` | What should we call you? | हम आपको किस नाम से पुकारें? |
| `name_label` | Your first name | आपका पहला नाम |
| `name_placeholder` | e.g. Priya | जैसे, प्रिया |
| `name_helper` | Used only to personalise your readings. | सिर्फ़ आपकी रीडिंग को आपके हिसाब से बनाने के लिए। |
| `name_error_short` | Enter at least 2 letters. | कम से कम 2 अक्षर लिखें। |
| `name_error_chars` | Use letters only. Spaces, - and ' are fine. | सिर्फ़ अक्षर लिखें। स्पेस, - और ' चल जाएँगे। |
| `gender_title` | Gender | लिंग |
| `gender_hint` | Optional. Only used to word your readings. | वैकल्पिक। सिर्फ़ रीडिंग की भाषा चुनने के लिए। |
| `gender_male` / `gender_female` / `gender_other` | Male / Female / Other | पुरुष / महिला / अन्य |
| `birth_title` | When were you born? | आपका जन्म कब हुआ था? |
| `birth_body` | Your birth date shapes your horoscope and birth chart. | आपकी जन्म तिथि से आपका राशिफल और जन्म कुंडली बनती है। |
| `birth_date_label` | Birth date | जन्म तिथि |
| `birth_date_placeholder` | Choose a date | तारीख़ चुनें |
| `birth_time_label` | Birth time (optional) | जन्म का समय (वैकल्पिक) |
| `birth_time_placeholder` | Choose a time | समय चुनें |
| `birth_date_confirm` | Set date | तारीख़ सेट करें |
| `birth_date_dismiss` | Close calendar | कैलेंडर बंद करें |
| `birth_time_dialog_title` | Birth time | जन्म का समय |
| `birth_time_confirm` | Set time | समय सेट करें |
| `birth_time_dismiss` | Close clock | घड़ी बंद करें |
| `birth_time_unknown` | I don't know my birth time | मुझे अपने जन्म का समय नहीं पता |
| `birth_time_unknown_note` | No problem. Readings that use your birth time will be a little less exact. | कोई बात नहीं। जिन रीडिंग में जन्म का समय लगता है, वे थोड़ी कम सटीक होंगी। |
| `home_greeting_morning` | Good morning, %1$s | सुप्रभात, %1$s |
| `home_greeting_afternoon` | Good afternoon, %1$s | नमस्ते, %1$s |
| `home_greeting_evening` | Good evening, %1$s | शुभ संध्या, %1$s |
| `home_greeting_night` | The stars are out, %1$s | तारे निकल आए हैं, %1$s |
| `home_hero_overline` | Palm reading | हस्तरेखा |
| `home_hero_title` | Read your palm | अपनी हथेली पढ़वाएँ |
| `home_hero_body` | Point your camera at your palm and see your heart, head and life lines. | कैमरा अपनी हथेली पर रखें और देखें अपनी हृदय, मस्तिष्क और जीवन रेखा। |
| `home_hero_cta` | Scan my palm | हथेली स्कैन करें |
| `home_explore` | Explore your destiny | अपनी किस्मत जानिए |
| `home_soon_badge` | Soon | जल्द |
| `home_tile_soon_cd` | %1$s, coming soon | %1$s, जल्द आ रहा है |
| `feature_palm` | Palm reading | हस्तरेखा |
| `feature_tarot` | Tarot | टैरो |
| `feature_horoscope` | Horoscope | राशिफल |
| `feature_birth_chart` | Birth chart | जन्म कुंडली |
| `feature_love_test` | Love test | लव टेस्ट |
| `feature_daily_report` | Daily report | दैनिक रिपोर्ट |
| `feature_ask_guide` | Ask [Guide] | [Guide] से पूछें |
| `coming_soon_overline` | Coming soon | जल्द ही |
| `coming_soon_body` | We're still working on this one. Your palm reading is ready today. | इस पर अभी काम चल रहा है। आपकी हस्तरेखा रीडिंग आज ही तैयार है। |
| `exit_title` | Leave [AppName]? | [AppName] से बाहर जाएँ? |
| `exit_body` | Your profile stays saved on this phone. Come back any time. | आपकी प्रोफ़ाइल इसी फ़ोन पर सेव रहेगी। जब चाहें लौट आइए। |
| `exit_stay` | Stay | रुकें |
| `exit_leave` | Leave | बाहर जाएँ |
| `hand_title` | Hand reading | हस्तरेखा रीडिंग |
| `hand_question` | Which hand should we read? | कौन-सा हाथ पढ़ें? |
| `hand_left` / `hand_right` | Left hand / Right hand | बायाँ हाथ / दायाँ हाथ |
| `hand_hint` | Many palm readers use your dominant hand for the present and future. | कई हस्तरेखा विशेषज्ञ वर्तमान और भविष्य के लिए आपका मुख्य हाथ देखते हैं। |
| `tips_good_title` | Good photo | अच्छी फ़ोटो |
| `tips_good_body` | Whole palm in the frame, fingers open, good light. | पूरी हथेली फ़्रेम में, उंगलियाँ खुली, अच्छी रोशनी। |
| `tips_bad_title` | Avoid these | इनसे बचें |
| `tips_bad_far` | Too far | बहुत दूर |
| `tips_bad_blurry` | Blurry or dark | धुंधली या अंधेरी |
| `tips_bad_fingers` | Fingers closed | उंगलियाँ बंद |
| `tips_bad_jewellery` | Jewellery or bent | गहने या मुड़ा हाथ |
| `tips_bad_cd` | Avoid: %1$s | इससे बचें: %1$s |
| `hand_requirement` | Use the back camera in good light, and take off rings if you can. | अच्छी रोशनी में पीछे वाला कैमरा इस्तेमाल करें, और हो सके तो अंगूठी उतार दें। |
| `hand_privacy` | Your photo is only used to make your reading and is deleted right after. | आपकी फ़ोटो सिर्फ़ रीडिंग बनाने में इस्तेमाल होती है और उसके तुरंत बाद मिटा दी जाती है। |
| `scan_placeholder_title` | Scanner arrives in the next update | स्कैनर अगले अपडेट में आएगा |
| `scan_placeholder_hand` | Hand: %1$s | हाथ: %1$s |

Copy rules: no claims of accuracy, no medical, legal or financial wording (PRD §11). Do not promise features that M1 lacks (no "in Settings", no "chat now"). Sentence case everywhere, no all-caps strings (the Overline style tracks letters but does not uppercase). No emoji.

---

## Assets (vector drawables, our Figma art only)

**Never use reference-app assets or anything under `_extract/`.**
- Source: Figma "1 · Foundations" `Illustration/*` and `Icon/*` components (file `Ej98dV7fZQUhRwUyfQIlyJ`). Export SVG through the Figma MCP when it is authenticated, or have the user export it manually.
- Fallback source: the SVG markup the 2026-11-26 build scripts passed to `createNodeFromSvg` (transcript lines 493 and 1170). Those scripts are our own art.
- Convert to `res/drawable/*.xml` VectorDrawable. Radial and linear gradients go in `aapt:attr`, which works at minSdk 26.
- Total size across all Phase 3 drawables < 150 KB. Art is decorative (`contentDescription = null`) unless stated otherwise.
- If an export is impossible, hand-author a simplified vector in the same palette under the **same name**, so the later swap is asset-only (as with the Phase 1 launcher orb, D-09).

| Drawable | Figma source | Used in | Size |
|----------|--------------|---------|------|
| `illus_guide_orb` | Illustration/[Guide] orb | Hero (104), intro 1 accent (48), intro 3 (160), Ask [Guide] tile (112), coming-soon (96) | square |
| `hero_orbit` | Hero card "orbit" layer (dashed ellipses rx 104/80, ry 40/28, rotate -14°, white @0.22/0.12, dash 3/6) | Hero, behind the orb, rotating | 220 x 140 dp viewport |
| `illus_constellation_hand` | Illustration/Constellation hand | Intro 1 (160 x 208), Palm tile (112), hand cards (92 x 120, mirrored for Left) | portrait |
| `illus_zodiac_wheel` | Illustration/Zodiac wheel | Intro 2 (176), Birth chart tile | square |
| `illus_tarot_pair` | Illustration/Tarot pair | Tarot tile | — |
| `illus_horoscope_star` | Illustration/Horoscope star | Horoscope tile | — |
| `illus_heart_orbit` | Illustration/Heart orbit | Love test tile | — |
| `illus_sun_medallion` | Illustration/Sun medallion | Daily report tile | — |
| `tip_good`, `tip_too_far`, `tip_blurry_dark`, `tip_fingers_closed`, `tip_jewellery_bent` | **New art, not in Figma** (Figma S07 used one generic hand icon for every tip). Drawn in the `Icon/hand` + Constellation hand style. Good: open palm filling a dashed frame. Too far: small palm in a large frame. Blurry/dark: palm with dimmed fill and blur strokes. Fingers closed: fingers together. Jewellery/bent: palm with a ring and a bent wrist | S07 tips | 56 dp |

Icons (`ic_*`, 24 dp, viewport 24, `strokeWidth` 1.8, round cap/join, `strokeColor` `@color/nocturne_text_primary`, tinted at use). The `pathData` values are the Figma `Icon/*` paths verbatim (SVG path syntax is valid in `pathData`). A later duotone export may replace them under the same names.

| Drawable | pathData |
|----------|----------|
| `ic_back` | `m15 18-6-6 6-6` (start with `M15 18 L9 12 L15 6` if the relative form is rejected) |
| `ic_check` | `M5 12 L10 17 L19 7` |
| `ic_close` | `M6 6l12 12M18 6 6 18` |
| `ic_chevron_down` | `m6 9 6 6 6-6` |
| `ic_arrow` | `M5 12h14M13 6l6 6-6 6` |
| `ic_alert` | `M12 3 2 20h20zM12 10v4M12 17.2v.1` |
| `ic_sparkle` | `M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8zM19 16l.7 1.6 1.6.7-1.6.7L19 20.6l-.7-1.6-1.6-.7 1.6-.7z` |

The splash icon stays `ic_launcher_foreground` (Phase 1 orb stand-in).

---

## Accessibility

- **Touch targets ≥ 48 dp:** buttons 52, fields/rows/tiles 56, checkbox row 48, back button 48. Hand cards and feature tiles are larger. Page dots are not interactive.
- **Semantics:**
  - Screen titles use `heading()`.
  - Radios and selectable tiles sit inside `selectableGroup()` with the right `Role`.
  - The checkbox row is `toggleable`.
  - Icon-only back button: `contentDescription` = `common_back`.
  - Illustrations are decorative (`null`). Tip cells and hand cards merge descendants with the descriptions above. The feature tile description includes "coming soon".
  - Page indicator: `intro_page_cd`.
  - The field error is announced: the supporting text is in the field's semantics, plus `error(...)`.
- **Contrast:** see the Color table. Every text pair is ≥ 4.5:1 within the placement rules; non-text indicators are ≥ 3:1.
- **Font scaling:** everything in sp, no fixed-height text containers, no truncation.
  - Verify at font scale 1.0, 1.3 and 2.0 (`adb shell settings put system font_scale 2.0`) on the Redmi.
  - At 2.0 every screen scrolls to its CTA. The hero hides the orb (`fontScale ≥ 1.5`). Dialog buttons wrap.
- **Devanagari:** font-metric line heights (`devanagariSafe()`), no clipped matras or conjuncts, in both locales (see Typography).
- **Reduce motion:** `ANIMATOR_DURATION_SCALE == 0` or battery saver → static background, no transitions, no entry stagger, no press scale (see the Motion table).
- **TalkBack order** follows the visual top-to-bottom order. The Home long-press debug hook adds no TalkBack action in release.

---

## Figma vs locked decisions (decision wins)

| Screen | Figma (2026-11-26 build) | Built in Phase 3 | Why |
|--------|--------------------------|------------------|-----|
| S01 | Composed splash: orb, wordmark, tagline, spinner, "Reading the stars…", ad and offline states | System splash, orb icon on `bg.base` only | D-02 (no artificial delay, no ads or remote config) |
| S01 motion | Stars fade → orb scale → wordmark | Splash exit fade 200 ms | D-02 |
| S02 | 5 languages, native ad, "change it any time in Settings" | en + hi only, no ad, no Settings promise | ONB-02; ads are Phase 6; no Settings in M1 |
| S03 | Native-ad variants; page 2 promotes chat now | No ads; pages follow D-08 (palm, astrology, [Guide]) with honest "soon" copy | D-08, PRD §11 (no misleading claims) |
| S04 | "Next" + native ad; 104 dp choice tiles | "Continue", no ad; 56 dp label-only tiles (Figma's screen hides the icons too) | D-09 |
| S05 | Wheel pickers; "Start my reading"; "We'll use 12:00 noon" | M3 DatePicker/TimePicker dialogs; "Continue"; neutral time-unknown note | D-10; D-04 stores no default time |
| S06 hero | "Ask [Guide]" chat hero | Palm-scan hero → HandSelect | D-12 |
| S06 grid | 8 tiles (incl. Moon sign, Soul energy, Vedic chart) with "New" badges; native ads; What's new sheet; "Welcome / Start here" first visit; menu icon | D-12's 7 tiles (Ask [Guide] full width), muted "Soon" badge, no ads, no What's new, time-of-day greeting only, no menu | D-11, D-12, deferred list |
| S06 exit | "Stay" ghost + "Exit" primary + native ad | "Stay" primary + "Leave" ghost, no ad | D-13 labels; the safe action is primary |
| S07 | "Scan my palm" + camera icon; tips "Cut off, Fingers closed, Bent, Jewellery" as generic hand icons; help icon; repeat-scan AD variant | "Continue"; D-15's 4 tips with distinct new art; no help; no AD variant; privacy line added | D-15; FEATURES.md privacy notice; Phase 6 |
| Tokens | Dialog radius 28, chip radius 20, 1.5 dp list stroke, several literal art colours | Theme `extraLarge` 32, Md 16, 2 dp selected stroke, mapped colours (see Color) | Tokens are the source of truth for chrome |
| Background | Parallax from scroll offset and device tilt (max 6 dp) | Drift-amplitude parallax only | Low-end jank and battery risk; D-14 does not require it |
| Buttons | Leading icons (arrow on Next, camera, sparkle) | No button icons in Phase 3 | Smallest change; `NocturneButton` has no icon slot |
| Type scale | 7 sizes (32/26/17/16/15/13/12) and 4 weights (400/500/600/700) | 4 sizes (32/26/15/13) and 2 weights (500/600) via 4 token edits (see Typography) | UI checker limit of 4 sizes and 2 weights. Update the Figma text styles to match at the visual QA pass |
| Spacing | 6 dp dot gap, 10 dp badge inset, 12 dp gutters and gaps, 14/12/10/14 tile title padding, 20 dp hero padding | 8, 8, 16 (gender gap and tip columns 8), 16/8/16/16, 24 | UI checker requires the 4/8/16/24/32/48/64 set |
| Picker buttons | M3 default "OK" / "Cancel" | "Set date" / "Close calendar", "Set time" / "Close clock" | Verb + noun labels |
| S02 CTA | "Next" | "Continue in English" / "हिंदी में आगे बढ़ें" | Verb + object, and it confirms the selected language |

---

## UI Considerations

Applicable state considerations resolved: 12 covered, 5 backstop, 4 unresolved.

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| empty | S04 name field (form) | ✅ covered | An empty name shows the placeholder and helper, no error, and Continue disabled (Copywriting: empty state) |
| empty | S05 birth date field (form) | ✅ covered | With no date picked, the field shows "Choose a date" and Continue is disabled. The time is optional and its empty state is valid |
| partial | S02-S05 incomplete onboarding (form) | ✅ covered | A restart after an incomplete onboarding begins at Language with every saved value pre-filled (D-04) |
| partial | S05 time unknown (form) | ✅ covered | "I don't know my birth time" clears and disables the time field and shows the neutral note |
| error | S04 name validation (form) | ✅ covered | Non-empty invalid input shows the matching error after blur or Done. A valid value clears it. Empty never errors |
| error | S04/S05 DataStore write failure (form) | 🧪 backstop | A failed save shows "Couldn't save. Please try again." above the CTA and keeps the user on the step (ViewModel test with a throwing repository) |
| error | S01 DataStore read failure (nav) | 🧪 backstop | An IOException on the first read routes to Language as an empty profile, never to a crash (routing-decision unit test) |
| loading | S01 splash (nav) | ✅ covered | The system splash stays up until the first profile read returns; then the stack is `[Home]` or `[Language]` with no visible intermediate frame |
| loading | S06 Home profile (static-content) | ✅ covered | Home renders once the profile Flow emits. No skeleton, because splash routing already waited for the same read |
| populated | S06 feature grid (list-collection) | ✅ covered | The grid always shows the 7 D-12 tiles in fixed order; Palm opens HandSelect, the other 6 open the coming-soon sheet |
| zero-one-many | S06 feature grid (list-collection) | ✅ covered | The count is fixed at 7 (odd): 3 rows of 2, then Ask [Guide] full width. No zero or one case exists |
| overflow | S02-S07 screens (static-content) | 🧪 backstop | At font scale 2.0 and with the keyboard open, every screen scrolls and its CTA stays reachable above the navigation bar or IME (Redmi screenshots at 1.0 / 1.3 / 2.0) |
| long-text | S06 greeting with a 30-char name; Hindi tile titles, tip captions, button labels (static-content, interactive-control) | 🧪 backstop | Text wraps, never truncates, and containers grow (`heightIn(min)` only). Checked in `hi` at font scale 1.3 on device |
| long-text | Devanagari line height (static-content) | 🧪 backstop | Hindi UI and Devanagari names in the English UI render without clipped matras or conjuncts (screenshot check, as in Phase 1 FND-02) |
| nav | Back behaviour (nav) | ✅ covered | Back follows D-01/D-13 exactly: Language exits, steps go back one, Home opens the exit dialog, an open sheet or dialog dismisses first, and there is no Back into onboarding after finishing |
| reduced-motion | Background, transitions, press, Home stagger (interactive-control) | ✅ covered | With animator scale 0 or battery saver: static star field (rest state), no screen transitions, no stagger, no press scale |
| i18n | S02 language switch (interactive-control) | ✅ covered | Picking a language re-renders the UI in that language at once, persists it, and survives an activity recreate on API 26-32 without leaving Language |
| unresolved | Hindi copy | ⚠ unresolved | All Hindi is Claude-written; it needs native-speaker review before release (STATE.md flag). The planner treats the strings as provisional |
| unresolved | Figma visual QA | ⚠ unresolved | Values come from the Figma build scripts, not live frames. Compare device screenshots with the S01-S07 frames once Figma access works. Manual Figma edits after 2026-11-26 may differ |
| unresolved | Illustration export | ⚠ unresolved | The vector drawables need a Figma SVG export (MCP auth or a manual export). The 5 tip drawables are new art. Hand-authored stand-ins under the final names are acceptable for M1 |
| unresolved | M3 picker theming and the TimePicker dialog API on the pinned BOM | ⚠ unresolved | The pinned material3 is 1.4.0 (BOM 2026.09.00). `TimePicker` goes in a `BasicAlertDialog` so the code does not depend on a dialog wrapper that may be experimental or missing in 1.4.0. The `ModalBottomSheet` animation cannot be set to the Figma emphasized curve. Accept the M3 defaults if theming gaps appear; keyboard focus uses the M3 ripple focus indication rather than the Figma 2 dp gold ring |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (no shadcn / component registry on Android) | none | not applicable. New Gradle artifacts (datastore-preferences 1.3.0, core-splashscreen 1.2.0, possibly appcompat, test libs) go through the D-18 `checkpoint:human-verify` consent task instead |

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
