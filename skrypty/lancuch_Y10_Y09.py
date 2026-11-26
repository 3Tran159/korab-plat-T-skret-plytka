# Y10 (powrot 45->0, plytka O03) na GPU, potem dokonczenie Y09 (39->45 deg) z TF7_CKPT_B. Sekwencyjnie.
import subprocess, sys
from pathlib import Path
S = Path(__file__).parent
for case in ["Y10_plytka_O03_rampa_powrotna_45do0_30kmh_h1c_IDDES",
             "Y09_plytka_O23_waska180x70_NACA2402_14mm_rampa_0do45_30kmh_h1c_IDDES"]:
    subprocess.run([sys.executable, str(S / "kontroler.py"), case], cwd=str(S))
