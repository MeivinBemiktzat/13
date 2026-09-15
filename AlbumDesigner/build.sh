#!/usr/bin/env bash
# Builds the Album Designer APK from source using aapt2 + javac + d8 + apksig.
# Requires the Android build jars in $TOOLS (see README).
set -euo pipefail
export JAVA_TOOL_OPTIONS=

HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

TOOLS="${TOOLS:-$HERE/../.tools}"
AAPT2="$TOOLS/aapt2"
ANDROID_JAR="$TOOLS/android.jar"
R8="$TOOLS/r8lib.jar"
APKSIG="$TOOLS/apksig.jar"

OUT="$HERE/build"
mkdir -p "$OUT"
if [ ! -f "$OUT/album.keystore" ]; then
  echo "==> generating signing keystore"
  keytool -genkeypair -keystore "$OUT/album.keystore" -storetype PKCS12 \
    -storepass album123 -keypass album123 -alias album -keyalg RSA -keysize 2048 \
    -validity 10950 -dname "CN=Album Designer, O=MikdashMelech, C=IL"
fi
GEN="$OUT/gen"
CLASSES="$OUT/classes"
DEX="$OUT/dex"
rm -rf "$GEN" "$CLASSES" "$DEX" "$OUT/res.zip" "$OUT/base.apk" "$OUT/unsigned.apk"
mkdir -p "$GEN" "$CLASSES" "$DEX"

echo "==> [1/6] aapt2 compile resources"
"$AAPT2" compile --dir res -o "$OUT/res.zip"

echo "==> [2/6] aapt2 link"
"$AAPT2" link \
  -o "$OUT/base.apk" \
  -I "$ANDROID_JAR" \
  --manifest AndroidManifest.xml \
  --java "$GEN" \
  -A assets \
  --min-sdk-version 24 \
  --target-sdk-version 33 \
  --version-code 1 --version-name 1.0 \
  "$OUT/res.zip"

echo "==> [3/6] javac"
find src "$GEN" -name '*.java' > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 \
  -classpath "$ANDROID_JAR" \
  -d "$CLASSES" @"$OUT/sources.txt" 2>&1 | grep -v 'warning:' || true

echo "==> [4/6] d8 -> classes.dex"
find "$CLASSES" -name '*.class' > "$OUT/classfiles.txt"
java -cp "$R8" com.android.tools.r8.D8 \
  --min-api 24 \
  --lib "$ANDROID_JAR" \
  --output "$DEX" \
  @"$OUT/classfiles.txt"

echo "==> [5/6] package dex into apk"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
( cd "$DEX" && zip -q "$OUT/unsigned.apk" classes.dex )

echo "==> [6/6] sign (v2)"
javac -encoding UTF-8 -classpath "$APKSIG" -d "$OUT" tools/Sign.java
java \
  --add-exports java.base/sun.security.x509=ALL-UNNAMED \
  --add-exports java.base/sun.security.pkcs=ALL-UNNAMED \
  -cp "$OUT:$APKSIG" Sign \
  "$OUT/unsigned.apk" "$OUT/AlbumDesigner.apk" \
  "$OUT/album.keystore" album123 album album123

echo "==> DONE: $OUT/AlbumDesigner.apk"
ls -lh "$OUT/AlbumDesigner.apk"
