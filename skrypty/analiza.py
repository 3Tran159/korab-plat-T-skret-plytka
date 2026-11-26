r"""Analiza Y01: okna 0,5 s, wentylacja, wykres zbiorczy -> 40_WYNIKI"""
import csv, json
from pathlib import Path
import numpy as np
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
D = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI\Y01_skret7_30kmh_h1c_IDDES"); OUT = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI"); OUT.mkdir(exist_ok=True)
rows = list(csv.DictReader(open(D / "TF7_HISTORIA.csv", encoding="utf8")))
t = np.array([float(r["time_s"]) for r in rows])
def col(k): return np.array([float(r[k]) if r.get(k) not in (None, "", "NaN") else np.nan for r in rows])
K = ["TF7_L_kg", "TF7_D_foil", "TF7_D_strut", "TF7_Dp_strut", "TF7_D_total", "TF7_Fy_strut", "TF7_Fy_foil", "TF7_Fy_total", "TF7_Mz_os_skretu", "TF7_Mx_przechyl",
     "TF7_went_strut_pct", "TF7_went_strut_m2", "TF7_went_foil_m2", "TF7_strut_pod_lustrem_m2", "TF7_Cp_min_strut", "TF7_Cp_min_foil", "TF7_max_woda_nad_lustrem_m", "TF7_interfaceCFL", "TF7_yplus_mean"]
res = {"n_rows": len(rows), "t_end": float(t[-1])}
for a, b in ((0.0, 0.5), (0.5, 1.0), (1.0, 1.5), (1.5, 2.01)):
    m = (t >= a) & (t < b); w = {}
    for k in K:
        v = col(k)[m]; w[k] = {"mean": float(np.nanmean(v)), "min": float(np.nanmin(v)), "max": float(np.nanmax(v)), "std": float(np.nanstd(v))}
    res[f"{a:.1f}-{min(b,2.0):.1f}s"] = w
went = col("TF7_went_strut_pct"); res["went_strut_pct_max"] = float(np.nanmax(went)); res["went_strut_pct_max_t"] = float(t[np.nanargmax(went)])
res["went_foil_m2_max"] = float(np.nanmax(col("TF7_went_foil_m2")))
(OUT / "analiza_Y01.json").write_text(json.dumps(res, indent=1))
fig, ax = plt.subplots(3, 2, figsize=(13, 10), sharex=True)
def p(a, ks, title, yl):
    for k in ks: a.plot(t, col(k), lw=1, label=k.replace("TF7_", ""))
    a.set_title(title); a.set_ylabel(yl); a.grid(alpha=.3); a.legend(fontsize=8)
p(ax[0, 0], ["TF7_L_kg"], "Sila nosna", "kg")
p(ax[0, 1], ["TF7_Fy_strut", "TF7_Fy_foil", "TF7_Fy_total"], "Sila boczna", "N")
p(ax[1, 0], ["TF7_D_foil", "TF7_D_strut", "TF7_D_total"], "Opor", "N")
p(ax[1, 1], ["TF7_went_strut_pct"], "Wentylacja: % zanurzonego strutu pokryty powietrzem", "%")
p(ax[2, 0], ["TF7_Cp_min_strut", "TF7_Cp_min_foil"], "Cp min (najsilniejsze ssanie)", "-")
p(ax[2, 1], ["TF7_Mz_os_skretu", "TF7_Mx_przechyl"], "Momenty: skret (os strutu) i przechyl", "Nm")
for a in ax[2]: a.set_xlabel("czas [s]")
fig.suptitle("Plat T KORAB, strut skrecony 7 deg, 30 km/h, h = 75 mm, VOF + SST k-omega IDDES"); fig.tight_layout(); fig.savefig(OUT / "Y01_historia.png", dpi=120)
for k, v in res.items():
    if isinstance(v, dict):
        print(k, {kk: round(vv["mean"], 4) for kk, vv in v.items()})
print({k: v for k, v in res.items() if not isinstance(v, dict)})
for k in ["TF7_Fy_total", "TF7_L_kg", "TF7_went_strut_pct", "TF7_Cp_min_strut"]:
    w = res["1.5-2.0s"][k]; print(k, "1.5-2 s min/max/std", round(w["min"], 3), round(w["max"], 3), round(w["std"], 3))
