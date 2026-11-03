package lifeos.md.tapilot.shell

import org.junit.Assert.*
import org.junit.Test

class LocalAdbInputTest {
    @Test fun validatesPortsWithoutAcceptingRemoteHostsOrCommands() {
        assertEquals(37123, LocalAdbInput.port(" 37123 "))
        for (value in listOf("", "0", "65536", "-1", "host:1234", "1234;id")) {
            assertThrows(IllegalArgumentException::class.java) { LocalAdbInput.port(value) }
        }
    }
    @Test fun preservesLeadingZerosAndRejectsMalformedPairingCodes() {
        assertEquals("001234", LocalAdbInput.code("001234"))
        for (value in listOf("12345", "1234567", "abcdef", "123 45")) {
            assertThrows(IllegalArgumentException::class.java) { LocalAdbInput.code(value) }
        }
    }
    @Test fun quotesBootstrapPathsAsSingleShellArguments() {
        assertEquals("'a'\\''b'", LocalAdbInput.quote("a'b"))
        assertEquals("'/path/with space/$(id)'", LocalAdbInput.quote("/path/with space/$(id)"))
    }
    @Test fun storedPairingDoesNotExtendOrReplaceConsent() {
        val consent = ShellConsent()
        assertNull(consent.pending(0))
        consent.arm("token", 0)
        assertEquals("token", consent.pending(299999))
        assertNull(consent.pending(300000))
        consent.clear()
        assertNull(consent.pending(1))
    }
}
