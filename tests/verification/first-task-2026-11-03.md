# First-task guide: OPPO acceptance, 2026-11-03

Physical device: OPPO PEUM00, Android 14. Isolated Debug application: `lifeos.md.tapilot.e2everify`. Production app data was preserved. The final integrated candidate was `89dae1a` and included the related handoff and progress changes.

1. Open the English first-task guide with model and device-control readiness shown separately.
2. Open model settings from the guide. The model group is at the top of settings.
3. Configure SIWC and enable device control. ColorOS background activity was enabled for the isolated app to complete the browser callback.
4. Fill the Calculator example: it remains an unsent draft. Review it before sending. Confirm that the keyboard does not hide the composer or send control.
5. Send the example. Calculator displays 12 × 8 = 96; independent screen verification succeeds.
6. Return to chat: the guide is gone. Restart: completion and history remain persisted. An earlier failed execution did not mark the guide complete.
7. Use Settings → Appearance & language → Language → Simplified Chinese. The app labels switch to Chinese.
8. To inspect the Chinese first-task card, temporarily move the isolated app's conversation file to a backup while stopped, then launch an empty test session. Confirm the title, explanation, readiness labels and buttons are Chinese. This is a localization UI check with an empty-session fixture, not a second successful live task.
9. Stop the app and restore the original conversation file. Verify the real test history remains, then re-enable device control through the normal ColorOS confirmation dialog.

## Evidence

- [English draft and keyboard layout](first-task-2026-11-03/acceptance-fixed-composer.png)
- [Successful result and completed guide](first-task-2026-11-03/acceptance-success-chat.png)
- [Language picker](first-task-2026-11-03/language-picker.png)
- [Chinese guide with empty-session fixture](first-task-2026-11-03/first-guide-zh-ready.png)

Both Chinese and English examples are covered by unit tests. No additional locales are claimed. The integrated Android test suite passed 408 tests with one existing opt-in live speech test skipped; beta lint and Debug APK packaging passed.
