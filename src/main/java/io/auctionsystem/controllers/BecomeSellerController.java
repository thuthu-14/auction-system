package io.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class BecomeSellerController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;

    @FXML private PasswordField passwordField; // Ô ẩn MK (mặc định)
    @FXML private TextField passwordTextField; // Ô hiện MK
    @FXML private Button btnTogglePassword;    // Nút con mắt

    @FXML
    private Button btnCancel;
    @FXML
    private Button btnSave;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        //btnSave.setOnAction(event -> handleSave());
        //btnCancel.setOnAction(event -> handleCancel());

        //con mắt password
        btnTogglePassword.setVisible(false); //ẩn khi mở
        //lắng nghe hành động
        passwordField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            btnTogglePassword.setVisible(!newValue.isEmpty());
        });

        passwordTextField.textProperty().addListener((observable, oldValue, newValue) ->
        {
            btnTogglePassword.setVisible(!newValue.isEmpty());
        });
    }

    @FXML
    private void handleSave() {
        // Lấy dữ liệu
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        String password = getPassword(); //lấy mk ko trim (mất dấu cách)

        // Kiểm tra tính hợp lệ (Validation)
        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || address.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập dữ liệu", "Vui lòng nhập đầy đủ tất cả các thông tin!");
            return;
        }

        // In ra console để test
        System.out.println("--- ĐANG LƯU THÔNG TIN NGƯỜI BÁN ---");
        System.out.println("Họ tên: " + name);
        System.out.println("Email: " + email);
        System.out.println("Số điện thoại: " + phone);
        System.out.println("Địa chỉ: " + address);
        System.out.println("Mật khẩu (Đã lấy thành công): " + "*".repeat(password.length()));
        // Trong thực tế, gọi logic Database ở đây

        // Hiển thị thông báo thành công
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thông tin đăng ký Người bán đã được lưu!");
    }

    @FXML
    private void handleCancel() {
        System.out.println("Người dùng đã bấm Quay lại/Hủy.");
        // TODO: Thêm code load lại màn hình Home vào contentArea
    }

    // Hàm ẩn/hiện mật khẩu
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

    // Hàm an toàn để lấy text mật khẩu dù đang ở trạng thái ẩn hay hiện
    private String getPassword() {
        return isPasswordVisible ? passwordTextField.getText() : passwordField.getText();
    }

    // Hàm tạo Popup thông báo
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();

        try {
            javafx.scene.text.Font.loadFont(getClass().getResourceAsStream("/io/auctionsystem/css/BDLifelessGrotesk-SemiBold.otf"), 14);

            String cssPath = "/io/auctionsystem/css/style.css";
            var cssResource = getClass().getResource(cssPath);

            if (cssResource != null) {
                dialogPane.getStylesheets().add(cssResource.toExternalForm());
                dialogPane.getStyleClass().add("my-custom-alert");
            }
        } catch (Exception e) {
            System.out.println("Cảnh báo: Không load được CSS/Font cho cảnh báo. Dùng giao diện mặc định.");
        }

        alert.showAndWait();
    }
}