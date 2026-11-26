r"""Y04 (rampa skretu 0->45 deg, obrot siatki): tabela co 1 deg i wykresy w funkcji kata.
Wejscie: 30_PRZEBIEGI\Y04...\TF7_HISTORIA.csv (wiersze zdublowane po wznowieniach -> bierzemy ostatni dla danego t).
Wyjscie: 40_WYNIKI\Y04\Y04_tabela_co_1deg.csv, Y04_sily_vs_kat.png, Y04_wentylacja_vs_kat.png
"""
import csv
from pathlib import Path
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

C = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI\Y04_rampa_0do45_30kmh_h1c_IDDES")
O = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\Y04"); O.mkdir(parents=True, exist_ok=True)
rows = {}
for r in csv.DictReader(open(C / "TF7_HISTORIA.csv", encoding="utf8")):
    # raport TF7_L_kg ma abs() na czesci cisnieniowej -> po zmianie znaku (>~39 deg) klamie; liczymy ze znakiem
    try:
        r["TF7_L_kg"] = str((float(r["TF7_Lp_foil_dyn"]) + float(r["TF7_Ls_foil"])) / 9.81)
        rows[round(float(r["time_s"]), 4)] = r
    except Exception: pass
T = sorted(rows)
K = ["TF7_L_kg", "TF7_Fy_total", "TF7_D_total", "TF7_Cp_min_strut", "TF7_Cp_min_foil", "TF7_went_strut_pct", "TF7_went_foil_m2",
     "TF7_Mz_os_skretu", "TF7_Mx_przechyl", "TF7_max_woda_nad_lustrem_m"]
b = {}
for t in T:
    if t < 0.3: continue
    r = rows[t]; a = float(r["TF7_skret_deg"])
    k = max(0, int(a + 0.5)) if t >= 0.4 else 0
    b.setdefault(k, []).append(r)
tab = []
for k in sorted(b):
    L = b[k]; tab.append([k] + [sum(float(x[n]) for x in L) / len(L) for n in K])
with open(O / "Y04_tabela_co_1deg.csv", "w", newline="", encoding="utf8") as f:
    w = csv.writer(f, delimiter=";")
    w.writerow(["skret_deg", "L_kg", "sila_boczna_N", "opor_N", "Cp_min_strut", "Cp_min_plat", "wentylacja_strutu_pct", "powietrze_plat_cm2",
                "Mz_skret_Nm", "Mx_przechyl_Nm", "woda_nad_lustrem_m"])
    for r in tab: w.writerow([r[0]] + [f"{v:.4g}" for v in r[1:7]] + [f"{r[7]*1e4:.3g}"] + [f"{v:.4g}" for v in r[8:]])

# przebiegi czasowe (pelna rozdzielczosc) w funkcji kata
ang = [float(rows[t]["TF7_skret_deg"]) for t in T if t >= 0.4]
g = lambda n: [float(rows[t][n]) for t in T if t >= 0.4]
fig, ax = plt.subplots(3, 1, figsize=(11, 11), sharex=True)
ax[0].plot(ang, [v * 9.81 for v in g("TF7_L_kg")], label="sila nosna plata", lw=1)
ax[0].plot(ang, g("TF7_Fy_total"), label="sila boczna", lw=1)
ax[0].plot(ang, g("TF7_D_total"), label="opor", lw=1)
ax[0].set_ylabel("sila [N]"); ax[0].legend(); ax[0].grid(alpha=.3)
ax[0].set_title("Y04: plat T KORAB, 30 km/h, h = 75 mm, skret strutu 0->45 deg w 3 s (15 deg/s), VOF + SST k-omega IDDES")
ax[1].plot(ang, g("TF7_went_strut_pct"), color="tab:red", lw=1, label="wentylacja strutu [% zanurzonej pow.]")
ax1b = ax[1].twinx(); ax1b.plot(ang, [v * 1e4 for v in g("TF7_went_foil_m2")], color="tab:purple", lw=1, label="powietrze na placie [cm2]")
ax[1].set_ylabel("wentylacja strutu [%]"); ax1b.set_ylabel("powietrze na placie [cm2]"); ax[1].grid(alpha=.3)
ax[1].legend(loc="upper left"); ax1b.legend(loc="center left")
ax[2].plot(ang, g("TF7_Cp_min_strut"), lw=1, label="Cp min strut"); ax[2].plot(ang, g("TF7_Cp_min_foil"), lw=1, label="Cp min plat")
ax2b = ax[2].twinx(); ax2b.plot(ang, g("TF7_Mz_os_skretu"), color="k", lw=1, label="moment skretu Mz [Nm]")
ax[2].set_ylabel("Cp min [-]"); ax2b.set_ylabel("Mz [Nm]"); ax[2].grid(alpha=.3); ax[2].legend(loc="lower left"); ax2b.legend(loc="lower right")
ax[2].set_xlabel("kat skretu strutu wzgledem naplywu [deg]")
for a in ax:
    a.axvline(20, color="gray", ls="--", lw=.8); a.axvline(25, color="red", ls="--", lw=.8)
ax[0].text(20.2, ax[0].get_ylim()[1] * .9, "oderwanie\n~20 deg", fontsize=8); ax[0].text(25.2, ax[0].get_ylim()[1] * .75, "wentylacja\n~25 deg", fontsize=8, color="red")
fig.tight_layout(); fig.savefig(O / "Y04_sily_wentylacja_vs_kat.png", dpi=130)
print("ok", len(tab), "wierszy; t max", T[-1])
for r in tab: print(r[0], " ".join(f"{v:.3g}" for v in r[1:]))
