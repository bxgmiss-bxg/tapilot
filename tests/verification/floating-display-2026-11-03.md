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


### Android 15 emulator visual verification

The borderless revision was verified separately on a fresh Pixel 7 / Android 15 ARM64 emulator using the isolated Debug package. This is emulator evidence, not OPPO evidence.

1. Start the secondary-display experiment and open the floating preview.
2. Return to Home: the actual fixture capture remains visible without a title bar, footer or outer border.
3. Drag the small top handle: the window moves while retaining its captured content and close control.
4. The Settings app hides the overlay; returning Home shows it again. Do not assume overlays are visible over every system or protected screen.
5. Close the test and shut down the isolated emulator to release local resources.

The light-blue fill belongs to the fixture itself. This visual check does not establish secondary-display input support; device control was not enabled in this emulator.

- [Borderless preview over Home](floating-display-2026-11-03/emulator-borderless-home.png)
- [Preview after dragging](floating-display-2026-11-03/emulator-borderless-moved.png)


### Android 15 emulator capability and lifecycle recheck

Reused the isolated Pixel 7 ARM64 / API 35 emulator with the borderless APK (implementation commit `2fea4a9`; later commits only update documentation). APK SHA-256: `bd3187e87fb111de037a0334767a087bc9a1c9eb34f895235436609aed9dda7c`. The test package's accessibility service was enabled and confirmed bound before exercising input. No root, shell injection into the virtual display, or model calls were used to make secondary input succeed. ADB drove the primary-screen test controls only.

1. Start the display and press **Tap secondary fixture**. Android cancels the gesture; the actual fixture counter remains zero. [Evidence](floating-display-2026-11-03/emulator-verify-tap.png).
2. Choose **Clock** from the app picker. The app reports that the system disallows launching it on the experiment display. The primary screen remains on the probe. [Evidence](floating-display-2026-11-03/emulator-verify-launch.png).
3. Open the floating preview. At the existing two-minute limit the overlay disappears, the preview clears, and status reports timeout/release. A drag attempted at the timeout boundary landed on the now-exposed primary input; this is not counted as a virtual-display swipe result. [Evidence](floating-display-2026-11-03/emulator-verify-swipe-drag.png).
4. Restart and float the new display (ID 3). The fixture appears again. [Evidence](floating-display-2026-11-03/emulator-verify-recreated.png).
5. Swipe inside the active floating preview. Android cancels this gesture too, the explicit cancellation message appears and the counter stays zero. [Evidence](floating-display-2026-11-03/emulator-verify-swipe-cancelled.png).
6. Drag the handle to the right. The window moves and retains its content. [Evidence](floating-display-2026-11-03/emulator-verify-dragged.png).
7. Type `main_ok` into the exposed primary input while the overlay remains visible. Text and keyboard work on the primary screen. [Evidence](floating-display-2026-11-03/emulator-verify-primary-input.png).
8. Press the overlay close control. Status changes to `Off; experiment resources released`, both previews disappear and primary text is preserved. `dumpsys display` contains no Tapilot probe display after closing. [Evidence](floating-display-2026-11-03/emulator-verify-close.png).
9. Start and float again, then leave the experiment with Back. Home has no floating preview and `dumpsys display` again contains no Tapilot probe display. Shut down the isolated emulator and release the runtime slot. [Evidence](floating-display-2026-11-03/emulator-verify-destroyed.png).

Result: rendering, capture, movement, primary input and cleanup pass in this emulator. Secondary input and Clock launch do not. These results reproduce the capability limitations beyond the two physical OEM devices; they do not establish that every Android configuration rejects secondary input. Keep the feature experimental and do not route production Agent tasks to it.
