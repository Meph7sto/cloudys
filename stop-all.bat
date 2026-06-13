@echo off
setlocal
powershell -NoExit -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" %*
exit /b %errorlevel%
