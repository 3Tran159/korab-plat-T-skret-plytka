r"""Optymalizacja plytki: porownanie O00..O05 (model zgrubiony) i Y04/Y05 (pelny model).
Kryteria: kat zalamania sily bocznej (Fy spada ponizej 70 % dotychczasowego maksimum), kat, przy ktorym wentylacja strutu > 30 %,
Fy max, opor i sila pionowa plytki przy 10 deg. Wiersze z wznowien (ten sam czas) -> ostatni zapis.
Wyjscie: 40_WYNIKI\OPT\opt_tabela.csv, opt_kryteria.csv, opt_vs_Y04Y05.png. Uzycie: python porownanie_opt.py"""
import csv, glob, os
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
R = r"T:\KORAB_PLAT_T_SKRET7_20261118"; O = R + r"\40_WYNIKI\OPT"; os.makedirs(O, exist_ok=True)
CASES = [("Y04_rampa_0do45_30kmh_h1c_IDDES", "Y04 pelny, bez plytki"), ("Y05_plytka_rampa_0do45_30kmh_h1c_IDDES", "Y05 pelny, prostokat"),
         ("Y07_plytka_O03_elipsa180x100_rampa_0do45_30kmh_h1c_IDDES", "Y07 pelny, O03"),
         ("O00_bez_plytki", "O00 bez plytki"), ("O01_prostokat_Y05", "O01 prostokat 100x100x3"), ("O03_elipsa_caly_strut", "O03 elipsa 180x100"),
         ("O04_elipsa_caly_strut_szeroka", "O04 elipsa 180x140"), ("O05_ogon", "O05 ogon 125x100"),
         ("O06_elipsa_przesunieta_do_tylu", "O06 elipsa 180x100 -30 mm"), ("O07_elipsa_waska", "O07 elipsa 180x70"),
         ("O08_elipsa_gleboko_30mm", "O08 O03 na 30 mm"), ("O09_elipsa_plytko_12mm", "O09 O03 na 12 mm"),
         ("O10_elipsa_1mm", "O10 O03 grubosc 1 mm"), ("O11_ogon_szeroki", "O11 ogon 125x140"),
         ("O12_elipsa_6mm", "O12 O03 na 6 mm (do 35)"), ("O13_elipsa_9mm", "O13 O03 na 9 mm (do 35)"), ("O14_elipsa_16mm", "O14 O03 na 16 mm (do 35)"),
         ("O15_elipsa_12mm_do35", "O15 O03 na 12 mm (do 35)"), ("O16_elipsa_waska_do35", "O16 elipsa 180x70 (do 35)"),
         ("O17_elipsa_20mm_do35", "O17 O03 na 20 mm (do 35)"),
         ("O18_waska_14mm", "O18 elipsa 180x70 na 14 mm (do 35)"), ("O19_elipsa_14mm", "O19 O03 na 14 mm (do 35)"),
         ("O20_waska_14mm_powtorka", "O20 = O18 powtorka (rozrzut)"),
         ("O21_O18_kat2", "O21 O18 kat +2"), ("O22_O18_kat4", "O22 O18 kat +4"), ("O23_O18_NACA2402", "O23 O18 NACA 2402")]


def load(c):
    f = glob.glob(f"{R}\\30_PRZEBIEGI\\{c}\\TF7_HISTORIA.csv")
    if not f: return []
    rows = {}
    for r in csv.DictReader(open(f[0])):
        try: rows[round(float(r["time_s"]), 5)] = r
        except Exception: pass
    out = [rows[k] for k in sorted(rows)]
    return [r for r in out if float(r["time_s"]) <= 3.4]


g = lambda r, k: float(r[k]) if r.get(k) not in (None, "") else float("nan")
Lfoil = lambda r: (g(r, "TF7_Lp_foil_dyn") + g(r, "TF7_Ls_foil")) / 9.81
Lpl = lambda r: g(r, "TF7_L_plytka") / 9.81 if r.get("TF7_L_plytka") not in (None, "") else 0.0


def crit(rows):
    if not rows: return {}
    fmax, kfy, kw = 0, None, None
    for r in rows:
        a, fy, w = g(r, "TF7_skret_deg"), g(r, "TF7_Fy_total"), g(r, "TF7_went_strut_pct")
        if a < 3: continue
        fmax = max(fmax, fy)
        if kfy is None and fy < 0.7 * fmax and fmax > 50: kfy = a
        if kw is None and w > 30: kw = a
    near = [r for r in rows if abs(g(r, "TF7_skret_deg") - 10) < 0.5]
    m = lambda f: sum(f(r) for r in near) / len(near) if near else float("nan")
    return {"kat_zalamania_Fy": kfy, "kat_went30": kw, "Fy_max": round(fmax, 1), "t_koniec": round(g(rows[-1], "time_s"), 3),
            "kat_koniec": round(g(rows[-1], "TF7_skret_deg"), 2),
            "D_plytki_10deg": round(m(lambda r: g(r, "TF7_D_plytka") if r.get("TF7_D_plytka") else 0.0), 2),
            "L_plytki_10deg_kg": round(m(Lpl), 2), "L_plat_10deg_kg": round(m(Lfoil), 2), "D_calk_10deg": round(m(lambda r: g(r, "TF7_D_total")), 1)}


data = {c: load(c) for c, _ in CASES}
with open(O + r"\opt_kryteria.csv", "w", newline="") as fh:
    w = csv.writer(fh); keys = ["kat_zalamania_Fy", "kat_went30", "Fy_max", "D_plytki_10deg", "L_plytki_10deg_kg", "L_plat_10deg_kg", "D_calk_10deg", "t_koniec", "kat_koniec"]
    w.writerow(["przypadek"] + keys)
    for c, lab in CASES:
        k = crit(data[c]); print(f"{lab:28s}", {x: k.get(x) for x in keys})
        w.writerow([lab] + [k.get(x) for x in keys])
with open(O + r"\opt_tabela.csv", "w", newline="") as fh:
    w = csv.writer(fh); w.writerow(["przypadek", "deg", "went_pct", "Fy", "L_plat_kg", "L_plat+pl_kg", "D", "D_plytki"])
    for c, lab in CASES:
        b = {}
        for r in data[c]: b.setdefault(round(g(r, "TF7_skret_deg")), []).append(r)
        for k in sorted(b):
            rs = b[k]; m = lambda f: sum(f(r) for r in rs) / len(rs)
            w.writerow([lab, k] + [round(x, 2) for x in (m(lambda r: g(r, "TF7_went_strut_pct")), m(lambda r: g(r, "TF7_Fy_total")), m(Lfoil),
                                                       m(lambda r: Lfoil(r) + Lpl(r)), m(lambda r: g(r, "TF7_D_total")),
                                                       m(lambda r: g(r, "TF7_D_plytka") if r.get("TF7_D_plytka") else 0.0))])
fig, ax = plt.subplots(4, 1, figsize=(10, 14), sharex=True)
sty = {"Y04": ("#555555", "-", 2.2), "Y05": ("#1f6fd1", "-", 2.2)}
cols = ["#888888", "#e8792b", "#2ca02c", "#9467bd", "#d62728"]
for i, (c, lab) in enumerate(CASES):
    rows = data[c]
    if not rows: continue
    col, ls, lw = sty.get(lab[:3], (cols[(i - 2) % len(cols)], "--", 1.5))
    x = [g(r, "TF7_skret_deg") for r in rows]
    for a, (f, t) in zip(ax, [(lambda r: g(r, "TF7_went_strut_pct"), "Wentylacja strutu [%]"), (lambda r: g(r, "TF7_Fy_total"), "Sila boczna Fy [N]"),
                             (lambda r: Lfoil(r) + Lpl(r), "Sila nosna plat + plytka [kg]"), (lambda r: g(r, "TF7_D_total"), "Opor calkowity [N]")]):
        a.plot(x, [f(r) for r in rows], color=col, ls=ls, lw=lw, label=lab); a.set_ylabel(t, fontsize=9); a.grid(alpha=0.3)
ax[0].legend(fontsize=8, ncol=2); ax[-1].set_xlabel("kat skretu [deg]"); ax[-1].set_xlim(0, 30)
fig.suptitle("Optymalizacja plytki antywentylacyjnej: model zgrubiony (O, linie przerywane) vs pelny (Y04/Y05)", fontsize=11)
fig.tight_layout(); fig.savefig(O + r"\opt_vs_Y04Y05.png", dpi=120); print("ok", O)
