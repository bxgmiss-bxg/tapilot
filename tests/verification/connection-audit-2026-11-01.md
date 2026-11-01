# Connection recovery audit — 2026-11-01

## Fixed in desktop plugin 0.6.8

1. Failed explicit connect left previews paused as `disconnected`, preventing recovery when networking or accessibility returned. Observation retries now resume before connecting. Explicit disconnect and privacy pause retain their behavior.
2. A remembered lock hid later pairing or transport errors. Report the latest observed failure without bypassing privacy pauses.
3. An old connection alert remained after recovery. Clear connection alerts on a fresh live result; preserve uncertain-action warnings.
4. Android local-conversation and device-lease errors appeared as network unavailability. Both now report `occupied`, with Chinese and English recovery messages.
5. Reinstalling changed code with the same runtime version retained the existing process. Bumped to 0.6.8 and tested the serialized upgrade from 0.6.7. This establishes a stale-code risk, not the precise OS/network cause of the earlier connection failure.

## Evidence

Four regression cases failed before their fixes and passed afterward. Full plugin check: **113 passed, zero failed**, including UI build consistency, explicit-disconnect persistence, privacy pause, certificate mismatch, mutation non-replay and concurrent runtime upgrade.

Live API 35 emulator validation, using its existing TLS pairing via an ADB TCP relay:

- Connected and captured a fresh screen.
- Disabled Wi-Fi; preview became unavailable and explicit connect failed.
- Re-enabled Wi-Fi; preview recovered without another connect or pairing request.
- Restarted the desktop runtime; preview recovered without pairing.
- Compared both saved device credentials before and after: unchanged.
- Restored the original physical-phone selection; connected and captured a fresh 988 × 1972 screen showing computer connected.

The emulator remains running. No phone APK update, credential reset, commit, push or public release was performed.

## Limits

Physical-phone DHCP changes, VPN changes, router client isolation and physical-phone Wi-Fi loss were not exercised. Discovery and credential-preservation unit tests do not establish physical network acceptance. The exact cause of the old process's earlier network error remains unproven.

## Artifacts

- Full checks: `/tmp/tapilot-deep-check.log`
- Pre-fix failures: `/tmp/tapilot-connection-repro.log`, `/tmp/tapilot-occupied-repro.log`
- Live results: `/tmp/tapilot-connection-live-results.json`
- Physical-phone report: `/Users/bytedance/.local/share/android-use/midscene_run/report/tapilot-92b83d4e-ef63-478a-9133-309e34920014.html`
