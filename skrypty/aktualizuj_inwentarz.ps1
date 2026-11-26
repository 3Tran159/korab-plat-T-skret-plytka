# Inwentarz archiwum danych z PRZENIESIENIA.csv (bez klatek png) + commit + push.
$g = 'C:\Program Files\Git\cmd\git.exe'
$R = Split-Path $PSScriptRoot -Parent
Set-Location $R
$src = 'H:\10_ARCHIWUM_PROJEKTOW\KORAB\KORAB_plat_T_skret_plytka_dane\PRZENIESIENIA.csv'
$rows = Import-Csv $src -Header rodzaj,skad,dokad,GB | ? { $_.dokad -notmatch '_wyniki_klatki' }
$rows | % { [pscustomobject]@{
    rodzaj = @{H='ARCHIWUM_H';HIST='HISTORIA_E_do_odstrzalu';DEL='DO_USUNIECIA'}[$_.rodzaj]
    plik = $_.dokad; GB = $_.GB; bylo = $_.skad } } |
  Export-Csv "$R\ARCHIWUM_DANYCH_inwentarz.csv" -NoTypeInformation -Encoding UTF8
& $g add -A
& $g commit -q -m "Archiwum danych: inwentarz po przeniesieniu na H: (26.11.2026)"
& $g push -q origin main
& $g log --oneline -1
