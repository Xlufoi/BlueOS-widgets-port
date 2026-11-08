#!/data/data/com.termux/files/usr/bin/bash
set -e
cd ~/BlueOSWidgetsApp
ANDROID_JAR=~/android-sdk/android.jar

echo "1. Generating R.java..."
aapt package -f -m \
    -J src \
    -M AndroidManifest.xml \
    -S res \
    -I $ANDROID_JAR

echo "2. Compiling Java sources..."
rm -rf bin/classes
mkdir -p bin/classes
javac -cp $ANDROID_JAR \
    -d bin/classes \
    $(find src -name "*.java")

echo "3. Converting to DEX..."
d8 --output bin/ $(find bin/classes -name "*.class")

echo "4. Packaging APK..."
aapt package -f \
    -M AndroidManifest.xml \
    -S res \
    -I $ANDROID_JAR \
    -F bin/unsigned.apk

cd bin
aapt add unsigned.apk classes.dex
cd ..

echo "5. Signing APK..."
mkdir -p /sdcard/Download/BlueOS_HyperOS
apksigner sign --ks ~/debug.keystore --ks-pass pass:android --out /sdcard/Download/BlueOS_HyperOS/BlueOS-Widgets.apk bin/unsigned.apk

echo "Done! Output: /sdcard/Download/BlueOS_HyperOS/BlueOS-Widgets.apk"
