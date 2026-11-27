import json
import math
import os
import time
import uuid

import pytest
from fastapi.testclient import TestClient

from app import jobs
from app.main import create_app
from app.schemas.palm import PalmReading
from app.services.image_prep import MAX_BYTES
from conftest import LANDMARKS, FakeSdk, client_factory, make_jpeg, poll, post_job, settings


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


# --- input validation ----------------------------------------------------------

BAD = {"error": {"code": "bad_request"}}


def headers():
    return {"X-Install-Id": str(uuid.uuid4()), "Idempotency-Key": str(uuid.uuid4())}


def assert_rejected(c, fake, r, status=422):
    assert r.status_code == status
    assert r.json() == BAD
    assert c.app.state.jobs.jobs == {}
    assert fake.calls == []


def form_post(c, *, files=None, data=None, hdrs=None):
    files = files if files is not None else {"image": ("palm.jpg", make_jpeg(), "image/jpeg")}
    data = data if data is not None else {
        "hand_side": "right",
        "locale": "en",
        "landmarks": json.dumps(LANDMARKS),
    }
    return c.post("/v1/palm/jobs", files=files, data=data, headers=hdrs or headers())


@pytest.mark.parametrize("hdrs", [{}, {"X-Install-Id": "abc"}], ids=["missing", "not-uuid"])
def test_install_id_required(hdrs):
    fake = FakeSdk()
    with client_factory(fake) as c:
        assert_rejected(c, fake, post_job(c, headers=hdrs))


def _pts(n=21, dims=3, x=0.5):
    return [[x] + [0.5] * (dims - 1) for _ in range(n)]


@pytest.mark.parametrize(
    "field,value",
    [
        ("hand_side", "middle"),
        ("locale", "fr"),
        ("landmarks", "not json"),
        ("landmarks", json.dumps(_pts(n=20))),
        ("landmarks", json.dumps(_pts(dims=4))),
        ("landmarks", json.dumps(_pts(x=math.nan))),  # json.dumps writes the literal NaN
        ("landmarks", json.dumps(_pts(x=math.inf))),  # literal Infinity
        ("landmarks", json.dumps(_pts(x=-math.inf))),
        ("landmarks", json.dumps(_pts()).replace("0.5", "1e400", 1)),  # overflows to inf
        ("landmarks", json.dumps(_pts(x=2.0))),
        ("landmarks", json.dumps(_pts()).replace("0.5", "true", 1)),
        ("landmarks", json.dumps({"points": _pts()})),
    ],
)
def test_bad_fields_rejected(field, value):
    fake = FakeSdk()
    data = {"hand_side": "right", "locale": "en", "landmarks": json.dumps(LANDMARKS), field: value}
    with client_factory(fake) as c:
        r = form_post(c, data=data)
        assert_rejected(c, fake, r)
    if field == "landmarks":
        assert value not in r.text


@pytest.mark.parametrize(
    "raw", [os.urandom(4096), make_jpeg(size=(200, 300))], ids=["non-image", "too-small"]
)
def test_bad_image_rejected(raw):
    fake = FakeSdk()
    with client_factory(fake) as c:
        assert_rejected(c, fake, post_job(c, raw))


def test_oversized_body_refused_before_form_is_read():
    fake = FakeSdk()
    body = b"x" * (MAX_BYTES + 64 * 1024 + 1)
    with client_factory(fake) as c:
        r = c.post(
            "/v1/palm/jobs",
            content=body,
            headers={**headers(), "Content-Type": "multipart/form-data; boundary=x"},
        )
        assert_rejected(c, fake, r, status=413)


def test_missing_content_length_refused():
    fake = FakeSdk()
    with client_factory(fake) as c:
        r = c.post(
            "/v1/palm/jobs",
            content=iter([b"--x--\r\n"]),  # a generator body is sent chunked, without Content-Length
            headers={**headers(), "Content-Type": "multipart/form-data; boundary=x"},
        )
        assert r.status_code in (411, 413)
        assert r.json() == BAD
        assert c.app.state.jobs.jobs == {}


def test_image_part_over_cap_rejected():
    fake = FakeSdk()
    with client_factory(fake) as c:
        r = form_post(c, files={"image": ("palm.jpg", b"\xff" * (MAX_BYTES + 1), "image/jpeg")})
        assert_rejected(c, fake, r)


def _good_data(**extra):
    return {"hand_side": "right", "locale": "en", "landmarks": json.dumps(LANDMARKS), **extra}


@pytest.mark.parametrize(
    "case",
    ["extra-field", "two-files", "oversized-field", "no-boundary", "no-image", "image-as-text"],
)
def test_malformed_multipart_rejected(case):
    fake = FakeSdk()
    jpeg = make_jpeg()
    with client_factory(fake) as c:
        if case == "extra-field":
            r = form_post(c, data=_good_data(extra="1"))
        elif case == "two-files":
            r = form_post(
                c,
                files=[
                    ("image", ("a.jpg", jpeg, "image/jpeg")),
                    ("image", ("b.jpg", jpeg, "image/jpeg")),
                ],
            )
        elif case == "oversized-field":
            r = form_post(c, data=_good_data(landmarks="[" + " " * (64 * 1024 + 1) + "]"))
        elif case == "no-boundary":
            r = c.post(
                "/v1/palm/jobs",
                content=b"--x\r\n\r\n--x--\r\n",
                headers={**headers(), "Content-Type": "multipart/form-data"},
            )
        elif case == "no-image":
            r = form_post(c, files={"other": ("a.txt", b"hi", "text/plain")})
        else:
            r = c.post(
                "/v1/palm/jobs", data=_good_data(image="not a file"), headers=headers()
            )
        assert_rejected(c, fake, r)
        assert "detail" not in r.text


def _hdrs(install=None, key=None) -> dict:
    h = {"X-Install-Id": install or str(uuid.uuid4())}
    if key is not None:
        h["Idempotency-Key"] = key
    return h


def test_idempotent_retry_returns_same_job():
    fake = FakeSdk()
    hdrs = _hdrs(key=str(uuid.uuid4()))
    with client_factory(fake) as c:
        first = post_job(c, headers=hdrs).json()["job_id"]
        poll(c, first)
        again = post_job(c, headers=hdrs)
    assert again.status_code == 202 and again.json()["job_id"] == first
    assert len(fake.calls) == 1


def test_idempotency_key_must_be_uuid():
    with client_factory() as c:
        bad = post_job(c, headers=_hdrs(key="abc"))
        none = post_job(c, headers=_hdrs())
    assert bad.status_code == 422 and bad.json() == {"error": {"code": "bad_request"}}
    assert none.status_code == 202


def test_single_flight_same_install_shares_running_job():
    fake = FakeSdk([0.3])
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        a = post_job(c, headers=_hdrs(install, str(uuid.uuid4()))).json()["job_id"]
        b = post_job(c, headers=_hdrs(install, str(uuid.uuid4()))).json()["job_id"]
        assert poll(c, a)["status"] == "done"
    assert a == b and len(fake.calls) == 1


def test_single_flight_never_crosses_installs():
    with client_factory(FakeSdk([0.3])) as c:
        a = post_job(c, headers=_hdrs(key=str(uuid.uuid4()))).json()["job_id"]
        b = post_job(c, headers=_hdrs(key=str(uuid.uuid4()))).json()["job_id"]
    assert a != b


def test_single_flight_keyed_by_hand():
    install = str(uuid.uuid4())
    with client_factory(FakeSdk([0.3])) as c:
        a = post_job(c, headers=_hdrs(install, str(uuid.uuid4()))).json()["job_id"]
        b = post_job(c, hand="left", headers=_hdrs(install, str(uuid.uuid4()))).json()["job_id"]
    assert a != b


def test_expired_job_is_404_and_key_is_released(monkeypatch):
    fake = FakeSdk()
    hdrs = _hdrs(key=str(uuid.uuid4()))
    with client_factory(fake) as c:
        old = post_job(c, headers=hdrs).json()["job_id"]
        poll(c, old)
        t0 = time.monotonic()
        monkeypatch.setattr(jobs, "now", lambda: t0 + jobs.JOB_TTL_S + 1)
        gone = c.get(f"/v1/palm/jobs/{old}")
        new = post_job(c, headers=hdrs).json()["job_id"]
        poll(c, new)
    assert gone.status_code == 404 and gone.json() == {"error": {"code": "not_found"}}
    assert new != old and len(fake.calls) == 2
