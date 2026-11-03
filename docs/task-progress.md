# Task execution progress

Tapilot records real runtime steps in the conversation. The latest step is visible while a task runs; **Show progress** expands the numbered history. Finished tasks retain that history after reopening the app.

## Event path

1. Midscene calls `Agent.onDumpUpdate` with the current `ExecutionDump`.
2. `step-progress.mjs` projects started Planning, Insight and Action Space tasks into small `execution.step` events. Task IDs remain stable across status updates. Pending plans are not presented as executed work.
3. Direct workflow nodes (`launch`, `home`, `back`, `recentApps`) emit started and terminal events because they bypass the Agent snapshot path.
4. Node writes each changed event to its existing `MIDSCENE_EVENT` channel. Input values, model thoughts, raw errors, screenshots and model responses are not included in the event payload.
5. `NodeTestRunCoordinator` attaches the owning conversation session. The main-thread coordinator ignores stale sessions and events received after stopping.
6. `ExecutionSteps` inserts a numbered row or updates the existing row, then persists the conversation. A terminal row cannot regress to running.
7. Native chat renders each row's action, available visible-target description and state. The latest 500 rows are retained in chat; older detail remains in the execution report. The UI explicitly indicates truncated history.
8. Task completion or process recovery marks unfinished rows as **Completion unknown**. A finished step means that execution stage ended; independent screenshot verification still decides whether a phone operation achieved its goal.

## Relationship to Midscene playground

The reference is the sibling Midscene repository's playground server (`packages/playground/src/server.ts`), which observes `onDumpUpdate` and stores an `ExecutionDump` per request, and the visualizer's grouped progress presentation. Tapilot uses the same supported SDK snapshot callback, with an independent native event projection, persisted conversation ledger and Compose UI. It does not embed or copy the playground web UI.

Earlier Tapilot versions only changed a short status when native capture or execute RPCs arrived. They did not retain a per-step history. This change adds that history without changing execution, confirmation, handoff or result-verification policy.

No Mika implementation or wording is used.
