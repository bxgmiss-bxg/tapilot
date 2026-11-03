package lifeos.md.tapilot.bridge

import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NodeTestRunCoordinatorTest {
    @Test
    fun `startup failure retains its cause instead of requiring a report`() {
        val error = nodeRunIncompleteError(1, "Cannot find package '@midscene/test'", listOf("Error: secondary failure"))
        assertTrue(error.message!!.contains("Cannot find package '@midscene/test'"))
        assertTrue(error.message!!.contains("exit code 1"))
        assertFalse(error.message!!.contains("secondary failure"))
    }

    @Test
    fun `native crash retains exit code and linker diagnostic`() {
        val error = nodeRunIncompleteError(134, null, listOf(
            "CANNOT LINK EXECUTABLE: library libexample.so not found", "stack frame 1",
        ))
        assertTrue(error.message!!.contains("exit code 134"))
        assertTrue(error.message!!.contains("library libexample.so not found"))
    }

    @Test
    fun `silent exit including zero is not treated as task success`() {
        for (code in listOf(0, 23, 137)) {
            val error = nodeRunIncompleteError(code, null, listOf("private task input"))
            assertTrue(error.message!!.contains("exit code $code"))
            assertFalse(error.message!!.contains("private task input"))
        }
    }

    @Test
    fun `diagnostics redact configured credentials and bound output`() {
        val error = nodeRunIncompleteError(1, "Invalid key private-key " + "x".repeat(3000), emptyList(),
            mapOf("MIDSCENE_MODEL_API_KEY" to "private-key"))
        assertFalse(error.message!!.contains("private-key"))
        assertTrue(error.message!!.contains("[REDACTED]"))
        assertTrue(error.message!!.length <= 2000)
    }

    @Test
    fun `process failure after user stop is reported as stopped`() {
        val failure = nodeRunFailure(IllegalStateException("read interrupted"), true)

        assertTrue(failure is NodeTestRunCoordinator.NodeRunStoppedException)
    }

    @Test
    fun `process failure without user stop remains unchanged`() {
        val original = IllegalStateException("read interrupted")

        assertSame(original, nodeRunFailure(original, false))
    }
}
