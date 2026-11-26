r"""Porownanie Y04 (bez plytki) i Y05/Y06 (plytka) co 1 stopien skretu. Uzycie: python porownaj_Y04_Y05.py [przypadek_plytki] [od] [do]"""
import sys, csv
R = r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI"
B = "Y04_rampa_0do45_30kmh_h1c_IDDES"
P = sys.argv[1] if len(sys.argv) > 1 else "Y05_plytka_rampa_0do45_30kmh_h1c_IDDES"
a0 = float(sys.argv[2]) if len(sys.argv) > 2 else 0; a1 = float(sys.argv[3]) if len(sys.argv) > 3 else 45
import glob


def load(case):
    f = glob.glob(f"{R}\\{case}\\TF7_HISTORIA.csv")[0]
    rows = list(csv.DictReader(open(f)))
    out = {}
    for r in rows:
        try: k = round(float(r["TF7_skret_deg"]))
        except: continue
        out.setdefault(k, []).append(r)
    return out


def m(rs, key, sign=1):
    v = [float(r[key]) for r in rs if r.get(key) not in (None, "")]
    return sign * sum(v) / len(v) if v else float("nan")


def L(rs):  # sila nosna platu ze znakiem [kg] (L_kg ma abs)
    return (m(rs, "TF7_Lp_foil_dyn") + m(rs, "TF7_Ls_foil")) / 9.81


A, Bp = load(B), load(P)
print(f"{'deg':>4} | {'went% Y04':>9} {'plyt':>6} | {'Fy Y04':>7} {'plyt':>6} | {'L kg Y04':>8} {'plyt':>6} {'+pl':>6} | {'D Y04':>6} {'plyt':>6} {'Dpl':>5} | {'Cpmin Y04':>9} {'plyt':>6}")
for k in range(int(a0), int(a1) + 1):
    if k not in Bp: continue
    a, b = A.get(k, []), Bp[k]
    Lpl = m(b, "TF7_L_plytka") / 9.81
    f = lambda rs, key: m(rs, key) if rs else float("nan")
    print(f"{k:4d} | {f(a,'TF7_went_strut_pct'):9.2f} {f(b,'TF7_went_strut_pct'):6.2f} | {f(a,'TF7_Fy_total'):7.1f} {f(b,'TF7_Fy_total'):6.1f} | "
          f"{(L(a) if a else float('nan')):8.1f} {L(b):6.1f} {L(b)+Lpl:6.1f} | {f(a,'TF7_D_total'):6.1f} {f(b,'TF7_D_total'):6.1f} {f(b,'TF7_D_plytka'):5.1f} | "
          f"{f(a,'TF7_Cp_min_strut'):9.2f} {f(b,'TF7_Cp_min_strut'):6.2f}")
