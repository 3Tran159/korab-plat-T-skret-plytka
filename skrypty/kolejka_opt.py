r"""Kolejka przypadkow CPU (model optymalizacyjny plytki): po kolei kontroler.py dla kazdego przypadku na podanych rdzeniach.
Uzycie: python kolejka_opt.py <rdzenie np. 18,19,23> [--czekaj-na render] przypadek1 przypadek2 ...
--czekaj-na <tekst>: najpierw czeka, az zaden proces STAR nie ma <tekst> w linii polecen (np. render historii Y05 na tych rdzeniach).
"""
import sys, subprocess, time, re
from pathlib import Path
R = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118"); PY = r"T:\NTNU_GODO_CONTROL_20261109\.venv\Scripts\python.exe"
LOG = R / "00_START" / "kolejka_opt.log"; NOWIN = 0x08000000
a = sys.argv[1:]; cores = a.pop(0); wait = None
if a and a[0] == "--czekaj-na": a.pop(0); wait = a.pop(0)
wpid = None
if a and a[0] == "--czekaj-pid": a.pop(0); wpid = int(a.pop(0))   # czekaj na koniec innej kolejki (PID)


def log(m):
    with open(LOG, "a", encoding="utf8") as f: f.write(time.strftime("%Y-%m-%d %H:%M:%S ") + f"[{cores}] " + m + "\n")


def busy(txt):
    ps = "Get-CimInstance Win32_Process | ? { $_.CommandLine -match 'uruchom_makro' } | % { $_.CommandLine }"
    out = subprocess.run(["powershell", "-NoProfile", "-Command", ps], capture_output=True, text=True, creationflags=NOWIN).stdout
    return txt.lower() in out.lower()


if wait:
    log(f"czekam na koniec procesow z '{wait}'")
    while busy(wait): time.sleep(60)
if wpid:
    log(f"czekam na koniec PID {wpid}")
    while subprocess.run(["powershell", "-NoProfile", "-Command", f"Get-Process -Id {wpid} -EA SilentlyContinue"],
                         capture_output=True, text=True, creationflags=NOWIN).stdout.strip(): time.sleep(60)
for c in a:
    p = R / "30_PRZEBIEGI" / c / "control.properties"
    t = re.sub(r"(?m)^cores=.*\n?", "", p.read_text()); t += f"cores={cores}\nnp={len(cores.split(','))}\n"
    t = re.sub(r"(?m)^np=.*\n(?=(?:.*\n)*np=)", "", t)   # zostaw ostatnie np=
    p.write_text(t)
    if (R / "00_START" / "STOP_OPT").exists(): log("STOP_OPT"); break
    log(f"start {c}")
    r = subprocess.run([PY, str(R / "00_START" / "kontroler.py"), c], creationflags=NOWIN)
    log(f"koniec {c} rc={r.returncode}")
log("kolejka pusta")
