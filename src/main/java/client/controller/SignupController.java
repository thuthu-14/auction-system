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
 * SignupController - XÃ¡Â»Â­ lÃƒÂ½ mÃƒÂ n hÃƒÂ¬nh Ã„ÂÃ„Æ’ng kÃƒÂ½ qua Server Socket
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
        LoggerUtil.info("Ã¢Å“â€œ SignupController initialized");
    }

    @FXML
    private void handleSignup() {
        String ho = Ho.getText().trim();
        String ten = Ten.getText().trim();
        String email = Email.getText().trim();
        String password = mk.getText();

        // 1. Validation cÃ†Â¡ bÃ¡ÂºÂ£n
        if (ho.isEmpty() || ten.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "ThiÃ¡ÂºÂ¿u thÃƒÂ´ng tin", "Vui lÃƒÂ²ng nhÃ¡ÂºÂ­p Ã„â€˜Ã¡ÂºÂ§y Ã„â€˜Ã¡Â»Â§ HÃ¡Â»Â, TÃƒÂªn, Email vÃƒÂ  MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "Email khÃƒÂ´ng hÃ¡Â»Â£p lÃ¡Â»â€¡", "Vui lÃƒÂ²ng nhÃ¡ÂºÂ­p email Ã„â€˜ÃƒÂºng Ã„â€˜Ã¡Â»â€¹nh dÃ¡ÂºÂ¡ng.");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u yÃ¡ÂºÂ¿u", "MÃ¡ÂºÂ­t khÃ¡ÂºÂ©u phÃ¡ÂºÂ£i cÃƒÂ³ ÃƒÂ­t nhÃ¡ÂºÂ¥t 6 kÃƒÂ½ tÃ¡Â»Â±.");
            return;
        }

        String username = buildUsername(ho, ten, email);

        if (!ValidationUtil.isValidUsername(username)) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "TÃƒÂªn Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p lÃ¡Â»â€”i", "KhÃƒÂ´ng thÃ¡Â»Æ’ tÃ¡ÂºÂ¡o tÃƒÂªn Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p. Vui lÃƒÂ²ng thÃ¡Â»Â­ email khÃƒÂ¡c.");
            return;
        }

        loginButton.setDisable(true);

        // 2. Giao tiÃ¡ÂºÂ¿p vÃ¡Â»â€ºi Server Ã¡Â»Å¸ luÃ¡Â»â€œng riÃƒÂªng
        new Thread(() -> {
            try {
                clientSocket = (ClientSocket) authClientService.ensureConnected(clientSocket);
                currentUser = authClientService.register(clientSocket, username, password, email);

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION,
                            "Dang ky thanh cong",
                            "Tao tai khoan thanh cong!",
                            "Ten dang nhap cua ban la: " + username + "\nBan co the dung ten nay hoac email de dang nhap.");
                    switchToLogin();
                });
            } catch (Exception e) {
                LoggerUtil.error("Signup error: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i hÃ¡Â»â€¡ thÃ¡Â»â€˜ng", "SÃ¡Â»Â± cÃ¡Â»â€˜ kÃ¡ÂºÂ¿t nÃ¡Â»â€˜i", e.getMessage());
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
            stage.setTitle("Ã„ÂÃ„Æ’ng nhÃ¡ÂºÂ­p");
            StageUtil.showMaximized(stage);

            LoggerUtil.info("Switched back to login screen");
        } catch (IOException e) {
            LoggerUtil.error("Error switching to login screen: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i UI", "KhÃƒÂ´ng mÃ¡Â»Å¸ Ã„â€˜Ã†Â°Ã¡Â»Â£c mÃƒÂ n hÃƒÂ¬nh Ã„â€˜Ã„Æ’ng nhÃ¡ÂºÂ­p", e.getMessage());
        }
    }

    private String buildUsername(String ho, String ten, String email) {
        // Ã†Â¯u tiÃƒÂªn tÃ¡ÂºÂ¡o username tÃ¡Â»Â« hÃ¡Â»Â+tÃƒÂªn, loÃ¡ÂºÂ¡i bÃ¡Â»Â kÃƒÂ½ tÃ¡Â»Â± Ã„â€˜Ã¡ÂºÂ·c biÃ¡Â»â€¡t
        String base = (ho + ten).replaceAll("[^a-zA-Z0-9_]", "");

        // NÃ¡ÂºÂ¿u chuÃ¡Â»â€”i quÃƒÂ¡ ngÃ¡ÂºÂ¯n, lÃ¡ÂºÂ¥y phÃ¡ÂºÂ§n trÃ†Â°Ã¡Â»â€ºc @ cÃ¡Â»Â§a email
        if (base.length() < 3) {
            int atIndex = email.indexOf('@');
            String emailPrefix = atIndex > 0 ? email.substring(0, atIndex) : "user";
            base = emailPrefix.replaceAll("[^a-zA-Z0-9_]", "");
        }

        // NÃ¡ÂºÂ¿u vÃ¡ÂºÂ«n quÃƒÂ¡ ngÃ¡ÂºÂ¯n, dÃƒÂ¹ng timestamp
        if (base.length() < 3) {
            base = "user" + System.currentTimeMillis();
        }

        // GiÃ¡Â»â€ºi hÃ¡ÂºÂ¡n Ã„â€˜Ã¡Â»â„¢ dÃƒÂ i tÃ¡Â»â€˜i Ã„â€˜a 50 kÃƒÂ½ tÃ¡Â»Â±
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
