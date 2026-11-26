from pathlib import Path

from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict

BACKEND_DIR = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    # Absolute path: the server finds backend/.env whichever directory it starts from (Pitfall 9)
    model_config = SettingsConfigDict(env_file=BACKEND_DIR / ".env", extra="ignore")
    anthropic_api_key: SecretStr | None = None
