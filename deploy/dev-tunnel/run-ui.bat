@echo off
setlocal
title AgriNav CMS UI (Vue3) - dev server

rem ============================================================
rem  AgriNav CMS frontend launcher (Windows)
rem  Frontend : AgriNav-CMS-dev-*\Agri-CMS-ui-v3 (Vue3 + Vite)
rem  Dev port : 5180 (strictPort, no automatic port fallback)
rem  Full doc : see the frontend setup guide in the repository root
rem ============================================================

rem Locate the CMS project folder, then the frontend folder inside it
set "CMS_HOME="
for /d %%D in ("%~dp0..\..\AgriNav-CMS-dev-*") do set "CMS_HOME=%%D"
if not defined CMS_HOME (
  echo [ERROR] CMS project folder AgriNav-CMS-dev-* was not found.
  echo Expected layout: smart-agriculture\AgriNav-CMS-dev-*\Agri-CMS-ui-v3
  pause
  exit /b 1
)
set "UI_HOME=%CMS_HOME%\Agri-CMS-ui-v3"
if not exist "%UI_HOME%\package.json" (
  echo [ERROR] Frontend folder Agri-CMS-ui-v3 was not found.
  pause
  exit /b 1
)
cd /d "%UI_HOME%"

rem Node.js 18 or newer is required (package.json engines)
where node >nul 2>nul
if errorlevel 1 (
  echo [ERROR] Node.js was not found in PATH.
  echo Install Node.js 18+ from https://nodejs.org/ then run this script again.
  pause
  exit /b 1
)
echo Node version:
node -v

if not exist ".env.development" (
  echo [ERROR] .env.development was not found in:
  echo   %UI_HOME%
  echo Please do the following:
  echo   1^) copy .env.development.example .env.development
  echo   2^) fill in VITE_APP_AMAP_KEY and VITE_APP_AMAP_SECURITY_CODE
  echo      ^(ask the team leader, or apply at https://console.amap.com/dev/key/app^)
  echo   3^) run this script again
  pause
  exit /b 1
)

if not exist "node_modules" (
  echo First run: installing dependencies, this may take a few minutes...
  call npm install
  if errorlevel 1 (
    echo [ERROR] npm install failed. Check your network and try again.
    pause
    exit /b 1
  )
)

echo ============================================================
echo  Starting frontend dev server...
echo  Local URL : http://localhost:5180
echo  strictPort is ON: if 5180 is occupied the server will exit.
echo  API prefix /dev-api is proxied to VITE_APP_API_TARGET.
echo  Default target: http://82.156.224.217 (cloud demo backend)
echo  To use a local backend on port 8088, add to .env.development:
echo    VITE_APP_API_TARGET=http://localhost:8088
echo  Press Ctrl+C to stop the server.
echo ============================================================
call npm run dev
pause
