#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# GLOBAL OPIFEX HUB-SIEM • WIFI SENTINEL VERSION CONTROL RUNNER
# ==============================================================================
set -euo pipefail

REAL_SCRIPT="$(readlink -f "${BASH_SOURCE[0]}")"
SCRIPT_DIR="$(cd "$(dirname "${REAL_SCRIPT}")" && pwd)"
python3 "${SCRIPT_DIR}/version_control.py" "$@"
