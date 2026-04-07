// src/main/java/client/controller/LoginController.java
package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.network.MessageHandler;
import common.*;
import server.model.User;
import util.LoggerUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LoginController - Xử lý màn hình Login
 * MVC Pattern: Controller
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private ProgressIndicator progressIndicator;

    private ConnectionManager connectionManager;
    private ClientSocket clientSocket;
    private User currentUser;
    private Runnable onLoginSuccess;

    /**
     * Initialize controller
     */
    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        messageLabel.setText("");

        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());

        LoggerUtil.info("✓ LoginController initialized");
    }

    /**
     * Set callback khi login thành công
     */
    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    /**
     * Xử lý login
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("❌ Username và password không được rỗng!");
            return;
        }

        // Disable buttons
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        progressIndicator.setVisible(true);

        // Chạy trên thread riêng
        new Thread(() -> {
            try {
                // Kết nối tới server
                connectionManager = ConnectionManager.getInstance();
                if (!connectionManager.isConnected()) {
                    if (!connectionManager.connect()) {
                        showError("❌ Không thể kết nối tới server!");
                        return;
                    }
                }

                clientSocket = connectionManager.getClientSocket();

                // Tạo login request
                LoginRequest loginRequest = new LoginRequest(username, password);
                Message message = new Message(MessageType.LOGIN, loginRequest, username);

                // Gửi tới server
                clientSocket.sendMessage(message);

                // Chờ response
                Message response = clientSocket.receiveMessage();

                if (response != null && response.getStatus().equals("SUCCESS")) {
                    currentUser = (User) response.getData();
                    LoggerUtil.info("✓ Login successful: " + currentUser.getUsername());

                    Platform.runLater(() -> {
                        showSuccess("✓ Đăng nhập thành công!");
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                    });
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                    showError("❌ " + errorMsg);
                }

            } catch (IOException e) {
                LoggerUtil.error("Login IO error: " + e.getMessage());
                showError("❌ Lỗi kết nối: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                LoggerUtil.error("Login ClassNotFoundException: " + e.getMessage());
                showError("❌ Lỗi dữ liệu: " + e.getMessage());
            } finally {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    registerButton.setDisable(false);
                    progressIndicator.setVisible(false);
                });
            }
        }).start();
    }

    /**
     * Xử lý register
     */
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("❌ Username và password không được rỗng!");
            return;
        }

        if (username.length() < 3) {
            showError("❌ Username phải có ít nhất 3 ký tự!");
            return;
        }

        if (password.length() < 6) {
            showError("❌ Password phải có ít nhất 6 ký tự!");
            return;
        }

        // Disable buttons
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        progressIndicator.setVisible(true);

        // Chạy trên thread riêng
        new Thread(() -> {
            try {
                // Kết nối tới server
                connectionManager = ConnectionManager.getInstance();
                if (!connectionManager.isConnected()) {
                    if (!connectionManager.connect()) {
                        showError("❌ Không thể kết nối tới server!");
                        return;
                    }
                }

                clientSocket = connectionManager.getClientSocket();

                // Tạo register request
                Map<String, String> registerData = new HashMap<>();
                registerData.put("username", username);
                registerData.put("password", password);
                registerData.put("email", username + "@auction.com");

                Message message = new Message(MessageType.REGISTER, registerData, username);

                // Gửi tới server
                clientSocket.sendMessage(message);

                // Chờ response
                Message response = clientSocket.receiveMessage();

                if (response != null && response.getStatus().equals("SUCCESS")) {
                    currentUser = (User) response.getData();
                    LoggerUtil.info("✓ Register successful: " + currentUser.getUsername());

                    Platform.runLater(() -> {
                        showSuccess("✓ Đăng ký thành công! Đang chuyển hướng...");
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                    });
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                    showError("❌ " + errorMsg);
                }

            } catch (IOException e) {
                LoggerUtil.error("Register IO error: " + e.getMessage());
                showError("❌ Lỗi kết nối: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                LoggerUtil.error("Register ClassNotFoundException: " + e.getMessage());
                showError("❌ Lỗi dữ liệu: " + e.getMessage());
            } finally {
                Platform.runLater(() -> {
                    loginButton.setDisable(false);
                    registerButton.setDisable(false);
                    progressIndicator.setVisible(false);
                });
            }
        }).start();
    }

    /**
     * Hiển thị error message
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(message);
        });
    }

    /**
     * Hiển thị success message
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText(message);
        });
    }

    /**
     * Lấy current user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Lấy ClientSocket
     */
    public ClientSocket getClientSocket() {
        return clientSocket;
    }
}
