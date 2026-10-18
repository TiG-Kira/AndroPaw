# gtp.py <id>: the picture check from outside. Starts the tester's "pattern" test in the running image and, at
# every stage it announces (view, gl1, gl2), takes a screenshot of the phone and looks for the 16 colours of
# the pattern, 4 x 4 cells in their order. Prints one verdict per stage; the screenshots stay as gtp-<id>-<stage>.png.
import os, subprocess, sys, time
from PIL import Image

W = os.environ.get("W", os.getcwd())
ADB = [os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")] + (["-s", os.environ["ANDROID_SERIAL"]] if os.environ.get("ANDROID_SERIAL") else [])
PATTERN = [0xff0000, 0x00ff00, 0x0000ff, 0xffff00, 0xff00ff, 0x00ffff, 0xffffff, 0x808080,
           0xff8000, 0x8000ff, 0x00ff80, 0x804000, 0x0080ff, 0xff0080, 0x80ff00, 0x404040]
TOL = 24        # per channel: the phone's display pipeline and scaling may move a colour a little

def rgb(v): return (v >> 16 & 255, v >> 8 & 255, v & 255)
def near(a, b): return all(abs(a[i] - b[i]) <= TOL for i in range(3))

def report(i):
    r = subprocess.run(ADB + ["shell", "run-as app.aemu cat files/images/%s/root/data/data/app.aemu.test/files/report.txt 2>/dev/null" % i],
                       capture_output=True).stdout.decode("utf-8", "replace")
    return r.replace("\r", "").split("\n")

def check(path):
    im = Image.open(path).convert("RGB")
    w, h = im.size
    px = im.load()
    first = rgb(PATTERN[0])
    cols = [rgb(v) for v in PATTERN]
    def ours(p): return any(near(p, c) for c in cols)
    # the pattern's box: from the first red pixel of the top left cell, right and down for as long as its colours go
    x0 = y0 = None
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            if near(px[x, y], first) and near(px[min(w - 1, x + 20), min(h - 1, y + 20)], first): x0, y0 = x, y; break
        if x0 is not None: break
    if x0 is None: return "no pattern on the screen (its first colour is nowhere)"
    while x0 > 0 and near(px[x0 - 1, y0], first): x0 -= 1
    while y0 > 0 and near(px[x0, y0 - 1], first): y0 -= 1
    # (between two cells the screen's scaling leaves a few blended pixels: a gap of up to 8 does not end the box)
    def run(at, limit, get):
        end, gap = at, 0
        for v in range(at + 1, limit):
            if ours(get(v)): end, gap = v, 0
            else:
                gap += 1
                if gap > 8: break
        return end
    x1 = run(x0, w, lambda v: px[v, y0 + 4])
    y1 = run(y0, h, lambda v: px[x0 + 4, v])
    bw, bh = x1 - x0 + 1, y1 - y0 + 1
    bad = []
    for i in range(16):
        x = x0 + (i % 4) * bw // 4 + bw // 8
        y = y0 + (i // 4) * bh // 4 + bh // 8
        if not near(px[x, y], rgb(PATTERN[i])): bad.append("cell %d is %02x%02x%02x, must be %06x" % ((i,) + px[x, y] + (PATTERN[i],)))
    if bw < w // 3 or bh < h // 8: bad.append("the box is small: %dx%d of %dx%d" % (bw, bh, w, h))
    return "ok (box %dx%d at %d,%d)" % (bw, bh, x0, y0) if not bad else "; ".join(bad)

def check_icons(path):
    # the "texthw" stage: 16 icons (hardware bitmaps of the pattern) with labels; the bridge once drew 5 of them
    im = Image.open(path).convert("RGB"); px = im.load(); w, h = im.size
    red = sum(1 for y in range(h // 10, h * 3 // 4, 4) for x in range(0, w, 4) if px[x, y][0] > 230 and px[x, y][1] < 30 and px[x, y][2] < 30)
    t = check_text(path)
    if red < 16 * 30: return "icons missing: %d red samples, need %d; text: %s" % (red, 16 * 30, t)
    return t if not t.startswith("ok") else "ok (%d red samples, %s)" % (red, t)

def check_text(path):
    # the "text" stage: white page, one white label with a shadow, then dark labels in seven sizes. A bridge that loses
    # the dark text (Launcher3 app list on 11, 15.10) leaves the page nearly white.
    im = Image.open(path).convert("L")
    w, h = im.size
    box = im.crop((0, h // 10, w, h * 5 // 8))
    dark = sum(1 for v in box.getdata() if v < 110)
    need = w * h // 400
    return ("ok (%d dark pixels)" % dark) if dark >= need else ("dark text missing: %d dark pixels, need %d" % (dark, need))

def main():
    i = sys.argv[1]
    env = dict(os.environ, NOWAIT="1")
    subprocess.run(["sh", W.replace("E:", "/e") + "/gt.sh", i, "pattern"], env=env, capture_output=True)
    seen, end, fails = set(), time.time() + 60, 0
    while time.time() < end:
        lines = report(i)
        for l in lines:
            if l.startswith("pattern.stage=") and l not in seen:
                seen.add(l)
                stage = l.split("=")[1].split(" ")[0]
                path = "%s/gtp-%s-%s.png" % (W, i, stage)
                with open(path, "wb") as f: f.write(subprocess.run(ADB + ["exec-out", "screencap", "-p"], capture_output=True).stdout)
                v = check_icons(path) if stage == "texthw" else check_text(path) if stage.startswith("text") else check(path)
                if not v.startswith("ok"): fails += 1
                print("host.pattern.%s=%s" % (stage, v))
        if "done=1" in lines: break
        time.sleep(0.4)
    for l in report(i):
        if l.startswith("pattern.") and not l.startswith("pattern.stage"): print(l)
    if not seen: print("host.pattern=no stage was announced"); fails += 1
    sys.exit(1 if fails else 0)

main()
