"""Every way a model call can fail ends the job with a D-01 code (READ-02, D-04, D-05)."""

from pydantic import ValidationError

from app.schemas.llm import LlmReading
from conftest import FakeSdk, client_factory, ok_reply, poll, post_job


def _validation_error() -> ValidationError:
    try:
        LlmReading.model_validate_json("{}")
    except ValidationError as e:
        return e
    raise AssertionError("expected a ValidationError")


def _run(fake, **settings_kw) -> dict:
    with client_factory(fake, **settings_kw) as c:
        r = post_job(c)
        assert r.status_code == 202
        return poll(c, r.json()["job_id"])


def test_malformed_once_then_ok():
    fake = FakeSdk([_validation_error(), ok_reply()])
    body = _run(fake)
    assert body["status"] == "done" and body["reading"] is not None
    assert len(fake.calls) == 2
    assert fake.calls[0] == fake.calls[1]  # identical retry, no error text appended


def test_malformed_twice_is_model_error():
    fake = FakeSdk([_validation_error()])
    body = _run(fake)
    assert body["status"] == "failed"
    assert body["error"] == {"code": "model_error"} and body["reading"] is None
    assert len(fake.calls) == 2
