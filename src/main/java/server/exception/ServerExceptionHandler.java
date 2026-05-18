package server.exception;

import util.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;

public final class ServerExceptionHandler {
    private static volatile boolean installed;

    private ServerExceptionHandler() {
    }

    public static void installGlobalHandlers() {
        if (installed) {
            return;
        }
        installed = true;

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                handle("Uncaught server exception in " + thread.getName(), throwable));
    }

    public static ServerException wrap(String context, Throwable throwable) {
        return wrap(null, context, throwable);
    }

    public static ServerException wrap(ServerErrorType forcedType, String context, Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof ServerException serverException) {
            return serverException;
        }

        ServerErrorType type = forcedType != null ? forcedType : classify(root);
        return new ServerException(type, buildClientMessage(type, root), root);
    }

    public static void handle(String context, Throwable throwable) {
        ServerException exception = wrap(context, throwable);
        Throwable cause = exception.getCause();
        LoggerUtil.error(context + ": " + exception.getClientMessage());
        if (cause != null) {
            LoggerUtil.error("Cause: " + cause.getClass().getSimpleName() + " - " + safeMessage(cause));
        }
    }

    public static void handle(ServerErrorType type, String context, Throwable throwable) {
        handle(context, wrap(type, context, throwable));
    }

    public static String clientMessage(Throwable throwable) {
        return wrap("Server error", throwable).getClientMessage();
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable == null) {
            return new ServerException(ServerErrorType.SYSTEM, "Da xay ra loi server.");
        }

        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof RuntimeException)
                && current.getCause() != null
                && current.getClass() != ServerException.class) {
            current = current.getCause();
        }
        return current;
    }

    private static ServerErrorType classify(Throwable throwable) {
        if (throwable instanceof AuthenticationException) {
            return ServerErrorType.AUTHENTICATION;
        }
        if (throwable instanceof PermissionDeniedException) {
            return ServerErrorType.PERMISSION;
        }
        if (throwable instanceof InvalidBidException
                || throwable instanceof AuctionClosedException
                || throwable instanceof InsufficientFundsException) {
            return ServerErrorType.BID;
        }
        if (throwable instanceof IllegalArgumentException) {
            return ServerErrorType.VALIDATION;
        }
        if (throwable instanceof DataAccessException || throwable instanceof SQLException) {
            return ServerErrorType.DATA;
        }
        if (throwable instanceof EOFException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof SocketException
                || throwable instanceof IOException) {
            return ServerErrorType.NETWORK;
        }
        return ServerErrorType.SYSTEM;
    }

    private static String buildClientMessage(ServerErrorType type, Throwable throwable) {
        String message = safeMessage(throwable);
        if (message != null && !message.isBlank()) {
            return message;
        }

        return switch (type) {
            case AUTHENTICATION -> "Thong tin dang nhap khong hop le.";
            case PERMISSION -> "Ban khong co quyen thuc hien thao tac nay.";
            case VALIDATION -> "Du lieu gui len khong hop le.";
            case BID -> "Khong the dat gia voi thong tin hien tai.";
            case DATA -> "Khong the doc hoac luu du lieu.";
            case NETWORK -> "Ket noi toi server bi gian doan.";
            default -> "Da xay ra loi server. Vui long thu lai.";
        };
    }

    private static String safeMessage(Throwable throwable) {
        return throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "";
    }
}
