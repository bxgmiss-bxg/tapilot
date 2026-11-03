# Floating virtual display: OPPO acceptance, 2026-11-03

Physical device: OPPO PEUM00, Android 14. Application: isolated `lifeos.md.tapilot.e2everify` Debug package. Production app and data were preserved. The device-control service was enabled; overlay permission was granted through Android settings to the test package only.

## Steps and results

1. Start the secondary-display probe. Display 12 renders the fixture and produces actual captured pixels.
2. Select Float and control display. The movable overlay shows the same fixture and display ID with letterboxing preserved.
3. Tap the fixture button inside the overlay. The Android gesture callback reports cancellation; the fixture count remains zero. The footer explicitly reports no primary-screen fallback.
4. Press Home. The overlay remains visible over the launcher. Drag its title bar and confirm it moves without moving the underlying launcher content.
5. After two minutes, the overlay disappears. Reopen the existing probe activity: it reports timeout and released resources, with the preview cleared.
6. Start again. Display 13 appears in a new overlay. Open Calculator on the primary display separately; the fixture remains visible over Calculator. This does **not** mean Calculator was launched on the virtual display.
7. Move the overlay and tap the primary Calculator's exposed 0 key outside it. The primary app accepts the input while the overlay stays visible.
8. Swipe inside the overlay. The system again cancels the secondary-display gesture; the fixture count stays zero and the primary screen does not receive the swipe.
9. Close the floating window using ×. Reopen the probe activity: it reports resources released and shows no preview or overlay.

**Verified:** real floating preview over another app, title-bar dragging, primary-screen input outside the window, explicit tap/swipe rejection, timeout cleanup, new-session recreation, and close-button cleanup.

**Not established:** successful secondary-display input on this device, arbitrary third-party apps running inside the virtual screen, background Agent execution, text/keyboard forwarding, or secondary-display system navigation. The window exposes a public-API input path; it cannot grant privileges Android denies.

## Evidence

- [floating-visible](floating-display-2026-11-03/floating-visible.png)
- [floating-input-result](floating-display-2026-11-03/floating-input-result.png)
- [floating-other-app](floating-display-2026-11-03/floating-other-app.png)
- [floating-moved-primary-input](floating-display-2026-11-03/floating-moved-primary-input.png)
- [floating-swipe-result](floating-display-2026-11-03/floating-swipe-result.png)
- [floating-timeout-status](floating-display-2026-11-03/floating-timeout-status.png)
- [floating-close-released](floating-display-2026-11-03/floating-close-released.png)

## Checks

- Debug unit tests: 398 passed; one existing opt-in speech test skipped. Includes three coordinate-mapping regressions for scaled coordinates, letterboxing, bounds and invalid inputs.
- `lintDebug` and isolated Debug APK build passed.
- Final APK embedded runtime archive SHA-256 matches its embedded manifest and the completed runtime bundle.
- `git diff --check` and local documentation/evidence links passed.

An initial packaging attempt lacked a current runtime asset after switching branches. The runtime was rebuilt, the APK was repackaged and its embedded checksum verified before the accepted device run.

## Borderless presentation follow-up

Removed the title bar, footer and colored window chrome. The capture now fills a rounded, borderless surface with a small drag handle and a translucent close control. Both controls retain 48 dp touch targets. Gesture cancellation uses a brief message; detailed probe status remains on the experiment page.

Validation: `assembleDebug -Pe2eVerification` and `lintDebug` passed. The updated isolated APK was installed successfully on the same OPPO. Visual verification is pending: the device remained on its lock screen, and wake/swipe attempts produced black or empty captures. Earlier screenshots above show the previous presentation, not this revision.
