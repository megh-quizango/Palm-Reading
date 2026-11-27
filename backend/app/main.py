import json
import math
import uuid
from contextlib import asynccontextmanager

import anthropic
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.datastructures import UploadFile
from starlette.exceptions import HTTPException
from starlette.formparsers import MultiPartParser

from app.config import Settings
from app.jobs import JobService
from app.services.claude_client import PalmModel
from app.services.image_prep import MAX_BYTES, BadImage, prepare
from app.services.palm_reader import PalmReader

MAX_BODY = MAX_BYTES + 64 * 1024  # image cap plus room for the text fields and boundaries
HANDS = frozenset({"left", "right"})
LOCALES = frozenset({"en", "hi"})

# Starlette spools uploads over 1 MB to a temp file; keep every accepted upload in memory (D-08)
MultiPartParser.spool_max_size = MAX_BODY


def error(status: int, code: str, headers: dict | None = None) -> JSONResponse:
    return JSONResponse({"error": {"code": code}}, status_code=status, headers=headers)


def _is_uuid(v: str | None) -> bool:
    try:
        uuid.UUID(v or "")
    except ValueError:
        return False
    return True


def _reject_constant(name: str):
    raise ValueError("non-finite constant")


def _parse_landmarks(s: str) -> list | None:
    """Exactly 21 [x, y, z] finite numbers with x and y in -0.5..1.5, else None."""
    try:
        pts = json.loads(s, parse_constant=_reject_constant)  # json accepts NaN/Infinity by default
    except ValueError:
        return None
    if not isinstance(pts, list) or len(pts) != 21:
        return None
    for p in pts:
        if not isinstance(p, list) or len(p) != 3:
            return None
        # bool is an int subclass; 1e400 parses to inf, so isfinite still matters
        if not all(isinstance(v, (int, float)) and not isinstance(v, bool) and math.isfinite(v) for v in p):
            return None
        if not (-0.5 <= p[0] <= 1.5 and -0.5 <= p[1] <= 1.5):
            return None
    return pts


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
        # Size gate before the body is read (Pitfall 1); error bodies never echo input (T-02-05)
        length = request.headers.get("Content-Length")
        if length is None:
            return error(411, "bad_request")
        if not length.isdigit() or int(length) > MAX_BODY:
            return error(413, "bad_request")
        if not _is_uuid(request.headers.get("X-Install-Id")):
            return error(422, "bad_request")
        jobs: JobService = request.app.state.jobs
        if jobs.reader is None:
            return error(503, "busy")
        try:
            form = await request.form(max_files=1, max_fields=3, max_part_size=64 * 1024)
        except HTTPException:  # Starlette maps every MultiPartException to a 400 "detail" body
            return error(422, "bad_request")
        try:
            image, hand = form.get("image"), form.get("hand_side")
            locale, landmarks_raw = form.get("locale"), form.get("landmarks")
            # Starlette's UploadFile: the parser builds the base class, not FastAPI's subclass
            if not isinstance(image, UploadFile) or not all(
                isinstance(v, str) for v in (hand, locale, landmarks_raw)
            ):
                return error(422, "bad_request")
            raw = await image.read()
        finally:
            await form.close()
        landmarks = _parse_landmarks(landmarks_raw)
        if len(raw) > MAX_BYTES or hand not in HANDS or locale not in LOCALES or landmarks is None:
            return error(422, "bad_request")
        try:
            prepared = prepare(raw)
        except BadImage:
            return error(422, "bad_request")
        del raw
        job = jobs.submit(prepared, hand, locale, landmarks)
        return JSONResponse({"job_id": job.id}, status_code=202)

    @app.get("/v1/palm/jobs/{job_id}")
    def get_job(job_id: str, request: Request):
        job = request.app.state.jobs.get(job_id)
        return job.view() if job else error(404, "not_found")

    return app


app = create_app()
