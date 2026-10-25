#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - generate branding resources from your images (Termux).
#
# Source images are found in this order:
#   1. arguments:      bash scripts/setup_images.sh <icon> <logo>
#   2. committed:      branding/icon.jpeg + branding/logo.jpeg in the repo
#                      (any of .jpeg/.jpg/.png, no resizing needed - upload as-is)
#   3. device storage: /storage/emulated/0/Download/download (6).jpeg / (5).jpeg
#
# Output:
#   mipmap-*/ic_launcher.png + ic_launcher_round.png + ic_launcher_foreground.png
#   drawable-nodpi/ikoko_logo.jpg
#
# scripts/termux_build.sh runs this automatically before every build.
#
set -e

REPO="$(cd "$(dirname "$0")/.." && pwd)"
RES="$REPO/app/src/main/res"

# print the first existing file among the arguments
find_first() {
  for f in "$@"; do
    [ -f "$f" ] && { echo "$f"; return 0; }
  done
  return 1
}

if [ -n "$1" ]; then
  SRC_ICON="$1"
else
  SRC_ICON="$(find_first \
    "$REPO/branding/icon.jpeg" "$REPO/branding/icon.jpg" "$REPO/branding/icon.png" \
    "/storage/emulated/0/Download/download (6).jpeg" || true)"
fi

if [ -n "$2" ]; then
  SRC_LOGO="$2"
else
  SRC_LOGO="$(find_first \
    "$REPO/branding/logo.jpeg" "$REPO/branding/logo.jpg" "$REPO/branding/logo.png" \
    "/storage/emulated/0/Download/download (5).jpeg" || true)"
fi

[ -n "$SRC_ICON" ] || { echo "Icon image not found."; echo "Commit it as branding/icon.jpeg (or pass it as the 1st argument)."; exit 1; }
[ -n "$SRC_LOGO" ] || { echo "Logo image not found."; echo "Commit it as branding/logo.jpeg (or pass it as the 2nd argument)."; exit 1; }

command -v magick >/dev/null 2>&1 || pkg install -y imagemagick

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
