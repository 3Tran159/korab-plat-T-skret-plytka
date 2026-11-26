r"""Filmy MP4 z klatek ANIMACJE\0x_* dowolnego przypadku; napis: czas i kat skretu odczytane z TF7_HISTORIA.csv przypadku
(interpolacja po czasie) -> dziala dla kazdej rampy (Y04/Y05/Y07, dwuetapowe O0x, powrotna Y08).
Klatki nazwane numerem kroku; czas = krok * dt_s (z control.properties). Uzycie:
  python filmy_przypadek.py <przypadek w 30_PRZEBIEGI> <prefiks> [katalog_wyjscia]
Wyjscie domyslnie 40_WYNIKI\<prefiks>\FILMY\<prefiks>_<scena>.mp4
"""
import sys, re, csv, subprocess, bisect
from pathlib import Path
sys.path.insert(0, r"T:\KORAB_PLAT_T_SKRET7_20261118\00_START\_py")
import imageio_ffmpeg
from PIL import Image, ImageDraw, ImageFont
FF = imageio_ffmpeg.get_ffmpeg_exe()
R = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118")
C = R / "30_PRZEBIEGI" / sys.argv[1]; PRE = sys.argv[2]
O = Path(sys.argv[3]) if len(sys.argv) > 3 else R / "40_WYNIKI" / PRE / "FILMY"; O.mkdir(parents=True, exist_ok=True)
cfg = dict(l.split("=", 1) for l in (C / "control.properties").read_text().split() if "=" in l)
DT = float(cfg.get("dt_s", "4e-4"))
T, A = [], []
for r in csv.DictReader(open(C / "TF7_HISTORIA.csv")):
    try: T.append(float(r["time_s"])); A.append(float(r["TF7_skret_deg"]))
    except Exception: pass
z = sorted(zip(T, A)); T = [a for a, _ in z]; A = [b for _, b in z]


def kat(t):
    i = bisect.bisect_left(T, t)
    if i <= 0: return A[0]
    if i >= len(T): return A[-1]
    t0, t1 = T[i - 1], T[i]
    return A[i - 1] + (A[i] - A[i - 1]) * ((t - t0) / (t1 - t0) if t1 > t0 else 0)


try: FONT = ImageFont.truetype(r"C:\Windows\Fonts\arial.ttf", 34)
except Exception: FONT = ImageFont.load_default()
opis = cfg.get("opis", "").replace("_", " ")
for d in sorted(p for p in (C / "ANIMACJE").iterdir() if p.is_dir()):
    fr = sorted(d.glob("*.png"), key=lambda f: int(re.findall(r"(\d+)\.png$", f.name)[0]))
    if len(fr) < 5: print("pomijam", d.name, len(fr)); continue
    tmp = d / "_z_napisem"; tmp.mkdir(exist_ok=True)
    for f in fr:
        k = int(re.findall(r"(\d+)\.png$", f.name)[0]); t = k * DT
        g = tmp / f.name
        if g.exists() and g.stat().st_mtime >= f.stat().st_mtime: continue
        im = Image.open(f).convert("RGB"); dr = ImageDraw.Draw(im)
        s = f"{PRE}   t = {t:5.2f} s   skret = {kat(t):4.1f} deg"
        dr.rectangle([12, 12, 12 + 34 * 0.56 * len(s) + 24, 64], fill=(0, 0, 0)); dr.text((24, 18), s, fill=(255, 255, 255), font=FONT)
        im.save(g)
    lst = d / "_lista2.txt"
    du = 0.04 if len(fr) > 200 else 0.1   # krotkie serie (model zgrubiony) wolniej, zeby film trwal kilka sekund
    lst.write_text("".join(f"file '{(tmp / f.name).as_posix()}'\nduration {du}\n" for f in fr), encoding="utf8")
    out = O / f"{PRE}_{d.name}.mp4"
    r = subprocess.run([FF, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst),
                        "-vf", "scale=1280:-2,format=yuv420p", "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-r", "25", str(out)],
                       capture_output=True, text=True)
    print(d.name, len(fr), "klatek ->", out.name, r.returncode, r.stderr[-300:])
