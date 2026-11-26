# Pitfalls Research

**Domain:** AI palm-reading and astrology Android app (Kotlin/Compose, CameraX + MediaPipe Hand Landmarker, FastAPI + Claude vision, ads-only, mass market India/SEA/LatAm on low/mid-end phones)
**Researched:** 2026-11-26
**Confidence:** MEDIUM-HIGH. Camera/MediaPipe/LLM/ads/networking items are well-established engineering knowledge (HIGH). Policy dates and legal points (Play deadlines, India DPDP) were partly verified and are flagged where they need a recheck before release.

Phase labels used below are a proposed split of Milestone 1. The roadmapper may rename them, but the dependencies hold.

- **P-Shell**: app shell, Nocturne theme, animated background, navigation, onboarding, home
- **P-Scan**: hand selection, CameraX, MediaPipe guidance, capture, gallery import
- **P-Pipeline**: FastAPI job API, Claude vision call, schema, cache, safety filter
- **P-Result**: generating screen, result tabs, line overlay, Room storage, invalid-photo screen
- **P-Harden**: ad-slot interface, privacy and Play-readiness, release build checks, on-device matrix

---

## Critical Pitfalls

### Pitfall 1: Trusting the vision LLM's polylines for the line overlay

**What goes wrong:**
The result screen draws Heart/Head/Life lines over the user's photo using `trace[[x,y]...]` returned by Claude. The lines land on fingers, off the palm, in the background, or cross each other. Every re-scan draws different lines. The "magic" moment becomes the most visibly fake moment. This is the Core Value screen.

**Why it happens:**
Anthropic's own docs say Claude's spatial reasoning is limited and precise localisation is a weak spot. The model also sees a resized image (long edge 1568 px on the standard tier; the token budget can shrink it further), so coordinates are in the resized space, not your original. Small, thin targets such as palm creases lose the most precision when the image is shrunk. The PRD fast-start (R-PALM-4) asks the LLM for geometry it is not good at.

**How to avoid:**
- Treat LLM polylines as untrusted hints, never as final geometry.
- Use the on-device landmarks as ground truth. Hand Landmarker already gives wrist, MCP and finger joint positions. Build the overlay from landmark-anchored curves (for example Heart line from the pinky-side palm edge toward the index/middle gap, Life line curving around the thumb base) and let the LLM only choose a few parameters, such as "long/short, curved/straight, broken/forked". The LLM's text then matches what is drawn.
- If LLM polylines are kept, pre-resize the image yourself to a known size (long edge 1568 or less), send that exact size in the prompt, ask for coordinates normalised 0..1 on that image, and return `image_size` that matches. Never assume your original dimensions.
- Server-side validation before returning: every point inside [0,1]; every point inside the hand's convex hull (from landmarks the client sends, or a server-side landmark run); point count 5..40; no self-intersection; each line within its expected palm zone. On failure, fall back to template curves warped to landmarks and do not fail the reading.
- Crop to the palm region (landmark bbox plus 15% margin) at full resolution before upload. Lines get more pixels, tokens drop, and background and faces leave the image. Return coordinates relative to the crop, with the crop rectangle, so the client maps back.
- Label lines "approximate" in copy. The PRD says the original draws traces too, so expectations are set, but misaligned lines are still read as broken.

**Warning signs:**
Lines outside the hand hull in spot checks. Line endpoints identical across different photos (the model returning canned coordinates). Coordinates that look correct only when the photo is exactly 1568 px. Different polylines on the same photo across runs.

**Phase to address:** P-Pipeline (contract and validation) and P-Result (renderer). Decide the geometry strategy before building the overlay. Retrofitting after the UI exists is a rewrite.

---

### Pitfall 2: Palm-facing detection that flips between states or passes the back of the hand

**What goes wrong:**
The guidance shows "palm detected" for the back of the hand (or flickers between "turn your palm" and "hold still"), the front camera path shows swapped Left/Right, or a fist briefly passes. Users either get stuck on a screen that will not accept their palm, or upload back-of-hand photos that the server then rejects, costing 10+ s and one LLM call.

**Why it happens:**
- MediaPipe's handedness label assumes a mirrored (selfie) input. With the back camera and an unflipped image the label is swapped, and the docs tell you to swap it yourself.
- Landmark `z` is relative to the wrist, not metric depth, so a "z says palm" heuristic is noisy.
- In video/live-stream mode MediaPipe tracks the previous box and only re-runs palm detection when tracking is lost, so a wrong handedness can persist across frames.
- The handedness score is always at least 0.5, so a low-confidence label looks decisive.
- Devs test with their own right hand only, in good light.

**How to avoid:**
- Decide palm vs back from geometry plus handedness, not handedness alone. Compute the 2D cross product of (wrist to index MCP) and (wrist to pinky MCP). Its sign, combined with the corrected handedness and the user's selected hand (S07), tells you which side faces the camera. Only the combination of the three is reliable. This is an inference from the landmark layout; validate it on recorded footage (MEDIUM confidence).
- Hand selection matters: the user chose Left or Right on S07, so reject a detected hand whose corrected handedness disagrees only when confidence is above about 0.8, otherwise stay in a "uncertain" state and do not flash errors.
- Debounce with hysteresis. Require N consecutive frames (for example 5 of 6) to enter a state and M frames to leave it. Never change the hint text faster than about 400 ms.
- Build a state machine as a pure Kotlin class that takes a landmark list and returns a state, and unit test it against recorded landmark JSON from real hands: left and right, palm and back, fist, spread, fingers together, far, near, rotated 90 degrees, with rings, in bad light. The ARTEMIS device can capture the exploration; commit the fixtures.
- Make "fingers spread" scale-invariant (angle between index and pinky MCP vectors, as in R-PALM-1), and normalise "hold still" motion by hand-bbox size, not by raw pixels per frame (R-PALM-1's px/frame threshold breaks across resolutions).
- Run a final check on the captured high-resolution still: run Hand Landmarker in IMAGE mode on it, confirm palm-facing and bbox, and only then upload. The hand moves between the last good analysis frame and the still.

**Warning signs:**
Hint text flickering. Right hand reported as Left on the back camera. The "palm detected" state reachable with a fist at distance. Users on S08 for more than 15 s with no state progress in analytics.

**Phase to address:** P-Scan. Build the state machine and fixtures first, then the camera UI on top.

---

### Pitfall 3: Frame analysis that works on a Pixel and janks, overheats or ANRs on a Redmi 9A / Realme C / Galaxy A0x

**What goes wrong:**
Preview stutters, the phone gets hot in 30 s, guidance lags by half a second, the app is killed for memory, or the GPU delegate crashes on some chipsets (Mali/PowerVR drivers on budget MediaTek and Unisoc SoCs).

**Why it happens:**
Developers run Hand Landmarker at full analysis resolution on every frame, convert YUV to Bitmap on the main thread, allocate a new Bitmap per frame, or forget to close the `ImageProxy`. On 2-3 GB RAM devices, GC churn alone causes dropped frames. GPU delegate behaviour varies by OEM driver.

**How to avoid:**
- `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST`, target resolution about 640x480, `OUTPUT_IMAGE_FORMAT_RGBA_8888` (avoids a manual YUV conversion), on a single dedicated analyzer executor. Always `close()` the `ImageProxy` in a `finally`.
- Reuse one Bitmap/buffer. Apply `imageInfo.rotationDegrees` before inference; a wrong rotation silently kills detection in portrait.
- Use `RunningMode.LIVE_STREAM` with the async listener, not IMAGE mode per frame.
- Default to the CPU delegate on first run. Offer GPU only behind a try/catch with a persisted "GPU failed" flag. CPU is fast enough for one hand at 640 px on mid-range chips; do not take crash risk for a latency win.
- Throttle inference to about 10-15 fps. A 15 fps ceiling is invisible to users and halves heat.
- Stop analysis when the screen is not resumed or when the state is "locked, capturing".
- Test on a real low-end device with release build, not the emulator and not a flagship. Track frame time p95 and thermal status (`PowerManager.getCurrentThermalStatus`) during a 2-minute scan session.

**Warning signs:**
Analyzer time per frame over 60 ms on the reference Xiaomi. Memory growth in the Android Studio profiler during a scan. `ImageAnalysis` backpressure warnings in logcat. Phone warm after one minute.

**Phase to address:** P-Scan. Write the 2-minute soak test on the reference phone into the phase's success criteria.

---

### Pitfall 4: Stripping EXIF and losing image orientation (sideways palms), plus gallery-import surprises

**What goes wrong:**
The privacy step "strip EXIF/GPS before upload" removes the orientation tag. The server (and Claude) now sees a rotated image, the quality gate says "no palm", and the overlay is drawn rotated. Gallery photos behave differently from camera photos. HEIC/WebP, very large images and photo-picker URIs add more edge cases.

**Why it happens:**
CameraX `ImageCapture` and most cameras store rotation in EXIF rather than in pixels. Re-encoding or metadata stripping without applying the rotation first is the classic bug. It only shows on some devices and orientations, so it passes on the dev phone.

**How to avoid:**
- Pipeline order: decode, apply EXIF orientation to pixels, crop to palm ROI, downscale to the target size, re-encode JPEG q85 (R-PALM-2: at least 1080 px short side applies to the crop's source, not necessarily the upload; cap the upload at about 1568 on the long edge since Claude resizes anyway), write with no metadata. Re-encoding from a decoded Bitmap strips EXIF by construction. Do not use a "remove tags" call on the original file.
- Use `ImageCapture.OnImageCapturedCallback` with `imageInfo.rotationDegrees`, or capture to file and read orientation before decode.
- Gallery import goes through the system Photo Picker (no storage permission needed), is decoded with a bounded sample size to avoid OOM on 2 GB devices, and runs through the same pipeline including on-device hand validation. Do not trust a gallery photo just because the user picked it.
- Add a Robolectric or instrumented test that feeds a photo with EXIF orientation 6 and asserts the output pixel orientation.

**Warning signs:**
Server rejections that correlate with a specific device model. Overlay lines rotated by exactly 90 degrees. OOM crashes on gallery import of 48-108 MP photos.

**Phase to address:** P-Scan (pipeline) with a regression test in P-Result.

---

### Pitfall 5: Photo quality failures disproportionate to skin tone, lighting, henna and low-end cameras

**What goes wrong:**
Dark-skinned hands in indoor light are called "too dark" or "no palm lines". Mehndi/henna patterns, rings, bangles and nail art confuse the model. Autofocus on budget phones hunts at 15-25 cm, so the captured still is soft even though the guidance said "hold still". The app rejects valid photos and users blame the app.

**Why it happens:**
- A single global brightness or Laplacian-variance threshold is resolution- and skin-tone dependent.
- Preview frames (low res) are checked while the high-res still is what gets uploaded.
- The reference app lists 7 invalid reasons (blurry/dark, too far, jewelry, bent, fingers too tight, incomplete, no palm lines); the LLM is a poor and inconsistent judge of "blur".
- Henna is common on palms in India and is invisible to a generic test set.

**How to avoid:**
- Do cheap, deterministic quality checks on device on the captured still, cropped to the palm ROI: Laplacian variance normalised by crop size, mean luminance measured over the palm region only (not the whole frame), and highlight clipping. Calibrate thresholds on a labelled set that deliberately includes deep skin tones, warm indoor light, and flash on/off.
- Wait for camera focus state to settle before capture (tap-to-focus at the palm centre, `FocusMeteringAction`, then capture). Offer the torch toggle but warn it can wash out lines.
- Let the server verdict handle only what the device cannot: jewelry, henna, occlusion, bent fingers. Keep the 7-reason enum fixed so the invalid-photo screen is simple, and add `uncertain` so a doubtful verdict can still produce a reading with a soft warning rather than a hard reject.
- Build a test set before tuning: about 40 photos covering Fitzpatrick range, henna, rings, bangles, kids, elderly (faint lines), motion blur, backlight. Track false-reject rate per category. Target false rejects under 5% on "valid" photos.
- Never use skin-colour thresholding for hand segmentation.

**Warning signs:**
Reject rate higher on specific testers. Same palm accepted in daylight and rejected indoors. High "no palm lines" verdicts on clearly good photos.

**Phase to address:** P-Scan (on-device checks, focus) and P-Pipeline (verdict prompt, test set).

---

### Pitfall 6: Non-deterministic readings ("same photo, different reading")

**What goes wrong:**
User scans, re-scans the same hand and gets a different personality, different line descriptions, and different Love/Career text. The product's credibility collapses on the second scan, and screenshots end up in reviews.

**Why it happens:**
The Messages API has no seed parameter, and temperature 0 reduces variation but does not guarantee identical output (the search did not confirm this against the API reference; treat as MEDIUM and check the current docs). A SHA-256 of JPEG bytes also differs after any re-encode, and a new capture of the same hand is a different image anyway.

**How to avoid:**
- Cache by a perceptual hash of the normalised palm crop (dHash/pHash on the cropped, downscaled, grayscale image) plus hand side, plus prompt version, plus language. PRD R-PALM-7 says "image hash for 24 h"; make it a perceptual hash with a Hamming distance threshold, and extend the TTL to at least 7 days for the same install. Store result JSON only, never the image.
- Client side, the stored Room reading is the source of truth. A re-scan within a short window offers "view your last reading" first.
- Reduce free variation: temperature 0 to 0.3, structured JSON output with a schema, a rubric prompt that maps measurable features (long/short, curved, broken, forked) to fixed phrases, then let the LLM write prose from those fields. Optionally two-stage: stage 1 extracts features as enums, stage 2 writes text from enums. Stage 1 repeated runs agree far more often than prose.
- Make the stage-1 feature object part of the cache and log it (not the image) so you can measure consistency.
- Include the prompt/schema version in the cache key so a prompt change does not serve stale shape.

**Warning signs:**
Repeat-scan tests on one fixed photo producing different `lines[].type` sets or tone. Cache hit rate near zero in logs.

**Phase to address:** P-Pipeline. Write a consistency test (same fixture 5x, assert enum equality) as an acceptance check.

---

### Pitfall 7: Latency and cost blowups from the vision call, and a public endpoint anyone can drain

**What goes wrong:**
p95 exceeds 30 s because of full-resolution images, long prompts and a large model. Or the unauthenticated upload endpoint is found by a script and burns the Anthropic budget overnight. Or the rewarded-ad gate is enforced only on the client, so a modified client gets unlimited readings.

**Why it happens:**
No accounts means no natural auth. Image tokens scale with pixels (roughly width x height / 750), so a 4000 px upload is expensive and slower. Local dev hides rate and abuse problems. On Cloud Run later, work done after the HTTP response is CPU-throttled, which stalls "background" jobs.

**How to avoid:**
- Cost and speed: upload crop at 1024-1568 px long edge; Haiku-class or Sonnet-class chosen by eval on the test set (start with the cheaper model, escalate only if quality fails); put the long static system prompt in a cacheable prefix (mind the minimum cacheable length per model); use structured outputs/JSON-schema constrained output so there are no parse retries; set `max_tokens` tight; retry only on 429/5xx/overloaded with jittered backoff and a hard 45 s job deadline (R-PALM-5).
- Abuse: per-install token (random UUID issued on first launch, stored in DataStore) plus per-IP and per-token rate limits; Play Integrity token verification before Cloud Run launch (design the header now, enforce later); a daily global spend cap with a kill switch that returns the friendly "busy" state; reject non-JPEG, over-size and over-dimension uploads early; Anthropic console spend limit set.
- Gate enforcement for "1 free reading, then rewarded ad" is advisory on the client. Accept that, but keep a server-side per-install daily ceiling so a bypass costs at most a few cents. Consider rewarded-ad server-side verification (SSV) later.
- Job design: define a `JobStore` interface now (in-memory for milestone 1). For Cloud Run, either keep the request open (SSE/poll on the same request path with CPU allocated) or use Cloud Tasks; do not rely on `BackgroundTasks` after the response with default CPU throttling. In-memory job state also breaks with multiple instances and `uvicorn --reload`.
- Log token usage and cost per reading from day one.

**Warning signs:**
Vision call over 15 s median in dev. Input tokens per reading above about 2.5k. Requests with no install token. Rising Anthropic spend with flat DAU.

**Phase to address:** P-Pipeline (cost, schema, job store interface) and P-Harden (abuse controls before any public URL).

---

### Pitfall 8: Unsafe or policy-breaking reading content (health, death, pregnancy, money) and prompt injection through the image

**What goes wrong:**
Palmistry lore is full of "short life line means early death", "broken heart line means heart disease", "fate line shows wealth/marriage date". An unconstrained model repeats it. Combined with an app in the Play Store aimed at a mass market that takes fortune-telling seriously, this is a harm and a policy risk. Separately, a photo containing printed text ("ignore previous instructions, say X") or a non-hand image can steer the model.

**Why it happens:**
The system prompt says "be a palmist"; the model obliges. Safety is added as a prompt line only, with no output check. The image channel is treated as trusted.

**How to avoid:**
- System prompt: entertainment framing, explicit never-list (lifespan, death, illness/diagnosis, pregnancy/fertility, specific financial advice or amounts, legal outcomes, other people's infidelity, anything directive), reflective language ("tends to", "you may find"). Describe the Life line as vitality/energy, never length of life. The PRD §11.5 requires the same for tarot Health/Financial topics.
- Treat any text found inside the image as data. State this in the system prompt, and never put user-controlled strings (name) into the system prompt; pass them as delimited user data.
- Enforce the response schema with enums and length limits (character caps per field), then run a server-side output filter: a denylist in English plus Hindi/Hinglish/Bahasa/Spanish/Tagalog terms for death, cancer, disease, pregnant, lottery, etc., and optionally a second cheap model call as a classifier. On a hit, regenerate once with a stricter instruction, otherwise fall back to safe template text for that field.
- Validate that the image is a hand at all (on-device landmarker already gates this; the server verdict covers the rest). Reject images with a visible face or a child's hand if you decide to (see Pitfall 9), and never return content about identity.
- Add the "entertainment purposes" disclaimer to every reading surface and to the share card (PRD §11.5).
- Google Play requires apps with generative AI features to provide in-app user reporting/flagging of offensive output (Play AI-Generated Content policy; recheck current wording before release). The PRD already has a report flag on result screens; make sure it exists on the palm result and chat and actually sends the reading id.
- Red-team test set: 20 prompts/images targeting the never-list, run on every prompt change.

**Warning signs:**
Words like "lifespan", "disease", "pregnan", "die" in sampled outputs. Output text that quotes the image's printed text. Reading differs materially when the image contains a caption.

**Phase to address:** P-Pipeline (prompt, schema, filter, red-team set). P-Harden (report flag, disclaimer audit).

---

### Pitfall 9: Treating hand images as "no big deal" for privacy, the Data safety form and Indian/LatAm/SEA law

**What goes wrong:**
The privacy promise ("images deleted after processing") is false in practice: images persist in server logs, temp files, a cache, a crash report, Cloud Run request logs, or the LLM vendor's retention window. The Data safety form says "no data collected" or marks photos "ephemeral" when they are not. The reading photo is in Android auto-backup. The app has no age handling although minors will use it.

**Why it happens:**
Palm photos are not obviously biometric, so teams skip the analysis, but palmprints can be used for biometric identification and regulators and reviewers treat hand/palm images conservatively. "Delete after processing" is implemented as "delete at the end of the happy path".

**How to avoid:**
- Server: process in memory where possible; if a temp file is needed, write to a per-request temp dir and delete in a `finally`; never log request bodies or base64; disable request-body logging on the proxy; the cache stores result JSON, not image bytes. Add a test that asserts no image file remains after success, failure and timeout.
- Vendor retention: check Anthropic's current API data retention terms (default retention is not zero; zero-retention is a separate arrangement) and say the true thing in the privacy policy: "image is sent to an AI provider for analysis and deleted from our servers after processing". Do not promise provider-side deletion you cannot prove. (Verify current terms before launch; LOW-MEDIUM confidence as the search did not cover it.)
- Play Data safety form: photos are collected and shared with a service provider (the AI backend). Per Google's help text, data sent off-device may only be omitted from the public section if it meets the narrow ephemeral standard (held in memory, kept no longer than needed for the real-time request). A job queue, a temp file or a cache breaks "ephemeral", so declare it honestly. Also declare ad SDK collection (device IDs, app activity), since third-party SDK collection is your responsibility. Keep the form, privacy policy and actual behaviour in one reviewed checklist.
- On device: store the photo in app-private storage, exclude it from backup (`android:allowBackup="false"` or `dataExtractionRules`/`fullBackupContent` exclusions), delete it when the reading is deleted, and add a "delete all my data" action in settings.
- Strip EXIF/GPS (done via re-encode, see Pitfall 4). Crop to the palm before upload, which also removes faces and rooms from the background.
- Age: the PRD has a 13+ gate. India's DPDP Act defines a child as under 18 and requires verifiable parental consent for children's data, with tracking and targeted advertising to children restricted (verify current DPDP Rules status and get legal review; MEDIUM confidence, my knowledge, not verified in this research pass). For an ads-only app the safe path is: neutral age screen, no personalised ads for under-18 or unknown age (set the ad SDK's child/under-age-of-consent flags), and do not collect birth details of minors beyond what is needed locally. Birth date is already collected on S05; use it for the age gate and keep it on-device.
- Also relevant to the target markets: Indonesia PDP law, Philippines DPA, Vietnam PDPD, Brazil LGPD, and GDPR/UK GDPR for EEA/UK installs. A consent and data-deletion path covers most of them.

**Warning signs:**
Image bytes visible in server logs or Cloud Logging. Files accumulating in the temp dir. Photos present in `adb backup` or Auto Backup output. A privacy policy that says "we never store your photos" while a cache stores them.

**Phase to address:** P-Pipeline (server deletion and no-log tests), P-Result (local storage and backup exclusion), P-Harden (Data safety form, policy text, age gate). Start the privacy checklist in P-Pipeline, not at the end.

---

### Pitfall 10: Ads that break Play policy or the product: unclosable ads, redirects, density, consent

**What goes wrong:**
The reference app (PRD §16.3) shows exactly the failure set: AppLovin full-screen ads with no close control (4 of 4 times on the Redmi, force-stop required), ads auto-opening the Play install sheet, interstitials on nearly every Back press, a rating prompt after every result. Reproducing any of these risks a "disruptive ads" policy action and one-star reviews. Ad SDK work also silently harms cold start and APK size.

**Why it happens:**
Ads are stubbed in milestone 1 behind an interface, then bolted on later with whatever the mediation default is. The interface is designed for "show ad" only, with no consent state, no load readiness, no frequency cap and no reward-callback semantics.

**How to avoid:**
- Design the ad interface now to carry what the real SDKs need: `isReady(slot)`, async `preload(slot)`, `show(activity, slot) -> Flow<AdEvent>` with `Shown/Clicked/Dismissed/Failed/RewardEarned`, a consent gate (`canRequestAds` from UMP), and a `FrequencyPolicy` object (no interstitial more than once per 90 s; none during the first-ever reading; never on Back; no app-open ad over a permission dialog or camera). A fake implementation must be able to simulate "ad never closes", "ad fails to load", and "reward not earned".
- Rewarded logic: grant the reading only on `RewardEarned`; if the ad is not ready or fails, fall back gracefully (let the user retry or give the reading after a short cool-down) rather than leaving them stuck. Never block the primary flow on ad load. Hard 4 s timeout on the splash ad (PRD §10).
- Whitelist ad networks; test every enabled mediation network on the low-end reference device for "closable" and "no auto-redirect". Disable a network that fails. Use test ad unit IDs and test devices in dev, and never tap live ads on your own device (account-ban risk).
- UMP consent: required for EEA/UK; implement it from the start of the ads milestone, request consent before SDK init, and check `canRequestAds` every launch. Do not initialise the SDK before consent resolves where it applies.
- Initialise ad SDKs off the main thread after the first frame so the cold-start budget (under 2 s to first frame) holds; mediation adapters inflate dex count and APK size on low-end devices.
- Rating prompt: Google's in-app review API is quota limited and the guidance is not to prompt repeatedly. Show it once after a positive moment (for example after the second saved reading), never after every result.
- Families/mixed audience: if minors are likely, use certified ad SDKs and a neutral age screen (Play Families policy).

**Warning signs:**
Any full-screen ad without a visible close affordance in an a11y dump. Ad-related ANRs. Cold start regression after adding an SDK. Policy warning emails in Play Console.

**Phase to address:** P-Harden for the interface and fake (milestone 1), then the ads milestone for real SDKs. Add "simulate unclosable ad" to the fake in milestone 1 so the app's own UI is proven to survive it.

---

### Pitfall 11: Google Play release gates discovered at the end (target API, 16 KB pages, new-account testing, listing claims)

**What goes wrong:**
The app builds fine and then cannot be published: target API too low, native libraries (MediaPipe, ad SDKs) not 16 KB page-size aligned, a new developer account missing the closed-testing requirement, or a store listing that promises "accurate predictions".

**Why it happens:**
Release requirements are checked when you try to upload. Today is 2026-11-26.

**How to avoid:**
- Target API: Google's Play Console help states that from 2026-08-31 new apps and updates must target Android 16 (API 36) or higher (one search snippet described the existing-app rule differently; recheck the Play Console page). A greenfield app should start on `targetSdk = 36`, which means edge-to-edge is enforced and predictive back and large-screen behaviours apply. Build the Nocturne shell edge-to-edge from day one rather than retrofitting insets.
- 16 KB page size: apps targeting Android 15+ must support 16 KB pages on 64-bit devices; Google's developer page gives 2027-02-01 as the date after which non-compliant updates cannot be released (earlier dates in older sources appear superseded; verify). Use current AGP and the latest MediaPipe tasks-vision, and the latest ad SDK versions; check alignment with `zipalign -c -P 16` or the APK Analyzer. Verify the .so files inside the MediaPipe AAR specifically (LOW confidence on which MediaPipe version first ships aligned libs; check release notes).
- New personal developer accounts have a closed-testing requirement (a minimum number of testers for a minimum number of days) before production access; check the current numbers in Play Console and start the closed test early, in parallel with development, since it has a calendar cost. An organisation account avoids it.
- Listing and in-app claims: no "100% accurate", "scientifically proven" or medical language. The disclaimer is also a store-listing item.
- Run `bundletool`/internal-track upload in P-Harden, even for a tiny build, to flush out signing, target API and policy declaration problems early.

**Warning signs:**
Pre-launch report or Play Console "app bundle contains native code that is not 16 KB aligned". Console blockers on first upload.

**Phase to address:** P-Shell (targetSdk and edge-to-edge), P-Harden (alignment check, internal-track upload, closed test start).

---

### Pitfall 12: Local development: the phone cannot reach the FastAPI server on the Windows PC

**What goes wrong:**
The upload hangs or fails on the real phone while it works on the emulator and `curl` on the PC. Hours are lost on firewall, cleartext and SSE issues.

**Why it happens (all separate causes):**
- `uvicorn` bound to `127.0.0.1` only.
- Windows Defender Firewall blocks inbound traffic on the Public network profile, and the Wi-Fi adapter is often marked Public.
- Android 9+ blocks cleartext HTTP by default; an `http://192.168.x.x:8000` URL fails with a cleartext error.
- Router "AP/client isolation" blocks phone to PC traffic, common on mobile hotspots and campus or shared Wi-Fi.
- The PC's DHCP address changes; a VPN on the PC or phone reroutes traffic.
- The emulator uses `10.0.2.2` for the host, which is wrong for a physical phone.
- OkHttp's default 10 s read timeout kills an SSE stream and its call timeout breaks long streams; proxies and some carriers buffer SSE.
- Python 3.14 is very new; some compiled dependencies may lack wheels (check `pydantic-core`, `pillow`, `uvloop`; uvloop does not apply on Windows anyway).

**How to avoid:**
- Default to `adb reverse tcp:8000 tcp:8000` and point the debug app at `http://127.0.0.1:8000`. It works over USB, bypasses the firewall and AP isolation, and is stable across IP changes. Put it in `tools/adb` as a script and in the README. Fall back to LAN IP only for untethered tests.
- Bind uvicorn to `0.0.0.0` only when using LAN (and add a firewall rule scoped to the private profile and port 8000); otherwise stay on loopback with adb reverse.
- Allow cleartext only in a `debug` source-set `network_security_config.xml` limited to localhost/10.0.2.2/the dev host; the release manifest keeps cleartext off. Never ship `usesCleartextTraffic="true"` in main.
- Base URL comes from `BuildConfig` per build type, not hard-coded.
- OkHttp for the job flow: use polling as the baseline (simple, proxy-proof) and SSE as an enhancement; for SSE set `readTimeout(0)` on the event-source client, handle reconnects, and fall back to polling after one failure. Cap the total job deadline in the app (45 s, R-PALM-5).
- Pin the server venv to a Python version with full wheel coverage (3.12 or 3.13) if any dependency fails on 3.14, and pin versions in a lockfile.
- Keep the project path in mind: `E:/Palm Reading` contains a space; some Gradle/NDK/native tooling and scripts break on spaces, so quote paths in all scripts and test a clean Gradle build early (LOW severity, but cheap to check in P-Shell).

**Warning signs:**
"Connection refused" vs "timeout" vs "Cleartext HTTP traffic not permitted" (each points to a different cause). Works on emulator only.

**Phase to address:** P-Shell/P-Pipeline kickoff: a "phone hits GET /health" smoke test is the first success criterion of the backend phase.

---

## Moderate Pitfalls

### Pitfall 13: Overlay misalignment (preview, result photo, crop mapping)
**What goes wrong:** Guidance boxes or result polylines are offset because `PreviewView` uses FILL_CENTER (crops) and the result image uses Fit/Contain (letterboxes), the front-camera preview is mirrored, or the crop offset from Pitfall 1 is forgotten.
**Prevention:** One tested coordinate-transform utility (`imageNormalized -> viewPx`) that takes content scale mode, crop rect, mirror flag and rotation. Unit test it with known points. In Compose, draw the overlay in the same layout box as the image, sized by the image's intrinsic aspect ratio (`Modifier.aspectRatio`), and map normalised coordinates to that box. Prefer normalised 0..1 everywhere in the data model.
**Phase:** P-Scan and P-Result.

### Pitfall 14: Compose performance with the animated cosmic background
**What goes wrong:** An always-running animation recomposes the whole screen at 60 fps, jank on low-end GPUs, heat and battery drain. `Modifier.blur`/RenderEffect is a no-op below Android 12 and costly above it. Debug builds look 5-10x slower than release, so the team either over-optimises or ignores real problems.
**Prevention:** Read animation state only in the draw phase (`drawBehind`/`graphicsLayer { }` lambdas, `Canvas` with a single `withFrameNanos` clock) so no recomposition occurs per frame. Cap particle counts (for example 40-80 stars), draw pre-rendered gradients, use few layers. Pause the animation when the screen is not resumed, on the scan screen and the generating screen use a reduced static variant (the camera pipeline needs the CPU), and respect "remove animations"/battery saver (`Settings.Global.ANIMATOR_DURATION_SCALE == 0` or `PowerManager.isPowerSaveMode`). Provide a "low motion" fallback that is a static gradient. Test performance only on a release or `profileable` build on the reference phone, enable a Baseline Profile and R8, and check with Layout Inspector recomposition counts.
**Phase:** P-Shell, with an explicit frame-time budget.

### Pitfall 15: Camera lifecycle, permissions and OEM quirks
**What goes wrong:** Black preview after returning from the permission dialog, after an incoming call, or when another app holds the camera. "Don't ask again" leaves the user with no path. OEM battery managers on Xiaomi/Realme/Oppo kill the process while the app is in the background during the generating step, losing the job.
**Prevention:** Bind CameraX to the lifecycle once, handle `CameraState` errors with a retry and a clear "camera in use / unavailable" state (a milestone requirement); the permission flow needs rationale, a denied state and a deep-link to app settings. Lock portrait for the scan. Persist `job_id` and image-reference in Room/DataStore the moment the upload returns, so on process death the Generating screen resumes by polling instead of restarting. Test on at least: a Redmi (MIUI/HyperOS), a Realme (ColorOS-based), a Samsung A-series (One UI), plus a Pixel or AOSP emulator as control. Keep CameraX current (OEM compatibility quirks are fixed per release).
**Phase:** P-Scan, P-Result.

### Pitfall 16: Believable but samey readings
**What goes wrong:** Users scan their friend's hand and get near-identical text. Love and Career tabs contradict the Palm summary. The product feels like a horoscope generator.
**Prevention:** Ground prose in the extracted feature enums (Pitfall 6) and make each line card cite a visible feature. Generate the three tabs from one shared feature object in one call so they agree. Vary phrasing by feature combination, not by randomness. Include the user's name/hand side in context. Track "readings with identical summary text" as a metric.
**Phase:** P-Pipeline.

### Pitfall 17: No evaluation harness for an LLM feature
**What goes wrong:** Prompt edits silently degrade quality, verdict accuracy or safety, since nothing automated catches it.
**Prevention:** A `fixtures/` folder of 30-50 labelled photos plus a script that runs the pipeline against them and prints: schema-valid rate, verdict agreement with labels, polyline-in-hull rate, safety filter hits, latency p50/p95, cost per reading, repeat-run enum agreement. Run it before every prompt or model change. Keep the real palm photos out of git or use consenting test volunteers only.
**Phase:** P-Pipeline.

### Pitfall 18: Multilingual output and Indic text issues
**What goes wrong:** The model replies in English when the user chose Hindi, or mixes scripts. Indic scripts consume more tokens (cost and latency up). Long strings overflow cards. Devanagari/Tamil rendering on old system fonts is broken or clipped by tight line heights in the design tokens.
**Prevention:** Keep JSON keys and enums in English, put the language code in the user message and require values in that language. Set char/sentence limits per field and test the longest language in each layout. Use Noto fonts bundled or downloadable only if needed (watch APK size), and give Text styles line-height slack for Indic scripts. Milestone 1 can ship English plus a language picker, but write the prompt and layouts so translation is a content change.
**Phase:** P-Pipeline (prompt/schema), P-Result (layout), later localisation milestone.

### Pitfall 19: Job store, SSE and cache designed for one process
**What goes wrong:** Works locally, breaks after deployment: multiple Cloud Run instances do not share in-memory jobs, cache or rate-limit counters; scale-to-zero loses jobs; SSE hits request time limits.
**Prevention:** Hide `JobStore`, `ResultCache` and `RateLimiter` behind interfaces from the start (in-memory implementation now; Redis/Firestore later). Keep the job result retrievable by `job_id` via plain GET so polling works across instances. See also Pitfall 7.
**Phase:** P-Pipeline.

### Pitfall 20: Local storage grows and leaks
**What goes wrong:** Every reading stores a 1-2 MB JPEG indefinitely; low-storage devices fill up; a deleted reading leaves its file; the Room schema changes without a migration and crashes updating users.
**Prevention:** Store a downscaled thumbnail/crop (about 1080 px, q80) only; a delete path removes row and file transactionally; cap history count; export Room schema and write the first migration test from version 1; use app-private storage excluded from backup.
**Phase:** P-Result.

### Pitfall 21: Astrology data inconsistencies (for later modules, but data collected now)
**What goes wrong:** The reference app showed Moon Sign "Capricorn" (tropical) next to Moon "Sagittarius" (sidereal Lahiri) (PRD §16.1). Birth time/timezone/unknown-time handling is wrong, producing inconsistent charts.
**Prevention:** In S05, store birth date, time (nullable with an explicit "I don't know"), and the birthplace's IANA timezone (not just a UTC offset). Label the zodiac system on every screen or choose one globally. Do this data capture correctly in milestone 1 even though the engine comes later, because re-collecting data from existing users is costly.
**Phase:** P-Shell (onboarding data model).

---

## Minor Pitfalls

### Pitfall 22: Disclaimer present but unreadable or skippable
Make it a real text style at 4.5:1 contrast and visible on the result and share card, not only in settings. Check with TalkBack.
**Phase:** P-Result.

### Pitfall 23: Rating prompt and "What's new" sheet shown too often
The reference re-shows its sheet on most returns to home and prompts for ratings after every result (PRD §16.2/16.3). Use a persisted counter: sheet once per version, rating prompt once after a positive moment.
**Phase:** P-Shell / P-Harden.

### Pitfall 24: Lock-screen and misleading-notification features creeping back
Both are in the PRD out-of-scope list. Keep them out of the manifest too (no `USE_FULL_SCREEN_INTENT`, no `SYSTEM_ALERT_WINDOW`). Notifications only for real results.
**Phase:** P-Harden manifest audit.

### Pitfall 25: Clean-room contamination
`_extract/` holds the decompiled reference APK. A copy-paste, a reused string key, a similar persona name or the same ad-unit ID is a legal and Play risk. Keep `_extract/` out of the Gradle/IDE source roots, do not reference its file paths in code comments, and run a "leftover" sweep before release (the replica-brand sweep tool exists for this).
**Phase:** P-Harden.

### Pitfall 26: Secrets in the APK or repo
The Anthropic key must never be in the app (project constraint). Also keep it out of `.env` files committed to git; add `.env` to `.gitignore` before the first backend commit.
**Phase:** P-Pipeline.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Use LLM polylines directly as the overlay | Fastest path to a result screen | Misaligned lines on the Core Value screen; rewrite of the renderer later | Never as the only path; fine as a hint behind validation and fallback |
| SHA-256 of JPEG bytes as the cache key | One line of code | Near-zero hit rate, readings contradict on re-scan | Never |
| In-memory job store and cache | No infra, fast iteration | Breaks on Cloud Run with multiple instances | Milestone 1 local, only behind an interface |
| Hard-code the LAN IP for the phone | Works today | Breaks on DHCP change, leaks into release builds | Never; use `adb reverse` and `BuildConfig` |
| `usesCleartextTraffic="true"` in main manifest | Removes the cleartext error | Release app allows HTTP; Play security flags | Never; debug-only network security config |
| Skip the fake ad's failure modes | Ad slot stub is trivial | Real SDKs reveal unclosable/failed-ad bugs after launch | Never; fake must simulate fail/never-close/no-reward |
| Single global quality threshold | Simple code | Rejects dark skin tones and indoor light | Only until the labelled set exists; replace in P-Scan |
| Put the animated background on every screen including scan | Visual consistency | CPU contention with analysis, heat, jank | Never on the scan and generating screens |
| Prompt-only safety, no output filter | Less server code | One bad output is a policy or harm incident | Never |
| Skip a Room migration test | Faster v1 | Crash on first schema change for existing users | Only before first public release |
| Test on emulator and one flagship | Fast loop | OEM-specific bugs found in reviews | Never for the scan phase; reference Redmi is mandatory |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Claude vision | Assuming returned coordinates are in the original image space | Pre-resize to a known size, tell the model the size, use normalised coordinates, validate |
| Claude vision | Sending 4000 px images | Crop to palm, long edge 1024-1568, JPEG q85 |
| Claude API | Free-text JSON parsed with regex, retries on parse failure | Structured outputs / JSON schema, check refusals and `stop_reason` before parsing |
| Claude API | Retrying on every error including 4xx | Retry only 429/5xx/overloaded with backoff and a total deadline |
| MediaPipe Hand Landmarker | Using the handedness label as-is on a non-mirrored image | Swap or correct for camera and mirroring, combine with landmark geometry |
| MediaPipe | IMAGE mode per frame, or bitmap alloc per frame | LIVE_STREAM mode, reused buffers, throttled to 10-15 fps |
| MediaPipe | GPU delegate by default | CPU default, GPU opt-in with persisted failure flag |
| CameraX | Analyzing the preview frame and uploading it | Capture a high-res still, re-validate with IMAGE mode, then upload |
| CameraX | Ignoring `rotationDegrees` | Apply rotation to pixels before inference and before EXIF strip |
| OkHttp SSE | Default read timeout of 10 s | `readTimeout(0)` for the stream client plus polling fallback |
| FastAPI | Reading uploads into memory unbounded; no size limit | Enforce max body size and dimensions at the edge and in the handler; `python-multipart` installed |
| FastAPI on Cloud Run | `BackgroundTasks` after the response | CPU is throttled after the response; keep the request open or use Cloud Tasks |
| AdMob/MAX | SDK init at app start on the main thread | Init after first frame, off-main, after UMP consent resolves |
| AdMob/MAX | Real ad unit IDs in debug | Test IDs and registered test devices only |
| UMP | Ignoring `canRequestAds` on later launches | Check on every launch; gate preloading |
| Play Data safety | Marking photos "ephemeral" while caching or logging them | Declare accurately; remove caching/logging of images before claiming ephemeral |
| Google Play in-app review | Prompting after each result | Once per positive moment; API is quota-limited |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Full-rate analysis at high resolution | Hot phone, laggy hints, dropped preview frames | 640x480 analysis, 10-15 fps, KEEP_ONLY_LATEST | Any 2-3 GB RAM phone, within 1 minute |
| Per-frame Bitmap allocation | GC pauses, OOM kills | Reuse buffers, RGBA_8888 output | Low-end devices immediately |
| Recomposition from infinite animation state | Constant 60 fps recomposition, jank | Read animation in draw phase only; cap particles; pause off-screen | Mid/low-end GPUs, any release build |
| Large upload on mobile data | Upload dominates the 12 s p50 budget | Crop + downscale, about 150-400 KB | Tier-2/3 networks (2-5 Mbps, high latency) |
| Large image tokens | Slow and costly Claude call | Downscale before upload | Every call; cost visible from day one |
| Cold start with SDKs | Slow first frame (budget under 2 s) | Lazy-init ads/analytics off main thread, Baseline Profile, minimal Application.onCreate | After ad SDKs are added in the ads milestone |
| Unbounded job concurrency | Anthropic 429s and p95 over 30 s | Concurrency limit and queue on the server; friendly busy state | Dozens of simultaneous scans; first UA campaign |
| In-memory cache/job store across instances | Cache misses, 404 on job poll | Shared store before multi-instance deploy | First Cloud Run scale-out |
| Room history with full-size photos | Storage warnings | Store 1080 px crops; cap history | After tens of readings |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Unauthenticated, unthrottled `/palm` endpoint | Anyone drains the LLM budget (HIGH) | Per-install token, rate limits, spend cap and kill switch, Play Integrity before public deploy |
| Rewarded gate enforced only on the client | Unlimited free readings via modified client (MED) | Server-side per-install daily ceiling; optionally ad SSV |
| Image bytes in logs, temp files, caches, crash reports | Breaks the privacy promise and the Data safety form (HIGH) | No-body logging, `finally` deletion, tests; result-only cache |
| Photos in Auto Backup or world-readable storage | Hand images leave the device unexpectedly (MED) | App-private storage, backup exclusion |
| User-controlled text (name) in the system prompt | Prompt injection (MED) | Pass as delimited user data; schema-constrained output; output filter |
| Text inside the photo treated as instructions | Steered or unsafe output (MED) | System prompt rule plus output filter and red-team tests |
| API key in app, repo or Gradle properties | Key theft (HIGH) | Server env var only; `.env` git-ignored; secret scan in CI |
| Cleartext HTTP in release | Interception on public Wi-Fi (MED) | Debug-only network security config; release TLS only (Cloud Run gives HTTPS) |
| No size/type validation on uploads | DoS or decompression bombs (MED) | Max bytes, max dimensions, decode with Pillow limits (`Image.MAX_IMAGE_PIXELS`), verify JPEG magic |
| Permissive manifest (full-screen intent, overlay) | Policy action and user distrust (MED) | Manifest audit; permissions only for camera (and later notifications) |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Hand guidance flickers or contradicts itself | Users give up on the Core Value moment | Hysteresis, single clear instruction at a time, a visual silhouette |
| Hard reject for borderline photos | Feels like the app is broken | `uncertain` verdict yields a reading with a soft tip; invalid screen shows the specific reason with a good/bad example |
| 12-30 s generating screen with a bare spinner | Perceived as hung on slow networks | Staged progress copy, cancel, resumable job, friendly busy state at 45 s |
| Ad before the first reading | Bad first impression and policy risk | None during the first reading of the first session (PRD) |
| Rewarded ad with no fallback when it fails to load | User blocked from the feature they came for | Retry/continue path after a cool-down |
| Interstitial on Back | "Too many ads" ratings (reference app) | Never on Back; one per 90 s cap |
| Result lines drawn confidently but wrong | Loss of trust | Anchor to landmarks, validate, label "approximate" |
| Camera-denied dead end | Lost users | Gallery import path and settings deep-link |
| Reading contradicts the last reading | Loss of credibility | Perceptual-hash cache and "view last reading" |
| Fixed text size and contrast issues | Excludes older and low-vision users | Respect font scale, 4.5:1 contrast, TalkBack labels on all controls |

## "Looks Done But Isn't" Checklist

- [ ] **Hand guidance:** often missing right-hand/left-hand and back-camera cases, rotation, and hysteresis; verify with fixture-driven unit tests and a live test of all 5 states with both hands on the reference Redmi.
- [ ] **Capture:** often missing a still-image re-validation and focus wait; verify the uploaded image is the high-res still, cropped, and sharp.
- [ ] **EXIF strip:** often missing orientation baked into pixels; verify with an orientation-6 test image and with `exiftool` showing no tags on the uploaded file.
- [ ] **Gallery import:** often missing bounded decode and the same validation path; verify with a 100 MP photo and a HEIC.
- [ ] **Line overlay:** often missing crop/scale/mirror mapping and out-of-hull handling; verify with a synthetic hand and on three different aspect ratios.
- [ ] **Determinism:** often missing the perceptual-hash cache and prompt-version key; verify with 5 runs of one photo and 3 re-captures of the same hand.
- [ ] **Server deletion:** often missing deletion on error and timeout paths; verify no image remains on disk or in logs after success, failure and timeout.
- [ ] **Safety filter:** often missing non-English terms and the injected-image case; verify with the red-team set in each supported language.
- [ ] **Disclaimer and report flag:** often missing from the share card and the invalid/empty states; verify on every reading surface.
- [ ] **Rewarded gate:** often missing the fail/never-closes/no-reward paths; verify with the fake ad's failure modes.
- [ ] **First-reading rule:** often missing "no ads in the first reading of the first session"; verify with a fresh install.
- [ ] **Offline and busy states:** often missing airplane-mode-mid-upload and server 429/529; verify both on device.
- [ ] **Process death:** often missing job resume; verify by killing the app during generating and reopening.
- [ ] **Release build:** often differs from debug (R8 strips MediaPipe/serialization classes, cleartext config, base URL); verify a minified release build end to end on the phone.
- [ ] **Play readiness:** often missing target API 36, 16 KB alignment, Data safety draft; verify with an internal-track upload.
- [ ] **Cold start:** often missing a measurement; verify under 2 s to first frame on the reference Redmi, release build.

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Overlay built on raw LLM polylines | HIGH | Introduce a landmark-anchored renderer behind the existing contract; treat LLM output as hints; ship via remote-config switch |
| Back-of-hand false positives shipped | MEDIUM | Add geometry check and hysteresis; ship a patch; widen server verdict rejection meanwhile |
| Readings not deterministic | MEDIUM | Add perceptual-hash cache server-side now; client "last reading" prompt; two-stage extraction later |
| LLM budget drained by abuse | MEDIUM | Spend-cap kill switch, rotate API key, add install token and rate limit, enforce Play Integrity |
| Unsafe reading text reaches users | MEDIUM | Server-side filter hot-fix (no app update needed since the filter is server-side), prompt change, re-run red team |
| Privacy promise violated (images in logs) | HIGH | Purge logs, fix, correct the privacy policy and Data safety form, consider user notice depending on jurisdiction |
| Policy strike for ads | HIGH | Remove the offending network/placement via remote config, appeal with a fix, ship a compliant build |
| 16 KB / target API blocker at upload | MEDIUM | Bump MediaPipe/ad SDK/AGP versions; rebuild; check alignment; request extension in Play Console if eligible |
| Compose jank from the background | LOW | Swap to the static fallback via a flag; move animation state to the draw phase |
| Room schema change without migration | MEDIUM | Ship a migration that rebuilds from stored JSON; worst case destructive migration with user notice |
| Phone cannot reach the dev server | LOW | `adb reverse`, check the firewall profile, check cleartext config |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 1. LLM polylines trusted | P-Pipeline, P-Result | 95% of validated lines inside the hand hull on the fixture set; overlay screenshot review on 3 phones |
| 2. Palm-facing flicker/false pass | P-Scan | Fixture-driven state machine tests; live test with both hands |
| 3. Low-end frame analysis | P-Scan | 2-minute soak on the reference Redmi; analyzer p95 under 60 ms; no thermal throttling |
| 4. EXIF orientation / gallery | P-Scan | Orientation-6 test; 100 MP and HEIC gallery import |
| 5. Photo quality / skin tone / henna | P-Scan, P-Pipeline | False-reject under 5% on valid set across skin tones |
| 6. Non-determinism | P-Pipeline | Same-photo 5-run enum equality; re-capture cache hit |
| 7. Latency, cost, abuse | P-Pipeline, P-Harden | p50 under 12 s / p95 under 30 s; cost per reading logged; rate-limit test |
| 8. Unsafe content / injection | P-Pipeline, P-Harden | Red-team set passes in all supported languages; report flag works |
| 9. Privacy / Data safety / age | P-Pipeline, P-Result, P-Harden | No-image-at-rest test; backup exclusion; form matches behaviour |
| 10. Ads policy and breakage | P-Harden (interface), ads milestone | Fake ad failure modes survive; real-network closability test per network |
| 11. Play release gates | P-Shell, P-Harden | Internal-track upload succeeds; alignment check clean |
| 12. Dev networking | P-Shell / P-Pipeline kickoff | Phone reaches `/health` via adb reverse; documented in README |
| 13. Overlay coordinate mapping | P-Scan, P-Result | Transform unit tests; screenshots on 3 aspect ratios |
| 14. Compose animation cost | P-Shell | Release-build frame time budget on reference phone |
| 15. Camera lifecycle / OEM kill | P-Scan, P-Result | Permission, call-interrupt, process-death tests; 3-OEM matrix |
| 16. Samey readings | P-Pipeline | Distinct-summary rate on fixture set |
| 17. No eval harness | P-Pipeline | Eval script exists and runs before prompt changes |
| 18. Multilingual output | P-Pipeline, localisation milestone | Hindi and one more language rendered without clipping |
| 19. Single-process job store | P-Pipeline | Interfaces in place; second-process test later |
| 20. Local storage growth | P-Result | Delete removes file and row; migration test |
| 21. Astrology data capture | P-Shell | Birth time nullable; timezone stored as IANA id |
| 22-26. Minor items | P-Result, P-Harden, P-Pipeline | Per-item checks above |

## Sources

- Repo evidence, HIGH confidence: `E:/Palm Reading/replica/PRD.md` §5.1 (R-PALM-1 to R-PALM-8), §7.3, §10, §11, §16.1 to §16.3 (on-device findings: unclosable AppLovin ads, auto-redirect to Play, rating prompt after every result, moon sign inconsistency); `E:/Palm Reading/.planning/PROJECT.md` (constraints, scope, dev machine).
- Anthropic vision docs and coordinates guide (https://platform.claude.com/docs/build-with-claude/vision, https://platform.claude.com/docs/build-with-claude/vision-coordinates): resize to 1568 px edge on the standard tier and token-budget resizing; "spatial reasoning abilities are limited"; recommendation to pre-resize and use the exact image size. HIGH.
- Anthropic structured outputs docs (https://platform.claude.com/docs/en/build-with-claude/structured-outputs): schema-constrained JSON via `output_config.format`; check refusals and token limits before parsing. HIGH. The "no seed / temperature 0 not fully deterministic" point is general knowledge not confirmed by the search: MEDIUM.
- MediaPipe Hand Landmarker docs (https://developers.google.com/edge/mediapipe/solutions/vision/hand_landmarker): handedness assumes a mirrored image and should be swapped otherwise; z is relative to the wrist; tracking re-runs palm detection only on loss. HIGH. Palm-vs-back geometric method is my inference: MEDIUM, validate on fixtures.
- Google Play Console help on target API and 16 KB pages (https://support.google.com/googleplay/android-developer/answer/11926878, https://developer.android.com/16kb-page-size): API 36 from 2026-08-31 for new apps/updates; 16 KB support for apps targeting Android 15+, 2027-02-01 cutoff per the developer page; sources partly inconsistent, recheck: MEDIUM.
- Google Play Data safety help (https://support.google.com/googleplay/android-developer/answer/10787469) and https://developer.android.com/security-and-privacy/data/declare-data-use: off-device data must be declared, narrow "ephemeral" exemption, SDK collection is the developer's responsibility. HIGH.
- Google Play Ads and Families policies (https://support.google.com/googleplay/android-developer/answer/9857753, https://support.google.com/googleplay/android-developer/answer/9893335, https://support.google.com/googleplay/android-developer/answer/12918983): disruptive ads definition, no simulating system UI, certified SDKs and neutral age screen for child/mixed audiences. HIGH.
- Not verified in this pass (flagged where used): Play AI-Generated Content policy wording, India DPDP Act age-of-child and consent rules, Anthropic API data retention terms, new-developer-account closed-testing numbers, which MediaPipe release first ships 16 KB-aligned native libraries. Treat as LOW-MEDIUM until checked in the relevant phase research.
- Engineering knowledge, MEDIUM-HIGH: CameraX ImageAnalysis/EXIF orientation behaviour, OkHttp default timeouts, Android 9+ cleartext default, `adb reverse`, Windows Firewall profiles, Cloud Run CPU throttling outside requests, Compose draw-phase state reads, `Modifier.blur` API 31 requirement.

---
*Pitfalls research for: AI palm-reading and astrology Android app*
*Researched: 2026-11-26*
