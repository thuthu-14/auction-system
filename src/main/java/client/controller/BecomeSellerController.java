package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClient;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import navigation.NavigationManager;
import server.model.RegularUser;
import server.model.User;

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
    private final SellerClient sellerClientService;

    public BecomeSellerController() {
        this(new SellerClientService());
    }

    BecomeSellerController(SellerClient sellerClientService) {
        this.sellerClientService = sellerClientService;
    }

    @FXML
    public void initialize() {
        btnSave.setOnAction(event -> handleSave());
        btnCancel.setOnAction(event -> handleCancel());
        btnTogglePassword.setVisible(false);

        currentUser = NavigationManager.getInstance().getCurrentUser();
        populateUserData();

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
        if (!email.matches("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đúng định dạng email!");
            return null;
        }
        if (!phone.matches("\\d{10,11}")) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập đúng số điện thoại!");
            return null;
        }
        return new SellerData(name, email, phone, address, password);
    }

    @FXML
    private void handleSave() {
        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lấy được thông tin người dùng hiện tại!");
            return;
        }
        if (!(currentUser instanceof RegularUser regularUser)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Chỉ người dùng thường mới có thể nâng cấp!");
            return;
        }
        if (regularUser.isSeller()) {
            showAlert(Alert.AlertType.WARNING, "Thông báo", "Bạn đã là Người bán rồi! Không cần nâng cấp lại.");
            NavigationManager.getInstance().goToHome();
            return;
        }

        SellerData data = validateInput();
        if (data == null) {
            return;
        }

        btnSave.setDisable(true);
        btnSave.setText("\u0110ang x\u1eed l\u00fd...");

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
                if (socket == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Chưa kết nối tới Server!"));
                    return;
                }

                sellerClientService.upgradeSeller(
                        socket,
                        regularUser,
                        data.name(),
                        data.phone(),
                        data.address(),
                        data.email(),
                        data.password()
                );

                Platform.runLater(() -> {
                    regularUser.upgradeSeller(data.name(), data.phone(), data.address(), data.email());
                    NavigationManager.getInstance().setCurrentUser(regularUser);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Chúc mừng! Bạn đã trở thành Người bán!");
                    NavigationManager.getInstance().setMainStage((javafx.stage.Stage) nameField.getScene().getWindow());
                    NavigationManager.getInstance().goToSellerHome();
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi Server", e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    btnSave.setDisable(false);
                    btnSave.setText("L\u01b0u");
                });
            }
        });
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        populateUserData();
    }

    public User getCurrentUser() {
        return currentUser;
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
            btnTogglePassword.setText("An");
        } else {
            passwordField.setText(passwordTextField.getText());
            showPassword(false);
            btnTogglePassword.setText("Hien");
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

    private void populateUserData() {
        if (emailField == null || currentUser == null) {
            return;
        }
        String email = currentUser.getEmail();
        if (email != null && !email.isBlank()) {
            emailField.setText(email);
            emailField.setEditable(false);
            emailField.setFocusTraversable(false);
        }
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        client.util.DialogUtil.showAlert(alertType, title, null, message, nameField);
    }
}
