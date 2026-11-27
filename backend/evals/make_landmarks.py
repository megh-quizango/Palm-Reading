"""Offline MediaPipe Hand Landmarker pass over the eval photo set (eval only, never the server).

Runs on prepare() output so the landmarks match the image the model sees (Pitfall 3). MediaPipe
lives only in an ephemeral env, never in pyproject.toml:
  uv run --directory backend --with mediapipe==1.2.3 python -m evals.make_landmarks
      --model evals/models/hand_landmarker.task   (one command line)
If mediapipe has no Python 3.13 wheel (A1), add `--python 3.12`, or supply landmarks JSON by hand.
"""

import argparse
import io
import json
import sys
import uuid
from pathlib import Path

from PIL import Image

from app.services.image_prep import prepare
from evals.run_eval import EVALS, ignored

SUFFIXES = (".jpg", ".jpeg", ".png", ".webp")


def mp_image(mp, np, jpeg: bytes):
    try:
        rgb = np.asarray(Image.open(io.BytesIO(jpeg)).convert("RGB"))
        return mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb)
    except (AttributeError, TypeError, ValueError):
        # A8 fallback: a git-ignored temp file for create_from_file, deleted right after
        tmp = EVALS / "results" / "tmp"
        tmp.mkdir(parents=True, exist_ok=True)
        f = tmp / f"{uuid.uuid4().hex}.jpg"
        f.write_bytes(jpeg)
        try:
            return mp.Image.create_from_file(str(f))
        finally:
            f.unlink(missing_ok=True)


def main(argv: list[str] | None = None) -> int:
    p = argparse.ArgumentParser(prog="python -m evals.make_landmarks")
    p.add_argument("--model", type=Path, default=EVALS / "models" / "hand_landmarker.task")
    p.add_argument("--photos", type=Path, default=EVALS / "photos")
    a = p.parse_args(argv)
    photos_dir = a.photos.resolve()
    if not ignored(photos_dir):
        print(f"refusing to run: {photos_dir} is not git-ignored", file=sys.stderr)
        return 2
    import mediapipe as mp  # deferred: --help works without the ephemeral env
    import numpy as np

    vision = mp.tasks.vision
    opts = vision.HandLandmarkerOptions(
        base_options=mp.tasks.BaseOptions(model_asset_path=str(a.model.resolve())),
        running_mode=vision.RunningMode.IMAGE,
        num_hands=1,
    )
    photos = sorted(f for f in photos_dir.iterdir() if f.suffix.lower() in SUFFIXES)
    found = 0
    with vision.HandLandmarker.create_from_options(opts) as lm:
        for f in photos:
            res = lm.detect(mp_image(mp, np, prepare(f.read_bytes()).jpeg))
            pts = [[q.x, q.y, q.z] for q in res.hand_landmarks[0]] if res.hand_landmarks else None
            found += pts is not None
            f.with_name(f"{f.stem}.landmarks.json").write_text(json.dumps(pts), encoding="utf-8")
    print(f"{len(photos)} photos processed, {found} with a hand")
    return 0


if __name__ == "__main__":
    sys.exit(main())
