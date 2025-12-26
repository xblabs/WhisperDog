@echo off
REM WhisperDog Windows Installer Build Script
REM Requires JDK 17+ with jpackage (included in JDK 16+)

echo Building WhisperDog Windows Installer...
echo.

REM Step 1: Build JAR with Maven
echo [1/2] Building JAR with Maven...
call mvn clean package
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven build failed!
    exit /b 1
)
echo.

REM Step 2: Create Windows Installer with jpackage
echo [2/2] Creating Windows installer with jpackage...
jpackage ^
    --type exe ^
    --input target ^
    --name WhisperDog ^
    --main-jar whisperdog-2.0.0-jar-with-dependencies.jar ^
    --main-class org.whisperdog.AudioRecorderUI ^
    --icon src/main/resources/whisperdog.ico ^
    --app-version 2.0.0 ^
    --vendor "xblabs" ^
    --copyright "MIT License" ^
    --description "Audio transcription tool with Whisper API integration" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --dest installer-output

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: jpackage failed! Make sure you have JDK 17+ installed.
    exit /b 1
)

echo.
echo ========================================
echo SUCCESS! Installer created at:
echo installer-output\WhisperDog-2.0.0.exe
echo ========================================
pause
