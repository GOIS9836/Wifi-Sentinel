#!/data/data/com.termux/files/usr/bin/env python3
"""
=============================================================================
 GLOBAL OPIFEX HUB-SIEM • WIFI SENTINEL APP VERSION CONTROL ENGINE
 Initiative : WiFi-Sentinel / Sentinel Command Center / Project-Lens
 Operator   : Liberty Tendai Kondo (GOIS9836-ARCHITECT / [POTRAZ-CDPA-OBSCURED])
 Target App : WiFi-Sentinel (Android 14 / API 34 // CUBOT KingKong 9)
 Governance : SemVer 2.0.0 • CISSP D8 (Software Dev Security) • Invariant: 0.00% Drift
=============================================================================
"""

import os
import sys
import re
import json
import argparse
import subprocess
from pathlib import Path

REPO_DIR = Path("/data/data/com.termux/files/home/Wifi-Sentinel")
GRADLE_FILE = REPO_DIR / "app" / "build.gradle.kts"
METADATA_FILE = REPO_DIR / "metadata.json"
PUSH_SCRIPT = REPO_DIR / "push_and_build.sh"

# ANSI Colors
C_RESET = "\033[0m"
C_BOLD = "\033[1m"
C_CYAN = "\033[96m"
C_GREEN = "\033[92m"
C_YELLOW = "\033[93m"
C_PURPLE = "\033[95m"
C_RED = "\033[91m"
C_GRAY = "\033[90m"

def get_current_version():
    if not GRADLE_FILE.exists():
        raise FileNotFoundError(f"build.gradle.kts not found at {GRADLE_FILE}")

    content = GRADLE_FILE.read_text(encoding="utf-8")
    
    code_match = re.search(r'versionCode\s*=\s*(\d+)', content)
    name_match = re.search(r'versionName\s*=\s*"([^"]+)"', content)

    if not code_match or not name_match:
        raise ValueError("Could not extract versionCode or versionName from build.gradle.kts")

    version_code = int(code_match.group(1))
    version_name = name_match.group(1)

    # Git status
    try:
        git_tag = subprocess.check_output(["git", "-C", str(REPO_DIR), "describe", "--tags", "--always"], text=True).strip()
        git_commit = subprocess.check_output(["git", "-C", str(REPO_DIR), "rev-parse", "--short", "HEAD"], text=True).strip()
        git_branch = subprocess.check_output(["git", "-C", str(REPO_DIR), "branch", "--show-current"], text=True).strip()
    except Exception:
        git_tag, git_commit, git_branch = "N/A", "N/A", "N/A"

    return {
        "version_code": version_code,
        "version_name": version_name,
        "git_tag": git_tag,
        "git_commit": git_commit,
        "git_branch": git_branch
    }

def calculate_next_version(current_name, current_code, bump_type):
    parts = current_name.split(".")
    while len(parts) < 3:
        parts.append("0")
    
    major, minor, patch = int(parts[0]), int(parts[1]), int(parts[2])

    if bump_type == "patch":
        patch += 1
    elif bump_type == "minor":
        minor += 1
        patch = 0
    elif bump_type == "major":
        major += 1
        minor = 0
        patch = 0
    else:
        raise ValueError(f"Invalid bump type: {bump_type}. Choose patch, minor, or major.")

    next_name = f"{major}.{minor}.{patch}"
    next_code = current_code + 1
    return next_name, next_code

def display_status():
    info = get_current_version()
    print("\n" + C_CYAN + C_BOLD + "╔═════════════════════════════════════════════════════════════════════════════╗" + C_RESET)
    print(C_CYAN + C_BOLD + "║          GLOBAL OPIFEX • WIFI SENTINEL APP VERSION CONTROL HUD              ║" + C_RESET)
    print(C_CYAN + C_BOLD + "╠═════════════════════════════════════════════════════════════════════════════╣" + C_RESET)
    print(f"{C_CYAN}║{C_RESET} {C_BOLD}Application    :{C_RESET} WiFi Sentinel (Android 14 / Target API 34)")
    print(f"{C_CYAN}║{C_RESET} {C_BOLD}Active Version :{C_RESET} {C_GREEN}{C_BOLD}v{info['version_name']}{C_RESET} (versionCode: {C_YELLOW}{info['version_code']}{C_RESET})")
    print(f"{C_CYAN}║{C_RESET} {C_BOLD}Git Tag / Head :{C_RESET} {C_PURPLE}{info['git_tag']}{C_RESET} on branch {C_CYAN}{info['git_branch']}{C_RESET} ({info['git_commit']})")
    print(f"{C_CYAN}║{C_RESET} {C_BOLD}Target Device  :{C_RESET} CUBOT KINGKONG 9 (MediaTek Helio G99 / Linux aarch64)")
    print(f"{C_CYAN}║{C_RESET} {C_BOLD}Governance     :{C_RESET} SemVer 2.0.0 • CISSP D8 • POTRAZ CDPA [Chapter 12:07]")
    print(C_CYAN + C_BOLD + "╠═════════════════════════════════════════════════════════════════════════════╣" + C_RESET)
    print(C_CYAN + C_BOLD + "║ RELEASE ARTIFACT INVENTORY:                                                 ║" + C_RESET)

    apk_files = sorted(list(REPO_DIR.glob("*.apk")))
    if apk_files:
        for apk in apk_files:
            size_mb = apk.stat().st_size / (1024 * 1024)
            print(f"  • {C_BOLD}{apk.name}{C_RESET} ({size_mb:.2f} MiB)")
    else:
        print("  • No local APK packages detected.")

    print(C_CYAN + C_BOLD + "╚═════════════════════════════════════════════════════════════════════════════╝" + C_RESET + "\n")

def perform_bump(bump_type, message=None):
    info = get_current_version()
    current_name = info["version_name"]
    current_code = info["version_code"]

    next_name, next_code = calculate_next_version(current_name, current_code, bump_type)

    print(f"\n[*] Bumping version: v{current_name} (code {current_code}) -> v{next_name} (code {next_code}) [{bump_type.upper()}]")

    # 1. Update app/build.gradle.kts
    gradle_content = GRADLE_FILE.read_text(encoding="utf-8")
    gradle_content = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {next_code}', gradle_content)
    gradle_content = re.sub(r'versionName\s*=\s*"[^"]+"', f'versionName = "{next_name}"', gradle_content)
    GRADLE_FILE.write_text(gradle_content, encoding="utf-8")
    print(f"  [✓] Updated {GRADLE_FILE.name}")

    # 2. Update push_and_build.sh
    if PUSH_SCRIPT.exists():
        push_content = PUSH_SCRIPT.read_text(encoding="utf-8")
        push_content = re.sub(r'WiFi-Sentinel-v[\d\.]+-API34', f'WiFi-Sentinel-v{next_name}-API34', push_content)
        PUSH_SCRIPT.write_text(push_content, encoding="utf-8")
        print(f"  [✓] Updated {PUSH_SCRIPT.name}")

    # 3. Commit and Tag via Git
    commit_msg = message or f"feat(release): bump version to v{next_name} (code {next_code})"
    tag_name = f"v{next_name}"

    try:
        subprocess.check_call(["git", "-C", str(REPO_DIR), "add", "app/build.gradle.kts", "push_and_build.sh"])
        subprocess.check_call(["git", "-C", str(REPO_DIR), "commit", "-m", commit_msg])
        subprocess.check_call(["git", "-C", str(REPO_DIR), "tag", "-a", tag_name, "-m", f"WiFi Sentinel Release {tag_name}"])
        print(f"  [✓] Committed changes: '{commit_msg}'")
        print(f"  [✓] Created signed git tag: {tag_name}")
    except subprocess.CalledProcessError as e:
        print(f"{C_RED}[!] Git commit/tag error: {e}{C_RESET}")
        return False

    print("\n" + C_GREEN + C_BOLD + f"🟢 [PASS // BENEDICTUS] Successfully bumped to v{next_name} (code {next_code})!" + C_RESET)
    print(f"To synchronize and trigger the automated GitHub Actions APK build pipeline, execute:")
    print(f"  {C_CYAN}cd ~/Wifi-Sentinel && git push origin main --tags{C_RESET}")
    print(f"Or simply run:")
    print(f"  {C_CYAN}cd ~/Wifi-Sentinel && bash push_and_build.sh{C_RESET}\n")
    return True

def main():
    parser = argparse.ArgumentParser(description="WiFi Sentinel App Version Control Engine")
    parser.add_argument("action", nargs="?", default="status", choices=["status", "bump", "current"])
    parser.add_argument("--type", choices=["patch", "minor", "major"], default="patch", help="Version bump increment type")
    parser.add_argument("-m", "--message", help="Custom commit release message")

    args = parser.parse_args()

    if args.action == "status":
        display_status()
    elif args.action == "current":
        info = get_current_version()
        print(f"v{info['version_name']} (code {info['version_code']})")
    elif args.action == "bump":
        perform_bump(args.type, args.message)

if __name__ == "__main__":
    main()
