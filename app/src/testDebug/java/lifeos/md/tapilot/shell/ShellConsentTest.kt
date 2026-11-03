package lifeos.md.tapilot.shell

import org.junit.Assert.*
import org.junit.Test

class ShellConsentTest {
    @Test fun rejectsUnrequestedWrongIdentityAndWrongToken() {
        val consent = ShellConsent()
        assertFalse(consent.consume("token", 2000, 0))
        consent.arm("token", 10)
        assertFalse(consent.consume("token", 10001, 11))
        assertFalse(consent.consume("wrong", 2000, 11))
        assertTrue(consent.consume("token", 2000, 11))
        assertFalse(consent.consume("token", 2000, 12))
    }
    @Test fun expiredAndRevokedConsentCannotConnect() {
        val consent = ShellConsent()
        consent.arm("token", 10)
        assertFalse(consent.consume("token", 2000, 300010))
        consent.arm("token", 400000)
        consent.clear()
        assertFalse(consent.consume("token", 2000, 400001))
    }
}
