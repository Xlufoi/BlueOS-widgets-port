@echo off
setlocal enabledelayedexpansion

echo === Building BlueOS Widgets on Windows ===

:: 1. Locate Android SDK
if not "%ANDROID_HOME%"=="" (
    set "SDK_DIR=%ANDROID_HOME%"
) else if not "%ANDROID_SDK_ROOT%"=="" (
    set "SDK_DIR=%ANDROID_SDK_ROOT%"
) else if exist "%LOCALAPPDATA%\Android\Sdk" (
    set "SDK_DIR=%LOCALAPPDATA%\Android\Sdk"
) else (
    echo Error: Android SDK not found! Please set ANDROID_HOME.
    exit /b 1
)

:: 2. Find android.jar (latest platform)
set "ANDROID_JAR="
for /d %%P in ("%SDK_DIR%\platforms\android-*") do (
    if exist "%%P\android.jar" set "ANDROID_JAR=%%P\android.jar"
)

if "%ANDROID_JAR%"=="" (
    echo Error: android.jar not found in %SDK_DIR%\platforms!
    exit /b 1
)
echo Using Android Platform: %ANDROID_JAR%

:: 3. Find build-tools
set "BT_DIR="
for /d %%B in ("%SDK_DIR%\build-tools\*") do (
    if exist "%%B\aapt.exe" set "BT_DIR=%%B"
)

if "%BT_DIR%"=="" (
    echo Error: build-tools not found in %SDK_DIR%\build-tools!
    exit /b 1
)
echo Using Build-Tools: %BT_DIR%

:: 4. Keystore
set "KEYSTORE=debug.keystore"
if not exist "%KEYSTORE%" (
    if exist "%USERPROFILE%\.android\debug.keystore" (
        set "KEYSTORE=%USERPROFILE%\.android\debug.keystore"
    ) else (
        echo Generating debug.keystore...
        keytool -genkeypair -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -dname "CN=Android Debug,O=Android,C=US" -validity 10000 -keyalg RSA -keysize 2048
    )
)

if exist bin rmdir /s /q bin
if exist out rmdir /s /q out
mkdir bin\classes
mkdir out

echo 1. Generating R.java...
"%BT_DIR%\aapt.exe" package -f -m -J src -M AndroidManifest.xml -S res -I "%ANDROID_JAR%"
if errorlevel 1 exit /b 1

echo 2. Compiling Java sources...
dir /s /b src\*.java > bin\sources.txt
javac -encoding UTF-8 -cp "%ANDROID_JAR%" -d bin\classes @bin\sources.txt
if errorlevel 1 exit /b 1

echo 3. Converting to DEX...
dir /s /b bin\classes\*.class > bin\classes.txt
call "%BT_DIR%\d8.bat" --output bin\ @bin\classes.txt
if errorlevel 1 exit /b 1

echo 4. Packaging APK...
"%BT_DIR%\aapt.exe" package -f -M AndroidManifest.xml -S res -I "%ANDROID_JAR%" -F bin\unsigned.apk
if errorlevel 1 exit /b 1

cd bin
"%BT_DIR%\aapt.exe" add unsigned.apk classes.dex
cd ..

echo 5. Signing APK...
call "%BT_DIR%\apksigner.bat" sign --ks "%KEYSTORE%" --ks-pass pass:android --out out\BlueOS-Widgets.apk bin\unsigned.apk
if errorlevel 1 exit /b 1

echo Done! Output APK: out\BlueOS-Widgets.apk
