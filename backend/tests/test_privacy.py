"""READ-05 / D-08: a palm photo never reaches disk or logs, and landmarks are never stored."""

import io
import random
import tempfile

from PIL import Image
from pydantic import ValidationError

from app.schemas.llm import LlmReading
from app.services import image_prep
from conftest import FakeSdk, client_factory, ok_reply, poll, post_job


def _validation_error() -> ValidationError:
    try:
        LlmReading.model_validate_json("{}")
    except ValidationError as e:
        return e
    raise AssertionError("expected a ValidationError")


def _big_jpeg() -> bytes:
    rnd = random.Random(6)
    img = Image.frombytes("RGB", (1500, 1500), rnd.randbytes(1500 * 1500 * 3))
    out = io.BytesIO()
    img.save(out, "JPEG", quality=95)
    return out.getvalue()


BIG_JPEG = _big_jpeg()
assert 1_048_576 < len(BIG_JPEG) < image_prep.MAX_BYTES  # spools to disk under Starlette's default

# (fake factory, settings, expected status, expected error code)
OUTCOMES = [
    (lambda: FakeSdk([ok_reply(w=1500, h=1500)]), {}, "done", None),
    (lambda: FakeSdk([_validation_error()]), {}, "failed", "model_error"),
    (lambda: FakeSdk([1.0]), {"job_deadline_s": 0.2}, "failed", "timeout"),
]


def run_outcomes(after=None) -> None:
    """Success, model_error and timeout on the 1.5 MB photo; `after(client)` runs inside each app."""
    for make_fake, kw, status, code in OUTCOMES:
        with client_factory(make_fake(), **kw) as c:
            r = post_job(c, BIG_JPEG)
            assert r.status_code == 202, r.text
            body = poll(c, r.json()["job_id"])
            assert body["status"] == status
            assert (body["error"] or {}).get("code") == code
            if after:
                after(c)


def test_no_disk_after_success_failure_timeout(monkeypatch, tmp_path):
    def rollover(self):
        raise AssertionError("spooled to disk")

    monkeypatch.setattr(tempfile.SpooledTemporaryFile, "rollover", rollover)
    monkeypatch.setattr(tempfile, "tempdir", str(tmp_path))
    monkeypatch.chdir(tmp_path)
    run_outcomes()
    assert list(tmp_path.iterdir()) == []
