@echo off
REM Create a portable WhisperDog package (JAR + launcher script)
REM This creates a simple ZIP that users can extract and run

setlocal

echo Creating portable WhisperDog package...
echo.

REM Build if needed
if not exist "target\whisperdog-2.0.0-jar-with-dependencies.jar" (
    echo Building JAR first...
    call mvn clean package
    if %ERRORLEVEL% NEQ 0 (
        echo ERROR: Build failed!
        exit /b 1
    )
)

REM Create portable directory
set PORTABLE_DIR=portable-whisperdog
if exist "%PORTABLE_DIR%" rmdir /s /q "%PORTABLE_DIR%"
mkdir "%PORTABLE_DIR%"

REM Copy JAR
echo Copying JAR file...
copy "target\whisperdog-2.0.0-jar-with-dependencies.jar" "%PORTABLE_DIR%\WhisperDog.jar"

REM Create launcher script in portable directory
echo Creating launcher script...
(
echo @echo off
echo REM WhisperDog Portable Launcher
echo.
echo java -jar WhisperDog.jar
echo.
echo if %%ERRORLEVEL%% NEQ 0 ^(
echo     echo ERROR: Failed to launch WhisperDog!
echo     echo Make sure Java 11+ is installed.
echo     pause
echo ^)
) > "%PORTABLE_DIR%\WhisperDog.bat"

REM Copy README
echo Creating README...
(
echo WhisperDog Portable v2.0.0
echo ===================================
echo.
echo Requirements:
echo - Java 11 or higher
echo.
echo To run:
echo 1. Double-click WhisperDog.bat
echo    OR
echo 2. Run from command line: java -jar WhisperDog.jar
echo.
echo Settings are saved to:
echo %%APPDATA%%\WhisperDog\config.properties
echo.
echo Your settings persist between runs!
echo.
echo For more info: https://github.com/xblabs/whisperdog-xb
) > "%PORTABLE_DIR%\README.txt"

REM Create ZIP (requires PowerShell)
echo Creating ZIP archive...
powershell -command "Compress-Archive -Path '%PORTABLE_DIR%\*' -DestinationPath 'WhisperDog-Portable.zip' -Force"

echo.
echo ========================================
echo SUCCESS! Portable package created:
echo WhisperDog-Portable.zip
echo.
echo Contents:
echo - WhisperDog.jar
echo - WhisperDog.bat (launcher)
echo - README.txt
echo ========================================
pause
