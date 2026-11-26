# KORAB – płat T: wentylacja strutu przy skręcie i płytka antywentylacyjna (CFD, STAR-CCM+)

Projekt z 18–26.11.2026: przedni płat „Olek” z wspornikiem (strutem), 30 km/h, zanurzenie h = 75 mm.
Pytania:
1. Przy jakim kącie skrętu strutu (kąt natarcia strutu = dryf/skręt) strut wentyluje i co się wtedy dzieje z siłą nośną płata.
2. Czy płytka antywentylacyjna na strucie przesuwa wentylację – jaki kształt, głębokość, koszt.
3. Czy wentylacja ma histerezę (powrót 45° → 0°).

## Wynik w skrócie

| Przypadek (pełny model) | Płytka | Wentylacja strutu > 30 % | Fy maks. | Uwagi |
|---|---|---|---|---|
| Y04 | brak | 25,3° | 169 N | siła nośna płata 46 kg przy 0°, 18 kg przy 26°, −7,5 kg przy 45° |
| Y05 | prostokąt 100×100×3, 20 mm | 23,8° (gorzej) | – | nie sięga za krawędź spływu strutu – kawerna schodzi tyłem |
| **Y07** | **O03: elipsa 180×100, NACA 0002, 20 mm** | **28,4°** | 128 N | najlepsza zweryfikowana; po wentylacji płat+płytka +1,2 kg vs −7,5 kg (Y04) przy 45° |
| Y09 | O23: elipsa 180×70, NACA 2402, 14 mm | 24,96° (gorzej) | 158 N | zwycięzca modelu zgrubionego (29,9°) – pełny model nie potwierdził |
| Y08 | brak, powrót 45→0° | – | – | przerwany przy 44,3° |
| Y10 | O03, powrót 45→0° | w toku (26.11) | – | histereza z płytką |

Wnioski:
- Płytka musi sięgać za krawędź spływu strutu (x ≈ 0,054 m); krótsza przyspiesza wentylację.
- Model zgrubiony (mesh ×2,5, dt 0,8 ms) dobrze porządkuje warianty duże (prostokąt vs elipsa: ±1°), ale przy wariantach różniących się o 1–2° (wąska/płytka O18/O23 vs O03) mylił się o ~5°. Każdego zwycięzcę sprawdzać pełnym modelem.
- Pełny opis i wykresy: `docs/RAPORT.md`, dokument „Płytka antywentylacyjna płata T KORAB – optymalizacja kształtu: wynik i metodyka” (claude.ai), `wyniki/`.

## Struktura

```
star/makra/            makra Java STAR-CCM+ (wszystko natywne, edytowalne w GUI)
  TFoilYawSetup.java   siatka + fizyka + płytka (STL) + ruch obrotowy (rampa skrętu) + raporty/sceny/historia -> <name>_SETUP.sim
  TFoilYawRun.java     liczenie w porcjach, punkty zapisu (CKPT), animacje, TF7_HISTORIA.csv, RUN_STATUS.json, STOP/deadline
  TFoilRampBack.java   rampa powrotna 45->0 z TF7_FINAL.sim
  TFoilHistRender.java filmy z pliku historii (.simh), sceny H01–H14
  diagnostyka/         makra pomocnicze użyte przy szukaniu błędów
skrypty/
  kontroler.py         autonomiczny kontroler przypadku: SETUP -> RUN (GPU/CPU), wznawianie z CKPT, 4 próby, alarmy, sprzątanie procesów
  kolejka_opt.py       kolejka wariantów optymalizacji (CPU, rdzenie), STOP_OPT
  lancuch_*.py, przygotuj_Y10.py  łańcuchy przypadków
  plytka_elipsa_stl.py generator płytki: elipsa w rzucie, profil NACA 4-cyfrowy, kąt ustawienia -> STL
  opt_plytka_przypadki.py  definicje wariantów O00–O23
  porownanie_opt.py, wykres_*.py, filmy_*.py, analiza*.py  analiza i wykresy
geometria/             małe pliki geometrii (duże STL – w archiwum danych, odtwarzalne generatorem)
przypadki/<przypadek>/ control.properties (wszystkie parametry), SETUP.json (log ustawień, jakość siatki),
                       RUN_STATUS.json, TF7_HISTORIA.csv (siły, wentylacja, kąt w czasie), ALARM.json
przypadki/_odrzucone/  próby z błędami (opisane nazwą katalogu)
przypadki/_kontroler.log  pełny dziennik kampanii
wyniki/                wykresy, tabele (csv), filmy mp4; OPT = optymalizacja kształtu
ARCHIWUM_DANYCH_inwentarz.csv  gdzie leżą ciężkie pliki .sim/.simh/STL
```

Nazewnictwo: `Y..` = pełny model (3–3,6 mln komórek, dt 0,4 ms, 5 it., GPU), `O..` = model zgrubiony do optymalizacji
(`mesh_scale=2.5`, 0,6–0,7 mln, dt 0,8 ms, 3 it., rampa 60°/s do 15°, potem 15°/s).

## Jak odtworzyć przypadek

1. Źródło: `T_6deg_jednofazowy_oryginal.sim` (projekt zanurzenia płata, ścieżka w `skrypty/kontroler.py`, `SRC`).
2. Nowy katalog przypadku z `control.properties` (wzór: `przypadki/Y07_...`). Płytka: `python skrypty/plytka_elipsa_stl.py out.stl xc Lx Wy tc zc [kąt] [m] [p]`, ścieżka w `plate_stl`.
3. `python skrypty/kontroler.py <katalog_przypadku>` – sam zrobi SETUP i RUN, wznawia z punktów zapisu.
   Sterowanie: plik `STOP` w katalogu przypadku (zapis i koniec), `STOP_KAMPANIA` w katalogu skryptów (blokada startu), `deadline=` w properties.
4. Powrót 45→0: properties jak `przypadki/Y10_...` (`no_init`, `back_*`), najpierw `TFoilRampBack.java` na `TF7_FINAL.sim`, potem kontroler.
5. Analiza: `porownanie_opt.py`, `wykres_Y04_Y05.py <przypadek> <prefiks>`, `filmy_przypadek.py`.

Wymagania: STAR-CCM+ 2606 (licencja PoD – wymaga sieci; zerwanie licencji kończy sesję „Connection reset”), Python 3 z numpy/matplotlib, GPU 12 GB dla pełnego modelu.

## Ustawienia fizyczne (pełny model)
VOF FlatVofWave, SST k-ω IDDES, All y+ (y+ ≈ 97), dt 0,4 ms, 5 it. wewn.; skręt = obrót całej siatki (RotatingMotion, oś z przez x = 0,16 m),
0° do 0,4 s, 15°/s do 45° (3,4 s). Lustro wody z = 0,05879 m. Strut: krawędź natarcia x = 0,1955, spływu x ≈ 0,054.

## Archiwum danych (pliki .sim)

| Gdzie | Co | Rozmiar |
|---|---|---|
| `H:\10_ARCHIWUM_PROJEKTOW\KORAB\KORAB_plat_T_skret_plytka_dane\<przypadek>\` | `TF7_FINAL.sim` (stan końcowy, pełne pole – otwierać w GUI) i `<name>_SETUP.sim` (siatka + ustawienia, t = 0); gdy brak FINAL – ostatni `TF7_CKPT` (Y05, Y08) | ~50 GB |
| `H:\...\KORAB_plat_T_skret_plytka_dane\_wyniki_klatki\` | klatki png animacji (filmy mp4 są w repo) | ~0,6 GB |
| `E:\20_DANE_OBLICZEN_PRZELEW\KORAB_plat_T_historie_DO_ODSTRZALU_gdy_brak_miejsca\` | historie pól `.simh` Y04/Y05/Y07 (źródło filmów H01–H14) – do skasowania, gdy zabraknie miejsca | 64 GB |
| `T:\..._do_usuniecia\`, `E:\..._do_usuniecia\` | pośrednie punkty zapisu, kopie robocze – do skasowania po decyzji | 79 GB |

Pełna lista plików: `ARCHIWUM_DANYCH_inwentarz.csv` (kolumna `bylo` = ścieżka przed przeniesieniem).
Y08 (powrót bez płytki) jest wznawialny z `H:\...\Y08_rampa_powrotna_45do0_30kmh_h1c_IDDES\TF7_CKPT_A.sim` – przed wznowieniem poprawić `LATEST_CHECKPOINT.json`.

## Znane ograniczenia
- y+ ≈ 100 (funkcja ścianowa) i rampa 15°/s – kąty wentylacji to raczej górne granice (opóźnienie ~1–2°).
- Raport `TF7_L_kg` ma abs() na części ciśnieniowej – powyżej ~39° liczyć Lp + Ls ze znakiem.
- O12 (płytka 6 mm): błąd orientacji ścian STL, niezdiagnozowany.
