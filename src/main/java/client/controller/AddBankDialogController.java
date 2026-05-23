package client.controller;

import client.network.ClientSocket;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.model.User;

public class AddBankDialogController {

    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;

    @FunctionalInterface
    public interface OnBankLinkedListener {
        void onBankLinked(String bankName, String accountNumber, double initialBalance);
    }

    private OnBankLinkedListener onBankLinkedListener;
    private Runnable onCloseCallback;

    private User currentUser;
    private ClientSocket clientSocket;

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

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
        account = account.replaceAll("[\\s-]", "");

        if (bank.isEmpty() || account.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập đầy đủ thông tin ngân hàng.");
            return;
        }

        if (!account.matches("\\d{6,20}")) {
            showAlert(Alert.AlertType.WARNING, "Số tài khoản không hợp lệ", "Số tài khoản chỉ gồm 6-20 chữ số.");
            return;
        }

        if (onBankLinkedListener == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể liên kết ngân hàng lúc này.");
            return;
        }

        onBankLinkedListener.onBankLinked(bank, account, 10_000_000.0);
        closePopup(event);
    }

    @FXML
    private void closePopup(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (event.getSource() instanceof Node node) {
            Stage stage = (Stage) node.getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        client.util.DialogUtil.showAlert(type, title, null, message, bankNameField);
    }
}
