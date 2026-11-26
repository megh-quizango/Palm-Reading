from fastapi import FastAPI


def create_app() -> FastAPI:
    app = FastAPI(title="palm-backend")

    @app.get("/healthz")
    def healthz() -> dict:
        return {"status": "ok"}

    return app


app = create_app()
