@echo off
setlocal
cd /d "%~dp0"
echo === Reddit Image Browser 0.1.2 ===
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java 17 is required. Install Android Studio with its bundled JDK or set JAVA_HOME.
  pause
  exit /b 1
)
if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT (
  if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
)
if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT (
  echo ERROR: Android SDK not found. Install Android Studio and set ANDROID_HOME.
  pause
  exit /b 1
)
set "GRADLE_EXE=%~dp0.tools\gradle-8.9\bin\gradle.bat"
if not exist "%GRADLE_EXE%" (
  echo Downloading Gradle 8.9 once to .tools ...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $d='.tools'; New-Item -ItemType Directory -Force $d ^| Out-Null; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.9-bin.zip' -OutFile "$d\gradle.zip"; Expand-Archive -Path "$d\gradle.zip" -DestinationPath $d -Force; Remove-Item "$d\gradle.zip""
  if errorlevel 1 (
    echo ERROR: Could not download Gradle. Check your internet connection.
    pause
    exit /b 1
  )
)
call "%GRADLE_EXE%" --no-daemon assembleDebug
if errorlevel 1 (
  echo BUILD FAILED. Review the errors above.
  pause
  exit /b 1
)
if not exist "output" mkdir "output"
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "output\RedditImageBrowser-0.1.2-debug.apk" >nul
echo.
echo SUCCESS: output\RedditImageBrowser-0.1.2-debug.apk
pause
