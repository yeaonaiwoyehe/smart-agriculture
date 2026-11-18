@echo off
title AgriNav SSH Tunnel - cloud shared databases
echo ============================================================
echo   AgriNav SSH Tunnel (cloud shared databases)
echo   Keep this window OPEN while developing. Close it to stop.
echo   Local port mappings:
echo     MySQL         127.0.0.1:13306
echo     PostgreSQL    127.0.0.1:15432
echo     Redis         127.0.0.1:16379
echo     Elasticsearch 127.0.0.1:19200
echo ============================================================
echo.
ssh -N -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -L 13306:127.0.0.1:3306 -L 15432:127.0.0.1:5432 -L 16379:127.0.0.1:6379 -L 19200:127.0.0.1:9200 root@82.156.224.217
echo.
echo Tunnel disconnected. Press any key to close.
pause >nul
