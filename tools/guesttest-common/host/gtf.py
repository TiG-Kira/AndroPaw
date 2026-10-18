# gtf.py <id> [tests]: frames counted on both sides. Runs each timed test of the guest tester (default ui,gl1,gl2,gl3)
# for 6 s and meanwhile asks the emulator twice, 3 s apart, how many frames its screen has shown (the debug
# broadcast "frames" of VmActivity). Prints the guest's rate, the host's rate and the difference; over 2 fps apart
# is a failure: the guest believes in frames nobody saw (or the other way round).
import os, subprocess, sys, time

W = os.environ.get("W", os.getcwd())
ADB = [os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")] + (["-s", os.environ["ANDROID_SERIAL"]] if os.environ.get("ANDROID_SERIAL") else [])

def sh(cmd):
    return subprocess.run(ADB + ["shell", cmd], capture_output=True).stdout.decode("utf-8", "replace").replace("\r", "")

def sample(i, n):
    name = "gtf%d.out" % n
    sh("run-as app.aemu rm -f files/images/%s/run/%s; am broadcast -a app.aemu.SHELL --ez frames true --es out %s" % (i, name, name))
    for _ in range(20):
        t = sh("run-as app.aemu cat files/images/%s/run/%s 2>/dev/null" % (i, name)).strip()
        if t.startswith("shown="):
            f = dict(p.split("=") for p in t.split(" "))
            return int(f["shown"]), int(f["ms"]), f["direct"]
        time.sleep(0.1)
    return None

def report(i):
    return sh("run-as app.aemu cat files/images/%s/root/data/data/app.aemu.test/files/report.txt 2>/dev/null" % i).split("\n")

def main():
    i = sys.argv[1]
    tests = (sys.argv[2] if len(sys.argv) > 2 else "ui,gl1,gl2,gl3").split(",")
    fails = 0
    for t in tests:
        subprocess.run(["sh", W.replace("E:", "/e") + "/gt.sh", i, t, "6"], env=dict(os.environ, NOWAIT="1"), capture_output=True)
        # (the test itself starts 0.8 s after the program; the window lies inside its 6 s)
        end = time.time() + 15
        while time.time() < end and not any(l == "test=" + t for l in report(i)): time.sleep(0.2)
        time.sleep(1.2)
        a = sample(i, 1)
        time.sleep(3)
        b = sample(i, 2)
        end = time.time() + 30
        lines = []
        while time.time() < end:
            lines = report(i)
            if "done=1" in lines: break
            time.sleep(0.5)
        guest = [l.split("=")[1] for l in lines if l.startswith(t + ".fps=")]
        if not a or not b or b[1] == a[1]: print("frames.%s=no answer from the emulator (old build?)" % t); fails += 1; continue
        if not guest: print("frames.%s=the guest gave no rate (%s)" % (t, [l for l in lines if l.startswith(t + ".")][:3])); fails += 1; continue
        host = (b[0] - a[0]) * 1000.0 / (b[1] - a[1])
        diff = abs(host - float(guest[0]))
        if diff > 2: fails += 1
        print("frames.%s=guest %s host %.1f diff %.1f %s (%s)" % (t, guest[0], host, diff, "ok" if diff <= 2 else "MISMATCH", "direct" if b[2] == "true" else "fb0"))
    sys.exit(1 if fails else 0)

main()
