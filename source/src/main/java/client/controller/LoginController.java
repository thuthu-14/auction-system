package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.network.MessageHandler;
import common.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
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
// imports
import javafx.scene.input.MouseEvent;





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
    private Runnable onAdminLoginSuccess;


    @FXML private TextField mkShow;

    @FXML
    private void showPassword() {
        mkShow.setText(passwordField.getText());
        mkShow.setVisible(true);
        mkShow.setManaged(true);
        passwordField.setVisible(false);
        passwordField.setManaged(false);
    }

    @FXML
    private void hidePassword() {
        passwordField.setText(mkShow.getText());
        passwordField.setVisible(true);
        passwordField.setManaged(true);
        mkShow.setVisible(false);
        mkShow.setManaged(false);
    }


    @FXML
    private void switchToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/signup.fxml"));
            Scene scene = new Scene(loader.load(), 1300, 800);

            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Tạo tài khoản");
            stage.show();

            LoggerUtil.info("Switched to signup screen");
        } catch (IOException e) {
            LoggerUtil.error("Error switching to signup screen: " + e.getMessage());
            showError(" Không mở được màn hình đăng ký: " + e.getMessage());
        }
    }








    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        messageLabel.setText("");

        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> switchToSignup());

        LoggerUtil.info("✓ LoginController initialized");

        mkShow.setVisible(false);
        mkShow.setManaged(false);

    }


    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }
    public void setOnAdminLoginSuccess(Runnable callback) {
        this.onAdminLoginSuccess = callback;
    }


    public void setClientSocket(ClientSocket socket) {
        this.clientSocket = socket;

    }




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


        new Thread(() -> {
            try {

                if (clientSocket == null) {
                    // Tạo connection mới
                    connectionManager = ConnectionManager.getInstance();
                    if (!connectionManager.isConnected()) {
                        if (!connectionManager.connect()) {
                            showError("❌ Không thể kết nối tới server!");
                            return;
                        }
                    }
                    clientSocket = connectionManager.getClientSocket();
                }





                LoginRequest loginRequest = new LoginRequest(username, password);
                Message message = new Message(MessageType.LOGIN, loginRequest, username);


                clientSocket.sendMessage(message);


                Message response = clientSocket.receiveMessage();

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    currentUser = (User) response.getData();
                    LoggerUtil.info("✓ Login successful: " + currentUser.getUsername());

                    Platform.runLater(() -> {
                        showSuccess("✓ Đăng nhập thành công!");


                        if (currentUser.getRole() == UserRole.ADMIN) {
                            if (onAdminLoginSuccess != null) {
                                LoggerUtil.info("DEBUG: Calling onAdminLoginSuccess callback");
                                onAdminLoginSuccess.run();  // Vào admin panel
                            } else {
                                LoggerUtil.error("ERROR: onAdminLoginSuccess callback is NULL!");
                            }
                        } else {
                            if (onLoginSuccess != null) {
                                LoggerUtil.info("DEBUG: Calling onLoginSuccess callback");
                                onLoginSuccess.run();  // Vào home screen
                            } else {
                                LoggerUtil.error("ERROR: onLoginSuccess callback is NULL!");
                            }
                        }
                    });

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


    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError(" Username và password không được rỗng!");
            return;
        }

        if (username.length() < 3) {
            showError(" Username phải có ít nhất 3 ký tự!");
            return;
        }

        if (password.length() < 6) {
            showError(" Password phải có ít nhất 6 ký tự!");
            return;
        }

        // Disable buttons
        loginButton.setDisable(true);
        registerButton.setDisable(true);
        progressIndicator.setVisible(true);


        new Thread(() -> {
            try {

                connectionManager = ConnectionManager.getInstance();
                if (!connectionManager.isConnected()) {
                    if (!connectionManager.connect()) {
                        showError("❌ Không thể kết nối tới server!");
                        return;
                    }
                }

                clientSocket = connectionManager.getClientSocket();


                Map<String, String> registerData = new HashMap<>();
                registerData.put("username", username);
                registerData.put("password", password);
                registerData.put("email", username + "@auction.com");

                Message message = new Message(MessageType.REGISTER, registerData, username);


                clientSocket.sendMessage(message);


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


    private void showError(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: red;");
            messageLabel.setText(message);
        });
    }


    private void showSuccess(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: green;");
            messageLabel.setText(message);
        });
    }


    public User getCurrentUser() {
        return currentUser;
    }


    public ClientSocket getClientSocket() {
        return clientSocket;
    }
}
