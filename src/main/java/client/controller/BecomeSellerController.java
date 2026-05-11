package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuthClientService;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import navigation.NavigationManager;
import server.model.User;
import server.model.RegularUser;

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
    private final SellerClientService sellerClientService = new SellerClientService();
    private final AuthClientService authClientService = new AuthClientService();

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
            showAlert(Alert.AlertType.WARNING, "ThÃƒÂ´ng bÃƒÂ¡o", "Vui lÃƒÂ²ng nhÃ¡ÂºÂ­p Ã„â€˜Ã¡ÂºÂ§y Ã„â€˜Ã¡Â»Â§ tÃ¡ÂºÂ¥t cÃ¡ÂºÂ£ cÃƒÂ¡c thÃƒÂ´ng tin!");
            return null;
        }
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert(Alert.AlertType.WARNING, "ThÃƒÂ´ng bÃƒÂ¡o", "Vui lÃƒÂ²ng nhÃ¡ÂºÂ­p Ã„â€˜ÃƒÂºng Ã„â€˜Ã¡Â»â€¹nh dÃ¡ÂºÂ¡ng email!");
            return null;
        }
        if (!phone.matches("\\d{10,11}")) {
            showAlert(Alert.AlertType.WARNING, "ThÃƒÂ´ng bÃƒÂ¡o", "Vui lÃƒÂ²ng nhÃ¡ÂºÂ­p Ã„â€˜ÃƒÂºng SDT");
            return null;
        }
        return new SellerData(name, email, phone, address, password);
    }

    @FXML
    private void handleSave() {
        if (this.currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "KhÃƒÂ´ng lÃ¡ÂºÂ¥y Ã„â€˜Ã†Â°Ã¡Â»Â£c thÃƒÂ´ng tin user hiÃ¡Â»â€¡n tÃ¡ÂºÂ¡i!");
            return;
        }

        if (!(currentUser instanceof RegularUser)) {
            showAlert(Alert.AlertType.ERROR, "LÃ¡Â»â€”i", "ChÃ¡Â»â€° ngÃ†Â°Ã¡Â»Âi dÃƒÂ¹ng thÃ†Â°Ã¡Â»Âng mÃ¡Â»â€ºi cÃƒÂ³ thÃ¡Â»Æ’ nÃƒÂ¢ng cÃ¡ÂºÂ¥p!");
            return;
        }

        RegularUser regularUser = (RegularUser) currentUser;

        if (regularUser.isSeller()) {
            showAlert(Alert.AlertType.WARNING, "ThÃƒÂ´ng bÃƒÂ¡o", "BÃ¡ÂºÂ¡n Ã„â€˜ÃƒÂ£ lÃƒÂ  NgÃ†Â°Ã¡Â»Âi bÃƒÂ¡n rÃ¡Â»â€œi! KhÃƒÂ´ng cÃ¡ÂºÂ§n nÃƒÂ¢ng cÃ¡ÂºÂ¥p lÃ¡ÂºÂ¡i.");
            NavigationManager.getInstance().goToHome();
            return;
        }

        SellerData data = validateInput();
        if (data != null) {
            btnSave.setDisable(true);
            btnSave.setText("Ã„Âang xÃ¡Â»Â­ lÃƒÂ½...");

            new Thread(() -> {
                try {
                    ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
                    if (socket == null) {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi mang", "Chua ket noi toi Server!"));
                        return;
                    }

                    sellerClientService.upgradeSeller(socket, regularUser, data.name(), data.phone(), data.address(), data.email());

                    Platform.runLater(() -> {
                        regularUser.upgradeSeller(data.name(), data.phone(), data.address(), data.email());
                        authClientService.saveSellerContext(regularUser);
                        showAlert(Alert.AlertType.INFORMATION, "Thanh cong", "Chuc mung! Ban da tro thanh Nguoi ban!");
                        NavigationManager.getInstance().setMainStage((javafx.stage.Stage) nameField.getScene().getWindow());
                        NavigationManager.getInstance().goToSellerHome();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi Server", e.getMessage()));
                } finally {
                    Platform.runLater(() -> {
                        btnSave.setDisable(false);
                        btnSave.setText("Ã„ÂÃ„Æ’ng kÃƒÂ½");
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
            btnTogglePassword.setText("Ã°Å¸â€â€™");
        } else {
            passwordField.setText(passwordTextField.getText());
            showPassword(false);
            btnTogglePassword.setText("Ã°Å¸â€˜Â");
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
