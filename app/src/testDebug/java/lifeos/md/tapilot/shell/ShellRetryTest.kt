package lifeos.md.tapilot.shell

import org.junit.Assert.*
import org.junit.Test

class ShellRetryTest {
    @Test fun prolongedFailureBacksOffAndRecoveryResetsDelay() {
        val retry = ShellRetry()
        assertEquals(listOf(2000L, 5000L, 15000L, 30000L, 60000L), (1..5).map { retry.nextDelay() })
        repeat(1000) { assertEquals(60000L, retry.nextDelay()) }
        retry.reset()
        assertEquals(2000L, retry.nextDelay())
    }
}
