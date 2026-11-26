#!/usr/bin/env bash
# Bundled fonts (D-05, D-06): download the variable OFL fonts from one pinned google/fonts commit,
# instance them to static weights, subset them, and write res/font/*.ttf + assets/licenses/*-OFL.txt.
#   bash tools/fonts.sh [WORK_DIR]   regenerate (WORK_DIR must be new or empty, outside the repo)
#   bash tools/fonts.sh --verify     check the committed set (static, coverage, GSUB, licences)
# Downloads are untrusted data: they stay in WORK_DIR/src, nothing there is executed, and fontTools
# runs as `python -I` from another directory with the paths passed as arguments.
set -euo pipefail
FONTS_SHA=6ebe737e44fd64ee736bbc6feed6136a6dbfd222 # google/fonts main on 2026-11-25
FONTTOOLS=fonttools==4.68.0
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FONT_DIR="$ROOT/android/app/src/main/res/font"
LIC_DIR="$ROOT/android/app/src/main/assets/licenses"
LATIN="U+0000-024F,U+1E00-1EFF,U+2000-206F,U+20AC,U+20B9,U+2122,U+2212,U+FEFF,U+FFFD"
DEVA="U+0020-007E,U+0900-097F,U+1CD0-1CF9,U+200C-200D,U+20B9,U+25CC,U+A830-A839,U+A8E0-A8FF"

py() { (cd "$ROOT/tools" && uv run -q --no-project --python 3.13 --with "$FONTTOOLS" python -I "$@"); }

if [ "${1:-}" = "--verify" ]; then
  py - "$FONT_DIR" "$LIC_DIR" <<'PY'
import sys
from pathlib import Path
from fontTools.ttLib import TTFont
fonts, lics = Path(sys.argv[1]), Path(sys.argv[2])
latin = ["cormorant_garamond_semibold"] + [f"manrope_{w}" for w in ("regular", "medium", "semibold", "bold")]
deva = [f"noto_sans_devanagari_{w}" for w in ("regular", "medium", "semibold", "bold")] + ["noto_serif_devanagari_semibold"]
errors = []
found = sorted(p.stem for p in fonts.glob("*.ttf")) if fonts.is_dir() else []
if found != sorted(latin + deva):
    errors.append(f"font set mismatch: missing {sorted(set(latin + deva) - set(found))}, extra {sorted(set(found) - set(latin + deva))}")
for name in latin + deva:
    path = fonts / f"{name}.ttf"
    if not path.exists():
        continue
    f = TTFont(path)
    cmap = f.getBestCmap()
    if "fvar" in f:
        errors.append(f"{name}: still has an fvar table (not static)")
    if name in latin:
        for cp in (0x41, 0x100):
            if cp not in cmap:
                errors.append(f"{name}: missing glyph U+{cp:04X}")
        if 0x915 in cmap:
            errors.append(f"{name}: unexpected Devanagari glyph U+0915 (subset too wide)")
    else:
        for cp in (0x915, 0x94D):
            if cp not in cmap:
                errors.append(f"{name}: missing glyph U+{cp:04X}")
        if "GSUB" not in f:
            errors.append(f"{name}: missing GSUB (Devanagari shaping)")
for fam in ("CormorantGaramond", "Manrope", "NotoSansDevanagari", "NotoSerifDevanagari"):
    lic = lics / f"{fam}-OFL.txt"
    if not lic.exists() or "open font license" not in lic.read_text(encoding="utf-8").lower():
        errors.append(f"{lic.name}: missing OFL text")
for e in errors:
    print("fonts --verify:", e, file=sys.stderr)
if errors:
    sys.exit(1)
print(f"fonts --verify OK: {len(found)} static fonts, 4 OFL texts")
PY
  exit
fi

WORK="${1:-$(mktemp -d)}"
mkdir -p "$WORK"
[ -z "$(ls -A "$WORK")" ] || { echo "work dir $WORK is not empty" >&2; exit 1; }
case "$(cd "$WORK" && pwd)/" in "$ROOT"/*) echo "work dir must be outside the repo" >&2; exit 1 ;; esac
mkdir "$WORK/src" "$WORK/build"
BASE="https://raw.githubusercontent.com/google/fonts/$FONTS_SHA/ofl"
get() { curl -fsSL --proto '=https' -o "$WORK/src/$2" "$BASE/$1"; }

# family dir | variable file (URL-encoded) | local name | licence name
get cormorantgaramond/CormorantGaramond%5Bwght%5D.ttf cormorant.ttf
get manrope/Manrope%5Bwght%5D.ttf manrope.ttf
get notosansdevanagari/NotoSansDevanagari%5Bwdth%2Cwght%5D.ttf notosans.ttf
get notoserifdevanagari/NotoSerifDevanagari%5Bwdth%2Cwght%5D.ttf notoserif.ttf
mkdir -p "$FONT_DIR" "$LIC_DIR"
for pair in cormorantgaramond:CormorantGaramond manrope:Manrope notosansdevanagari:NotoSansDevanagari notoserifdevanagari:NotoSerifDevanagari; do
  get "${pair%%:*}/OFL.txt" "${pair%%:*}-OFL.txt"
  cp "$WORK/src/${pair%%:*}-OFL.txt" "$LIC_DIR/${pair#*:}-OFL.txt"
done

build() { # src axes unicodes out
  local tmp="$WORK/build/$4.ttf"
  # shellcheck disable=SC2086
  py -m fontTools.varLib.instancer "$WORK/src/$1" $2 -o "$tmp"
  py -m fontTools.subset "$tmp" --output-file="$FONT_DIR/$4.ttf" --unicodes="$3" --layout-features='*' --name-IDs='*'
}
build cormorant.ttf "wght=600" "$LATIN" cormorant_garamond_semibold
for w in regular:400 medium:500 semibold:600 bold:700; do
  build manrope.ttf "wght=${w#*:}" "$LATIN" "manrope_${w%%:*}"
  build notosans.ttf "wght=${w#*:} wdth=100" "$DEVA" "noto_sans_devanagari_${w%%:*}"
done
build notoserif.ttf "wght=600 wdth=100" "$DEVA" noto_serif_devanagari_semibold
echo "fonts written; run: bash tools/fonts.sh --verify"
