@echo off
title AgriNav First-time Setup - SSH keyless login
echo ============================================================
echo   First-time setup: generate a key and install it on the server.
echo   You only need to run this ONCE on this computer.
echo ============================================================
echo.
set "KEY=%USERPROFILE%\.ssh\id_ed25519"
if not exist "%KEY%" (
  echo No key found. Generating an ed25519 key (no passphrase)...
  ssh-keygen -t ed25519 -N "" -f "%KEY%"
) else (
  echo An existing key was found. Skipping key generation.
)
echo.
echo You will now be asked to enter the server login password ONCE.
echo Note: the password is not displayed while you type it.
echo.
type "%KEY%.pub" | ssh root@82.156.224.217 "mkdir -p ~/.ssh && cat >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys"
echo.
echo ============================================================
echo   Done. From now on just double-click tunnel.bat to connect.
echo ============================================================
pause
