import json
import sys
from pathlib import Path

from app.schemas.palm import PalmReading

BACKEND = Path(__file__).resolve().parents[1]
CONTRACTS = BACKEND.parent / "contracts"
sys.path.insert(0, str(BACKEND / "scripts"))

from export_schema import export  # noqa: E402


def load(name: str) -> dict:
    return json.loads((CONTRACTS / name).read_text(encoding="utf-8"))


def test_sample_round_trips_unchanged():
    raw = load("palm_reading.sample.json")
    assert PalmReading.model_validate(raw).model_dump(mode="json") == raw


def test_committed_schema_matches_models():
    # parsed objects, not text: a CRLF checkout would break a text compare
    assert load("palm_reading.v1.schema.json") == export()


def test_sample_trace_points_are_normalised_pairs():
    raw = load("palm_reading.sample.json")
    points = [p for line in raw["lines"] for p in line["trace"]]
    assert points
    assert all(len(p) == 2 and all(0 <= c <= 1 for c in p) for p in points)
    assert {line["trace_source"] for line in raw["lines"]} == {"llm", "template", "none"}
