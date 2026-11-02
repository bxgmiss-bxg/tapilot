package lifeos.md.tapilot.conversation

import org.json.JSONObject

internal object FirstTaskGuide {
    fun verifiedPhoneTask(entry: JSONObject): Boolean = entry.optString("kind", "task") == "task" &&
        entry.optString("status") == "succeeded" && entry.optString("phase") == "completed" &&
        entry.optJSONArray("completedOperations")?.objects().orEmpty().any { it.optBoolean("success") }

    fun visible(data: JSONObject): Boolean = !data.optBoolean("firstTaskDismissed") &&
        !data.optBoolean("firstTaskCompleted") &&
        data.optJSONArray("history")?.objects().orEmpty().none(::verifiedPhoneTask)

    fun example(zh: Boolean) = if (zh) "打开计算器，计算 12 × 8，并读取屏幕上显示的结果。"
        else "Open Calculator, calculate 12 × 8, and read the result shown on screen."
}
