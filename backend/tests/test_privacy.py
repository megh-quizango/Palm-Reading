"""READ-05 / D-08: a palm photo never reaches disk or logs, and landmarks are never stored."""

import base64
import io
import logging
import random
import tempfile
import time
from collections import deque

from PIL import Image
from pydantic import ValidationError

from app import jobs
from app.schemas.llm import LlmReading
from app.services import image_prep
from conftest import LANDMARKS, FakeSdk, client_factory, make_jpeg, ok_reply, poll, post_job


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


def _all_outcomes(after=None) -> None:
    """The three outcomes, plus a 422 rejection and a done job for an EXIF/GPS photo."""
    run_outcomes(after)
    with client_factory(FakeSdk([ok_reply()])) as c:
        bad = [list(p) for p in LANDMARKS]
        bad[2][0] = 2.0
        assert post_job(c, landmarks=bad).status_code == 422
        r = post_job(c, make_jpeg(exif=True))
        assert r.status_code == 202, r.text
        assert poll(c, r.json()["job_id"])["status"] == "done"
        if after:
            after(c)


def test_sdk_and_pil_loggers_never_debug(caplog):
    caplog.set_level(logging.DEBUG)
    assert not logging.getLogger("anthropic._base_client").isEnabledFor(logging.DEBUG)
    assert not logging.getLogger("PIL.TiffImagePlugin").isEnabledFor(logging.DEBUG)


def test_no_image_or_landmarks_in_logs(caplog):
    caplog.set_level(logging.DEBUG)
    _all_outcomes()
    assert not [r.name for r in caplog.records if r.name.startswith(("anthropic", "PIL"))]
    text = caplog.text
    prefixes = [
        base64.b64encode(image_prep.prepare(j).jpeg).decode()[:40]
        for j in (BIG_JPEG, make_jpeg(exif=True))
    ]
    for needle in ["/9j/", "0.123457", "\\xff\\xd8", "\xff\xd8", "JFIF", *prefixes]:
        assert needle not in text, needle


def _is_landmarks(x) -> bool:
    return len(x) == 21 and all(
        isinstance(p, (list, tuple)) and len(p) == 3 and all(isinstance(v, (int, float)) for v in p)
        for p in x
    )


def _leaks(root) -> list[str]:
    """Walk app-held state; only objects whose type lives in the `app` package are descended into,
    so the injected FakeSdk (which keeps request kwargs on purpose) and asyncio primitives are leaves."""
    seen, found, stack = set(), [], [root]
    while stack:
        x = stack.pop()
        if id(x) in seen:
            continue
        seen.add(id(x))
        if isinstance(x, (bytes, bytearray, memoryview)):
            found.append(f"bytes len={len(x)}")
        elif isinstance(x, str):
            if "/9j/" in x or "0.123457" in x:
                found.append(f"str {x[:60]!r}")
        elif isinstance(x, float):
            if x == 0.123457:
                found.append("landmark float")
        elif isinstance(x, dict):
            stack += [*x.keys(), *x.values()]
        elif isinstance(x, (list, tuple, set, frozenset, deque)):
            if isinstance(x, (list, tuple)) and _is_landmarks(x):
                found.append("landmarks list")
            stack += list(x)
        elif type(x).__module__.split(".")[0] == "app" and hasattr(x, "__dict__"):
            stack += list(vars(x).values())
    return found


def test_no_image_or_landmarks_in_stores(caplog):
    caplog.set_level(logging.DEBUG)
    before = set(jobs.TASKS)

    def check(c):
        svc = c.app.state.jobs
        assert svc.jobs and _leaks(svc) == []
        end = time.monotonic() + 1.0
        while jobs.TASKS - before and time.monotonic() < end:
            time.sleep(0.01)
        assert not jobs.TASKS - before  # every job task finished and was discarded

    _all_outcomes(check)


def test_no_disk_after_success_failure_timeout(monkeypatch, tmp_path):
    def rollover(self):
        raise AssertionError("spooled to disk")

    monkeypatch.setattr(tempfile.SpooledTemporaryFile, "rollover", rollover)
    monkeypatch.setattr(tempfile, "tempdir", str(tmp_path))
    monkeypatch.chdir(tmp_path)
    run_outcomes()
    assert list(tmp_path.iterdir()) == []
