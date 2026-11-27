import asyncio
import base64

import anthropic
import pytest

from app.schemas.llm import LlmReading
from app.services.claude_client import SYSTEM_PROMPT, Malformed, ModelFailure, PalmModel, Refused
from conftest import FakeSdk, make_jpeg, reply_with, sdk_error


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


def _read(outcome):
    return asyncio.run(PalmModel(FakeSdk([outcome]), "claude-sonnet-5-5").read_once(make_jpeg(), 960, 1280, "right", "en"))


def test_sdk_exception_classes_exist():
    for name in ("RateLimitError", "InternalServerError", "APIConnectionError", "APITimeoutError", "APIStatusError"):
        assert isinstance(getattr(anthropic, name), type)
    assert issubclass(anthropic.APITimeoutError, anthropic.APIConnectionError)


def test_refusal_raises_refused_with_usage():
    reply = reply_with(stop_reason="refusal", model="claude-haiku-5-5")
    with pytest.raises(Refused) as ei:
        _read(reply)
    assert ei.value.code == "model_error" and not ei.value.billable
    assert ei.value.usage is reply.usage and ei.value.model == "claude-haiku-5-5"


@pytest.mark.parametrize("kw", [{"stop_reason": "max_tokens"}, {"parsed_output": None}])
def test_cut_or_unparsed_reply_is_malformed_with_usage(kw):
    reply = reply_with(**kw)
    with pytest.raises(Malformed) as ei:
        _read(reply)
    assert ei.value.usage is reply.usage and ei.value.model == reply.model


@pytest.mark.parametrize(
    "err, code, billable",
    [
        (sdk_error(anthropic.APITimeoutError), "timeout", True),
        (sdk_error(anthropic.APIConnectionError), "busy", True),
        (sdk_error(anthropic.RateLimitError, 429), "busy", False),
        (sdk_error(anthropic.InternalServerError, 500), "busy", False),
        (sdk_error(anthropic.OverloadedError, 529), "busy", False),
        (sdk_error(anthropic.APIStatusError, 400), "model_error", False),
    ],
)
def test_sdk_errors_map_to_codes(err, code, billable):
    with pytest.raises(ModelFailure) as ei:
        _read(err)
    assert (ei.value.code, ei.value.billable) == (code, billable)
    assert not isinstance(ei.value, Refused)
