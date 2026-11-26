r"""Kontroler: plat T, skret strutu, 30 km/h, zanurzenie projektowe 1 MAC, VOF + SST k-omega IDDES.
Setup i liczenie przypiete do rdzenia 0 (CORES), liczenie na GPU do terminu z control.properties (deadline),
wznowienie z LATEST_CHECKPOINT.json. Log: 00_START\kontroler.log. Plik 00_START\STOP_KAMPANIA blokuje start.

Straznik (NIE zatrzymuje liczenia): co 60 s czyta TF7_HISTORIA.csv; gdy liczby wygladaja na rozbieznosc
(|Cp min| > 5, sila nosna poza 15..90 kg, opor poza 0..150 N, NaN) zapisuje <przypadek>\ALARM.json
i wpis w logu. Decyzje o zatrzymaniu podejmuje Julek.
Pilnowanie rdzenia: co 60 s przypina WSZYSTKIE procesy STAR tego przypadku (takze proces liczacy uruchamiany
przez MPI poza drzewem procesow) do rdzenia 0.
Poprzednia wersja: _odrzucone\kontroler_v1_przed_rdzen0.py
"""
import sys, json, time, threading, base64, subprocess, math
from pathlib import Path
sys.path.insert(0, r"T:\NTNU_GODO_CONTROL_20261109\20_NARZEDZIA")
import starrun
ROOT = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118"); MAK = ROOT / "20_MAKRA"
SRC = Path(r"T:\KORAB_PLAT_T_ZANURZENIE_20261116\10_ZRODLO\T_6deg_jednofazowy_oryginal.sim")
LOG = ROOT / "00_START" / "kontroler.log"; STOP = ROOT / "00_START" / "STOP_KAMPANIA"
CASE = ROOT / "30_PRZEBIEGI" / sys.argv[1] if len(sys.argv) > 1 else ROOT / "30_PRZEBIEGI" / "Y01_skret7_30kmh_h1c_IDDES"
CORES = (22,)   # 2026-11-20: 2 rdzenie (22-23) nie przyspieszyly (jeden watek); 1 rdzen P, rdzen 0 wolny dla systemu
# 2026-11-21: control.properties moze nadpisac: cores=18,19,20 / np=6 / gpu=false / run_timeout_s (przypadki CPU modelu optymalizacyjnego)
try:
    _P = dict(l.split("=", 1) for l in (CASE / "control.properties").read_text().split() if "=" in l)
except Exception:
    _P = {}
if _P.get("cores"): CORES = tuple(int(c) for c in _P["cores"].split(","))
NP = int(_P.get("np", "1")); GPU = _P.get("gpu", "true").lower() != "false"
RUN_TMO = int(float(_P.get("run_timeout_s", 40 * 3600)))   # 16 h ucinalo Y05 przed koncem (sesja 03:55 -> 19:54)
CPU_MASK = sum(1 << c for c in CORES)
NOWIN = 0x08000000


def log(m):
    with open(LOG, "a", encoding="utf8") as f: f.write(time.strftime("%Y-%m-%d %H:%M:%S ") + m + "\n")


def jl(p):
    try: return json.loads(Path(p).read_text(encoding="utf8"))
    except Exception: return {}


# ---------- pilnowanie rdzenia ----------
PS_LIST = ("Get-CimInstance Win32_Process | Where-Object { $_.Name -match '^(star-ccm\\+|starccm\\+|mpiexec|hydra_pmi_proxy)' } | "
           "ForEach-Object { \"$($_.ProcessId)`t$($_.CommandLine)\" }")


def pin_case(case: Path):
    key = str(case).lower().replace("/", "\\")
    try:
        out = subprocess.run(["powershell", "-NoProfile", "-Command", PS_LIST], capture_output=True, text=True,
                             timeout=60, creationflags=NOWIN).stdout
    except Exception:
        return []
    pids = []
    for line in out.splitlines():
        if "\t" not in line: continue
        pid, cmd = line.split("\t", 1)
        txt = cmd.lower().replace("/", "\\")
        for flag in ("-encoded-session", "-xencoded-session"):
            if flag in cmd:
                try:
                    enc = cmd.split(flag, 1)[1].split()[0]
                    txt += " " + base64.b64decode(enc + "=" * (-len(enc) % 4)).decode("utf8", "ignore").lower().replace("/", "\\")
                except Exception:
                    pass
        if key in txt: pids.append(int(pid))
    if pids:
        ps = ";".join(f"try{{$p=Get-Process -Id {p} -EA Stop;if([int64]$p.ProcessorAffinity -ne {CPU_MASK})"
                      f"{{$p.ProcessorAffinity=[IntPtr]{CPU_MASK}}}}}catch{{}}" for p in pids)
        try: subprocess.run(["powershell", "-NoProfile", "-Command", ps], capture_output=True, timeout=60, creationflags=NOWIN)
        except Exception: pass
    return pids


# ---------- straznik (tylko alarm, bez zatrzymywania) ----------
def check(case: Path):
    csv = case / "TF7_HISTORIA.csv"
    if not csv.exists(): return None
    try:
        lines = csv.read_text(encoding="utf8").strip().splitlines()
        if len(lines) < 2: return None
        h = lines[0].split(","); row = dict(zip(h, lines[-1].split(",")))
        f = lambda k: float(row.get(k, "nan"))
        cp, L, D, t = f("TF7_Cp_min_strut"), f("TF7_L_kg"), f("TF7_D_total"), f("time_s")
    except Exception:
        return None
    why = []
    if any(math.isnan(x) or math.isinf(x) for x in (cp, L, D)): why.append("NaN/Inf")
    else:
        if abs(cp) > 5: why.append(f"|Cp min strut| = {abs(cp):.3g} > 5")
        if t > 0.05 and not 15 <= L <= 90: why.append(f"sila nosna {L:.3g} kg poza 15..90")
        if t > 0.05 and not 0 <= D <= 150: why.append(f"opor {D:.3g} N poza 0..150")
    return (t, why, {"Cp_min_strut": cp, "L_kg": L, "D_total": D}) if why else None


def guard(case: Path, stop_evt: threading.Event):
    alarmed = (case / "ALARM.json").exists()
    while not stop_evt.wait(60):
        pin_case(case)
        if alarmed: continue
        r = check(case)
        if r:
            t, why, vals = r
            (case / "ALARM.json").write_text(json.dumps({"czas": time.strftime("%Y-%m-%d %H:%M:%S"), "t_s": t, "powod": why, **vals},
                                                       indent=1), encoding="utf8")
            log(f"ALARM t={t} {'; '.join(why)} (liczenie trwa dalej, decyzja: Julek)")
            alarmed = True


def run_star(sim, macro, d, gpu, env=None, timeout_s=3600):
    ev = threading.Event(); threading.Thread(target=guard, args=(d, ev), daemon=True).start()
    try:
        return starrun.launch(str(sim), str(macro), str(d), cpus=CORES, np_=NP if not gpu else 1, timeout_s=timeout_s, gpu=gpu, extra_env=env)
    finally:
        ev.set()
        # sprzatanie: po wyjsciu launchera zabij pozostale procesy STAR TEGO przypadku (wiszace serwery trzymaly 11,7 GB GPU 21.11)
        time.sleep(20)
        left = pin_case(d)
        if left:
            log(f"sprzatanie: wiszace procesy przypadku {left} -> kill")
            for p in left:
                subprocess.run(["taskkill", "/F", "/PID", str(p)], capture_output=True, creationflags=NOWIN)


def main():
    d = CASE; log(f"=== start {d.name} (rdzen {CORES}) ===")
    if STOP.exists(): log("STOP_KAMPANIA"); return
    if not (jl(d / "SETUP.json").get("stage") == "done" and any(d.glob("*_SETUP.sim"))):
        log("SETUP start")
        props = dict(l.split("=", 1) for l in (d / "control.properties").read_text().split() if "=" in l)
        src = props.get("source_sim", str(SRC))
        r = run_star(src, MAK / "TFoilYawSetup.java", d, gpu=False, timeout_s=3 * 3600)
        s = jl(d / "SETUP.json"); log(f"SETUP {s.get('stage')} rc={r.get('returncode')} {r.get('seconds')} s cells={s.get('cells')} err={s.get('error', '')}")
        if s.get("stage") != "done": return
    for attempt in range(4):
        if STOP.exists(): log("STOP_KAMPANIA"); return
        st = jl(d / "RUN_STATUS.json").get("state")
        if st in ("DONE", "STOPPED_DEADLINE"): log(f"koniec: {st}"); return
        lc = jl(d / "LATEST_CHECKPOINT.json").get("path")
        sim = Path(lc) if lc and Path(lc).exists() else next(d.glob("*_SETUP.sim"))
        env = {"TF_RESUMED": "true" if sim.name.startswith("TF7_CKPT") else "false", "TF_RESUMED_FROM": sim.name}
        log(f"RUN start {'GPU' if GPU else f'CPU np={NP} {CORES}'} z {sim.name} (proba {attempt + 1})")
        r = run_star(sim, MAK / "TFoilYawRun.java", d, gpu=GPU, env=env, timeout_s=RUN_TMO)
        s = jl(d / "RUN_STATUS.json")
        log(f"RUN -> {s.get('state')} rc={r.get('returncode')} {r.get('seconds')} s t={s.get('time_s')} L={s.get('TF7_L_kg')} went={s.get('TF7_went_strut_pct')} err={s.get('error', '')}")
        if s.get("state") in ("DONE", "STOPPED_DEADLINE", "STOPPED_BY_FILE"): return
        time.sleep(60)
    log("HOLD: 4 nieudane proby")


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[2] == "pin":   # tryb pomocniczy: tylko przypnij procesy przypadku
        print(pin_case(CASE)); sys.exit()
    main()
