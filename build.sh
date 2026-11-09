#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building BlueOS Widgets ==="

# 1. Locate Android SDK
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
    SDK_DIR="$ANDROID_HOME"
elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    SDK_DIR="$ANDROID_SDK_ROOT"
elif [ -d "$HOME/Android/Sdk" ]; then
    SDK_DIR="$HOME/Android/Sdk"
elif [ -d "$HOME/Library/Android/sdk" ]; then
    SDK_DIR="$HOME/Library/Android/sdk"
elif [ -n "$LOCALAPPDATA" ] && [ -d "$LOCALAPPDATA/Android/Sdk" ]; then
    SDK_DIR="$LOCALAPPDATA/Android/Sdk"
elif [ -d "$HOME/android-sdk" ]; then
    SDK_DIR="$HOME/android-sdk"
fi

# 2. Find android.jar
ANDROID_JAR=""
if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR/platforms" ]; then
    ANDROID_JAR=$(find "$SDK_DIR/platforms" -name "android.jar" 2>/dev/null | sort -V | tail -n 1 || true)
fi

if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
    if [ -f "$HOME/android-sdk/android.jar" ]; then
        ANDROID_JAR="$HOME/android-sdk/android.jar"
    elif [ -f "./android.jar" ]; then
        ANDROID_JAR="./android.jar"
    fi
fi

if [ -z "$ANDROID_JAR" ] || [ ! -f "$ANDROID_JAR" ]; then
    echo "Error: android.jar not found! Please set ANDROID_HOME or place android.jar in project root."
    exit 1
fi
echo "Using Android Platform: $ANDROID_JAR"

# 3. Locate build tools (aapt, d8, apksigner)
AAPT_BIN="aapt"
D8_BIN="d8"
APKSIGNER_BIN="apksigner"

if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR/build-tools" ]; then
    LATEST_BT=$(find "$SDK_DIR/build-tools" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | sort -V | tail -n 1 || true)
    if [ -n "$LATEST_BT" ] && [ -f "$LATEST_BT/aapt" ]; then
        AAPT_BIN="$LATEST_BT/aapt"
        D8_BIN="$LATEST_BT/d8"
        APKSIGNER_BIN="$LATEST_BT/apksigner"
    elif [ -n "$LATEST_BT" ] && [ -f "$LATEST_BT/aapt.exe" ]; then
        AAPT_BIN="$LATEST_BT/aapt.exe"
        D8_BIN="$LATEST_BT/d8.bat"
        APKSIGNER_BIN="$LATEST_BT/apksigner.bat"
    fi
fi

# 4. Locate or generate Keystore
KEYSTORE=""
if [ -f "./debug.keystore" ]; then
    KEYSTORE="./debug.keystore"
elif [ -f "$HOME/.android/debug.keystore" ]; then
    KEYSTORE="$HOME/.android/debug.keystore"
elif [ -f "$HOME/debug.keystore" ]; then
    KEYSTORE="$HOME/debug.keystore"
else
    echo "Generating debug.keystore..."
    keytool -genkeypair \
        -keystore ./debug.keystore \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -dname "CN=Android Debug,O=Android,C=US" \
        -validity 10000 \
        -keyalg RSA \
        -keysize 2048
    KEYSTORE="./debug.keystore"
fi

# Clean and create build dirs
rm -rf bin out
mkdir -p bin/classes out

echo "1. Generating R.java..."
"$AAPT_BIN" package -f -m \
    -J src \
    -M AndroidManifest.xml \
    -S res \
    -I "$ANDROID_JAR"

echo "2. Compiling Java sources..."
javac -encoding UTF-8 -cp "$ANDROID_JAR" \
    -d bin/classes \
    $(find src -name "*.java")

echo "3. Converting to DEX..."
"$D8_BIN" --output bin/ $(find bin/classes -name "*.class")

echo "4. Packaging APK..."
"$AAPT_BIN" package -f \
    -M AndroidManifest.xml \
    -S res \
    -I "$ANDROID_JAR" \
    -F bin/unsigned.apk

cd bin
"$AAPT_BIN" add unsigned.apk classes.dex
cd ..

echo "5. Signing APK..."
"$APKSIGNER_BIN" sign \
    --ks "$KEYSTORE" \
    --ks-pass pass:android \
    --out out/BlueOS-Widgets.apk \
    bin/unsigned.apk

# If on Android / Termux with external storage, also copy to Download
if [ -d "/sdcard/Download" ]; then
    mkdir -p /sdcard/Download/BlueOS_HyperOS
    cp out/BlueOS-Widgets.apk /sdcard/Download/BlueOS_HyperOS/BlueOS-Widgets.apk
    echo "Copied to: /sdcard/Download/BlueOS_HyperOS/BlueOS-Widgets.apk"
fi

echo "Done! Output APK: out/BlueOS-Widgets.apk"
