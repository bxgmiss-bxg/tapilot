package lifeos.md.tapilot.conversation

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FirstTaskGuideTest {
    private fun state(vararg entries: JSONObject) = JSONObject().put("history", JSONArray(entries.toList()))
    private fun task(status: String = "succeeded", phase: String = "completed", success: Boolean = true) = JSONObject()
        .put("status", status).put("phase", phase)
        .put("completedOperations", JSONArray().put(JSONObject().put("success", success)))

    @Test fun newInstallAndFailedTaskKeepGuideAvailable() {
        assertTrue(FirstTaskGuide.visible(state()))
        assertTrue(FirstTaskGuide.visible(state(task("failed", "failed"))))
        assertTrue(FirstTaskGuide.visible(state(task(success = false))))
    }
    @Test fun generalAnswerAndPartialOrInterruptedTaskDoNotFinishGuide() {
        assertTrue(FirstTaskGuide.visible(state(JSONObject().put("status", "succeeded").put("phase", "completed"))))
        assertTrue(FirstTaskGuide.visible(state(task("waiting_user", "waiting_user"))))
        assertTrue(FirstTaskGuide.visible(state(task("stopped", "paused"))))
        assertTrue(FirstTaskGuide.visible(state(task().put("kind", "instruction"))))
    }
    @Test fun verifiedCompletionOrExplicitSkipHidesGuide() {
        assertFalse(FirstTaskGuide.visible(state(task())))
        assertFalse(FirstTaskGuide.visible(state().put("firstTaskCompleted", true)))
        assertFalse(FirstTaskGuide.visible(state().put("firstTaskDismissed", true)))
    }
}
