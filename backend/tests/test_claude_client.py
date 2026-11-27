import asyncio
import base64

from app.schemas.llm import LlmReading
from app.services.claude_client import SYSTEM_PROMPT, PalmModel
from conftest import FakeSdk, make_jpeg


def test_parse_call_shape():
    fake = FakeSdk()
    jpeg = make_jpeg()
    reply = asyncio.run(PalmModel(fake, "claude-sonnet-5-5").read_once(jpeg, 960, 1280, "right", "en"))
    assert reply.model == "claude-sonnet-5-5"
    assert isinstance(reply.parsed, LlmReading)
    [kw] = fake.calls
    assert set(kw) == {"model", "max_tokens", "system", "messages", "output_format", "output_config"}
    assert kw["output_config"] == {"effort": "low"}
    assert kw["output_format"] is LlmReading
    [system] = kw["system"]
    assert system["cache_control"] == {"type": "ephemeral"}
    assert system["text"] == SYSTEM_PROMPT
    [turn] = kw["messages"]
    assert turn["role"] == "user"
    image, text = turn["content"]
    assert image["type"] == "image"
    assert image["source"]["type"] == "base64"
    assert image["source"]["media_type"] == "image/jpeg"
    assert base64.standard_b64decode(image["source"]["data"]) == jpeg
    assert image["transformations"] == {"oversized_image": "error"}
    assert text["type"] == "text"
    assert "960x1280" in text["text"]
    assert "English" in text["text"]


def test_system_prompt_is_stable():
    assert "\r" not in SYSTEM_PROMPT
    assert len(SYSTEM_PROMPT) >= 2500
