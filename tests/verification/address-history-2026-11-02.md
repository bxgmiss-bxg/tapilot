# Pairing latency and remembered addresses — 0.6.13

- Cause: pairing status used the disconnected preview's 4000 ms polling interval
  and waited for device-list refresh first.
- Pairing now polls every 300 ms and skips device-list refresh while pairing.
  Actual host/phone latency is not guaranteed by that interval.
- Each TLS phone identity keeps up to eight recent IP/port endpoints. Saving
  a verified new endpoint retains older endpoints and the current credentials.
- On connection failure, remembered endpoints receive parallel, bounded TLS-only
  probes before mDNS/LAN discovery. A matching endpoint must still pass pinned
  TLS verification and authentication before it becomes current. No phone action
  is replayed. Forgetting a phone removes its history.
- Legacy records gain history when saved; previously overwritten addresses cannot
  be recovered from records that no longer contain them.
- Validation: plugin check passed 132 tests. Added UI polling/confirmation test,
  registry history/credential/bound/forget test, and real TLS reconnect test with
  an unrelated certificate at a remembered address (zero credentials sent).
- Physical home/company network switching and Codex embedded timing remain
  unverified. Discovery restrictions and entirely new IPs can still require scan
  recovery. Existing mounted panels must reload the installed plugin.
