r"""Linia wody na strucie (t = 2 s): po obu stronach, wzdluz cieciwy. Glebokosc = z (najnizszy punkt sciany z powietrzem > 0,5) - z lustra."""
import csv, numpy as np
from pathlib import Path
import matplotlib; matplotlib.use("Agg")
import matplotlib.pyplot as plt
D = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI\Y01_skret7_30kmh_h1c_IDDES\post_linia_wody"); ZS = 0.05879; Q = 0.5 * 997.561 * 8.333 ** 2
r = list(csv.reader(open(D / "STRUT_SCIANA.csv", encoding="utf8"))); a = np.array([[float(v) for v in x] for x in r[1:]])
air, ny, p, x, y, z = a.T
m = (z > ZS - 0.08) & (z < ZS + 0.15)           # pas wokol lustra
xmin, xmax = x[m & (z < ZS)].min(), x[m & (z < ZS)].max(); c = xmax - xmin
print(f"strut przy lustrze: x {xmin:.4f} .. {xmax:.4f}, cieciwa ~{c*1000:.0f} mm (krawedz natarcia = max x)")
bins = np.linspace(xmin, xmax, 11)
res = {}
for side, sel in (("strona +y (normalna +y)", ny > 0.3), ("strona -y (normalna -y)", ny < -0.3)):
    zz = []
    for i in range(10):
        b = m & sel & (x >= bins[i]) & (x < bins[i + 1])
        za = z[b & (air > 0.5)]; zw = z[b & (air < 0.5)]
        deep = za.min() - ZS if za.size else np.nan            # najglebszy punkt sciany w powietrzu
        top_w = zw.max() - ZS if zw.size else np.nan             # najwyzej zwilzony punkt
        cpn = (p[b & (z < ZS - 0.01) & (z > ZS - 0.03)] - 101325 - 997.561 * 9.81 * (ZS - z[b & (z < ZS - 0.01) & (z > ZS - 0.03)])) / Q
        zz.append(((xmax - (bins[i] + bins[i + 1]) / 2) / c, deep * 1000, top_w * 1000, cpn.mean() if cpn.size else np.nan))
    res[side] = np.array(zz)
    print(side); print("  x/c   najglebsze powietrze [mm]   najwyzej woda [mm]   Cp 10-30 mm pod lustrem")
    for row in zz: print(f"  {row[0]:.2f}   {row[1]:8.1f}   {row[2]:8.1f}   {row[3]:6.3f}")
fig, ax = plt.subplots(figsize=(8, 4.5))
for k, v in res.items():
    l, = ax.plot(v[:, 0], v[:, 1], "o-", label=k + ": najglebsze powietrze")
    ax.plot(v[:, 0], v[:, 2], "s--", color=l.get_color(), label=k + ": najwyzej woda")
ax.axhline(0, c="k", lw=1); ax.set_xlabel("x/c strutu (0 = krawedz natarcia)"); ax.set_ylabel("wzgledem lustra wody spokojnej [mm]"); ax.grid(alpha=.3); ax.legend(fontsize=7)
ax.set_title("Linia wody na strucie, t = 2 s (skret 7 deg, 30 km/h, h = 75 mm; strut zanurzony ~70 mm)", fontsize=9)
fig.tight_layout(); fig.savefig(r"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\Y01_linia_wody_strut.png", dpi=130)
