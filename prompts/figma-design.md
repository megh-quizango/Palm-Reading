# Prompt: Figma designs for every screen (with options)

Paste everything below the line into Claude Code, opened in the `Palm Reading` repo folder.

---

Design the complete mobile UI for our AI palm-reading and astrology Android app in Figma, using the Figma connector (official Figma MCP) and the **ui-ux-pro-max** skill. Give me several visual directions to choose from first, then design every screen in the direction I pick.

## Inputs (read these first)

- `replica/PRD.md`: §3 lists the screens (S01–S38; S31 Paywall has been removed, so skip it). §4 has the flows, §5 the feature specs, §7.3 the monetisation (ads only, no paywall), and §16 what the reference app really shows on device.
- `replica/features.csv`: rows marked `skip` are out of scope.
- `replica/screens/*.png`: screenshots of the reference app (720×1600). If this folder is missing, work from the PRD text alone. Map:
  - onb1 = S03 onboarding, info1 = S04 name/gender, home1 = S06 home, hand1 = S07 hand selection, scan1 = S08 scanner
  - tarot1/2/3 = S15–S16 tarot pick and reveal, horo1 = S18 horoscope grid, love1 = S29 love result, daily1 = S30 daily report
  - vedic1 = S23 Vedic chart, soul1 = S26 soul energy, moon0/moon1 = S25 moon sign, chat0 = S11 chat, rate1 = S37 rating dialog
  - ad*.png, launch2 and play1 are ad screens: use them only to place ad slots, never as design references

## Rules

1. **Clean room.** Match the reference's *kind* of design (dark cosmic night sky, deep indigo and violet, glowing illustrated feature cards, serif display headings, rounded glassy cards, mystical but readable) and its layouts and flows. But everything must be our own: illustrations, icons, copy, logo and persona name. Use a placeholder brand **"[AppName]"** and a placeholder sage persona **"[Guide]"**. Don't trace, copy or embed any reference artwork, and don't put the reference app's name or text anywhere.
2. **No paywall, no premium.** No paywall screen, crown, VIP badge, lock icon or "Go premium". Gates are rewarded ads: show an "AD" chip on the button (e.g. "Scan again · AD") and a small "Watch a short ad to unlock" sheet.
3. **Ad slots are part of the design.** Use clearly labelled placeholder components: Banner (360×56, bottom), Native card (in-feed, with an "Ad" label), Collapsible native (bottom, with a chevron) and a full-screen interstitial frame. Follow PRD §7.3: no ad during a first-time user's first reading, and no ad covering primary content or CTAs.
4. **Android, Material 3 foundations.** Frames are 360×800 (Pixel-class) with status and navigation bars. Touch targets ≥ 48 dp, text contrast ≥ 4.5:1, dynamic-type-safe text boxes, and an icon-only button always gets a label. SVG icons only, no emoji as icons.
5. **Every screen gets its states,** per the "States to build" column of PRD §3: e.g. empty, loading, error, offline, permission denied, invalid photo with each of its 7 reasons, chat quota reached, rewarded-unlock.

## Step 1: design system options (stop and ask me)

1. Run the ui-ux-pro-max design-system generator with three different directions, all fitting the product (mystical, spiritual, astrology, palmistry, mobile, India/SEA mass market). For example:
   ```
   python <skill>/scripts/search.py "astrology palmistry tarot spiritual mobile app" --design-system -p "[AppName]" --variance 4
   python <skill>/scripts/search.py "astrology palmistry tarot spiritual mobile app" --design-system -p "[AppName]" --variance 8
   python <skill>/scripts/search.py "mystical cosmic night sky premium wellness mobile" --design-system -p "[AppName]" --variance 6
   ```
   Then use `--stack jetpack-compose` for implementation notes, and `--domain ux` for accessibility and touch checks.
2. Turn them into **3 distinct directions**. One stays close to the reference look (dark cosmic plus glowing illustrated cards). One is a lighter or warmer celestial take. One is a bolder editorial or modern take. Each gets: colour roles (with contrast checked), a type pair, radius, elevation and glow, an icon style, an illustration style, and motion notes.
3. In a new Figma file named **"[AppName] – UI"**, make a page **"0 · Directions"**. For each direction, show its tokens and the same 4 key screens side by side: **Home (S06), Hand scan (S08), Palm result (S10), AI chat (S11)**.
4. **Stop and ask me which direction to use** (or which mix). Do not start Step 2 until I answer.

## Step 2: full design in the chosen direction

Build it in the same Figma file:

- **Page "1 · Foundations":** colour, type, spacing, radius, elevation and glow as Figma variables and styles, plus the icon set and illustration guidelines.
- **Page "2 · Components":** buttons (primary, secondary, ghost, with the "AD" chip variant), feature card, hero card, chips, text field, date and time wheel, segmented control, tabs, accordion section, list row, bottom sheet, dialog, toast, loading indicator, skeleton, chat bubbles (user, guide, streaming), suggestion chip, palm-line legend, tarot card (face-down, revealed), zodiac tile, score ring, planet table row, the 4 ad slots, and empty, error and offline blocks. Each one is a component with variants and states.
- **Page "3 · Screens":** one section per flow (PRD §4: F01–F09, F11, F12) with every screen and its states, named `S## Name / State`:
  - **Launch and onboarding:** S01 Splash, S02 Language, S03 Onboarding ×3, S04 Name and gender, S05 Birth date and time
  - **Home:** S06 Home (including the "What's new" bottom sheet and the exit dialog)
  - **Palm:** S07 Hand selection, S08 Hand scan (all live-guidance states: back of hand, open hand, too far, hold still, palm detected; camera denied; camera unavailable), S09 Generating, S10 Hand result (Palm / Love / Career tabs, line overlay on the photo, invalid photo with 7 reasons)
  - **Chat:** S11 AI chat (greeting, suggestions, streaming, scan-request card, quota reached, blocked, busy, unavailable), S12 Conversation, S13 Chat history (with empty state)
  - **Tarot:** S14 Topics (6 topics), S15 Pick a card, S16 Card reveal, S17 Explanation (what to do / what to avoid)
  - **Horoscope:** S18 Sign grid, S19 Result (including translating and offline states)
  - **Astrology:** S20 Astrology hub, S21 Birth info, S22 Place search (with no-results state), S23 Vedic result, S24 Chart style (North Indian, South Indian and Western chart renderings), S25 Moon sign, S26 Soul energy. Label the zodiac system on each of these (sidereal vs tropical), as PRD §16 asks
  - **Love test:** S27 Love test, S28 Checking, S29 Result (with all 7 score bands)
  - **Daily report:** S30 Daily report
  - **Settings:** S32 Settings, S33 Licences, S34 Web view, S35 "Before you go" screen
  - **Dialogs:** S37 Rating (satisfied, then a reasons branch) and S38 Report content
  - **Do not design:** S31 Paywall or S36 Lock-screen activity (both out of scope)
- **Page "4 · Prototype":** wire the F01 first-reading flow and the F02 result-to-chat flow as a clickable prototype.

**Options inside Step 2.** For the 6 highest-impact screens (S06 Home, S08 Scan, S10 Palm result, S11 Chat, S16 Tarot reveal, S29 Love result), make **2 layout variants each** (A and B) next to each other, so I can pick per screen.

## Process

- Load the Figma skill (`/figma-use`) before any `use_figma` call. Create the file with `create_new_file`, then `create_session`. Build with auto-layout and components, not loose shapes.
- Before designing each screen, read its PRD row and its reference screenshot, if one exists.
- After each page, take a screenshot with `get_screenshot` and check it against the ui-ux-pro-max pre-delivery checklist: contrast, touch targets, no emoji icons, text reflow, visible focus states and states covered. Fix any problems before moving on.
- At the end, give me: the Figma file link, a table of every screen ID with its frame name and status (done, or skipped and why), the open design questions, and the design tokens as JSON (`--tokens dtcg`) saved to `design-system/` in the repo.
