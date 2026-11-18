# ============================================================
# AgriNav 本地一键构建脚本（Windows / PowerShell）
# 作用：打包两个后端 JAR、构建前端 dist，并把产物归集到 deploy 目录
# 用法：在仓库根执行  powershell -ExecutionPolicy Bypass -File deploy\build-all.ps1
# 前置：JDK21、Maven、便携 Node22（路径见下方变量，可按本机调整）
# ============================================================
$ErrorActionPreference = "Stop"

$root      = Split-Path -Parent $PSScriptRoot
$cmsDir    = Join-Path $root "AgriNav-CMS-dev-117a7a186108ddd50ee96286ca5b48e94da416bc"
$gpsDir    = Join-Path $root "AgriNav-GPS-dev-ec449519968cd61472a454f6fe6889d89a43c8b3"
$uiDir     = Join-Path $cmsDir "Agri-CMS-ui"
$node22npm = "C:\Users\Lenovo\dev\node-v22.23.5-win-x64\npm.cmd"

Write-Host "==> [1/4] 打包 CMS 后端..."
mvn -f (Join-Path $cmsDir "pom.xml") clean package -DskipTests

Write-Host "==> [2/4] 打包 GPS 后端..."
mvn -f (Join-Path $gpsDir "pom.xml") clean package -DskipTests

Write-Host "==> [3/4] 构建前端 dist..."
Push-Location $uiDir
$env:NODE_OPTIONS = "--openssl-legacy-provider"
& $node22npm run build:prod
Pop-Location

Write-Host "==> [4/4] 归集产物到 deploy..."
Copy-Item (Join-Path $cmsDir "Agri-CMS-admin\target\Agri-CMS-admin.jar") (Join-Path $PSScriptRoot "cms\app.jar") -Force
Copy-Item (Join-Path $gpsDir "jtt808-server\target\jtt808-server-1.0.0-SNAPSHOT.jar") (Join-Path $PSScriptRoot "gps\app.jar") -Force
$distTarget = Join-Path $PSScriptRoot "frontend\dist"
if (Test-Path $distTarget) { Remove-Item $distTarget -Recurse -Force }
Copy-Item (Join-Path $uiDir "dist") $distTarget -Recurse

Write-Host "==> 构建完成。可执行 deploy\scripts\prepare.sh 预检（在服务器上）。"
