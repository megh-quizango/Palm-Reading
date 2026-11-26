---
phase: 01-foundations-contract
plan: 05
subsystem: design-system, showcase, launcher-icon, dev-tools
status: complete
tags: [compose, design-tokens, drop-shadow, gradients, star-background, reduce-motion, adaptive-icon, adb]
requires:
  - "01-03: BackendStatusRow / BackendStatusViewModel.retry(), smoke-phone.sh switches"
  - "01-04: NocturneTokens.kt, NocturneTheme / LocalNocturne (reduceMotion, type), nocturne_colors.xml, screen.py hash"
provides:
  - "Effects.kt: Modifier.nocturneGlow(NocturneShadow, Shape), NocturneGradientAngles, gradientLine(), nocturneBrush(), NocturneBorderWidth"
  - "components: NocturneButton + NocturneButtonStyle (Primary/Secondary/Ghost, adChip), NocturneChip, AdChip, FeatureCard"
  - "NocturneBackground(modifier, content): full-bleed seeded star sky, draw-phase animation, static under reduceMotion"
  - "Adaptive vector [Guide] orb launcher icon (background / foreground / monochrome)"
  - "tools/check-motion.sh off|on"
affects: [phase-3, phase-4, phase-5, phase-6]
tech-stack:
  added: []
  patterns:
    - "One hand-written conversion point (Effects.kt) from token glows/gradient stops to Compose; components pass NocturneGradientAngles constants, never bare angles"
    - "Infinite-transition values are State objects read only inside drawBehind, so frames redraw without recomposing"
    - "Background content sits in its own graphicsLayer so a star frame does not re-record the content"
key-files:
  created:
    - android/app/src/main/java/app/palm/core/designsystem/Effects.kt
    - android/app/src/test/java/app/palm/core/designsystem/EffectsTest.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/Buttons.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/Chips.kt
    - android/app/src/main/java/app/palm/core/designsystem/components/FeatureCard.kt
    - android/app/src/main/java/app/palm/core/designsystem/NocturneBackground.kt
    - android/app/src/main/res/mipmap-anydpi/ic_launcher.xml
    - android/app/src/main/res/drawable/ic_launcher_foreground.xml
    - android/app/src/main/res/drawable/ic_launcher_monochrome.xml
    - tools/check-motion.sh
  modified:
    - android/app/src/main/java/app/palm/showcase/ShowcaseScreen.kt
    - android/app/src/main/java/app/palm/showcase/BackendStatus.kt
    - android/app/src/main/AndroidManifest.xml
key-decisions:
  - "A10 confirmed against the Compose 1.12.1 AAR: drop-shadow type is androidx.compose.ui.graphics.shadow.Shadow(radius, color, spread, offset: DpOffset); Modifier.dropShadow(shape, shadow) is in androidx.compose.ui.draw"
  - "NocturneBorderWidth = 1.dp lives in Effects.kt (the tokens have no border width; the plan fixes 1 dp)"
  - "Pill shape = RoundedCornerShape(50) (radius.pill 999 px on any height)"
  - "Star drift is a small circular sway (sin/cos over the 40 s AmbientSlow loop) so the Restart loop is seamless"
  - "NocturneBackground provides LocalContentColor = NocturneSemantic.Foreground, replacing what the removed Surface supplied"
requirements-completed: [FND-02, FND-04]
duration: 14 min
started: 2026-11-26T11:13:30Z
completed: 2026-11-26
plan_head_before: 6dcd7472496bef22cc8798f60d656d5942e1ca87
plan_head_after: aca129a024d92c926fd81da489d40c9259c806fe
actuals:
  tokens: 5660     # chars/4 over the added lines of git diff 6dcd747..aca129a (22,636 chars, 13 files)
  tasks: 2
  commits: 2
coverage:
  - deliverable: "Gradient angles from one place (NocturneGradientAngles) and the CSS angle-to-endpoint mapping"
    human_judgment: false
    verification:
      - kind: test
        ref: 'bash tools/gradlew.sh :app:testDebugUnitTest --tests "app.palm.core.designsystem.*" (EffectsTest tests=3 failures=0)'
        status: pass
  - deliverable: "Primary/Secondary/Ghost buttons, Scan again + AD chip, chip and feature card on the phone"
    human_judgment: false
    verification:
      - kind: command
        ref: 'SMOKE_SCROLLS=10 bash tools/smoke-phone.sh "Backend: Connected" "Primary" "Secondary" "Ghost" "Scan again" "AD" "Feature card" "Retry"'
        status: pass
  - deliverable: "Restyled status chip keeps Unreachable, Retry and recovery to Connected"
    human_judgment: false
    verification:
      - kind: command
        ref: 'SMOKE_NO_REVERSE=1 SMOKE_RETRY_EXPECT="Backend: Connected" bash tools/smoke-phone.sh "Backend: Unreachable" "Retry"'
        status: pass
  - deliverable: "Stars static with animations off, animating with animations on, setting restored"
    human_judgment: false
    verification:
      - kind: command
        ref: "bash tools/install-debug.sh && bash tools/check-motion.sh off; bash tools/check-motion.sh on; settings get global animator_duration_scale == null"
        status: pass
  - deliverable: "APK declares the ic_launcher adaptive icon"
    human_judgment: false
    verification:
      - kind: command
        ref: 'aapt2 dump badging app-debug.apk | grep -q "application-icon.*ic_launcher" (res/mipmap-anydpi-v21/ic_launcher.xml)'
        status: pass
  - deliverable: "Showcase matches the Nocturne tokens on the phone (Hindi shaping, glows, gradient directions, light status icons, orb icon)"
    human_judgment: true
    rationale: "Phase-end single on-device visual check (backstop truth); screenshots captured, reply pending from the user"
    verification:
      - kind: manual
        ref: "tools/adb/shots/showcase-top.png, showcase-components.png, showcase-type.png, launcher-drawer.png"
        status: pending
---

# Phase 1 Plan 05: Nocturne Components, Star Background and Orb Icon Summary

**The showcase now has token-styled buttons (a primary gradient at 90 degrees with a primary glow, secondary, ghost, and "Scan again" with an AD chip), a chip, and a feature card (135 degree gradient, foil border and card glow). All of them get their shadow and gradient values from a single conversion point, Effects.kt. The backend status is now a Nocturne chip with a coloured dot and a ghost Retry button. The screen sits on a seeded, full-bleed star sky that is animated only in the draw phase and stays static when system animations are off. The launcher shows an adaptive vector gold orb icon with a monochrome layer.**

## Performance

- Duration: about 14 min (2026-11-26T11:13Z to 11:27Z)
- Tasks: 2/2. Task 1 was the tracer: its three verify commands passed before Task 2 began.
- Files: 13 (10 created, 3 modified)

## Accomplishments

- **Effects.kt** is the only file that calls `dropShadow`. `nocturneGlow` maps blur to radius, plus spread, colour and `DpOffset`, and imports the drop-shadow `Shadow` explicitly (Pitfall 13). `gradientLine` follows the CSS rule: the line passes through the centre along (sin a, -cos a) with length |w sin a| + |h cos a|. `nocturneBrush` is a `ShaderBrush` that builds a `LinearGradientShader` between those two points. `NocturneGradientAngles` sets Primary 90, Hero 135, Sky 180, Card 135, FoilBorder 135 and HeroAurora 135.
- **Components.** `NocturneButton` is 52 dp tall with a minimum width of 48 dp, a pill shape and `labelL` text. Clicks are clipped to the pill and the button has the Button role. Disabled shows the same colours at 0.38 alpha. `NocturneChip` uses a Muted pill, `labelM` text and an optional dot. `AdChip` shows "AD" in `overline` with a 1 dp AdOutline border. `FeatureCard` has an Lg radius, the Card gradient, a 1 dp foil border brush, GlowCard, and `titleM` / `bodyM` (MutedForeground) text.
- **Status chip.** The dot is StatusSuccess for Connected, StatusError for Unreachable and MutedForeground for Checking. The cause and base URL are in `caption`, and Retry is a Ghost `NocturneButton`. All texts and the single-flight `retry()` are unchanged.
- **NocturneBackground** draws 60 stars from a fixed seed in three layers (1 to 2.5 dp, TextPrimary and BrandLink). One `rememberInfiniteTransition` drives twinkle (Ambient and AmbientFast with GentleSine, reversing, a per-star phase) and a circular drift (AmbientSlow). The animated values are read only inside `drawBehind`. When `reduceMotion` is on, no transition is created and the stars are drawn at rest.
- **Icon.** `mipmap-anydpi/ic_launcher.xml` sets the background to `@color/nocturne_bg_base` and adds the foreground orb (gold r=20 inside a 2 dp brand-link ring at r=29, all within the 33-unit safe radius) and a monochrome layer. The manifest sets `android:icon` and `android:roundIcon`. The MIUI app drawer shows it (`launcher-drawer.png`).
- **check-motion.sh** saves `animator_duration_scale`, sets it to 0 (off) or deletes it (on), restarts the app, waits 6 s and hashes rows 120 to 1400 twice, 2 s apart. A trap always restores the original value.

## Verification results

| Check | Result |
|---|---|
| `testDebugUnitTest --tests "app.palm.core.designsystem.*"` | EffectsTest 3/3 (90, 180, 135 endpoints) |
| `SMOKE_SCROLLS=10 smoke-phone.sh "Backend: Connected" Primary Secondary Ghost "Scan again" AD "Feature card" Retry` | OK |
| `SMOKE_NO_REVERSE=1 SMOKE_RETRY_EXPECT="Backend: Connected" smoke-phone.sh "Backend: Unreachable" Retry` | OK, then Connected after Retry |
| `install-debug.sh && check-motion.sh off` | identical hashes (`65a31852...` x2) |
| `check-motion.sh on` | hashes differ |
| `settings get global animator_duration_scale` afterwards | `null` (the phone's original value; restored by the trap after each run) |
| `aapt2 dump badging` | `application-icon-*: res/mipmap-anydpi-v21/ic_launcher.xml` |
| Full `:app:testDebugUnitTest` | pass |
| Acceptance greps (both tasks) | all pass; `dropShadow` appears in exactly 1 file; no `Color(0x` in the new files |

**System setting note:** check-motion.sh changed `animator_duration_scale` on the Redmi four times (two `off` runs and two `on` runs). The original value was `null` (unset). Every run restored it with `settings delete`, and a final read returned `null`.

## Smoothness evidence (input for Phase 3 FND-03, not a gate)

Collected with animations on, the showcase at the top, after `dumpsys gfxinfo app.palm.dev.debug reset` and 10 s of idle:

```
Total frames rendered: 609
Janky frames: 609 (100.00%)
50th percentile: 24ms
90th percentile: 25ms
99th percentile: 40ms
Number Missed Vsync: 0
Number High input latency: 609
Number Slow UI thread: 0
Number Slow issue draw commands: 0
Number Frame deadline missed: 0
```

The display runs at 60 Hz and the app rendered about 60.9 fps. All 609 "janky" frames come only from the `High input latency` counter: no frames missed vsync, missed a deadline or had a slow UI thread. That counter is known to flag every animation-driven frame on Android 11 when there is no touch input, so this looks like a counter artefact, not real jank. Before the content layer was added, the same run gave the same numbers (607 frames, 24/25/38 ms). Phase 3 should use `FrameTimeline`-based metrics or Macrobenchmark, not the `Janky frames` line.

## Screenshots for the phase-end visual check (git-ignored, local)

- `tools/adb/shots/showcase-top.png`: status chip Connected (green dot), Retry, all buttons, the chip, the feature card, stars
- `tools/adb/shots/showcase-components.png`: buttons, chip, feature card glow and foil border, start of Colours
- `tools/adb/shots/showcase-type.png`: all 11 styles with Hindi lines
- `tools/adb/shots/launcher-drawer.png`: [AppName] with the gold orb icon in the MIUI drawer

## Task Commits

1. **Task 1 (tracer): token-styled buttons, chips, feature card and restyled status chip** - `1a8f1e1` (feat)
2. **Task 2: animated star background, vector orb adaptive icon and motion check** - `aca129a` (feat)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Correctness] NocturneBackground provides LocalContentColor**
- **Found during:** Task 2
- **Issue:** Replacing the showcase `Surface` with `NocturneBackground` drops Surface's content colour, so plain `Text` would fall back to black.
- **Fix:** NocturneBackground wraps its content in `CompositionLocalProvider(LocalContentColor provides NocturneSemantic.Foreground)`.
- **Files:** NocturneBackground.kt. **Commit:** aca129a

**2. [Rule 2 - Performance] Content in its own graphicsLayer**
- **Found during:** Task 2 gfxinfo run
- **Issue:** the stars invalidate the draw of the Box that also hosts the content, which would re-record the whole showcase (text, glows) every frame on low-end phones.
- **Fix:** the content sits in `Box(Modifier.fillMaxSize().graphicsLayer())`. The gfxinfo counters stayed the same (see above), so this is a cheap safeguard rather than a measured win.
- **Files:** NocturneBackground.kt. **Commit:** aca129a

**3. [Rule 3 - Blocking] Two compile fixes**
- `Size.center` needs `import androidx.compose.ui.geometry.center`. The `InfiniteTransition.loop` helper needed `@Composable`. Both were fixed before their task's commit.

**Total deviations:** 3 auto-fixed. **Impact:** none on scope.

## Known Stubs

- The launcher icon is a vector-drawn orb that stands in for the Figma [Guide] illustration. The user approved this on 2026-11-26 (D-09), and the three-layer structure means swapping in the real art only replaces assets. It does not block the plan goal.

## Notes for Phase 3

- `NocturneBackground` is the base to extend for FND-03's 6-layer background. Add layers inside the same `drawBehind`, and keep animated reads in the draw phase.
- `reduceMotion` is read once per process (NocturneTheme `remember`), so a change in the setting applies after the app restarts. check-motion.sh relies on this.
- With edge-to-edge, scrolled content passes under the transparent status bar; at rest, the content respects `safeDrawing`.

## Self-Check: PASSED

- All 10 created files exist. Commits 1a8f1e1 and aca129a are ancestors of HEAD, and plan_head_before..HEAD counts 2 commits.
