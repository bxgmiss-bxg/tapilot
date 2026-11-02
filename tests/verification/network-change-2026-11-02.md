# Network-change recovery and blank-screen status

Observed on the user's Mac: physical interfaces were 100.82.236.136/20 and
100.82.167.61/20, while the selected phone remained saved as 192.168.31.115:39877.
A 2.5-second mDNS discovery returned no matching phone. The phone's current IP
was not available, so corporate Wi-Fi isolation and real-phone recovery remain
unverified. The existing fallback only covers one /24 slice per interface.

Two regression tests failed before the fix: a live response without image data
could leave an empty preview labelled Connected, and failed image decoding also
retained that label. Both pass after the fix. A slow frame request now clears old
pixels and Connected status after five seconds; actions stay disabled until fresh
pixels load.

Transport tests verify that changing the physical network invalidates a stale
connection without replaying a mutation. TLS tests verify discovery can skip an
unrelated certificate and authenticate/save a later correct endpoint; no token
is sent to the unrelated server. Address-update tests verify identity before save,
retain credentials/selection/manual-disconnect state, and reject invalid or
unverified addresses without changing the registry. UI tests verify address
updates only initiate connection after server verification.

Validation: full plugin check passed (123 tests), then two additional screen
interaction cases passed. One existing runtime-stop/restart test had a transient
fetch failure on the first run; its isolated rerun and the subsequent full suite
passed. These tests do not prove reachability of the phone on company Wi-Fi.
