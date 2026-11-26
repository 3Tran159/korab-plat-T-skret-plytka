r"""Wykres porownawczy Y04 (bez plytki) vs przypadek z plytka, w funkcji kata skretu (pelna historia, wygladzona 0,25 deg).
Uzycie: python wykres_Y04_Y05.py [przypadek] [prefiks] -> 40_WYNIKI\<prefiks>\<prefiks>_vs_Y04.png + tabela co 1 deg CSV"""
import sys, csv, glob, os
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
R = r"T:\KORAB_PLAT_T_SKRET7_20261118"
P = sys.argv[1] if len(sys.argv) > 1 else "Y05_plytka_rampa_0do45_30kmh_h1c_IDDES"
pre = sys.argv[2] if len(sys.argv) > 2 else "Y05"
out = f"{R}\\40_WYNIKI\\{pre}"; os.makedirs(out, exist_ok=True)


def load(case):
    rows = list(csv.DictReader(open(glob.glob(f"{R}\\30_PRZEBIEGI\\{case}\\TF7_HISTORIA.csv")[0])))
    d = {}
    for r in rows:
        try:
            k = round(float(r["TF7_skret_deg"]) * 4) / 4
            if float(r["time_s"]) > 3.4: k = 45.25   # odcinek po rampie (staly kat)
        except: continue
        d.setdefault(k, []).append(r)
    return d


def series(d, f):
    ks = sorted(k for k in d if k <= 45)
    ys = []
    for k in ks:
        v = [f(r) for r in d[k]]; v = [x for x in v if x == x]
        ys.append(sum(v) / len(v) if v else float("nan"))
    return ks, ys


g = lambda key: (lambda r: float(r[key]) if r.get(key) not in (None, "") else float("nan"))
Lfoil = lambda r: (float(r["TF7_Lp_foil_dyn"]) + float(r["TF7_Ls_foil"])) / 9.81
Lall = lambda r: Lfoil(r) + float(r["TF7_L_plytka"]) / 9.81
A, B = load("Y04_rampa_0do45_30kmh_h1c_IDDES"), load(P)
panels = [("Wentylacja strutu [% pow. pod lustrem]", g("TF7_went_strut_pct"), None),
          ("Sila boczna Fy [N]", g("TF7_Fy_total"), None),
          ("Sila nosna [kg]", Lfoil, Lall),
          ("Opor calkowity D [N]", g("TF7_D_total"), g("TF7_D_plytka")),
          ("Cp min na strucie [-]", g("TF7_Cp_min_strut"), None)]
fig, ax = plt.subplots(len(panels), 1, figsize=(9, 15), sharex=True)
for a, (t, f, f2) in zip(ax, panels):
    a.plot(*series(A, f), color="#555555", lw=1.6, label="Y04 bez plytki")
    a.plot(*series(B, f), color="#1f6fd1", lw=1.6, label=f"{pre} plytka")
    if f2 is not None:
        lab = "plat + plytka" if "nosna" in t else "opor plytki"
        a.plot(*series(B, f2), color="#1f6fd1", lw=1.2, ls="--", label=f"{pre} {lab}")
    a.set_ylabel(t, fontsize=9); a.grid(alpha=0.3); a.legend(fontsize=8, loc="best")
ax[-1].set_xlabel("kat skretu strutu [deg] (30 km/h, h = 75 mm, rampa 15 deg/s)")
fig.suptitle(f"Plat T KORAB: {pre} (plytka antywentylacyjna 20 mm pod lustrem) vs Y04", fontsize=11)
fig.tight_layout(); p = f"{out}\\{pre}_vs_Y04.png"; fig.savefig(p, dpi=130)
with open(f"{out}\\{pre}_vs_Y04_co1deg.csv", "w", newline="") as fh:
    w = csv.writer(fh); w.writerow(["deg", "went_Y04", f"went_{pre}", "Fy_Y04", f"Fy_{pre}", "L_Y04", f"Lplat_{pre}", f"Lplat+pl_{pre}", "D_Y04", f"D_{pre}", f"Dpl_{pre}", "Cpmin_Y04", f"Cpmin_{pre}"])
    for k in range(0, 46):
        a, b = A.get(float(k), []), B.get(float(k), [])
        if not a or not b: continue
        mm = lambda rs, f: sum(f(r) for r in rs) / len(rs)
        w.writerow([k] + [round(x, 2) for x in (mm(a, g("TF7_went_strut_pct")), mm(b, g("TF7_went_strut_pct")), mm(a, g("TF7_Fy_total")), mm(b, g("TF7_Fy_total")),
                    mm(a, Lfoil), mm(b, Lfoil), mm(b, Lall), mm(a, g("TF7_D_total")), mm(b, g("TF7_D_total")), mm(b, g("TF7_D_plytka")),
                    mm(a, g("TF7_Cp_min_strut")), mm(b, g("TF7_Cp_min_strut")))])
print("ok", p)
