r"""Tepa plytka prostopadloscienna (jak Y05) jako zamkniety STL [m]. Blok SimpleBlockPart po obrocie (skret != 0) znika z Subtract,
STL obraca sie poprawnie. Uzycie: python plytka_prostopadloscian_stl.py out.stl x0 x1 y0 y1 z0 z1"""
import sys
out = sys.argv[1]; x0, x1, y0, y1, z0, z1 = map(float, sys.argv[2:8])
P = [(x, y, z) for z in (z0, z1) for y in (y0, y1) for x in (x0, x1)]   # indeks = x + 2y + 4z
F = [(0, 2, 3, 1), (4, 5, 7, 6), (0, 1, 5, 4), (2, 6, 7, 3), (0, 4, 6, 2), (1, 3, 7, 5)]   # normalne na zewnatrz
with open(out, "w") as f:
    f.write("solid blok\n")
    for a, b, c, d in F:
        for t in ((a, b, c), (a, c, d)):
            f.write(" facet normal 0 0 0\n  outer loop\n")
            for i in t: f.write("   vertex %.9e %.9e %.9e\n" % P[i])
            f.write("  endloop\n endfacet\n")
    f.write("endsolid blok\n")
print("ok", out)
