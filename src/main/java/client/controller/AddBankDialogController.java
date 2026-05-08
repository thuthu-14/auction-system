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
    @FXML private TextField initialBalanceField;

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
        // Tránh lỗi NullPointerException nếu FXML không map id chính xác
        String bank = bankNameField != null ? bankNameField.getText().trim() : "";
        String account = accountNumberField != null ? accountNumberField.getText().trim() : "";

        if (bank.isEmpty() || account.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập đầy đủ thông tin ngân hàng.");
            return;
        }

        // Tặng sẵn 10.000.000đ khi thêm ngân hàng mới để test
        double initialBalance = 10000000.0;

        if (onBankLinkedListener != null) {
            onBankLinkedListener.onBankLinked(bank, account, initialBalance);
        }

        closePopup(event);
    }

    @FXML
    private void closePopup(ActionEvent event) {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
        if (event.getSource() instanceof Node) {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        client.util.DialogUtil.showAlert(type, title, null, message, bankNameField);
    }
}
