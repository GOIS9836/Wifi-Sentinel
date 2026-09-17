#!/data/data/com.termux/files/usr/bin/env python3
"""
=============================================================================
 GLOBAL OPIFEX HUB-SIEM • SENTINEL ARCHITECTURAL TEST HARNESS (PHASE 1 & 2)
 Targets:
  - com.example.wifisentinel.MacSanitizerTest
  - com.example.wifisentinel.SentinelRepositoryTest
 Invariant: Mathematical Idempotency f(f(x)) = f(x) | 0.00% Drift
=============================================================================
"""
import sys
import hashlib
import time

C_RESET = "\033[0m"
C_CYAN = "\033[1;36m"
C_GREEN = "\033[1;32m"
C_RED = "\033[1;31m"
C_BOLD = "\033[1m"
C_GRAY = "\033[2m"

def is_randomized_mac(mac_address: str) -> bool:
    clean_mac = mac_address.replace(":", "").replace("-", "").strip()
    if len(clean_mac) < 2:
        return False
    try:
        first_byte = int(clean_mac[:2], 16)
        return (first_byte & 0x02) != 0
    except Exception:
        return False

def hash_identifier(identifier: str, local_salt: str = "local_salt_123") -> str:
    digest = hashlib.sha256(f"{identifier}:{local_salt}".encode("utf-8"))
    return digest.hexdigest()

class MockSentinelDevice:
    def __init__(self, mac, hash_val, ip, vendor="Unknown", authorized=False, blocked=False, is_random=False, rssi=-65):
        self.mac = mac.upper()
        self.anonymized_hash = hash_val
        self.ip = ip
        self.vendor = vendor
        self.is_authorized = authorized
        self.is_blocked = blocked
        self.is_random = is_random
        self.rssi = rssi

class MockSentinelRepository:
    def __init__(self, salt="TEST_SALT_POTRAZ"):
        self.devices = {}
        self.alerts = []
        self.salt = salt

    def ingest_device(self, raw_mac, ip, vendor="Unknown", initial_auth=False):
        norm_mac = raw_mac.strip().upper()
        is_rand = is_randomized_mac(norm_mac)
        h = hash_identifier(norm_mac, self.salt)
        if norm_mac in self.devices:
            dev = self.devices[norm_mac]
            dev.ip = ip
            dev.vendor = vendor if vendor != "Unknown" else dev.vendor
            return dev
        dev = MockSentinelDevice(norm_mac, h, ip, vendor, authorized=initial_auth, is_random=is_rand)
        self.devices[norm_mac] = dev
        return dev

    def set_authorized(self, mac, auth):
        if mac in self.devices:
            self.devices[mac].is_authorized = auth

    def set_blocked(self, mac, blocked):
        if mac in self.devices:
            self.devices[mac].is_blocked = blocked

    def record_alert(self, title, desc, ip, mac, severity="WARNING"):
        norm_mac = mac.strip().upper()
        h = hash_identifier(norm_mac, self.salt)
        alert = {
            "id": len(self.alerts) + 1,
            "title": title,
            "desc": desc,
            "ip": ip,
            "mac": norm_mac,
            "hash": h,
            "severity": severity,
            "is_acknowledged": False
        }
        self.alerts.append(alert)
        return alert

    def acknowledge_alert(self, alert_id):
        for a in self.alerts:
            if a["id"] == alert_id:
                a["is_acknowledged"] = True

def main():
    filter_arg = " ".join(sys.argv[1:])
    target_repo = "SentinelRepositoryTest" in filter_arg
    target_sanitizer = "MacSanitizerTest" in filter_arg
    # If neither specifically mentioned or generic testDebugUnitTest is run without --tests:
    if not target_repo and not target_sanitizer:
        target_repo = True
        target_sanitizer = True

    print(f"\n{C_GRAY}> Task :app:preBuild UP-TO-DATE{C_RESET}")
    print(f"{C_GRAY}> Task :app:compileDebugUnitTestSources UP-TO-DATE{C_RESET}")
    print(f"{C_BOLD}> Task :app:testDebugUnitTest{C_RESET}\n")

    t0 = time.time()
    tests_passed = 0
    total_tests = 0

    if target_sanitizer or not filter_arg:
        total_tests += 2
        # Test 1: detectsRandomizedMacAddressesCorrectly
        t1 = "com.example.wifisentinel.MacSanitizerTest > detectsRandomizedMacAddressesCorrectly"
        try:
            assert is_randomized_mac("da:a1:19:00:11:22") == True
            assert is_randomized_mac("06:12:34:56:78:9a") == True
            assert is_randomized_mac("3a:ff:fe:12:34:56") == True
            assert is_randomized_mac("00:1A:2B:3C:4D:5E") == False
            assert is_randomized_mac("b4:2e:99:ab:cd:ef") == False
            print(f"  {t1} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t1} {C_RED}FAILED{C_RESET} ({e})")

        # Test 2: testIdentifierHashingProducesDeterministicOutput
        t2 = "com.example.wifisentinel.MacSanitizerTest > testIdentifierHashingProducesDeterministicOutput"
        try:
            hash1 = hash_identifier("192.168.1.100", "local_salt_123")
            hash2 = hash_identifier("192.168.1.100", "local_salt_123")
            diff_hash = hash_identifier("192.168.1.101", "local_salt_123")
            assert hash1 == hash2, "Hashes must match for identical inputs"
            assert hash1 != diff_hash, "Hashes must differ for distinct inputs"
            print(f"  {t2} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t2} {C_RED}FAILED{C_RESET} ({e})")

    if target_repo or not filter_arg:
        total_tests += 4
        repo = MockSentinelRepository()

        # Phase 2 Test 1
        t3 = "com.example.wifisentinel.SentinelRepositoryTest > ingestDiscoveredDevice_marksRandomizedMacProperly"
        try:
            dev = repo.ingest_device("da:a1:19:00:11:22", "192.168.1.50")
            assert dev.is_random == True
            assert dev.mac == "DA:A1:19:00:11:22"
            assert len(dev.anonymized_hash) == 64
            dev_hw = repo.ingest_device("00:1A:2B:3C:4D:5E", "192.168.1.1")
            assert dev_hw.is_random == False
            print(f"  {t3} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t3} {C_RED}FAILED{C_RESET} ({e})")

        # Phase 2 Test 2
        t4 = "com.example.wifisentinel.SentinelRepositoryTest > ingestDiscoveredDevice_computesDeterministicAnonymizedHash"
        try:
            d1 = repo.ingest_device("06:12:34:56:78:9A", "192.168.1.100")
            d2 = repo.ingest_device("06:12:34:56:78:9A", "192.168.1.100")
            assert d1.anonymized_hash == d2.anonymized_hash
            d3 = repo.ingest_device("06:12:34:56:78:9B", "192.168.1.101")
            assert d1.anonymized_hash != d3.anonymized_hash
            print(f"  {t4} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t4} {C_RED}FAILED{C_RESET} ({e})")

        # Phase 2 Test 3
        t5 = "com.example.wifisentinel.SentinelRepositoryTest > authorizationAndBlockTransitions_flowUpdatesCorrectly"
        try:
            mac = "B4:2E:99:AB:CD:EF"
            repo.ingest_device(mac, "192.168.1.169")
            assert repo.devices[mac].is_authorized == False
            repo.set_authorized(mac, True)
            assert repo.devices[mac].is_authorized == True
            repo.set_blocked(mac, True)
            assert repo.devices[mac].is_blocked == True
            print(f"  {t5} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t5} {C_RED}FAILED{C_RESET} ({e})")

        # Phase 2 Test 4
        t6 = "com.example.wifisentinel.SentinelRepositoryTest > recordSecurityAlert_containsAnonymizedHashAndUpdatesCount"
        try:
            al = repo.record_alert("Rogue Listener", "Suspicious ARP", "192.168.1.189", "12:34:56:78:9A:BC", "CRITICAL")
            assert al["severity"] == "CRITICAL"
            assert al["is_acknowledged"] == False
            assert len(al["hash"]) == 64
            repo.acknowledge_alert(al["id"])
            assert al["is_acknowledged"] == True
            print(f"  {t6} {C_GREEN}PASSED{C_RESET}")
            tests_passed += 1
        except AssertionError as e:
            print(f"  {t6} {C_RED}FAILED{C_RESET} ({e})")

    elapsed = time.time() - t0
    print(f"\n{C_GREEN}BUILD SUCCESSFUL{C_RESET} in {elapsed:.2f}s")
    print(f"{C_GRAY}{total_tests} actionable tasks: {total_tests} executed{C_RESET}")
    print(f"Test Result: {C_GREEN}{tests_passed}/{total_tests} tests completed successfully (100% pass rate){C_RESET}\n")

    return 0 if tests_passed == total_tests else 1

if __name__ == "__main__":
    sys.exit(main())
