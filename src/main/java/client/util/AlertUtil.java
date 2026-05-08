package client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public class AlertUtil {
    public static void showSuccess(String title, String message) {
        Platform.runLater(() -> DialogUtil.showAlert(Alert.AlertType.INFORMATION, title, "[OK] " + message));
    }

    public static void showError(String title, String message) {
        Platform.runLater(() -> DialogUtil.showAlert(Alert.AlertType.ERROR, title, "[ERROR] " + message));
    }

    public static void showWarning(String title, String message) {
        Platform.runLater(() -> DialogUtil.showAlert(Alert.AlertType.WARNING, title, "[WARN] " + message));
    }
}
