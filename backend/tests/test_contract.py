import copy
import json
import sys
from pathlib import Path

import pytest
from pydantic import ValidationError

from app.schemas.palm import PalmReading

BACKEND = Path(__file__).resolve().parents[1]
CONTRACTS = BACKEND.parent / "contracts"
sys.path.insert(0, str(BACKEND / "scripts"))

from export_schema import export  # noqa: E402


def load(name: str) -> dict:
    return json.loads((CONTRACTS / name).read_text(encoding="utf-8"))


def test_sample_round_trips_unchanged():
    raw = load("palm_reading.sample.json")
    assert PalmReading.model_validate_json(json.dumps(raw)).model_dump(mode="json") == raw


def test_committed_schema_matches_models():
    # parsed objects, not text: a CRLF checkout would break a text compare
    assert load("palm_reading.v1.schema.json") == export()


def test_sample_trace_points_are_normalised_pairs():
    raw = load("palm_reading.sample.json")
    points = [p for line in raw["lines"] for p in line["trace"]]
    assert points
    assert all(len(p) == 2 and all(0 <= c <= 1 for c in p) for p in points)
    assert {line["trace_source"] for line in raw["lines"]} == {"llm", "template", "none"}


# --- invariants, unusable-photo verdict, reason codes, privacy (D-14, FND-05) ---

REASONS = ["blurry_or_dark", "too_far", "jewelry", "bent", "fingers_tight", "incomplete", "no_palm_lines"]


def test_invalid_photo_sample_round_trips_unchanged():
    raw = load("palm_reading.invalid_photo.sample.json")
    assert PalmReading.model_validate_json(json.dumps(raw)).model_dump(mode="json") == raw


@pytest.mark.parametrize("reason", REASONS)
def test_every_reason_code_is_accepted(reason):
    raw = load("palm_reading.invalid_photo.sample.json")
    raw["verdict"]["reason"] = reason
    assert PalmReading.model_validate_json(json.dumps(raw)).verdict.reason == reason


def line(raw, kind):
    return next(x for x in raw["lines"] if x["type"] == kind)


def _ok_false_with_line(r):
    r["verdict"] = {"ok": False, "reason": "jewelry"}
    r["palm"] = r["love"] = r["career"] = None


def _ok_false_no_reason(r):
    r["verdict"]["reason"] = None


def _ok_false_with_love(r):
    r["love"] = load("palm_reading.sample.json")["love"]


def _ok_true_with_reason(r):
    r["verdict"]["reason"] = "too_far"


def _ok_true_null_palm(r):
    r["palm"] = None


VALID, INVALID = "palm_reading.sample.json", "palm_reading.invalid_photo.sample.json"

BROKEN = {
    "ok_false_with_lines": (VALID, _ok_false_with_line),
    "ok_false_reason_null": (INVALID, _ok_false_no_reason),
    "ok_false_love_set": (INVALID, _ok_false_with_love),
    "ok_true_with_reason": (VALID, _ok_true_with_reason),
    "ok_true_palm_null": (VALID, _ok_true_null_palm),
    "coord_above_1": (VALID, lambda r: line(r, "heart")["trace"][0].__setitem__(0, 1.2)),
    "coord_below_0": (VALID, lambda r: line(r, "heart")["trace"][0].__setitem__(1, -0.1)),
    "llm_empty_trace": (VALID, lambda r: line(r, "heart").__setitem__("trace", [])),
    "template_single_point": (VALID, lambda r: line(r, "life").__setitem__("trace", [[0.3, 0.5]])),
    "none_with_points": (VALID, lambda r: line(r, "fate").__setitem__("trace", [[0.5, 0.9], [0.5, 0.4]])),
    "absent_with_trace": (VALID, lambda r: line(r, "life").__setitem__("present", False)),
    "duplicate_heart": (VALID, lambda r: r["lines"].append(copy.deepcopy(line(r, "heart")))),
    "unknown_top_level_key": (VALID, lambda r: r.__setitem__("landmarks", [])),
    "schema_version_2": (VALID, lambda r: r.__setitem__("schema_version", 2)),
    "hand_side_both": (VALID, lambda r: r.__setitem__("hand_side", "both")),
    "ok_true_no_lines": (VALID, lambda r: r.__setitem__("lines", [])),
    "ok_true_missing_head": (VALID, lambda r: r.__setitem__("lines", [x for x in r["lines"] if x["type"] != "head"])),
    "coerced_bool": (VALID, lambda r: line(r, "heart").__setitem__("present", "yes")),
    "coerced_int": (VALID, lambda r: r["image_size"].__setitem__("w", "960")),
    "coerced_float": (VALID, lambda r: line(r, "heart")["trace"][0].__setitem__(0, "0.5")),
    "schema_version_true": (VALID, lambda r: r.__setitem__("schema_version", True)),
}


@pytest.mark.parametrize("case", BROKEN)
def test_invariant_break_is_rejected(case):
    name, mutate = BROKEN[case]
    raw = copy.deepcopy(load(name))
    mutate(raw)
    with pytest.raises(ValidationError):
        PalmReading.model_validate_json(json.dumps(raw))


def property_names(node):
    if isinstance(node, dict):
        for key, value in node.items():
            if key == "properties" and isinstance(value, dict):
                yield from value
            yield from property_names(value)
    elif isinstance(node, list):
        for item in node:
            yield from property_names(item)


def test_privacy_schema_has_no_image_or_landmark_fields():
    names = list(property_names(export()))
    assert "trace" in names and "verdict" in names  # walker reaches $defs
    banned = ("landmark", "hash", "bytes", "base64", "photo", "exif", "gps")
    assert [n for n in names if any(b in n.lower() for b in banned)] == []
