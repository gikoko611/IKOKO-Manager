#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - app icon + in-app logo setup (Termux)
#
# Uses:
#   /storage/emulated/0/Download/download (6).jpeg  -> launcher icon
#   /storage/emulated/0/Download/download (5).jpeg  -> logo shown on the main screen
#
# Run:  bash scripts/setup_images.sh
#
set -e

SRC_ICON="/storage/emulated/0/Download/download (6).jpeg"
SRC_LOGO="/storage/emulated/0/Download/download (5).jpeg"

REPO="$HOME/IKOKO-Manager"
[ -d "$REPO" ] || git clone https://github.com/gikoko611/IKOKO-Manager.git "$REPO"
RES="$REPO/app/src/main/res"

command -v magick >/dev/null 2>&1 || pkg install -y imagemagick

for f in "$SRC_ICON" "$SRC_LOGO"; do
  [ -f "$f" ] || { echo "Missing image: $f"; exit 1; }
done

# 1) Launcher icon -> every density bucket.
#    Density PNGs take precedence over the mipmap-anydpi vector fallback,
#    so the real icon shows on all devices.
for pair in "mdpi 48" "hdpi 72" "xhdpi 96" "xxhdpi 144" "xxxhdpi 192"; do
  set -- $pair
  mkdir -p "$RES/mipmap-$1"
  magick "$SRC_ICON" -resize "${2}x${2}^" -gravity center -extent "${2}x${2}" \
    "$RES/mipmap-$1/ic_launcher.png"
done

# 2) In-app logo -> replaces the placeholder vector.
magick "$SRC_LOGO" -resize 1024x1024\> "$RES/drawable/ikoko_logo.png"
rm -f "$RES/drawable/ikoko_logo.xml"

echo ""
echo "Done - icon + logo installed."
echo "Commit:  cd $REPO && git add -A && git commit -m 'app icon + logo' && git push"
