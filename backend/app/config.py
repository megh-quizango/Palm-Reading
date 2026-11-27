from pathlib import Path

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

BACKEND_DIR = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    # Absolute path: the server finds backend/.env whichever directory it starts from (Pitfall 9)
    model_config = SettingsConfigDict(env_file=BACKEND_DIR / ".env", extra="ignore")
    anthropic_api_key: SecretStr | None = None
    palm_model: str = "claude-sonnet-5-5"  # env PALM_MODEL (D-06)
    job_deadline_s: float = 45.0  # whole-job deadline (D-03, R-PALM-5)
    claude_concurrency: int = 4  # concurrent model calls (D-03)
