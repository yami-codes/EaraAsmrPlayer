@echo off
setlocal
cd /d "%~dp0"
echo Building Eara ASMR Player for Windows...
call gradlew.bat :desktopApp:packageMsi
if %ERRORLEVEL% NEQ 0 (
  echo Build failed.
  exit /b %ERRORLEVEL%
)
echo Windows MSI package is ready under desktopApp\build\compose\binaries\main\msi\
endlocal
