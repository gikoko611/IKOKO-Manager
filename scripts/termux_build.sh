#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - build the debug APK entirely on-device (Termux).
#
# Run:  bash scripts/termux_build.sh
# Requires a one-time:  bash scripts/termux_setup.sh
#
set -e

# Keep Android from killing Termux during the long first build.
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock

REPO_URL="https://github.com/gikoko611/IKOKO-Manager.git"
DIR="$HOME/IKOKO-Manager"
OUT="/storage/emulated/0/Download/I-KO-KO-Manager-debug.apk"

[ -d "$DIR" ] || git clone "$REPO_URL" "$DIR"
cd "$DIR"
git pull --rebase

# 0) The one-time setup must have been run.
[ -d "$HOME/android-sdk/cmdline-tools/latest" ] || {
  echo "Android SDK not found."
  echo "Run this ONCE first:  bash scripts/termux_setup.sh"
  exit 1
}

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

# 4) Build (the first build downloads a lot - keep Termux open!).
./gradlew --no-daemon assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk "$OUT"
echo ""
echo "APK ready: $OUT"
