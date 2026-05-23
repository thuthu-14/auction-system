package client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class WalletTransactionDialogController {

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label amountLabel;
    @FXML private TextField amountField;
    @FXML private Button confirmButton;

    @FunctionalInterface
    public interface OnTransactionConfirmListener {
        void onConfirm(String amount);
    }

    private OnTransactionConfirmListener onConfirmListener;
    private Runnable onCloseCallback;

    public void setTransactionData(String type, String walletBalanceText) {
        boolean deposit = "DEPOSIT".equals(type);
        titleLabel.setText(deposit ? "Nạp tiền" : "Rút tiền");
        subtitleLabel.setText(deposit
                ? "Nhập số tiền muốn nạp vào ví của bạn"
                : "Nhập số tiền muốn rút về tài khoản ngân hàng");
        amountLabel.setText(deposit ? "Số tiền nạp" : "Số tiền rút");
        amountField.setPromptText(deposit ? "VD: 1.000.000" : "VD: 500.000");
        confirmButton.setText(deposit ? "Xác nhận nạp tiền" : "Xác nhận rút tiền");

        if (!deposit && walletBalanceText != null && !walletBalanceText.isBlank()) {
            subtitleLabel.setText("Số dư hiện có: " + walletBalanceText);
        }
    }

    public void setOnConfirmListener(OnTransactionConfirmListener listener) {
        this.onConfirmListener = listener;
    }

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        String amount = amountField != null ? amountField.getText().trim() : "";
        if (amount.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng nhập số tiền.");
            return;
        }

        if (onConfirmListener == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể thực hiện giao dịch lúc này.");
            return;
        }

        onConfirmListener.onConfirm(amount);
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
        client.util.DialogUtil.showAlert(type, title, null, message, amountField);
    }
}
