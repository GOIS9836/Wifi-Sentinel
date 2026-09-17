#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# GLOBAL OPIFEX HUB-SIEM • PUSH & TRIGGER APK BUILD PIPELINE (NODES-PERGAMUS)
# ==============================================================================
set -euo pipefail

REPO_DIR="/data/data/com.termux/files/home/Wifi-Sentinel"
cd "$REPO_DIR"

echo "╔═════════════════════════════════════════════════════════════════════════════╗"
echo "║      GLOBAL OPIFEX HUB-SIEM • GITHUB CI DEPLOYMENT PIPELINE TRIGGER         ║"
echo "╠═════════════════════════════════════════════════════════════════════════════╣"
echo "║ Target Repository : https://github.com/GOIS9836/Wifi-Sentinel.git          ║"
echo "║ Active Branch     : main                                                    ║"
echo "║ Target Device     : CUBOT KINGKONG 9 (Android 14 / API 34)                  ║"
echo "║ CI Workflow       : .github/workflows/build-apk.yml                         ║"
echo "╚═════════════════════════════════════════════════════════════════════════════╝"

echo ""
echo "[*] Pending commits ready for upstream synchronization:"
git log origin/main..main --oneline

echo ""
echo "[*] Initiating git push to origin main..."
echo "[!] Note: If prompted, enter your GitHub Username (GOIS9836) and PAT (token)."
echo "[!] Git Credential Helper is configured to save it permanently."
echo ""

git push origin main

echo ""
echo "============================================================================="
echo "🟢 [PASS // BENEDICTUS] Commits successfully pushed to GitHub!"
echo "🚀 GitHub Actions workflow '.github/workflows/build-apk.yml' is now running."
echo "📦 Once complete, download the artifact 'WiFi-Sentinel-v1.0.1-API34' APK."
echo "============================================================================="
