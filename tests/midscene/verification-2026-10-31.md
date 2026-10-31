# Address-change recovery verification

The saved phone endpoint was 100.82.243.72. mDNS returned no phones, including
a 10-second scan and a scan explicitly bound to the physical Wi-Fi interface.
The user supplied 192.168.31.115. A direct TLS connection verified the original
certificate and authenticated with the existing pairing, then persisted only
the changed endpoint. No pairing credentials were printed.

Added bounded physical-interface IPv4 /24 TLS identity discovery after mDNS
fails, and a persistent Pair / pair again button in the device panel. The
fallback found the same phone at 192.168.31.115 in 5634 ms. An old-address
connection attempt reached authentication but was refused with Another task is
already registered; no controller was forcibly disconnected. Full automatic
recovery while unoccupied remains to be verified. Root cause of absent multicast
advertisements was not established; the fallback avoids requiring them.

All 96 plugin tests passed, including subnet bounds and concurrency/identity
selection tests. Generated UI check passed. Actual native Codex button rendering
and user-driven QR pairing were not exercised in this run. Local Codex plugin
0.6.1 was reinstalled from this working tree. No APK change is required.
