package lifeos.md.tapilot.chat

import org.json.JSONArray
import org.json.JSONObject

/** A runtime task, not a claim that the user's goal has been verified. */
data class ExecutionStep(val id: String, val number: Int, val action: String, val target: String, val status: String) {
    fun label(zh: Boolean): String {
        val names = mapOf(
            "Plan" to ("分析下一步" to "Plan next action"),
            "Locate" to ("定位屏幕目标" to "Locate target"),
            "Tap" to ("点击" to "Tap"), "Input" to ("输入文字" to "Enter text"),
            "Scroll" to ("滚动" to "Scroll"), "Swipe" to ("滑动" to "Swipe"),
            "Launch" to ("打开应用" to "Open app"), "Query" to ("读取屏幕" to "Read screen"),
            "Assert" to ("核对屏幕" to "Check screen"),
            "AndroidBackButton" to ("返回" to "Go back"),
            "AndroidHomeButton" to ("回到桌面" to "Go home"),
            "AndroidRecentAppsButton" to ("打开最近应用" to "Open recent apps"),
        )
        val name = names[action]?.let { if (zh) it.first else it.second } ?: action
        return if (target.isBlank()) name else "$name · $target"
    }
    fun stateLabel(zh: Boolean): String = when (status) {
        "finished" -> if (zh) "已执行" else "Executed"
        "failed" -> if (zh) "失败" else "Failed"
        "cancelled" -> if (zh) "已取消" else "Cancelled"
        "interrupted" -> if (zh) "未确认完成" else "Completion unknown"
        else -> if (zh) "执行中" else "Running"
    }
    companion object {
        fun from(values: JSONArray?): List<ExecutionStep> = if (values == null) emptyList() else
            List(values.length()) { index -> values.getJSONObject(index).let {
                ExecutionStep(it.getString("id"), it.optInt("number", index + 1), it.getString("action"),
                    it.optString("target"), it.getString("status"))
            } }
    }
}

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
            if (existing.optString("status") != "running" || existing.optString("status") == status) return false
            existing.put("status", status)
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
