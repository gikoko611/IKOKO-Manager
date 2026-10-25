#!/data/data/com.termux/files/usr/bin/bash
#
# I KO KO Manager - upload your icon + logo images into the repo (Termux).
#
# Copies the two images from your Download folder into branding/ inside the
# repo and pushes the commit to GitHub. A one-time GitHub login (gh) is
# required; it shows a short code and opens the browser to authorize.
#
# Run:  bash scripts/upload_branding.sh
#
set -e

SRC_ICON="/storage/emulated/0/Download/download (6).jpeg"
SRC_LOGO="/storage/emulated/0/Download/download (5).jpeg"
DIR="$HOME/IKOKO-Manager"

# -- tools ---------------------------------------------------------------
command -v git >/dev/null 2>&1 || pkg install -y git
command -v gh  >/dev/null 2>&1 || pkg install -y gh

# -- shared storage (for reading the Download folder) ---------------------
[ -d "/storage/emulated/0/Download" ] || termux-setup-storage

# -- one-time GitHub login -------------------------------------------------
if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub login (one time only):"
  echo "  A short code will be shown - press Enter, then paste the code in the"
  echo "  browser page that opens and authorize. Do this as the gikoko611 account."
  gh auth login --hostname github.com --git-protocol https --web
fi
gh auth setup-git   # lets plain "git push" use the gh login

# -- git identity (needed for commit) ---------------------------------------
git config user.name  >/dev/null 2>&1 || git config --global user.name  "gikoko611"
git config user.email >/dev/null 2>&1 || git config --global user.email "gikoko611@users.noreply.github.com"

# -- repo --------------------------------------------------------------------
[ -d "$DIR" ] || git clone https://github.com/gikoko611/IKOKO-Manager.git "$DIR"
cd "$DIR"
git pull --rebase

for f in "$SRC_ICON" "$SRC_LOGO"; do
  [ -f "$f" ] || { echo "Missing image: $f"; exit 1; }
done

mkdir -p branding
cp "$SRC_ICON" branding/icon.jpeg
cp "$SRC_LOGO" branding/logo.jpeg

git add branding
git commit -m "branding: icon + logo source images" || echo "No new images to commit."
git push

echo ""
echo "Uploaded: branding/icon.jpeg + branding/logo.jpeg are now in the repo."
echo "Next:  bash scripts/termux_build.sh"
