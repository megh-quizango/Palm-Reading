"""In-memory image prep (D-08): decode, orient, cap the long edge, re-encode (drops EXIF/GPS)."""

import hashlib
import io
from dataclasses import dataclass

from PIL import Image, ImageFilter, ImageOps, ImageStat

MAX_BYTES = 8 * 1024 * 1024
MAX_PIXELS = 40_000_000
LONG_EDGE = 1568
MIN_SHORT_SIDE = 256
# D-09 pre-gate: deliberately lax start values (A2); the eval calibrates them
PREGATE_MIN_MEAN = 35
PREGATE_MIN_LAPLACIAN_VAR = 15


class BadImage(ValueError):
    pass


@dataclass(frozen=True)
class Prepared:
    jpeg: bytes
    w: int
    h: int
    sha256: str
    dhash: int
    laplacian_var: float
    brightness: float


def dhash(gray: Image.Image, n: int = 8) -> int:
    """64-bit horizontal-gradient hash of the downscaled grey image (D-12)."""
    px = gray.resize((n + 1, n), Image.Resampling.LANCZOS).tobytes()
    bits = 0
    for r in range(n):
        row = px[r * (n + 1) : (r + 1) * (n + 1)]
        for c in range(n):
            bits = (bits << 1) | (row[c] > row[c + 1])
    return bits


def hamming(a: int, b: int) -> int:
    return (a ^ b).bit_count()


def laplacian_var(gray: Image.Image) -> float:
    small = gray.copy()
    small.thumbnail((512, 512))  # size-independent threshold
    kernel = ImageFilter.Kernel((3, 3), [0, 1, 0, 1, -4, 1, 0, 1, 0], scale=1, offset=128)
    lap = small.filter(kernel)
    # Pillow copies the 1 px border unfiltered; raw grey values there would inflate the variance
    return ImageStat.Stat(lap.crop((1, 1, lap.width - 1, lap.height - 1))).var[0]


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
    gray = img.convert("L")
    return Prepared(
        jpeg, img.width, img.height, hashlib.sha256(jpeg).hexdigest(),
        dhash(gray), laplacian_var(gray), ImageStat.Stat(gray).mean[0],
    )
