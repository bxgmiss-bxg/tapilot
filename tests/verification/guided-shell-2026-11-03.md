# Guided shell setup verification — 2026-11-03

## Scope and environment

Experimental Debug flow only. No Agent background task execution is claimed. Tests used the isolated `lifeos.md.tapilot.e2everify` package. Device interactions used screenshot-guided ADB input; these are not Midscene AI E2E results. The computer installed the candidate and drove visible UI for testing, but did not perform ADB pairing or helper bootstrap for the successful phone setup paths.

- Android 15 / API 35 ARM64 emulator, AVD `Tapilot_Borderless_20261103`.
- OPPO Find N / PEUM00, Android 14, folded outer screen.
- English and Chinese app resources; phone system settings remained English.

## Observed results

| Check | Emulator | OPPO |
| --- | --- | --- |
| Consent initially off; recovery separately optional | Passed | Passed |
| Wireless debugging shortcut | Correct row highlighted | Correct developer-options section opened |
| Local pairing-port discovery | Passed | Passed |
| Six-digit code through notification, no manual ports | Passed | Passed |
| Real shell UID 2000 helper and authenticated Binder connection | Passed | Passed |
| Captured floating display, actual test counter 0 → 1 | Passed | Passed |
| Closing setup retains connection | Passed | Passed |
| Revocation stops helper | Passed | Passed |
| Incorrect pairing code rejected without helper | Passed | Not repeated |
| Inline code history removed after reply processing | Passed | Passed on successful pairing |
| Reuse existing pairing after fresh consent | Passed | Passed on final candidate |
| Force-stop with active display releases helper/display | Passed | Passed on final candidate |
| Automatic recovery after Wireless debugging switched off | Passed; no display recreated | Permission refused |
| Real device reboot restores connection without pairing again | Passed; no display recreated | Not claimed |
| Forget pairing deletes local private identity | Passed | Not repeated |

The emulator recovery check disabled `adb_wifi_enabled` as fault injection. The opted-in app re-enabled it itself, connected a fresh helper, and left only the primary display. The reboot check used a real emulator reboot, not an app restart.

OPPO rejected the same fixed grant command used by the paired client with `SecurityException: grantRuntimePermission: Neither user 2000 nor current process has android.permission.GRANT_RUNTIME_PERMISSIONS`. A workstation diagnostic reproduced the refusal and granted nothing. No security-monitoring settings were disabled. This device requires manual Wireless debugging restoration; pairing, helper startup and floating input still work.

## Evidence

- [OPPO floating input and successful trial](guided-shell-2026-11-03/oppo-floating-input.png)
- [Emulator floating input and successful trial](guided-shell-2026-11-03/emulator-floating-input.png)
- [Emulator connection restored after reboot](guided-shell-2026-11-03/emulator-reboot-restored.png)
- [Chinese consent screen](guided-shell-2026-11-03/chinese-consent.png)

The OPPO floating-input capture is from the final candidate. The emulator captures precede bottom-placement polish. Pairing-code and lockscreen images are deliberately excluded.

- [Final OPPO recovery permission explanation](guided-shell-2026-11-03/oppo-recovery-limitation.png)
- [OPPO reconnect after force-stop and opening the ordinary main activity, without recreating a display](guided-shell-2026-11-03/oppo-reconnected-without-display.png)

## Limitations

No claim is made for every OEM, Android version, third-party app, battery policy or Wi-Fi network. No OPPO reboot recovery claim is made. Manual port fallback and computer fallback retain their preceding transport coverage but have not received a full new UI end-to-end run in this guided-flow candidate. Android controls trusted-network prompts and can require user interaction.

## Final build and regression check

`assembleDebug`, `testDebugUnitTest`, `lintDebug` and `assembleBeta` passed using JDK 17, Gradle workers=2 and no parallel execution. Unit results: 407 total, 406 passed, one existing skipped test, zero failures/errors. Beta merged manifest has no shell setup activity, recovery receiver or `WRITE_SECURE_SETTINGS`; Beta DEX has no `lifeos.md.tapilot.shell` classes.

Debug APK SHA-256: `c26c78f6b8e32405f415475a38402af9be10e6691458e1b3512c3711a960a7ab`.
Beta APK SHA-256: `2b92f5eec1f2d9ee820c683a9c95d1c0bb3156bdc8ddc2f3eba1a45ff7299110`.

The final Debug APK was installed on OPPO. Revoked consent remained off after update. Fresh consent reused pairing, reported the actual recovery-permission limitation, and passed the floating test. Force-stop removed the helper and display. Opening the ordinary main activity restored the authorized helper without a display. Revocation then removed the helper again. No package release or PR merge was performed.
