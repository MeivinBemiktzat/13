#!/usr/bin/env bash
# Final APK signing with APK Signature Scheme v2/v3 (required to install on
# Android 11+ with targetSdk 30+). Needs apksigner and zipalign, which ship in
# Android SDK Build-Tools. Point BUILD_TOOLS at an extracted build-tools dir, or
# drop `apksigner`, `lib/apksigner.jar` and `zipalign` into ./tools.
#
#   Source: https://dl.google.com/android/repository/build-tools_r34-linux.zip
#
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$ROOT/tools"
APKDIR="$ROOT/build/apk"
KS="$ROOT/prisma.keystore"
UNSIGNED="$APKDIR/prisma-unsigned.apk"
ALIGNED="$APKDIR/prisma-aligned.apk"
RELEASE="$APKDIR/prisma-release.apk"

BUILD_TOOLS="${BUILD_TOOLS:-$TOOLS}"

find_tool() {
    local name="$1"
    for d in "$BUILD_TOOLS" "$TOOLS"; do
        [ -x "$d/$name" ] && { echo "$d/$name"; return 0; }
    done
    command -v "$name" 2>/dev/null && return 0
    return 1
}

ZIPALIGN="$(find_tool zipalign || true)"
APKSIGNER="$(find_tool apksigner || true)"
APKSIGNER_JAR=""
[ -f "$BUILD_TOOLS/lib/apksigner.jar" ] && APKSIGNER_JAR="$BUILD_TOOLS/lib/apksigner.jar"
[ -z "$APKSIGNER_JAR" ] && [ -f "$TOOLS/apksigner.jar" ] && APKSIGNER_JAR="$TOOLS/apksigner.jar"

if [ -z "$ZIPALIGN" ] || { [ -z "$APKSIGNER" ] && [ -z "$APKSIGNER_JAR" ]; }; then
    echo "ERROR: missing signing tools."
    echo "  zipalign : ${ZIPALIGN:-NOT FOUND}"
    echo "  apksigner: ${APKSIGNER:-${APKSIGNER_JAR:-NOT FOUND}}"
    echo "Provide Android SDK Build-Tools (see header) and re-run:"
    echo "  BUILD_TOOLS=/path/to/build-tools/34.0.0 ./sign.sh"
    exit 2
fi

[ -f "$UNSIGNED" ] || { echo "Run ./build.sh first (missing $UNSIGNED)"; exit 1; }

echo "==> zipalign"
"$ZIPALIGN" -f -p 4 "$UNSIGNED" "$ALIGNED"

echo "==> apksigner (v1+v2+v3)"
if [ -n "$APKSIGNER" ]; then
    "$APKSIGNER" sign --ks "$KS" --ks-key-alias prisma \
        --ks-pass pass:prisma123 --key-pass pass:prisma123 \
        --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
        --out "$RELEASE" "$ALIGNED"
    "$APKSIGNER" verify --verbose "$RELEASE"
else
    java -jar "$APKSIGNER_JAR" sign --ks "$KS" --ks-key-alias prisma \
        --ks-pass pass:prisma123 --key-pass pass:prisma123 \
        --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
        --out "$RELEASE" "$ALIGNED"
    java -jar "$APKSIGNER_JAR" verify --verbose "$RELEASE"
fi

echo "Release APK (Android 13 ready): $RELEASE"
