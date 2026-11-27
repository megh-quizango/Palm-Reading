"""Reader-level D-10 tests: model replies pass through PalmReader with Claude faked at the SDK."""

import asyncio

from conftest import LANDMARKS, FakeSdk, make_jpeg, ok_reply

from app.schemas.llm import LlmReading
from app.schemas.palm import PalmReading
from app.services import line_geometry as geo
from app.services import palm_reader
from app.services.claude_client import PalmModel
from app.services.palm_reader import PalmReader

W, H = 960, 1280


def reply_with(**lines):
    """ok_reply() with per-line overrides, e.g. heart={"points": [...]}."""
    r = ok_reply()
    data = r.parsed_output.model_dump()
    for key, patch in lines.items():
        data["lines"][key] |= patch
    r.parsed_output = LlmReading.model_validate(data)
    return r


def read(reply, landmarks=LANDMARKS):
    reader = PalmReader(PalmModel(FakeSdk([reply]), "m"))
    res = asyncio.run(reader.read(make_jpeg(), W, H, "right", "en", landmarks))
    PalmReading.model_validate_json(res.reading.model_dump_json())
    return res, {line.type: line for line in res.reading.lines}


def test_off_palm_heart_becomes_template(monkeypatch):
    monkeypatch.setattr(palm_reader, "TEMPLATE_FIRST", False)
    top_edge = [{"x": x, "y": 5} for x in (100, 260, 420, 580, 740, 900)]
    res, lines = read(reply_with(heart={"points": top_edge}))
    heart = lines["heart"]
    assert heart.trace_source == "template"
    assert len(heart.trace) >= 2
    palm = geo.palm_from_landmarks(LANDMARKS, W, H)
    # 4-decimal normalisation moves a point by at most 0.05 px; the hull has far more margin
    assert all(geo.inside(palm.hull, (x * W, y * H)) for x, y in heart.trace)
    assert all(lines[k].trace_source == "llm" for k in ("head", "life", "fate", "sun"))
    assert res.line_checks["heart"] == "hull"


def test_absent_heart_gets_template():
    res, lines = read(reply_with(heart={"present": False, "points": []}))
    assert lines["heart"].present is True
    assert lines["heart"].trace_source == "template"
    assert res.line_checks["heart"] == "omitted"


def test_absent_fate_stays_absent():
    res, lines = read(reply_with(fate={"present": False, "points": []}))
    fate = lines["fate"]
    assert (fate.present, fate.trace, fate.trace_source) == (False, [], "none")
    assert "fate" not in res.line_checks


def test_swapped_heart_and_head_both_templates(monkeypatch):
    monkeypatch.setattr(palm_reader, "TEMPLATE_FIRST", False)
    base = ok_reply().parsed_output.lines
    heart = [p.model_dump() for p in base.head.points]
    head = [p.model_dump() for p in base.heart.points]
    res, lines = read(reply_with(heart={"points": heart}, head={"points": head}))
    assert lines["heart"].trace_source == lines["head"].trace_source == "template"
    assert res.line_checks["heart"] == res.line_checks["head"] == "pair"
    assert lines["life"].trace_source == "llm"


def test_degenerate_landmarks_keep_required_lines():
    res, lines = read(ok_reply(), landmarks=[[0.5, 0.5, 0.0]] * 21)
    for key in ("heart", "head", "life"):
        assert (lines[key].present, lines[key].trace, lines[key].trace_source) == (True, [], "none")
    assert [line.type for line in res.reading.lines] == list(palm_reader.LINE_ORDER)


def test_template_first_switch(monkeypatch):
    monkeypatch.setattr(palm_reader, "TEMPLATE_FIRST", True)
    res, lines = read(ok_reply())
    assert all(line.trace_source == "template" for line in lines.values())
    assert set(res.line_checks.values()) == {"pass"}
