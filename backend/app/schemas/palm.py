"""PalmReading v1: the public reading contract shared with the Android client (D-14).

These models are the authoring source. `scripts/export_schema.py` writes
`contracts/palm_reading.v1.schema.json` from them. A field-shape change ships as a
new schema_version, never as a silent edit of v1.

Validation is strict (no "yes" -> True, no "960" -> 960). Parse wire JSON with
`PalmReading.model_validate_json(...)`: strict Python-mode validation would reject the
JSON string forms of UUID and datetime.
"""

from typing import Annotated, Final, Literal, Self
from uuid import UUID

from pydantic import AwareDatetime, BaseModel, ConfigDict, Field, field_validator, model_validator

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
REQUIRED_LINES: Final = frozenset({"heart", "head", "life"})
Coord = Annotated[float, Field(ge=0, le=1)]
Point = tuple[Coord, Coord]


class ContractModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)


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

    @model_validator(mode="after")
    def _trace_rules(self) -> Self:
        if (self.trace_source == "none") != (not self.trace):
            raise ValueError("trace_source none must hold exactly when trace is empty")
        if not self.present and self.trace:
            raise ValueError("present false requires an empty trace")
        if self.trace_source in ("llm", "template") and len(self.trace) < 2:
            raise ValueError("llm and template traces need at least 2 points")
        return self


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
    """Palm reading v1. Invariants: verdict.ok true implies reason null, palm, love,
    career present and heart, head and life lines listed; verdict.ok false implies reason set, lines empty and palm, love,
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

    @field_validator("schema_version", mode="before")
    @classmethod
    def _no_bool_version(cls, v: object) -> object:
        # Literal[1] would accept true even in strict mode (True == 1)
        if isinstance(v, bool):
            raise ValueError("schema_version must be the integer 1")
        return v

    @model_validator(mode="after")
    def _verdict_rules(self) -> Self:
        sections = (self.palm, self.love, self.career)
        if self.verdict.ok:
            if self.verdict.reason is not None:
                raise ValueError("verdict.ok true requires reason null")
            if any(x is None for x in sections):
                raise ValueError("verdict.ok true requires palm, love and career")
            missing = REQUIRED_LINES - {line.type for line in self.lines}
            if missing:
                raise ValueError(f"verdict.ok true requires lines {sorted(missing)}")
        else:
            if self.verdict.reason is None:
                raise ValueError("verdict.ok false requires a reason")
            if self.lines:
                raise ValueError("verdict.ok false requires empty lines")
            if any(x is not None for x in sections):
                raise ValueError("verdict.ok false requires palm, love and career null")
        types = [line.type for line in self.lines]
        if len(types) != len(set(types)):
            raise ValueError("a line type appears more than once")
        return self
