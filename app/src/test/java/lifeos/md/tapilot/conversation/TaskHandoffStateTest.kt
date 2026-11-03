package lifeos.md.tapilot.conversation

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TaskHandoffStateTest {
    private fun task(id: String, at: Long, status: String = "stopped") = JSONObject()
        .put("id", id).put("conversationId", "chat").put("createdAt", at)
        .put("status", status).put("stopReason", "user_handoff")

    @Test fun handoffOnlyBecomesAvailableAfterConfirmedStop() {
        assertNull(TaskHandoffState.pending(listOf(task("a", 1, "running")), "chat"))
        assertNull(TaskHandoffState.pending(listOf(task("a", 1, "failed")), "chat"))
        assertEquals("a", TaskHandoffState.pending(listOf(task("a", 1)), "chat")?.getString("id"))
    }
    @Test fun newerTaskHidesOldHandoffRegardlessOfHistoryOrder() {
        val old = task("a", 1)
        val next = task("b", 2, "succeeded")
        assertNull(TaskHandoffState.pending(listOf(next, old), "chat"))
        assertNull(TaskHandoffState.pending(listOf(old, next), "chat"))
    }
    @Test fun openingFloatingPanelOffersContinuationAfterItStopsTheTask() {
        val paused = task("a", 1).put("stopReason", "panel")
        assertSame(paused, TaskHandoffState.pending(listOf(paused), "chat"))
        paused.put("status", "running")
        assertNull(TaskHandoffState.pending(listOf(paused), "chat"))
    }
    @Test fun ordinaryStopDoesNotOfferManualHandoff() {
        assertNull(TaskHandoffState.pending(listOf(task("a", 1).put("stopReason", "user")), "chat"))
    }
    @Test fun waitingForUserIsResumableAndOtherConversationsAndAmendmentsAreIgnored() {
        val waiting = task("a", 1, "waiting_user")
        val amendment = task("b", 2, "succeeded").put("kind", "instruction")
        val other = task("c", 3, "succeeded").put("conversationId", "other")
        assertSame(waiting, TaskHandoffState.pending(listOf(other, amendment, waiting), "chat"))
    }
    @Test fun handoffSummaryKeepsVerifiedProgressWithoutClaimingCompletion() {
        val entry = task("a", 1).put("completedOperations", org.json.JSONArray()
            .put(JSONObject().put("success", true).put("result", "Draft saved")))
        val summary = TaskFeedback.response(entry, false)
        assertTrue(summary.contains("Automatic actions have stopped"))
        assertTrue(summary.contains("Draft saved"))
        assertTrue(summary.contains("not yet confirmed complete"))
        entry.put("status", "failed")
        assertFalse(TaskFeedback.response(entry, false).contains("You can now use the phone"))
    }
}
