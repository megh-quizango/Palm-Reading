"""PalmReading v1: the public reading contract shared with the Android client (D-14).

These models are the authoring source. `scripts/export_schema.py` writes
`contracts/palm_reading.v1.schema.json` from them. A field-shape change ships as a
new schema_version, never as a silent edit of v1.
"""

from typing import Annotated, Final, Literal
from uuid import UUID

from pydantic import AwareDatetime, BaseModel, ConfigDict, Field

SCHEMA_VERSION: Final = 1

HandSide = Literal["left", "right"]
LineType = Literal["heart", "head", "life", "fate", "sun"]
TraceSource = Literal["llm", "template", "none"]
InvalidReason = Literal[
    "blurry_or_dark",
    "too_far",
    "jewelry",
    "bent",
    "fingers_tight",
    "incomplete",
    "no_palm_lines",
]
Coord = Annotated[float, Field(ge=0, le=1)]
Point = tuple[Coord, Coord]


class ContractModel(BaseModel):
    model_config = ConfigDict(extra="forbid")


class Meta(ContractModel):
    model: str
    prompt_version: str
    generated_at: AwareDatetime
    locale: str = Field(description="BCP-47 language tag, e.g. en, hi")


class ImageSize(ContractModel):
    """Size of the image sent to the model; trace coordinates are relative to it."""

    w: int = Field(gt=0)
    h: int = Field(gt=0)


class Verdict(ContractModel):
    ok: bool
    reason: InvalidReason | None


class PalmLine(ContractModel):
    """trace_source none exactly when trace is empty; present false implies an empty
    trace; llm and template traces have at least 2 points. Points are [x, y] in 0..1."""

    type: LineType
    label: str = Field(min_length=1)
    present: bool
    trace: list[Point]
    trace_source: TraceSource
    interpretation: str


class PalmSummary(ContractModel):
    summary: str
    traits: list[str]


class Love(ContractModel):
    how_you_love: str
    ideal_partner: str
    challenges: str


class Career(ContractModel):
    strengths: str
    challenges: str
    ideal_industries: list[str]


class PalmReading(ContractModel):
    """Palm reading v1. Invariants: verdict.ok true implies reason null and palm, love,
    career present; verdict.ok false implies reason set, lines empty and palm, love,
    career null; no line type appears twice. Carries no hand landmarks or image data."""

    schema_version: Literal[1]
    reading_id: UUID
    hand_side: HandSide
    meta: Meta
    image_size: ImageSize
    verdict: Verdict
    lines: list[PalmLine]
    palm: PalmSummary | None
    love: Love | None
    career: Career | None
