"""Per-install rate windows, daily spend cap and per-attempt cost (READ-10, D-14)."""

import datetime as dt
import io
import time
import uuid
from types import SimpleNamespace

import anthropic
import pytest
from PIL import Image
from pydantic import ValidationError

from app import jobs
from app.schemas.llm import LlmReading
from app.services.claude_client import PRICES, cost, worst_case_cost
from conftest import FakeSdk, client_factory, jpeg_variant, ok_reply, poll, post_job, reply_with, sdk_error

SONNET = "claude-sonnet-5-5"


def _usage(i=0, o=0, cw=0, cr=0):
    return SimpleNamespace(
        input_tokens=i, output_tokens=o, cache_creation_input_tokens=cw, cache_read_input_tokens=cr
    )


def _hdrs(install: str, key: str | None = None) -> dict:
    return {"X-Install-Id": install, "Idempotency-Key": key or str(uuid.uuid4())}


def _spent(c) -> float:
    return sum(c.app.state.jobs.ledger.values())


def _validation_error() -> ValidationError:
    try:
        LlmReading.model_validate_json("{}")
    except ValidationError as e:
        return e
    raise AssertionError("expected a ValidationError")


@pytest.fixture(autouse=True)
def exact_hits_only(monkeypatch):
    # one-pixel variants stay model-bound instead of hitting the dHash cache
    monkeypatch.setattr(jobs, "DHASH_MAX_DISTANCE", -1)


def test_cost_from_usage():
    assert cost(SONNET, _usage(1000, 500)) == pytest.approx(0.007)
    assert cost(SONNET, _usage(1000, 500, cw=1000)) == pytest.approx(0.007 + 1000 * 2.00 * 1.25 / 1e6)
    assert cost(SONNET, _usage(1000, 500, cr=1000)) == pytest.approx(0.007 + 1000 * 0.10 / 1e6)
    assert PRICES["claude-haiku-5-5"] == (0.10, 0.50, 0.01)
    assert cost("claude-unknown", _usage(1000, 500)) == cost("claude-opus-5-5", _usage(1000, 500))
    none = SimpleNamespace(
        input_tokens=1000, output_tokens=None, cache_creation_input_tokens=None, cache_read_input_tokens=None
    )
    assert cost(SONNET, none) == pytest.approx(0.002)


def test_sixth_job_in_ten_minutes_is_busy(monkeypatch):
    fake = FakeSdk()
    a, b = str(uuid.uuid4()), str(uuid.uuid4())
    with client_factory(fake) as c:
        for i in range(5):
            r = post_job(c, jpeg_variant(i), headers=_hdrs(a))
            assert r.status_code == 202
            poll(c, r.json()["job_id"])
        n_jobs = len(c.app.state.jobs.jobs)
        r = post_job(c, jpeg_variant(5), headers=_hdrs(a))
        assert r.status_code == 429 and r.json() == {"error": {"code": "busy"}}
        assert int(r.headers["Retry-After"]) > 0
        assert len(c.app.state.jobs.jobs) == n_jobs  # no job for the refused request
        assert len(fake.calls) == 5
        other = post_job(c, jpeg_variant(6), headers=_hdrs(b))
        assert other.status_code == 202
        poll(c, other.json()["job_id"])
        t0 = time.monotonic()
        monkeypatch.setattr(jobs, "now", lambda: t0 + 601)
        assert post_job(c, jpeg_variant(5), headers=_hdrs(a)).status_code == 202


def test_twenty_first_job_in_a_day_is_busy():
    install = str(uuid.uuid4())
    with client_factory(rate_limit_10min=100) as c:
        for i in range(20):
            r = post_job(c, jpeg_variant(i), headers=_hdrs(install))
            assert r.status_code == 202
            poll(c, r.json()["job_id"])
        r = post_job(c, jpeg_variant(20), headers=_hdrs(install))
    assert r.status_code == 429 and r.json() == {"error": {"code": "busy"}}


def test_free_paths_do_not_count():
    fake = FakeSdk()
    install = str(uuid.uuid4())
    key = str(uuid.uuid4())
    with client_factory(fake) as c:
        for i in range(4):
            poll(c, post_job(c, jpeg_variant(i), headers=_hdrs(install, key if i == 0 else None)).json()["job_id"])
        assert post_job(c, jpeg_variant(0), headers=_hdrs(install)).status_code == 202  # cache hit
        assert post_job(c, jpeg_variant(0), headers=_hdrs(install, key)).status_code == 202  # idempotent
        assert post_job(c, _black_jpeg(), headers=_hdrs(install)).status_code == 202  # pre-gate
        fifth = post_job(c, jpeg_variant(4), headers=_hdrs(install))
        assert fifth.status_code == 202
        poll(c, fifth.json()["job_id"])
        assert post_job(c, jpeg_variant(5), headers=_hdrs(install)).status_code == 429
    assert len(fake.calls) == 5


def _black_jpeg() -> bytes:
    out = io.BytesIO()
    Image.new("RGB", (960, 1280)).save(out, "JPEG", quality=90)
    return out.getvalue()


def test_spend_cap_blocks_until_next_utc_day(monkeypatch):
    with client_factory(daily_spend_cap_usd=0.01) as c:
        poll(c, post_job(c, jpeg_variant(0)).json()["job_id"])
        assert _spent(c) == pytest.approx(0.028)
        r = post_job(c, jpeg_variant(1))
        assert r.status_code == 429 and r.json() == {"error": {"code": "busy"}}
        assert int(r.headers["Retry-After"]) > 0
        tomorrow = jobs.today() + dt.timedelta(days=1)
        monkeypatch.setattr(jobs, "today", lambda: tomorrow)
        assert post_job(c, jpeg_variant(1)).status_code == 202


def _spend_of(fake, **kw) -> float:
    with client_factory(fake, **kw) as c:
        poll(c, post_job(c).json()["job_id"])
        return _spent(c)


def test_max_tokens_twice_charges_both_attempts():
    spent = _spend_of(FakeSdk([reply_with(stop_reason="max_tokens")]))
    assert spent == pytest.approx(2 * cost(SONNET, ok_reply().usage))


def test_attempts_without_usage_charge_worst_case():
    assert _spend_of(FakeSdk([_validation_error()])) == 2 * worst_case_cost(SONNET)
    assert _spend_of(FakeSdk([1.0]), job_deadline_s=0.2) == worst_case_cost(SONNET)


def test_refusal_charges_usage_and_status_errors_nothing():
    assert _spend_of(FakeSdk([reply_with(stop_reason="refusal")])) == pytest.approx(
        cost(SONNET, ok_reply().usage)
    )
    assert _spend_of(FakeSdk([sdk_error(anthropic.RateLimitError, 429)])) == 0
