import pytest
from fastapi.testclient import TestClient

from app.config import BACKEND_DIR, Settings
from app.main import create_app


@pytest.fixture(autouse=True)
def no_os_key(monkeypatch):
    # Settings also reads OS env vars; a developer's shell key must not flip "missing" cases
    monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)


def healthz(settings: Settings):
    r = TestClient(create_app(settings)).get("/healthz")
    assert r.status_code == 200
    return r


@pytest.mark.parametrize("key", [None, "", "   "])
def test_absent_empty_or_blank_key_is_missing(key):
    assert healthz(Settings(_env_file=None, anthropic_api_key=key)).json() == {
        "status": "ok",
        "anthropic_key": "missing",
    }


def test_configured_key_is_never_echoed():
    r = healthz(Settings(_env_file=None, anthropic_api_key="sk-test-123"))
    assert r.json() == {"status": "ok", "anthropic_key": "configured"}
    assert set(r.json()) == {"status", "anthropic_key"}
    assert "sk-test-123" not in r.text


def test_key_read_from_env_file(tmp_path):
    env = tmp_path / ".env"
    env.write_text("ANTHROPIC_API_KEY=sk-file-1\n", encoding="utf-8")
    r = healthz(Settings(_env_file=env))
    assert r.json()["anthropic_key"] == "configured"
    assert "sk-file-1" not in r.text


def test_env_file_path_is_absolute():
    env_file = Settings.model_config["env_file"]
    assert env_file.is_absolute()
    assert env_file == BACKEND_DIR / ".env"
