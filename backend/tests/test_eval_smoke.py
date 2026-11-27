"""Fake-mode smoke tests of the eval harness (D-15). Every run_eval.main call injects
settings() so no test reads backend/.env or can build a real client (D-16)."""

import csv
import json

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
