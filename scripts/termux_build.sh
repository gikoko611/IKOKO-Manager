#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - build the debug APK entirely on-device (Termux).
#
# Run:  bash scripts/termux_build.sh
# Requires a one-time:  bash scripts/termux_setup.sh
#
set -e

REPO_URL="https://github.com/gikoko611/IKOKO-Manager.git"
DIR="$HOME/IKOKO-Manager"
OUT="/storage/emulated/0/Download/I-KO-KO-Manager-debug.apk"

[ -d "$DIR" ] || git clone "$REPO_URL" "$DIR"
cd "$DIR"
git pull --rebase

# 1) Branding: launcher icons + logo generated from your images
#    (branding/icon + branding/logo in the repo, or the Download folder).
#    Skipped automatically if no source image exists anywhere - e.g. when
#    ready-made icon resources were committed directly.
bash scripts/setup_images.sh \
  || echo "Branding: no source images found - keeping existing resources."

# 2) Point Gradle at the Termux Android SDK.
echo "sdk.dir=$HOME/android-sdk" > local.properties

# 3) Gradle wrapper (first build only).
[ -x ./gradlew ] || gradle wrapper --gradle-version 8.9 --no-daemon

# 4) Build.
./gradlew --no-daemon assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk "$OUT"
echo ""
echo "APK ready: $OUT"
