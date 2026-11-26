r"""Model optymalizacyjny plytki antywentylacyjnej (21.11.2026): ta sama geometria T, siatka zgrubiona (mesh_scale=2 -> ok. 1/8 komorek),
CPU (np=3), skret staly 15 deg do t=0,4 s, potem 15 deg/s do 30 deg (t=1,4 s), koniec 1,5 s -> okno progu wentylacji (Y04 ~25 deg, Y05 ~22,5 deg).
Kalibracja: O00 bez plytki (vs Y04), O01 prostokat Y05 (vs Y05). Warianty: elipsy NACA (plytka_elipsa_stl.py), grubosc 2 mm.
Strut: krawedz natarcia x = 0,1955, os skretu (1/4 cieciwy) x = 0,16 -> cieciwa ~0,142 m, krawedz splywu x ~0,054 (naplyw od +x).
Uzycie: python opt_plytka_przypadki.py  -> tworzy katalogi 30_PRZEBIEGI\O0x_* z control.properties i STL w 10_GEOMETRIA\opt\
"""
import subprocess, sys
from pathlib import Path
R = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118"); PY = r"T:\NTNU_GODO_CONTROL_20261109\.venv\Scripts\python.exe"
G = R / "10_GEOMETRIA" / "opt"; G.mkdir(parents=True, exist_ok=True)
BASE = """U=8.333
rot_deg=7.42
yaw_deg=15.0
ramp_to_deg=30.0
ramp_t0_s=0.4
ramp_T_s=1.0
ramp_mode=mesh
yaw_axis_x=0.16
h_m=0.07519
turbulence=iddes
max_cells=3600000
strut_min_surf_m=0.0003
mesh_scale=2.0
dt_s=4e-4
inner=5
t_end_s=1.5
chunk=5
ckpt_every=250
anim_every=25
history_every=1000000
q_iso=2e4
h_scenes=false
deadline=2026-11-24T12:00
gpu=false
np=3
"""
ZC = 0.05879 - 0.02
# nazwa: (opis, None | 'rect' | (xc, Lx, Wy))
V = {
    "O00_bez_plytki": ("kalibracja, bez plytki", None),
    "O01_prostokat_Y05": ("kalibracja, prostokat 100x100x3 jak Y05", "rect"),
    "O02_elipsa_Y06": ("elipsa 100x100 jak Y06, x 0,0955..0,1955", (0.1455, 0.10, 0.10)),
    "O03_elipsa_caly_strut": ("elipsa 180x100, x 0,03..0,21 (caly strut + 24 mm za splywem)", (0.12, 0.18, 0.10)),
    "O04_elipsa_caly_strut_szeroka": ("elipsa 180x140, x 0,03..0,21", (0.12, 0.18, 0.14)),
    "O05_ogon": ("elipsa 125x100 od polowy cieciwy do 50 mm za splywem, x 0,005..0,13", (0.0675, 0.125, 0.10)),
}
for n, (opis, g) in V.items():
    d = R / "30_PRZEBIEGI" / n; d.mkdir(parents=True, exist_ok=True)
    txt = BASE + f"name={n[:3]}\nopis={opis.replace(' ', '_')}\n"
    if g == "rect":
        txt += "plate_dz=0.02\nplate_L=0.10\nplate_W=0.10\nplate_t=0.003\nplate_x_le=0.1955\nplate_cell_m=0.0015\n"
    elif g:
        xc, L, W = g; stl = G / f"{n}.stl"
        subprocess.run([PY, str(R / "00_START" / "plytka_elipsa_stl.py"), str(stl), str(xc), str(L), str(W), str(0.002 / L), str(ZC)], check=True)
        txt += (f"plate_dz=0.02\nplate_L={L}\nplate_W={W}\nplate_t=0.002\nplate_x_le={xc + L / 2:.4f}\nplate_stl={stl.as_posix()}\n"
                "plate_cell_m=0.001\nplate_min_surf_m=0.0002\nplate_vol_m=0.0015\nplate_zone_dz=0.005\n")
    (d / "control.properties").write_text(txt)
    print(n, opis)
