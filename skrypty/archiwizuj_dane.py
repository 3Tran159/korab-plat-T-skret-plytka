# Archiwizacja ciezkich danych projektu na H: (FINAL + SETUP + historie .simh; Y08 dodatkowo ostatni CKPT - wznawialny).
# Posrednie punkty zapisu -> <dysk>\..._do_usuniecia\ (kasowanie dopiero po zgodzie Julka). Nic nie jest kasowane.
# python archiwizuj_dane.py          -> tylko plan (dry run)
# python archiwizuj_dane.py --wykonaj
import sys, shutil, json, csv
from pathlib import Path

GO = "--wykonaj" in sys.argv
ARCH = Path(r"H:\10_ARCHIWUM_PROJEKTOW\KORAB\KORAB_plat_T_skret_plytka_dane")
SRC = [Path(r"T:\KORAB_PLAT_T_SKRET7_20261118"), Path(r"E:\KORAB_PLAT_T_SKRET7_20261118_przeniesione")]
DEL = {"T": Path(r"T:\KORAB_PLAT_T_SKRET7_20261118_do_usuniecia"), "E": Path(r"E:\KORAB_PLAT_T_SKRET7_20261118_do_usuniecia")}
HIST = Path(r"E:\20_DANE_OBLICZEN_PRZELEW\KORAB_plat_T_historie_DO_ODSTRZALU_gdy_brak_miejsca")  # decyzja Julka 26.11
AKTYWNE = ("Y09_plytka_O23", "Y10_plytka_O03")       # licza sie / czekaja - nie ruszac
WZNAWIALNE = ("Y08_rampa_powrotna",)                 # zachowac ostatni CKPT
plan = []

for root in SRC:
    for f in root.rglob("*"):
        if not f.is_file() or f.suffix.lower() not in (".sim", ".simh", ".sim~", ".png"):
            continue
        rel = f.relative_to(root)
        case = f.parent.name
        if any(a in str(f) for a in AKTYWNE) or "_do_usuniecia" in str(f):
            continue
        if f.suffix.lower() == ".png":
            # tylko klatki animacji z renderow (render_*, katalogi >10 png); reszta wykresow zostaje
            if not (any(p.startswith("render_") for p in rel.parts) or ("40_WYNIKI" in rel.parts and len(list(f.parent.glob("*.png"))) > 10)):
                continue
            plan.append(["H", f, ARCH / "_wyniki_klatki" / rel])
            continue
        n = f.name
        same = [p for r in SRC for p in r.rglob(case) if p.is_dir()]   # ten sam przypadek na T: i E:
        ckpts = sorted((c for p in same for c in p.glob("TF7_CKPT*.sim")), key=lambda p: p.stat().st_mtime)
        ostatni_ckpt_bez_final = (n.startswith("TF7_CKPT") and ckpts and f == ckpts[-1]
                                  and not any(c for p in same for c in p.glob("*FINAL*.sim")))
        keep = ("FINAL" in n or "SETUP" in n or f.suffix.lower() == ".simh" or ostatni_ckpt_bez_final)
        sub = "_odrzucone" if ("_odrzucone" in str(rel) or "90_PROBY" in rel.parts) else ""
        if f.suffix.lower() == ".simh" and f.stat().st_size > 2**30:
            plan.append(["HIST", f, HIST / case / n])
        elif keep:
            plan.append(["H", f, ARCH / sub / case / n])
        else:
            plan.append(["DEL", f, DEL[root.drive[0]] / rel])

gb = lambda rows: round(sum(r[1].stat().st_size for r in rows) / 2**30, 1)
h = [r for r in plan if r[0] == "H"]; d = [r for r in plan if r[0] == "DEL"]
print(json.dumps({"do_H_plikow": len(h), "do_H_GB": gb(h), "do_usuniecia_plikow": len(d), "do_usuniecia_GB": gb(d)}))
if not GO:
    for r in sorted(h, key=lambda r: -r[1].stat().st_size)[:15]:
        print("H  ", round(r[1].stat().st_size / 2**30, 2), r[1])
    for r in sorted(d, key=lambda r: -r[1].stat().st_size)[:8]:
        print("DEL", round(r[1].stat().st_size / 2**30, 2), r[1])
    sys.exit()

ARCH.mkdir(parents=True, exist_ok=True)
log = []
for kind, f, dst in plan:
    dst.parent.mkdir(parents=True, exist_ok=True)
    if dst.exists():
        dst = dst.with_name(dst.stem + f"__{f.drive[0]}" + dst.suffix)
    size = f.stat().st_size
    shutil.move(str(f), str(dst))
    log.append([kind, str(f), str(dst), round(size / 2**30, 3)])
with open(ARCH / "PRZENIESIENIA.csv", "a", newline="", encoding="utf-8") as fh:
    csv.writer(fh).writerows(log)
for p in DEL.values():
    if p.exists():
        (p / "CZYTAJ.txt").write_text("Posrednie punkty zapisu projektu KORAB plat T (26.11.2026). Do skasowania po zgodzie Julka. "
                                      "Stany koncowe i ustawienia: " + str(ARCH) + "\n", encoding="utf-8")
if HIST.exists():
    (HIST / "CZYTAJ.txt").write_text("Historie pol (.simh) projektu KORAB plat T: Y04, Y05, Y07 (zrodlo filmow H01-H14). "
        "Decyzja Julka 26.11.2026: moga byc skasowane, gdy zabraknie miejsca (filmy sa w repo "
        "github.com/3Tran159/korab-plat-T-skret-plytka, stany koncowe na " + str(ARCH) + ").\n", encoding="utf-8")
print("przeniesiono", len(log))
