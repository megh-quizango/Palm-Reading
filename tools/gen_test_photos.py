"""Generate the instrumented test photos for PhotoProcessorTest (D-16).

Run with backend/.venv/Scripts/python.exe -I tools/gen_test_photos.py [--check]
(Pillow 13 from the backend venv; nothing to install).
--check verifies the committed files and exits 1 naming the first problem.

orient6_gps.jpg  1200x900 stored, EXIF Orientation 6 + GPS + Make + DateTimeOriginal;
                 once the orientation is applied: red block top-left, green block top-right
huge_100mp.jpg   10000x10000 four-colour blocks, quality 40
no_hand.jpg      1280x960 gradient with a few shapes, no hand
bomb_256mp.png   16000x16000 mode L solid (256 MP, tiny after deflate)
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps

Image.MAX_IMAGE_PIXELS = None  # the bomb is the point; Pillow's own guard would refuse it

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "android/app/src/androidTest/assets/photos"
MAX_BYTES = 3_000_000
BLOCK = 100
RED = (255, 0, 0)
GREEN = (0, 255, 0)
GREY = (128, 128, 128)

ORIENTATION, MAKE, EXIF_IFD, GPS_IFD, DATETIME_ORIGINAL = 0x0112, 0x010F, 0x8769, 0x8825, 0x9003


def orient6():
    shown = Image.new("RGB", (900, 1200), GREY)
    d = ImageDraw.Draw(shown)
    d.rectangle([0, 0, BLOCK - 1, BLOCK - 1], fill=RED)
    d.rectangle([900 - BLOCK, 0, 899, BLOCK - 1], fill=GREEN)
    # Orientation 6 = rotate 90 degrees clockwise to display, so store it rotated 90 counter-clockwise
    stored = shown.transpose(Image.Transpose.ROTATE_90)
    exif = Image.Exif()
    exif[ORIENTATION] = 6
    exif[MAKE] = "PalmTestCam"
    exif.get_ifd(EXIF_IFD)[DATETIME_ORIGINAL] = "2026:01:02 03:04:05"
    gps = exif.get_ifd(GPS_IFD)
    gps[1] = "N"
    gps[2] = (12.0, 58.0, 30.0)
    gps[3] = "E"
    gps[4] = (77.0, 35.0, 15.0)
    return stored, {"exif": exif.tobytes(), "quality": 90}


def huge():
    img = Image.new("RGB", (10000, 10000))
    d = ImageDraw.Draw(img)
    for i, c in enumerate([RED, GREEN, (0, 0, 255), (255, 255, 0)]):
        x, y = (i % 2) * 5000, (i // 2) * 5000
        d.rectangle([x, y, x + 4999, y + 4999], fill=c)
    return img, {"quality": 40}


def no_hand():
    img = Image.linear_gradient("L").resize((1280, 960)).convert("RGB")
    d = ImageDraw.Draw(img)
    d.ellipse([100, 100, 400, 400], fill=(40, 80, 160))
    d.ellipse([900, 500, 1150, 750], fill=(200, 160, 40))
    d.rectangle([500, 200, 800, 350], fill=(30, 140, 90))
    d.rectangle([300, 600, 700, 850], fill=(170, 60, 60))
    return img, {"quality": 85}


def bomb():
    return Image.new("L", (16000, 16000), 77), {"optimize": True}


PHOTOS = {
    "orient6_gps.jpg": (orient6, (1200, 900)),
    "huge_100mp.jpg": (huge, (10000, 10000)),
    "no_hand.jpg": (no_hand, (1280, 960)),
    "bomb_256mp.png": (bomb, (16000, 16000)),
}


def near(px, want):
    return all(abs(a - b) < 40 for a, b in zip(px, want))


def check():
    for name, (_, size) in PHOTOS.items():
        path = OUT / name
        if not path.is_file():
            return f"{name}: missing"
        if path.stat().st_size > MAX_BYTES:
            return f"{name}: {path.stat().st_size} bytes > {MAX_BYTES}"
        with Image.open(path) as img:
            if img.size != size:
                return f"{name}: stored size {img.size}, want {size}"
            if name == "orient6_gps.jpg":
                exif = img.getexif()
                if exif.get(ORIENTATION) != 6 or not exif.get(MAKE):
                    return f"{name}: Orientation 6 or Make missing"
                if not exif.get_ifd(EXIF_IFD).get(DATETIME_ORIGINAL):
                    return f"{name}: DateTimeOriginal missing"
                gps = exif.get_ifd(GPS_IFD)
                if not all(k in gps for k in (1, 2, 3, 4)):
                    return f"{name}: GPS latitude/longitude missing"
                shown = ImageOps.exif_transpose(img).convert("RGB")
                w, _ = shown.size
                if not near(shown.getpixel((10, 10)), RED) or not near(shown.getpixel((w - 11, 10)), GREEN):
                    return f"{name}: red/green corners not top-left/top-right after the orientation"
    return None


def main():
    if "--check" in sys.argv[1:]:
        problem = check()
        if problem:
            print(f"gen_test_photos: {problem}", file=sys.stderr)
            return 1
        print(f"photos OK: {len(PHOTOS)} files")
        return 0
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (make, _) in PHOTOS.items():
        img, kwargs = make()
        img.save(OUT / name, **kwargs)
        print(f"wrote {name}: {(OUT / name).stat().st_size} bytes")
    return 0


if __name__ == "__main__":
    sys.exit(main())
