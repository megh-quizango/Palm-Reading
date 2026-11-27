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
    try:
        # Header-only probe: Pillow's own bomb error only fires at twice its limit
        probe = Image.open(io.BytesIO(raw))
        if probe.format not in {"JPEG", "PNG", "WEBP"} or probe.width * probe.height > MAX_PIXELS:
            raise BadImage("unsupported format or too many pixels")
        probe.verify()  # invalidates the object: reopen for the real decode
        img = ImageOps.exif_transpose(Image.open(io.BytesIO(raw))).convert("RGB")
    except BadImage:
        raise
    except (OSError, SyntaxError, ValueError, Image.DecompressionBombError) as e:
        # UnidentifiedImageError is an OSError; truncated data surfaces as OSError on decode
        raise BadImage(type(e).__name__) from None
    if min(img.size) < MIN_SHORT_SIDE:
        raise BadImage("short side too small")
    img.thumbnail((LONG_EDGE, LONG_EDGE), Image.Resampling.LANCZOS)  # never upscales
    out = io.BytesIO()
    img.save(out, "JPEG", quality=85)  # fresh encode: no EXIF/GPS
    jpeg = out.getvalue()
    return Prepared(jpeg, img.width, img.height, hashlib.sha256(jpeg).hexdigest())
