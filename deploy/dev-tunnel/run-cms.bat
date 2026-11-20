@echo off
title AgriNav CMS - local run against cloud database
set "ENVFILE=%~dp0dev-local.env"
if not exist "%ENVFILE%" (
  echo [ERROR] dev-local.env was not found.
  echo Please copy dev-local.env.example to dev-local.env, fill in the passwords
  echo given by the team leader, then run this script again.
  echo.
  pause
  exit /b 1
)

rem Load variables from dev-local.env (skip comments and blank lines)
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%ENVFILE%") do (
  if not "%%b"=="" set "%%a=%%b"
)

rem Locate the CMS project folder (folder name starts with AgriNav-CMS-dev-)
set "CMS_HOME="
for /d %%D in ("%~dp0..\..\AgriNav-CMS-dev-*") do set "CMS_HOME=%%D"
if not defined CMS_HOME (
  echo [ERROR] CMS project folder was not found.
  pause
  exit /b 1
)

echo ============================================================
echo  Starting CMS backend...
echo  Connecting directly to the public cloud database (no tunnel).
echo  Project: %CMS_HOME%
echo ============================================================
cd /d "%CMS_HOME%"

rem Step 1: install all modules to the local repository (first run takes longer)
echo [Step 1/2] Building and installing modules...
call mvn install -DskipTests -q
if errorlevel 1 (
  echo [ERROR] Maven build failed. Check the messages above.
  pause
  exit /b 1
)

rem Step 2: run the admin module only (this is where the main class lives)
echo [Step 2/2] Starting Spring Boot (Agri-CMS-admin on port 8088)...
cd Agri-CMS-admin
call mvn spring-boot:run
echo.
echo CMS process ended. Press any key to close.
pause >nul
