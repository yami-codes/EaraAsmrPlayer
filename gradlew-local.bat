@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "GRADLE_USER_HOME=%PROJECT_DIR%.gradle-user-home"
set "TEMP=%PROJECT_DIR%.gradle-temp"
set "TMP=%PROJECT_DIR%.gradle-temp"

if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"
if not exist "%TEMP%" mkdir "%TEMP%"

call "%PROJECT_DIR%gradlew.bat" %*
exit /b %ERRORLEVEL%
