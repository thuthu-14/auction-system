package client.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * AlertUtil - Utility cho alert operations
 */
public class AlertUtil {

    /**
     * Hiển thị success alert
     */
    public static void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText("✓ " + message);
            alert.showAndWait();
        });
    }

    /**
     * Hiển thị error alert
     */
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText("❌ " + message);
            alert.showAndWait();
        });
    }

    /**
     * Hiển thị warning alert
     */
    public static void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText("⚠ " + message);
            alert.showAndWait();
        });
    }
}
