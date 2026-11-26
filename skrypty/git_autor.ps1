# Ustawia autora globalnie (adres noreply GitHuba - prywatny e-mail nie trafia do commitow) i poprawia autora dotychczasowych commitow.
$g = 'C:\Program Files\Git\cmd\git.exe'
Set-Location (Split-Path $PSScriptRoot -Parent)
& $g config --global user.name 'Julek'
& $g config --global user.email '361491866+3Tran159@users.noreply.github.com'
& $g config --global core.autocrlf true
& $g add -A
& $g commit -q -m "skrypt git_autor: adres noreply" 2>$null
& $g -c core.editor=true rebase -q --root --exec "git commit --amend --no-edit --reset-author -q"
& $g push -q --force origin main
& $g log --format="%h %an <%ae> %s"
