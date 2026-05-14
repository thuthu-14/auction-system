package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuthClientService;
import client.util.ResponsiveSceneUtil;
import client.util.StageUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import server.model.User;
import util.LoggerUtil;
import util.ValidationUtil;

import java.io.IOException;

/**
 * SignupController - Xử lý màn hình Đăng ký qua Server Socket
 */
public class SignupController {

    @FXML private TextField Ho;
    @FXML private TextField Ten;
    @FXML private TextField Email;
    @FXML private PasswordField mk;
    @FXML private Button loginButton;

    private ConnectionManager connectionManager;
    private ClientSocket clientSocket;
    private User currentUser;
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    public void initialize() {
        LoggerUtil.info("? SignupController initialized");
    }

    @FXML
    private void handleSignup() {
        String ho = Ho.getText().trim();
        String ten = Ten.getText().trim();
        String email = Email.getText().trim();
        String password = mk.getText();

        // 1. Validation co bán
        if (ho.isEmpty() || ten.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thiếu thông tin", "Vui lòng nhập đầy đủ Họ, Tên, Email và Mật khẩu.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Email không hợp lệ", "Vui lòng nhập email đúng định dạng.");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu yếu", "Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        String username = buildUsername(ho, ten, email);

        if (!ValidationUtil.isValidUsername(username)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên đăng nhập lỗi", "Không thể tạo tên đăng nhập. Vui lòng thử email khác.");
            return;
        }

        loginButton.setDisable(true);

        // 2. Giao tiếp với Server ở luồng riêng
        new Thread(() -> {
            try {
                clientSocket = (ClientSocket) authClientService.ensureConnected(clientSocket);
                currentUser = authClientService.register(clientSocket, username, password, email);

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION,
                            "Đăng ký thành công",
                            "Tạo tài khoản thành công!",
                            "Tên đăng nhập của bạn là: " + username + "\nBạn có thể dùng tên này hoặc email để đăng nhập.");
                    switchToLogin();
                });
            } catch (Exception e) {
                LoggerUtil.error("Signup error: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Sự cố kết nối", e.getMessage());
            } finally {
                Platform.runLater(() -> loginButton.setDisable(false));
            }
        }).start();
    }

    @FXML
    private void switchToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = ResponsiveSceneUtil.createScaledScene(loader.load());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
            StageUtil.showMaximized(stage);

            LoggerUtil.info("Switched back to login screen");
        } catch (IOException e) {
            LoggerUtil.error("Error switching to login screen: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi UI", "Không mở được màn hình đăng nhập", e.getMessage());
        }
    }

    private String buildUsername(String ho, String ten, String email) {
        // Ưu tiên tạo username từ họ+tên, loại bỏ ký tự đặc biệt
        String base = (ho + ten).replaceAll("[^a-zA-Z0-9_]", "");

        // Nếu chuỗi quá ngắn, lấy phần trước @ của email
        if (base.length() < 3) {
            int atIndex = email.indexOf('@');
            String emailPrefix = atIndex > 0 ? email.substring(0, atIndex) : "user";
            base = emailPrefix.replaceAll("[^a-zA-Z0-9_]", "");
        }

        // Nếu vẫn quá ngắn, dùng timestamp
        if (base.length() < 3) {
            base = "user" + System.currentTimeMillis();
        }

        // Giới hạn độ dài tối đa 50 ký tự
        if (base.length() > 50) {
            base = base.substring(0, 50);
        }

        return base;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            client.util.DialogUtil.showAlert(type, title, header, content);
        });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public ClientSocket getClientSocket() {
        return clientSocket;
    }
}
