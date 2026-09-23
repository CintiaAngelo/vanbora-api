# =====================================================================
#  VanBora - Baixa as imagens usadas pelos dados de demonstracao
# ---------------------------------------------------------------------
#  O DataSeeder aponta fotos de perfil, de dependentes, de ajudantes e do
#  veiculo para /uploads/seed/<arquivo>. Como `uploads/` nao e versionado
#  (midia de runtime), este script recria essa pasta a partir de servicos
#  publicos de imagem de demonstracao.
#
#  Uso (a partir de backend/):
#     powershell -ExecutionPolicy Bypass -File .\database\download_seed_media.ps1
#
#  Idempotente: arquivos ja existentes sao mantidos (use -Force para refazer).
# =====================================================================
param([switch]$Force)

$ErrorActionPreference = 'Stop'
$root = Join-Path (Split-Path -Parent $PSScriptRoot) 'uploads\seed'
New-Item -ItemType Directory -Force -Path $root | Out-Null

# Retratos de pessoas: randomuser.me (retratos de demonstracao, uso livre).
$portraits = @{
    'transporter-roberto.jpg'   = 'men/32'
    'transporter-fernanda.jpg'  = 'women/44'
    'transporter-carlos.jpg'    = 'men/75'
    'transporter-patricia.jpg'  = 'women/68'
    'transporter-anderson.jpg'  = 'men/51'
    'transporter-simone.jpg'    = 'women/26'
    'helper-carlos-mendes.jpg'  = 'men/85'
    'helper-lucia-ferreira.jpg' = 'women/12'
    'helper-joana-prado.jpg'    = 'women/59'
    'helper-rafael-dias.jpg'    = 'men/19'
    'guardian-mariana.jpg'      = 'women/33'
    'guardian-juliana.jpg'      = 'women/8'
    'guardian-marcos.jpg'       = 'men/40'
    'guardian-renata.jpg'       = 'women/71'
    'guardian-thiago.jpg'       = 'men/64'
    'guardian-camila.jpg'       = 'women/90'
}

# Criancas: avatares ilustrados (DiceBear), para nao usar foto de menor real.
$kids = @(
    'lucas', 'sofia', 'pedro', 'ana', 'miguel', 'helena', 'gabriel', 'laura', 'enzo'
)

# Fotos de veiculo e de mensagem: loremflickr (fotos Creative Commons por palavra-chave).
$photos = @{
    'van-roberto-1.jpg'   = 'van?lock=11'
    'van-roberto-2.jpg'   = 'minibus?lock=12'
    'van-roberto-3.jpg'   = 'schoolbus?lock=13'
    'van-fernanda-1.jpg'  = 'van?lock=21'
    'van-fernanda-2.jpg'  = 'minibus?lock=22'
    'van-carlos-1.jpg'    = 'van?lock=31'
    'van-carlos-2.jpg'    = 'minibus?lock=32'
    'van-patricia-1.jpg'  = 'van?lock=41'
    'van-patricia-2.jpg'  = 'schoolbus?lock=42'
    'van-anderson-1.jpg'  = 'van?lock=51'
    'van-anderson-2.jpg'  = 'minibus?lock=52'
    'van-simone-1.jpg'    = 'van?lock=61'
    'van-simone-2.jpg'    = 'schoolbus?lock=62'
    'chat-van-parada.jpg' = 'van,street?lock=71'
    'chat-escola.jpg'     = 'school,building?lock=72'
}

function Save-Image([string]$url, [string]$name) {
    $dest = Join-Path $root $name
    if ((Test-Path $dest) -and -not $Force) {
        Write-Host "  = $name (ja existe)"
        return
    }
    try {
        Invoke-WebRequest -Uri $url -OutFile $dest -TimeoutSec 40 -UseBasicParsing
        Write-Host "  + $name"
    } catch {
        Write-Warning "  ! falhou $name : $($_.Exception.Message)"
    }
}

Write-Host "Baixando midia de demonstracao para $root"
foreach ($e in $portraits.GetEnumerator()) {
    Save-Image "https://randomuser.me/api/portraits/$($e.Value).jpg" $e.Key
}
foreach ($seed in $kids) {
    Save-Image "https://api.dicebear.com/9.x/adventurer/png?seed=$seed&size=256&backgroundColor=ffd5dc,c0aede,b6e3f4,d1d4f9" "kid-$seed.png"
}
foreach ($e in $photos.GetEnumerator()) {
    Save-Image "https://loremflickr.com/900/600/$($e.Value)" $e.Key
}
Write-Host "Concluido. Arquivos: $((Get-ChildItem $root -File).Count)"
