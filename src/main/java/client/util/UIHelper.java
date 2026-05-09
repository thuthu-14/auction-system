package client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class UIHelper {
    public static void showInfo(String title, String header, String content) {
        DialogUtil.showAlert(Alert.AlertType.INFORMATION, title, header, content);
    }

    public static void showError(String title, String header, String content) {
        DialogUtil.showAlert(Alert.AlertType.ERROR, title, header, content);
    }

    public static void showWarning(String title, String header, String content) {
        DialogUtil.showAlert(Alert.AlertType.WARNING, title, header, content);
    }

    public static boolean showConfirmation(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        DialogUtil.prepareDialog(alert, null);
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static Optional<String> showInputDialog(String title, String header, String label) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(label);
        DialogUtil.prepareDialog(dialog, null);
        return dialog.showAndWait();
    }
}
