package lifeos.md.tapilot.shell

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import org.junit.Assert.*
import org.junit.Test

class PairingDeadlineTest {
    @Test fun forwardsBothDirectionsAndClosesAStalledHandshake() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 3000
            PairingDeadline(server.localPort, 1000).use { bridge ->
                Socket("127.0.0.1", bridge.port).use { client ->
                    client.soTimeout = 3000
                    server.accept().use { target ->
                        target.soTimeout = 3000
                        client.getOutputStream().write(42)
                        assertEquals(42, target.getInputStream().read())
                        target.getOutputStream().write(24)
                        assertEquals(24, client.getInputStream().read())
                        assertEquals(-1, client.getInputStream().read())
                    }
                }
            }
        }
    }
}
