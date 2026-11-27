"""Server output filter (D-13, READ-07): a prose field that mentions death, illness, pregnancy,
medical care, legal outcomes or a financial directive is rewritten from neutral text."""

import re
import unicodedata
from typing import Final

from app.schemas.llm import LlmReading


def normalise(s: str) -> str:
    """Match copy only: NFKC, casefold, drop format chars (ZWSP, joiners), fold nukta and chandrabindu."""
    s = unicodedata.normalize("NFKC", s).casefold()
    s = "".join(ch for ch in s if unicodedata.category(ch) != "Cf")
    return s.replace("़", "").replace("ँ", "ं")


# Patterns go through normalise too, so stems fold like the text; use only lowercase escapes
RULES: Final = [
    (rule_id, re.compile(normalise(pattern)))
    for rule_id, pattern in [
        ("en.death", r"\b(die|dies|died)\b(?!-hard)|\bdying\b|\bdeaths?\b|\bpass(es|ed|ing)? away\b"),
        ("en.lifespan", r"\blife\s*span\b|\blifespans?\b|\blife expectancy\b|\blongevity\b"),
        # The p1 prompt asks for each line's length, so "a long life line" is fine
        ("en.life_length", r"\b(long|short) life\b(?![\s-]*lines?\b)"),
        (
            "en.illness",
            r"\billness\w*|\bdiseases?\b|\bcancers?\b|\bdiabet\w*|\bheart attacks?\b"
            r"|\bsurger(y|ies)\b|\bdiagnos(is|ed)\b|\bmedicat\w*|\bdoctors?\b|\bhospitals?\b",
        ),
        (
            "en.pregnancy",
            r"\bpregnan\w*|\bfertility\b|\binfertil\w*|\bmiscarr\w*|\bconceiv\w*\s+(a\s+)?(child|baby)",
        ),
        ("en.gambling", r"\blotter(y|ies)\b|\bgambl\w*"),
        ("en.legal", r"\blawsuits?\b|\bcourts?\b|\blegal action\b"),
        # Advice, not bare career words: "Medicine" or "Finance" as an industry is fine
        (
            "en.directive",
            r"\b(should|must|need to|have to|avoid|stop|start)\b.{0,40}?"
            r"\b(invest\w*|stocks?\b|shares\b|crypto\w*|loans?\b|borrow\w*|sue\b|divorce\w*"
            r"|medicine\w*|medicat\w*|treatment\w*|diet\w*)",
        ),
        (
            "hinglish",
            r"\b(maut|mrityu|bimari\w*|rog|garbh\w*|dawai\w*|ilaaj|satta|jua|mukadm\w*)\b",
        ),
        # Devanagari: no \b (matras are not \w, A10), so plain stems and lookarounds
        ("hi.death", r"मृत्यु|मौत|(?<![ऀ-ॿ])मर(न|े| जा| सक)|निधन|देहांत"),
        (
            "hi.lifespan",
            r"(लंबी|लम्बी|कम|छोटी)\s*उम्र|उम्र\s*(लंबी|लम्बी|कम|छोटी)|आयु|दीर्घायु|अल्पायु",
        ),
        ("hi.illness", r"बीमार|रोग|कैंसर|दवा|इलाज|डॉक्टर|अस्पताल"),
        ("hi.pregnancy", r"गर्भ|प्रेग्नें|बांझ"),
        ("hi.gambling", r"लॉटरी|सट्टा|जुआ"),
        ("hi.legal", r"मुकदमा|अदालत|कोर्ट"),
        # bare शेयर also means "share (feelings)", so only the market senses count
        ("hi.directive", r"(निवेश|शेयर(ों| में| बाज़ार)|कर्ज|लोन|तलाक).{0,30}(करें|चाहिए|कीजिए|मत)"),
    ]
]


def hits(text: str) -> list[str]:
    """Ids of the matching rules, never the matched text, so callers can log or count safely."""
    t = normalise(text)
    return [rule_id for rule_id, rx in RULES if rx.search(t)]


NEUTRAL: Final[dict[str, dict[str, str | list[str]]]] = {
    "en": {
        "summary": "Your palm suggests a thoughtful nature with a quiet, steady energy.",
        "how_you_love": "You may find that you show care in your own thoughtful way.",
        "ideal_partner": "Someone kind and easy to talk to may bring out your best.",
        "love_challenges": "You may find it helps to share your feelings a little sooner.",
        "strengths": "You tend to bring patience and focus to what you do.",
        "career_challenges": "You may find that juggling many tasks at once takes practice.",
        "interpretation": "This line suggests a steady, thoughtful energy.",
        "traits": ["thoughtful", "steady", "kind"],
        "ideal_industries": ["design", "education", "communication"],
    },
    "hi": {
        "summary": "आपकी हथेली एक विचारशील स्वभाव और शांत, स्थिर ऊर्जा का संकेत देती है।",
        "how_you_love": "आप अपने विचारशील ढंग से परवाह जताते हैं।",
        "ideal_partner": "कोई दयालु और सहज बातचीत करने वाला साथी आपके लिए अच्छा हो सकता है।",
        "love_challenges": "अपनी भावनाएँ थोड़ा जल्दी साझा करना आपके लिए मददगार हो सकता है।",
        "strengths": "आप अपने काम में धैर्य और ध्यान लाते हैं।",
        "career_challenges": "एक साथ कई काम संभालने में थोड़ा अभ्यास लग सकता है।",
        "interpretation": "यह रेखा एक स्थिर, विचारशील ऊर्जा का संकेत देती है।",
        "traits": ["विचारशील", "धैर्यवान", "दयालु"],
        "ideal_industries": ["डिज़ाइन", "शिक्षा", "संचार"],
    },
}

STR_FIELDS: Final = (
    "summary",
    "how_you_love",
    "ideal_partner",
    "love_challenges",
    "strengths",
    "career_challenges",
)
LIST_FIELDS: Final = ("traits", "ideal_industries")


def filter_llm(llm: LlmReading, locale: str) -> tuple[LlmReading, int]:
    """A copy of llm with every hit field rewritten, plus the number of rewritten fields."""
    neutral = NEUTRAL[locale]
    update: dict = {}
    for name in STR_FIELDS:
        if hits(getattr(llm, name)):
            update[name] = neutral[name]
    for name in LIST_FIELDS:
        items = getattr(llm, name)
        kept = [item for item in items if not hits(item)]
        if len(kept) < len(items):
            kept += [d for d in neutral[name] if d not in kept][: max(0, 3 - len(kept))]
            update[name] = kept
    lines = {
        key: line.model_copy(update={"interpretation": neutral["interpretation"]})
        for key in type(llm.lines).model_fields
        if hits((line := getattr(llm.lines, key)).interpretation)
    }
    count = len(update) + len(lines)
    if lines:
        update["lines"] = llm.lines.model_copy(update=lines)
    return llm.model_copy(update=update), count
