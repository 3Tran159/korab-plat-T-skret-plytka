"""Uruchom makro STAR w trybie batch na pliku .sim (licencja przez starrun.license_env, klucz nie jest wypisywany).
Uzycie: python uruchom_makro.py <sim> <makro.java> <katalog_wyjscia> [gpu] [cpus np. 22,23] [timeout_s]
"""
import sys, json
sys.path.insert(0, r"T:\NTNU_GODO_CONTROL_20261109\20_NARZEDZIA")
import starrun

sim, macro, out = sys.argv[1:4]
gpu = len(sys.argv) > 4 and sys.argv[4] == "gpu"
cpus = tuple(int(c) for c in sys.argv[5].split(",")) if len(sys.argv) > 5 else (22, 23)
tmo = int(sys.argv[6]) if len(sys.argv) > 6 else 3600
res = starrun.launch(sim, macro, out, cpus=cpus, np_=1, timeout_s=tmo, gpu=gpu)
print(json.dumps(res, default=str, indent=1))
