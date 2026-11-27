"""In-memory async job runner (D-03): one asyncio task per job, no queue library."""

import asyncio
import logging
import time
import uuid
from dataclasses import dataclass
from typing import Any

from app.config import Settings
from app.schemas.palm import PalmReading
from app.services.image_prep import Prepared
from app.services.palm_reader import PalmReader

log = logging.getLogger("palm.jobs")

# Strong references so running tasks are not garbage-collected (D-03)
TASKS: set[asyncio.Task] = set()
now = time.monotonic  # tests patch jobs.now


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
        self.sem = asyncio.Semaphore(settings.claude_concurrency)

    def submit(self, prepared: Prepared, hand: str, locale: str, landmarks: Any) -> Job:
        job = Job(str(uuid.uuid4()), "queued", hand, locale, now())
        self.jobs[job.id] = job
        task = asyncio.create_task(self._run(job, prepared.jpeg, prepared.w, prepared.h, landmarks))
        TASKS.add(task)
        task.add_done_callback(TASKS.discard)
        return job

    def get(self, job_id: str) -> Job | None:
        return self.jobs.get(job_id)

    async def _run(self, job: Job, jpeg: bytes, w: int, h: int, landmarks: Any) -> None:
        try:
            async with asyncio.timeout(self.settings.job_deadline_s):
                async with self.sem:
                    job.status = "running"
                    result = await self.reader.read(jpeg, w, h, job.hand, job.locale, landmarks)
            job.reading, job.status = result.reading, "done"
        except Exception as e:  # failure-state mapping is plan 02-03
            # job id and class name only: exception text can carry input values (Pitfall 6)
            log.error("job_failed job_id=%s error=%s", job.id, type(e).__name__)
            job.status, job.error = "failed", "model_error"
        finally:
            del jpeg, landmarks  # drop the image reference as soon as the call returns (D-08)
