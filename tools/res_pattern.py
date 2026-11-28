# res_pattern.py XML KEY... : print each Android string resource as a literal ERE (one line per key, argument order)
# for tools/ui.sh expect. Android escapes are undone the way the screen shows them (ui.py folds newlines into spaces).
import re, sys, xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding="utf-8")  # -I ignores PYTHONUTF8 and a Windows pipe defaults to cp1252
if len(sys.argv) < 3:
    sys.exit("usage: res_pattern.py XML KEY...")
strings = {n.get("name"): "".join(n.itertext()) for n in ET.parse(sys.argv[1]).getroot().iter("string")}
UNESCAPE = {"n": " ", "t": " "}  # any other escaped character (' " @ ? \) stands for itself
for key in sys.argv[2:]:
    if key not in strings:
        sys.exit(f"missing key: {key} in {sys.argv[1]}")
    value = re.sub(r"\\(.)", lambda m: UNESCAPE.get(m.group(1), m.group(1)), strings[key])
    # ERE metacharacters only: re.escape also escapes characters like - and #, which GNU grep warns about as stray
    print(re.sub(r"([.\[\]()*+?{}|^$\\])", r"\\\1", value))
