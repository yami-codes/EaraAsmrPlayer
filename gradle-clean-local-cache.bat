@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "BUILD_DIR=%PROJECT_DIR%.build_asmr_player_android"
set "TEMP_DIR=%PROJECT_DIR%.gradle-temp"
set "LOCAL_BUILD_CACHE=%PROJECT_DIR%.gradle-user-home\caches\build-cache-1"

call "%PROJECT_DIR%gradlew-local.bat" --stop

if exist "%BUILD_DIR%" (
    rmdir /s /q "%BUILD_DIR%"
)

if exist "%TEMP_DIR%" (
    rmdir /s /q "%TEMP_DIR%"
)

if exist "%LOCAL_BUILD_CACHE%" (
    rmdir /s /q "%LOCAL_BUILD_CACHE%"
)

echo Local Gradle build artifacts and temporary cache cleaned.
