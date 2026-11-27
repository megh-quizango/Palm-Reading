"""PALM_MODE=template: canned readings end to end on synthetic photos, no Anthropic client."""

import json

import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.schemas.palm import PalmReading
from app.services import safety
from app.services.template_model import MODEL_ID
from tests.conftest import jpeg_variant, poll, post_job, settings


def template_client() -> TestClient:
    # no anthropic_client and no key: the autouse guard fails the test if a real client is built
    return TestClient(create_app(settings(palm_mode="template")))


@pytest.mark.parametrize("locale", ["en", "hi"])
def test_template_reading_end_to_end(locale):
    with template_client() as c:
        r = post_job(c, locale=locale)
        assert r.status_code == 202
        body = poll(c, r.json()["job_id"])
    assert body["status"] == "done", body
    reading = PalmReading.model_validate_json(json.dumps(body["reading"]))
    assert reading.verdict.ok and reading.meta.model == MODEL_ID and reading.meta.locale == locale
    lines = {line.type: line for line in reading.lines}
    assert {"heart", "head", "life"} <= lines.keys()
    for line in lines.values():
        assert line.trace_source == "template" and len(line.trace) >= 4
    texts = [reading.palm.summary, *[line.interpretation for line in reading.lines]]
    assert not any(safety.hits(t) for t in texts)


def test_same_photo_same_reading_and_variants_differ():
    with template_client() as c:
        jpeg = jpeg_variant(1)
        a = poll(c, post_job(c, jpeg).json()["job_id"])["reading"]
        b = poll(c, post_job(c, jpeg).json()["job_id"])["reading"]
        summaries = {
            poll(c, post_job(c, jpeg_variant(i)).json()["job_id"])["reading"]["palm"]["summary"]
            for i in range(2, 8)
        }
    assert a["palm"]["summary"] == b["palm"]["summary"]
    assert len(summaries) > 1


def test_default_mode_is_template():
    from app.config import Settings

    assert Settings(_env_file=None).palm_mode == "template"
