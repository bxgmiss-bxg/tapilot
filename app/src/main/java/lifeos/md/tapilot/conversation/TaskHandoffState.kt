package lifeos.md.tapilot.conversation

import org.json.JSONObject

/** Only the latest settled task may offer a handoff continuation. Never replay an older task. */
internal object TaskHandoffState {
    fun pending(history: List<JSONObject>, conversationId: String): JSONObject? = history
        .filter { it.optString("conversationId", it.optString("id")) == conversationId && it.optString("kind", "task") == "task" }
        .maxByOrNull { it.optLong("createdAt") }
        ?.takeIf {
            (it.optString("status") == "stopped" && it.optString("stopReason") == "user_handoff") ||
                it.optString("status") == "waiting_user"
        }
}
