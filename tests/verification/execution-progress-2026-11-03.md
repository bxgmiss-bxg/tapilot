# Execution progress: OPPO acceptance, 2026-11-03

Device: OPPO PEUM00, Android 14. The isolated `lifeos.md.tapilot.e2everify` Debug app was used; the production app and its data were retained. This is physical-device acceptance, not an emulator or fixture run. The local integration included PRs #29–#32; the latest tested integration commit was `89dae1a`.

## Steps and observations

1. Configure SIWC and enable device control in the isolated app.
2. Fill the English first-task example, review the unsent draft, and send it.
3. Observe Calculator execute 12 × 8 and display 96. Independent screen verification succeeded. This earlier run exposed missing protocol timestamps, which were fixed before the successful run.
4. Expand chat progress: numbered execution rows are visible. Restart the app: the rows remain available. ColorOS disables this app's accessibility service after force-stop; enable it again through system settings for subsequent execution.
5. Install the final target-description fix and send a real task to calculate 23 × 7.
6. Observe Launch, Plan, Locate and Tap events. Resolved tap targets include `AC clear button`, `2 key` and `3 key`. Plan and Locate are distinct; the cancelled multiplication tap remains cancelled rather than becoming successful.
7. Bring Tapilot forward and take over. The interrupted task retains 14 rows. This run is intentionally incomplete and is not a successful calculation of 161.
8. Switch the app to Simplified Chinese, restart and expand the retained history. Action/state labels are localized; model-generated target descriptions retain their original language. Scroll through the rows and confirm targets and terminal states remain visible.

The subsequent handoff continuation made a fresh capture, but the model request timed out. That result is not counted as successful handoff acceptance or a completed calculation. No raw OAuth data, model reasoning, or private input values are included in this evidence.

## Screenshots

- [Calculator result](execution-progress-2026-11-03/acceptance-calculator-progress.png)
- [Original numbered rows](execution-progress-2026-11-03/acceptance-progress-rows.png)
- [History after restart](execution-progress-2026-11-03/acceptance-restarted.png)
- [Localized rows and resolved targets](execution-progress-2026-11-03/progress-target-visible.png)
- [Cancelled step retained](execution-progress-2026-11-03/continuation-retry.png)

## Automated checks

The final runtime bundle passed 135 tests. The integrated Android checks passed 408 tests with one existing opt-in live speech test skipped; beta lint and isolated Debug APK packaging passed. No changes were merged into main or released as part of this acceptance.

## Later handoff retry

A subsequent Chinese handoff retry successfully verified the manually completed 23 × 7 = 161 result. Its resumed ledger contained Query, Plan, Query with no repeated calculator taps. The initial network-failed attempt above remains a separate failed attempt. See [handoff acceptance](https://github.com/tapilot-ai/tapilot/blob/feat/explicit-task-handoff-20261103/tests/verification/handoff-2026-11-03.md) for the complete sequence.
