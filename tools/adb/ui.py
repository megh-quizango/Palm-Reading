# Dump the current screen's UI tree as compact lines: [idx] text/desc  (cx,cy) flags
import os, subprocess, sys, re, xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
S = os.environ["ADB_SERIAL"]
def adb(*a): return subprocess.run(["adb", "-s", S, *a], capture_output=True, text=True, encoding="utf-8", errors="replace").stdout
adb("shell", "rm", "-f", "/sdcard/ui.xml")  # a failed dump ("could not get idle state") must not reuse the last screen
adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
xml = adb("shell", "cat", "/sdcard/ui.xml")
xml = xml[xml.find("<?xml"):]
root = ET.fromstring(xml)
act = adb("shell", "dumpsys", "activity", "activities")
m = re.search(r"mResumedActivity: \S+ \S+ (\S+)", act) or re.search(r"topResumedActivity=\S+ \S+ (\S+)", act)
print("activity:", m.group(1) if m else "?")
for i, n in enumerate(root.iter("node")):
    t = (n.get("text") or "").strip(); d = (n.get("content-desc") or "").strip()
    c = n.get("clickable") == "true"; sc = n.get("scrollable") == "true"
    if not (t or d or c or sc): continue
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", n.get("bounds")))
    flags = ("C" if c else "") + ("S" if sc else "") + ("E" if n.get("class","").endswith("EditText") else "")
    label = " | ".join(x for x in (t, d) if x).replace("\n", " ")[:140]
    print(f"{label or '-'}  ({(x1+x2)//2},{(y1+y2)//2}) {flags}")
