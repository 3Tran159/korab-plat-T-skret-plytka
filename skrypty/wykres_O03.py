r"""Wykres zwyciezcy optymalizacji: O03 (elipsa 180x100) vs O00 (bez plytki) i O01 (prostokat) w modelu zgrubionym.
Wentylacja strutu, sila boczna, sila nosna plat+plytka, opor calkowity i opor plytki w funkcji kata. -> 40_WYNIKI\OPT\O03_vs_O00_O01.png"""
import csv
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
R = r"T:\KORAB_PLAT_T_SKRET7_20261118"
CASES = [("O00_bez_plytki", "O00 bez plytki", "#888888", 1.6), ("O01_prostokat_Y05", "O01 prostokat 100x100x3 (jak Y05)", "#e8792b", 1.6),
         ("O03_elipsa_caly_strut", "O03 elipsa 180x100, NACA 2 mm", "#1f6fd1", 2.4)]


def load(c):
    rows = {}
    for r in csv.DictReader(open(rf"{R}\30_PRZEBIEGI\{c}\TF7_HISTORIA.csv")):
        try: rows[round(float(r["time_s"]), 5)] = r
        except Exception: pass
    return [rows[k] for k in sorted(rows)]


g = lambda r, k: float(r[k]) if r.get(k) not in (None, "") else 0.0
L = lambda r: (g(r, "TF7_Lp_foil_dyn") + g(r, "TF7_Ls_foil") + g(r, "TF7_L_plytka")) / 9.81
P = [("Wentylacja strutu [% pow. zanurzonej]", lambda r: g(r, "TF7_went_strut_pct")), ("Sila boczna Fy [N]", lambda r: g(r, "TF7_Fy_total")),
     ("Sila nosna plat + plytka [kg]", L), ("Opor calkowity [N]", lambda r: g(r, "TF7_D_total")), ("Opor samej plytki [N]", lambda r: g(r, "TF7_D_plytka"))]
fig, ax = plt.subplots(len(P), 1, figsize=(9, 15), sharex=True)
for c, lab, col, lw in CASES:
    rows = load(c); x = [g(r, "TF7_skret_deg") for r in rows]
    for a, (t, f) in zip(ax, P):
        if "plytki" in t and c.startswith("O00"): continue
        a.plot(x, [f(r) for r in rows], color=col, lw=lw, label=lab); a.set_ylabel(t, fontsize=9); a.grid(alpha=0.3)
ax[0].axhline(30, color="k", ls=":", lw=0.8); ax[0].text(0.5, 31, "kryterium: 30 %", fontsize=8)
for k, col in ((24.1, "#888888"), (22.8, "#e8792b"), (27.9, "#1f6fd1")):
    ax[0].axvline(k, color=col, ls="--", lw=0.8); ax[0].text(k + 0.15, 2, f"{k:.1f}".replace(".", ",") + "°", color=col, fontsize=8)
ax[0].legend(fontsize=8, loc="upper left"); ax[-1].set_xlabel("kat skretu strutu [deg] (model zgrubiony, 30 km/h, h = 75 mm, plytka 20 mm pod lustrem)")
ax[-1].set_xlim(0, 29)
fig.suptitle("Plytka O03: wentylacja strutu przesunieta z 24,1° na 27,9° (+3,8°); opor plytki 3,8 N przy 10°", fontsize=11)
fig.text(0.5, 0.003, "Spadki Fy przy ~16° (O01, O03) zbiegaja sie z koncem szybkiej rampy 60->15 °/s - prawdopodobnie artefakt metody; sprawdzi Y07 (pelny model).", ha="center", fontsize=8)
fig.tight_layout(rect=[0, 0.01, 1, 0.98]); fig.savefig(rf"{R}\40_WYNIKI\OPT\O03_vs_O00_O01.png", dpi=120); print("ok")
