import json

from fastapi.testclient import TestClient

from app.main import create_app
from app.schemas.palm import PalmReading
from conftest import FakeSdk, client_factory, make_jpeg, poll, post_job, settings


def test_happy_path_reading_validates_against_contract():
    fake = FakeSdk()
    with client_factory(fake) as c:
        r = post_job(c, make_jpeg())
        assert r.status_code == 202
        job_id = r.json()["job_id"]
        assert isinstance(job_id, str)
        body = poll(c, job_id)
    assert body["status"] == "done"
    assert body["error"] is None
    reading = PalmReading.model_validate_json(json.dumps(body["reading"]))
    assert [line.type for line in reading.lines] == ["heart", "head", "life", "fate", "sun"]
    assert body["reading"]["image_size"] == {"w": 960, "h": 1280}
    assert reading.meta.prompt_version == "p1"
    assert reading.meta.model == "claude-sonnet-5-5"
    for line in reading.lines:
        assert line.trace_source == "llm"
        assert all(0 <= v <= 1 for pt in line.trace for v in pt)
    assert len(fake.calls) == 1


def test_unknown_job_is_404():
    with client_factory() as c:
        r = c.get("/v1/palm/jobs/does-not-exist")
    assert r.status_code == 404
    assert r.json() == {"error": {"code": "not_found"}}


def test_no_key_no_client_returns_busy():
    with TestClient(create_app(settings())) as c:
        r = post_job(c)
        assert r.status_code == 503
        assert r.json() == {"error": {"code": "busy"}}
        assert c.get("/healthz").json()["anthropic_key"] == "missing"
