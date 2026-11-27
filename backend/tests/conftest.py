"""Shared fakes and helpers. Tests never reach the real Anthropic API (D-16)."""

import asyncio
import io
import json
import time
import uuid
from types import SimpleNamespace

import anthropic
import httpx2
import pytest
from fastapi.testclient import TestClient
from PIL import Image, ImageDraw

from app.config import Settings
from app.main import create_app
from app.schemas.llm import LlmReading


def _no_real_client(*args, **kwargs):
    raise AssertionError("real Anthropic client in tests")


@pytest.fixture(autouse=True)
def no_real_anthropic(monkeypatch):
    for var in ("ANTHROPIC_API_KEY", "PALM_MODEL", "DAILY_SPEND_CAP_USD"):
        monkeypatch.delenv(var, raising=False)
    # Backstop: a client that slips past the guard fails on connect instead of reaching the API
    monkeypatch.setenv("ANTHROPIC_BASE_URL", "http://127.0.0.1:9")
    monkeypatch.setattr(anthropic, "AsyncAnthropic", _no_real_client)
    monkeypatch.setattr(anthropic, "Anthropic", _no_real_client)


def make_jpeg(size=(960, 1280), exif=False) -> bytes:
    """Skin-toned photo with a dense dark grid: bright and sharp enough for the pre-gate."""
    img = Image.new("RGB", size, (214, 172, 140))
    draw = ImageDraw.Draw(img)
    for x in range(0, size[0], 16):
        draw.line([(x, 0), (x, size[1])], fill=(90, 60, 48), width=2)
    for y in range(0, size[1], 16):
        draw.line([(0, y), (size[0], y)], fill=(90, 60, 48), width=2)
    out = io.BytesIO()
    if exif:
        ex = Image.Exif()
        ex[0x010F] = "TestCam"  # Make
        gps = ex.get_ifd(0x8825)  # GPSInfo IFD
        gps.update({1: "N", 2: (12.0, 58.0, 30.0), 3: "E", 4: (77.0, 35.0, 10.0)})
        img.save(out, "JPEG", quality=90, exif=ex)
    else:
        img.save(out, "JPEG", quality=90)
    return out.getvalue()


def jpeg_variant(i: int) -> bytes:
    """make_jpeg() with one mid-cell pixel turned white: new sha256, same dHash."""
    img = Image.open(io.BytesIO(make_jpeg()))
    img.putpixel((8 + 16 * i, 8), (255, 255, 255))
    out = io.BytesIO()
    img.save(out, "JPEG", quality=90)
    return out.getvalue()


# 21 normalised right-palm points [x, y, z] (wrist low, fingers up). Landmark 1's z is the
# privacy marker 0.123457 that plan 02-06 searches logs for.
LANDMARKS = [
    [0.50, 0.88, 0.0],
    [0.36, 0.80, 0.123457],
    [0.27, 0.70, -0.02],
    [0.21, 0.61, -0.03],
    [0.17, 0.53, -0.04],
    [0.37, 0.50, -0.01],
    [0.35, 0.38, -0.02],
    [0.34, 0.30, -0.03],
    [0.33, 0.22, -0.04],
    [0.48, 0.48, -0.01],
    [0.48, 0.35, -0.02],
    [0.48, 0.26, -0.03],
    [0.48, 0.17, -0.04],
    [0.58, 0.50, -0.01],
    [0.60, 0.38, -0.02],
    [0.61, 0.30, -0.03],
    [0.62, 0.23, -0.04],
    [0.67, 0.54, -0.01],
    [0.70, 0.45, -0.02],
    [0.72, 0.39, -0.03],
    [0.73, 0.33, -0.04],
]
LANDMARKS_LEFT = [[round(1 - x, 6), y, z] for x, y, z in LANDMARKS]

# Pixel points in a 960x1280 photo, inside the palm polygon of LANDMARKS 0, 1, 2, 5, 9, 13, 17
_POINTS = {
    "heart": [(620, 710), (580, 700), (540, 690), (500, 690), (460, 700), (420, 715)],
    "head": [(380, 780), (420, 790), (460, 800), (500, 810), (540, 822), (565, 835)],
    "life": [(370, 760), (340, 820), (330, 880), (345, 940), (375, 1000), (410, 1050)],
    "fate": [(470, 1080), (472, 1000), (474, 920), (476, 840), (478, 760), (480, 690)],
    "sun": [(555, 880), (558, 840), (561, 800), (563, 760), (565, 720), (566, 680)],
}


def ok_reply(w=960, h=1280):
    lines = {
        key: {
            "present": True,
            "length": "medium",
            "curvature": "gentle",
            "depth": "clear",
            "continuity": "continuous",
            "points": [{"x": round(x * w / 960), "y": round(y * h / 1280)} for x, y in pts],
            "interpretation": f"A clear, gently curving {key} line suggests a steady and thoughtful nature.",
        }
        for key, pts in _POINTS.items()
    }
    parsed = LlmReading.model_validate(
        {
            "verdict": {"ok": True, "reason": "none"},
            "lines": lines,
            "summary": "Your palm shows a calm, curious spirit. You tend to think before you act.",
            "traits": ["curious", "patient", "warm"],
            "how_you_love": "You may find that you show care through small, steady gestures.",
            "ideal_partner": "Someone playful who enjoys quiet evenings and long talks may suit you.",
            "love_challenges": "You may hold back your feelings until you feel truly at ease.",
            "strengths": "You tend to plan carefully and finish what you start.",
            "career_challenges": "You may find fast, noisy settings tiring over time.",
            "ideal_industries": ["design", "education", "research"],
        }
    )
    usage = SimpleNamespace(
        input_tokens=4000,
        output_tokens=2000,
        cache_creation_input_tokens=0,
        cache_read_input_tokens=0,
    )
    return SimpleNamespace(
        parsed_output=parsed, stop_reason="end_turn", usage=usage, model="claude-sonnet-5-5"
    )


def sdk_error(cls, status: int | None = None) -> Exception:
    """An SDK exception built with the installed 2.14.0 constructors (httpx2 is the SDK's transport)."""
    req = httpx2.Request("POST", "https://api.anthropic.com/v1/messages")
    if status is None:  # APIConnectionError / APITimeoutError take only the request
        return cls(request=req)
    return cls("sdk error", response=httpx2.Response(status, request=req), body=None)


def reply_with(**kw):
    """ok_reply() with fields replaced, e.g. stop_reason="refusal" or parsed_output=None."""
    r = ok_reply()
    for k, v in kw.items():
        setattr(r, k, v)
    return r


class FakeSdk:
    """Stands in for AsyncAnthropic at the SDK boundary. Outcomes are consumed in order and
    the last one repeats: an Exception is raised, a float sleeps then returns ok_reply()."""

    def __init__(self, outcomes=None):
        self.outcomes = list(outcomes) if outcomes else [ok_reply()]
        self.calls: list[dict] = []
        self.messages = self

    async def parse(self, **kw):
        self.calls.append(kw)
        out = self.outcomes[min(len(self.calls), len(self.outcomes)) - 1]
        if isinstance(out, Exception):
            raise out
        if isinstance(out, float):
            await asyncio.sleep(out)
            return ok_reply()
        return out


def settings(**kw) -> Settings:
    # Existing tests exercise the Claude path with a fake SDK; template mode is opted into
    return Settings(**{"_env_file": None, "anthropic_api_key": None, "palm_mode": "claude", **kw})


def client_factory(fake=None, **settings_kw) -> TestClient:
    # Always use with `with`: background job tasks die with the per-request portal otherwise
    return TestClient(create_app(settings(**settings_kw), anthropic_client=fake or FakeSdk()))


def post_job(c, jpeg=None, *, hand="right", locale="en", landmarks=LANDMARKS, headers=None):
    hdrs = {"X-Install-Id": str(uuid.uuid4()), "Idempotency-Key": str(uuid.uuid4())}
    if headers is not None:
        hdrs = headers
    return c.post(
        "/v1/palm/jobs",
        files={"image": ("palm.jpg", jpeg if jpeg is not None else make_jpeg(), "image/jpeg")},
        data={"hand_side": hand, "locale": locale, "landmarks": json.dumps(landmarks)},
        headers=hdrs,
    )


def poll(c, job_id, timeout=2.0) -> dict:
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        body = c.get(f"/v1/palm/jobs/{job_id}").json()
        if body["status"] in ("done", "failed"):
            return body
        time.sleep(0.01)
    raise AssertionError(f"job {job_id} did not finish in {timeout}s")
