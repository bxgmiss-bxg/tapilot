# Panel recovery at conversation continuation (0.6.10)

The host log recorded `mcp_app_sandbox.browser_presentation_failed` and repeated
`MCP sandbox RPC timed out` failures on 2026-11-02 at 10:35 Asia/Shanghai. These
show a host-side presentation/communication failure, not its underlying cause.

The opener previously always returned `openai/widgetSessionId: tapilot-screen`.
It now accepts `recover: true` and returns a fresh UUID-based widget session.
Ordinary opens reuse the latest session within that MCP worker. Tool instructions
and the bundled skill request recovery once at the start of each user turn that
invokes Tapilot or continues phone work, before CLI operations. They prohibit
reopening for every observation, changing privacy/disconnect state, or replaying
phone actions as part of recovery.

If one recovery request still leaves the host view unusable, the skill directs
continuation through the existing `preview --open` browser fallback. There is no
host-render acknowledgement, so this fallback is based on observed failure or
user feedback, not an inferred phone polling timeout.

Validation: all 118 plugin tests passed. The stdio MCP test verifies fresh session
IDs for recovery, stable IDs for ordinary opens, invalid argument rejection, and
no pairing reads/writes or runtime initialization (both paths intentionally unusable
in the fixture). Built assets pass freshness checks. The installed Codex source
reads the widgetSessionId result metadata; an actual crashed-view replacement has
not been verified because this environment cannot automate the Codex UI.
