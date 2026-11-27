"""D-10 rule tests for line_geometry. Curves are built in the hand-local (s, t) frame so they
do not depend on hand-picked pixels."""

import math

import pytest
from conftest import LANDMARKS, LANDMARKS_LEFT

from app.services import line_geometry as geo

W, H = 960, 1280
PALM = geo.palm_from_landmarks(LANDMARKS, W, H)
# Head-like curve across the palm, about 0.7 L long, every point inside the hull
CURVE = [(0.88, -0.18), (0.75, -0.05), (0.62, 0.05), (0.50, 0.12), (0.38, 0.12), (0.28, 0.08)]


def px(st):
    return [PALM.world(s, t) for s, t in st]


def length_l(st):
    return sum(math.dist(a, b) for a, b in zip(st, st[1:]))


def test_valid_curve_passes():
    assert 0.6 < length_l(CURVE) < 0.9
    assert geo.validate(px(CURVE), PALM) == "pass"


@pytest.mark.parametrize("n", [3, 25])
def test_point_count_out_of_range(n):
    st = [(0.3 + 0.5 * i / (n - 1), 0.0) for i in range(n)]
    assert geo.validate(px(st), PALM) == "count"


def test_consecutive_duplicates_dropped_before_count():
    five = px(CURVE[:5])
    pts = [five[0], five[0], five[1], five[2], five[2], five[3], five[4], five[4]]
    assert geo.validate(pts, PALM) == "pass"


def test_too_many_points_outside_hull():
    st = CURVE[:4] + [(0.6, 1.5), (0.7, 1.6)]  # 4 of 6 inside: 67%
    assert geo.validate(px(st), PALM) == "hull"


def test_too_short():
    st = [(0.6, -0.05), (0.6, 0.0), (0.6, 0.05), (0.6, 0.09)]
    assert length_l(st) < geo.MIN_LEN
    assert geo.validate(px(st), PALM) == "length"


def test_too_long():
    st = [(0.3, -0.2), (0.3, 0.1), (0.5, -0.2), (0.5, 0.1), (0.7, -0.2), (0.7, 0.2), (0.85, -0.2), (0.85, 0.2)]
    assert length_l(st) > geo.MAX_LEN
    assert geo.validate(px(st), PALM) == "length"


def test_one_fold_passes():
    st = [(0.6, -0.2), (0.6, 0.0), (0.6, 0.15), (0.61, -0.05), (0.62, -0.15)]
    assert geo.validate(px(st), PALM) == "pass"


def test_two_folds_rejected():
    st = [(0.6, -0.2), (0.6, 0.1), (0.61, -0.1), (0.62, 0.1)]
    assert geo.validate(px(st), PALM) == "folds"


def test_pair_heart_above_head():
    heart, head = geo.template("heart", PALM), geo.template("head", PALM)
    assert geo.pair_ok(heart, head, PALM)
    assert not geo.pair_ok(head, heart, PALM)


def test_degenerate_landmarks():
    assert geo.palm_from_landmarks([[0.5, 0.5, 0.0]] * 21, W, H) is None
    nan = [list(p) for p in LANDMARKS]
    nan[7][0] = float("nan")
    assert geo.palm_from_landmarks(nan, W, H) is None


@pytest.mark.parametrize("landmarks", [LANDMARKS, LANDMARKS_LEFT], ids=["right", "left"])
@pytest.mark.parametrize("kind", list(geo.TEMPLATES))
def test_templates_inside_hull(kind, landmarks):
    palm = geo.palm_from_landmarks(landmarks, W, H)
    pts = geo.template(kind, palm)
    assert len(pts) >= 2
    assert all(geo.inside(palm.hull, p) for p in pts)


@pytest.mark.parametrize("kind", list(geo.TEMPLATES))
def test_left_template_mirrors_right(kind):
    left = geo.template(kind, geo.palm_from_landmarks(LANDMARKS_LEFT, W, H))
    right = geo.template(kind, PALM)
    for (lx, ly), (rx, ry) in zip(left, right, strict=True):
        assert lx == pytest.approx(W - rx, abs=1e-6)
        assert ly == pytest.approx(ry, abs=1e-6)
