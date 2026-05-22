package server.exception;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class ServerExceptionHandlerTest {

    @Test
    void wrapReturnsExistingServerException() {
        ServerException original = new ServerException(ServerErrorType.DATA, "data failed");

        assertSame(original, ServerExceptionHandler.wrap("context", original));
    }

    @Test
    void wrapClassifiesKnownExceptionTypes() {
        assertEquals(ServerErrorType.AUTHENTICATION,
                ServerExceptionHandler.wrap("ctx", new AuthenticationException("bad login")).getType());
        assertEquals(ServerErrorType.PERMISSION,
                ServerExceptionHandler.wrap("ctx", new PermissionDeniedException("denied")).getType());
        assertEquals(ServerErrorType.BID,
                ServerExceptionHandler.wrap("ctx", new InvalidBidException("bad bid")).getType());
        assertEquals(ServerErrorType.BID,
                ServerExceptionHandler.wrap("ctx", new AuctionClosedException("closed")).getType());
        assertEquals(ServerErrorType.BID,
                ServerExceptionHandler.wrap("ctx", new InsufficientFundsException("funds")).getType());
        assertEquals(ServerErrorType.VALIDATION,
                ServerExceptionHandler.wrap("ctx", new IllegalArgumentException("invalid")).getType());
        assertEquals(ServerErrorType.DATA,
                ServerExceptionHandler.wrap("ctx", new SQLException("sql")).getType());
        assertEquals(ServerErrorType.DATA,
                ServerExceptionHandler.wrap("ctx", new DataAccessException("data")).getType());
        assertEquals(ServerErrorType.NETWORK,
                ServerExceptionHandler.wrap("ctx", new EOFException("eof")).getType());
        assertEquals(ServerErrorType.NETWORK,
                ServerExceptionHandler.wrap("ctx", new SocketTimeoutException("timeout")).getType());
        assertEquals(ServerErrorType.NETWORK,
                ServerExceptionHandler.wrap("ctx", new IOException("io")).getType());
        assertEquals(ServerErrorType.SYSTEM,
                ServerExceptionHandler.wrap("ctx", new RuntimeException()).getType());
    }

    @Test
    void wrapUnwrapsRuntimeAndCompletionExceptions() {
        ServerException wrapped = ServerExceptionHandler.wrap("ctx",
                new CompletionException(new RuntimeException(new PermissionDeniedException("denied"))));

        assertEquals(ServerErrorType.PERMISSION, wrapped.getType());
        assertEquals("denied", wrapped.getClientMessage());
    }

    @Test
    void forcedTypeOverridesClassificationAndDefaultMessageIsUsedWhenCauseHasNoMessage() {
        ServerException exception = ServerExceptionHandler.wrap(
                ServerErrorType.NETWORK, "ctx", new IllegalArgumentException());

        assertEquals(ServerErrorType.NETWORK, exception.getType());
        assertEquals("Kết nối tới server bị gián đoạn.", exception.getClientMessage());
    }

    @Test
    void clientMessageHandlesNullThrowable() {
        assertEquals("Đã xảy ra lỗi server.", ServerExceptionHandler.clientMessage(null));
    }

    @Test
    void installGlobalHandlersIsIdempotent() {
        ServerExceptionHandler.installGlobalHandlers();
        Thread.UncaughtExceptionHandler first = Thread.getDefaultUncaughtExceptionHandler();
        ServerExceptionHandler.installGlobalHandlers();

        assertSame(first, Thread.getDefaultUncaughtExceptionHandler());
    }
}
