package lifeos.md.tapilot.chat

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import lifeos.md.tapilot.conversation.TaskLifecycle

class ExecutionStepsTest {
    private fun event(id: String, status: String) = JSONObject().put("id", id).put("action", "Tap")
        .put("target", "Equals button").put("status", status)

    @Test fun snapshotsUpdateOneRowAndCannotRegressTerminalStatus() {
        val entry = JSONObject()
        assertTrue(ExecutionSteps.update(entry, event("a", "running")))
        assertFalse(ExecutionSteps.update(entry, event("a", "running")))
        assertTrue(ExecutionSteps.update(entry, event("a", "finished")))
        assertFalse(ExecutionSteps.update(entry, event("a", "running")))
        val rows = ExecutionStep.from(entry.getJSONArray("executionSteps"))
        assertEquals(1, rows.size)
        assertEquals("finished", rows.single().status)
        assertEquals("Tap · Equals button", rows.single().label(false))
    }

    @Test fun stoppingPreservesCompletedStepsAndDoesNotClaimPendingStepSucceeded() {
        val entry = JSONObject()
        ExecutionSteps.update(entry, event("a", "finished"))
        ExecutionSteps.update(entry, event("b", "running"))
        TaskLifecycle.finish(entry, "stopped")
        assertEquals(listOf("finished", "interrupted"), ExecutionStep.from(entry.getJSONArray("executionSteps")).map { it.status })
    }

    @Test fun recoveryRetainsHistoryAndSettlesAnInterruptedStep() {
        val entry = JSONObject().put("id", "task").put("status", "running")
        ExecutionSteps.update(entry, event("a", "finished"))
        ExecutionSteps.update(entry, event("b", "running"))
        val state = JSONObject().put("history", org.json.JSONArray().put(entry))
        val restored = JSONObject(state.toString())
        TaskLifecycle.recover(restored)
        val recovered = restored.getJSONArray("history").getJSONObject(0)
        assertEquals("stopped", recovered.getString("status"))
        assertEquals(listOf("finished", "interrupted"), ExecutionStep.from(recovered.getJSONArray("executionSteps")).map { it.status })
    }

    @Test fun boundedHistoryKeepsOriginalStepNumbersAndRejectsInvalidEvents() {
        val entry = JSONObject()
        assertFalse(ExecutionSteps.update(entry, event("", "finished")))
        assertFalse(ExecutionSteps.update(entry, event("a", "pending")))
        repeat(ExecutionSteps.LIMIT + 2) { ExecutionSteps.update(entry, event("$it", "finished")) }
        val rows = ExecutionStep.from(entry.getJSONArray("executionSteps"))
        assertEquals(ExecutionSteps.LIMIT, rows.size)
        assertEquals(3, rows.first().number)
        assertEquals(ExecutionSteps.LIMIT + 2, rows.last().number)
    }
}
