package client.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Window;

public final class DialogUtil {
    private DialogUtil() {
    }

    public static void showAlert(Alert.AlertType type, String title, String content) {
        showAlert(type, title, null, content, null);
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content) {
        showAlert(type, title, header, content, null);
    }

    public static void showAlert(Alert.AlertType type, String title, String header, String content, Node ownerNode) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        prepareDialog(alert, ownerNode);
        alert.showAndWait();
    }

    public static void prepareDialog(Dialog<?> dialog, Node ownerNode) {
        Window owner = resolveOwner(ownerNode);
        if (owner != null && dialog.getOwner() == null) {
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        dialog.setOnShown(event -> Platform.runLater(() -> centerDialog(dialog, owner)));
    }

    public static void centerDialog(Dialog<?> dialog, Node ownerNode) {
        centerDialog(dialog, resolveOwner(ownerNode));
    }

    private static Window resolveOwner(Node ownerNode) {
        if (ownerNode != null && ownerNode.getScene() != null) {
            Window window = ownerNode.getScene().getWindow();
            if (window != null && window.isShowing()) {
                return window;
            }
        }

        return Window.getWindows().stream()
                .filter(Window::isShowing)
                .filter(Window::isFocused)
                .findFirst()
                .orElseGet(() -> Window.getWindows().stream()
                        .filter(Window::isShowing)
                        .findFirst()
                        .orElse(null));
    }

    private static void centerDialog(Dialog<?> dialog, Window owner) {
        if (dialog.getDialogPane().getScene() == null) {
            return;
        }

        Window dialogWindow = dialog.getDialogPane().getScene().getWindow();
        if (dialogWindow == null) {
            return;
        }

        Rectangle2D bounds = Screen.getScreensForRectangle(
                        dialogWindow.getX(),
                        dialogWindow.getY(),
                        Math.max(dialogWindow.getWidth(), 1),
                        Math.max(dialogWindow.getHeight(), 1))
                .stream()
                .findFirst()
                .orElse(Screen.getPrimary())
                .getVisualBounds();

        double x;
        double y;
        if (owner != null && owner.isShowing()) {
            x = owner.getX() + (owner.getWidth() - dialogWindow.getWidth()) / 2;
            y = owner.getY() + (owner.getHeight() - dialogWindow.getHeight()) / 2;
        } else {
            x = bounds.getMinX() + (bounds.getWidth() - dialogWindow.getWidth()) / 2;
            y = bounds.getMinY() + (bounds.getHeight() - dialogWindow.getHeight()) / 2;
        }

        dialogWindow.setX(clamp(x, bounds.getMinX(), bounds.getMaxX() - dialogWindow.getWidth()));
        dialogWindow.setY(clamp(y, bounds.getMinY(), bounds.getMaxY() - dialogWindow.getHeight()));
    }

    private static double clamp(double value, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
