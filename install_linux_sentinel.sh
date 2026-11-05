#!/usr/bin/env bash
# =============================================================================
#  GLOBAL OPIFEX HUB-SIEM • WIFI SENTINEL LINUX INSTALLER & SYSTEMD DEPLOYER
#  Global Opifex Innovative Solutions (GOIS)
#  Lead: Liberty Tendai Kondo (GOIS9836)
# =============================================================================

set -e

C_CYAN='\033[1;36m'
C_GREEN='\033[1;32m'
C_YELLOW='\033[1;33m'
C_RED='\033[1;31m'
C_RESET='\033[0m'

echo -e "${C_CYAN}╔═══════════════════════════════════════════════════════════════╗${C_RESET}"
echo -e "${C_CYAN}║${C_RESET}   GLOBAL OPIFEX • WIFI SENTINEL LINUX ENDPOINT DEPLOYMENT     ${C_CYAN}║${C_RESET}"
echo -e "${C_CYAN}╚═══════════════════════════════════════════════════════════════╝${C_RESET}"

# Check Python 3
if ! command -v python3 &> /dev/null; then
    echo -e "${C_RED}[!] Python 3 is required. Please install python3 (e.g. sudo apt install python3).${C_RESET}"
    exit 1
fi

HUB_HOST="${1:-127.0.0.1}"
HUB_PORT="${2:-8721}"
INSTALL_DIR="/usr/local/bin"

if [ "$EUID" -ne 0 ]; then
    INSTALL_DIR="$HOME/.local/bin"
    mkdir -p "$INSTALL_DIR"
fi

SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/wifi_sentinel_desktop.py"

if [ ! -f "$SCRIPT_PATH" ]; then
    echo -e "${C_YELLOW}[*] Downloading wifi_sentinel_desktop.py from local hub http://${HUB_HOST}:${HUB_PORT}...${C_RESET}"
    curl -sSL "http://${HUB_HOST}:${HUB_PORT}/agent/wifi_sentinel_desktop.py" -o /tmp/wifi_sentinel_desktop.py
    SCRIPT_PATH="/tmp/wifi_sentinel_desktop.py"
fi

cp "$SCRIPT_PATH" "$INSTALL_DIR/wifi-sentinel"
chmod +x "$INSTALL_DIR/wifi-sentinel"

echo -e "${C_GREEN}[✓] WiFi Sentinel installed to $INSTALL_DIR/wifi-sentinel${C_RESET}"

# If root, offer systemd service
if [ "$EUID" -eq 0 ] && command -v systemctl &> /dev/null; then
    cat <<EOF > /etc/systemd/system/wifi-sentinel.service
[Unit]
Description=Global Opifex WiFi Sentinel Linux Endpoint Defender
After=network.target network-online.target

[Service]
Type=simple
User=root
ExecStart=$INSTALL_DIR/wifi-sentinel --daemon --hub $HUB_HOST --port $HUB_PORT
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

    systemctl daemon-reload
    systemctl enable wifi-sentinel.service
    systemctl restart wifi-sentinel.service
    echo -e "${C_GREEN}[✓] Systemd service enabled & started: wifi-sentinel.service${C_RESET}"
fi

echo -e "\n${C_GREEN}Deployment Complete!${C_RESET}"
echo -e "Run interactively:  ${C_CYAN}wifi-sentinel --cli${C_RESET}"
echo -e "Run desktop GUI:    ${C_CYAN}wifi-sentinel --gui${C_RESET}"
echo -e "Lockdown peers:     ${C_CYAN}sudo wifi-sentinel --lockdown${C_RESET}"
