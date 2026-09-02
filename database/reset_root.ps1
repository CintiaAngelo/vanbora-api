# =====================================================================
#  VanBora - Reset da senha do root do MySQL 8 (Windows)
#  Pode ser executado de um PowerShell normal: ele se auto-eleva (UAC).
#
#  As senhas sao informadas na execucao - nenhuma fica no repositorio.
#    .\reset_root.ps1
#    .\reset_root.ps1 -RootPassword 'xxx' -AppPassword 'yyy'
#
#  A senha da aplicacao (usuario `vanbora`) e a que o backend espera em
#  DB_PASSWORD - veja backend/.env.example.
# =====================================================================

param(
    [string]$RootPassword,
    [string]$AppPassword
)

# --- Auto-elevação ---
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host 'Solicitando elevacao (UAC)...' -ForegroundColor Yellow
    # As senhas nao vao na linha de comando do processo elevado (ficariam visiveis
    # na lista de processos); o script re-pergunta na janela com privilegio.
    Start-Process powershell -Verb RunAs -ArgumentList "-ExecutionPolicy Bypass -NoProfile -File `"$PSCommandPath`""
    exit
}

if (-not $RootPassword) { $RootPassword = Read-Host 'Nova senha do root do MySQL' }
if (-not $AppPassword)  { $AppPassword  = Read-Host 'Nova senha do usuario `vanbora` (sera o DB_PASSWORD do backend)' }
if (-not $RootPassword -or -not $AppPassword) {
    Write-Host 'ERRO: as duas senhas sao obrigatorias.' -ForegroundColor Red; Read-Host 'Enter'; exit 1
}

$bin       = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
$defaults  = 'C:\ProgramData\MySQL\MySQL Server 8.0\my.ini'
$template  = Join-Path $PSScriptRoot 'reset_root.sql.template'
# O init-file e gerado a partir do template com as senhas informadas e vive so
# em %TEMP%, apagado no final.
$initFile  = Join-Path $env:TEMP 'vanbora_reset_root.sql'
$service   = 'MySQL80'
$rootPw    = $RootPassword
$appPw     = $AppPassword
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

if (-not (Test-Path $template))  { Write-Host "ERRO: nao achei $template"  -ForegroundColor Red; Read-Host 'Enter'; exit 1 }
if (-not (Test-Path $defaults))  { Write-Host "ERRO: nao achei $defaults"  -ForegroundColor Red; Read-Host 'Enter'; exit 1 }

# Gera o init-file com as senhas informadas (utf8 sem BOM: o mysqld nao aceita BOM).
$sql = (Get-Content $template -Raw).Replace('__ROOT_PASSWORD__', $rootPw).Replace('__APP_PASSWORD__', $appPw)
[IO.File]::WriteAllText($initFile, $sql, (New-Object Text.UTF8Encoding($false)))

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
    # O arquivo temporario contem as senhas em texto: sai daqui em qualquer desfecho.
    Remove-Item $initFile -Force -ErrorAction SilentlyContinue
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
    Write-Host '  root e vanbora atualizados com as senhas informadas.'
    Write-Host '  Defina DB_PASSWORD (backend/.env) com a senha do usuario vanbora.'
} else {
    Write-Host "FALHOU.  root OK=$rootOk  vanbora OK=$appOk" -ForegroundColor Red
    Write-Host '--- ultimas linhas do log do mysqld temporario ---' -ForegroundColor Yellow
    if (Test-Path $logFile) { Get-Content $logFile -Tail 30 } else { Write-Host '(log nao gerado)' }
    Write-Host '--------------------------------------------------' -ForegroundColor Yellow
    Write-Host 'Copie TODA a saida acima e envie para o assistente.'
}

Read-Host "`nPressione Enter para fechar"
