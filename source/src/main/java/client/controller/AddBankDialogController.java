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
    @FXML private TextField initialBalanceField;

    @FunctionalInterface
    public interface OnBankLinkedListener {
        void onBankLinked(String bankName, String accountNumber, double initialBalance);
    }

    private OnBankLinkedListener onBankLinkedListener;

    public void setOnBankLinkedListener(OnBankLinkedListener listener) {
        this.onBankLinkedListener = listener;
    }

    @FXML
    private void handleConfirmBankLink(ActionEvent event) {
        String bank = bankNameField != null ? bankNameField.getText().trim() : "";
        String account = accountNumberField != null ? accountNumberField.getText().trim() : "";
        String initial = initialBalanceField != null ? initialBalanceField.getText().trim() : "";

        if (bank.isEmpty() || account.isEmpty() || initial.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập đầy đủ thông tin.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(initial.replace(",", "").trim());
            if (amount < 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số dư ban đầu phải >= 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số dư ban đầu không hợp lệ.");
            return;
        }

        if (onBankLinkedListener != null) {
            onBankLinkedListener.onBankLinked(bank, account, amount);
        }

        closePopup(event);
    }

    @FXML
    private void closePopup(ActionEvent event) {
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
