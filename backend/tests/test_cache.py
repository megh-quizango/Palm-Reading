"""Result cache (READ-04, D-12) and quality pre-gate (D-09)."""

import io
import time
import uuid

import pytest
from PIL import Image, ImageDraw, ImageFilter, ImageStat

from app import jobs
from app.services import claude_client
from app.services import image_prep as ip
from conftest import LANDMARKS_LEFT, FakeSdk, client_factory, jpeg_variant, make_jpeg, poll, post_job


def _hdrs(install: str) -> dict:
    return {"X-Install-Id": install, "Idempotency-Key": str(uuid.uuid4())}


def _jpeg(img: Image.Image) -> bytes:
    out = io.BytesIO()
    img.save(out, "JPEG", quality=90)
    return out.getvalue()


def _ramp(rising: bool) -> bytes:
    """Gridded photo under a horizontal brightness ramp: dHash bits all one way."""
    w, h = 960, 1280
    img = Image.new("RGB", (w, h))
    draw = ImageDraw.Draw(img)
    for x in range(w):
        f = 0.4 + 0.6 * (x / w if rising else 1 - x / w)
        draw.line([(x, 0), (x, h)], fill=(int(214 * f), int(172 * f), int(140 * f)))
    for x in range(0, w, 16):
        draw.line([(x, 0), (x, h)], fill=(90, 60, 48), width=2)
    for y in range(0, h, 16):
        draw.line([(0, y), (w, y)], fill=(90, 60, 48), width=2)
    return _jpeg(img)


def test_same_photo_same_install_is_cached():
    fake = FakeSdk()
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        first = poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
        r = post_job(c, headers=_hdrs(install))
        assert r.status_code == 202
        again = c.get(f"/v1/palm/jobs/{r.json()['job_id']}").json()
    assert first["status"] == again["status"] == "done"
    assert again["reading"] == first["reading"]
    assert again["job_id"] != first["job_id"]
    assert len(fake.calls) == 1


def test_pregate_metrics_unit():
    board = Image.new("L", (512, 512))
    draw = ImageDraw.Draw(board)
    for i in range(0, 512, 32):
        for j in range(0, 512, 32):
            if (i // 32 + j // 32) % 2:
                draw.rectangle([i, j, i + 31, j + 31], fill=255)
    assert ip.laplacian_var(board) > ip.laplacian_var(board.filter(ImageFilter.GaussianBlur(8)))
    assert ImageStat.Stat(Image.new("L", (64, 64))).mean[0] < ip.PREGATE_MIN_MEAN
    p = ip.prepare(make_jpeg())
    assert p.brightness >= ip.PREGATE_MIN_MEAN and p.laplacian_var >= ip.PREGATE_MIN_LAPLACIAN_VAR


def test_recapture_within_distance_is_hit():
    fake = FakeSdk()
    install = str(uuid.uuid4())
    a, b = ip.prepare(make_jpeg()), ip.prepare(jpeg_variant(3))
    assert a.sha256 != b.sha256 and ip.hamming(a.dhash, b.dhash) <= jobs.DHASH_MAX_DISTANCE
    with client_factory(fake) as c:
        first = poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
        second = poll(c, post_job(c, jpeg_variant(3), headers=_hdrs(install)).json()["job_id"])
    assert second["reading"] == first["reading"] and len(fake.calls) == 1


def test_visibly_different_photo_is_miss():
    up, down = _ramp(True), _ramp(False)
    assert ip.hamming(ip.prepare(up).dhash, ip.prepare(down).dhash) > jobs.DHASH_MAX_DISTANCE
    fake = FakeSdk()
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        first = poll(c, post_job(c, up, headers=_hdrs(install)).json()["job_id"])
        second = poll(c, post_job(c, down, headers=_hdrs(install)).json()["job_id"])
    assert first["reading"]["reading_id"] != second["reading"]["reading_id"]
    assert len(fake.calls) == 2


def test_other_install_never_gets_the_entry():
    fake = FakeSdk()
    with client_factory(fake) as c:
        a = poll(c, post_job(c, headers=_hdrs(str(uuid.uuid4()))).json()["job_id"])
        b = poll(c, post_job(c, headers=_hdrs(str(uuid.uuid4()))).json()["job_id"])
    assert a["reading"]["reading_id"] != b["reading"]["reading_id"] and len(fake.calls) == 2


@pytest.mark.parametrize("change", ["hand", "locale", "prompt"])
def test_key_change_is_miss(change, monkeypatch):
    fake = FakeSdk()
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
        kw = {}
        if change == "hand":
            kw = {"hand": "left", "landmarks": LANDMARKS_LEFT}
        elif change == "locale":
            kw = {"locale": "hi"}
        else:
            monkeypatch.setattr(claude_client, "PROMPT_VERSION", "p-test")
        poll(c, post_job(c, headers=_hdrs(install), **kw).json()["job_id"])
    assert len(fake.calls) == 2


def test_entries_expire_after_24h(monkeypatch):
    fake = FakeSdk()
    install = str(uuid.uuid4())
    with client_factory(fake) as c:
        poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
        t0 = time.monotonic()
        monkeypatch.setattr(jobs, "now", lambda: t0 + jobs.CACHE_TTL_S + 1)
        poll(c, post_job(c, headers=_hdrs(install)).json()["job_id"])
    assert len(fake.calls) == 2


def test_cache_is_bounded_and_holds_json_only(monkeypatch):
    monkeypatch.setattr(jobs, "CACHE_MAX", 2)
    monkeypatch.setattr(jobs, "DHASH_MAX_DISTANCE", -1)  # exact sha256 hits only
    install = str(uuid.uuid4())
    with client_factory() as c:
        for i in range(3):
            poll(c, post_job(c, jpeg_variant(i), headers=_hdrs(install)).json()["job_id"])
        cache = c.app.state.jobs.cache
    shas = [k[4] for k in cache]
    assert len(cache) == 2 and ip.prepare(jpeg_variant(0)).sha256 not in shas
    for created, dh, reading in cache.values():
        assert type(created) is float and type(dh) is int and type(reading) is str


@pytest.mark.parametrize("kind", ["dark", "blurry"])
def test_pregate_answers_without_model_call(kind):
    if kind == "dark":
        img = Image.new("RGB", (960, 1280))
    else:
        img = Image.linear_gradient("L").resize((960, 1280)).convert("RGB")
        img = img.filter(ImageFilter.GaussianBlur(12))
    fake = FakeSdk()
    with client_factory(fake) as c:
        r = post_job(c, _jpeg(img))
        assert r.status_code == 202
        body = poll(c, r.json()["job_id"])
    assert body["status"] == "done"
    assert body["reading"]["verdict"] == {"ok": False, "reason": "blurry_or_dark"}
    assert body["reading"]["lines"] == [] and len(fake.calls) == 0
