# Y10: przygotowanie rampy powrotnej 45->0 z Y07 TF7_FINAL (TFoilRampBack.java, CPU),
# potem czeka na koniec Y09 (GPU) i odpala kontroler.py dla Y10. Log: 00_START\kontroler.log
import sys, time, json, subprocess
from pathlib import Path
Y10 = "Y10_plytka_O03_rampa_powrotna_45do0_30kmh_h1c_IDDES"
sys.argv = ["kontroler.py", Y10]
import kontroler as K
d = K.CASE
src = K.ROOT / "30_PRZEBIEGI" / "Y07_plytka_O03_elipsa180x100_rampa_0do45_30kmh_h1c_IDDES" / "TF7_FINAL.sim"
y09 = next((K.ROOT / "30_PRZEBIEGI").glob("Y09_plytka_O23*"))

if not (K.jl(d / "SETUP.json").get("stage") == "done" and any(d.glob("*_SETUP.sim"))):
    K.log(f"Y10 RampBack start z {src}")
    r = K.run_star(src, K.MAK / "TFoilRampBack.java", d, gpu=False, timeout_s=3 * 3600)
    s = K.jl(d / "SETUP.json")
    K.log(f"Y10 RampBack {s.get('stage')} rc={r.get('returncode')} {r.get('seconds')} s warn={s.get('warnings', '')}")
    if s.get("stage") != "done":
        sys.exit(1)

while True:  # czekaj na Y09 (GPU)
    if (K.ROOT / "00_START" / "STOP_KAMPANIA").exists():
        K.log("Y10: STOP_KAMPANIA"); sys.exit()
    st = K.jl(y09 / "RUN_STATUS.json").get("state")
    if st in ("DONE", "STOPPED_DEADLINE", "STOPPED_BY_FILE", "FAILED", "ERROR") or (K.ROOT / "00_START" / "Y10_START_TERAZ").exists():
        break
    time.sleep(120)
time.sleep(300)  # kontroler Y09 sprzata procesy
K.log(f"Y10: Y09 stan {st} -> start kontrolera Y10")
subprocess.Popen([sys.executable, str(K.ROOT / "00_START" / "kontroler.py"), Y10], cwd=str(K.ROOT / "00_START"))
