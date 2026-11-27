"""Fake-mode smoke tests of the eval harness (D-15). Every run_eval.main call injects
settings() so no test reads backend/.env or can build a real client (D-16)."""

import json

from evals import run_eval

from conftest import settings


def files(d, pattern):
    return sorted(d.glob(pattern))


def test_fake_synthetic_run_writes_rows_and_summary(tmp_path):
    assert run_eval.main(["--fake", "--synthetic", "--out", str(tmp_path)], settings=settings()) == 0
    rows = json.loads(files(tmp_path, "*-claude-sonnet-5-5.json")[0].read_text(encoding="utf-8"))
    assert len(rows) == 3 and all(r["outcome"] == "ok" for r in rows)
    text = files(tmp_path, "*-summary.md")[0].read_text(encoding="utf-8")
    assert "p50" in text and "p95" in text
