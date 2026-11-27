"""Palm geometry for D-10: checks model line traces against the hand landmarks and draws
landmark-anchored template lines. Pure Python; all geometry in pixels (Pitfall 3)."""

import math
from dataclasses import dataclass
from typing import Final

Pt = tuple[float, float]

PALM: Final = (0, 1, 2, 5, 9, 13, 17)  # wrist, thumb CMC/MCP, index/middle/ring/pinky MCP
HULL_EXPAND: Final = 1.15  # palm hull scaled about its centroid
MIN_INSIDE: Final = 0.8  # share of trace points that must sit inside the expanded hull
MIN_L_PX: Final = 20.0  # wrist to middle MCP below this: landmarks unusable

# (s, t): s along wrist -> middle MCP in units of L (1.0 = MCP row), t across (+ pinky side).
# Start -> end. Tuned from RESEARCH Pattern 7 so every point sits inside the landmark hull (A3).
TEMPLATES: Final = {
    "heart": [(0.80, 0.30), (0.82, 0.15), (0.84, 0.00), (0.86, -0.12), (0.90, -0.20)],
    "head": [(0.72, -0.26), (0.66, -0.15), (0.60, 0.00), (0.56, 0.10), (0.52, 0.18)],
    "life": [(0.72, -0.26), (0.58, -0.30), (0.42, -0.28), (0.26, -0.22), (0.12, -0.12)],
    "fate": [(0.10, 0.02), (0.32, 0.03), (0.56, 0.02), (0.78, 0.00)],
    "sun": [(0.46, 0.15), (0.56, 0.18), (0.66, 0.21), (0.76, 0.23)],
}


def _cross(o: Pt, a: Pt, b: Pt) -> float:
    return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])


def hull(pts: list[Pt]) -> list[Pt]:
    """Andrew monotone chain; counter-clockwise in coordinate terms."""
    pts = sorted(set(pts))

    def half(seq):
        out: list[Pt] = []
        for p in seq:
            while len(out) >= 2 and _cross(out[-2], out[-1], p) <= 0:
                out.pop()
            out.append(p)
        return out[:-1]

    return half(pts) + half(list(reversed(pts)))


def expand(poly: list[Pt], k: float = HULL_EXPAND) -> list[Pt]:
    cx = sum(p[0] for p in poly) / len(poly)
    cy = sum(p[1] for p in poly) / len(poly)
    return [(cx + k * (x - cx), cy + k * (y - cy)) for x, y in poly]


def inside(poly: tuple[Pt, ...], p: Pt) -> bool:
    n = len(poly)
    return all(_cross(poly[i], poly[(i + 1) % n], p) >= 0 for i in range(n))


@dataclass(frozen=True)
class Palm:
    """Landmark pixels, expanded palm hull and the hand-local frame (origin wrist)."""

    px: tuple[Pt, ...]
    hull: tuple[Pt, ...]
    origin: Pt
    u: Pt  # unit, wrist -> middle MCP
    v: Pt  # unit, across the palm toward the pinky side
    L: float  # wrist to middle MCP, px

    def local(self, p: Pt) -> Pt:
        dx, dy = p[0] - self.origin[0], p[1] - self.origin[1]
        return (
            (dx * self.u[0] + dy * self.u[1]) / self.L,
            (dx * self.v[0] + dy * self.v[1]) / self.L,
        )

    def world(self, s: float, t: float) -> Pt:
        (ox, oy), (ux, uy), (vx, vy) = self.origin, self.u, self.v
        return (ox + self.L * (s * ux + t * vx), oy + self.L * (s * uy + t * vy))


def palm_from_landmarks(landmarks, w: int, h: int) -> Palm | None:
    px = tuple((float(x) * w, float(y) * h) for x, y, _ in landmarks)
    if len(px) != 21 or not all(math.isfinite(c) for p in px for c in p):
        return None
    (wx, wy), (mx, my) = px[0], px[9]
    length = math.hypot(mx - wx, my - wy)
    if length < MIN_L_PX:
        return None
    ux, uy = (mx - wx) / length, (my - wy) / length
    vx, vy = -uy, ux
    # +t toward the pinky: this sign is what mirrors left hands with no handedness label
    if (px[17][0] - px[5][0]) * vx + (px[17][1] - px[5][1]) * vy < 0:
        vx, vy = -vx, -vy
    poly = hull([px[i] for i in PALM])
    if len(poly) < 3:
        return None
    return Palm(px, tuple(expand(poly)), (wx, wy), (ux, uy), (vx, vy), length)


def template(kind: str, palm: Palm) -> list[Pt]:
    return [palm.world(s, t) for s, t in TEMPLATES[kind]]


def validate(pts: list[Pt], palm: Palm) -> str:
    """Name of the first D-10 rule the trace fails, or "pass"."""
    if sum(inside(palm.hull, p) for p in pts) < MIN_INSIDE * len(pts):
        return "hull"
    return "pass"
