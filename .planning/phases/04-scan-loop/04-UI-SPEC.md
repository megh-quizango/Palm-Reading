---
phase: "4"
slug: "scan-loop"
status: draft
shadcn_initialized: false
preset: none
created: "2026-11-27"
---

# Phase 4: Scan Loop, UI Design Contract

> Visual and interaction contract for the S08 scanner (full-bleed preview, palm-outline guide, guidance chip, capture/torch/gallery/back controls), the camera permission screens (denied, permanently denied), the camera-unavailable gallery fallback, the processing state, the photo rejection dialog and the `ScanReady` placeholder. Written by gsd-ui-researcher, to be verified by gsd-ui-checker.

**The Figma MCP was not available in this session.** This researcher had no ToolSearch and no Figma tools, so no live `get_metadata`, `get_screenshot`, `get_design_context` or `get_variable_defs` calls were made against file `Ej98dV7fZQUhRwUyfQIlyJ`. As in Phase 3, the Figma values were rebuilt from **our own `use_figma` build scripts of the 2026-11-26 design session**, kept in the local transcript `172eeb1d-…cb.jsonl`:
- Line 875: "3 · Screens", section "Palm reading (S08–S10)". It holds S08 Layout A states (Back of hand, Open hand, Too far, Hold still, Palm detected, Capture failed), Camera denied, Camera unavailable and **S08 Layout B (full bleed)**.
- Line 709: "2 · Components". It holds `IconButton` Style=Camera (52 dp, white @0.10) and `Scan guidance chip` (Neutral / Warning / Success / Error).
- Line 744: `State block` (Permission denied, Error).
- Line 494: `Icon/*` paths.
- Line 1303: "5 · Motion" rows for the scan guidance chip and the S08 sequence.

No later Figma call touched S08 (the transcript ends at line 1365 with an unrelated fix). The values are exact for what the scripts built. Manual Figma edits made after 2026-11-26 are **not** captured. Every value below is mapped onto `design-system/nocturne.tokens.json` and the generated `NocturneTokens.kt`. A visual QA pass against the real frames is still owed (see UI Considerations).

**Precedence:** the locked decisions D-01..D-17 in `04-CONTEXT.md` come first. Figma comes second, then the Phase 3 conventions in `03-UI-SPEC.md`. Where they disagree, see "Figma vs locked decisions".

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none (shadcn does not apply: native Android, Jetpack Compose) |
| Preset | not applicable |
| Component library | In-repo "Nocturne" design system (`app.palm.core.designsystem`) on Material 3 primitives (`androidx.compose.material3` 1.4.0 via Compose BOM 2026.09.00, as pinned in `gradle/libs.versions.toml`) |
| Icon library | Custom vector drawables from Figma `Icon/*` (24 dp grid, 1.8 dp stroke, round caps and joins, `strokeColor` `@color/nocturne_text_primary`, tinted at use). Same format as the existing `ic_back.xml`. No `material-icons-extended` |
| Font | Cormorant Garamond SemiBold 600 (display), Manrope 500/600 (UI), with Noto Devanagari fallback (`NocturneFonts`). All bundled |
| Tokens | `NocturneTokens.kt`, generated from `nocturne.tokens.json` by `tools/gen_tokens.py`. **Phase 4 adds no token and changes no token.** Alpha values (0.72, 0.6, 0.45, …) are applied with `.copy(alpha = …)` on token colours |
| Camera surface | CameraX `PreviewView` (`ImplementationMode.PERFORMANCE`, `ScaleType.FILL_CENTER`) in `AndroidView` (D-03). Compose draws every overlay above it |

---

## Component Inventory

Enumerated by `rg -U "@Composable\s*\n\s*fun \w+" android/app/src/main/java/app/palm/core/designsystem` — 11 public composables — app.palm.core.designsystem@e505cd3 (in-repo, git HEAD) — 2026-11-27.
Material 3 primitives: resolved by `curl -s https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom | grep -A1 '<artifactId>material3</artifactId>'` — androidx.compose.material3:material3@1.4.0 — 2026-11-27. M3 is not listed as a table. Any M3 1.4.0 composable may be used (Phase 4 needs `CircularProgressIndicator`, `BasicAlertDialog`, `Icon`, `Text`).
Filesystem cross-check: `ls android/app/src/main/java/app/palm/core/designsystem/components/*.kt | wc -l` = 6 files (Buttons, Chips, ChoiceTile, FeatureCard, TextFields, TopBar). They hold 9 of the 11 composables. The other 2 are `NocturneTheme` and `NocturneBackground`. No mismatch.

This table is a **non-exhaustive** list of known-good components, not a closed allowlist.

### Existing (reuse as-is)

| Component | Import path | Phase 4 use |
|-----------|-------------|-------------|
| `NocturneTheme`, `LocalNocturne` (`type`, `reduceMotion`) | `app.palm.core.designsystem` | `reduceMotion` drives every reduced-motion branch below. `type` gives all text styles. `devanagariSafe()` applies in `hi` |
| `NocturneButton` + `NocturneButtonStyle` (Primary, Ghost) | `app.palm.core.designsystem.components` | Permission, fallback, dialog and ScanReady actions. No change |
| `NocturneChip` | `app.palm.core.designsystem.components` | Hand label ("Right hand") in the scanner top row. Non-interactive. No change |
| `NocturneTopBar` | `app.palm.core.designsystem.components` | Permission and fallback screens, ScanReady. No change |
| `nocturneGlow`, `nocturneBrush`, `NocturneGradientAngles`, `NocturneBorderWidth` (1 dp), `NocturneSelectedBorderWidth` (2 dp) | `app.palm.core.designsystem` (Effects.kt) | Shutter fill and glow, borders |
| `NocturneBackground` | `app.palm.core.designsystem` | **Not shown on Scan** (`Scan.showsBackground = false` stays). It does show behind `ScanReady` (default `true`) |
| `FeatureCard`, `NocturneTextField`, `NocturneSelectField`, `NocturneChoiceTile`, `AdChip` | — | Not used in Phase 4 |

### New in `core/designsystem/components` (shared; Phase 5/6 reuse them for offline/busy/invalid states)

| Component | File | Built on | Used by |
|-----------|------|----------|---------|
| `NocturneStateBlock(icon: Painter, tone: Color, title, body, primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>? = null)` | `StateBlock.kt` | `Column`, centred. Layout in "Permission and fallback screens" | Permission denied, permanently denied, camera unavailable, ScanReady "photo gone" |
| `NocturneDialog(title, body, confirm: Pair<String, () -> Unit>, dismiss: Pair<String, () -> Unit>? = null, onDismissRequest)` | `Dialog.kt` | The `ExitDialog` recipe moved to the design system (M3 `BasicAlertDialog`, `bg.raised`, theme `extraLarge` radius 32, 1 dp hairline @0.35, 24 dp padding, 16 dp gaps, `widthIn(max = 312.dp)`, end-aligned `FlowRow` with 8 dp gaps, the same enter animation) | Photo rejection dialog. `ExitDialog` can keep its own copy until someone touches it. Do not refactor it in this phase |

### Screen-local (`feature/scan`, one screen only)

| Component | Notes |
|-----------|-------|
| `ScannerScreen` | Hosts the preview, the overlays and the controls. Replaces `feature/handselect/ScanPlaceholderScreen.kt`, which is deleted along with its two strings |
| `GuidanceChip` | See "Guidance chip" |
| `PalmGuideOverlay` | Guide outline, corners, scan line and focus ring, drawn in one `Canvas` |
| `ShutterButton` | 76 dp. Disabled / Enabled / Busy states |
| `CameraIconButton` | 52 dp round button for Back, Gallery and Torch |
| `ProcessingOverlay` | Full-screen scrim with a spinner and a label |
| `ScanReadyScreen` | Placeholder route (D-01) |
| Debug HUD | **Debug builds only** (D-09): raw state, per-check scores and a "Record landmarks" toggle. Exempt from this contract (any Caption text on a scrim) and absent from release |

---

## Spacing Scale

From `NocturneSpace`. **Phase 4 uses only 4, 8, 16, 24 and 32 dp for padding, gaps and insets.** No exceptions. Figma's 10, 12, 14, 20 and 28 dp values are rounded onto the scale (see "Figma vs locked decisions"). S2, S12 and S20 exist as tokens, but Phase 4 code must not use them.

| Token | Value | Usage in Phase 4 |
|-------|-------|------------------|
| S4 | 4 dp | Shutter ring-to-disc gap. Gap between the chip message and the hint line |
| S8 | 8 dp | Top-row top padding. Top row → chip gap. Chip vertical padding and icon gap. Title → body in the state block. Button gap. ScanReady meta line gap |
| S16 | 16 dp | Screen side padding. Chip horizontal padding. Control-bar top padding. Guide clearance from the chip and the bar. Corner-box inset around the guide. Gap between the spinner and the label. Photo → meta gap on ScanReady |
| S24 | 24 dp | Control-bar bottom padding (above the navigation-bar inset). State block: disc → title, body → buttons. Dialog padding. Screen top padding under the top bar |
| S32 | 32 dp | Control-bar side padding |
| S48 | 48 dp | Minimum touch target (`NocturneSize.TouchMin`) |

**Component and art sizes (dimensions, not spacing; all multiples of 4):**
- Controls: shutter 76 dp (4 dp ring, 4 dp gap, 60 dp disc). Camera icon buttons 52 dp (Figma "camera controls 52"). `NocturneTopBar` 64 dp with a 48 dp back button. Buttons 52 dp tall. Dialog max width 312 dp.
- Icons: 24 dp in buttons and the shutter, 20 dp in the guidance chip, 40 dp in the state-block disc.
- Art: state-block disc 96 dp. Focus ring 64 dp. Corner brackets with 32 dp arms. Spinner 48 dp (processing) / 24 dp (shutter busy). ScanReady landmark dots 8 dp.
- Strokes: guide outline 2 dp (dash 8 on / 8 off), corner brackets 4 dp, scan line 2 dp, focus ring 2 dp, contrast halo 4 dp (guide, corners, focus ring), shutter ring 4 dp, 1 dp hairlines.
- Radii: `NocturneRadius.Lg` 24 (guidance chip, ScanReady photo), `Xl` 32 (control-bar top corners, dialog via theme `extraLarge`), corner-bracket bend 8 dp (`Sm`), pill/circle for buttons and the shutter.

---

## Typography

All styles come from `LocalNocturne.current.type`. Never set `fontSize` or `fontWeight` inline. **Phase 4 renders 3 sizes (26, 15, 13 sp) and 2 weights (500, 600).** No new styles and no token changes.

| Size | Token | Size / line height | Weight | Used for |
|------|-------|--------------------|--------|----------|
| 26 | Display M | 26 / 32 sp | 600 (Cormorant) | `NocturneTopBar` titles ("Scan your palm", "Your palm photo"), state-block titles, dialog titles |
| 15 | Title M | 15 / 22 sp | 600 (Manrope) | **Guidance chip message**, processing label, ScanReady source line |
| 15 | Label L | 15 / 20 sp | 600 (Manrope) | `NocturneButton` labels |
| 15 | Body L | 15 / 22 sp | 500 (Manrope) | State-block body, dialog body |
| 13 | Label M | 13 / 18 sp | 600 (Manrope) | Hand label chip (`NocturneChip`) |
| 13 | Caption | 13 / 18 sp | 500 (Manrope) | Wrong-hand hint line in the chip, ScanReady meta line |

Rules:
- **Devanagari:** the `hi` locale already swaps the whole theme to `devanagariSafe()` (font-metric line height, 0 letter spacing). Text containers use `heightIn(min = …)` only, never a fixed height. No `maxLines` on any Phase 4 text. Everything wraps.
- The guidance chip message uses Title M, not Figma's Label M 13 sp: it is read at arm's length over a moving preview.
- Display L (32) is not rendered in Phase 4. The scanner has no visible title (it has a pane title for TalkBack, see Accessibility).

---

## Color

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | The live camera image over `bg.camera` #05040E | Scanner (full bleed). `bg.camera` is the fill before the first frame and the opaque fill of the permission/fallback screens (Scan keeps `showsBackground = false`) |
| Secondary (30%) | `surface.scrim` #000000 @0.72 (floating camera chrome) and @0.6 (control bar, top gradient); `surface.card` #1C1645 (state-block disc); `bg.raised` #1A1240 (dialog); `surface.card-hi` #2A2160 (hand label chip) | Chip, back button, control bar, processing overlay, state discs, dialog |
| Accent (10%) | `accent.gold` #F2C66D | Only the elements listed below |
| Brand / interactive | `gradient.primary` (#5B3FE6 → #7A4EF2, 90°) + `GlowPrimary`; `brand.link` #A99BFF | Enabled shutter disc, Primary buttons. `brand.link` for corner brackets and the scan line while searching, spinners and ScanReady landmark dots |
| Status | `status.success` #6EE7B7, `status.warning` #FFD27A, `status.error` #FF8A8A | Success: PalmDetected chip icon/border, enabled shutter ring, guide outline and corners when detected. Warning: BackOfHand / ClosedHand / TooFar chip icon and border, wrong-hand hint icon, permission state discs. Error: capture-failed and guide-unavailable chip, camera-unavailable disc |
| Destructive | `status.error` #FF8A8A | No destructive button in Phase 4 (Retake deletes only a photo the user can retake at once; see Copywriting). Error tone only |

**Accent (gold) reserved for:** (1) the torch toggle's **on** state (disc fill `accent.gold`, icon `text.on-accent`); (2) `focus.ring` (2 dp) for keyboard/D-pad focus where a custom indication is drawn. Not for the shutter, the chip, the guide or any text.

**Camera chrome rule.** Anything drawn over the live preview sits on its own scrim, because the scene behind it can be anything from a black room to a white wall:
- Floating chrome (guidance chip, back button, processing overlay): `surface.scrim` @0.72.
- Control bar and top gradient: `surface.scrim` @0.6.
- Overlay strokes (guide, corners, focus ring): drawn twice, first as a 4 dp `surface.scrim` @0.35 halo, then as the coloured stroke on top.
- Text over the camera is always `text.primary`. Status colours are used only for icons and borders.

**Contrast (WCAG 2.x, computed 2026-11-27 with the worst case: a pure white scene behind the scrim):**

| Pair | Ratio | Verdict |
|------|-------|---------|
| text.primary on scrim @0.72 over white (#474747) | 8.35 | pass. Figma's black @0.5 gives 3.55 (fail), so the scrim alpha is raised |
| status.warning / success / error icon on scrim @0.72 over white | 6.53 / 6.10 / 4.09 | pass (≥ 3:1 for non-text) |
| text.primary icon on control bar (scrim @0.6 over white, #666666) | 5.16 | pass |
| brand.link stroke (corners) on scrim @0.72 over white | 3.90 | pass (non-text). On a raw white scene it is 1.66, so the halo is required |
| status.success on a raw white scene | 1.52 | That is why the shutter ring sits on the bar and the guide strokes get the halo. On black: 13.78 |
| text.on-accent on accent.gold (torch on) | 11.1 | pass |
| white on gradient.primary ends (enabled shutter icon, Primary buttons) | 6.37 / 5.00 | pass |
| text.primary / text.muted on bg.camera (fallback screens) | 18.3 / 9.6 | pass |
| status.warning / status.error on surface.card (state discs) | 11.8 / 7.4 | pass |
| text.primary / text.muted / brand.link on bg.raised (dialog) | 15.7 / 8.2 / 7.3 | pass |

---

## Layout and screen chrome

- **Edge-to-edge**, as in Phase 3. On Scan the preview draws behind both system bars. The status-bar icons stay light, and they read because of the top gradient (below).
- **Portrait lock on Scan only:** a `DisposableEffect` sets `requestedOrientation = SCREEN_ORIENTATION_PORTRAIT` and restores the previous value on dispose. Android 16 ignores this on large screens (sw ≥ 600 dp). The layout below still works there, because every overlay is placed from the view size (see UI Considerations).
- **Back behaviour:**
  - Scan → HandSelect (D-01), from every Scan sub-state.
  - While a dialog is open, Back dismisses it.
  - While processing, Back cancels the job and stays on the scanner (or on the fallback screen it started from).
  - ScanReady Back works exactly like "Retake photo": it deletes the capture and pops to Scan (D-12).
- **Navigation:** a finished capture or import does `pushFrom(Scan(hand), ScanReady)`, so Scan stays underneath. Retake does `popFrom(ScanReady)`. `ScanReady` is a new `@Serializable data object ScanReady : Route` with the default `showsBackground = true` (the sky returns). The capture comes from `ScanResultHolder` (D-01), never from route arguments.
- **Lifecycle (D-03, D-04, D-05):** analysis runs only while the screen is resumed. On resume the permission is checked again, so coming back from Settings with the camera allowed shows the live scanner without a tap. When the screen leaves, the torch turns off.

---

## Screens

### S08 Scanner: live state (SCAN-03, SCAN-04, D-02, D-05, D-07, D-08)

Reference frame 360 x 800 dp. Layers, back to front:

1. **Preview:** `PreviewView` `fillMaxSize`, FILL_CENTER, back camera. Before the first frame it shows `bg.camera`.
   - Tap anywhere on the preview that is not a control: a `FocusMeteringAction` at that point, plus the focus ring (layer 3).
   - Semantics: `contentDescription` = `scan_preview_cd`.
2. **Top gradient:** vertical `surface.scrim` @0.6 → transparent, height = status-bar inset + 128 dp. Not interactive.
3. **`PalmGuideOverlay`** (one `Canvas`, all values read in the draw phase):
   - **Guide box:** centred horizontally, and vertically inside the free region between the chip's bottom + 16 dp and the control bar's top − 16 dp. Width = `min(0.72 × viewWidth, (freeHeight − 32 dp) / 1.3)`, height = width × 1.3 (the 200:260 viewBox). On 360 x 800 that gives about 259 x 337 dp, matching Figma B (260 x 338).
   - **Palm outline:** the Figma `HAND` path (constant below), scaled into the guide box, 2 dp stroke, round caps and joins, `PathEffect.dashPathEffect(8 dp on, 8 dp off)`, with the halo under it.
     - Searching colour: `text.primary` @0.45. PalmDetected colour: `status.success` @0.9.
     - Mirroring: the Figma path has its thumb on the **left**. To the rear camera a right palm shows its thumb on the screen's right. So draw the path mirrored (`scaleX = -1` about the box centre) for `HandSide.RIGHT`, and as-is for `LEFT`. Confirm on the phone with a real hand (backstop row).
     - Path (viewBox 0 0 200 260, our own Figma art): `M60 250C40 210 30 180 28 150L22 100C20 88 36 84 40 96L50 130L48 50C48 36 66 36 66 50L70 120L74 28C74 14 94 14 94 28L94 118L102 36C102 22 122 22 122 36L118 124L132 66C134 52 152 56 150 70L138 150C134 190 124 222 116 250`. Parse it with `PathParser().parsePathString(…).toPath()` once per size in `drawWithCache`. A VectorDrawable cannot dash a stroke, so this is not a drawable.
   - **Corner brackets:** 4 L-shapes on the guide box inflated by 16 dp. Arms 32 dp, 4 dp stroke, round caps, 8 dp bend radius, with the halo under them. Colour `brand.link` while searching, `status.success` when PalmDetected.
   - **Scan line:** 2 dp tall, as wide as the corner box. Horizontal gradient `brand.link` alpha 0 → 1 → 0. Sweeps from the corner-box top to its bottom and back. It is the screen's one loop, so it runs only while the shown state is not PalmDetected, nothing is processing and `reduceMotion` is false. Otherwise it is not drawn.
   - **Focus ring:** a 64 dp circle with a 2 dp `text.primary` stroke and the halo, centred on the tap point or on the palm centre during pre-capture focus. It stays visible until the focus result or the 1 s cap (D-05), then fades out.
4. **Top row:** under the status-bar inset, 16 dp sides, 8 dp top padding, a `Box` 52 dp tall.
   - `CenterStart`: `CameraIconButton` with `ic_back`, `contentDescription` `common_back` → HandSelect.
   - `Center`: `NocturneChip` with `hand_right` / `hand_left` (Label M, `text.primary` on `surface.card-hi`). Not interactive. It is the reference for the wrong-hand hint.
5. **Guidance chip:** 8 dp under the top row, centred, `widthIn(max = viewWidth − 32 dp)`. See "Guidance chip".
6. **Control bar:** bottom, full width.
   - Fill `surface.scrim` @0.6, top corners Xl 32. Padding 16 dp top, 32 dp sides, 24 dp bottom + the navigation-bar inset.
   - `Row(SpaceBetween, CenterVertically)`:
     - Gallery: `CameraIconButton` with `ic_gallery`, `contentDescription` `scan_choose_gallery` → `PickVisualMedia(ImageOnly)` (D-11).
     - `ShutterButton`.
     - Torch: a `CameraIconButton` toggle. Shown only when the camera reports a flash unit. Otherwise a 52 dp empty `Spacer`, so the shutter stays centred.

**`CameraIconButton`:** 52 dp circle, `clickable(role = Role.Button)` (torch: `toggleable(role = Role.Switch)`), 24 dp icon in `text.primary`.
- Fill: `surface.scrim` @0.72 when floating (Back). Inside the bar: `text.primary` @0.12, rising to @0.2 while pressed (Figma Style=Camera).
- Border: 1 dp `border.hairline` @0.35.
- Disabled (while processing): alpha 0.38.
- Torch off: `ic_flash_off`. Torch on: `ic_flash`, disc `accent.gold`, icon `text.on-accent`. `stateDescription` `common_on` / `common_off`.

**`ShutterButton`** (76 dp; Figma "Shutter"):

| State | When | Look | Semantics |
|-------|------|------|-----------|
| Disabled | Any shown state except PalmDetected, **or** PalmDetected held < 500 ms (D-08), or the guide is unavailable | Ring 4 dp `text.primary` @0.4; disc 60 dp `text.primary` @0.18; 24 dp `ic_camera` `text.on-primary`; whole button alpha 0.6; no glow | `contentDescription` `scan_capture_cd`, `stateDescription` `scan_capture_waiting`, `disabled()` |
| Enabled | PalmDetected held ≥ 500 ms | Ring 4 dp `status.success`; disc `gradient.primary`; icon `text.on-primary`; `GlowPrimary` on the disc | `contentDescription` `scan_capture_cd`, `Role.Button` |
| Pressed | Finger down while enabled | Scale 0.97 (as `NocturneButton`) | — |
| Busy | From the tap through focus (≤ 1 s) and `takePicture`, until the processing overlay takes over | Enabled look; the icon is replaced by a 24 dp M3 `CircularProgressIndicator` (2 dp stroke, `text.on-primary`); not clickable; gallery and torch disabled | `stateDescription` `scan_capture_busy` |

Tapping the enabled shutter runs the focus-then-capture sequence (D-05). It is ignored if the shown state has left PalmDetected by then. After capture the processing overlay shows.

### Guidance chip (SCAN-03, D-02, D-07, D-08)

- **Look:** `Row` with 8 dp vertical / 16 dp horizontal padding and an 8 dp gap, `RoundedCornerShape(NocturneRadius.Lg)`. That reads as a pill on one line and a rounded rectangle on two.
  - Fill `surface.scrim` @0.72. Border 1 dp in the state's tone @0.9 (Neutral: `border.hairline` @0.5).
  - Content: a 20 dp state icon in the tone colour (decorative), then a `Column` with a 4 dp gap:
    - The message (Title M, `text.primary`).
    - When WrongHand is active, the hint line (Caption, `text.primary`).
- **One chip, one live region:** the chip node is `mergeDescendants` with `liveRegion = LiveRegionMode.Polite`. It changes only when the **shown** (stabilised) state or the hint changes, never per frame.
- **The shown state comes from the stabiliser (D-08):** a raw state must persist about 250 ms before the chip changes, and NoHand dropouts under about 300 ms do not leave PalmDetected. The UI never reads raw per-frame states (the debug HUD is the only exception).

| Shown state | Tone | Icon | Message key | Shutter |
|-------------|------|------|-------------|---------|
| NoHand (also the state before the first result) | Neutral (`text.primary` icon, hairline border) | `ic_hand` | `scan_guide_no_hand` | Disabled |
| BackOfHand | Warning | `ic_hand` | `scan_guide_back_of_hand` | Disabled |
| ClosedHand | Warning | `ic_hand` | `scan_guide_closed_hand` | Disabled |
| TooFar | Warning | `ic_search` | `scan_guide_too_far` | Disabled |
| Moving | Neutral | `ic_hand` | `scan_guide_moving` | Disabled |
| PalmDetected | Success | `ic_check` | `scan_guide_palm_detected` | Disabled until held 500 ms, then Enabled |
| CaptureFailed (transient, after a `takePicture` or focus error) | Error | `ic_alert` | `scan_guide_capture_failed` | Follows the live state underneath |
| GuideUnavailable (the hand landmarker failed to start) | Error | `ic_alert` | `scan_guide_unavailable` | Disabled (gallery still opens; its import shows the "failed" rejection if the landmarker cannot run) |

- **CaptureFailed** stays up for 4 s (`CAPTURE_ERROR_MS = 4000`, a plain timeout, not a motion token). It also clears early when the shown live state changes. Then the live message returns.
- **WrongHand hint (advisory, D-07):** while the hand the model sees confidently differs from the chosen hand, the chip adds the hint line. The icon stays the state's icon. The hint never changes the tone and **never blocks capture**.
  - The chosen hand is Right and the model sees Left → `scan_hint_wrong_hand_left`.
  - The chosen hand is Left and the model sees Right → `scan_hint_wrong_hand_right`.
  - It uses the same 250 ms dwell as the state, so it does not flicker.

### Permission and fallback screens (SCAN-02, D-04, D-11)

All of these render inside the Scan route (`showsBackground = false`, opaque `bg.camera`).

**Layout:**
- `NocturneTopBar(title = scan_title, onBack → HandSelect)` under the status-bar inset, 16 dp sides.
- A `verticalScroll` column (16 dp sides, 24 dp top, 24 dp + navigation inset at the bottom). Flexible spacers above and below centre `NocturneStateBlock` when it fits. At large font scale the column scrolls.

**`NocturneStateBlock`:** full width, content centred.
1. A 96 dp disc: `surface.card` fill, 1 dp stroke in the tone @0.5, 40 dp icon in the tone (decorative).
2. 24 dp gap, then the title (Display M, `text.primary`, centred, `heading()`).
3. 8 dp gap, then the body (Body L, `text.muted`, centred).
4. 24 dp gap, then the primary `NocturneButton` (full width).
5. If there is a secondary action: 8 dp gap, then a Ghost `NocturneButton` (full width).

| State | Trigger | Icon / tone | Title / body | Primary | Secondary |
|-------|---------|-------------|--------------|---------|-----------|
| Checking / requesting | First composition, and while the system permission dialog is up | — | Nothing but `bg.camera` and the top row's back button (scanner layout). No chip, no controls | — | — |
| Denied | The user said no and `shouldShowRequestPermissionRationale` is true | `ic_camera_off` / `status.warning` | `scan_perm_title` / `scan_perm_body` | `scan_perm_allow` → request again | `scan_choose_gallery` → picker |
| Permanently denied | Denied, rationale false, **and the app has asked before** | `ic_camera_off` / `status.warning` | `scan_perm_blocked_title` / `scan_perm_blocked_body` | `scan_perm_open_settings` → `ACTION_APPLICATION_DETAILS_SETTINGS` for the package | `scan_choose_gallery` → picker |
| No back camera | `hasCamera(DEFAULT_BACK_CAMERA)` is false | `ic_camera_off` / `status.error` | `scan_no_camera_title` / `scan_no_camera_body` | `scan_choose_gallery` → picker | — |
| Camera error | The camera fails to open, or `CameraState` reports an error (camera in use, disconnected) | `ic_alert` / `status.error` | `scan_no_camera_title` / `scan_camera_error_body` | `scan_choose_gallery` → picker | `scan_camera_retry` → rebind the camera |

- "Has asked before" is a flag saved when the request first returns. `rememberSaveable` is not enough across app restarts, so keep it in DataStore. Without it, a first-time user would see "Open settings" before Android ever showed its dialog.
- A gallery import started here shows the processing overlay over this screen. Success goes to ScanReady; a rejection opens the dialog over this screen.

### Processing state (SCAN-05, SCAN-06, D-10)

- Shown from the end of capture (or the return of the gallery picker with a photo) until `PhotoProcessor` finishes. It covers the scanner or the fallback screen it started from.
- Full-screen `Box`, `surface.scrim` @0.72, consumes all touches. Centred column with a 16 dp gap:
  - A 48 dp M3 `CircularProgressIndicator` (indeterminate, 4 dp stroke, `brand.link`, track `text.primary` @0.12).
  - `scan_processing` (Title M, `text.primary`, centred). It is a polite live region, announced once.
- While it shows: analysis is paused, the torch turns off, the controls are disabled, and the scan line stops.
- Outcomes:
  - Success → `ScanReady`.
  - No hand → rejection dialog "no hand".
  - Undecodable or HEIC on API 26-27 → rejection dialog "unsupported".
  - Any other failure (I/O, out of memory) → rejection dialog "failed".
  - Back → cancel (see Back behaviour).

### Photo rejection dialog (SCAN-05, D-10)

`NocturneDialog` over the screen the import or capture came from. Title (Display M, `heading()`), body (Body L, muted), and buttons end-aligned in a `FlowRow` that wraps at large font.

| Reason | Title / body | Source = gallery | Source = camera |
|--------|--------------|------------------|-----------------|
| No hand | `scan_reject_no_hand_title` / `scan_reject_no_hand_body` | Primary `scan_choose_another` → picker; Ghost `common_close` → dismiss | Primary `scan_retake` → dismiss to the live scanner |
| Unsupported photo | `scan_reject_unsupported_title` / `scan_reject_unsupported_body` | Same as above | — (camera JPEGs always decode) |
| Failed | `scan_reject_failed_title` / `scan_reject_failed_body` | Same as above | Primary `scan_retake` → dismiss |

Back or a scrim tap = dismiss. Nothing is kept: the cache file is deleted on every rejection (D-12).

### ScanReady placeholder (D-01, D-12)

Route `ScanReady`, with the sky behind it.

- `NocturneTopBar(title = scan_ready_title, onBack = retake)`.
- A `verticalScroll` column, 16 dp sides, 24 dp top:
  1. **Photo:** full width, the photo's own aspect ratio, `heightIn(max = 60% of the viewport height)`, radius Lg 24, clipped, 1 dp `border.hairline` @0.35.
     - `Image` with `ContentScale.Fit` (decoded once, off the main thread, from the `ProcessedCapture` file).
     - Over it, in the same box and through the same Fit transform: the 21 landmarks as 8 dp `brand.link` dots with a 1 dp `bg.base` outline. This is the on-device check that the landmarks were mapped into the crop (SCAN-06).
     - `contentDescription` = `scan_ready_photo_cd` (21).
  2. 16 dp gap, then `scan_ready_source` (Title M, `text.primary`), e.g. "Right hand · Camera".
  3. 8 dp gap, then `scan_ready_meta` (Caption, `text.muted`), e.g. "21 hand points · 960 × 1280 px".
- **Bottom CTA** (16 dp sides, 16 dp above the navigation inset):
  - Primary `scan_ready_read`, **disabled** (38% alpha, D-01).
  - 8 dp gap, then Ghost `scan_retake` → delete the capture and `popFrom(ScanReady)`.
- States:
  - **Loading:** the photo box shows a 48 dp `brand.link` spinner, centred, at a 4:3 placeholder ratio until the bitmap is decoded.
  - **Filled:** as above.
  - **Photo gone:** the holder is empty, for example after process death, because the capture is deleted at app start (D-12). The column shows `NocturneStateBlock` (`ic_camera` / `brand.link`, `scan_ready_gone_title` / `scan_ready_gone_body`, primary `scan_retake`) instead of the photo and the CTA.

---

## Motion

Durations and easings come from `NocturneDurations` / `NocturneEasings`. The Figma "5 · Motion" rows are mapped onto them.

| Element | Animation | Duration / easing | Reduced motion (`LocalNocturne.reduceMotion`) |
|---------|-----------|-------------------|-----------------------------------------------|
| Guidance chip state change | `AnimatedContent` cross-fade of the icon and text; `animateColorAsState` for the border | `Quick` 200, `Standard` | Instant swap |
| Chip entering PalmDetected | One scale pulse 1 → 1.04 → 1 (Figma "one gentle scale pulse") | `Standard` 300 total, `Standard` | None |
| Guide outline and corners: searching ↔ detected | Colour cross-fade | `Quick` 200, `Standard` | Instant |
| Scan line | Sweep top ↔ bottom, reverse, infinite. Figma says 1.6 s; there is no token for that, so it uses `AmbientFast` | `AmbientFast` 2400, `GentleSine` | Not drawn |
| Shutter Disabled → Enabled | Ring and disc colours cross-fade; glow fades in | `Quick` 200, `Standard` | Instant |
| Shutter press | Scale 0.97 | `Press` 150, `Standard` | No scale |
| Focus ring | Appears at scale 1.2 → 1, fades out after the focus result or 1 s | In: `Quick` 200, `EmphasizedDecelerate`. Out: `Quick` 200, `EmphasizedAccelerate` | Appears and disappears without scale or fade |
| Torch toggle | Disc fill cross-fade | `Instant` 100 | Instant |
| Processing overlay | Fade in / out | `Quick` 200, `Standard` | Instant |
| Rejection dialog | Like `ExitDialog`: scale 0.92 → 1 + fade | `Standard` 300 | Fade only |
| Scan ↔ ScanReady, Scan ↔ HandSelect | The existing `AppNavDisplay` transitions | Phase 3 | Phase 3 (none) |
| Corner pulse while searching (Figma) | **Not built** (the scan line is the one loop on this screen) | — | — |

Every animated value over the preview is read in the draw phase (`drawWithCache` / `graphicsLayer` lambdas), so camera frames never trigger recomposition. The guidance `StateFlow` emits only shown-state changes. Loops stop when the screen pauses (the frame clock stops). Nothing flashes faster than 3 times per second.

---

## Copywriting Contract

Brand "[AppName]" and persona "[Guide]" stay literal placeholders. All strings live in `res/values/strings_scan.xml` and `res/values-hi/strings_scan.xml` (a new file per locale, as Phase 3 split home/onboarding). The Hindi is Claude-written and is flagged for native-speaker review. Placeholders are positional.

| Element | Copy |
|---------|------|
| Primary CTA (phase) | The shutter: "Take photo" (`scan_capture_cd`, icon-only, spoken). Visible verb + noun CTAs: "Allow camera", "Open settings", "Choose from gallery", "Choose another photo", "Retake photo", "Try camera again" |
| Locked labels (D-01) | "Read my palm (next update)", disabled on ScanReady. D-01's "Retake" is written as "Retake photo" |
| Empty state heading | Scanner with no hand in view: "Place your palm inside the frame" (chip). ScanReady with no capture: "This photo is gone" |
| Empty state body | ScanReady with no capture: "Take a new photo to continue." + [Retake photo] |
| Error state | No hand: "No hand found" / "We couldn't find a hand in this photo. Try a clear photo of your open palm." (D-10 wording) + [Choose another photo] or [Retake photo]. Camera error: "Camera not available" / "Another app may be using the camera. Close it and try again, or pick a palm photo from your gallery." + [Choose from gallery] [Try camera again]. Capture failed: "Couldn't take the photo. Try again." |
| Destructive confirmation | None. "Retake photo" deletes the processed photo without a confirmation, because a new one takes seconds and nothing has been read or saved yet |

### String table (keys, English, Hindi)

| Key | English | Hindi |
|-----|---------|-------|
| `scan_title` | Scan your palm | अपनी हथेली स्कैन करें |
| `scan_preview_cd` | Camera preview. Tap to focus. | कैमरा प्रीव्यू। फ़ोकस करने के लिए टैप करें। |
| `scan_guide_no_hand` | Place your palm inside the frame | अपनी हथेली फ़्रेम के अंदर रखें |
| `scan_guide_back_of_hand` | Turn your palm toward the camera | हथेली को कैमरे की तरफ़ घुमाएँ |
| `scan_guide_closed_hand` | Open your hand and spread your fingers | हाथ खोलें और उंगलियाँ फैलाएँ |
| `scan_guide_too_far` | Bring your hand a little closer | हाथ को थोड़ा पास लाएँ |
| `scan_guide_moving` | Hold still | हाथ स्थिर रखें |
| `scan_guide_palm_detected` | Palm detected. Tap to scan | हथेली मिल गई। स्कैन करने के लिए टैप करें |
| `scan_guide_capture_failed` | Couldn't take the photo. Try again. | फ़ोटो नहीं ली जा सकी। फिर से कोशिश करें। |
| `scan_guide_unavailable` | The hand guide couldn't start. Go back and try again. | हैंड गाइड शुरू नहीं हो सका। वापस जाकर फिर से कोशिश करें। |
| `scan_hint_wrong_hand_left` | This looks like your left hand. You chose your right. | यह आपका बायाँ हाथ लगता है। आपने दायाँ हाथ चुना है। |
| `scan_hint_wrong_hand_right` | This looks like your right hand. You chose your left. | यह आपका दायाँ हाथ लगता है। आपने बायाँ हाथ चुना है। |
| `scan_capture_cd` | Take photo | फ़ोटो लें |
| `scan_capture_waiting` | Waiting for your palm | आपकी हथेली का इंतज़ार है |
| `scan_capture_busy` | Taking photo | फ़ोटो ली जा रही है |
| `scan_torch_cd` | Flash | फ़्लैश |
| `common_on` / `common_off` | On / Off | चालू / बंद |
| `common_close` | Close | बंद करें |
| `scan_choose_gallery` | Choose from gallery | गैलरी से चुनें |
| `scan_processing` | Preparing your photo… | आपकी फ़ोटो तैयार हो रही है… |
| `scan_perm_title` | Allow the camera to scan | स्कैन के लिए कैमरे की अनुमति दें |
| `scan_perm_body` | [AppName] uses the back camera to guide your palm into the frame and take one photo. You can also pick a palm photo from your gallery. | [AppName] पीछे वाले कैमरे से आपकी हथेली को फ़्रेम में लाने में मदद करता है और एक फ़ोटो लेता है। आप गैलरी से भी हथेली की फ़ोटो चुन सकते हैं। |
| `scan_perm_allow` | Allow camera | कैमरे की अनुमति दें |
| `scan_perm_blocked_title` | Camera access is off | कैमरे की अनुमति बंद है |
| `scan_perm_blocked_body` | Turn on the camera for [AppName] in your phone's settings, or pick a clear palm photo from your gallery. | अपने फ़ोन की सेटिंग में [AppName] के लिए कैमरा चालू करें, या गैलरी से अपनी हथेली की साफ़ फ़ोटो चुनें। |
| `scan_perm_open_settings` | Open settings | सेटिंग खोलें |
| `scan_no_camera_title` | Camera not available | कैमरा उपलब्ध नहीं है |
| `scan_no_camera_body` | This phone has no back camera we can use. You can still read a palm photo from your gallery. | इस फ़ोन में ऐसा पीछे वाला कैमरा नहीं है जिसे हम इस्तेमाल कर सकें। आप फिर भी गैलरी से हथेली की फ़ोटो चुन सकते हैं। |
| `scan_camera_error_body` | Another app may be using the camera. Close it and try again, or pick a palm photo from your gallery. | हो सकता है कोई और ऐप कैमरा इस्तेमाल कर रहा हो। उसे बंद करके फिर से कोशिश करें, या गैलरी से हथेली की फ़ोटो चुनें। |
| `scan_camera_retry` | Try camera again | कैमरा फिर से आज़माएँ |
| `scan_reject_no_hand_title` | No hand found | हाथ नहीं मिला |
| `scan_reject_no_hand_body` | We couldn't find a hand in this photo. Try a clear photo of your open palm. | इस फ़ोटो में हमें हाथ नहीं मिला। अपनी खुली हथेली की साफ़ फ़ोटो आज़माएँ। |
| `scan_reject_unsupported_title` | Can't open this photo | यह फ़ोटो नहीं खुल सकी |
| `scan_reject_unsupported_body` | This photo type doesn't open on this phone. Try a JPEG or PNG photo. | यह फ़ोटो इस फ़ोन पर नहीं खुलती। JPEG या PNG फ़ोटो आज़माएँ। |
| `scan_reject_failed_title` | Something went wrong | कुछ गड़बड़ हो गई |
| `scan_reject_failed_body` | We couldn't prepare this photo. Please try again. | हम यह फ़ोटो तैयार नहीं कर सके। कृपया फिर से कोशिश करें। |
| `scan_choose_another` | Choose another photo | दूसरी फ़ोटो चुनें |
| `scan_retake` | Retake photo | फ़ोटो फिर से लें |
| `scan_ready_title` | Your palm photo | आपकी हथेली की फ़ोटो |
| `scan_ready_photo_cd` | Your palm photo with %1$d hand points marked | आपकी हथेली की फ़ोटो, %1$d बिंदु चिह्नित |
| `scan_ready_source` | %1$s · %2$s | %1$s · %2$s |
| `scan_source_camera` / `scan_source_gallery` | Camera / Gallery | कैमरा / गैलरी |
| `scan_ready_meta` | %1$d hand points · %2$d × %3$d px | %1$d हाथ के बिंदु · %2$d × %3$d px |
| `scan_ready_read` | Read my palm (next update) | मेरी हथेली पढ़ें (अगले अपडेट में) |
| `scan_ready_gone_title` | This photo is gone | यह फ़ोटो अब नहीं है |
| `scan_ready_gone_body` | Take a new photo to continue. | आगे बढ़ने के लिए नई फ़ोटो लें। |

Reused from Phase 3: `common_back`, `hand_left`, `hand_right` (the first argument of `scan_ready_source` and the top-row chip). Removed with the placeholder screen: `scan_placeholder_title`, `scan_placeholder_hand` (both locales).

Copy rules (as in Phase 3): no claims of accuracy; no medical, legal or financial wording; no promise of features M1 lacks (the "next update" label is the D-01 placeholder); sentence case; no emoji. "Settings" means the phone's settings (there is no in-app Settings screen).

---

## Assets (vector drawables, our Figma art only)

**Never use reference-app assets or anything under `_extract/`.**
- New icons use the Figma `Icon/*` paths from build script line 494, verbatim in `pathData`.
- SVG `<circle>` and `<rect>` become arcs, as written below.
- Format = the existing `ic_back.xml`: 24 dp, viewport 24, `strokeWidth` 1.8, round cap/join, `strokeColor` `@color/nocturne_text_primary`, no fill. Tinted at use.
- A later duotone export (Figma line 1255 added a 16% tint layer and a gold accent sparkle to some icons) may replace them under the same names. Phase 4 ships the single-stroke versions, like Phase 3.

| Drawable | pathData (one `<path>` per entry) | Used in |
|----------|-----------------------------------|---------|
| `ic_camera` | `M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3z` · `M9 13a3 3 0 1 0 6 0a3 3 0 1 0-6 0` | Shutter, ScanReady "gone" disc |
| `ic_camera_off` | `M2 2l20 20M7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h14M9.5 4h5L17 7h3a2 2 0 0 1 2 2v9` | Permission and no-camera discs |
| `ic_gallery` | `M6 3h12a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z` · `M7 9a2 2 0 1 0 4 0a2 2 0 1 0-4 0` · `m21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21` | Gallery button |
| `ic_flash` | `M13 2 4 14h7l-1 8 9-12h-7z` | Torch on |
| `ic_flash_off` | `M2 2l20 20M13 2l-2.5 3.3M17.2 12.5 19 10h-5.5M11 14H4l3-4M10.5 22l1-6.5` | Torch off |
| `ic_hand` | `M18 11V6a2 2 0 0 0-4 0v5M14 10V4a2 2 0 0 0-4 0v6M10 10.5V6a2 2 0 0 0-4 0v8M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.9-6-2.3l-3.6-3.6a2 2 0 0 1 2.8-2.8L7 15` | Chip: NoHand, BackOfHand, ClosedHand, Moving |
| `ic_search` | `M4 11a7 7 0 1 0 14 0a7 7 0 1 0-14 0` · `m20 20-4-4` | Chip: TooFar (Figma uses `search` for "Too far away") |

Reused: `ic_back`, `ic_check`, `ic_alert`.

**Drawn in code, not drawables:** the palm outline (needs a dashed stroke; path above), the corner brackets, the scan line, the focus ring, the shutter, and the ScanReady landmark dots. **Not needed:** illustrations. The scanner shows the user's own camera image, and the state blocks use icon discs (Figma `State block`). The 7 new drawables add well under 10 KB.

---

## Accessibility

- **Touch targets:** shutter 76 dp, camera buttons 52 dp, top-bar back 48 dp, `NocturneButton` 52 dp tall. The preview's tap-to-focus covers the whole free area. All of these are ≥ 48 dp.
- **Semantics:**
  - The scanner sets `paneTitle = scan_title`, so TalkBack names the screen.
  - The guidance chip is one merged node with a **polite live region** (D-02). It announces the message plus the hint, only on shown-state changes (at most one change per 250 ms dwell).
  - The shutter: `Role.Button`, `contentDescription` "Take photo", a `stateDescription` for waiting/busy, and `disabled()` when not capturable. TalkBack users get the state from the chip and capture once it says "Palm detected".
  - The torch: `Role.Switch` with "Flash" + "On"/"Off".
  - Gallery and back carry their content descriptions.
  - All icons and overlay strokes are decorative (`null`).
  - The preview has `scan_preview_cd` and an `onClick` label for focus.
  - State-block and dialog titles are `heading()`.
  - The processing label is a polite live region.
  - The ScanReady photo has a description; its landmark dots are decorative.
- **Contrast:** see the Color table. Every text-on-camera pair is ≥ 8.35:1 in the worst case. Status icons are ≥ 4.09:1 and strokes ≥ 3:1 thanks to the scrims and halos.
- **Font scaling (1.0 / 1.3 / 2.0):**
  - The chip wraps up to its max width (about 3 lines at 2.0 in Hindi), and the guide box shrinks to the remaining free height.
  - The top-row hand chip wraps.
  - The control bar holds icons only, so it does not grow.
  - Fallback screens and ScanReady scroll to their actions.
  - Dialog buttons wrap in the `FlowRow`.
- **Devanagari:** font-metric line heights via the theme's `devanagariSafe()`. No fixed heights, no clipped matras in the chip or the dialogs.
- **Reduce motion** (animator scale 0 or battery saver, Phase 3 rule): no scan line, no pulse, no press scale, no focus-ring scale, instant colour swaps (see the Motion table).
- **TalkBack order:** back → hand chip → guidance chip → preview → gallery → shutter → torch.

---

## Figma vs locked decisions (decision wins)

| Item | Figma (2026-11-26 build) | Built in Phase 4 | Why |
|------|--------------------------|------------------|-----|
| Layout | Layout A: top app bar + inset 300 x 380 dp rounded viewfinder on `bg.camera`, chip and controls below. Layout B: full bleed | **Layout B** (full-bleed preview) with Layout A's corners and scan line drawn around the guide | D-02 "full-bleed preview" |
| Top controls | B: Back + Left/Right/Auto segmented + help (photo tips). A: "Right hand · Change" chip + help | Back + a non-interactive hand label chip. No segmented control, no help, no "Change" | D-01 (hand comes from the route; Back returns to HandSelect, which already has the tips); D-07 WrongHand is a hint only |
| Corners | Pulse lavender while searching | Static `brand.link`, cross-fading to `status.success` on detection | One loop per screen (the scan line); low-end jank budget (SCAN-07) |
| Scan line | 1.6 s sweep | `AmbientFast` 2.4 s | No 1.6 s token |
| Guidance chip | Black @0.5 fill, Label M 13 sp, padding 10/16, gap 10 | `surface.scrim` @0.72, Title M 15 sp, padding 8/16, gap 8, wrong-hand hint line | Contrast (3.55 → 8.35 over a white scene); readability; spacing scale; D-07 |
| Chip copy | "Hold still…", "Palm detected · tap to scan", "Couldn't capture. Try again" | "Hold still", "Palm detected. Tap to scan", "Couldn't take the photo. Try again." | Sentence punctuation; same meaning |
| Camera icon button | White @0.10 fill everywhere | White @0.12 inside the bar; `surface.scrim` @0.72 when floating (Back); 1 dp hairline | White @0.10 has no contrast over a bright scene |
| Shutter | 3 dp ring, 26 dp icon, disabled whole-opacity 0.6 | 4 dp ring + 4 dp gap, 24 dp icon (`NocturneSize.Icon`), 0.6 kept; Busy state added | Multiples of 4; D-05 focus-then-capture needs a busy state |
| Control bar | Scrim @0.55, top radius 28, padding 16/28/20/28 | Scrim @0.6, radius Xl 32, padding 16/32/24/32 | Tokens and spacing scale |
| Captions under controls (A: "Gallery", "Flash", "Tap to scan" / "Waiting for palm") | Present in A, absent in B | Not built; content descriptions instead | Layout B; the chip already carries the state |
| Permission | One "Camera denied" block: "Open settings" + "Choose from gallery" | Two variants: Denied ("Allow camera" + gallery) and Permanently denied ("Open settings" + gallery) | D-04 |
| Camera unavailable | One block: "Choose from gallery" only | No-camera (gallery only) and camera error (gallery + "Try camera again") | D-04, PITFALLS 15 (camera in use needs a retry) |
| Processing, no-hand rejection, ScanReady | Not in Figma (S09 Generating and the S10 invalid-photo screens belong to Phases 5-6) | New, built from existing tokens and the `ExitDialog` / `State block` patterns | D-01, D-10 |
| Icons | Duotone upgrade (16% tint + gold accent, 1.7 dp stroke) | Single-stroke 1.8 dp paths | Phase 3 convention; the duotone swap is asset-only later |

---

## UI Considerations

Applicable state considerations resolved: 17 covered, 7 backstop, 5 unresolved.

| Category | Element(s) | Status | Resolution / Reason |
|----------|------------|--------|---------------------|
| loading | Scanner before the first camera frame / landmarker ready (static-content) | ✅ covered | The preview shows `bg.camera`, the chip shows NoHand "Place your palm inside the frame", and the shutter is disabled until a PalmDetected state has held 500 ms |
| loading | Processing after capture or import (interactive-control) | ✅ covered | A full-screen scrim with a spinner and "Preparing your photo…" blocks input, pauses analysis, turns the torch off, and ends on ScanReady or the rejection dialog. Back cancels it |
| loading | Shutter during focus + capture (interactive-control) | ✅ covered | The shutter shows a spinner, ignores taps, and disables gallery and torch from the tap until the processing overlay appears (focus capped at 1 s) |
| loading | ScanReady photo decode (static-content) | ✅ covered | The photo box shows a `brand.link` spinner at a 4:3 ratio until the bitmap is decoded |
| error | Camera permission denied with rationale (nav) | ✅ covered | The Denied block shows "Allow camera" (re-request) and "Choose from gallery" (Copywriting: `scan_perm_*`) |
| error | Camera permission permanently denied (nav) | ✅ covered | The Permanently denied block shows "Open settings" (app details settings) and "Choose from gallery". It never shows before the first system dialog (DataStore "asked" flag) |
| error | No back camera (nav) | ✅ covered | The No-camera block offers "Choose from gallery" only; the import pipeline works without a camera |
| error | Camera open / `CameraState` error (nav) | ✅ covered | The camera-error block offers "Choose from gallery" and "Try camera again" (rebind) |
| error | Capture or focus failure (interactive-control) | ✅ covered | The chip shows "Couldn't take the photo. Try again." in the error tone for 4 s or until the live state changes. The scanner stays live |
| error | Gallery or capture photo with no hand (form) | ✅ covered | The rejection dialog shows "No hand found" with the D-10 body. Gallery: [Choose another photo] [Close]. Camera: [Retake photo]. The cache file is deleted |
| error | HEIC on API 26-27 or an undecodable photo (form) | ✅ covered | The rejection dialog shows "Can't open this photo" / "Try a JPEG or PNG photo." |
| error | Processing I/O or out-of-memory failure (form) | ✅ covered | The rejection dialog shows "Something went wrong" / "We couldn't prepare this photo. Please try again." |
| error | Hand landmarker fails to start (static-content) | ✅ covered | The chip shows the error tone "The hand guide couldn't start. Go back and try again.", the shutter stays disabled, and Back still works |
| populated | PalmDetected (interactive-control) | ✅ covered | Chip in the success tone + guide and corners in `status.success`. After 500 ms the shutter turns Enabled (green ring, gradient disc, glow) |
| partial | Wrong hand detected (static-content) | ✅ covered | The chip adds the advisory hint line (`scan_hint_wrong_hand_*`) and keeps its state and tone. Capture stays available when the palm is detected (D-07) |
| partial | No flash unit (interactive-control) | ✅ covered | The torch button is replaced by a 52 dp spacer and the shutter stays centred |
| empty | ScanReady with no capture (after process death) (static-content) | ✅ covered | It shows the "This photo is gone" state block with [Retake photo] instead of a broken image |
| nav | Back and Settings round trip (nav) | ✅ covered | Back follows "Layout and screen chrome". Returning from Settings with the camera allowed shows the live scanner without a tap (permission re-checked on resume) |
| reduced-motion | Scan line, pulse, press, focus ring, dialog (interactive-control) | ✅ covered | With animator scale 0 or battery saver: no scan line, no pulse or scale, instant colour swaps, dialog fade only |
| flicker | Guidance chip stability (static-content) | 🧪 backstop | Held still, the shown state does not change (250 ms dwell, 300 ms dropout tolerance, D-08). Verified by the stabiliser unit tests on fixture sequences, plus an on-device screen recording with each hand in each pose |
| overflow | Chip, permission screens, dialog, ScanReady at font scale 2.0 in Hindi (static-content, interactive-control) | 🧪 backstop | The chip wraps (guide shrinks), fallback screens and ScanReady scroll to their actions, dialog buttons wrap. Checked with Redmi screenshots at 1.0 / 1.3 / 2.0 in en and hi |
| long-text | Devanagari in the chip and dialogs (static-content) | 🧪 backstop | Hindi strings render with no clipped matras or conjuncts over the camera scrim (screenshot check, as in Phase 3) |
| alignment | Guide outline mirroring and guide box vs FILL_CENTER crop (static-content) | 🧪 backstop | The guide's thumb side matches the chosen hand seen through the rear camera, and a hand that fills the guide passes the TooFar check. Checked on the phone with both hands; coordinate maths unit-tested (PITFALLS 13) |
| contrast | Chip and controls over a bright scene (static-content) | 🧪 backstop | Pointed at a white wall and at a dark room, chip text, icons, corners and the guide stay legible (device screenshots) |
| a11y | TalkBack live-region pacing (static-content) | 🧪 backstop | With TalkBack on, each shown-state change is announced once, politely, without a queue of stale messages. If it is too chatty, raise the announce dwell to 1 s (planner may tune) |
| i18n | Hindi scanner and fallback copy (static-content) | 🧪 backstop | Every Phase 4 string exists in `values-hi/strings_scan.xml`. A lint or unit check fails on a missing translation |
| unresolved | Hindi copy | ⚠ unresolved | All Hindi is Claude-written and needs native-speaker review before release (STATE.md flag). Treat the strings as provisional |
| unresolved | Figma visual QA | ⚠ unresolved | Values come from the build scripts, not live frames (MCP unavailable). Compare device screenshots with "S08 Hand scan / … (Layout B)" and the state frames once Figma access works |
| unresolved | Icon export | ⚠ unresolved | The 7 new icons are hand-written from the Figma paths. A duotone export may replace them under the same names |
| unresolved | Large screens and landscape | ⚠ unresolved | Android 16 ignores the portrait lock at sw ≥ 600 dp. The layout is size-driven, but guidance thresholds were only calibrated in portrait. Assumed acceptable for M1 (phones only) |
| unresolved | Torch washing out palm lines | ⚠ unresolved | PITFALLS suggests warning that the flash can wash out lines. Not built (no copy, no hint). Revisit if Phase 6 rejections show glare |

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| none (no shadcn or component registry on Android) | none | not applicable. New Gradle artifacts (CameraX 1.7.0 core/camera2/lifecycle/view, MediaPipe tasks-vision 1.2.3, exifinterface 1.4.2) and the `hand_landmarker.task` model go through the D-17 consent checkpoint |

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
