"""Every way a model call can fail ends the job with a D-01 code (READ-02, D-04, D-05)."""

import logging

import anthropic
import pytest
from pydantic import ValidationError

from app import jobs
from app.schemas.llm import LlmReading
from conftest import FakeSdk, client_factory, ok_reply, poll, post_job, reply_with, sdk_error


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


def test_max_tokens_cut_is_retried():
    fake = FakeSdk([reply_with(stop_reason="max_tokens"), ok_reply()])
    assert _run(fake)["status"] == "done"
    assert len(fake.calls) == 2


def test_refusal_is_model_error_without_retry():
    fake = FakeSdk([reply_with(stop_reason="refusal")])
    body = _run(fake)
    assert body["status"] == "failed" and body["error"] == {"code": "model_error"}
    assert len(fake.calls) == 1


@pytest.mark.parametrize(
    "err",
    [
        sdk_error(anthropic.RateLimitError, 429),
        sdk_error(anthropic.InternalServerError, 500),
        sdk_error(anthropic.OverloadedError, 529),
        sdk_error(anthropic.APIConnectionError),
    ],
)
def test_busy_sdk_errors(err):
    fake = FakeSdk([err])
    body = _run(fake)
    assert body["status"] == "failed" and body["error"] == {"code": "busy"}
    assert len(fake.calls) == 1  # the SDK already retried once (max_retries=1)


def test_sdk_timeout_is_timeout():
    fake = FakeSdk([sdk_error(anthropic.APITimeoutError)])
    assert _run(fake)["error"] == {"code": "timeout"}
    assert len(fake.calls) == 1


@pytest.mark.parametrize("status", [400, 401, 404])
def test_status_errors_are_model_error(status):
    fake = FakeSdk([sdk_error(anthropic.APIStatusError, status)])
    assert _run(fake)["error"] == {"code": "model_error"}
    assert len(fake.calls) == 1


def test_overall_deadline_is_timeout():
    body = _run(FakeSdk([1.0]), job_deadline_s=0.2)
    assert body["status"] == "failed" and body["error"] == {"code": "timeout"}


def test_no_retry_without_deadline_left_is_timeout(monkeypatch):
    monkeypatch.setattr(jobs, "RETRY_MIN_LEFT_S", 100.0)
    fake = FakeSdk([_validation_error()])
    assert _run(fake)["error"] == {"code": "timeout"}
    assert len(fake.calls) == 1


def test_unexpected_exception_logs_no_message(caplog):
    caplog.set_level(logging.DEBUG)
    with client_factory(FakeSdk([RuntimeError("leak-0.123457")])) as c:
        job_id = post_job(c).json()["job_id"]
        body = poll(c, job_id)
    assert body["error"] == {"code": "model_error"}
    assert job_id in caplog.text and "RuntimeError" in caplog.text
    assert "leak-0.123457" not in caplog.text
