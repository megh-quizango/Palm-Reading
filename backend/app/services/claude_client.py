"""The only module that knows the Anthropic SDK call shape (D-04)."""

import base64
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Final

from app.schemas.llm import LlmReading

PROMPT_VERSION: Final = "p1"
# CRLF normalised so the cached prefix is byte-stable across checkouts (Pitfall 8)
SYSTEM_PROMPT: Final[str] = (
    (Path(__file__).resolve().parents[1] / "prompts" / "palm_v1.md")
    .read_text(encoding="utf-8")
    .replace("\r\n", "\n")
)
LANGUAGES: Final = {"en": "English", "hi": "Hindi"}


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
        # No sampling, thinking, tool_choice or prefill parameters: they 400 on 5.x models (D-04)
        msg = await self.client.messages.parse(
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
        return ModelReply(msg.parsed_output, msg.usage, msg.model)
