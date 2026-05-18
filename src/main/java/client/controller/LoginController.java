package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientException;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuthClientService;
import common.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import navigation.NavigationManager;
import util.LoggerUtil;

import java.io.InputStream;

/**
 * LoginController - Xử lý màn hình Login qua Server Socket
 */
public class LoginController {

    @FXML private Label loginTitleLabel;
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
    private final AuthClientService authClientService = new AuthClientService();

    @FXML
    public void initialize() {
        applyTitleFont();
        progressIndicator.setVisible(false);
        messageLabel.setText("");
        mkShow.setVisible(false);
        mkShow.setManaged(false);

        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> switchToSignup());
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        mkShow.setOnAction(e -> handleLogin());

        LoggerUtil.info("? LoginController initialized");
    }

    private void applyTitleFont() {
        if (loginTitleLabel == null) {
            return;
        }

        try (InputStream input = getClass().getResourceAsStream("/CSS/DarleySans-Regular.otf")) {
            Font font = input == null ? null : Font.loadFont(input, 36);
            if (font != null) {
                loginTitleLabel.setFont(font);
            } else {
                loginTitleLabel.setFont(Font.font("Darley Sans Regular", 36));
            }
        } catch (Exception e) {
            LoggerUtil.warn("Could not apply Darley font to login title: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String email = usernameField.getText().trim();
        String password = getEnteredPassword();

        if (email.isEmpty() || password.isEmpty()) {
            showError("? Email và mật khẩu không được rỗng!");
            return;
        }

        setLoading(true);

        new Thread(() -> {
            try {
                clientSocket = (ClientSocket) authClientService.ensureConnected(clientSocket);
                currentUser = authClientService.login(clientSocket, email, password);
                LoggerUtil.info("Đăng nhập thành công cho user: " + currentUser.getUsername());
                Platform.runLater(() -> {
                    showSuccess("Đăng nhập thành công!");
                    proceedToDashboard();
                });
            } catch (Exception e) {
                ClientException exception = ClientExceptionHandler.wrap(ClientErrorType.AUTHENTICATION, "Login error", e);
                ClientExceptionHandler.handle("Login error", exception);
                showError("Lỗi hệ thống: " + e.getMessage());
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
                nav.goToAdminHome();
            }
        } else if (currentUser instanceof server.model.RegularUser) {
            server.model.RegularUser regUser = (server.model.RegularUser) currentUser;

            if (regUser.isSeller()) {
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

    @FXML
    private void switchToSignup() {
        NavigationManager navigation = NavigationManager.getInstance();
        navigation.setMainStage((Stage) registerButton.getScene().getWindow());
        navigation.navigateTo("/fxml/Signup.fxml");
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

