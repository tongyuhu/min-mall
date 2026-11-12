param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$webRoot = Join-Path $projectRoot 'frontend'
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw '请先安装 Node.js 22.12 或更高版本。'
}
$listener = Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host '5173 端口已有服务，请确认是否为已启动的商城前端：http://127.0.0.1:5173'
    return
}
Push-Location $webRoot
try {
    if (-not (Test-Path -LiteralPath (Join-Path $webRoot 'node_modules'))) {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
    }
    Write-Host '商城前台：http://127.0.0.1:5173'
    Write-Host '管理后台：http://127.0.0.1:5173/admin'
    Write-Host '后端需运行在 127.0.0.1:18080；按 Ctrl+C 停止前端服务。'
    & npm.cmd run dev
    if ($LASTEXITCODE -ne 0) { throw '前端服务启动失败，请查看上方错误。' }
}
finally { Pop-Location }
