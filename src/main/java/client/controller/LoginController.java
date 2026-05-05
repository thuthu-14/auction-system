package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.*;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import navigation.NavigationManager;
import util.LoggerUtil;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * LoginController - Xử lý màn hình Login qua Server Socket
 */
public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private TextField mkShow;

    private ConnectionManager connectionManager;
    private ClientSocket clientSocket;
    private server.model.User currentUser;
    private Runnable onLoginSuccess;
    private Runnable onAdminLoginSuccess;

    @FXML
    public void initialize() {
        progressIndicator.setVisible(false);
        messageLabel.setText("");
        mkShow.setVisible(false);
        mkShow.setManaged(false);

        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> switchToSignup());
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        mkShow.setOnAction(e -> handleLogin());

        LoggerUtil.info("✓ LoginController initialized");
    }

    @FXML
    private void handleLogin() {
        String email = usernameField.getText().trim();
        String password = getEnteredPassword();

        if (email.isEmpty() || password.isEmpty()) {
            showError("❌ Email và mật khẩu không được rỗng!");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                if (clientSocket == null) {
                    connectionManager = ConnectionManager.getInstance();
                    if (!connectionManager.isConnected()) {
                        if (!connectionManager.connect()) {
                            showError("❌ Không thể kết nối tới server!");
                            return;
                        }
                    }
                    clientSocket = connectionManager.getClientSocket();
                }

                LoginRequest loginRequest = new LoginRequest(email, password);
                Message loginMsg = new Message(MessageType.LOGIN, loginRequest, email);
                clientSocket.sendMessage(loginMsg);

                Message response = clientSocket.receiveMessage();

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    try {
                        com.google.gson.Gson contextGson = util.JsonUtil.getGson();

                        String json = contextGson.toJson(response.getData());

                        currentUser = contextGson.fromJson(json, server.model.User.class);

                        LoggerUtil.info("✓ Login thành công cho user: " + currentUser.getUsername() + " (Role: " + currentUser.getRole() + ")");
                    } catch (Exception e) {
                        LoggerUtil.error("Lỗi parse dữ liệu User: " + e.getMessage());
                        if (response.getData() instanceof server.model.User) {
                            currentUser = (server.model.User) response.getData();
                        }
                    }

                    Platform.runLater(() -> {
                        showSuccess("✓ Đăng nhập thành công!");
                        proceedToDashboard();
                    });
                } else {
                    String errorMsg = (response != null) ? response.getMessage() : "Sai tài khoản hoặc mật khẩu!";
                    showError("❌ " + errorMsg);
                }

            } catch (Exception e) {
                LoggerUtil.error("Login error: " + e.getMessage());
                showError("❌ Lỗi hệ thống: " + e.getMessage());
            } finally {
                setLoading(false);
            }
        }).start();
    }

    private void proceedToDashboard() {
        if (currentUser == null) return;

        Stage stage = (Stage) usernameField.getScene().getWindow();
        NavigationManager nav = NavigationManager.getInstance();
        nav.setMainStage(stage);
        nav.setCurrentUser(currentUser);
        nav.setClientSocket(clientSocket);

        if (currentUser.getRole() == UserRole.ADMIN) {
            if (onAdminLoginSuccess != null) {
                onAdminLoginSuccess.run();
            } else {
                // SỬ DỤNG navigateTo cho Admin
                nav.navigateTo("/fxml/AdminDashboard.fxml");
            }
        } else if (currentUser instanceof server.model.RegularUser) {
            server.model.RegularUser regUser = (server.model.RegularUser) currentUser;

            if (regUser.isSeller()) {
                // Lưu thông tin Seller cục bộ để tiện truy xuất
                saveSellerContext(regUser);
                nav.goToSellerHome();
            } else {
                // ĐÂY LÀ NHÁNH CHO TÀI KHOẢN MỚI HOẶC NGƯỜI CHỈ CÓ QUYỀN MUA/BID
                LoggerUtil.info("Điều hướng: Màn hình Home (Bidder)");
                if (onLoginSuccess != null) {
                    onLoginSuccess.run();
                } else {
                    nav.goToHome();
                }
            }
        }
    }

    private void saveSellerContext(server.model.RegularUser seller) {
        try {
            Map<String, String> sellerIdMap = new HashMap<>();
            sellerIdMap.put("sellerId", seller.getUserId());
            sellerIdMap.put("sellerName", seller.getShopName());
            util.JsonUtil.saveToJson("data/json/current_seller_id.json", sellerIdMap);
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Could not save seller context.");
        }
    }

    @FXML
    private void switchToSignup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SignUp.fxml"));
            Scene scene = new Scene(loader.load(), 1300, 800);
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Tạo tài khoản");
            stage.show();
        } catch (IOException e) {
            showError("❌ Không mở được màn hình đăng ký.");
        }
    }

    @FXML
    private void showPassword() {
        mkShow.setText(passwordField.getText());
        togglePass(true);
    }

    @FXML
    private void hidePassword() {
        passwordField.setText(mkShow.getText());
        togglePass(false);
    }

    private void togglePass(boolean show) {
        mkShow.setVisible(show);
        mkShow.setManaged(show);
        passwordField.setVisible(!show);
        passwordField.setManaged(!show);
    }

    private String getEnteredPassword() {
        return mkShow.isVisible() ? mkShow.getText() : passwordField.getText();
    }

    private void setLoading(boolean isLoading) {
        Platform.runLater(() -> {
            loginButton.setDisable(isLoading);
            registerButton.setDisable(isLoading);
            progressIndicator.setVisible(isLoading);
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: #ff4d4d;");
            messageLabel.setText(message);
        });
    }

    private void showSuccess(String message) {
        Platform.runLater(() -> {
            messageLabel.setStyle("-fx-text-fill: #2ecc71;");
            messageLabel.setText(message);
        });
    }

    public void setOnLoginSuccess(Runnable callback) { this.onLoginSuccess = callback; }
    public void setOnAdminLoginSuccess(Runnable callback) { this.onAdminLoginSuccess = callback; }
    public void setClientSocket(ClientSocket socket) { this.clientSocket = socket; }

    public server.model.User getCurrentUser() {
        return this.currentUser;
    }

    public client.network.ClientSocket getClientSocket() {
        return this.clientSocket;
    }
}
