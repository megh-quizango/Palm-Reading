"""Fake-mode smoke tests of the eval harness (D-15). Every run_eval.main call injects
settings() so no test reads backend/.env or can build a real client (D-16)."""

import csv
import json
import sys
from types import SimpleNamespace

import pytest

from app.services.claude_client import Malformed, ModelFailure, Refused, worst_case_cost
from evals import run_eval

from conftest import settings

SONNET, HAIKU = "claude-sonnet-5-5", "claude-haiku-5-5"
STEMS = ("zq_alpha", "zq_bravo", "zq_charlie")


def files(d, pattern):
    return sorted(d.glob(pattern))


def rows_of(out):
    rows = [r for f in files(out, "*.json") for r in json.loads(f.read_text(encoding="utf-8"))]
    return sorted(rows, key=lambda r: r["seq"])


def summary_of(out):
    return files(out, "*-summary.md")[0].read_text(encoding="utf-8")


def scripted(monkeypatch, outcomes):
    class Scripted(run_eval.FakeClient):
        def __init__(self):
            super().__init__(outcomes=list(outcomes))

    monkeypatch.setattr(run_eval, "FakeClient", Scripted)


def photo_dir(tmp_path):
    """A labelled photo set outside the repo (so the ignore gate passes) with landmarks."""
    d = tmp_path / "photos"
    d.mkdir()
    with open(d / "labels.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["file", "hand_side", "expected_ok", "expected_reason", "group", "notes"])
        for stem, (_, hand, group, jpeg, lm) in zip(STEMS, run_eval.synthetic_set()):
            (d / f"{stem}.jpg").write_bytes(jpeg)
            (d / f"{stem}.landmarks.json").write_text(json.dumps(lm), encoding="utf-8")
            ok = stem != "zq_charlie"
            w.writerow([f"{stem}.jpg", hand, str(ok).lower(), "" if ok else "jewelry", group, "x"])
    return d


def test_fake_synthetic_run_writes_rows_and_summary(tmp_path):
    assert run_eval.main(["--fake", "--synthetic", "--out", str(tmp_path)], settings=settings()) == 0
    rows = json.loads(files(tmp_path, "*-claude-sonnet-5-5.json")[0].read_text(encoding="utf-8"))
    assert len(rows) == 3 and all(r["outcome"] == "ok" for r in rows)
    text = files(tmp_path, "*-summary.md")[0].read_text(encoding="utf-8")
    assert "p50" in text and "p95" in text


def test_pct_is_nearest_rank():
    xs = list(range(20, 0, -1))
    assert run_eval.pct(xs, 50) == 10 and run_eval.pct(xs, 95) == 19


def test_full_summary_has_every_table_and_no_photo_ids(tmp_path):
    out = tmp_path / "out"
    argv = ["--fake", "--photos", str(photo_dir(tmp_path)), "--out", str(out),
            "--repeats", "3", "--locales", "en,hi"]
    assert run_eval.main(argv, settings=settings()) == 0
    text = summary_of(out)
    for col in ("calls", "timeout", "busy", "model_error", "p50 s", "p95 s", "mean out tok",
                "mean pts/call", "pts share", "mean $", "max $", "lines valid %",
                "template-first", "enum agree", "refusals", "suspected refusals", "malformed",
                "verdict agree", "cache-read share", "count", "hull", "length", "folds", "pair",
                "omitted", "dHash", "intra", "inter", "laplacian_var", "brightness",
                "would reject"):
        assert col in text, col
    assert f"| {SONNET} | en |" in text and f"| {SONNET} | hi |" in text
    assert "| 100.0 (100.0..100.0) | no |" in text
    for stem in STEMS:
        assert stem not in text
    assert "calm, curious" not in text and "steady, thoughtful" not in text
    assert len(rows_of(out)) == 18 and files(out, "*.csv")
    assert len(files(out / "overlays", "*.png")) == 3


def test_lines_valid_bootstraps_photos():
    assert run_eval.lines_valid([(5, 5)] * 6) == (100.0, 100.0, 100.0, "no")
    assert run_eval.lines_valid([(5, 5)] * 5 + [(0, 5)] * 2)[3] == "inconclusive"
    assert run_eval.lines_valid([(1, 5)] * 8)[3] == "yes"
    assert run_eval.lines_valid([]) is None


def test_enum_agreement_is_modal_share_averaged():
    def row(reason):
        enums = {"verdict_ok": True, "verdict_reason": reason}
        enums |= {f"{k}.{f}": "x" for k in run_eval.LINE_ORDER for f in run_eval.ENUM_FIELDS}
        return {"photo": "p", "enums": enums}

    n = 2 + len(run_eval.LINE_ORDER) * len(run_eval.ENUM_FIELDS)
    got = run_eval.agreement([row("none"), row("none"), row("bent")])
    assert got == pytest.approx((n - 1 + 2 / 3) / n)
    assert run_eval.agreement([row("none")]) is None


def test_timeout_row_keeps_wall_latency_and_counts(tmp_path, monkeypatch):
    scripted(monkeypatch, [ModelFailure("timeout")])
    argv = ["--fake", "--synthetic", "--concurrency", "1", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    rows = rows_of(tmp_path)
    assert rows[0]["outcome"] == "timeout" and rows[0]["latency_s"] >= 0
    m = run_eval.aggregate(rows)[(SONNET, "en")]
    assert m["calls"] == 3 and m["failures"]["timeout"] == 1


def test_malformed_refusal_and_failures_counted_separately(tmp_path, monkeypatch):
    scripted(monkeypatch, [Malformed(), Refused(), ModelFailure("busy"), ModelFailure("model_error"),
                           None, Malformed()])
    argv = ["--fake", "--synthetic", "--concurrency", "1", "--repeats", "2", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    m = run_eval.aggregate(rows_of(tmp_path))[(SONNET, "en")]
    assert (m["malformed"], m["refusals"]) == (2, 1)
    assert m["failures"] == {"timeout": 0, "busy": 1, "model_error": 1}
    assert m["suspected_refusals"] == 0  # the two malformed rows are different photos


def test_keyless_real_mode_exits_2_naming_env(tmp_path, capsys):
    assert run_eval.main(["--synthetic", "--out", str(tmp_path)], settings=settings()) == 2
    cap = capsys.readouterr()
    assert "backend/.env" in cap.out + cap.err and "sk-" not in cap.out + cap.err


def test_keyed_real_mode_reaches_the_conftest_guard(tmp_path):
    with pytest.raises(AssertionError, match="real Anthropic client"):
        run_eval.main(["--synthetic", "--out", str(tmp_path)],
                      settings=settings(anthropic_api_key="sk-test"))


def test_default_paths_are_anchored_on_the_package(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    base = __import__("pathlib").Path(run_eval.__file__).resolve().parent
    a = run_eval.parse_args([])
    assert a.photos == base / "photos" and a.out == base / "results"
    seen = []
    monkeypatch.setattr(run_eval, "ignored", lambda p: seen.append(p) or True)
    assert run_eval.main([], settings=settings()) == 2
    assert seen == [base / "photos", base / "results"]


def test_real_ignore_gate():
    assert run_eval.ignored(run_eval.EVALS / "photos")
    assert run_eval.ignored(run_eval.EVALS / "results")
    assert not run_eval.ignored(run_eval.REPO / "backend" / "app")
    assert run_eval.ignored(run_eval.REPO.parent / "elsewhere")


def test_unignored_photos_dir_exits_2_before_reading(tmp_path, monkeypatch):
    photos = tmp_path / "missing"  # reading it would raise
    monkeypatch.setattr(run_eval, "ignored", lambda p: p != photos.resolve())
    argv = ["--fake", "--photos", str(photos), "--out", str(tmp_path / "out")]
    assert run_eval.main(argv, settings=settings()) == 2
    assert not (tmp_path / "out").exists()


def test_budget_stop_after_first_call_over_budget(tmp_path, capsys):
    argv = ["--fake", "--synthetic", "--concurrency", "1", "--max-usd", "0.01", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    assert len(rows_of(tmp_path)) == 1 and "budget" in capsys.readouterr().out


def test_malformed_without_usage_charges_worst_case(tmp_path, monkeypatch):
    scripted(monkeypatch, [Malformed()])
    argv = ["--fake", "--synthetic", "--concurrency", "1", "--max-usd", "0.05", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    [row] = rows_of(tmp_path)
    assert row["cost"] == 0 and row["charge"] == pytest.approx(worst_case_cost(SONNET))


def test_schedule_alternates_models_photo_by_photo(tmp_path):
    argv = ["--fake", "--synthetic", "--concurrency", "1", "--limit", "2",
            "--models", f"{SONNET},{HAIKU}", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    order = [(r["photo"], r["model"]) for r in rows_of(tmp_path)]
    assert order == [("syn1", SONNET), ("syn1", HAIKU), ("syn2", SONNET), ("syn2", HAIKU)]


def test_make_landmarks_help_needs_no_mediapipe(monkeypatch):
    from evals import make_landmarks

    monkeypatch.setitem(sys.modules, "mediapipe", None)  # import would fail
    with pytest.raises(SystemExit) as e:
        make_landmarks.main(["--help"])
    assert e.value.code == 0


def test_make_landmarks_runs_image_mode_on_prepared_output(tmp_path, monkeypatch, capsys):
    from evals import make_landmarks

    seen = {"sizes": []}

    class Landmarker:
        @classmethod
        def create_from_options(cls, opts):
            seen["opts"] = opts
            return cls()

        def __enter__(self):
            return self

        def __exit__(self, *exc):
            return False

        def detect(self, img):
            seen["sizes"].append(img.data.size)
            pts = [SimpleNamespace(x=0.5, y=0.5, z=0.0)] * 21
            return SimpleNamespace(hand_landmarks=[pts] if len(seen["sizes"]) == 1 else [])

    vision = SimpleNamespace(
        HandLandmarker=Landmarker,
        HandLandmarkerOptions=lambda **kw: kw,
        RunningMode=SimpleNamespace(IMAGE="image"),
    )
    mp = SimpleNamespace(
        Image=lambda image_format, data: SimpleNamespace(data=data),
        ImageFormat=SimpleNamespace(SRGB="srgb"),
        tasks=SimpleNamespace(BaseOptions=lambda model_asset_path: model_asset_path, vision=vision),
    )
    monkeypatch.setitem(sys.modules, "mediapipe", mp)
    monkeypatch.setitem(sys.modules, "numpy", SimpleNamespace(asarray=lambda img: img))
    lm = run_eval.SYN_LANDMARKS
    (tmp_path / "a.jpg").write_bytes(run_eval.synthetic_jpeg(lm, size=(1700, 2200)))
    (tmp_path / "b.jpg").write_bytes(run_eval.synthetic_jpeg(lm))
    argv = ["--photos", str(tmp_path), "--model", str(tmp_path / "m.task")]
    assert make_landmarks.main(argv) == 0
    assert seen["opts"]["running_mode"] == "image" and seen["opts"]["num_hands"] == 1
    assert max(seen["sizes"][0]) == 1568  # prepare() output, not the raw 2200 px file
    assert len(json.loads((tmp_path / "a.landmarks.json").read_text(encoding="utf-8"))) == 21
    assert json.loads((tmp_path / "b.landmarks.json").read_text(encoding="utf-8")) is None
    assert capsys.readouterr().out.strip() == "2 photos processed, 1 with a hand"


# ---------- image red-team (D-13) ----------


def redteam_files(out):
    return [json.loads(f.read_text(encoding="utf-8")) for f in files(out / "redteam", "*.json")]


def test_redteam_render_changes_a_strip_in_memory(tmp_path, monkeypatch):
    from PIL import Image, ImageChops

    from evals import redteam

    monkeypatch.chdir(tmp_path)
    jpeg = run_eval.synthetic_jpeg(run_eval.SYN_LANDMARKS)
    for _, script, text in redteam.INJECTIONS[:3]:
        font = redteam.FONT_HI if script == "hi" else None
        out = redteam.render(jpeg, text, font)
        a, b = (Image.open(__import__("io").BytesIO(x)).convert("L") for x in (jpeg, out))
        box = ImageChops.difference(a, b).point(lambda v: 255 if v > 60 else 0).getbbox()
        assert box is not None and box[3] - box[1] < a.height / 2  # a strip, not the whole photo
    assert list(tmp_path.iterdir()) == []


def test_redteam_injections_cover_every_topic():
    from evals import redteam

    assert len(redteam.INJECTIONS) == 13
    by = {}
    for topic, script, _ in redteam.INJECTIONS:
        by.setdefault(topic, set()).add(script)
    assert by.pop("death") == {"en", "hinglish", "hi"}
    assert set(by) == {"illness", "pregnancy", "medical", "legal", "financial"}
    assert all(s == {"en", "hi"} for s in by.values())


def test_redteam_score_counts_after_hits_and_raw_echo():
    from conftest import ok_reply

    from evals import redteam

    inj = redteam.INJECTIONS[0][2]
    clean = ok_reply().parsed
    echoed = clean.model_copy(update={"summary": f"Calm. {inj.upper()}"})
    assert redteam.score(clean, clean, inj) == {"hits_after": 0, "echoed": False}
    got = redteam.score(echoed, echoed, inj)
    assert got["hits_after"] >= 1 and got["echoed"] is True


def test_redteam_fake_run_filters_every_echo_and_runs_first(tmp_path):
    argv = ["--fake", "--synthetic", "--redteam", "--concurrency", "1", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    rt = redteam_files(tmp_path)
    assert len(rt) == 2 * 13 * 2  # redteam-limit photos x injections x locales x 1 model
    assert sum(r["safety_hits"] for r in rt) > 0
    assert sum(r["hits_after"] for r in rt) == 0
    assert sum(r["echoed"] for r in rt) == len(rt)
    assert all(r["raw"] and r["final"] for r in rt)
    assert max(r["seq"] for r in rt) < min(r["seq"] for r in rows_of(tmp_path))
    text = summary_of(tmp_path)
    assert "## Red-team" in text and "raqm" in text and "| financial | hi |" in text


@pytest.mark.parametrize(
    "outcomes, kind",
    [
        ([Refused()], "refusal"),
        ([Malformed()], "malformed"),
        ([ModelFailure("model_error")], "model_error"),
        ([ModelFailure("busy"), ModelFailure("busy")], "busy"),
        ([ModelFailure("timeout"), ModelFailure("timeout")], "timeout"),
    ],
)
def test_redteam_cell_without_reading_exits_1_after_full_run(tmp_path, monkeypatch, outcomes, kind):
    monkeypatch.setattr(run_eval, "RETRY_PAUSE", 0)
    scripted(monkeypatch, outcomes)
    argv = ["--fake", "--synthetic", "--redteam", "--concurrency", "1", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 1
    assert len(rows_of(tmp_path)) == 3 and len(redteam_files(tmp_path)) == 52  # all calls ran
    agg = run_eval.redteam_agg(redteam_files(tmp_path))
    assert agg[("death", "en")]["failed"][kind] == 1
    assert "## Red-team" in summary_of(tmp_path)


def test_redteam_busy_then_ok_is_retried_once(tmp_path, monkeypatch):
    monkeypatch.setattr(run_eval, "RETRY_PAUSE", 0)
    scripted(monkeypatch, [ModelFailure("busy")])
    argv = ["--fake", "--synthetic", "--redteam", "--concurrency", "1", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    first = min(redteam_files(tmp_path), key=lambda r: r["seq"])
    assert first["attempts"] == 2 and first["outcome"] == "ok"


def test_redteam_after_filter_hit_exits_1(tmp_path, monkeypatch):
    from app.services import palm_reader

    monkeypatch.setattr(palm_reader.safety, "filter_llm", lambda llm, locale: (llm, 0))
    argv = ["--fake", "--synthetic", "--redteam", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 1
    assert sum(r["hits_after"] for r in redteam_files(tmp_path)) > 0


def test_redteam_only_with_zero_repeats(tmp_path):
    argv = ["--fake", "--synthetic", "--redteam", "--repeats", "0", "--out", str(tmp_path)]
    assert run_eval.main(argv, settings=settings()) == 0
    assert files(tmp_path, "*.json") == [] and len(redteam_files(tmp_path)) == 52
    text = summary_of(tmp_path)
    assert "## Bake-off" not in text and "## Red-team" in text
