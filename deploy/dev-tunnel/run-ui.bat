@echo off
title AgriNav CMS UI - dev server
rem Fix webpack 4 + Node 17+ "digital envelope routines unsupported" error
set "NODE_OPTIONS=--openssl-legacy-provider"

rem Locate the CMS project folder, then the frontend folder inside it
set "CMS_HOME="
for /d %%D in ("%~dp0..\..\AgriNav-CMS-dev-*") do set "CMS_HOME=%%D"
if not defined CMS_HOME (
  echo [ERROR] CMS project folder was not found.
  pause
  exit /b 1
)
set "UI_HOME=%CMS_HOME%\Agri-CMS-ui"
if not exist "%UI_HOME%" (
  echo [ERROR] Frontend folder Agri-CMS-ui was not found.
  pause
  exit /b 1
)
cd /d "%UI_HOME%"

if not exist ".env.development" (
  echo [ERROR] .env.development was not found.
  echo Copy .env.development.example to .env.development and fill in the Amap key
  echo given by the team leader, then run this script again.
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
echo  Starting frontend dev server (default port 80, auto-opens).
echo  Make sure the CMS backend is running on port 8088.
echo ============================================================
call npm run dev
pause
