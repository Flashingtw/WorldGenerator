@echo off
title WorldGenerator - Paper 26.1.2
cd /d "%~dp0"

echo Starting WorldGenerator test server...
echo Server console commands can be entered in this window.
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"

echo.
echo The server has stopped.
pause
