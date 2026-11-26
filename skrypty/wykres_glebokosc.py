r"""Studium glebokosci plytki O03 (elipsa 180x100, NACA 2 mm): kat wentylacji (>30 %) i Fy maks w funkcji glebokosci pod lustrem.
Dane z 40_WYNIKI\OPT\opt_kryteria.csv (porownanie_opt.py). -> 40_WYNIKI\OPT\glebokosc.png"""
import csv
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
O = r"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\OPT"
K = {r["przypadek"]: r for r in csv.DictReader(open(O + r"\opt_kryteria.csv"))}
# glebokosc [mm] -> (etykieta przypadku, rampa do [deg]); dla tej samej glebokosci przypadek z rampa do 35 ma pierwszenstwo
D = [(9, "O13 O03 na 9 mm (do 35)", 35), (12, "O15 O03 na 12 mm (do 35)", 35), (12, "O09 O03 na 12 mm", 29), (16, "O14 O03 na 16 mm (do 35)", 35),
     (20, "O17 O03 na 20 mm (do 35)", 35), (20, "O03 elipsa 180x100", 29), (30, "O08 O03 na 30 mm", 29)]
pts = {}
for dz, lab, ramp in D:
    r = K.get(lab)
    if not r or not r["Fy_max"] or dz in pts: continue
    kw = r["kat_went30"]
    pts[dz] = (float(kw) if kw else float(r["kat_koniec"]), not kw, float(r["Fy_max"]), lab, ramp)
ref = K["O00 bez plytki"]; k0 = float(ref["kat_went30"]); f0 = float(ref["Fy_max"])
x = sorted(pts)
fig, ax = plt.subplots(1, 2, figsize=(11, 4.2))
ax[0].plot(x, [pts[d][0] for d in x], "-o", color="#1f6fd1")
for d in x:
    k, low, f, lab, ramp = pts[d]
    ax[0].annotate((">= " if low else "") + f"{k:.1f}".replace(".", ",") + "°", (d, k), textcoords="offset points", xytext=(6, 6), fontsize=9)
    if low: ax[0].plot(d, k, "^", color="#1f6fd1", ms=10)
ax[0].axhline(k0, color="#888", ls="--"); ax[0].text(x[-1], k0 + 0.15, "bez plytki", ha="right", color="#666", fontsize=9)
ax[0].set_xlabel("glebokosc plytki pod lustrem [mm]"); ax[0].set_ylabel("kat skretu przy wentylacji strutu > 30 % [deg]"); ax[0].grid(alpha=0.3)
ax[1].plot(x, [pts[d][2] for d in x], "-o", color="#1f6fd1")
ax[1].axhline(f0, color="#888", ls="--"); ax[1].text(x[-1], f0 + 1, "bez plytki", ha="right", color="#666", fontsize=9)
ax[1].set_xlabel("glebokosc plytki pod lustrem [mm]"); ax[1].set_ylabel("Fy maks [N]"); ax[1].grid(alpha=0.3)
fig.suptitle("Plytka O03 (elipsa 180x100, 2 mm): wplyw glebokosci (model zgrubiony; trojkat = dolna granica, rampa konczyla sie przed wentylacja)", fontsize=10)
fig.tight_layout(); fig.savefig(O + r"\glebokosc.png", dpi=130); print("ok", {d: pts[d][:3] for d in x})
