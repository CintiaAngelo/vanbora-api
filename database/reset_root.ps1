# =====================================================================
#  VanBora - Reset da senha do root do MySQL 8 (Windows)
#  Pode ser executado de um PowerShell normal: ele se auto-eleva (UAC).
#
#  Senhas resultantes:  root = Vanbora@2026  |  vanbora = Vanbora@123
# =====================================================================

# --- Auto-elevação ---
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host 'Solicitando elevacao (UAC)...' -ForegroundColor Yellow
    Start-Process powershell -Verb RunAs -ArgumentList "-ExecutionPolicy Bypass -NoProfile -File `"$PSCommandPath`""
    exit
}

$bin       = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$defaults  = 'C:\ProgramData\MySQL\MySQL Server 8.0\my.ini'
$initFile  = Join-Path $PSScriptRoot 'reset_root.sql'
$service   = 'MySQL80'
$rootPw    = 'Vanbora@2026'
$appPw     = 'Vanbora@123'
$logFile   = Join-Path $env:TEMP 'vanbora_mysqld_reset.log'

$mysqld    = Join-Path $bin 'mysqld.exe'
$mysql     = Join-Path $bin 'mysql.exe'
$mysqladm  = Join-Path $bin 'mysqladmin.exe'

# Testa um login sem expor a senha na linha de comando (usa MYSQL_PWD => sem warning).
function Test-Login($user, $pw) {
    $env:MYSQL_PWD = $pw
    & $mysql "-u$user" "-e" "SELECT 1;" 1>$null 2>$null
    $ok = ($LASTEXITCODE -eq 0)
    Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
    return $ok
}

if (-not (Test-Path $initFile))  { Write-Host "ERRO: nao achei $initFile"  -ForegroundColor Red; Read-Host 'Enter'; exit 1 }
if (-not (Test-Path $defaults))  { Write-Host "ERRO: nao achei $defaults"  -ForegroundColor Red; Read-Host 'Enter'; exit 1 }

$proc = $null
try {
    Write-Host '1/5  Parando o servico MySQL80...' -ForegroundColor Cyan
    Stop-Service $service -Force -ErrorAction SilentlyContinue
    $deadline = (Get-Date).AddSeconds(30)
    while ((Get-Service $service).Status -ne 'Stopped' -and (Get-Date) -lt $deadline) { Start-Sleep 1 }
    Start-Sleep 2

    Write-Host '2/5  Subindo mysqld temporario com o init-file...' -ForegroundColor Cyan
    if (Test-Path $logFile) { Remove-Item $logFile -Force -ErrorAction SilentlyContinue }
    # Argumentos como UMA string, com aspas internas (o caminho do defaults-file tem espacos).
    $argString = "--defaults-file=`"$defaults`" --init-file=`"$initFile`" --console"
    $proc = Start-Process -FilePath $mysqld -ArgumentList $argString `
        -WindowStyle Hidden -PassThru -RedirectStandardError $logFile

    Write-Host '3/5  Aguardando aplicar o reset (ate ~45s)...' -ForegroundColor Cyan
    $applied = $false
    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep 3
        if (Test-Login 'root' $rootPw) { $applied = $true; break }
        if ($proc.HasExited) { break }
    }

    if ($applied) {
        Write-Host '     Reset aplicado. Encerrando mysqld temporario...' -ForegroundColor Cyan
        $env:MYSQL_PWD = $rootPw
        & $mysqladm "-uroot" shutdown 1>$null 2>$null
        Remove-Item Env:\MYSQL_PWD -ErrorAction SilentlyContinue
        Start-Sleep 4
    }
}
finally {
    if ($proc -and -not $proc.HasExited) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        Start-Sleep 2
    }
    Write-Host '4/5  Religando o servico MySQL80...' -ForegroundColor Cyan
    Start-Service $service -ErrorAction SilentlyContinue
    Start-Sleep 5
}

Write-Host '5/5  Verificando logins...' -ForegroundColor Cyan
$rootOk = Test-Login 'root' $rootPw
$appOk  = Test-Login 'vanbora' $appPw

Write-Host ''
if ($rootOk -and $appOk) {
    Write-Host 'SUCESSO!' -ForegroundColor Green
    Write-Host "  root    -> $rootPw"
    Write-Host "  vanbora -> $appPw  (usada pelo backend por padrao)"
} else {
    Write-Host "FALHOU.  root OK=$rootOk  vanbora OK=$appOk" -ForegroundColor Red
    Write-Host '--- ultimas linhas do log do mysqld temporario ---' -ForegroundColor Yellow
    if (Test-Path $logFile) { Get-Content $logFile -Tail 30 } else { Write-Host '(log nao gerado)' }
    Write-Host '--------------------------------------------------' -ForegroundColor Yellow
    Write-Host 'Copie TODA a saida acima e envie para o assistente.'
}

Read-Host "`nPressione Enter para fechar"
