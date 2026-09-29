package ua.kyiv.alertwidget

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class RequestFailureClassifierTest {
    @Test fun serverFailureIsDifferentFromClientFailure() {
        assertEquals(FailureKind.HTTP_SERVER, RequestFailureClassifier.fromHttpCode(503))
        assertEquals(FailureKind.HTTP_CLIENT, RequestFailureClassifier.fromHttpCode(429))
    }

    @Test fun timeoutAndDnsHaveSeparateReasons() {
        assertEquals(
            FailureKind.TIMEOUT,
            RequestFailureClassifier.fromException(SocketTimeoutException()),
        )
        assertEquals(
            FailureKind.DNS,
            RequestFailureClassifier.fromException(UnknownHostException()),
        )
    }

    @Test fun connectionTlsAndInvalidResponseStayDistinct() {
        assertEquals(
            FailureKind.CONNECTION,
            RequestFailureClassifier.fromException(ConnectException()),
        )
        assertEquals(
            FailureKind.TLS,
            RequestFailureClassifier.fromException(SSLHandshakeException("handshake failed")),
        )
        assertEquals(
            FailureKind.INVALID_RESPONSE,
            RequestFailureClassifier.fromException(IllegalArgumentException()),
        )
    }
}
