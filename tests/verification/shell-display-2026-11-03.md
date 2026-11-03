# Optional shell helper verification — 2026-11-03

Status: Experimental, Debug only, off by default. This record covers the new shell-assisted path, not production Agent execution.

## Environment

- Isolated Pixel 7 ARM64 emulator, Android 15 / API 35, serial `emulator-5580`.
- Verification package `lifeos.md.tapilot.e2everify`; normal ADB shell UID 2000, no root.
- Independently implemented helper; no Mika code or wording included.
- No OPPO or Redmi retest of this new shell path in this run. Previous accessibility-only failures do not describe this implementation.

## Observed checks

1. With no consent, Start was disabled and the computer starter exited with code 1. [Screenshot](shell-display-2026-11-03/unauthorized-final.png).
2. Accepted the in-app consent dialog, then ran the starter against the explicitly selected emulator. The helper connected and Start became available.
3. Created display 2 and opened the floating preview. A tap in the preview changed the actual fixture counter from 0 to 1. [Screenshot](shell-display-2026-11-03/floating-tap-final.png).
4. Selected Clock. System activity diagnostics placed `com.google.android.deskclock` on display 2 while the experiment remained on display 0. The floating preview showed Clock.
5. Tapped the Stopwatch tab and Start through the preview; the real timer advanced. Tapped Pause successfully. [Running stopwatch](shell-display-2026-11-03/stopwatch-running.png).
6. Entered `primary-focus-ok` in the primary-screen input, operated the floating stopwatch and dragged its handle. Primary input retained focus; text remained intact.
7. Revoked authorization while the floating display was active. The overlay and capture disappeared, the helper exited, and display diagnostics showed only display 0. Start became disabled. [Screenshot](shell-display-2026-11-03/revoked.png).
8. Authorized and connected a fresh helper, created another floating display, then killed only that test helper process. Binder death released the display and overlay and disabled Start. A starter retry without fresh consent failed. [Screenshot](shell-display-2026-11-03/helper-death.png).
9. The two-minute display timeout also released the display during the earlier shell test in this session. No production background-task claim is made.

## Automated checks

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug -Pe2eVerification`: passed with Gradle worker limit 2 and no parallel build.
- Android unit tests: 401 total, 400 passed, 1 existing skip, 0 failures/errors. New consent tests cover missing consent, wrong caller, wrong token, replay, expiry and revocation.
- Runtime bundle preparation: 130 tests passed.

## Remaining limits

Activation currently requires a computer with an authorized ADB connection and a Debug build. There is no in-app wireless pairing wizard. Android 12+ is required by the handoff adapter; only Android 15 was exercised here. OEM compatibility, other Android versions, long-running sessions, virtual-screen text entry/system navigation and Agent task routing are not established by these checks.
