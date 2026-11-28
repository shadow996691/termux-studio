#!/data/data/com.termux/files/usr/bin/bash
set -e
REPO_URL="${1:-https://YOUR_GITHUB_USERNAME.github.io/termux-studio/termux-repo}"
echo "deb [trusted=yes] ${REPO_URL} stable main" > "$PREFIX/etc/apt/sources.list.d/termux-studio.list"
pkg update -y
pkg install -y termux-studio
termux-studio link-storage
termux-studio probe
