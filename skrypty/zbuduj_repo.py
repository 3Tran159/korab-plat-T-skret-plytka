# Buduje lekkie repo projektu (KOPIUJE, niczego nie przenosi ani nie kasuje).
# python zbuduj_repo.py [cel]   domyslnie F:\20_DANE_OBLICZEN\CFD_KORAB_i_praca_inz\korab-plat-T-skret-plytka
import shutil, sys, json, csv
from pathlib import Path

T = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118")
ROOTS = [T / "30_PRZEBIEGI",
         Path(r"E:\KORAB_PLAT_T_SKRET7_20261118_przeniesione\30_PRZEBIEGI"),
         Path(r"H:\10_ARCHIWUM_PROJEKTOW\KORAB\KORAB_PLAT_T_SKRET7_odrzucone_20261122")]
DST = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"F:\20_DANE_OBLICZEN\CFD_KORAB_i_praca_inz\korab-plat-T-skret-plytka")

GLOWNE_MAKRA = {"TFoilYawSetup.java", "TFoilYawRun.java", "TFoilRampBack.java", "TFoilHistRender.java"}
CASE_FILES = {"control.properties", "SETUP.json", "RUN_STATUS.json", "TF7_HISTORIA.csv", "ALARM.json",
              "LATEST_CHECKPOINT.json", "launch_result.json"}
LIGHT_EXT = {".png", ".csv", ".json", ".txt", ".md", ".svg", ".pdf"}
SKIP_SCRIPTS_EXT = {".log", ".err", ".out"}
MAX_LIGHT = 20 * 1024 * 1024
inv = []

def cp(src, dst):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)

# 1. makra STAR
for f in (T / "20_MAKRA").glob("*.java"):
    cp(f, DST / "star" / "makra" / ("" if f.name in GLOWNE_MAKRA else "diagnostyka") / f.name)
# 2. skrypty
for f in (T / "00_START").iterdir():
    if f.is_file() and f.suffix == ".py":
        cp(f, DST / "skrypty" / f.name)
cp(T / "00_START" / "kontroler.log", DST / "przypadki" / "_kontroler.log")
# 3. geometria (STL i male pliki)
for f in (T / "10_GEOMETRIA").rglob("*"):
    if f.is_file():
        inv.append(["geometria", str(f), round(f.stat().st_size / 2**30, 3)])
        if f.stat().st_size < 2 * 1024 * 1024:
            cp(f, DST / "geometria" / f.relative_to(T / "10_GEOMETRIA"))
# 4. przypadki: tylko lekkie pliki opisowe i historie; inwentarz ciezkich plikow
seen = {}
for root in ROOTS:
    if not root.exists():
        continue
    for d in sorted(p for p in root.rglob("*") if p.is_dir() and (p / "control.properties").exists()):
        name = d.name
        tag = "" if name not in seen else f"__{root.drive[0]}"
        if root.drive.startswith("H") or "_odrzucone" in str(d):
            sub = "_odrzucone"
        else:
            sub = ""
        out = DST / "przypadki" / sub / (name + tag)
        seen[name] = 1
        for f in d.iterdir():
            if f.is_file() and f.name in CASE_FILES:
                cp(f, out / f.name)
        for f in d.rglob("*"):
            if f.is_file() and f.suffix in (".sim", ".simh"):
                inv.append([name, str(f), round(f.stat().st_size / 2**30, 3)])
# 5. wyniki: wykresy, tabele i filmy mp4; bez klatek animacji (katalogi z >50 png) i bez .sim
for f in (T / "40_WYNIKI").rglob("*"):
    if not f.is_file() or f.stat().st_size >= MAX_LIGHT:
        continue
    if any(p.startswith("render_") or p == "profile" for p in f.parts):
        continue
    if f.suffix.lower() == ".png" and (len(list(f.parent.glob("*.png"))) > 10 or any(p.startswith("render_") for p in f.parts)):
        continue
    if f.suffix.lower() in LIGHT_EXT | {".mp4"}:
        cp(f, DST / "wyniki" / f.relative_to(T / "40_WYNIKI"))
filmy = [[str(f), round(f.stat().st_size / 2**20, 1)] for f in (T / "40_WYNIKI").rglob("*.mp4")]
# 6. inwentarz ciezkich danych
DST.mkdir(parents=True, exist_ok=True)
with open(DST / "ARCHIWUM_DANYCH_inwentarz.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh); w.writerow(["przypadek", "plik", "GB"]); w.writerows(inv)
with open(DST / "wyniki" / "FILMY_inwentarz.csv", "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh); w.writerow(["plik", "MB"]); w.writerows(filmy)
tot = sum(f.stat().st_size for f in DST.rglob("*") if f.is_file())
print(json.dumps({"repo": str(DST), "MB": round(tot / 2**20, 1), "przypadki": len(seen),
                  "sim_simh_plikow": len(inv), "sim_simh_GB": round(sum(r[2] for r in inv), 1),
                  "filmow": len(filmy), "filmy_GB": round(sum(r[1] for r in filmy) / 1024, 1)}, ensure_ascii=False))
