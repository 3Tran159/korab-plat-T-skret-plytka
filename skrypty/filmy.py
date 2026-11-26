r"""Sklada klatki ANIMACJE\0x_* w MP4 (H.264, 25 kl/s) -> 40_WYNIKI\FILMY. ffmpeg z imageio_ffmpeg w 00_START\_py."""
import sys, subprocess
from pathlib import Path
sys.path.insert(0, r"T:\KORAB_PLAT_T_SKRET7_20261118\00_START\_py")
import imageio_ffmpeg
FF = imageio_ffmpeg.get_ffmpeg_exe()
A = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\30_PRZEBIEGI\Y01_skret7_30kmh_h1c_IDDES\ANIMACJE"); O = Path(r"T:\KORAB_PLAT_T_SKRET7_20261118\40_WYNIKI\FILMY"); O.mkdir(parents=True, exist_ok=True)
for d in sorted(A.iterdir()):
    fr = sorted(d.glob("*.png"))
    if len(fr) < 5: print("pomijam", d.name, len(fr)); continue
    lst = d / "_lista.txt"
    lst.write_text("".join(f"file '{f.as_posix()}'\nduration 0.04\n" for f in fr), encoding="utf8")
    out = O / f"Y01_{d.name}.mp4"
    r = subprocess.run([FF, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst), "-vf", "scale=1280:-2,format=yuv420p",
                        "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-r", "25", str(out)], capture_output=True, text=True)
    print(d.name, len(fr), "klatek ->", out.name, r.returncode, r.stderr[-200:])
