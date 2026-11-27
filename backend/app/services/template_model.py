"""Canned readings for PALM_MODE=template: no Anthropic call, no key, no spend.

Same interface as PalmModel. Lines carry no points, so every trace becomes a landmark-anchored
template (trace_source "template"); the prose variant is picked from the photo's SHA-256, so the
same photo always gets the same text.
"""

import hashlib
from types import SimpleNamespace
from typing import Final

from app.schemas.llm import LlmReading
from app.services.claude_client import ModelReply

MODEL_ID: Final = "template-v1"

_LINE = {"present": True, "length": "medium", "curvature": "gentle", "depth": "clear",
         "continuity": "continuous", "points": []}

_TEXT: Final = {
    "en": [
        {
            "summary": "Your palm shows a calm, curious spirit that likes to understand things deeply.",
            "traits": ["curious", "patient", "warm"],
            "lines": {
                "heart": "A gently curved heart line suggests warmth and loyalty in close bonds.",
                "head": "A clear head line points to careful, practical thinking.",
                "life": "A steady life line suggests energy you pace well.",
                "fate": "A visible fate line hints at a strong sense of direction.",
                "sun": "A sun line suggests creativity that others notice.",
            },
            "how_you_love": "You may show care through small, steady gestures.",
            "ideal_partner": "Someone playful who enjoys long talks may suit you.",
            "love_challenges": "You may hold back your feelings at first.",
            "strengths": "You tend to plan carefully and finish what you start.",
            "career_challenges": "You may find noisy, rushed settings tiring.",
            "ideal_industries": ["design", "education", "research"],
        },
        {
            "summary": "Your palm hints at a bold, open nature with a quick mind.",
            "traits": ["bold", "sociable", "inventive"],
            "lines": {
                "heart": "A long heart line suggests you love openly and generously.",
                "head": "A long head line points to wide-ranging ideas.",
                "life": "A broad life line suggests an active, outgoing pace.",
                "fate": "A forked fate line hints at more than one path that interests you.",
                "sun": "A clear sun line suggests you enjoy being seen for your work.",
            },
            "how_you_love": "You tend to show love through shared adventures.",
            "ideal_partner": "A calm, grounded partner may balance your energy.",
            "love_challenges": "You may move fast and need to slow down to listen.",
            "strengths": "You start things with energy and bring people along.",
            "career_challenges": "Routine work may bore you quickly.",
            "ideal_industries": ["media", "sales", "entrepreneurship"],
        },
        {
            "summary": "Your palm suggests a gentle, thoughtful person with a creative streak.",
            "traits": ["thoughtful", "kind", "creative"],
            "lines": {
                "heart": "A curved heart line suggests deep feelings you share with a few people.",
                "head": "A sloping head line points to imagination and intuition.",
                "life": "A smooth life line suggests a steady, balanced rhythm.",
                "fate": "A faint fate line hints that you shape your own path as you go.",
                "sun": "A short sun line suggests quiet pride in what you make.",
            },
            "how_you_love": "You may express love through listening and thoughtful words.",
            "ideal_partner": "A patient partner who values honesty may suit you.",
            "love_challenges": "You may wait for others to speak first.",
            "strengths": "You notice details others miss and think before you act.",
            "career_challenges": "You may doubt your ideas before sharing them.",
            "ideal_industries": ["writing", "art", "healthcare support"],
        },
    ],
    "hi": [
        {
            "summary": "आपकी हथेली एक शांत और जिज्ञासु स्वभाव दिखाती है।",
            "traits": ["जिज्ञासु", "धैर्यवान", "स्नेही"],
            "lines": {
                "heart": "हल्की मुड़ी हृदय रेखा रिश्तों में स्नेह और वफ़ादारी दिखाती है।",
                "head": "साफ़ मस्तिष्क रेखा सोच-समझकर काम करने की आदत दिखाती है।",
                "life": "स्थिर जीवन रेखा संतुलित ऊर्जा का संकेत देती है।",
                "fate": "भाग्य रेखा आपके लक्ष्य की स्पष्ट दिशा दिखाती है।",
                "sun": "सूर्य रेखा आपकी रचनात्मकता का संकेत देती है।",
            },
            "how_you_love": "आप छोटे-छोटे कामों से अपना प्यार जताते हैं।",
            "ideal_partner": "बातें करना पसंद करने वाला खुशमिज़ाज साथी आपके लिए अच्छा हो सकता है।",
            "love_challenges": "शुरुआत में आप अपनी भावनाएँ छिपा सकते हैं।",
            "strengths": "आप योजना बनाकर काम पूरा करते हैं।",
            "career_challenges": "शोर-शराबे वाली जगहें आपको थका सकती हैं।",
            "ideal_industries": ["डिज़ाइन", "शिक्षा", "शोध"],
        },
        {
            "summary": "आपकी हथेली एक साहसी और खुले स्वभाव का संकेत देती है।",
            "traits": ["साहसी", "मिलनसार", "नवाचारी"],
            "lines": {
                "heart": "लंबी हृदय रेखा खुलकर प्यार करने का संकेत देती है।",
                "head": "लंबी मस्तिष्क रेखा नए विचारों की ओर झुकाव दिखाती है।",
                "life": "चौड़ी जीवन रेखा सक्रिय स्वभाव का संकेत देती है।",
                "fate": "भाग्य रेखा की शाखा कई रुचियों का संकेत देती है।",
                "sun": "साफ़ सूर्य रेखा दिखाती है कि आपको अपने काम की पहचान अच्छी लगती है।",
            },
            "how_you_love": "आप साथ में नए अनुभव करके प्यार जताते हैं।",
            "ideal_partner": "शांत और समझदार साथी आपकी ऊर्जा को संतुलित कर सकता है।",
            "love_challenges": "आप जल्दी आगे बढ़ते हैं, कभी रुककर सुनना भी ज़रूरी है।",
            "strengths": "आप जोश से शुरुआत करते हैं और लोगों को साथ लाते हैं।",
            "career_challenges": "एक जैसा काम आपको जल्दी उबा सकता है।",
            "ideal_industries": ["मीडिया", "बिक्री", "उद्यमिता"],
        },
    ],
}


def _reading(t: dict) -> LlmReading:
    return LlmReading.model_validate(
        {
            "verdict": {"ok": True, "reason": "none"},
            "lines": {k: {**_LINE, "interpretation": v} for k, v in t["lines"].items()},
            **{k: v for k, v in t.items() if k != "lines"},
        }
    )


class TemplateModel:
    model = MODEL_ID

    async def read_once(self, jpeg: bytes, w: int, h: int, hand: str, locale: str) -> ModelReply:
        variants = _TEXT[locale]
        pick = variants[int.from_bytes(hashlib.sha256(jpeg).digest()[:4], "big") % len(variants)]
        usage = SimpleNamespace(input_tokens=0, output_tokens=0,
                                cache_creation_input_tokens=0, cache_read_input_tokens=0)
        return ModelReply(_reading(pick), usage, MODEL_ID)
