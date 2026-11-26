# Raport przebiegu prac – KORAB płat T, skręt strutu i płytka antywentylacyjna

Stan na 26.11.2026. Źródło: dziennik projektu (claude.ai, `claude/korab-plat-T-rampa-skretu.md`) + dokument „Płytka antywentylacyjna płata T KORAB – optymalizacja kształtu: wynik i metodyka”.

## Y04 – rampa skrętu 0→45°, bez płytki (20.11.2026)

Ustawienia: STAR-CCM+ 2606, VOF (FlatVofWave), SST k-ω IDDES, All y+ (y+ ≈ 97), dt = 0,4 ms, 5 iteracji wewn., GPU RTX PRO 3000.
Pełny model, siatka 2,91 mln komórek: min. rozmiar powierzchni strutu 0,3 mm (0,1 mm dawało mikrokomórki 1e-13…1e-15 m³ i rozbieżność przy t ≈ 0,084 s), Cell Quality Remediation.
Skręt = obrót całej siatki (RotatingMotion, oś x = 0,16 m), napływ stały −8,33 m/s; 0° do 0,4 s, 15°/s do 45° (3,4 s), koniec 3,5 s.
Odrzucone: obracanie kierunku napływu w nieruchomej domenie – fizycznie błędne (basen przyspiesza bokiem ~2,2 m/s², sztucznie rośnie siła nośna).

| Skręt | Siła nośna płata [kg] | Siła boczna [N] | Opór [N] | Cp min strut | Wentylacja strutu [%] |
|---|---|---|---|---|---|
| 0° | 46,3 | 1 | 35,8 | −0,60 | 0 |
| 10° | 44,5 | 69 | 40,5 | −1,65 | 0,6 |
| 15° | 42,1 | 103 | 46,9 | −2,94 | 1,5 |
| 20° | 37,2 | 134 | 63,7 | −3,92 | 2,8 |
| 24° | 32,8 | 163 | 82,9 | −4,64 | 3,4 |
| 25° | 25,8 | 106 | 74,7 | −3,51 | 17 |
| 26° | 17,4 | 54 | 63,1 | −2,67 | 43 |
| 30° | 11,3 | 68 | 75,9 | −2,72 | 48 |
| 39° | −0,3 | 103 | 115 | −2,74 | 49 |
| 45° | −7,5 | 118 | 145 | −2,62 | 49 |

- 0–19°: przepływ przylegający, Fy ≈ 6,8 N/°, siła nośna spada ≈ cos²β, wentylacja ≤ 2,6 %.
- ~19,5–20°: początek oderwania na strucie.
- ~25°: pełna wentylacja strony ssącej strutu w ~0,05 s (3 → 43–48 %); Fy 163 → 54 N, siła nośna płata 33 → 18 kg.
- Spadek siły nośnej nie wynika z powietrza na płacie (połówka za kawerną 7,0 kg, mokra 5,0 kg przy 29,5°) – zmiana opływu po zaniku cyrkulacji strutu.
- Raport `TF7_L_kg` ma abs() na części ciśnieniowej – powyżej ~39° liczyć ze znakiem.

## Y05 – płytka prostokątna 100×100×3, 20 mm pod lustrem (21.11.2026)
Wentylacja wcześniej: 22–23° (Y04: 25°). Do 21° 3× mniej powietrza na strucie, +3–5 % Fy; opór płytki 10–13 N, siła pionowa płytki ~−30 N.
Po wentylacji płytka niesie (+4…+7 kg). Mechanizm (film Y05H_H14): kawerna zaczyna się przy tylnym narożu płytki – płytka kończy się na x = 0,0955, a krawędź spływu strutu jest na x ≈ 0,054. **Płytka musi sięgać za krawędź spływu.**

## Optymalizacja kształtu (22–24.11.2026)
Model zgrubiony: `mesh_scale=2.5` (0,62–0,73 mln), dt 0,8 ms, 3 it., rampa 60°/s do 15°, potem 15°/s do 29° lub 35°. CPU 3 rdzenie ~4–9 h lub GPU 35 min–2 h.
Kryterium: kąt, przy którym > 30 % strutu w powietrzu. Kalibracja: Y04 25,3° / O00 24,1°; Y05 23,8° / O01 22,8° – model zaniża ~1°, rozrzut powtórzeń ±1°.

| Wariant | Went. > 30 % | Fy maks. | Uwagi |
|---|---|---|---|
| O00 bez płytki | 24,1° | | |
| O01 prostokąt (Y05) | 22,8° | | |
| O03 elipsa 180×100, 20 mm | 27,9° (powt. 29,0°) | | → Y07 |
| O04 elipsa 180×140 | 28,1° | | |
| O05 ogon 125×100 | 26,5° | | |
| O10 elipsa 1 mm | 28,6° | | |
| O11 ogon 125×140 | 27,1° | | |
| O13–O17 głębokość 9/12/16/20/30 mm | 27,5 / 29,0 / 29,8 / 29,0 / 26,3° | 164 / 161 / 150 / 145 / 131 N | do 35° |
| O16 180×70, 20 mm | 30,5° | 137 N | |
| O18 180×70, 14 mm | 30,0° (powt. 30,1°) | 144 N | opór 2,7 N, −1,5 kg |
| O19 180×100, 14 mm | 29,4° | 156 N | |
| O21 / O22 kąt ustawienia +2° / +4° | 27,1 / 25,1° | | siła nośna płytki – odrzucone |
| O23 O18 + NACA 2402 | 29,9° | 149 N | −0,5 kg, opór 3,1 N → Y09 |

## Weryfikacja pełnym modelem
- **Y07 (O03):** wentylacja 28,4°, Fy maks. 128 N (Y04: 169 N), plateau Fy ~100 N od 15° (to fizyka płytki, nie artefakt rampy); przy 45° płat+płytka +1,2 kg vs −7,5 kg w Y04.
- **Y09 (O23):** wentylacja 30 % przy **24,96°** – gorzej niż Y07, na poziomie Y04 bez płytki; Fy maks. 158 N przy 17,5°. Model zgrubiony przewidywał 29,9°. Przebieg zatrzymany przy 39,3° (utrata licencji przy przenoszeniu laptopa), punkt zapisu 38,6°.
- Wniosek: najlepsza zweryfikowana płytka to O03. Model zgrubiony nie rozróżnia wiarygodnie wariantów wąskich/płytkich.

## Histereza (rampa powrotna 45→0°)
- Y08 (bez płytki, z Y04 FINAL): przerwany przy t = 3,548 s (44,3°), wznawialny.
- Y10 (płytka O03, z Y07 FINAL): `TFoilRampBack.java`, 15°/s od t = 3,5 s, koniec 6,6 s. Start 26.11.2026 10:19.

## Błędy i lekcje
- importStlPart zwraca void → porównanie zbioru części przed/po imporcie; duplikaty ścian STL → tolerancja 1e-9 i 12 cyfr.
- v2 optymalizacji: nagły start pod 17° → sztuczna wentylacja. v3: obrót = 0 (refleksja wybrała getRotationRateInput) → prędkość ustawiana wprost + `ramp_rate_check` w SETUP.json.
- Martwe sesje STAR trzymały GPU → kontroler zabija po każdej sesji wiszące procesy swojego przypadku (tylko swoje PID).
- Deadline skopiowany ze starego przypadku zabił O21–O23 → zawsze sprawdzać `deadline`.
- Łańcuch Y09→Y10: warunek czekania akceptował stary stan STOPPED_BY_FILE → dwa przypadki na jednym GPU. Czekać na DONE i brak procesów.
- Licencja PoD wymaga sieci: przeniesienie laptopa = zerwana sesja („Connection reset”, NullPointerException w makrze).
