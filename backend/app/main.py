from fastapi import FastAPI

from app.config import Settings


def create_app(settings: Settings | None = None) -> FastAPI:
    s = settings or Settings()
    app = FastAPI(title="palm-backend")

    @app.get("/healthz")
    def healthz() -> dict:
        # Reports only whether a key is set; never the key, its length or prefix
        key = s.anthropic_api_key.get_secret_value() if s.anthropic_api_key else ""
        return {"status": "ok", "anthropic_key": "configured" if key.strip() else "missing"}

    return app


app = create_app()
