#!/usr/bin/env bash
# Bundle the deliverable ZIP: the installable APK (release if signed with
# apksigner, otherwise the interim v1 APK) plus full source and build scripts.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
APKDIR="$ROOT/build/apk"
DIST="$ROOT/dist"
STAGE="$DIST/Prisma"
rm -rf "$DIST"; mkdir -p "$STAGE"

if [ -f "$APKDIR/prisma-release.apk" ]; then
    cp "$APKDIR/prisma-release.apk" "$STAGE/Prisma.apk"
    SIG="v2/v3 (Android 13 ready)"
elif [ -f "$APKDIR/prisma-v1.apk" ]; then
    cp "$APKDIR/prisma-v1.apk" "$STAGE/Prisma.apk"
    SIG="v1 interim (installs on Android <= 10; run sign.sh for v2/v3)"
else
    echo "No APK found; run ./build.sh"; exit 1
fi

mkdir -p "$STAGE/source"
cp -r "$ROOT/app" "$STAGE/source/"
cp "$ROOT/build.sh" "$ROOT/sign.sh" "$ROOT/package.sh" "$STAGE/source/"
[ -f "$ROOT/README.md" ] && cp "$ROOT/README.md" "$STAGE/"
echo "Signature: $SIG" > "$STAGE/APK-INFO.txt"

( cd "$DIST" && zip -qr "$ROOT/Prisma.zip" Prisma )
echo "Packaged: $ROOT/Prisma.zip  (APK signature: $SIG)"
ls -la "$ROOT/Prisma.zip"
