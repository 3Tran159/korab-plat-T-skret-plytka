# Jednorazowe: inicjalizacja repo git i pierwszy commit.
$g = 'C:\Program Files\Git\cmd\git.exe'
Set-Location (Split-Path $PSScriptRoot -Parent)
& $g init -b main
& $g config user.name 'Julek'
& $g config user.email 'julek@korab.local'
& $g add -A
& $g commit -q -m "KORAB plat T: skret strutu i plytka antywentylacyjna - makra STAR, skrypty, przypadki, wyniki (stan 26.11.2026)"
& $g count-objects -vH
& $g log --oneline -1
