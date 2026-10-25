#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - one-time build environment setup for Termux.
# Installs JDK 17, aapt2, ImageMagick, git, gradle, gh (GitHub CLI) and the
# Android SDK command-line tools, then writes the Termux-specific Gradle
# settings.
#
# Run once:  bash scripts/termux_setup.sh
#
set -e

# Keep Android from killing Termux during the long install.
command -v termux-wake-lock >/dev/null 2>&1 && termux-wake-lock

echo "== 1/5 Termux packages =="
pkg update -y
pkg install -y openjdk-17 aapt2 git gh gradle imagemagick unzip curl

echo "== 2/5 Shared storage access =="
termux-setup-storage

echo "== 3/5 Android SDK command-line tools =="
SDK="$HOME/android-sdk"
mkdir -p "$SDK/cmdline-tools"
if [ ! -d "$SDK/cmdline-tools/latest" ]; then
  curl -fLo "$TMPDIR/cmdline-tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q "$TMPDIR/cmdline-tools.zip" -d "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -f "$TMPDIR/cmdline-tools.zip"
fi

echo "== 4/5 SDK platform 35 + build tools (this can take a while) =="
yes | bash "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null
bash "$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-35" "build-tools;35.0.0"

echo "== 5/5 Gradle settings for Termux =="
mkdir -p "$HOME/.gradle"
cat > "$HOME/.gradle/gradle.properties" <<'EOF'
# Machine-specific Termux build settings (not part of the repo).
org.gradle.java.home=/data/data/com.termux/files/usr/lib/jvm/openjdk-17
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
EOF

echo ""
echo "Setup done. Build anytime with:  bash scripts/termux_build.sh"
