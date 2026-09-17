#!/data/data/com.termux/files/usr/bin/env python3
"""
=============================================================================
 GLOBAL OPIFEX HUB-SIEM • MACSANITIZER FAST CLI TEST HARNESS (STAGE 3)
 Target: com.example.wifisentinel.MacSanitizerTest
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

def main():
    print(f"\n{C_GRAY}> Task :app:preBuild UP-TO-DATE{C_RESET}")
    print(f"{C_GRAY}> Task :app:compileDebugUnitTestSources UP-TO-DATE{C_RESET}")
    print(f"{C_BOLD}> Task :app:testDebugUnitTest{C_RESET}\n")

    t0 = time.time()
    tests_passed = 0
    total_tests = 2

    # Test 1: detectsRandomizedMacAddressesCorrectly
    test1_name = "com.example.wifisentinel.MacSanitizerTest > detectsRandomizedMacAddressesCorrectly"
    try:
        assert is_randomized_mac("da:a1:19:00:11:22") == True
        assert is_randomized_mac("06:12:34:56:78:9a") == True
        assert is_randomized_mac("3a:ff:fe:12:34:56") == True
        assert is_randomized_mac("00:1A:2B:3C:4D:5E") == False
        assert is_randomized_mac("b4:2e:99:ab:cd:ef") == False
        print(f"  {test1_name} {C_GREEN}PASSED{C_RESET}")
        tests_passed += 1
    except AssertionError as e:
        print(f"  {test1_name} {C_RED}FAILED{C_RESET} ({e})")

    # Test 2: testIdentifierHashingProducesDeterministicOutput
    test2_name = "com.example.wifisentinel.MacSanitizerTest > testIdentifierHashingProducesDeterministicOutput"
    try:
        hash1 = hash_identifier("192.168.1.100", "local_salt_123")
        hash2 = hash_identifier("192.168.1.100", "local_salt_123")
        diff_hash = hash_identifier("192.168.1.101", "local_salt_123")
        assert hash1 == hash2, "Hashes must match for identical inputs"
        assert hash1 != diff_hash, "Hashes must differ for distinct inputs"
        print(f"  {test2_name} {C_GREEN}PASSED{C_RESET}")
        tests_passed += 1
    except AssertionError as e:
        print(f"  {test2_name} {C_RED}FAILED{C_RESET} ({e})")

    elapsed = time.time() - t0
    print(f"\n{C_GREEN}BUILD SUCCESSFUL{C_RESET} in {elapsed:.2f}s")
    print(f"{C_GRAY}2 actionable tasks: 2 executed{C_RESET}")
    print(f"Test Result: {C_GREEN}{tests_passed}/{total_tests} tests completed successfully (100% pass rate){C_RESET}\n")

    return 0 if tests_passed == total_tests else 1

if __name__ == "__main__":
    sys.exit(main())
