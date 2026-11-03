package lifeos.md.tapilot.conversation

import org.json.JSONArray
import org.json.JSONObject

/** Bounded per-attempt history; repeated snapshots update a row instead of adding duplicates. */
object ExecutionSteps {
    const val LIMIT = 500
    fun update(entry: JSONObject, event: JSONObject): Boolean {
        val id = event.optString("id").take(200)
        val action = event.optString("action").take(60)
        val status = event.optString("status")
        if (id.isBlank() || action.isBlank() || status !in setOf("running", "finished", "failed", "cancelled")) return false
        val rows = entry.optJSONArray("executionSteps") ?: JSONArray().also { entry.put("executionSteps", it) }
        val existing = (0 until rows.length()).map { rows.getJSONObject(it) }.find { it.optString("id") == id }
        if (existing != null) {
            // Descriptions can resolve after the first snapshot; state remains monotonic.
            val target = event.optString("target").take(240)
            val updateTarget = target.isNotBlank() && target != existing.optString("target")
            val updateStatus = existing.optString("status") == "running" && status != "running"
            if (!updateTarget && !updateStatus) return false
            if (updateTarget) existing.put("target", target)
            if (updateStatus) existing.put("status", status)
        } else {
            val number = entry.optInt("executionStepCount") + 1
            entry.put("executionStepCount", number)
            if (rows.length() >= LIMIT) rows.remove(0)
            rows.put(JSONObject().put("id", id).put("number", number).put("action", action)
                .put("target", event.optString("target").take(240)).put("status", status))
        }
        return true
    }
    fun settle(entry: JSONObject) {
        val rows = entry.optJSONArray("executionSteps") ?: return
        for (i in 0 until rows.length()) rows.getJSONObject(i).let {
            if (it.optString("status") == "running") it.put("status", "interrupted")
        }
    }
}
