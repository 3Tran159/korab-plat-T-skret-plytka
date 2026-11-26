r"""Y04: sklada klatki ANIMACJE\0x_* w MP4 (H.264, 25 kl/s) z naniesionym czasem symulacji i katem skretu.
Klatki nazwane numerem kroku (dt = 4e-4 s) -> czas i kat liczone z nazwy pliku (odporne na luki).
Wyjscie: 40_WYNIKI\Y04\FILMY\Y04_<scena>.mp4.  Uzycie: python filmy_Y04.py [katalog_ANIMACJE] [prefiks]
"""
import sys, re, subprocess
from pathlib import Path
sys.path.insert(0, r"T:\KORAB_PLAT_T_SKRET7_20261118\00_START\_py")
import imageio_ffmpeg
FF = imageio_ffmpeg.get_ffmpeg_exe()
A = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI\Y04_rampa_0do45_30kmh_h1c_IDDES\ANIMACJE")
PRE = sys.argv[2] if len(sys.argv) > 2 else "Y04"
O = Path(rf"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\{PRE}\FILMY"); O.mkdir(parents=True, exist_ok=True)
DT, T0, TR, B = 4e-4, 0.4, 3.0, 45.0
FONT = "C\\:/Windows/Fonts/arial.ttf"


def kat(t): return B * min(max((t - T0) / TR, 0.0), 1.0)


for d in sorted(p for p in A.iterdir() if p.is_dir()):
    fr = sorted(d.glob("*.png"), key=lambda f: int(re.findall(r"(\d+)\.png$", f.name)[0]))
    if len(fr) < 5: print("pomijam", d.name, len(fr)); continue
    tmp = d / "_napisy"; tmp.mkdir(exist_ok=True)
    lst = d / "_lista.txt"; lines = []
    for f in fr:
        k = int(re.findall(r"(\d+)\.png$", f.name)[0]); t = k * DT
        txt = tmp / (f.stem + ".txt"); txt.write_text(f"t = {t:5.2f} s    skret = {kat(t):4.1f} deg", encoding="utf8")
        lines.append(f"file '{f.as_posix()}'\nduration 0.04\n")
    lst.write_text("".join(lines), encoding="utf8")
    # napis per klatka: drawtext z textfile nie zmienia sie w czasie, wiec liczymy z numeru klatki n
    k0 = int(re.findall(r"(\d+)\.png$", fr[0].name)[0]); dk = (int(re.findall(r"(\d+)\.png$", fr[-1].name)[0]) - k0) / max(len(fr) - 1, 1)
    tex = f"({k0}+n*{dk})*{DT}"
    ang = f"{B}*clip(({tex}-{T0})/{TR}\\,0\\,1)"
    dt_ = (f"drawtext=fontfile='{FONT}':fontsize=34:fontcolor=white:box=1:boxcolor=black@0.55:boxborderw=8:x=20:y=20:"
           f"text='t = %{{eif\\:trunc({tex})\\:d}}.%{{eif\\:mod(trunc({tex}*100)\\,100)\\:d\\:2}} s    skret = "
           f"%{{eif\\:trunc({ang})\\:d}}.%{{eif\\:mod(trunc({ang}*10)\\,10)\\:d}} deg'")
    out = O / f"{PRE}_{d.name}.mp4"
    r = subprocess.run([FF, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst),
                        "-vf", f"scale=1280:-2,{dt_},format=yuv420p", "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-r", "25", str(out)],
                       capture_output=True, text=True)
    print(d.name, len(fr), "klatek ->", out.name, r.returncode, r.stderr[-300:])
