"""In-memory async job runner (D-03): one asyncio task per job, no queue library."""

import asyncio
import logging
import time
import uuid
from dataclasses import dataclass
from typing import Any

from pydantic import ValidationError

from app.config import Settings
from app.schemas.palm import PalmReading
from app.services import claude_client
from app.services.claude_client import Malformed, ModelFailure
from app.services import image_prep as ip
from app.services.image_prep import Prepared, hamming
from app.services.palm_reader import PalmReader, invalid_reading

log = logging.getLogger("palm.jobs")

# Strong references so running tasks are not garbage-collected (D-03)
TASKS: set[asyncio.Task] = set()
now = time.monotonic  # tests patch jobs.now
JOB_TTL_S = 600  # jobs are swept lazily this long after creation (D-03)
RETRY_MIN_LEFT_S = 15.0  # a malformed reply is retried only with this much deadline left (D-05)
DHASH_MAX_DISTANCE = 6  # of 64 bits; a re-capture within this distance is a cache hit (D-12, eval-tuned)
CACHE_TTL_S = 86_400
CACHE_MAX = 2000


@dataclass
class Job:
    id: str
    status: str  # queued | running | done | failed
    hand: str
    locale: str
    created: float
    reading: PalmReading | None = None
    error: str | None = None

    def view(self) -> dict:
        return {
            "job_id": self.id,
            "status": self.status,
            "reading": self.reading.model_dump(mode="json") if self.reading else None,
            "error": {"code": self.error} if self.error else None,
        }


class JobService:
    def __init__(self, settings: Settings, reader: PalmReader | None):
        self.settings = settings
        self.reader = reader
        self.jobs: dict[str, Job] = {}
        # Both keys carry the install id: one install never receives another's job (D-02, T-02-13)
        self.by_idem: dict[tuple[str, str], str] = {}
        self.inflight: dict[tuple[str, str, str, str], str] = {}  # (install, sha256, hand, locale)
        self.sem = asyncio.Semaphore(settings.claude_concurrency)
        # (install, hand, locale, prompt version, sha256) -> (created, dhash, reading JSON) only (D-12)
        self.cache: dict[tuple[str, str, str, str, str], tuple[float, int, str]] = {}

    def _sweep(self) -> None:
        # Lazy, on every request; no background sweeper task (D-03)
        stale = now() - CACHE_TTL_S
        self.cache = {k: v for k, v in self.cache.items() if v[0] >= stale}
        cutoff = now() - JOB_TTL_S
        dead = {jid for jid, j in self.jobs.items() if j.created < cutoff}
        if not dead:
            return
        for jid in dead:
            del self.jobs[jid]
        self.by_idem = {k: v for k, v in self.by_idem.items() if v not in dead}
        self.inflight = {k: v for k, v in self.inflight.items() if v not in dead}

    def submit(
        self,
        prepared: Prepared,
        hand: str,
        locale: str,
        landmarks: Any,
        *,
        install_id: str,
        idem_key: str | None = None,
    ) -> Job:
        self._sweep()
        idem = (install_id, idem_key) if idem_key else None
        flight = (install_id, prepared.sha256, hand, locale)
        ckey = (install_id, hand, locale, claude_client.PROMPT_VERSION, prepared.sha256)
        job = self.jobs.get(self.by_idem.get(idem))
        if job is None and (cached := self._cached(ckey, prepared.dhash)):
            job = self._done(hand, locale, PalmReading.model_validate_json(cached))
        if job is None:
            job = self.jobs.get(self.inflight.get(flight))
        if job is None and (
            prepared.brightness < ip.PREGATE_MIN_MEAN
            or prepared.laplacian_var < ip.PREGATE_MIN_LAPLACIAN_VAR
        ):
            # Obviously dark or blurry: answered without a model call (D-09)
            reading = invalid_reading(
                "blurry_or_dark", w=prepared.w, h=prepared.h, hand=hand, locale=locale, model="pregate"
            )
            self._store(ckey, prepared.dhash, reading)
            job = self._done(hand, locale, reading)
        if job is None:
            job = Job(str(uuid.uuid4()), "queued", hand, locale, now())
            self.jobs[job.id] = job
            self.inflight[flight] = job.id
            task = asyncio.create_task(
                self._run(job, prepared, landmarks, flight, ckey)
            )
            TASKS.add(task)
            task.add_done_callback(TASKS.discard)
        if idem:
            self.by_idem[idem] = job.id
        return job

    def _cached(self, ckey: tuple, dh: int) -> str | None:
        """Exact sha256 first, else the nearest same-prefix dHash within DHASH_MAX_DISTANCE."""
        hit = self.cache.get(ckey)
        if hit is None:
            near = [
                (hamming(v[1], dh), v) for k, v in self.cache.items() if k[:4] == ckey[:4]
            ]
            near = [n for n in near if n[0] <= DHASH_MAX_DISTANCE]
            hit = min(near, key=lambda n: n[0])[1] if near else None
        return hit[2] if hit else None

    def _store(self, ckey: tuple, dh: int, reading: PalmReading) -> None:
        self.cache.pop(ckey, None)
        self.cache[ckey] = (now(), dh, reading.model_dump_json())
        while len(self.cache) > CACHE_MAX:  # dicts keep insertion order: drop the oldest
            del self.cache[next(iter(self.cache))]

    def _done(self, hand: str, locale: str, reading: PalmReading) -> Job:
        job = Job(str(uuid.uuid4()), "done", hand, locale, now(), reading=reading)
        self.jobs[job.id] = job
        return job

    def get(self, job_id: str) -> Job | None:
        self._sweep()
        return self.jobs.get(job_id)

    async def _run(
        self, job: Job, prepared: Prepared, landmarks: Any, flight: tuple, ckey: tuple
    ) -> None:
        jpeg, w, h = prepared.jpeg, prepared.w, prepared.h
        loop = asyncio.get_running_loop()
        try:
            async with asyncio.timeout(self.settings.job_deadline_s) as deadline:
                async with self.sem:
                    job.status = "running"
                    for attempt in range(2):
                        try:
                            result = await self.reader.read(jpeg, w, h, job.hand, job.locale, landmarks)
                            break
                        # Malformed reply, or a reading the strict contract rejects: one identical retry
                        except (Malformed, ValidationError) as e:
                            if attempt:
                                raise ModelFailure("model_error") from e
                            if deadline.when() - loop.time() < RETRY_MIN_LEFT_S:
                                raise ModelFailure("timeout") from e
            job.reading, job.status = result.reading, "done"
            self._store(ckey, prepared.dhash, result.reading)
        except Exception as e:
            code = (
                e.code if isinstance(e, ModelFailure)
                else "timeout" if isinstance(e, TimeoutError)
                else "model_error"
            )
            # job id, code and class name only: exception text can carry input values (Pitfall 6)
            log.error("job_failed job_id=%s code=%s error=%s", job.id, code, type(e.__cause__ or e).__name__)
            job.status, job.error = "failed", code
        finally:
            self.inflight.pop(flight, None)  # single-flight covers queued and running jobs only
            del jpeg, landmarks, prepared  # drop the image reference as soon as the call returns (D-08)
