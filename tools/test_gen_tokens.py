"""Unit tests for gen_tokens.py. Run: python -I tools/test_gen_tokens.py"""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))  # python -I leaves the script dir off sys.path
import gen_tokens as g  # noqa: E402


def gen(data):
    return g.generate(data)


def col(v):
    return {"color": {"bg": {"base": {"$value": v, "$type": "color"}}}}


class Conversions(unittest.TestCase):
    def test_hex6_gets_opaque_alpha(self):
        kt, xml = gen(col("#0B0820"))
        self.assertIn("val BgBase = Color(0xFF0B0820)", kt)
        self.assertIn('<color name="nocturne_bg_base">#FF0B0820</color>', xml)

    def test_hex8_moves_css_alpha_to_front(self):
        self.assertIn("Color(0x736A4CF5)", gen(col("#6A4CF573"))[0])

    def test_px_em_ms_and_bezier(self):
        data = {
            "dimension": {"$type": "dimension", "space": {"2": {"$value": "2px"}}, "radius": {"neg": {"$value": "-8px"}}},
            "duration": {"instant": {"$value": "100ms", "$type": "duration"}},
            "cubicBezier": {"standard": {"$value": [0.2, 0, 0, 1], "$type": "cubicBezier"}},
        }
        kt = gen(data)[0]
        self.assertIn("val S2 = 2.dp", kt)  # $type inherited from the group, digit key prefixed
        self.assertIn("val Neg = (-8).dp", kt)
        self.assertIn("const val Instant = 100", kt)
        self.assertIn("val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)", kt)

    def test_reference_resolves_to_constant(self):
        data = col("#0B0820")
        data["semantic"] = {"background": {"$value": "{color.bg.base}", "$type": "color"}}
        kt = gen(data)[0]
        self.assertIn("val Background = NocturneColors.BgBase", kt)
        self.assertEqual(kt.count("0xFF0B0820"), 1)


class RealTokens(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(g.TOKENS.read_text(encoding="utf-8"))
        cls.kt, cls.xml = gen(cls.data)

    def test_all_11_type_specs(self):
        for name in ("DisplayXl", "DisplayL", "DisplayM", "DisplayS", "TitleM", "BodyL", "BodyM",
                     "LabelL", "LabelM", "Caption", "Overline"):
            self.assertIn(f"val {name} = NocturneTypeSpec(", self.kt)
        self.assertIn("val Overline = NocturneTypeSpec(NocturneFontRole.Ui, 600, 13f, 18f, 0.12f)", self.kt)
        self.assertIn("val TitleM = NocturneTypeSpec(NocturneFontRole.Ui, 600, 15f, 22f, 0f)", self.kt)
        self.assertIn("val DisplayXl = NocturneTypeSpec(NocturneFontRole.Display, 600, 40f, 44f, 0f)", self.kt)
        self.assertIn('"Display XL" to DisplayXl,', self.kt)
        self.assertIn('"Overline" to Overline,', self.kt)

    def test_colors_xml_bg_base(self):
        self.assertIn('<color name="nocturne_bg_base">#FF0B0820</color>', self.xml)

    def test_deterministic(self):
        self.assertEqual(gen(self.data), gen(json.loads(g.TOKENS.read_text(encoding="utf-8"))))

    def test_by_path_map_covers_every_colour(self):
        self.assertIn('"bg.base" to BgBase,', self.kt)
        self.assertIn('"palm.sun" to PalmSun,', self.kt)
        self.assertEqual(self.kt.count('" to '), 27 + 11)  # colours + type names

    def test_header(self):
        self.assertTrue(self.kt.startswith(g.HEADER + "\n"))


class Failures(unittest.TestCase):
    def assertFails(self, data, *needles):
        with self.assertRaises(g.TokenError) as cm:
            gen(data)
        for n in needles:
            self.assertIn(n, str(cm.exception))

    def test_identifier_collision_names_both_paths(self):
        data = {"color": {"a": {"b-c": {"$value": "#000000", "$type": "color"}},
                          "a-b": {"c": {"$value": "#FFFFFF", "$type": "color"}}}}
        self.assertFails(data, "color.a.b-c", "color.a-b.c")

    def test_unresolvable_reference(self):
        data = {"semantic": {"primary": {"$value": "{color.nope}", "$type": "color"}}}
        self.assertFails(data, "semantic.primary", "color.nope")

    def test_missing_value(self):
        self.assertFails({"color": {"bg": {"base": {"$type": "color"}}}}, "color.bg.base", "missing $value")

    def test_empty_value(self):
        self.assertFails(col(""), "color.bg.base", "empty $value")

    def test_unknown_type(self):
        self.assertFails({"x": {"y": {"$value": "1", "$type": "blob"}}}, "x.y", "blob")


if __name__ == "__main__":
    unittest.main()
