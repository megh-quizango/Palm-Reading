"""D-13 output filter: denylist (en, hi, Hinglish), directive patterns, rewrite and red-team."""

import pytest
from conftest import FakeSdk, client_factory, ok_reply, poll, post_job

from app.schemas.llm import LlmReading
from app.services.safety import NEUTRAL, RULES, filter_llm, hits

ENGLISH = [
    "you will die young",
    "a long life ahead",
    "life expectancy",
    "see a doctor about your heart",
    "your diabetes",
    "you may get pregnant soon",
    "fertility",
    "you should invest in crypto now",
    "you must stop your medication",
    "a lawsuit will go your way",
    "you will win the lottery",
]
HINDI = [
    "आपकी उम्र कम है",
    "मृत्यु का योग",
    "मौत",
    "बीमारी",
    "रोग",
    "गर्भ",
    "डॉक्टर से मिलें",
    "अस्पताल",
    "शेयर में निवेश करें",
    "आपको शेयर बाज़ार में पैसा लगाना चाहिए",
    "अदालत",
    "सट्टा",
]
HINDI_VARIANTS = [
    "आप बीमार पड़ सकते हैं",
    "बीमारियों से सावधान रहें",
    "जीवन रेखा लंबी आयु का संकेत है",
    "कम उम्र में",
    "दीर्घायु",
    "आप जल्दी मरेंगे",
    "आप मर सकते हैं",
    "निधन",
    "देहांत",
    "मुक़दमा",  # precomposed qa (U+0958)
    "मुक़दमा",  # ka + nukta
    "बाँझ",  # chandrabindu
    "मृ​त्यु",  # zero-width space inside the word
    "मृत्‍यु",  # zero-width joiner inside the word
]
HINGLISH = ["aapki maut jaldi", "bimari", "dawai band karo", "satta khelo", "mukadma"]
CLEAN = [
    "Finance",
    "Banking",
    "Medicine",
    "a career in design",
    "you love deeply",
    "A long life line shows steady energy.",
    "Your short life line is clear and deep.",
    "You diagnose problems quickly.",
    "You conceive bold ideas.",
    "a die-hard romantic",
    "आप दिल से सोचते हैं",
    "आप मेहनती हैं",
    "कमरे में",
    "अपनी भावनाएँ शेयर करें",
]


@pytest.mark.parametrize("text", ENGLISH)
def test_english_redteam_hits(text):
    assert hits(text)


@pytest.mark.parametrize("text", HINDI)
def test_hindi_redteam_hits(text):
    assert hits(text)


@pytest.mark.parametrize("text", HINDI_VARIANTS)
def test_hindi_variants_hit(text):
    assert hits(text)


@pytest.mark.parametrize("text", HINGLISH)
def test_hinglish_hits(text):
    assert hits(text)


@pytest.mark.parametrize("text", CLEAN)
def test_innocuous_text_is_not_flagged(text):
    assert hits(text) == []


def test_hits_returns_rule_ids_not_text():
    ids = hits("You will die young and should invest in crypto.")
    assert len(ids) >= 2 and set(ids) <= {rule_id for rule_id, _ in RULES}


def llm_with(**fields) -> LlmReading:
    data = ok_reply().parsed_output.model_dump()
    lines = fields.pop("lines", {})
    for key, text in lines.items():
        data["lines"][key]["interpretation"] = text
    return LlmReading.model_validate(data | fields)


def test_filter_rewrites_every_hit_field():
    llm = llm_with(
        summary="Your palm warns of a serious illness.",
        traits=["curious", "you will die young", "warm"],
        ideal_partner="Someone who helps you through a lawsuit.",
        career_challenges="You should invest in shares before 30.",
        ideal_industries=["design", "gambling", "research"],
        lines={"heart": "Your heart line shows a heart attack risk."},
    )
    out, count = filter_llm(llm, "en")
    en = NEUTRAL["en"]
    assert count == 6
    assert (out.summary, out.ideal_partner, out.career_challenges) == (
        en["summary"],
        en["ideal_partner"],
        en["career_challenges"],
    )
    assert out.lines.heart.interpretation == en["interpretation"]
    assert out.traits == ["curious", "warm", "thoughtful"]
    assert out.ideal_industries == ["design", "research", "education"]
    assert out.how_you_love == llm.how_you_love
    assert out.lines.head == llm.lines.head
    assert out.lines.heart.points == llm.lines.heart.points


def test_filter_is_idempotent():
    llm = llm_with(summary="मृत्यु का योग", traits=["रोग", "शांत"], lines={"life": "दीर्घायु"})
    once, count = filter_llm(llm, "hi")
    assert count == 3
    twice, again = filter_llm(once, "hi")
    assert (twice, again) == (once, 0)


@pytest.mark.parametrize("locale", ["en", "hi"])
def test_neutral_text_has_no_hits(locale):
    for value in NEUTRAL[locale].values():
        for text in value if isinstance(value, list) else [value]:
            assert hits(text) == [], text
    assert set(NEUTRAL[locale]) == set(NEUTRAL["en"])


def strings(node):
    if isinstance(node, str):
        yield node
    elif isinstance(node, dict):
        for v in node.values():
            yield from strings(v)
    elif isinstance(node, list):
        for v in node:
            yield from strings(v)


@pytest.mark.parametrize("locale", ["en", "hi"])
def test_redteam_through_job_api(locale):
    bad = ENGLISH + HINDI + HINDI_VARIANTS + HINGLISH
    reply = ok_reply()
    reply.parsed_output = llm_with(
        summary=bad[0],
        traits=bad,  # every red-team string passes through the API once
        how_you_love=bad[4],
        ideal_partner=bad[12],
        love_challenges=bad[13],
        strengths=bad[24],
        career_challenges=bad[36],
        ideal_industries=bad[::-1],
        lines={k: bad[5 + i * 4] for i, k in enumerate(("heart", "head", "life", "fate", "sun"))},
    )
    with client_factory(FakeSdk([reply])) as c:
        body = poll(c, post_job(c, locale=locale).json()["job_id"])
    assert body["status"] == "done"
    assert body["reading"]["verdict"]["ok"] is True
    found = {s: hits(s) for s in strings(body["reading"]) if hits(s)}
    assert found == {}
