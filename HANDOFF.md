# Handoff: AI palm-reading app rebuild

**Goal:** clean-room rebuild of an AI palm-reading + astrology Android app, using "Taqde" v1.0.8 (`com.ai.palmreading.scanpalmistry.horoscope`) as the reference. Same features and flows, our own brand, copy, art and backend. Uses the Replica skill pack.

## Done (2026-11-25)

1. **Static recon of the APK** with REA: decoded manifest, string tables (49 locales), and class, API-path and config-key names from the DEX string pools. The code was not decompiled (REA's JADX provider isn't configured).
2. **PRD and feature matrix:** `replica/PRD.md` (§1–15) and `replica/features.csv` (70 rows, scored later by `replica-diff`).
3. **On-device verification** on a Redmi (Android 11, India), about 35 min driven over adb. Findings are in `PRD.md` §16. Biggest ones: no paywall was ever shown and every gate was a rewarded ad; an interstitial fires every 3rd chat message; the love test depends only on the names; the Moon Sign screen (tropical zodiac) contradicts the Vedic chart (sidereal).

## Next step

Answer the open questions in `PRD.md` §15 (brand, launch markets, ads vs IAP, chat history storage, palm dataset), then run **`/replica-architect`** against `replica/PRD.md` and `replica/features.csv`.

## Still unverified (PRD §16.4)

Palm-reading result (needs a real palm photo), paywall screens and prices, chat daily quota, tarot free count beyond 1, notifications.

## Not in this repo (on purpose)

| What | Why | How to get it back |
|---|---|---|
| `AI+Palm+Reading_1.0.8_APKPure.xapk` | third-party app, 97 MB | download v1.0.8 from APKPure, or install "Taqde" from Play |
| `_extract/` (split APKs, dex, decoded manifest, string tables) | third-party content | unzip the XAPK; strings via REA `decode_android_resources` on the base APK |
| `replica/screens/` (26 screenshots of the original) | third-party UI, and this repo is public | re-capture with `tools/adb/shot.sh` |

## Tooling notes

- **Artemis** stopped working because the Gemini key is free tier (20 requests/day for the Flash model). A paid key in `artemis/.env` is needed for full runs.
- **`tools/adb/`** has the helpers used instead: `ui.py` (dumps the screen's UI tree), `t.sh tap|key|text|swipe|none` (act, then dump), `shot.sh NAME`, `back.sh REGEX` (Back and auto-close ads), `relaunch.sh`, `chat.sh "msg"`. They need Git Bash or another POSIX shell, Python 3, adb, and `ADB_SERIAL` set (`adb devices`). The tap coordinates assume a 720×1600 screen.
- **AppLovin full-screen ads** in the original never become closable on that phone. Use `relaunch.sh` (force-stop and relaunch) to get past them.
