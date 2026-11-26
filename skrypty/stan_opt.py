r"""Stan przypadkow optymalizacji plytki: t, kat, wentylacja strutu co ~0,5 deg + ostatni wiersz. Uzycie: python stan_opt.py [przypadek...]"""
import sys, csv, glob, os
R = r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI"
cs = sys.argv[1:] or sorted(os.path.basename(p) for p in glob.glob(R + r"\O0*"))
for c in cs:
    f = f"{R}\\{c}\\TF7_HISTORIA.csv"
    if not os.path.exists(f): print(c, "brak historii"); continue
    rows = list(csv.DictReader(open(f)))
    if not rows: print(c, "pusta"); continue
    st = open(f"{R}\\{c}\\RUN_STATUS.json").read()[-80:] if os.path.exists(f"{R}\\{c}\\RUN_STATUS.json") else ""
    out, last = [], None
    for r in rows:
        k = round(float(r["TF7_skret_deg"]) * 2) / 2
        if k != last:
            out.append(f"{k:g}:{float(r['TF7_went_strut_pct']):.1f}%/Fy{float(r['TF7_Fy_total']):.0f}"); last = k
    r = rows[-1]
    print(f"{c}: t={float(r['time_s']):.3f} wall={float(r['wall_s'])/3600:.2f} h  | " + "  ".join(out))
