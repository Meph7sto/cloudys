@echo off
setlocal
powershell -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
exit /b %errorlevel%
