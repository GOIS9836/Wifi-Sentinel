#!/usr/bin/env python3
"""
=============================================================================
 GLOBAL OPIFEX HUB-SIEM • WIFI SENTINEL LINUX DESKTOP & ENDPOINT DEFENDER
 Global Opifex Innovative Solutions (GOIS)
 "Hapana chinoramba — Simply the Digital Era — The Lens of Security Architecture"
 Architecture: Linux Workstation / Server / Edge Node Sentry
 Governance: POTRAZ CDPA [Chapter 12:07] • CISSP D5/D8 • SSCP D1
 Invariant: Mathematical Idempotency f(f(x)) = f(x) | 0.00% Drift
 Operator: Liberty Tendai Kondo (GOIS9836)
=============================================================================
"""

import sys
import os
import re
import json
import time
import socket
import platform
import argparse
import subprocess
import hashlib
import urllib.request
import urllib.error
from typing import Dict, List, Set, Any, Tuple, Optional

# ANSI Tactical Palette
C_RESET = "\033[0m"
C_BOLD = "\033[1m"
C_DIM = "\033[2m"
C_CYAN = "\033[1;36m"
C_GREEN = "\033[1;32m"
C_YELLOW = "\033[1;33m"
C_RED = "\033[1;31m"
C_MAGENTA = "\033[1;35m"
C_BLUE = "\033[1;34m"
C_WHITE = "\033[1;37m"

DEFAULT_HUB_HOST = "127.0.0.1"
DEFAULT_HUB_PORT = 8721
VERSION = "3.2.0-STABLE"

# Known Rogue / Backdoor Ports
ROGUE_WATCHED_PORTS = {
    5555: "Wireless ADB Backdoor (TCP 5555)",
    4444: "Metasploit / Meterpreter Reverse Shell (TCP 4444)",
    1337: "Elite Backdoor / C2 Beacon (TCP 1337)",
    31337: "Back Orifice / Trojan Socket (TCP 31337)",
    8080: "HTTP Alt Proxy (Check Authorization)",
    9999: "Urgent C2 Listener / Reverse Shell"
}

# =============================================================================
# 1. POTRAZ CHAPTER 12:07 CRYPTOGRAPHIC ANONYMIZATION
# =============================================================================

def hash_identifier(identifier: str, salt: str = "WiFiSentinel-Salt-V1") -> str:
    """
    POTRAZ Ch. 12:07 Privacy Invariant:
    Deterministic SHA-256 pseudonymization of MAC addresses and IPs before logging.
    """
    digest = hashlib.sha256(f"{identifier}:{salt}".encode("utf-8"))
    return digest.hexdigest()

def mask_mac(mac: str) -> str:
    parts = mac.strip().split(":")
    if len(parts) == 6:
        return f"{parts[0]}:{parts[1]}:**:**:**:{parts[5]}"
    return mac

def is_randomized_mac(mac: str) -> bool:
    clean = mac.replace(":", "").replace("-", "").strip()
    if len(clean) < 2:
        return False
    try:
        first_byte = int(clean[:2], 16)
        return (first_byte & 0x02) != 0
    except Exception:
        return False

# =============================================================================
# 2. LINUX HARDWARE & NETWORK TELEMETRY PROFILER
# =============================================================================

class LinuxTelemetryProfiler:

    @staticmethod
    def get_hostname() -> str:
        return socket.gethostname()

    @staticmethod
    def get_os_info() -> Dict[str, str]:
        distro = "Linux Generic"
        if os.path.exists("/etc/os-release"):
            try:
                with open("/etc/os-release", "r", encoding="utf-8") as f:
                    for line in f:
                        if line.startswith("PRETTY_NAME="):
                            distro = line.split("=", 1)[1].strip().strip('"')
                            break
            except Exception:
                pass
        return {
            "distro": distro,
            "kernel": platform.release(),
            "arch": platform.machine()
        }

    @staticmethod
    def get_default_interface() -> Tuple[str, str, str]:
        """
        Returns (interface_name, local_ip, gateway_ip)
        """
        iface = "eth0"
        gw_ip = "192.168.1.1"
        local_ip = "127.0.0.1"

        # Check default route
        try:
            out = subprocess.check_output(["ip", "route", "show", "default"], text=True, stderr=subprocess.DEVNULL)
            parts = out.strip().split()
            if "default" in parts:
                idx_via = parts.index("via") if "via" in parts else -1
                idx_dev = parts.index("dev") if "dev" in parts else -1
                if idx_via != -1 and idx_via + 1 < len(parts):
                    gw_ip = parts[idx_via + 1]
                if idx_dev != -1 and idx_dev + 1 < len(parts):
                    iface = parts[idx_dev + 1]
        except Exception:
            pass

        # Get local IP on that interface
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
            s.close()
        except Exception:
            try:
                local_ip = socket.gethostbyname(socket.gethostname())
            except Exception:
                local_ip = "127.0.0.1"

        return iface, local_ip, gw_ip

    @staticmethod
    def get_mac_address(iface: str) -> str:
        path = f"/sys/class/net/{iface}/address"
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    return f.read().strip().upper()
            except Exception:
                pass
        return "00:00:00:00:00:00"

    @staticmethod
    def get_wifi_status(iface: str) -> Dict[str, Any]:
        """
        Parses iwconfig or nmcli to get Wi-Fi parameters.
        """
        status = {
            "is_wifi": False,
            "ssid": "Wired / Ethernet",
            "bssid": "00:00:00:00:00:00",
            "rssi_dbm": -50,
            "frequency_ghz": 5.0,
            "channel": 36,
            "bitrate": "1000 Mbps"
        }

        # Try nmcli first
        try:
            out = subprocess.check_output(["nmcli", "-t", "-f", "ACTIVE,SSID,BSSID,CHAN,FREQ,SIGNAL,RATE", "dev", "wifi"], text=True, stderr=subprocess.DEVNULL)
            for line in out.strip().splitlines():
                parts = line.split(":")
                if parts and parts[0] == "yes":
                    status["is_wifi"] = True
                    status["ssid"] = parts[1] if len(parts) > 1 else "Unknown"
                    # BSSID might have colons escaped or merged
                    status["bssid"] = ":".join(parts[2:8]) if len(parts) >= 8 else "00:00:00:00:00:00"
                    status["channel"] = int(parts[8]) if len(parts) > 8 and parts[8].isdigit() else 36
                    status["rssi_dbm"] = int(parts[10]) - 100 if len(parts) > 10 and parts[10].isdigit() else -55
                    return status
        except Exception:
            pass

        # Fallback to iwconfig
        try:
            out = subprocess.check_output(["iwconfig", iface], text=True, stderr=subprocess.DEVNULL)
            status["is_wifi"] = True
            m_ssid = re.search(r'ESSID:"([^"]+)"', out)
            if m_ssid:
                status["ssid"] = m_ssid.group(1)
            m_ap = re.search(r'Access Point:\s*([0-9A-Fa-f:]{17})', out)
            if m_ap:
                status["bssid"] = m_ap.group(1).upper()
            m_signal = re.search(r'Signal level=(-\d+)\s*dBm', out)
            if m_signal:
                status["rssi_dbm"] = int(m_signal.group(1))
            m_freq = re.search(r'Frequency:([\d.]+)\s*GHz', out)
            if m_freq:
                status["frequency_ghz"] = float(m_freq.group(1))
                status["channel"] = 36 if status["frequency_ghz"] >= 5.0 else 6
        except Exception:
            pass

        return status

# =============================================================================
# 3. LINUX HOST SECURITY & ROGUE SOCKET SENTRY
# =============================================================================

class LinuxHostAuditor:

    @staticmethod
    def audit_listening_ports() -> List[Dict[str, Any]]:
        """
        Inspects /proc/net/tcp and /proc/net/tcp6 for rogue listeners (state 0A = LISTEN).
        """
        listeners = []
        for path in ("/proc/net/tcp", "/proc/net/tcp6"):
            if not os.path.exists(path):
                continue
            try:
                with open(path, "r", encoding="utf-8") as f:
                    for line in f:
                        parts = line.strip().split()
                        if len(parts) >= 4 and parts[3] == "0A":
                            local_addr = parts[1]
                            if ":" in local_addr:
                                port_hex = local_addr.split(":")[1]
                                port_dec = int(port_hex, 16)
                                is_rogue = port_dec in ROGUE_WATCHED_PORTS
                                listeners.append({
                                    "port": port_dec,
                                    "protocol": "TCP",
                                    "is_rogue": is_rogue,
                                    "desc": ROGUE_WATCHED_PORTS.get(port_dec, "Standard Listener")
                                })
            except Exception:
                pass
        return listeners

    @staticmethod
    def check_promiscuous_interfaces() -> List[str]:
        """
        Detects if any interface is in promiscuous packet sniffing mode.
        """
        promisc = []
        try:
            out = subprocess.check_output(["ip", "-d", "link", "show"], text=True, stderr=subprocess.DEVNULL)
            for block in out.split("\n\n"):
                if "PROMISC" in block:
                    m = re.search(r'^\d+:\s*([^:@]+)', block)
                    if m:
                        promisc.append(m.group(1).strip())
        except Exception:
            pass
        return promisc

    @staticmethod
    def check_ip_forwarding() -> bool:
        path = "/proc/sys/net/ipv4/ip_forward"
        if os.path.exists(path):
            try:
                with open(path, "r", encoding="utf-8") as f:
                    return f.read().strip() == "1"
            except Exception:
                pass
        return False

# =============================================================================
# 4. SUBNET & ARP SENTRY (Zero-Tolerance to Rogue Peering)
# =============================================================================

class LinuxSubnetSentry:

    @staticmethod
    def scan_arp_table() -> List[Dict[str, Any]]:
        devices = []
        # Parse /proc/net/arp
        if os.path.exists("/proc/net/arp"):
            try:
                with open("/proc/net/arp", "r", encoding="utf-8") as f:
                    lines = f.readlines()[1:]  # skip header
                    for line in lines:
                        parts = line.strip().split()
                        if len(parts) >= 6:
                            ip = parts[0]
                            flags = parts[2]
                            mac = parts[3].upper()
                            dev = parts[5]
                            if mac != "00:00:00:00:00:00" and flags != "0x0":
                                is_rand = is_randomized_mac(mac)
                                devices.append({
                                    "ip": ip,
                                    "mac": mac,
                                    "iface": dev,
                                    "is_randomized_mac": is_rand,
                                    "pseudonym_hash": hash_identifier(mac)[:16],
                                    "is_authorized": False,
                                    "is_blocked": False
                                })
            except Exception:
                pass
        return devices

# =============================================================================
# 5. LINUX IPTABLES / NFTABLES FIREWALL ENFORCER
# =============================================================================

class LinuxFirewallEnforcer:

    @staticmethod
    def get_active_blocked_macs() -> Set[str]:
        blocked = set()
        try:
            out = subprocess.check_output(["iptables", "-S", "INPUT"], text=True, stderr=subprocess.DEVNULL)
            for line in out.splitlines():
                if "--mac-source" in line and "-j DROP" in line:
                    m = re.search(r'--mac-source\s+([0-9A-Fa-f:]{17})', line)
                    if m:
                        blocked.add(m.group(1).upper())
        except Exception:
            pass
        return blocked

    @staticmethod
    def block_mac(mac: str) -> Tuple[bool, str]:
        norm = mac.strip().upper()
        cmd = ["iptables", "-I", "INPUT", "-m", "mac", "--mac-source", norm, "-j", "DROP"]
        if os.geteuid() != 0:
            cmd = ["sudo"] + cmd
        try:
            subprocess.check_call(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return True, f"Blocked MAC {norm} via iptables"
        except Exception as e:
            return False, f"Failed: {e} (Need root/sudo privileges)"

    @staticmethod
    def unblock_mac(mac: str) -> Tuple[bool, str]:
        norm = mac.strip().upper()
        cmd = ["iptables", "-D", "INPUT", "-m", "mac", "--mac-source", norm, "-j", "DROP"]
        if os.geteuid() != 0:
            cmd = ["sudo"] + cmd
        try:
            subprocess.check_call(cmd, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return True, f"Unblocked MAC {norm}"
        except Exception as e:
            return False, f"Failed: {e}"

    @staticmethod
    def flush_sentinel_rules() -> Tuple[bool, str]:
        blocked = LinuxFirewallEnforcer.get_active_blocked_macs()
        count = 0
        for mac in blocked:
            LinuxFirewallEnforcer.unblock_mac(mac)
            count += 1
        return True, f"Flushed {count} active rule(s)"

# =============================================================================
# 6. TELEMETRY SYNC TO ANDROID SENTINEL HUB (:8721)
# =============================================================================

def sync_telemetry_to_hub(hub_host: str, hub_port: int, report: Dict[str, Any]) -> Tuple[bool, str]:
    endpoint = f"http://{hub_host}:{hub_port}/api/v1/telemetry/ingest"
    payload = {
        "node_id": report["hostname"],
        "node_type": "LINUX_DESKTOP",
        "timestamp": int(time.time() * 1000),
        "source_subsystem": "WIFI_SENTINEL_LINUX",
        "category": "ENDPOINT_TELEMETRY",
        "severity": report["security_posture"],
        "summary": f"Linux Node {report['hostname']} ({report['ip']}) Telemetry: {report['active_threats_count']} threat(s), {report['subnet_device_count']} LAN peer(s)",
        "payload": report
    }
    try:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            endpoint,
            data=data,
            headers={"Content-Type": "application/json; charset=utf-8"},
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=1.8) as resp:
            return True, f"HTTP {resp.status}"
    except Exception as e:
        return False, str(e)

# =============================================================================
# 7. TACTICAL CONSOLE HUD (TERMINAL / SSH / SERVER MODE)
# =============================================================================

def render_terminal_hud(report: Dict[str, Any], hub_status: Tuple[bool, str]):
    os.system("clear" if os.name == "posix" else "cls")
    print(f"{C_MAGENTA}[GOIS-SIEM // NODES-PERGAMUS // LINUX ENDPOINT SENTRY v{VERSION}]{C_RESET}")
    print(f"{C_CYAN}╔═════════════════════════════════════════════════════════════════════════════════╗{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}      {C_WHITE}GLOBAL OPIFEX HUB-SIEM • WIFI SENTINEL DESKTOP LINUX DEFENDER{C_RESET}              {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}      {C_DIM}\"Hapana chinoramba — Simply the Digital Era — The Lens of Security\"{C_RESET}        {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}╠═════════════════════════════════════════════════════════════════════════════════╣{C_RESET}")
    
    node_str = f"{report['hostname']} ({report['os']['distro']})"
    ip_str = f"{report['ip']} [{report['iface']}]"
    print(f"{C_CYAN}║{C_RESET}  Host Node       : {C_GREEN}{node_str:<58}{C_RESET} {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}  Network Stack   : {C_YELLOW}{ip_str:<58}{C_RESET} {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}  Hardware MAC    : {C_WHITE}{report['mac']:<58}{C_RESET} {C_CYAN}║{C_RESET}")
    
    wifi = report["wifi"]
    if wifi["is_wifi"]:
        wifi_str = f"SSID: '{wifi['ssid']}' | BSSID: {wifi['bssid']} | RSSI: {wifi['rssi_dbm']} dBm | Ch {wifi['channel']}"
    else:
        wifi_str = f"Ethernet / Wired Interface [{report['iface']}]"
    print(f"{C_CYAN}║{C_RESET}  RF Connection   : {C_CYAN}{wifi_str:<58}{C_RESET} {C_CYAN}║{C_RESET}")
    
    posture_badge = f"{C_GREEN}🟢 [PASS // BENEDICTUS]{C_RESET}" if report["active_threats_count"] == 0 else f"{C_RED}🔴 [ELEVATED THREAT // {report['active_threats_count']} RISKS]{C_RESET}"
    print(f"{C_CYAN}║{C_RESET}  Security Posture: {posture_badge:<58} {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}  POTRAZ Ch. 12:07: {C_MAGENTA}Pseudonymized SHA-256 Hashes [STRICT ENFORCEMENT]{C_RESET}              {C_CYAN}║{C_RESET}")
    
    hub_badge = f"{C_GREEN}🟢 CONNECTED (:8721){C_RESET}" if hub_status[0] else f"{C_YELLOW}🟡 STANDBY ({hub_status[1][:32]}){C_RESET}"
    print(f"{C_CYAN}║{C_RESET}  Android Hub Sync: {hub_badge:<58} {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}╠═════════════════════════════════════════════════════════════════════════════════╣{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}  {C_WHITE}SUBNET HOST INVENTORY & ARP CACHE ({len(report['subnet_devices'])} PEERS DETECTED):{C_RESET}{' ' * 24}{C_CYAN}║{C_RESET}")
    
    if not report['subnet_devices']:
        print(f"{C_CYAN}║{C_RESET}    {C_DIM}No ARP cache entries currently active on {report['iface']}{C_RESET}{' ' * 36}{C_CYAN}║{C_RESET}")
    else:
        for dev in report['subnet_devices'][:6]:
            rand_tag = f"{C_CYAN}[LAA-RAND]{C_RESET}" if dev['is_randomized_mac'] else f"{C_DIM}[HW-OUI]{C_RESET}"
            block_tag = f"{C_RED}[BLOCKED]{C_RESET}" if dev['is_blocked'] else f"{C_GREEN}[PERMITTED]{C_RESET}"
            dev_line = f"• {dev['ip']:<15} {mask_mac(dev['mac']):<17} {rand_tag} {block_tag} Hash:{dev['pseudonym_hash']}"
            print(f"{C_CYAN}║{C_RESET}    {dev_line:<66} {C_CYAN}║{C_RESET}")
            
    print(f"{C_CYAN}╠═════════════════════════════════════════════════════════════════════════════════╣{C_RESET}")
    print(f"{C_CYAN}║{C_RESET}  {C_WHITE}HOST HARDENING & ROGUE LISTENER AUDIT:{C_RESET}{' ' * 42}{C_CYAN}║{C_RESET}")
    
    rogue_listeners = [l for l in report['listeners'] if l['is_rogue']]
    if not rogue_listeners:
        print(f"{C_CYAN}║{C_RESET}    {C_GREEN}✓ Clean Local Socket State. Zero rogue listener ports active.{C_RESET}{' ' * 19}{C_CYAN}║{C_RESET}")
    else:
        for r in rogue_listeners:
            alert_line = f"⚠️ ROGUE PORT DETECTED: Port {r['port']} - {r['desc']}"
            print(f"{C_CYAN}║{C_RESET}    {C_RED}{alert_line:<66}{C_RESET} {C_CYAN}║{C_RESET}")
            
    if report['ip_forwarding']:
        print(f"{C_CYAN}║{C_RESET}    {C_YELLOW}⚠️ IPv4 Forwarding is Enabled (/proc/sys/net/ipv4/ip_forward=1){C_RESET}{' ' * 12}{C_CYAN}║{C_RESET}")
        
    if report['promiscuous_interfaces']:
        print(f"{C_CYAN}║{C_RESET}    {C_RED}🚨 PROMISCUOUS MODE DETECTED ON: {','.join(report['promiscuous_interfaces'])}{C_RESET}{' ' * 16}{C_CYAN}║{C_RESET}")
        
    print(f"{C_CYAN}╠═════════════════════════════════════════════════════════════════════════════════╣{C_RESET}")
    blocked_count = len(report['firewall_blocked_macs'])
    fw_str = f"Active iptables MAC Drop Rules: {blocked_count} host(s) quarantined"
    print(f"{C_CYAN}║{C_RESET}  Firewall Posture: {C_WHITE}{fw_str:<58}{C_RESET} {C_CYAN}║{C_RESET}")
    print(f"{C_CYAN}╚═════════════════════════════════════════════════════════════════════════════════╝{C_RESET}")
    print(f"\n{C_DIM}Interactive Controls:{C_RESET} [L] Lockdown Unauthorized Hosts • [F] Flush Firewall • [S] Immediate Scan • [Q] Exit")

# =============================================================================
# 8. TKINTER DESKTOP GUI (FALLBACK IF DISPLAY IS PRESENT)
# =============================================================================

def try_launch_desktop_gui(hub_host: str, hub_port: int):
    try:
        import tkinter as tk
        from tkinter import ttk, messagebox
    except ImportError:
        print(f"{C_YELLOW}[!] Tkinter is not installed on this Linux desktop. Falling back to Tactical Terminal HUD.{C_RESET}")
        return False

    if not os.environ.get("DISPLAY") and not os.environ.get("WAYLAND_DISPLAY"):
        print(f"{C_YELLOW}[!] No X11 or Wayland DISPLAY found. Falling back to Tactical Terminal HUD.{C_RESET}")
        return False

    root = tk.Tk()
    root.title(f"WiFi Sentinel Desktop • Linux Endpoint Defender v{VERSION}")
    root.geometry("880x640")
    root.configure(bg="#0B0F19")

    # Dark Theme Styling
    style = ttk.Style()
    style.theme_use("clam")
    style.configure(".", background="#0B0F19", foreground="#E2E8F0")
    style.configure("TLabel", background="#0B0F19", foreground="#E2E8F0", font=("Helvetica", 10))
    style.configure("Header.TLabel", font=("Helvetica", 13, "bold"), foreground="#00E5FF")
    style.configure("TButton", background="#1E293B", foreground="#00E5FF", borderwidth=1)

    header_frame = tk.Frame(root, bg="#131B2E", padx=16, pady=12)
    header_frame.pack(fill=tk.X)

    title_lbl = tk.Label(header_frame, text="🛡️ GLOBAL OPIFEX • WIFI SENTINEL LINUX DEFENDER", bg="#131B2E", fg="#00E5FF", font=("Helvetica", 14, "bold"))
    title_lbl.pack(anchor=tk.W)

    status_var = tk.StringVar(value="Status: Initializing scan...")
    status_lbl = tk.Label(header_frame, textvariable=status_var, bg="#131B2E", fg="#10B981", font=("Helvetica", 10))
    status_lbl.pack(anchor=tk.W)

    # Main split
    content = tk.Frame(root, bg="#0B0F19", padx=16, pady=12)
    content.pack(fill=tk.BOTH, expand=True)

    # Treeview for hosts
    hosts_frame = tk.LabelFrame(content, text=" Active Subnet Hosts (ARP Inspection) ", bg="#0B0F19", fg="#00E5FF", padx=8, pady=8)
    hosts_frame.pack(fill=tk.BOTH, expand=True)

    cols = ("IP", "MAC", "LAA", "Firewall", "Hash")
    tree = ttk.Treeview(hosts_frame, columns=cols, show="headings", height=8)
    for col in cols:
        tree.heading(col, text=col)
        tree.column(col, width=140)
    tree.pack(fill=tk.BOTH, expand=True)

    # Action buttons
    btn_frame = tk.Frame(root, bg="#0B0F19", padx=16, pady=10)
    btn_frame.pack(fill=tk.X)

    def refresh_data():
        iface, local_ip, gw_ip = LinuxTelemetryProfiler.get_default_interface()
        mac = LinuxTelemetryProfiler.get_mac_address(iface)
        wifi = LinuxTelemetryProfiler.get_wifi_status(iface)
        devices = LinuxSubnetSentry.scan_arp_table()
        blocked = LinuxFirewallEnforcer.get_active_blocked_macs()

        status_var.set(f"Host: {socket.gethostname()} | IP: {local_ip} | Wi-Fi: {wifi['ssid']} ({wifi['rssi_dbm']} dBm) | Active Rules: {len(blocked)}")

        for item in tree.get_children():
            tree.delete(item)

        for dev in devices:
            is_bl = dev['mac'] in blocked
            tree.insert("", tk.END, values=(
                dev["ip"],
                mask_mac(dev["mac"]),
                "Randomized" if dev["is_randomized_mac"] else "Hardware",
                "DROP / Blocked" if is_bl else "Allowed",
                dev["pseudonym_hash"]
            ))

    def on_block_selected():
        sel = tree.selection()
        if not sel:
            messagebox.showinfo("Select Host", "Please select a host in the table to isolate.")
            return
        vals = tree.item(sel[0])["values"]
        mac_to_block = vals[1]
        ok, msg = LinuxFirewallEnforcer.block_mac(mac_to_block)
        messagebox.showinfo("Firewall Action", msg)
        refresh_data()

    def on_flush_rules():
        ok, msg = LinuxFirewallEnforcer.flush_sentinel_rules()
        messagebox.showinfo("Firewall Flush", msg)
        refresh_data()

    refresh_btn = tk.Button(btn_frame, text="🔄 Refresh Scan", bg="#1E293B", fg="#00E5FF", command=refresh_data, padx=10, pady=5)
    refresh_btn.pack(side=tk.LEFT, padx=5)

    block_btn = tk.Button(btn_frame, text="⛔ Block Selected Host", bg="#7F1D1D", fg="#FCA5A5", command=on_block_selected, padx=10, pady=5)
    block_btn.pack(side=tk.LEFT, padx=5)

    flush_btn = tk.Button(btn_frame, text="🛡️ Flush All Blocks", bg="#1E293B", fg="#10B981", command=on_flush_rules, padx=10, pady=5)
    flush_btn.pack(side=tk.LEFT, padx=5)

    refresh_data()
    root.mainloop()
    return True

# =============================================================================
# 9. MAIN RUNTIME LOOP
# =============================================================================

def collect_full_report() -> Dict[str, Any]:
    iface, local_ip, gw_ip = LinuxTelemetryProfiler.get_default_interface()
    mac = LinuxTelemetryProfiler.get_mac_address(iface)
    os_info = LinuxTelemetryProfiler.get_os_info()
    wifi_status = LinuxTelemetryProfiler.get_wifi_status(iface)
    listeners = LinuxHostAuditor.audit_listening_ports()
    promisc = LinuxHostAuditor.check_promiscuous_interfaces()
    ip_fwd = LinuxHostAuditor.check_ip_forwarding()
    devices = LinuxSubnetSentry.scan_arp_table()
    blocked_macs = LinuxFirewallEnforcer.get_active_blocked_macs()

    for d in devices:
        d["is_blocked"] = d["mac"] in blocked_macs

    rogue_count = len([l for l in listeners if l["is_rogue"]]) + (1 if promisc else 0) + (1 if ip_fwd else 0)
    posture = "BENEDICTUS" if rogue_count == 0 else "WARNING"

    return {
        "hostname": socket.gethostname(),
        "ip": local_ip,
        "gateway_ip": gw_ip,
        "mac": mac,
        "iface": iface,
        "os": os_info,
        "wifi": wifi_status,
        "listeners": listeners,
        "promiscuous_interfaces": promisc,
        "ip_forwarding": ip_fwd,
        "subnet_devices": devices,
        "subnet_device_count": len(devices),
        "firewall_blocked_macs": list(blocked_macs),
        "active_threats_count": rogue_count,
        "security_posture": posture,
        "timestamp": int(time.time() * 1000)
    }

def main():
    parser = argparse.ArgumentParser(description="Global Opifex WiFi Sentinel: Linux Desktop & Endpoint Defender")
    parser.add_argument("--hub", default=DEFAULT_HUB_HOST, help="WiFi Sentinel Android Hub IP or Hostname (default: 127.0.0.1)")
    parser.add_argument("--port", type=int, default=DEFAULT_HUB_PORT, help="Hub port (default: 8721)")
    parser.add_argument("--gui", action="store_true", help="Force graphical desktop Tkinter UI window")
    parser.add_argument("--cli", action="store_true", help="Run interactive tactical terminal HUD")
    parser.add_argument("--daemon", action="store_true", help="Run headlessly as background systemd daemon service")
    parser.add_argument("--interval", type=int, default=15, help="Scan interval in seconds (default: 15s)")
    parser.add_argument("--lockdown", action="store_true", help="Immediately lockdown and drop all unwhitelisted LAN peers")
    parser.add_argument("--flush", action="store_true", help="Flush all WiFi Sentinel iptables drop rules and exit")
    parser.add_argument("--json", action="store_true", help="Print single JSON telemetry snapshot and exit")
    args = parser.parse_args()

    if args.flush:
        ok, msg = LinuxFirewallEnforcer.flush_sentinel_rules()
        print(f"[{'OK' if ok else 'ERR'}] {msg}")
        return 0

    if args.lockdown:
        devices = LinuxSubnetSentry.scan_arp_table()
        count = 0
        for dev in devices:
            if not dev["is_randomized_mac"]:
                ok, msg = LinuxFirewallEnforcer.block_mac(dev["mac"])
                if ok:
                    count += 1
        print(f"[LOCKDOWN] Enforced iptables drops against {count} unauthorized host(s).")
        return 0

    if args.json:
        rep = collect_full_report()
        print(json.dumps(rep, indent=2))
        return 0

    if args.gui:
        if try_launch_desktop_gui(args.hub, args.port):
            return 0

    # If running with GUI allowed and not explicit CLI/daemon, try GUI first
    if not args.cli and not args.daemon and (os.environ.get("DISPLAY") or os.environ.get("WAYLAND_DISPLAY")):
        if try_launch_desktop_gui(args.hub, args.port):
            return 0

    print(f"{C_CYAN}[i] Starting WiFi Sentinel Linux Sentry Loop (Interval: {args.interval}s, Hub: {args.hub}:{args.port}){C_RESET}")

    try:
        while True:
            report = collect_full_report()
            hub_status = sync_telemetry_to_hub(args.hub, args.port, report)

            if not args.daemon:
                render_terminal_hud(report, hub_status)
            else:
                posture_tag = "OK" if report["active_threats_count"] == 0 else "WARN"
                print(f"[{time.strftime('%X')}] [SENTINEL-DAEMON] Host={report['hostname']} Posture={posture_tag} Peers={len(report['subnet_devices'])} HubSync={hub_status[0]}")

            time.sleep(args.interval)
    except KeyboardInterrupt:
        print(f"\n{C_YELLOW}[!] WiFi Sentinel Linux Sentry terminated by operator.{C_RESET}")
        return 0

if __name__ == "__main__":
    sys.exit(main())
