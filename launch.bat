@echo off
cd /d %~dp0

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven не знайдений! Будь ласка, встановіть Maven і переконайтесь, що він доданий до PATH.
    pause
    exit /b
)

mvn clean javafx:run

pause
