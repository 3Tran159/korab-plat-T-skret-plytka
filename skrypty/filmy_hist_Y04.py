r"""Y04: sklada klatki renderu z historii (40_WYNIKI\Y04\render_0..2\FILMY_HIST\<scena>\k<stan>.png) w MP4.
Stany co 0,03 s (116 stanow), 12 kl/s -> ok. 10 s filmu. Napis czas/kat jest juz na klatkach (adnotacja STAR).
Wyjscie: 40_WYNIKI\Y04\FILMY\Y04H_<scena>.mp4
"""
import sys, re, subprocess
from pathlib import Path
sys.path.insert(0, r"T:\KORAB_PLAT_T_SKRET7_20261118\00_START\_py")
import imageio_ffmpeg
FF = imageio_ffmpeg.get_ffmpeg_exe()
PRE = sys.argv[1] if len(sys.argv) > 1 else "Y04"   # 2026-11-21: przypadek jako argument (Y05 ...)
R = Path(rf"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\{PRE}"); O = R / "FILMY"; O.mkdir(parents=True, exist_ok=True)
sc = {}
for j in sorted(R.glob("render_[0-9]")):
    for d in (j / "FILMY_HIST").iterdir():
        if d.is_dir(): sc.setdefault(d.name, []).extend(d.glob("k*.png"))
for name, fr in sorted(sc.items()):
    fr = sorted(fr, key=lambda f: int(re.findall(r"k(\d+)\.png$", f.name)[0]))
    lst = R / f"_lista_{name}.txt"
    lst.write_text("".join(f"file '{f.as_posix()}'\nduration {1/12:.5f}\n" for f in fr) + f"file '{fr[-1].as_posix()}'\n", encoding="utf8")
    out = O / f"{PRE}H_{name}.mp4"
    r = subprocess.run([FF, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst), "-vf", "scale=1600:-2,format=yuv420p",
                        "-c:v", "libx264", "-preset", "veryfast", "-crf", "22", "-r", "12", str(out)], capture_output=True, text=True)
    print(name, len(fr), "->", out.name, r.returncode, r.stderr[-200:])
