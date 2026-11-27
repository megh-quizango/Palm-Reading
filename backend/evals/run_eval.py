"""Eval harness (D-15): runs the real prepare -> PalmModel -> PalmReader pipeline over a photo
set and writes per-call rows plus an aggregates-only markdown summary.

Run from backend/: `python -m evals.run_eval --fake --synthetic` (no key, no photos).
Photo set (git-ignored): <photos>/*.jpg|jpeg|png|webp, <photos>/labels.csv with columns
file,hand_side,expected_ok,expected_reason,group,notes, optional <stem>.landmarks.json.
"""

import argparse
import asyncio
import base64
import csv
import hashlib
import io
import json
import math
import random
import statistics
import subprocess
import sys
import time
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import datetime
from itertools import combinations
from pathlib import Path
from types import SimpleNamespace
from typing import Any

import anthropic  # module import: the client class is looked up at call time (D-16 guard)
from PIL import Image, ImageDraw

from app.config import Settings
from app.jobs import DHASH_MAX_DISTANCE
from app.schemas.llm import LlmReading
from app.services import image_prep as ip
from app.services import line_geometry as geo
from app.services.claude_client import (
    PROMPT_VERSION,
    Malformed,
    ModelFailure,
    PalmModel,
    Refused,
    cost,
    worst_case_cost,
)
from app.services.palm_reader import LINE_ORDER, PalmReader
from evals import redteam

# Anchored on the package, never on the cwd (Pitfall 9)
EVALS = Path(__file__).resolve().parent
REPO = EVALS.parents[1]

ENUM_FIELDS = ("length", "curvature", "depth", "continuity")
FAILURE_KINDS = ("timeout", "busy", "model_error")
RULES = ("pass", "count", "hull", "length", "folds", "pair", "omitted")
TOKENS_PER_POINT = 10  # about 10 output tokens per {"x":..,"y":..} point (RESEARCH Pattern 2)
TEMPLATE_FIRST_AT = 70.0  # D-11: LLM lines valid below this share -> template-first
RETRY_PAUSE = 5.0  # seconds before the one red-team retry on busy or timeout
REDTEAM_FAILS = ("busy", "timeout", "refusal", "malformed", "model_error")

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


@dataclass
class Call:
    kind: str  # "bake" | "redteam"
    photo: Photo
    model: str
    locale: str
    repeat: int
    prep: ip.Prepared  # the image sent: the photo's own, or a red-team rendering
    injection: tuple | None = None  # (topic, script, text) for red-team cells


def parse_args(argv: list[str] | None) -> argparse.Namespace:
    p = argparse.ArgumentParser(prog="python -m evals.run_eval")
    p.add_argument("--fake", action="store_true", help="fake model client, no key, no spend")
    p.add_argument("--synthetic", action="store_true", help="generated photos instead of --photos")
    p.add_argument("--models", default=None, help="comma list; default PALM_MODEL")
    p.add_argument("--locales", default="en")
    p.add_argument("--repeats", type=int, default=1)
    p.add_argument("--limit", type=int, default=None)
    p.add_argument("--concurrency", type=int, default=4)
    p.add_argument("--max-usd", type=float, default=8.0, help="stop scheduling past this charge")
    p.add_argument("--redteam", action="store_true", help="injected-text image red-team first")
    p.add_argument("--redteam-limit", type=int, default=2, help="photos to render injections on")
    p.add_argument("--photos", type=Path, default=EVALS / "photos")
    p.add_argument("--out", type=Path, default=EVALS / "results")
    return p.parse_args(argv)


def ignored(path: Path) -> bool:
    """True when git ignores `path` (probed through a child, so a missing dir still matches a
    directory rule); paths outside the repo are not ours to guard."""
    try:
        rel = Path(path).resolve().relative_to(REPO)
    except ValueError:
        return True
    probe = f"{rel.as_posix()}/.probe"
    return subprocess.run(["git", "check-ignore", "-q", probe], cwd=REPO).returncode == 0


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


def load_photos(d: Path) -> list[Photo]:
    photos = []
    with open(d / "labels.csv", newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            p = d / row["file"].strip()
            lm_file = p.with_name(f"{p.stem}.landmarks.json")
            lm = json.loads(lm_file.read_text(encoding="utf-8")) if lm_file.exists() else None
            photos.append(
                Photo(
                    p.stem,
                    row["hand_side"].strip() or "right",
                    {"true": True, "false": False}.get(row["expected_ok"].strip().lower()),
                    row["expected_reason"].strip(),
                    row["group"].strip(),
                    row["notes"],
                    ip.prepare(p.read_bytes()),
                    lm,
                )
            )
    return photos


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


# ---------- running ----------


def pct(xs: list[float], q: float) -> float:
    """Nearest-rank percentile."""
    s = sorted(xs)
    return s[max(1, math.ceil(q / 100 * len(s))) - 1]


async def attempt(reader: PalmReader, call: Call) -> tuple[Any, str, float, Any, float]:
    """One PalmReader.read attempt: (ReadResult or None, outcome, wall s, usage, charge).
    charge is what the attempt may have cost: actual when usage came back, else worst case
    unless the failure never reached the model."""
    worst = worst_case_cost(call.model)
    p = call.prep
    t0 = time.perf_counter()
    try:
        r = await reader.read(p.jpeg, p.w, p.h, call.photo.hand, call.locale, call.photo.landmarks)
    except (Malformed, Refused) as e:
        kind = "malformed" if isinstance(e, Malformed) else "refusal"
        u = e.usage
        return None, kind, time.perf_counter() - t0, u, cost(call.model, u) if u else worst
    except ModelFailure as e:
        return None, e.code, time.perf_counter() - t0, None, worst if e.billable else 0.0
    dt = time.perf_counter() - t0
    v = r.reading.verdict
    return r, "ok" if v.ok else f"invalid:{v.reason}", dt, r.usage, cost(call.model, r.usage)


def make_row(seq: int, call: Call, r: Any, outcome: str, dt: float, usage: Any, charge: float) -> dict:
    ph, p = call.photo, call.prep
    enums, points = {}, 0
    if r is not None:
        enums = {"verdict_ok": r.llm.verdict.ok, "verdict_reason": r.llm.verdict.reason}
        for k in LINE_ORDER:
            line = getattr(r.llm.lines, k)
            enums |= {f"{k}.{f}": getattr(line, f) for f in ENUM_FIELDS}
            points += len(line.points)
    no_lm = ph.landmarks is None or geo.palm_from_landmarks(ph.landmarks, p.w, p.h) is None
    return {
        "seq": seq,
        "kind": call.kind,
        "photo": ph.id,
        "group": ph.group,
        "hand": ph.hand,
        "model": call.model,
        "locale": call.locale,
        "repeat": call.repeat,
        "latency_s": round(dt, 4),
        "input_tokens": getattr(usage, "input_tokens", None),
        "output_tokens": getattr(usage, "output_tokens", None),
        "cache_read": getattr(usage, "cache_read_input_tokens", None),
        "cache_write": getattr(usage, "cache_creation_input_tokens", None),
        "cost": cost(call.model, usage) if usage is not None else 0.0,  # actual, for reporting
        "charge": charge,  # budget accounting
        "outcome": outcome,
        "expected_ok": ph.expected_ok,
        "expected_reason": ph.expected_reason,
        "no_landmarks": no_lm,
        "line_checks": r.line_checks if r is not None else {},
        "enums": enums,
        "points": points,
        "safety_hits": r.safety_hits if r is not None else 0,
    }


async def run(a: argparse.Namespace, client: Any, calls: list[Call], owned: bool) -> dict:
    """Runs calls in order through one semaphore; stops scheduling once the charge passes
    --max-usd. Returns rows, the first reading per (photo, model) and the budget state."""
    sem = asyncio.Semaphore(a.concurrency)
    readers = {m: PalmReader(PalmModel(client, m)) for m in {c.model for c in calls}}
    st: dict = {"rows": [], "first": {}, "prose": [], "spent": 0.0, "seq": 0, "skipped": 0}

    async def one(call: Call) -> None:
        try:
            seq = st["seq"]
            st["seq"] += 1
            # latency covers only the read await, inside the semaphore (no queue wait)
            r, outcome, dt, usage, charge = await attempt(readers[call.model], call)
            st["spent"] += charge
            attempts = 1
            if call.kind == "redteam" and outcome in ("busy", "timeout"):
                # max_retries=0, so one harness retry keeps a lone 429/529 from failing a cell;
                # the retry is its own charged attempt. Bake-off calls are never retried.
                await asyncio.sleep(RETRY_PAUSE)
                r, outcome, dt, usage, charge2 = await attempt(readers[call.model], call)
                st["spent"] += charge2
                charge, attempts = charge + charge2, 2
            row = make_row(seq, call, r, outcome, dt, usage, charge)
            st["rows"].append(row)
            if call.kind == "bake":
                if r is not None:
                    st["first"].setdefault((call.photo.id, call.model), (call, r.llm))
                return
            topic, script, injected = call.injection
            row |= {"topic": topic, "script": script, "attempts": attempts}
            row |= redteam.score(r.reading, r.llm, injected) if r else {"hits_after": 0, "echoed": False}
            # raw and final prose stay in the git-ignored results dir for the 02-08 human review
            st["prose"].append(
                row
                | {
                    "injected": injected,
                    "raw": r.llm.model_dump(mode="json") if r else None,
                    "final": r.reading.model_dump(mode="json") if r else None,
                }
            )
        finally:
            sem.release()

    tasks = []
    try:
        for i, call in enumerate(calls):
            await sem.acquire()
            if st["spent"] > a.max_usd:
                sem.release()
                st["skipped"] = len(calls) - i
                break
            tasks.append(asyncio.create_task(one(call)))
        await asyncio.gather(*tasks)
    finally:
        if owned:
            await client.close()
    st["rows"].sort(key=lambda r: r["seq"])
    return st


# ---------- metrics ----------


def mean(xs) -> float | None:
    xs = list(xs)
    return sum(xs) / len(xs) if xs else None


def lines_valid(per_photo: list[tuple[int, int]], n: int = 1000, seed: int = 0):
    """(pass %, 95% low, 95% high, template-first verdict) from (passed, total) per photo.
    Resamples photos, not calls: repeats and lines of one photo share an image."""
    if not per_photo:
        return None

    def share(s):
        return 100 * sum(p for p, _ in s) / sum(t for _, t in s)

    rng = random.Random(seed)
    boots = [share(rng.choices(per_photo, k=len(per_photo))) for _ in range(n)]
    lo, hi = pct(boots, 2.5), pct(boots, 97.5)
    verdict = "yes" if hi < TEMPLATE_FIRST_AT else "no" if lo > TEMPLATE_FIRST_AT else "inconclusive"
    return round(share(per_photo), 1), round(lo, 1), round(hi, 1), verdict


def agreement(rows: list[dict]) -> float | None:
    """Per enum field, the modal value's share across repeats of one photo, averaged."""
    by = defaultdict(list)
    for r in rows:
        if r["enums"]:
            by[r["photo"]].append(r["enums"])
    shares = [
        Counter(e[f] for e in runs).most_common(1)[0][1] / len(runs)
        for runs in by.values()
        if len(runs) >= 2
        for f in runs[0]
    ]
    return mean(shares)


def verdict_match(r: dict) -> bool:
    e = r["enums"]
    if e["verdict_ok"] != r["expected_ok"]:
        return False
    return r["expected_ok"] or not r["expected_reason"] or e["verdict_reason"] == r["expected_reason"]


def aggregate(rows: list[dict]) -> dict[tuple[str, str], dict]:
    """Bake-off metrics per (model, locale), in schedule order."""
    bake = [r for r in rows if r["kind"] == "bake"]
    out = {}
    for key in dict.fromkeys((r["model"], r["locale"]) for r in bake):
        g = [r for r in bake if (r["model"], r["locale"]) == key]
        read = [r for r in g if r["enums"]]
        lm = [r for r in read if not r["no_landmarks"]]
        per_photo: dict[str, list[int]] = defaultdict(lambda: [0, 0])
        types = {k: Counter() for k in LINE_ORDER}
        for r in lm:
            for k, v in r["line_checks"].items():
                per_photo[r["photo"]][0] += v == "pass"
                per_photo[r["photo"]][1] += 1
                types[k][v] += 1
        outs = [r["output_tokens"] for r in g if r["output_tokens"] is not None]
        mal = Counter(r["photo"] for r in g if r["outcome"] == "malformed")
        labelled = [r for r in read if r["expected_ok"] is not None]
        repeats = [r for r in g if r["repeat"] > 0 and r["cache_read"] is not None]
        mean_out, mean_pts = mean(outs), mean(r["points"] for r in read)
        lat = [r["latency_s"] for r in g]
        out[key] = {
            "calls": len(g),
            "failures": {k: sum(r["outcome"] == k for r in g) for k in FAILURE_KINDS},
            "p50": pct(lat, 50),
            "p95": pct(lat, 95),
            "mean_out": mean_out,
            "mean_points": mean_pts,
            "points_share": mean_pts * TOKENS_PER_POINT / mean_out if mean_out and mean_pts else None,
            "mean_cost": mean(r["cost"] for r in g),
            "max_cost": max(r["cost"] for r in g),
            "lines": lines_valid([tuple(v) for v in per_photo.values() if v[1]]),
            "line_types": types,
            "no_landmarks": len(read) - len(lm),
            "enum_agreement": agreement(read),
            "refusals": sum(r["outcome"] == "refusal" for r in g),
            "suspected_refusals": sum(n >= 2 for n in mal.values()),
            "malformed": sum(mal.values()),
            "verdict_agreement": mean(verdict_match(r) for r in labelled),
            "cache_share": mean(r["cache_read"] > 0 for r in repeats),
        }
    return out


def fmt(v, spec: str = ".2f") -> str:
    return "n/a" if v is None else format(v, spec)


def photo_tables(photos: list[Photo]) -> list[str]:
    out = ["", "## dHash (re-capture distance)", "",
           "| pairs | n | min | median | current DHASH_MAX_DISTANCE |", "|---|---|---|---|---|"]
    pairs = {"intra": [], "inter": []}
    for p, q in combinations(photos, 2):
        same = bool(p.group) and p.group == q.group
        pairs["intra" if same else "inter"].append(ip.hamming(p.prep.dhash, q.prep.dhash))
    for k, d in pairs.items():
        mn, md = (min(d), statistics.median(d)) if d else (None, None)
        out.append(f"| {k} | {len(d)} | {fmt(mn, 'd')} | {fmt(md, '.1f')} | {DHASH_MAX_DISTANCE} |")
    out += ["", "## Pre-gate", "",
            f"Current thresholds: brightness >= {ip.PREGATE_MIN_MEAN}, "
            f"laplacian_var >= {ip.PREGATE_MIN_LAPLACIAN_VAR}.", "",
            "| expected_ok | n | laplacian_var min / median / max | brightness min / median / max |",
            "|---|---|---|---|"]
    for flag in (True, False):
        ps = [p for p in photos if p.expected_ok is flag]
        cells = []
        for attr in ("laplacian_var", "brightness"):
            v = [getattr(p.prep, attr) for p in ps]
            cells.append(" / ".join(f"{x:.1f}" for x in (min(v), statistics.median(v), max(v))) if v else "n/a")
        out.append(f"| {str(flag).lower()} | {len(ps)} | {cells[0]} | {cells[1]} |")
    rejected = sum(
        p.prep.brightness < ip.PREGATE_MIN_MEAN or p.prep.laplacian_var < ip.PREGATE_MIN_LAPLACIAN_VAR
        for p in photos
        if p.expected_ok
    )
    out.append(f"\nexpected_ok=true photos the current thresholds would reject: {rejected}")
    return out


def redteam_agg(rows: list[dict]) -> dict[tuple[str, str], dict]:
    """Red-team counts per (topic, locale): cells, before/after-filter hits, raw echoes and
    failed cells (no reading) by kind."""
    out: dict = {}
    for r in rows:
        if r["kind"] != "redteam":
            continue
        g = out.setdefault(
            (r["topic"], r["locale"]),
            {"cells": 0, "before": 0, "after": 0, "echoed": 0, "failed": Counter()},
        )
        g["cells"] += 1
        g["before"] += r["safety_hits"]
        g["after"] += r["hits_after"]
        g["echoed"] += r["echoed"]
        if not r["enums"]:
            g["failed"][r["outcome"]] += 1
    return out


def redteam_table(rows: list[dict]) -> list[str]:
    agg = redteam_agg(rows)
    if not agg:
        return []
    out = ["", "## Red-team (injected text in the image)", "",
           f"Pillow raqm: {'yes' if redteam.RAQM else 'no (Devanagari rendered unshaped)'}. "
           "Before = fields the filter rewrote (prompt compliance); after = denylist hits in the "
           "served reading (must be 0); echoed = injected sentence found in the raw model prose.", "",
           "| topic | locale | cells | before-filter hits | after-filter hits | echoed (raw) | failed "
           + " | failed ".join(REDTEAM_FAILS) + " |",
           "|" + "---|" * (6 + len(REDTEAM_FAILS))]
    for (topic, loc), g in agg.items():
        fails = " | ".join(str(g["failed"][k]) for k in REDTEAM_FAILS)
        out.append(f"| {topic} | {loc} | {g['cells']} | {g['before']} | {g['after']} | {g['echoed']} | {fails} |")
    tot = {k: sum(g[k] for g in agg.values()) for k in ("cells", "before", "after", "echoed")}
    failed = sum(sum(g["failed"].values()) for g in agg.values())
    out.append(f"\nTotal: {tot['cells']} cells, {tot['before']} before-filter hits, "
               f"{tot['after']} after-filter hits, {tot['echoed']} echoed, {failed} failed cells.")
    return out


def summary(a: argparse.Namespace, rows: list[dict], photos: list[Photo], st: dict) -> str:
    """Aggregates only: no photo ids, file names, per-photo rows or reading text (D-15)."""
    mode = "fake" if a.fake else "real"
    out = ["# Eval summary", "",
           f"Mode {mode}, prompt {PROMPT_VERSION}, {len(photos)} photos, repeats {a.repeats}, "
           f"concurrency {a.concurrency}, charged ${st['spent']:.4f} of --max-usd {a.max_usd}."]
    if st["skipped"]:
        out.append(f"\nBudget stop: {st['skipped']} scheduled calls not run.")
    agg = aggregate(rows)
    if agg:
        out += ["", "## Bake-off", "",
                "| model | locale | calls | timeout | busy | model_error | p50 s | p95 s "
                "| mean out tok | mean pts/call | pts share | mean $ | max $ | lines valid % (95% CI) "
                "| template-first | enum agree | refusals | suspected refusals | malformed "
                "| verdict agree | cache-read share |",
                "|" + "---|" * 21]
        for (m, loc), g in agg.items():
            f = g["failures"]
            lv = g["lines"]
            lines = f"{lv[0]:.1f} ({lv[1]:.1f}..{lv[2]:.1f}) | {lv[3]}" if lv else "n/a | n/a"
            out.append(
                f"| {m} | {loc} | {g['calls']} | {f['timeout']} | {f['busy']} | {f['model_error']} "
                f"| {g['p50']:.2f} | {g['p95']:.2f} | {fmt(g['mean_out'], '.0f')} "
                f"| {fmt(g['mean_points'], '.1f')} | {fmt(g['points_share'], '.0%')} "
                f"| {g['mean_cost']:.4f} | {g['max_cost']:.4f} | {lines} "
                f"| {fmt(g['enum_agreement'], '.0%')} | {g['refusals']} | {g['suspected_refusals']} "
                f"| {g['malformed']} | {fmt(g['verdict_agreement'], '.0%')} "
                f"| {fmt(g['cache_share'], '.0%')} |"
            )
        out += ["", "## Lines valid per line type (count per rule)", "",
                "| model | locale | line | " + " | ".join(RULES) + " | no landmarks | valid % |",
                "|" + "---|" * (len(RULES) + 5)]
        for (m, loc), g in agg.items():
            for k, c in g["line_types"].items():
                total = sum(c.values())
                cells = " | ".join(str(c[x]) for x in RULES)
                valid = fmt(100 * c["pass"] / total if total else None, ".1f")
                out.append(f"| {m} | {loc} | {k} | {cells} | {g['no_landmarks']} | {valid} |")
    out += redteam_table(rows)
    out += photo_tables(photos)
    return "\n".join(out) + "\n"


def overlay(call: Call, llm: LlmReading, path: Path) -> None:
    """LLM trace (red) and landmark template (blue) per line on the prepared image."""
    p = call.prep
    img = Image.open(io.BytesIO(p.jpeg)).convert("RGB")
    d = ImageDraw.Draw(img)
    lm = call.photo.landmarks
    palm = geo.palm_from_landmarks(lm, p.w, p.h) if lm else None
    for k in LINE_ORDER:
        pts = [(q.x, q.y) for q in getattr(llm.lines, k).points]
        if len(pts) >= 2:
            d.line(pts, fill=(235, 60, 60), width=4)
        if palm is not None:
            d.line(geo.template(k, palm), fill=(60, 150, 235), width=3)
    img.save(path, "PNG")


def write_rows(out: Path, ts: str, rows: list[dict]) -> None:
    for m in dict.fromkeys(r["model"] for r in rows):
        mine = [r for r in rows if r["model"] == m]
        (out / f"{ts}-{m}.json").write_text(json.dumps(mine, indent=1), encoding="utf-8")
        with open(out / f"{ts}-{m}.csv", "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(mine[0]))
            w.writeheader()
            for r in mine:
                w.writerow({k: json.dumps(v) if isinstance(v, dict) else v for k, v in r.items()})


def main(argv: list[str] | None = None, settings: Settings | None = None) -> int:
    a = parse_args(argv)
    s = settings or Settings()
    models = a.models.split(",") if a.models else [s.palm_model]
    photos_dir, out = a.photos.resolve(), a.out.resolve()
    # Public repo: never read photos from, or write results into, a tracked path (Pitfall 9)
    for d in ([] if a.synthetic else [photos_dir]) + [out]:
        if not ignored(d):
            print(f"refusing to run: {d} is not git-ignored", file=sys.stderr)
            return 2
    key = ""
    if not a.fake:
        key = s.anthropic_api_key.get_secret_value().strip() if s.anthropic_api_key else ""
        if not key:
            print("ANTHROPIC_API_KEY missing in backend/.env", file=sys.stderr)
            return 2
    if not a.synthetic and not (photos_dir / "labels.csv").exists():
        print(f"no labels.csv in {photos_dir}", file=sys.stderr)
        return 2
    if a.fake:
        client, owned = FakeClient(), False
    else:
        # 120 s: no call is cut at a latency the bake-off judges; no retries: every 429,
        # 5xx and timeout stays a visible row instead of a hidden re-send
        client, owned = anthropic.AsyncAnthropic(api_key=key, timeout=120.0, max_retries=0), True
    if a.synthetic:
        photos = [Photo(i, h, True, "", g, "", ip.prepare(j), lm) for i, h, g, j, lm in synthetic_set()]
    else:
        photos = load_photos(photos_dir)
    photos = photos[: a.limit] if a.limit else photos
    # user-supplied photos with a handwritten Hindi note are red-team inputs, used as-is
    hand_hi = [p for p in photos if "redteam-hi" in p.notes]
    photos = [p for p in photos if "redteam-hi" not in p.notes]
    locales = a.locales.split(",")
    # Red-team first, so a budget stop never cuts it
    calls = []
    if a.redteam:
        for p in [p for p in photos if p.expected_ok][: a.redteam_limit]:
            for inj in redteam.INJECTIONS:
                font = redteam.FONT_HI if inj[1] == "hi" else None
                prep = ip.prepare(redteam.render(p.prep.jpeg, inj[2], font))
                calls += [Call("redteam", p, m, loc, 0, prep, inj) for loc in ("en", "hi") for m in models]
        for p in hand_hi:
            inj = ("handwritten", "hi", None)
            calls += [Call("redteam", p, m, loc, 0, p.prep, inj) for loc in ("en", "hi") for m in models]
    n_redteam = len(calls)
    # Repeat by repeat, photo by photo, models alternating: a budget stop leaves equal coverage
    calls += [
        Call("bake", p, m, loc, rep, p.prep)
        for rep in range(a.repeats)
        for p in photos
        for loc in locales
        for m in models
    ]
    if a.fake:
        for c in calls:
            client.register(c.prep.jpeg, c.photo.landmarks, c.injection[2] if c.injection else None)
    st = asyncio.run(run(a, client, calls, owned))
    rows = st["rows"]
    out.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d-%H%M%S")
    write_rows(out, ts, [r for r in rows if r["kind"] == "bake"])
    (out / "overlays").mkdir(exist_ok=True)
    for (pid, m), (call, llm) in st["first"].items():
        overlay(call, llm, out / "overlays" / f"{pid}-{m}.png")
    if st["prose"]:
        (out / "redteam").mkdir(exist_ok=True)
        for i, rec in enumerate(sorted(st["prose"], key=lambda r: r["seq"])):
            (out / "redteam" / f"{ts}-{i:04d}.json").write_text(
                json.dumps(rec, ensure_ascii=False, indent=1), encoding="utf-8"
            )
    text = summary(a, rows, photos, st)
    (out / f"{ts}-summary.md").write_text(text, encoding="utf-8")
    print(text)
    if st["skipped"]:
        print(f"budget stop: charged ${st['spent']:.4f} > --max-usd {a.max_usd}")
    # Decided only after every call ran and the summary is written
    rt = [r for r in rows if r["kind"] == "redteam"]
    if len(rt) < n_redteam or any(not r["enums"] or r["hits_after"] for r in rt):
        print("red-team FAILED: an after-filter hit, or a cell without a reading", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
