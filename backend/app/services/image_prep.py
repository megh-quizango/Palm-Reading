"""In-memory image prep (D-08): decode, orient, cap the long edge, re-encode (drops EXIF/GPS)."""

import hashlib
import io
from dataclasses import dataclass

from PIL import Image, ImageOps

MAX_BYTES = 8 * 1024 * 1024
MAX_PIXELS = 40_000_000
LONG_EDGE = 1568
MIN_SHORT_SIDE = 256


class BadImage(ValueError):
    pass


@dataclass(frozen=True)
class Prepared:
    jpeg: bytes
    w: int
    h: int
    sha256: str


def prepare(raw: bytes) -> Prepared:
    img = ImageOps.exif_transpose(Image.open(io.BytesIO(raw))).convert("RGB")
    img.thumbnail((LONG_EDGE, LONG_EDGE), Image.Resampling.LANCZOS)  # never upscales
    out = io.BytesIO()
    img.save(out, "JPEG", quality=85)  # fresh encode: no EXIF/GPS
    jpeg = out.getvalue()
    return Prepared(jpeg, img.width, img.height, hashlib.sha256(jpeg).hexdigest())
