"""Generate the synthetic hand-landmark fixtures for the guidance unit tests (D-09).

Usage: python -I tools/gen_landmark_fixtures.py [--check] [--stats DIR]
(no flag)    writes android/app/src/test/resources/landmarks/*.json
--check      regenerates in memory and exits 1 naming the first file that differs or is missing
--stats DIR  prints per-file numbers for the 04-05 calibration (no gate)

Stdlib only, seeded random.Random(4). The canonical hand is our own construction (research
"Synthetic fixtures"): a right palm facing the rear camera, upright, in a 480x640 portrait frame.
Handedness labels are the corrected labels (swapHandedness false, research Pitfall 1).
"""
import json
import math
import random
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "android/app/src/test/resources/landmarks"
W, H = 480, 640
STEP_MS = 83
SCORE = 0.97

# Mirror GuidanceConfig defaults (HandGuidance.kt): these must change together.
TOO_FAR_ENTER, TOO_FAR_EXIT = 0.40, 0.44
MOVE_WINDOW_MS, MOVE_ENTER, MOVE_EXIT = 300, 0.12, 0.08
CURL_RATIO = 1.5

RIGHT_PALM = [
    (240, 560),
    (290, 520), (330, 470), (360, 425), (385, 385),
    (290, 400), (300, 330), (305, 285), (310, 245),
    (245, 395), (248, 315), (250, 265), (252, 220),
    (200, 405), (195, 335), (192, 290), (190, 255),
    (160, 425), (150, 370), (145, 335), (140, 305),
]
FINGERS = [(5, 8), (9, 12), (13, 16), (17, 20)]  # (mcp, tip) for index, middle, ring, pinky
PALM_IDX = (0, 5, 9, 13, 17)


def palm_centre(pts):
    return (sum(pts[i][0] for i in PALM_IDX) / 5, sum(pts[i][1] for i in PALM_IDX) / 5)


def bbox(pts):
    xs, ys = [p[0] for p in pts], [p[1] for p in pts]
    return min(xs), min(ys), max(xs), max(ys)


def bbox_centre(pts):
    x0, y0, x1, y1 = bbox(pts)
    return ((x0 + x1) / 2, (y0 + y1) / 2)


def hand_size(pts):
    return math.dist(pts[9], pts[0])


def mirror(pts):
    return [(W - x, y) for x, y in pts]


def scale(pts, s):
    cx, cy = bbox_centre(pts)
    return [(cx + (x - cx) * s, cy + (y - cy) * s) for x, y in pts]


def rotate(pts, deg):
    cx, cy = bbox_centre(pts)
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def shift(pts, dx, dy=0.0):
    return [(x + dx, y + dy) for x, y in pts]


def fist(pts):
    cx, cy = palm_centre(pts)
    out = list(pts)
    for mcp, tip in FINGERS:
        tx, ty = (pts[mcp][0] + cx) / 2, (pts[mcp][1] + cy) / 2
        for i in range(mcp + 1, tip + 1):
            out[i] = (tx, ty)
    return out


def fraction_scale(pts, fraction):
    """Scale that gives max(bbox w, h) / short side of the 480x640 frame == fraction."""
    x0, y0, x1, y1 = bbox(pts)
    return fraction * min(W, H) / max(x1 - x0, y1 - y0)


def frame(t, label, pts):
    return {"t": t, "handedness": label, "score": SCORE,
            "points": [[round(x / W, 5), round(y / H, 5), 0.0] for x, y in pts]}


def no_hand(t):
    return {"t": t, "handedness": None, "score": 0.0, "points": []}


def fixture(name, chosen, expected, frames, view=(W, H), settle=0):
    data = {"label": name, "chosen": chosen, "expected": expected, "width": W, "height": H,
            "viewWidth": view[0], "viewHeight": view[1], "settleFrames": settle, "frames": frames}
    return f"{name}.json", json.dumps(data, indent=1) + "\n"


def hand_cases(rng, hand):
    label = "Right" if hand == "right" else "Left"
    chosen = label.upper()
    palm = RIGHT_PALM if hand == "right" else mirror(RIGHT_PALM)
    back = mirror(palm)  # same label, mirrored geometry: the back of the same hand
    size = hand_size(palm)
    one = lambda pts: [frame(0, label, pts)]
    cases = [
        (f"{hand}_palm", "PalmDetected", one(palm), (W, H), 0),
        (f"{hand}_back", "BackOfHand", one(back), (W, H), 0),
        (f"{hand}_fist", "ClosedHand", one(fist(palm)), (W, H), 0),
        (f"{hand}_far", "TooFar", one(scale(palm, 0.3)), (W, H), 0),
        (f"{hand}_rot90", "PalmDetected", one(rotate(palm, 90)), (W, H), 0),
        (f"{hand}_rot180", "PalmDetected", one(rotate(palm, 180)), (W, H), 0),
        (f"{hand}_rot270", "PalmDetected", one(rotate(palm, 270)), (W, H), 0),
        (f"{hand}_half", "PalmDetected", one(scale(palm, 0.5)), (1080, 2400), 0),
    ]
    still = [frame(i * STEP_MS, label, [(x + rng.gauss(0, 2), y + rng.gauss(0, 2)) for x, y in palm]) for i in range(30)]
    cases.append((f"{hand}_still", "PalmDetected", still, (W, H), 0))
    # triangle wave of one 0.2-hand-size step per frame: every frame moves
    wave = [0, 1, 2, 1, 0, -1, -2, -1]
    moving = [frame(i * STEP_MS, label, shift(palm, wave[i % 8] * 0.2 * size)) for i in range(30)]
    cases.append((f"{hand}_moving", "Moving", moving, (W, H), 1))
    # starts far, then alternates below the enter value and inside the enter/exit band
    fracs = [0.30] + [TOO_FAR_ENTER - 0.02, TOO_FAR_ENTER + 0.02] * 15
    far_b = [frame(i * STEP_MS, label, scale(palm, fraction_scale(palm, f))) for i, f in enumerate(fracs[:30])]
    cases.append((f"{hand}_far_boundary", "TooFar", far_b, (W, H), 0))
    # one 0.2 step enters Moving; then alternating steps a, b keep the 3-step window sum inside the band
    hi, lo = MOVE_ENTER - 0.01, MOVE_EXIT + 0.01
    a, b = (2 * hi - lo) / 3 * size, (2 * lo - hi) / 3 * size
    x, xs = 40.0 - bbox(palm)[0], []  # start with the hand's left edge at 40 px so it stays in frame
    for i in range(30):
        xs.append(x)
        x += 0.2 * size if i == 0 else (a if i % 2 else b)
    move_b = [frame(i * STEP_MS, label, shift(palm, dx)) for i, dx in enumerate(xs)]
    cases.append((f"{hand}_moving_boundary", "Moving", move_b, (W, H), 1))
    return [fixture(n, chosen, e, f, v, s) for n, e, f, v, s in cases]


def dropout(name, gap_ms, expected):
    frames, t = [], 0
    for _ in range(12):
        frames.append(frame(t, "Right", RIGHT_PALM)); t += STEP_MS
    end = t + gap_ms
    while t < end:
        frames.append(no_hand(t)); t += STEP_MS
    if expected == "PalmDetected":
        for _ in range(6):
            frames.append(frame(t, "Right", RIGHT_PALM)); t += STEP_MS
    return fixture(name, "RIGHT", expected, frames)


def generate():
    rng = random.Random(4)
    files = dict(hand_cases(rng, "right") + hand_cases(rng, "left"))
    files.update([dropout("right_dropout_short", 250, "PalmDetected"), dropout("right_dropout_long", 400, "NoHand")])
    return files


def stats(directory):
    for path in sorted(Path(directory).glob("*.json")):
        data = json.loads(path.read_text(encoding="utf-8"))
        w, h = data["width"], data["height"]
        vw, vh = data.get("viewWidth", w), data.get("viewHeight", h)
        sc = max(vw / w, vh / h)
        short = min(vw / sc, vh / sc)
        hands = [f for f in data["frames"] if len(f["points"]) == 21]
        labels = {}
        for f in data["frames"]:
            labels[f["handedness"]] = labels.get(f["handedness"], 0) + 1
        print(f"{path.name}: label={data['label']} frames={len(data['frames'])} hands={len(hands)}")
        print("  handedness share: " + ", ".join(f"{k}={v / len(data['frames']):.2f}" for k, v in labels.items()))
        if not hands:
            continue
        px = [[(p[0] * w, p[1] * h) for p in f["points"]] for f in hands]
        print(f"  mean score: {sum(f['score'] for f in hands) / len(hands):.3f}")
        chir = [(p[5][0] - p[0][0]) * (p[17][1] - p[0][1]) - (p[5][1] - p[0][1]) * (p[17][0] - p[0][0]) for p in px]
        print(f"  chirality sign: +{sum(c > 0 for c in chir)} / -{sum(c < 0 for c in chir)}")
        ratios = [sum(math.dist(p[t], p[0]) / math.dist(p[m], p[0]) for p in px) / len(px) for m, t in FINGERS]
        print("  tip/mcp ratio (index, middle, ring, pinky): " + ", ".join(f"{r:.2f}" for r in ratios))
        curled = sum(sum(math.dist(p[t], p[0]) < CURL_RATIO * math.dist(p[m], p[0]) for m, t in FINGERS) for p in px)
        print(f"  mean curled fingers: {curled / len(px):.2f}")
        fr = [max(bbox(p)[2] - bbox(p)[0], bbox(p)[3] - bbox(p)[1]) / short for p in px]
        print(f"  mean hand fraction: {sum(fr) / len(fr):.3f}")
        motion = 0.0
        for i, f in enumerate(hands):
            c, size = palm_centre(px[i]), hand_size(px[i])
            for j in range(i):
                if f["t"] - hands[j]["t"] <= MOVE_WINDOW_MS:
                    motion = max(motion, math.dist(c, palm_centre(px[j])) / size)
        print(f"  max motion over {MOVE_WINDOW_MS} ms: {motion:.3f}")


def main(argv):
    if "--stats" in argv:
        i = argv.index("--stats")
        if i + 1 >= len(argv):
            print("usage: --stats DIR", file=sys.stderr)
            return 2
        stats(argv[i + 1])
        return 0
    files = generate()
    if "--check" in argv:
        for name, text in files.items():
            path = OUT / name
            if not path.exists() or path.read_bytes() != text.encode("utf-8"):
                print(f"fixture differs or is missing: {path.relative_to(ROOT)}", file=sys.stderr)
                return 1
        print(f"fixtures OK: {len(files)} files")
        return 0
    OUT.mkdir(parents=True, exist_ok=True)
    for name, text in files.items():
        (OUT / name).write_bytes(text.encode("utf-8"))
    print(f"wrote {len(files)} fixtures to {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
