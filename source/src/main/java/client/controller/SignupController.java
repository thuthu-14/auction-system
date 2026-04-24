package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
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
import java.util.HashMap;
import java.util.Map;

public class SignupController {

    @FXML
    private TextField Ho;
    @FXML
    private TextField Ten;
    @FXML
    private TextField Email;
    @FXML
    private PasswordField mk;
    @FXML
    private Button loginButton;

    private ConnectionManager connectionManager;
    private ClientSocket clientSocket;
    private User currentUser;

    @FXML
    public void initialize() {
        LoggerUtil.info("SignupController initialized");
    }

    @FXML
    private void handleSignup() {
        String ho = Ho.getText().trim();
        String ten = Ten.getText().trim();
        String email = Email.getText().trim();
        String password = mk.getText();

        if (ho.isEmpty() || ten.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Thiếu thông tin", "Vui lòng nhập đầy đủ Họ, Tên, Email và Mật khẩu.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Email không hợp lệ", "Vui lòng nhập email đúng định dạng.");
            return;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu không hợp lệ", "Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        String username = buildUsername(ho, ten, email);

        if (!ValidationUtil.isValidUsername(username)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Tên đăng nhập không hợp lệ",
                    "Không thể tạo tên đăng nhập từ Họ/Tên/Email. Vui lòng nhập email hợp lệ hơn.");
            return;
        }

        loginButton.setDisable(true);

        new Thread(() -> {
            try {
                connectionManager = ConnectionManager.getInstance();

                if (!connectionManager.isConnected()) {
                    if (!connectionManager.connect()) {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối tới server",
                                "Hãy kiểm tra server đã chạy chưa.");
                        return;
                    }
                }

                clientSocket = connectionManager.getClientSocket();

                Map<String, String> registerData = new HashMap<>();
                registerData.put("username", username);
                registerData.put("password", password);
                registerData.put("email", email);

                Message message = new Message(MessageType.REGISTER, registerData, username);
                clientSocket.sendMessage(message);

                Message response = clientSocket.receiveMessage();

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    currentUser = (User) response.getData();

                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION,
                                "Đăng ký thành công",
                                "Tạo tài khoản thành công",
                                "Tên đăng nhập của bạn là: " + username + "\n"
                                        + "Bạn có thể dùng tên này để đăng nhập.");
                        switchToLogin();
                    });
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                    showAlert(Alert.AlertType.ERROR, "Đăng ký thất bại", "Không thể tạo tài khoản", errorMsg);
                }

            } catch (IOException e) {
                LoggerUtil.error("Signup IO error: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối tới server", e.getMessage());
            } catch (ClassNotFoundException e) {
                LoggerUtil.error("Signup ClassNotFoundException: " + e.getMessage());
                showAlert(Alert.AlertType.ERROR, "Lỗi dữ liệu", "Không đọc được phản hồi từ server", e.getMessage());
            } finally {
                Platform.runLater(() -> loginButton.setDisable(false));
            }
        }).start();
    }

    @FXML
    private void switchToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logindemo.fxml"));
            Scene scene = new Scene(loader.load(), 1300, 800);


            Stage stage = (Stage) loginButton.getScene().getWindow();

            LoginController loginController = loader.getController();

            if (clientSocket != null) {
                loginController.setClientSocket(clientSocket);
            }

            loginController.setOnLoginSuccess(() -> {
                try {
                    FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
                    Scene homeScene = new Scene(homeLoader.load(), 1000, 700);

                    HomeScreenController homeController = homeLoader.getController();
                    homeController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());

                    homeController.setOnLogout(() -> {
                        try {
                            switchToLogin();
                        } catch (Exception ex) {
                            LoggerUtil.error("Error returning to login screen: " + ex.getMessage());
                        }
                    });

                    stage.setScene(homeScene);
                    stage.setTitle("Chợ Đấu giá");
                    stage.show();

                    LoggerUtil.info("Switched to home screen from signup flow");
                } catch (IOException e) {
                    LoggerUtil.error("Error loading home screen: " + e.getMessage());
                }
            });

            loginController.setOnAdminLoginSuccess(() -> {
                try {
                    FXMLLoader adminLoader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
                    Scene adminScene = new Scene(adminLoader.load(), 1000, 700);

                    AdminController adminController = adminLoader.getController();
                    adminController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());


                    stage.setScene(adminScene);
                    stage.setTitle("Bảng điều khiển Admin");
                    stage.show();

                    LoggerUtil.info("Switched to admin screen from signup flow");
                } catch (IOException e) {
                    LoggerUtil.error("Error loading admin panel: " + e.getMessage());
                }
            });

            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
            stage.show();

            LoggerUtil.info("Switched to login screen");
        } catch (IOException e) {
            LoggerUtil.error("Error switching to login screen: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không mở được màn hình đăng nhập", e.getMessage());
        }
    }



    private String buildUsername(String ho, String ten, String email) {
        // Ưu tiên tạo username từ họ+tên
        String base = (ho + ten).replaceAll("[^a-zA-Z0-9_]", "");


        // Nếu không đủ, fallback sang phần trước @ của email
        if (base.length() < 3) {
            int atIndex = email.indexOf('@');
            String emailPrefix = atIndex > 0 ? email.substring(0, atIndex) : "user";
            base = emailPrefix.replaceAll("[^a-zA-Z0-9_]", "");

        }

        if (base.length() < 3) {
            base = "user" + System.currentTimeMillis();
        }


        if (base.length() > 50) {
            base = base.substring(0, 50);
        }

        return base;
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public ClientSocket getClientSocket() {
        return clientSocket;
    }



    private void showLoginFromLogout() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logindemo.fxml"));
        Scene scene = new Scene(loader.load(), 1300, 800);

        LoginController loginController = loader.getController();

        // Set callbacks
        loginController.setOnLoginSuccess(() -> {
            try {
                FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
                Scene homeScene = new Scene(homeLoader.load(), 1000, 700);

                HomeScreenController homeController = homeLoader.getController();
                homeController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());
                homeController.setOnLogout(() -> {
                    try {
                        showLoginFromLogout();
                    } catch (IOException e) {
                        LoggerUtil.error("Error: " + e.getMessage());
                    }
                });

                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(homeScene);
                stage.setTitle("🏪 Chợ Đấu giá");
                stage.show();
            } catch (IOException e) {
                LoggerUtil.error("Error loading home screen: " + e.getMessage());
            }
        });

        loginController.setOnAdminLoginSuccess(() -> {
            try {
                FXMLLoader adminLoader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml"));
                Scene adminScene = new Scene(adminLoader.load(), 1000, 700);

                AdminController adminController = adminLoader.getController();
                adminController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());


                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(adminScene);
                stage.setTitle("⚙️ Bảng điều khiển Admin");
                stage.show();
            } catch (IOException e) {
                LoggerUtil.error("Error loading admin panel: " + e.getMessage());
            }
        });

        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setTitle("Đăng nhập");
        stage.show();

        LoggerUtil.info("Returned to login screen");
    }




}


