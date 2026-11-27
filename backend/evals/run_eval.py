"""Eval harness (D-15): runs the real prepare -> PalmModel -> PalmReader pipeline over a photo
set and writes per-call rows plus an aggregates-only markdown summary.

Run from backend/: `python -m evals.run_eval --fake --synthetic` (no key, no photos).
"""

import argparse
import asyncio
import base64
import hashlib
import io
import json
import math
import random
import sys
import time
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from types import SimpleNamespace
from typing import Any

from PIL import Image, ImageDraw, ImageOps

from app.config import Settings
from app.schemas.llm import LlmReading
from app.services import image_prep as ip
from app.services import line_geometry as geo
from app.services.claude_client import Malformed, ModelFailure, PalmModel, Refused, cost
from app.services.palm_reader import LINE_ORDER, PalmReader

# Anchored on the package, never on the cwd (Pitfall 9)
EVALS = Path(__file__).resolve().parent
REPO = EVALS.parents[1]

# 21 normalised right-palm landmarks [x, y, z], wrist low and fingers up (synthetic photos)
SYN_LANDMARKS = [
    [0.50, 0.88, 0.0], [0.36, 0.80, 0.0], [0.27, 0.70, 0.0], [0.21, 0.61, 0.0],
    [0.17, 0.53, 0.0], [0.37, 0.50, 0.0], [0.35, 0.38, 0.0], [0.34, 0.30, 0.0],
    [0.33, 0.22, 0.0], [0.48, 0.48, 0.0], [0.48, 0.35, 0.0], [0.48, 0.26, 0.0],
    [0.48, 0.17, 0.0], [0.58, 0.50, 0.0], [0.60, 0.38, 0.0], [0.61, 0.30, 0.0],
    [0.62, 0.23, 0.0], [0.67, 0.54, 0.0], [0.70, 0.45, 0.0], [0.72, 0.39, 0.0],
    [0.73, 0.33, 0.0],
]
FINGERS = ((1, 2, 3, 4), (5, 6, 7, 8), (9, 10, 11, 12), (13, 14, 15, 16), (17, 18, 19, 20))


@dataclass
class Photo:
    id: str
    hand: str
    expected_ok: bool | None
    expected_reason: str
    group: str
    notes: str
    prep: ip.Prepared
    landmarks: list | None


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="python -m evals.run_eval")
    p.add_argument("--fake", action="store_true", help="fake model client, no key, no spend")
    p.add_argument("--synthetic", action="store_true", help="generated photos instead of --photos")
    p.add_argument("--models", default=None, help="comma list; default PALM_MODEL")
    p.add_argument("--locales", default="en")
    p.add_argument("--repeats", type=int, default=1)
    p.add_argument("--limit", type=int, default=None)
    p.add_argument("--concurrency", type=int, default=4)
    p.add_argument("--photos", type=Path, default=EVALS / "photos")
    p.add_argument("--out", type=Path, default=EVALS / "results")
    return p.parse_args(argv)


def synthetic_jpeg(landmarks: list, size=(960, 1280), shade: int = 0) -> bytes:
    """Skin-toned palm with dark creases on a plain background, sharp and bright."""
    w, h = size
    img = Image.new("RGB", size, (70 + shade, 92 + shade, 110 + shade))
    draw = ImageDraw.Draw(img)
    px = [(x * w, y * h) for x, y, _ in landmarks]
    skin = (214 + shade // 2, 172 + shade // 2, 140 + shade // 2)
    draw.polygon(geo.hull([px[i] for i in geo.PALM]), fill=skin)
    for f in FINGERS:
        draw.line([px[0 if f[0] == 1 else f[0]]] + [px[i] for i in f], fill=skin, width=w // 14)
    palm = geo.palm_from_landmarks(landmarks, w, h)
    for key in ("heart", "head", "life", "fate"):
        draw.line(geo.template(key, palm), fill=(120, 78, 62), width=5)
    noise = Image.effect_noise(size, 40).convert("RGB")  # skin texture keeps the image sharp
    img = Image.blend(img, noise, 0.08)
    out = io.BytesIO()
    img.save(out, "JPEG", quality=90)
    return out.getvalue()


def synthetic_set() -> list[tuple[str, str, str, bytes, list]]:
    """(id, hand, group, jpeg, landmarks): one right hand captured twice, one left hand."""
    left = [[round(1 - x, 6), y, z] for x, y, z in SYN_LANDMARKS]
    return [
        ("syn1", "right", "a", synthetic_jpeg(SYN_LANDMARKS), SYN_LANDMARKS),
        ("syn2", "right", "a", synthetic_jpeg(SYN_LANDMARKS, shade=6), SYN_LANDMARKS),
        ("syn3", "left", "b", synthetic_jpeg(left), left),
    ]


class FakeClient:
    """Stands in for AsyncAnthropic: `.messages.parse(**kw)` returns an ok reply whose lines
    follow the template shapes of the registered landmarks, with small jitter. `outcomes`
    are consumed in order (an Exception is raised, None means an ok reply); then ok."""

    def __init__(self, outcomes: list | None = None):
        self.outcomes = list(outcomes or [])
        self.messages = self
        self.images: dict[str, tuple[list | None, str | None]] = {}
        self.rng = random.Random(0)

    def register(self, jpeg: bytes, landmarks: list | None, echo: str | None = None) -> None:
        self.images[hashlib.sha256(jpeg).hexdigest()] = (landmarks, echo)

    async def parse(self, **kw) -> Any:
        await asyncio.sleep(0)
        if self.outcomes:
            out = self.outcomes.pop(0)
            if isinstance(out, Exception):
                raise out
        data = base64.standard_b64decode(kw["messages"][0]["content"][0]["source"]["data"])
        w, h = Image.open(io.BytesIO(data)).size
        landmarks, echo = self.images.get(hashlib.sha256(data).hexdigest(), (None, None))
        palm = geo.palm_from_landmarks(landmarks or SYN_LANDMARKS, w, h)
        j = lambda v: round(v + self.rng.uniform(-2, 2))  # noqa: E731
        lines = {
            key: {
                "present": True,
                "length": "medium",
                "curvature": "gentle",
                "depth": "clear",
                "continuity": "continuous",
                "points": [{"x": j(x), "y": j(y)} for x, y in geo.template(key, palm)],
                "interpretation": f"A clear {key} line suggests a steady, thoughtful nature.",
            }
            for key in LINE_ORDER
        }
        summary = "Your palm shows a calm, curious spirit."
        parsed = LlmReading.model_validate(
            {
                "verdict": {"ok": True, "reason": "none"},
                "lines": lines,
                "summary": f"{summary} {echo}" if echo else summary,
                "traits": ["curious", "patient", "warm"],
                "how_you_love": "You may show care through small, steady gestures.",
                "ideal_partner": "Someone playful who enjoys long talks may suit you.",
                "love_challenges": "You may hold back your feelings at first.",
                "strengths": "You tend to plan carefully and finish what you start.",
                "career_challenges": "You may find noisy settings tiring.",
                "ideal_industries": ["design", "education", "research"],
            }
        )
        usage = SimpleNamespace(
            input_tokens=4000,
            output_tokens=2000,
            cache_creation_input_tokens=0,
            cache_read_input_tokens=0,
        )
        return SimpleNamespace(
            parsed_output=parsed, stop_reason="end_turn", usage=usage, model=kw["model"]
        )


def pct(xs: list[float], q: float) -> float:
    """Nearest-rank percentile."""
    s = sorted(xs)
    return s[max(1, math.ceil(q / 100 * len(s))) - 1]


def usage_of(e: Any) -> Any:
    return getattr(e, "usage", None)


async def attempt(reader: PalmReader, photo: Photo, locale: str) -> tuple[Any, str, float, Any]:
    """One PalmReader.read attempt: (ReadResult or None, outcome, wall seconds, usage)."""
    t0 = time.perf_counter()
    try:
        r = await reader.read(
            photo.prep.jpeg, photo.prep.w, photo.prep.h, photo.hand, locale, photo.landmarks
        )
    except Malformed as e:
        return None, "malformed", time.perf_counter() - t0, usage_of(e)
    except Refused as e:
        return None, "refusal", time.perf_counter() - t0, usage_of(e)
    except ModelFailure as e:
        return None, e.code, time.perf_counter() - t0, None
    dt = time.perf_counter() - t0
    v = r.reading.verdict
    return r, "ok" if v.ok else f"invalid:{v.reason}", dt, r.usage


async def run(a: argparse.Namespace, client: Any, models: list[str], photos: list[Photo]) -> list[dict]:
    sem = asyncio.Semaphore(a.concurrency)
    readers = {m: PalmReader(PalmModel(client, m)) for m in models}
    rows: list[dict] = []

    async def one(photo: Photo, model: str, locale: str, rep: int) -> None:
        async with sem:
            r, outcome, dt, usage = await attempt(readers[model], photo, locale)
        rows.append(
            {
                "photo": photo.id,
                "model": model,
                "locale": locale,
                "repeat": rep,
                "latency_s": round(dt, 4),
                "input_tokens": getattr(usage, "input_tokens", None),
                "output_tokens": getattr(usage, "output_tokens", None),
                "cost": cost(model, usage) if usage is not None else 0.0,
                "outcome": outcome,
                "line_checks": r.line_checks if r else {},
                "safety_hits": r.safety_hits if r else 0,
            }
        )

    locales = a.locales.split(",")
    await asyncio.gather(
        *(
            one(p, m, loc, rep)
            for rep in range(a.repeats)
            for p in photos
            for loc in locales
            for m in models
        )
    )
    return rows


def summary(rows: list[dict], models: list[str]) -> str:
    out = ["# Eval summary", "", "| model | locale | calls | p50 s | p95 s | mean $ | max $ |",
           "|---|---|---|---|---|---|---|"]
    for m in models:
        for loc in sorted({r["locale"] for r in rows}):
            g = [r for r in rows if r["model"] == m and r["locale"] == loc]
            if not g:
                continue
            lat = [r["latency_s"] for r in g]
            c = [r["cost"] for r in g]
            out.append(
                f"| {m} | {loc} | {len(g)} | {pct(lat, 50):.2f} | {pct(lat, 95):.2f} "
                f"| {sum(c) / len(c):.4f} | {max(c):.4f} |"
            )
    return "\n".join(out) + "\n"


def main(argv: list[str] | None = None, settings: Settings | None = None) -> int:
    a = parse_args(argv)
    s = settings or Settings()
    models = a.models.split(",") if a.models else [s.palm_model]
    client = FakeClient()
    photos = []
    for pid, hand, group, jpeg, lm in synthetic_set():
        prep = ip.prepare(jpeg)
        client.register(prep.jpeg, lm)
        photos.append(Photo(pid, hand, True, "", group, "", prep, lm))
    rows = asyncio.run(run(a, client, models, photos))
    out = a.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    for m in models:
        mine = [r for r in rows if r["model"] == m]
        (out / f"{ts}-{m}.json").write_text(json.dumps(mine, indent=1), encoding="utf-8")
    text = summary(rows, models)
    (out / f"{ts}-summary.md").write_text(text, encoding="utf-8")
    print(text)
    return 0


if __name__ == "__main__":
    sys.exit(main())
