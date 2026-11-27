"""The only module that knows the Anthropic SDK call shape (D-04)."""

import base64
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Final

import anthropic
from pydantic import ValidationError

from app.schemas.llm import LlmReading

PROMPT_VERSION: Final = "p1"
# CRLF normalised so the cached prefix is byte-stable across checkouts (Pitfall 8)
SYSTEM_PROMPT: Final[str] = (
    (Path(__file__).resolve().parents[1] / "prompts" / "palm_v1.md")
    .read_text(encoding="utf-8")
    .replace("\r\n", "\n")
)
LANGUAGES: Final = {"en": "English", "hi": "Hindi"}


class Malformed(Exception):
    """Unusable model output; the job retries once (D-05). usage/model are None when the SDK
    raised before returning a message."""

    def __init__(self, usage: Any = None, model: str | None = None):
        super().__init__()
        self.usage, self.model = usage, model


class ModelFailure(Exception):
    """Ends the job with code busy | timeout | model_error, no retry. billable: the request may
    have reached the model without returning usage."""

    def __init__(self, code: str, billable: bool = False):
        super().__init__(code)
        self.code, self.billable = code, billable


class Refused(ModelFailure):
    """stop_reason refusal (D-04): not retried, since refusals repeat."""

    def __init__(self, usage: Any = None, model: str | None = None):
        super().__init__("model_error")
        self.usage, self.model = usage, model


@dataclass
class ModelReply:
    parsed: LlmReading
    usage: Any
    model: str


class PalmModel:
    def __init__(self, client: Any, model: str):
        self.client = client
        self.model = model

    async def read_once(self, jpeg: bytes, w: int, h: int, hand: str, locale: str) -> ModelReply:
        try:
            msg = await self._parse(jpeg, w, h, hand, locale)
        except ValidationError:
            # parse() validates before stop_reason is visible (Pitfall 2); the error text can
            # carry model output, so it is dropped
            raise Malformed() from None
        # Most specific first: APITimeoutError subclasses APIConnectionError, and
        # InternalServerError covers 529 overloaded
        except anthropic.APITimeoutError as e:
            raise ModelFailure("timeout", billable=True) from e
        except (anthropic.RateLimitError, anthropic.InternalServerError) as e:
            raise ModelFailure("busy") from e
        except anthropic.APIConnectionError as e:
            raise ModelFailure("busy", billable=True) from e
        except anthropic.APIStatusError as e:  # 400/401/403/404: our bug or a key problem
            raise ModelFailure("model_error") from e
        if msg.stop_reason == "refusal":
            raise Refused(msg.usage, msg.model)
        if msg.stop_reason == "max_tokens" or msg.parsed_output is None:
            raise Malformed(msg.usage, msg.model)
        return ModelReply(msg.parsed_output, msg.usage, msg.model)

    async def _parse(self, jpeg: bytes, w: int, h: int, hand: str, locale: str) -> Any:
        # No sampling, thinking, tool_choice or prefill parameters: they 400 on 5.x models (D-04)
        return await self.client.messages.parse(
            model=self.model,
            max_tokens=8000,
            system=[{"type": "text", "text": SYSTEM_PROMPT, "cache_control": {"type": "ephemeral"}}],
            messages=[
                {
                    "role": "user",
                    "content": [
                        {
                            "type": "image",
                            "source": {
                                "type": "base64",
                                "media_type": "image/jpeg",
                                "data": base64.standard_b64encode(jpeg).decode(),
                            },
                            # never let the API silently resize: coordinates stay 1:1 (Pitfall 3)
                            "transformations": {"oversized_image": "error"},
                        },
                        {
                            "type": "text",
                            "text": f"Image size: {w}x{h} px. Hand: {hand}. "
                            f"Write all text values in: {LANGUAGES[locale]}.",
                        },
                    ],
                }
            ],
            output_format=LlmReading,
            output_config={"effort": "low"},
        )
