r"""Plytka antywentylacyjna o rzucie eliptycznym i przekroju NACA 00tt -> zamkniety STL (ASCII), uklad laboratorium [m].
Rzut z gory: elipsa o osiach Lx (wzdluz cieciwy strutu) i Wy (w poprzek), srodek w (x_c, 0).
Kazdy przekroj y = const: profil NACA 00tt o lokalnej cieciwie c(y) = Lx*sqrt(1-(2y/Wy)^2), krawedz natarcia od strony +x (naplyw z +x),
grubosc wzgledna stala -> grubosc maleje do zera na obwodzie elipsy (ostra krawedz dookola).
Uzycie: python plytka_elipsa_stl.py out.stl x_c Lx Wy tc z_c
"""
import sys, math
out = sys.argv[1]; xc, Lx, Wy, tc, zc = map(float, sys.argv[2:7])
# 2026-11-24: opcjonalnie kat ustawienia [deg] (dodatni = krawedz natarcia, od strony +x, w gore; obrot wokol srodka xc, zc)
# i pochylenie profilu NACA 4-cyfrowe: m (np. 0.02), p (np. 0.4) -> NACA m p tc, np. 2402
inc = math.radians(float(sys.argv[7])) if len(sys.argv) > 7 else 0.0
cm = float(sys.argv[8]) if len(sys.argv) > 8 else 0.0
cp = float(sys.argv[9]) if len(sys.argv) > 9 else 0.4


def camber(u):
    if cm == 0: return 0.0
    return cm / cp ** 2 * (2 * cp * u - u * u) if u < cp else cm / (1 - cp) ** 2 * ((1 - 2 * cp) + 2 * cp * u - u * u)
NX, NY = 100, 121


def naca(u):  # polowa grubosci / cieciwa (tc ustalone), zamknieta TE
    return 5 * tc * (0.2969 * math.sqrt(u) - 0.1260 * u - 0.3516 * u ** 2 + 0.2843 * u ** 3 - 0.1036 * u ** 4)


us = [0.5 * (1 - math.cos(math.pi * i / (NX - 1))) for i in range(NX)]
vs = [math.sin(-math.pi / 2 + math.pi * j / (NY - 1)) for j in range(NY)]       # -1..1, gesto przy koncach


def P(i, j, s):
    y = Wy / 2 * vs[j]; c = Lx * math.sqrt(max(0.0, 1 - vs[j] ** 2))
    dx = c / 2 - c * us[i]; dz = c * (camber(us[i]) + s * naca(us[i]))
    return (xc + dx * math.cos(inc) - dz * math.sin(inc), y, zc + dz * math.cos(inc) + dx * math.sin(inc))


tri = []
for i in range(NX - 1):
    for j in range(NY - 1):
        for s in (1, -1):
            a, b, cc, d = P(i, j, s), P(i + 1, j, s), P(i + 1, j + 1, s), P(i, j + 1, s)
            tri += [(a, b, cc), (a, cc, d)] if s > 0 else [(a, cc, b), (a, d, cc)]


def area_n(a, b, c):
    u = [b[k] - a[k] for k in range(3)]; v = [c[k] - a[k] for k in range(3)]
    n = [u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]]
    return n, math.sqrt(sum(x * x for x in n))


nt, A = 0, 0.0
with open(out, "w") as f:
    f.write("solid plytka_elipsa\n")
    for a, b, cc in tri:
        n, l = area_n(a, b, cc)
        if l < 1e-14: continue
        nt += 1; A += l / 2; n = [x / l for x in n]
        f.write(f" facet normal {n[0]:.6e} {n[1]:.6e} {n[2]:.6e}\n  outer loop\n")
        for p in (a, b, cc): f.write(f"   vertex {p[0]:.12e} {p[1]:.12e} {p[2]:.12e}\n")
        f.write("  endloop\n endfacet\n")
    f.write("endsolid plytka_elipsa\n")
print(f"ok {out}: {nt} trojkatow, pow. zwilzona {A*1e4:.1f} cm2 (prostokat 100x100: ~200 cm2), t_max {tc*Lx*1000:.2f} mm")
