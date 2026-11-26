"""Read raw phone screen pixels (stdlib). Needs ADB_SERIAL (source tools/_device.sh).

  python -I tools/screen.py pixel X Y      -> #RRGGBB
  python -I tools/screen.py hash Y0 Y1     -> sha256 of rows Y0..Y1 (inclusive)
  python -I tools/screen.py --self-test
"""
import hashlib
import os
import struct
import subprocess
import sys


def parse(raw):
    """Raw `screencap` (RGBA_8888): 12-byte header on API <= 30, 16 bytes (+ colour space) on 31+."""
    w, h = struct.unpack_from("<II", raw, 0)
    header = len(raw) - w * h * 4
    if header not in (12, 16):
        raise ValueError(f"unexpected screencap size {len(raw)} for {w}x{h}")
    return w, h, raw[header:]


def pixel(buf, x, y):
    w, h, px = buf
    if not (0 <= x < w and 0 <= y < h):
        raise ValueError(f"({x},{y}) outside {w}x{h}")
    r, g, b = px[(y * w + x) * 4:(y * w + x) * 4 + 3]
    return f"#{r:02X}{g:02X}{b:02X}"


def rows_hash(buf, y0, y1):
    w, h, px = buf
    if not (0 <= y0 <= y1 < h):
        raise ValueError(f"rows {y0}..{y1} outside height {h}")
    return hashlib.sha256(px[y0 * w * 4:(y1 + 1) * w * 4]).hexdigest()


def capture():
    serial = os.environ["ADB_SERIAL"]
    raw = subprocess.run(["adb", "-s", serial, "exec-out", "screencap"], capture_output=True, check=True).stdout
    return parse(raw)


def self_test():
    px = bytes([0x0B, 0x08, 0x20, 0xFF, 1, 2, 3, 0xFF, 4, 5, 6, 0xFF, 7, 8, 9, 0xFF])
    for header in (struct.pack("<III", 2, 2, 1), struct.pack("<IIII", 2, 2, 1, 1)):
        buf = parse(header + px)
        assert pixel(buf, 0, 0) == "#0B0820", pixel(buf, 0, 0)
        assert pixel(buf, 1, 1) == "#070809", pixel(buf, 1, 1)
        assert rows_hash(buf, 1, 1) == hashlib.sha256(px[8:]).hexdigest()
        assert rows_hash(buf, 0, 1) == hashlib.sha256(px).hexdigest()
    try:
        parse(struct.pack("<III", 2, 2, 1) + px[:-1])
        raise AssertionError("short buffer accepted")
    except ValueError:
        pass
    print("screen.py self-test OK")


def main(a):
    if a == ["--self-test"]:
        self_test()
    elif len(a) == 3 and a[0] == "pixel":
        print(pixel(capture(), int(a[1]), int(a[2])))
    elif len(a) == 3 and a[0] == "hash":
        print(rows_hash(capture(), int(a[1]), int(a[2])))
    else:
        print(__doc__, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
