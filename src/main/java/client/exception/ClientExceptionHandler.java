package client.exception;

import client.util.DialogUtil;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import util.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.CompletionException;

public final class ClientExceptionHandler {
    private static volatile boolean installed;

    private ClientExceptionHandler() {
    }

    public static void installGlobalHandlers() {
        if (installed) {
            return;
        }
        installed = true;

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                handle("Uncaught exception in " + thread.getName(), throwable, true));
    }

    public static ClientException wrap(String context, Throwable throwable) {
        return wrap(null, context, throwable);
    }

    public static ClientException wrap(ClientErrorType forcedType, String context, Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof ClientException clientException) {
            return clientException;
        }

        ClientErrorType type = forcedType != null ? forcedType : classify(root);
        return new ClientException(type, buildUserMessage(type, root), root);
    }

    public static void handle(String context, Throwable throwable) {
        handle(context, throwable, false, null);
    }

    public static void handle(ClientErrorType type, String context, Throwable throwable) {
        handle(type, context, throwable, false, null);
    }

    public static void handle(String context, Throwable throwable, boolean notifyUser) {
        handle(context, throwable, notifyUser, null);
    }

    public static void handle(String context, Throwable throwable, boolean notifyUser, Node ownerNode) {
        handle(null, context, throwable, notifyUser, ownerNode);
    }

    public static void handle(ClientErrorType type, String context, Throwable throwable, boolean notifyUser, Node ownerNode) {
        ClientException clientException = wrap(type, context, throwable);
        LoggerUtil.error(context + ": " + clientException.getUserMessage());
        if (clientException.getCause() != null) {
            LoggerUtil.error("Cause: " + clientException.getCause().getClass().getSimpleName()
                    + " - " + safeMessage(clientException.getCause()));
        }

        if (notifyUser) {
            showError(clientException, ownerNode);
        }
    }

    public static void showError(Throwable throwable) {
        showError(wrap("Client error", throwable), null);
    }

    public static void showError(Throwable throwable, Node ownerNode) {
        showError(wrap("Client error", throwable), ownerNode);
    }

    public static void showError(ClientErrorType type, String context, Throwable throwable) {
        showError(wrap(type, context, throwable), null);
    }

    public static void showError(ClientErrorType type, String context, Throwable throwable, Node ownerNode) {
        showError(wrap(type, context, throwable), ownerNode);
    }

    public static void showError(ClientException exception, Node ownerNode) {
        Runnable action = () -> DialogUtil.showAlert(
                Alert.AlertType.ERROR,
                exception.getType().defaultTitle(),
                null,
                exception.getUserMessage(),
                ownerNode);

        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        if (throwable == null) {
            return new ClientException(ClientErrorType.UNKNOWN, "Da xay ra loi khong xac dinh.");
        }
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof RuntimeException)
                && current.getCause() != null
                && current.getClass() != ClientException.class) {
            current = current.getCause();
        }
        return current;
    }

    private static ClientErrorType classify(Throwable throwable) {
        if (throwable instanceof ConnectException
                || throwable instanceof SocketTimeoutException
                || throwable instanceof SocketException
                || throwable instanceof EOFException
                || throwable instanceof IOException) {
            return ClientErrorType.NETWORK;
        }
        if (throwable instanceof SQLException) {
            return ClientErrorType.DATA;
        }
        if (throwable instanceof IllegalArgumentException) {
            return ClientErrorType.VALIDATION;
        }
        if (throwable instanceof IllegalStateException) {
            return ClientErrorType.UI;
        }
        return ClientErrorType.UNKNOWN;
    }

    private static String buildUserMessage(ClientErrorType type, Throwable throwable) {
        String message = safeMessage(throwable);
        if (message != null && !message.isBlank()) {
            return message;
        }

        return switch (type) {
            case NETWORK -> "Khong the ket noi toi server. Vui long kiem tra lai ket noi.";
            case VALIDATION -> "Du lieu nhap vao khong hop le.";
            case NAVIGATION -> "Khong the mo man hinh duoc yeu cau.";
            case AUTHENTICATION -> "Khong the xac thuc tai khoan.";
            case DATA -> "Khong the doc hoac luu du lieu.";
            case UI -> "Giao dien chua san sang de thuc hien thao tac.";
            default -> "Da xay ra loi he thong. Vui long thu lai.";
        };
    }

    private static String safeMessage(Throwable throwable) {
        return throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "";
    }
}
