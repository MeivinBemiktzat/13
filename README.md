# Prisma — 3D Match Odyssey

A fully offline, original 3D Match-3 game for Android, built without Gradle
using a minimal command-line toolchain. No internet, no servers, no cloud APIs,
no third-party game engine — pure Android framework + OpenGL ES 2.0.

## Highlights
- **Real 3D rendering** (OpenGL ES 2.0): every gem is a procedurally generated,
  flat-shaded faceted crystal with diffuse + specular + rim lighting and facet
  sheen; additive particle bursts; an animated nebula backdrop. No external 3D
  model or texture assets are required.
- **Deep match-3 engine**: cascades/combos, special gems (row/column stripes,
  5×5 bomb, color nova), obstacles (jelly, ice, stone), deadlock detection with
  auto-shuffle, hint search, per-move scoring with combo multipliers.
- **300 levels** (`LevelFactory`) with escalating difficulty and rotating
  objectives: score targets, color collection, jelly clearing, ice breaking.
- **Progression & meta**: per-level stars and high scores, coins, power-ups
  (hammer, shuffle, +5 moves), 9 achievements, sound toggle — all persisted
  locally via SharedPreferences.
- **Polished UI** built entirely in code: menu, scrollable level map, in-game
  HUD, win/lose dialogs, achievements — icon-only (vector icons, no emoji).
- **Android 13 target** (`targetSdk 33`), compatible down to `minSdk 21`
  (Android 5.0).

## Project layout
```
app/src/main/java/com/prisma/match3/
  engine/   game rules: Board, LevelFactory, matching, specials, objectives
  render/   OpenGL ES: GameRenderer, GemMesh, ParticleSystem, shaders, GameView
  ui/       Ui helpers, IconView (vector icons), Sfx
  MainActivity.java   screen flow + HUD + GameListener
app/src/main/res/       manifest resources, adaptive launcher icon
tools/                  aapt2, android.jar, r8lib.jar (provided)
build.sh                aapt2 -> javac -> d8 -> package -> jarsigner (v1)
sign.sh                 zipalign + apksigner (v2/v3) — needs Build-Tools
package.sh              bundle APK + source into Prisma.zip
```

## Building
```
./build.sh      # produces build/apk/prisma-unsigned.apk and prisma-v1.apk
./sign.sh       # produces build/apk/prisma-release.apk  (needs apksigner+zipalign)
./package.sh    # bundles Prisma.zip
```

### Toolchain used
- JDK 21 (`javac`, `jar`, `jarsigner`, `keytool`)
- `aapt2` (resource compile/link)
- `d8` from `r8lib.jar` (dexing)

### Final signing (required for Android 13 install)
Apps targeting SDK 30+ must carry an **APK Signature Scheme v2/v3** signature
to install on Android 11+. That requires `apksigner` and `zipalign` from the
**Android SDK Build-Tools**, which are not part of the provided toolchain:

    https://dl.google.com/android/repository/build-tools_r34-linux.zip

Extract it and run:

    BUILD_TOOLS=/path/to/build-tools/34.0.0 ./sign.sh
    ./package.sh

The interim `prisma-v1.apk` is JAR-signed (v1) only and installs on Android 10
and older; use `sign.sh` for the Android-13-ready release build.
