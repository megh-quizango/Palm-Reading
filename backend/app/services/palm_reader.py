"""Assembles the model's LlmReading into the frozen PalmReading v1 contract (D-05)."""

import json
import uuid
from dataclasses import dataclass
from datetime import UTC, datetime
from typing import Any, Final

from app.schemas.llm import LlmReading
from app.schemas.palm import PalmReading
from app.services.claude_client import PROMPT_VERSION, PalmModel

LINE_ORDER: Final = ("heart", "head", "life", "fate", "sun")
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


def assemble(
    llm: LlmReading, *, w: int, h: int, hand: str, locale: str, model: str, landmarks: Any
) -> PalmReading:
    if not llm.verdict.ok:
        # "none" with ok=false is contradictory; the closest contract reason is no_palm_lines
        reason = llm.verdict.reason if llm.verdict.reason != "none" else "no_palm_lines"
        return invalid_reading(reason, w=w, h=h, hand=hand, locale=locale, model=model)
    lines = []
    for key in LINE_ORDER:
        line = getattr(llm.lines, key)
        trace = [[_clamp(p.x / w), _clamp(p.y / h)] for p in line.points] if line.present else []
        if len(trace) < 2:
            trace = []
        lines.append(
            {
                "type": key,
                "label": LABELS[locale][key],
                "present": line.present,
                "trace": trace,
                "trace_source": "llm" if trace else "none",
                "interpretation": line.interpretation,
            }
        )
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
    return _validate(payload)


class PalmReader:
    def __init__(self, model: PalmModel):
        self.model = model

    async def read(
        self, jpeg: bytes, w: int, h: int, hand: str, locale: str, landmarks: Any
    ) -> ReadResult:
        reply = await self.model.read_once(jpeg, w, h, hand, locale)
        reading = assemble(
            reply.parsed, w=w, h=h, hand=hand, locale=locale, model=reply.model, landmarks=landmarks
        )
        return ReadResult(reading, reply.parsed, reply.usage, reply.model)
