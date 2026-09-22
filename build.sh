#!/usr/bin/env bash
# Prisma — Gradle-free Android build using the user-provided toolchain:
#   aapt2 (resource compile/link), javac (JDK 21), d8 (from r8lib.jar), jarsigner.
# Produces build/apk/prisma-unsigned.apk and, if a keystore exists, a v1-signed
# APK. Final v2/v3 signing is done by sign.sh once apksigner is available.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$ROOT/tools"
APP="$ROOT/app/src/main"
OUT="$ROOT/build"
GEN="$OUT/gen"
CLASSES="$OUT/classes"
DEX="$OUT/dex"
APKDIR="$OUT/apk"
PKG="com.prisma.match3"

MIN_SDK=21
TARGET_SDK=33

AAPT2="$TOOLS/aapt2"
ANDROID_JAR="$TOOLS/android.jar"
R8="$TOOLS/r8lib.jar"

rm -rf "$GEN" "$CLASSES" "$DEX" "$APKDIR"
mkdir -p "$GEN" "$CLASSES" "$DEX" "$APKDIR" "$OUT/flat"

echo "==> [1/6] aapt2 compile resources"
"$AAPT2" compile --dir "$APP/res" -o "$OUT/flat/res.zip"

echo "==> [2/6] aapt2 link"
"$AAPT2" link \
    -o "$APKDIR/base.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$APP/AndroidManifest.xml" \
    --java "$GEN" \
    --min-sdk-version "$MIN_SDK" \
    --target-sdk-version "$TARGET_SDK" \
    --version-code 1 --version-name 1.0 \
    "$OUT/flat/res.zip"

echo "==> [3/6] javac"
find "$ROOT/app/src/main/java" "$GEN" -name '*.java' > "$OUT/sources.txt"
javac -source 8 -target 8 -encoding UTF-8 \
    -classpath "$ANDROID_JAR" \
    -d "$CLASSES" \
    @"$OUT/sources.txt" 2> "$OUT/javac.log" || { cat "$OUT/javac.log"; exit 1; }
grep -i warning "$OUT/javac.log" | head -3 || true

echo "==> [4/6] jar classes"
( cd "$CLASSES" && jar cf "$OUT/classes.jar" . )

echo "==> [5/6] d8 -> dex"
java -cp "$R8" com.android.tools.r8.D8 \
    --release --min-api "$MIN_SDK" \
    --lib "$ANDROID_JAR" \
    --output "$DEX" \
    "$OUT/classes.jar" 2> "$OUT/d8.log" || { cat "$OUT/d8.log"; exit 1; }

echo "==> [6/6] package apk"
cp "$APKDIR/base.apk" "$APKDIR/prisma-unsigned.apk"
( cd "$DEX" && zip -q "$APKDIR/prisma-unsigned.apk" classes.dex )
echo "Unsigned APK: $APKDIR/prisma-unsigned.apk"

# Interim v1 signing (installs on Android <= 10 / target<=29). Android 13 needs
# v2 via apksigner: run sign.sh once build-tools is provided.
KS="$ROOT/prisma.keystore"
if [ -f "$KS" ]; then
    echo "==> jarsigner (v1 interim)"
    cp "$APKDIR/prisma-unsigned.apk" "$APKDIR/prisma-v1.apk"
    jarsigner -keystore "$KS" -storepass prisma123 -keypass prisma123 \
        -digestalg SHA-256 -sigalg SHA256withRSA \
        "$APKDIR/prisma-v1.apk" prisma >/dev/null
    echo "v1-signed APK: $APKDIR/prisma-v1.apk"
fi
echo "BUILD OK"
