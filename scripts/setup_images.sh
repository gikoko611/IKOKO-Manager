#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - generate branding resources from your own images (Termux).
#
#   icon  /storage/emulated/0/Download/download (6).jpeg  -> launcher icons
#          (legacy square + round + adaptive-icon foreground, all densities)
#   logo  /storage/emulated/0/Download/download (5).jpeg  -> top banner logo
#
# Run from anywhere:   bash scripts/setup_images.sh
#   or custom paths:   bash scripts/setup_images.sh <icon> <logo>
#
# scripts/termux_build.sh runs this automatically before every build.
#
set -e

SRC_ICON="${1:-/storage/emulated/0/Download/download (6).jpeg}"
SRC_LOGO="${2:-/storage/emulated/0/Download/download (5).jpeg}"

# repo root = parent directory of this script
RES="$(cd "$(dirname "$0")/.." && pwd)/app/src/main/res"

command -v magick >/dev/null 2>&1 || pkg install -y imagemagick

for f in "$SRC_ICON" "$SRC_LOGO"; do
  [ -f "$f" ] || { echo "Missing image: $f"; exit 1; }
done

echo "Icon : $SRC_ICON"
echo "Logo : $SRC_LOGO"
echo "Out  : $RES"
echo ""

# 1) Launcher icons.  density:legacy_px:adaptive_foreground_px
DENSITIES="mdpi:48:108 hdpi:72:162 xhdpi:96:216 xxhdpi:144:324 xxxhdpi:192:432"

for d in $DENSITIES; do
  density="${d%%:*}"
  rest="${d#*:}"
  s="${rest%%:*}"
  f="${rest##*:}"
  out="$RES/mipmap-$density"
  mkdir -p "$out"

  # square legacy icon (center-crop)
  magick "$SRC_ICON" -resize "${s}x${s}^" -gravity center -extent "${s}x${s}" \
    "$out/ic_launcher.png"

  # round icon (circle-cropped copy)
  mask="$(mktemp "${TMPDIR:-/tmp}/ikoko_mask_XXXXXX.png")"
  magick -size "${s}x${s}" xc:black -fill white \
    -draw "circle $((s/2-1)),$((s/2-1)) $((s/2-1)),0" "$mask"
  magick "$out/ic_launcher.png" "$mask" -alpha off -compose CopyOpacity -composite \
    "$out/ic_launcher_round.png"
  rm -f "$mask"

  # adaptive icon foreground: artwork at ~62% centered on a transparent canvas
  a=$((f * 62 / 100))
  magick -size "${f}x${f}" xc:none \
    \( "$SRC_ICON" -resize "${a}x${a}^" -gravity center -extent "${a}x${a}" \) \
    -gravity center -composite "$out/ic_launcher_foreground.png"

  echo "  mipmap-$density/  ic_launcher + round + foreground (${s}px)"
done

# 2) Top banner logo - same resource name as the placeholder vector, so the
#    app code never changes. drawable-nodpi = Android must never rescale it.
mkdir -p "$RES/drawable-nodpi"
magick "$SRC_LOGO" -resize "1280x1280>" "$RES/drawable-nodpi/ikoko_logo.jpg"
rm -f "$RES/drawable/ikoko_logo.xml"
echo "  drawable-nodpi/ikoko_logo.jpg"

echo ""
echo "Branding resources ready."
