package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.ProfileClient;
import client.service.ProfileClientService;
import client.util.DialogUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import navigation.NavigationContextAware;
import navigation.NavigationManager;
import server.model.RegularUser;
import server.model.User;

public class ProfileController implements NavigationContextAware {

    @FXML
    private VBox profileRoot;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField addressField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label userIdLabel;

    private User currentUser;
    private ClientSocket clientSocket;
    private final ProfileClient profileClientService;

    public ProfileController() {
        this(new ProfileClientService());
    }

    ProfileController(ProfileClient profileClientService) {
        this.profileClientService = profileClientService;
    }

    @FXML
    private void initialize() {
        Platform.runLater(this::forceProfileTextColors);
    }

    public void setUserData(User user) {
        setUserData(user, NavigationManager.getInstance().getClientSocket());
    }

    @Override
    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user != null ? user : NavigationManager.getInstance().getCurrentUser();
        this.clientSocket = socket != null ? socket : NavigationManager.getInstance().getClientSocket();
        populateUserData();
        Platform.runLater(this::forceProfileTextColors);
    }

    private void populateUserData() {
        if (currentUser == null) {
            return;
        }
        if (fullNameField != null) {
            fullNameField.setText(nonBlank(currentUser.getUsername(), ""));
        }
        if (emailField != null) {
            emailField.setText(nonBlank(currentUser.getEmail(), ""));
        }
        if (currentUser instanceof RegularUser regularUser) {
            if (phoneField != null) {
                phoneField.setText(nonBlank(regularUser.getShopPhone(), ""));
            }
            if (addressField != null) {
                addressField.setText(nonBlank(regularUser.getShopAddress(), ""));
            }
            if (userIdLabel != null) {
                userIdLabel.setText("#" + nonBlank(regularUser.getUserId(), "BUBBLY"));
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
        String name = value(fullNameField);
        String phone = value(phoneField);
        String address = value(addressField);
        String password = value(passwordField);
        if (name.isBlank() || phone.isBlank() || address.isBlank()) {
            DialogUtil.showAlert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Thiếu thông tin",
                    "Vui lòng cập nhật đầy đủ họ tên, số điện thoại và địa chỉ.",
                    profileRoot);
            return;
        }
        if (!phone.matches("\\d{10,11}")) {
            DialogUtil.showAlert(javafx.scene.control.Alert.AlertType.WARNING,
                    "Số điện thoại không hợp lệ",
                    "Số điện thoại phải gồm 10-11 chữ số.",
                    profileRoot);
            return;
        }

        ClientSocket socket = clientSocket != null ? clientSocket : ConnectionManager.getInstance().getClientSocket();
        client.util.ClientTaskRunner.run(() -> {
            try {
                User updatedUser = profileClientService.updateProfile(socket, currentUser, name, phone, address, password);
                Platform.runLater(() -> {
                    currentUser = updatedUser;
                    NavigationManager.getInstance().setCurrentUser(updatedUser);
                    populateUserData();
                    DialogUtil.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION,
                            "Đã lưu",
                            "Thông tin hồ sơ đã được cập nhật.",
                            profileRoot);
                });
            } catch (Exception e) {
                Platform.runLater(() -> DialogUtil.showAlert(javafx.scene.control.Alert.AlertType.ERROR,
                        "Lỗi",
                        e.getMessage(),
                        profileRoot));
            }
        });
    }

    private void forceProfileTextColors() {
        applyProfileTextColors(profileRoot);
    }

    private void applyProfileTextColors(Node node) {
        if (node == null) {
            return;
        }
        if (node instanceof Label label) {
            applyLabelColor(label, "#1a1a1a");
        } else if (node instanceof TextField textField) {
            String style = stripTextFill(textField.getStyle());
            textField.setStyle(style + " -fx-text-fill: #1a1a1a; -fx-prompt-text-fill: #9ca3af;");
            textField.setOpacity(1.0);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyProfileTextColors(child);
            }
        }
    }

    private void applyLabelColor(Label label, String color) {
        label.setTextFill(Color.web(color));
        String style = stripTextFill(label.getStyle());
        label.setStyle(style + " -fx-text-fill: " + color + ";");
    }

    private String stripTextFill(String style) {
        String cleaned = style == null ? "" : style.replaceAll("-fx-text-fill\\s*:\\s*[^;]+;?", "").trim();
        if (!cleaned.isEmpty() && !cleaned.endsWith(";")) {
            cleaned += ";";
        }
        return cleaned;
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    public static boolean hasCompleteBidProfile(User user) {
        return user instanceof RegularUser regularUser
                && regularUser.getShopPhone() != null && !regularUser.getShopPhone().isBlank()
                && regularUser.getShopAddress() != null && !regularUser.getShopAddress().isBlank();
    }

    private String value(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    @FXML
    private void handleLogout() {
        try {
            NavigationManager navigation = NavigationManager.getInstance();
            navigation.setMainStage((Stage) profileRoot.getScene().getWindow());
            navigation.goToLogin();
        } catch (Exception e) {
            ClientExceptionHandler.showError(ClientErrorType.NAVIGATION, "Profile logout", e, profileRoot);
        }
    }
}
