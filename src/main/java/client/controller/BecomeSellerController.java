package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import navigation.NavigationManager;
import server.model.User;
import server.model.RegularUser;
import util.JsonUtil;

import java.util.HashMap;
import java.util.Map;

public class BecomeSellerController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;

    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button btnTogglePassword;

    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private User currentUser;
    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        btnSave.setOnAction(event -> handleSave());
        btnCancel.setOnAction(event -> handleCancel());

        btnTogglePassword.setVisible(false);
        passwordField.textProperty().addListener((observable, oldValue, newValue) ->
                btnTogglePassword.setVisible(!newValue.isEmpty())
        );

        passwordTextField.textProperty().addListener((observable, oldValue, newValue) ->
                btnTogglePassword.setVisible(!newValue.isEmpty())
        );
    }

    private record SellerData(String name, String email, String phone, String address, String password) {}

    private SellerData validateInput() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String password = getPassword();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đầy đủ tất cả các thông tin!");
            return null;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đúng định dạng email!");
            return null;
        }
        if (!phone.matches("\\d{10,11}")) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đúng SDT");
            return null;
        }
        return new SellerData(name, email, phone, address, password);
    }

    @FXML
    private void handleSave() {
        if (this.currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được thông tin user hiện tại!");
            return;
        }

        if (!(currentUser instanceof RegularUser)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Chỉ người dùng thường mới có thể nâng cấp!");
            return;
        }

        RegularUser regularUser = (RegularUser) currentUser;

        if (regularUser.isSeller()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Bạn đã là Người bán rồi! Không cần nâng cấp lại.");
            NavigationManager.getInstance().goToHome();
            return;
        }

        SellerData data = validateInput();
        if (data != null) {
            btnSave.setDisable(true);
            btnSave.setText("Đang xử lý...");

            new Thread(() -> {
                try {
                    ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
                    if (socket == null) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Chưa kết nối tới Server!"));
                        return;
                    }

                    // 1. Đóng gói dữ liệu gửi Server
                    Map<String, String> payload = new HashMap<>();
                    payload.put("userId", currentUser.getUserId());
                    payload.put("shopName", data.name());
                    payload.put("phone", data.phone());
                    payload.put("address", data.address());
                    payload.put("email", data.email());

                    // 2. Gửi lệnh UPGRADE_SELLER
                    Message request = new Message(MessageType.UPGRADE_SELLER, payload, currentUser.getUsername());

                    // 3. Chờ phản hồi
                    Message response = socket.sendAndReceive(request);

                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.getStatus())) {

                            // Cập nhật User cục bộ
                            regularUser.upgradeSeller(data.name(), data.phone(), data.address(), data.email());

                            // Lưu JSON cục bộ cho Client
                            try {
                                Map<String, String> sellerIdMap = new HashMap<>();
                                sellerIdMap.put("sellerId", regularUser.getUserId());
                                sellerIdMap.put("sellerName", regularUser.getShopName());
                                sellerIdMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
                                JsonUtil.saveToJson("data/json/current_seller_id.json", sellerIdMap);
                            } catch (Exception ignored) {}

                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chúc mừng! Bạn đã trở thành Người bán!");

                            NavigationManager.getInstance().setMainStage((javafx.stage.Stage) nameField.getScene().getWindow());
                            NavigationManager.getInstance().goToSellerHome();
                        } else {
                            String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                            showAlert(Alert.AlertType.ERROR, "Lỗi Server", errorMsg);
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới Server."));
                } finally {
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Đăng ký");
                    });
                }
            }).start();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return this.currentUser;
    }

    @FXML
    private void handleCancel() {
        NavigationManager.getInstance().goToHome();
    }

    @FXML
    public void togglePasswordVisibility(ActionEvent event) {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            passwordTextField.setText(passwordField.getText());
            showPassword(true);
            btnTogglePassword.setText("🔒");
        } else {
            passwordField.setText(passwordTextField.getText());
            showPassword(false);
            btnTogglePassword.setText("👁");
        }
    }

    private void showPassword(boolean show) {
        passwordTextField.setVisible(show);
        passwordTextField.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
    }

    private String getPassword() {
        return isPasswordVisible ? passwordTextField.getText() : passwordField.getText();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        client.util.DialogUtil.showAlert(alertType, title, null, message, nameField);
    }
}
