import json
from contextlib import asynccontextmanager

import anthropic
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.formparsers import MultiPartParser

from app.config import Settings
from app.jobs import JobService
from app.services.claude_client import PalmModel
from app.services.image_prep import MAX_BYTES, prepare
from app.services.palm_reader import PalmReader

# Starlette spools uploads over 1 MB to a temp file; keep every accepted upload in memory (D-08)
MultiPartParser.spool_max_size = MAX_BYTES + 64 * 1024


def error(status: int, code: str, headers: dict | None = None) -> JSONResponse:
    return JSONResponse({"error": {"code": code}}, status_code=status, headers=headers)


def create_app(settings: Settings | None = None, anthropic_client: object | None = None) -> FastAPI:
    s = settings or Settings()

    def key() -> str:
        return s.anthropic_api_key.get_secret_value().strip() if s.anthropic_api_key else ""

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        client, owned = anthropic_client, None
        if client is None and key():
            # Looked up on the module at call time so the test guard can intercept it.
            # Per-attempt timeout = job deadline, so the SDK never cuts and re-sends a live attempt.
            client = owned = anthropic.AsyncAnthropic(
                api_key=key(), timeout=s.job_deadline_s, max_retries=1
            )
        reader = PalmReader(PalmModel(client, s.palm_model)) if client is not None else None
        app.state.jobs = JobService(s, reader)
        try:
            yield
        finally:
            if owned is not None:
                await owned.close()

    app = FastAPI(title="palm-backend", lifespan=lifespan)

    @app.get("/healthz")
    def healthz() -> dict:
        # Reports only whether a key is set; never the key, its length or prefix
        return {"status": "ok", "anthropic_key": "configured" if key() else "missing"}

    @app.post("/v1/palm/jobs")
    async def create_job(request: Request):
        jobs: JobService = request.app.state.jobs
        if jobs.reader is None:
            return error(503, "busy")
        form = await request.form(max_files=1, max_fields=3, max_part_size=64 * 1024)
        try:
            image = form["image"]
            hand, locale = form["hand_side"], form["locale"]
            landmarks = json.loads(form["landmarks"])
            raw = await image.read()
        finally:
            await form.close()
        job = jobs.submit(prepare(raw), hand, locale, landmarks)
        return JSONResponse({"job_id": job.id}, status_code=202)

    @app.get("/v1/palm/jobs/{job_id}")
    def get_job(job_id: str, request: Request):
        job = request.app.state.jobs.get(job_id)
        return job.view() if job else error(404, "not_found")

    return app


app = create_app()
