@echo off
title AI Test Platform Frontend
cd /d "%~dp0"
echo ================================================
echo   AI Test Platform frontend starting...
echo   Browser: http://localhost:5173
echo   Keep this window OPEN while testing.
echo   Stop: press Ctrl+C then Y, or just close this window.
echo ================================================
call npm run dev
echo.
echo Frontend exited (stopped or error). Press any key to close.
pause >nul
