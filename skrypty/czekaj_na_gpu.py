r"""Czeka, az karta graficzna bedzie wolna od innych procesow STAR (np. przebieg innego agenta),
potem sprzata resztki nieudanego startu Y04 i uruchamia kontroler.py Y04.
Log: 00_START\kontroler.log (wpisy 'GPU-WAIT').  Uzycie: python czekaj_na_gpu.py <przypadek>
"""
import sys, time, subprocess, shutil
from pathlib import Path
ROOT = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118"); START = ROOT / "00_START"
CASE = ROOT / "30_PRZEBIEGI" / sys.argv[1]
LOG = START / "kontroler.log"; STOP = START / "STOP_KAMPANIA"
PY = r"T:\NTNU_GODO_CONTROL_20261109\.venv\Scripts\python.exe"


def log(m):
    with open(LOG, "a", encoding="utf8") as f: f.write(time.strftime("%Y-%m-%d %H:%M:%S ") + "GPU-WAIT " + m + "\n")


def star_on_gpu():
    try:
        out = subprocess.run(["nvidia-smi", "--query-compute-apps=pid,process_name", "--format=csv,noheader"],
                             capture_output=True, text=True, timeout=60).stdout
    except Exception:
        return ["?"]
    return [l.strip() for l in out.splitlines() if "star-ccm+.exe" in l.lower()]   # tylko proces liczacy (launcher python.exe przebiegow CPU tez bywa na liscie)


def main():
    log(f"czekam na wolna karte dla {CASE.name}")
    free = 0
    while free < 2:
        busy = star_on_gpu()
        free = free + 1 if not busy else 0
        if busy: log(f"zajeta: {busy}")
        time.sleep(120)
    dst = ROOT / "_odrzucone" / (CASE.name + "_resztki_" + time.strftime("%Y%m%d_%H%M"))
    dst.mkdir(parents=True, exist_ok=True)
    for p in CASE.iterdir():
        if p.name in ("control.properties", "SETUP.json", "ANIMACJE") or p.name.endswith("_SETUP.sim"): continue   # ANIMACJE: sciezki scen (Y08)
        shutil.move(str(p), str(dst / p.name))
    if STOP.exists(): STOP.unlink()
    log(f"karta wolna, resztki -> {dst.name}, start kontrolera")
    subprocess.Popen([PY, str(START / "kontroler.py"), CASE.name], cwd=START,
                     stdout=open(START / "kontroler_Y04.out", "a"), stderr=open(START / "kontroler_Y04.err", "a"),
                     creationflags=0x08000000)


if __name__ == "__main__":
    main()
