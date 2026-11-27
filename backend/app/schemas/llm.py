"""LLM-side structured-output schema (D-05). Not the public contract: palm.py is.

Every field is required, with no Optional, unions or numeric/length constraints, because
the structured-outputs API rejects or strips them. Property order is the order the model
writes in: verdict first, then line features, then prose.
"""

from typing import Annotated, Literal

from pydantic import BaseModel, BeforeValidator


def _lower(v: object) -> object:
    # Enum casing is not guaranteed by the API; casing drift is not a malformed reading
    return v.lower() if isinstance(v, str) else v


Lc = BeforeValidator(_lower)

LlmReason = Literal[
    "none",  # "none" instead of null: no union
    "blurry_or_dark",
    "too_far",
    "jewelry",
    "bent",
    "fingers_tight",
    "incomplete",
    "no_palm_lines",
]


class Px(BaseModel):
    x: int  # pixel column in the sent image
    y: int  # pixel row


class LlmLine(BaseModel):
    present: bool
    length: Annotated[Literal["short", "medium", "long"], Lc]
    curvature: Annotated[Literal["straight", "gentle", "strong"], Lc]
    depth: Annotated[Literal["faint", "clear", "deep"], Lc]
    continuity: Annotated[Literal["continuous", "broken", "chained", "forked"], Lc]
    points: list[Px]
    interpretation: str


class LlmLines(BaseModel):
    heart: LlmLine
    head: LlmLine
    life: LlmLine
    fate: LlmLine
    sun: LlmLine


class LlmVerdict(BaseModel):
    ok: bool
    reason: Annotated[LlmReason, Lc]


class LlmReading(BaseModel):
    verdict: LlmVerdict
    lines: LlmLines
    summary: str
    traits: list[str]
    how_you_love: str
    ideal_partner: str
    love_challenges: str
    strengths: str
    career_challenges: str
    ideal_industries: list[str]
