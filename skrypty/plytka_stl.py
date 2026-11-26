r"""Plytka antywentylacyjna o oplywowym przekroju -> zamkniety STL (ASCII) w ukladzie laboratorium [m].
Przekroj wzdluz cieciwy: symetryczny NACA 00tt z zamknieta krawedzia splywu (wsp. -0.1036), grubosc maks. t.
Konce w rozpietosci: grubosc maleje eliptycznie do 0 na ostatnich e metrach (ostre, zaokraglone w rzucie z boku).
Uzycie: python plytka_stl.py out.stl x_le chord span t z_c [e]
  x_le - polozenie krawedzi natarcia (naplyw z +x, wiec cieciwa biegnie do x_le-chord)
"""
import sys, math
out, xle, c, W, t, zc = sys.argv[1], *map(float, sys.argv[2:7])
e = float(sys.argv[7]) if len(sys.argv) > 7 else 0.010
NX, NY = 120, 81


def naca(xc, tc):  # polowa grubosci / cieciwa, zamknieta TE
    return 5 * tc * (0.2969 * math.sqrt(xc) - 0.1260 * xc - 0.3516 * xc ** 2 + 0.2843 * xc ** 3 - 0.1036 * xc ** 4)


xs = [0.5 * (1 - math.cos(math.pi * i / (NX - 1))) for i in range(NX)]          # 0..1, zageszczone przy krawedziach
ys = [-W / 2 + W * j / (NY - 1) for j in range(NY)]
# zageszczenie przy koncach (eliptyczne zaokraglenie)
ys = [math.copysign(W / 2 * (1 - (1 - abs(2 * y / W)) ** 1.6), y) for y in ys]


def half(i, j):
    y = ys[j]; d = abs(y) - (W / 2 - e)
    f = math.sqrt(max(0.0, 1 - (d / e) ** 2)) if d > 0 else 1.0
    return c * naca(xs[i], t / c) * f


P = lambda i, j, s: (xle - c * xs[i], ys[j], zc + s * half(i, j))
tri = []
for i in range(NX - 1):
    for j in range(NY - 1):
        for s in (1, -1):
            a, b, cc, d = P(i, j, s), P(i + 1, j, s), P(i + 1, j + 1, s), P(i, j + 1, s)
            if s > 0: tri += [(a, b, cc), (a, cc, d)]
            else: tri += [(a, cc, b), (a, d, cc)]


def nrm(a, b, cc):
    u = [b[k] - a[k] for k in range(3)]; v = [cc[k] - a[k] for k in range(3)]
    n = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]]
    l = math.sqrt(sum(x * x for x in n)) or 1.0
    return [x / l for x in n]


with open(out, "w") as f:
    f.write("solid plytka\n")
    for a, b, cc in tri:
        if a == b or b == cc or a == cc: continue
        n = nrm(a, b, cc)
        f.write(f" facet normal {n[0]:.6e} {n[1]:.6e} {n[2]:.6e}\n  outer loop\n")
        for p in (a, b, cc): f.write(f"   vertex {p[0]:.7e} {p[1]:.7e} {p[2]:.7e}\n")
        f.write("  endloop\n endfacet\n")
    f.write("endsolid plytka\n")
print("ok", out, len(tri), "trojkatow; t_max", t, "m; LE r ~", round(1.1019 * (t / c) ** 2 * c * 1000, 3), "mm")
