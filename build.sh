#!/usr/bin/env bash
# Build APK Android KHÔNG cần Gradle: aapt -> javac -> dx/d8 -> zipalign -> apksigner.
#   ./build.sh          build APK debug đã ký: build/planet-conquest-<version>.apk
#   ./build.sh test     chạy kiểm thử engine trên JVM (tools/EngineSim.java)
#   ./build.sh install  build rồi cài lên thiết bị qua adb
# Biến môi trường: ANDROID_HOME (mặc định /usr/lib/android-sdk), PLATFORM_JAR, BUILD_TOOLS
set -euo pipefail
cd "$(dirname "$0")"

APP_ID=com.planetconquest.game
VERSION_CODE=2
VERSION_NAME=0.2.0
MIN_SDK=21
TARGET_SDK=34
OUT=build
SRC=app/src/main
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8"

if [ "${1:-apk}" = "test" ]; then
  rm -rf "$OUT/test" && mkdir -p "$OUT/test"
  javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -d "$OUT/test" \
    $(find "$SRC/java/com/planetconquest/game/engine" -name '*.java') tools/EngineSim.java
  java $JAVA_OPTS -cp "$OUT/test" EngineSim
  exit $?
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/lib/android-sdk}}"
PLATFORM_JAR="${PLATFORM_JAR:-$(ls -d "$SDK"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1)}"
BT="${BUILD_TOOLS:-$(ls -d "$SDK"/build-tools/[0-9]*/ 2>/dev/null | sort -V | tail -1)}"
BT="${BT%/}"
[ -f "$PLATFORM_JAR" ] || { echo "Không tìm thấy android.jar (đặt ANDROID_HOME hoặc PLATFORM_JAR)"; exit 1; }
[ -x "$BT/aapt" ] || { echo "Không tìm thấy build-tools/aapt (đặt BUILD_TOOLS)"; exit 1; }
echo "SDK platform : $PLATFORM_JAR"
echo "Build tools  : $BT"

rm -rf "$OUT/gen" "$OUT/obj" "$OUT/apk" && mkdir -p "$OUT/gen" "$OUT/obj" "$OUT/apk"

# Manifest nguồn không khai báo package/uses-sdk (để tương thích Gradle AGP 8); chèn khi build CLI.
sed -e "s|<manifest |<manifest package=\"$APP_ID\" android:versionCode=\"$VERSION_CODE\" android:versionName=\"$VERSION_NAME\" |" \
    -e "s|<application|<uses-sdk android:minSdkVersion=\"$MIN_SDK\" android:targetSdkVersion=\"$TARGET_SDK\" />\n\n    <application|" \
    "$SRC/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"

echo "[1/5] aapt: đóng gói tài nguyên"
"$BT/aapt" package -f -M "$OUT/AndroidManifest.xml" -S "$SRC/res" -I "$PLATFORM_JAR" -J "$OUT/gen" -F "$OUT/apk/unaligned.apk"

echo "[2/5] javac: biên dịch Java"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -Xlint:-deprecation -bootclasspath "$PLATFORM_JAR" \
  -d "$OUT/obj" $(find "$SRC/java" "$OUT/gen" -name '*.java')

echo "[3/5] dex"
if [ -x "$BT/d8" ]; then
  "$BT/d8" --min-api $MIN_SDK --lib "$PLATFORM_JAR" --output "$OUT/apk" $(find "$OUT/obj" -name '*.class')
else
  "$BT/dx" --dex --min-sdk-version=$MIN_SDK --output="$OUT/apk/classes.dex" "$OUT/obj"
fi
(cd "$OUT/apk" && "$BT/aapt" add unaligned.apk classes.dex > /dev/null)

echo "[4/5] zipalign"
"$BT/zipalign" -f -p 4 "$OUT/apk/unaligned.apk" "$OUT/apk/aligned.apk"

echo "[5/5] ký APK (debug keystore)"
KS="${KEYSTORE:-$OUT/debug.keystore}"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" > /dev/null 2>&1
fi
APK="$OUT/planet-conquest-$VERSION_NAME.apk"
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android --out "$APK" "$OUT/apk/aligned.apk"
"$BT/apksigner" verify "$APK"
echo "Xong: $APK ($(du -h "$APK" | cut -f1))"

if [ "${1:-}" = "install" ]; then
  adb install -r "$APK"
  adb shell am start -n "$APP_ID/.MainActivity"
fi
