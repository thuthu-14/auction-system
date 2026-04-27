package client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddBankDialogController {

    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;

    @FunctionalInterface
    public interface OnBankLinkedListener {
        void onBankLinked(String bankName, String accountNumber, double initialBalance);
    }

    private OnBankLinkedListener onBankLinkedListener;
    private Runnable onCloseCallback;

    public void setOnBankLinkedListener(OnBankLinkedListener listener) {
        this.onBankLinkedListener = listener;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void handleConfirmBankLink(ActionEvent event) {
        String bank = bankNameField != null ? bankNameField.getText().trim() : "";
        String account = accountNumberField != null ? accountNumberField.getText().trim() : "";

        if (bank.isEmpty() || account.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        if (onBankLinkedListener != null) {
            onBankLinkedListener.onBankLinked(bank, account, 0.0);
        }

        closePopup(event);
    }

    @FXML
    private void closePopup(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}