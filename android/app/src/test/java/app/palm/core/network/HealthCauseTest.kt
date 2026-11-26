package app.palm.core.network

import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthCauseTest {
    @Test fun refused() = assertEquals(
        "Connection refused: run tools/dev-reverse.sh and tools/run-backend.sh",
        causeOf(ConnectException("Failed to connect to /127.0.0.1:8000")),
    )

    @Test fun timeout() = assertEquals("Timed out: is tools/run-backend.sh running?", causeOf(SocketTimeoutException("timeout")))

    @Test fun cleartext() = assertEquals(
        "Plain HTTP blocked: debug network config missing",
        causeOf(UnknownServiceException("CLEARTEXT communication to 127.0.0.1 not permitted by network security policy")),
    )

    @Test fun unknownServiceWithoutCleartextFallsBack() =
        assertEquals("Network error: UnknownServiceException", causeOf(UnknownServiceException("other")))

    @Test fun unknownHost() = assertEquals("Host not found", causeOf(UnknownHostException("api.invalid")))

    // OkHttp's shape when the adb reverse rule exists but nothing listens on the PC
    @Test fun endOfStream() = assertEquals(
        "No response: start tools/run-backend.sh",
        causeOf(IOException("unexpected end of stream on http://127.0.0.1:8000/...", EOFException("\\n not found: limit=0"))),
    )

    @Test fun bareEof() = assertEquals("No response: start tools/run-backend.sh", causeOf(EOFException()))

    @Test fun reset() = assertEquals("No response: start tools/run-backend.sh", causeOf(SocketException("Connection reset")))

    @Test fun otherIo() = assertEquals("Network error: IOException", causeOf(IOException("boom")))
}
