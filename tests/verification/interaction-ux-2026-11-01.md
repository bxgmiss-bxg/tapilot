# Device interaction improvements (0.6.9)

- Device switches show the target name and row progress; successful switches close the floating picker and restore focus. Failed switches retain a retry action.
- Network recovery uses one status line, a visible retry action, and expandable help. Pairing/install prompts are reserved for setup or invalid pairing.
- Active-device rows reflect lock, occupancy, reconnect, privacy pause, and accessibility state. Addresses live in device details.
- Pairing expiry keeps an explicit refresh/cancel screen. Scan, confirmation, and successful connection remain separate phases.
- Arrow keys, Home/End, native Enter, Escape, live status announcements, and reduced-motion loading are supported.
- Outside-click detection uses the original event path, so replacing device rows cannot close the picker prematurely. Device-list responses from an older UI epoch are ignored.

## Validation

- `node scripts/check.mjs --quick`: passed, including 118 plugin tests, 18 E2E infrastructure checks, and accessibility package tests.
- Ego browser ran the actual screen module with mocked transport: picker keeps main top at 82 px; switching retains the open picker while busy, then closes and focuses SUMMARY; pairing expiry exposes refresh and cancel.
- Browser screenshots inspected for expiry layout. Browser transport was mocked; this is not a new physical-device or embedded Codex acceptance test.
- Local plugin installed at 0.6.9. Emulator-5554 remains running. No phone pairing credentials changed.
