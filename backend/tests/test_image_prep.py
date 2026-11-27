import io
import os

import pytest
from PIL import Image

from app.services.image_prep import MAX_PIXELS, BadImage, prepare
from conftest import make_jpeg


def encode(img: Image.Image, fmt: str) -> bytes:
    out = io.BytesIO()
    img.save(out, fmt)
    return out.getvalue()


def test_exif_and_gps_are_stripped():
    raw = make_jpeg(exif=True)
    assert Image.open(io.BytesIO(raw)).getexif().get_ifd(0x8825)  # fixture really carries GPS
    p = prepare(raw)
    out = Image.open(io.BytesIO(p.jpeg))
    assert out.format == "JPEG"
    assert not out.getexif()
    assert (p.w, p.h) == out.size


def test_long_edge_capped_aspect_kept():
    p = prepare(make_jpeg(size=(3000, 2000)))
    assert p.w == 1568
    assert abs(p.w / p.h - 1.5) < 0.01


def test_small_photo_is_never_upscaled():
    p = prepare(make_jpeg(size=(600, 800)))
    assert (p.w, p.h) == (600, 800)


@pytest.mark.parametrize("fmt", ["PNG", "WEBP"])
def test_png_and_webp_become_jpeg(fmt):
    p = prepare(encode(Image.new("RGB", (640, 480), (200, 160, 130)), fmt))
    assert Image.open(io.BytesIO(p.jpeg)).format == "JPEG"
    assert (p.w, p.h) == (640, 480)


@pytest.mark.parametrize(
    "raw",
    [
        encode(Image.new("RGB", (640, 480)), "GIF"),
        os.urandom(4096),
        make_jpeg()[: len(make_jpeg()) // 2],
        b"",
    ],
    ids=["gif", "random", "truncated", "empty"],
)
def test_non_images_are_rejected(raw):
    with pytest.raises(BadImage):
        prepare(raw)


def test_too_many_pixels_rejected_before_decode():
    # 1-bit PNG keeps the fixture small; the cap must trip on the header, not after decoding
    raw = encode(Image.new("1", (8000, MAX_PIXELS // 8000 + 1)), "PNG")
    with pytest.raises(BadImage):
        prepare(raw)


def test_short_side_below_floor_rejected():
    with pytest.raises(BadImage):
        prepare(make_jpeg(size=(200, 400)))
