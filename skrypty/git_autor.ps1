# Ustawia autora globalnie i poprawia autora dotychczasowych commitow (repo swieze, tylko wlasne commity).
$g = 'C:\Program Files\Git\cmd\git.exe'
Set-Location (Split-Path $PSScriptRoot -Parent)
& $g config --global user.name 'Julek'
& $g config --global user.email 'serkieza@gmail.com'
& $g config --global core.autocrlf true
& $g config --unset user.name
& $g config --unset user.email
& $g add -A
& $g commit -q -m "skrypt git_autor" 2>$null
& $g -c core.editor=true rebase -q --root --exec "git commit --amend --no-edit --reset-author -q"
& $g push -q --force-with-lease origin main
& $g log --format="%h %an <%ae> %s"
