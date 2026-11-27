"""Assembles the model's LlmReading into the frozen PalmReading v1 contract (D-05)."""

import json
import uuid
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any, Final

from app.schemas.llm import LlmReading
from app.schemas.palm import REQUIRED_LINES, PalmReading
from app.services import line_geometry as geo
from app.services import safety
from app.services.claude_client import PROMPT_VERSION, PalmModel

LINE_ORDER: Final = ("heart", "head", "life", "fate", "sun")
# D-11: True draws every present line from its template; the eval (02-09) decides the default
TEMPLATE_FIRST: Final = False
LABELS: Final = {
    "en": {
        "heart": "Heart line",
        "head": "Head line",
        "life": "Life line",
        "fate": "Fate line",
        "sun": "Sun line",
    },
    "hi": {
        "heart": "हृदय रेखा",
        "head": "मस्तिष्क रेखा",
        "life": "जीवन रेखा",
        "fate": "भाग्य रेखा",
        "sun": "सूर्य रेखा",
    },
}


@dataclass
class ReadResult:
    reading: PalmReading
    llm: LlmReading
    usage: Any
    model: str
    # per line: the first D-10 rule its LLM trace failed, or pass (eval data for D-11)
    line_checks: dict[str, str] = field(default_factory=dict)
    # prose fields the D-13 filter rewrote (eval: the prompt's own compliance)
    safety_hits: int = 0


def _base(*, w: int, h: int, hand: str, locale: str, model: str) -> dict:
    return {
        "schema_version": 1,
        "reading_id": str(uuid.uuid4()),
        "hand_side": hand,
        "meta": {
            "model": model,
            "prompt_version": PROMPT_VERSION,
            "generated_at": datetime.now(UTC).isoformat(),
            "locale": locale,
        },
        "image_size": {"w": w, "h": h},
    }


def _validate(payload: dict) -> PalmReading:
    # Strict mode needs wire JSON for the UUID and datetime string forms (Phase 1 note)
    return PalmReading.model_validate_json(json.dumps(payload, ensure_ascii=False))


def invalid_reading(reason: str, *, w: int, h: int, hand: str, locale: str, model: str) -> PalmReading:
    payload = _base(w=w, h=h, hand=hand, locale=locale, model=model)
    payload |= {
        "verdict": {"ok": False, "reason": reason},
        "lines": [],
        "palm": None,
        "love": None,
        "career": None,
    }
    return _validate(payload)


def _clamp(v: float) -> float:
    return min(1.0, max(0.0, round(v, 4)))


def _norm(pts: list[geo.Pt], w: int, h: int) -> list[list[float]]:
    return [[_clamp(x / w), _clamp(y / h)] for x, y in pts]


def _lines(llm: LlmReading, *, w: int, h: int, locale: str, landmarks: Any):
    palm = geo.palm_from_landmarks(landmarks, w, h)
    checks: dict[str, str] = {}
    pts: dict[str, list[geo.Pt]] = {}
    for key in LINE_ORDER:
        line = getattr(llm.lines, key)
        if line.present and line.points:
            pts[key] = [(float(p.x), float(p.y)) for p in line.points]
            if palm is not None:
                checks[key] = geo.validate(pts[key], palm)
        elif line.present or key in REQUIRED_LINES:
            checks[key] = "omitted"
    if checks.get("heart") == checks.get("head") == "pass" and not geo.pair_ok(
        pts["heart"], pts["head"], palm
    ):
        checks["heart"] = checks["head"] = "pair"
    lines = []
    for key in LINE_ORDER:
        line = getattr(llm.lines, key)
        present = line.present or key in REQUIRED_LINES
        trace: list[geo.Pt] = []
        source = "none"
        if present and palm is not None:
            if checks.get(key) == "pass" and not TEMPLATE_FIRST:
                trace, source = pts[key], "llm"
            else:
                trace, source = geo.template(key, palm), "template"
        lines.append(
            {
                "type": key,
                "label": LABELS[locale][key],
                "present": present,
                "trace": _norm(trace, w, h),
                "trace_source": source,
                "interpretation": line.interpretation,
            }
        )
    return lines, checks


def assemble(
    llm: LlmReading, *, w: int, h: int, hand: str, locale: str, model: str, landmarks: Any
) -> tuple[PalmReading, dict[str, str]]:
    if not llm.verdict.ok:
        # "none" with ok=false is contradictory; the closest contract reason is no_palm_lines
        reason = llm.verdict.reason if llm.verdict.reason != "none" else "no_palm_lines"
        return invalid_reading(reason, w=w, h=h, hand=hand, locale=locale, model=model), {}
    lines, checks = _lines(llm, w=w, h=h, locale=locale, landmarks=landmarks)
    payload = _base(w=w, h=h, hand=hand, locale=locale, model=model)
    payload |= {
        "verdict": {"ok": True, "reason": None},
        "lines": lines,
        "palm": {"summary": llm.summary, "traits": llm.traits},
        "love": {
            "how_you_love": llm.how_you_love,
            "ideal_partner": llm.ideal_partner,
            "challenges": llm.love_challenges,
        },
        "career": {
            "strengths": llm.strengths,
            "challenges": llm.career_challenges,
            "ideal_industries": llm.ideal_industries,
        },
    }
    return _validate(payload), checks


class PalmReader:
    def __init__(self, model: PalmModel):
        self.model = model

    async def read(
        self, jpeg: bytes, w: int, h: int, hand: str, locale: str, landmarks: Any
    ) -> ReadResult:
        reply = await self.model.read_once(jpeg, w, h, hand, locale)
        # llm stays the unfiltered model output (eval enum agreement); only the reading is served
        safe, safety_hits = safety.filter_llm(reply.parsed, locale)
        reading, checks = assemble(
            safe, w=w, h=h, hand=hand, locale=locale, model=reply.model, landmarks=landmarks
        )
        return ReadResult(reading, reply.parsed, reply.usage, reply.model, checks, safety_hits)
