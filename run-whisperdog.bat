@echo off
REM WhisperDog Launcher Script
REM Simple batch file to launch WhisperDog JAR

setlocal

REM Check if JAR exists
set JAR_FILE=target\whisperdog-2.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found at %JAR_FILE%
    echo.
    echo Please build the project first:
    echo   mvn clean package
    echo.
    pause
    exit /b 1
)

REM Launch WhisperDog
echo Starting WhisperDog...
echo.
java -jar "%JAR_FILE%"

REM If Java fails
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Failed to launch WhisperDog!
    echo Make sure Java 11+ is installed and in your PATH.
    echo.
    pause
    exit /b 1
)
