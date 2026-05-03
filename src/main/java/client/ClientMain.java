package client;

import client.controller.AdminHomeController;
import client.controller.HomeScreenController;
import client.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import navigation.NavigationManager; // <-- Import thêm cái này để đồng bộ
import util.LoggerUtil;

import java.io.IOException;

public class ClientMain extends Application {

    private static Stage primaryStage;
    private LoginController loginController;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        // BẮT BUỘC CÓ ĐỂ ĐỒNG BỘ: Cấp phát main stage cho NavigationManager
        NavigationManager.getInstance().setMainStage(primaryStage);

        LoggerUtil.info("╔════════════════════════════════════════╗");
        LoggerUtil.info("║   AUCTION SYSTEM CLIENT v1.0           ║");
        LoggerUtil.info("║   Online Bidding Platform              ║");
        LoggerUtil.info("╚════════════════════════════════════════╝");

        showLoginScreen();

        stage.setOnCloseRequest(e -> {
            LoggerUtil.info("✓ Application closed");
            System.exit(0);
        });
    }

    /**
     * Hiển thị login screen
     */
    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));

            // SỬA: Đồng bộ kích thước màn hình giống như trong SignupController (1300x800)
            Scene scene = new Scene(loader.load(), 1300, 800);

            loginController = loader.getController();
            loginController.setOnAdminLoginSuccess(this::showAdminPanel);
            loginController.setOnLoginSuccess(this::showHomeScreen);

            primaryStage.setTitle("🏪 Hệ thống Đấu giá - Đăng nhập");
            primaryStage.setScene(scene);
            primaryStage.show();

            LoggerUtil.info("✓ Login screen displayed");
        } catch (IOException e) {
            LoggerUtil.error("❌ Error loading login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/AdminHome.fxml"));

            if (loader.getLocation() == null) {
                LoggerUtil.error("❌ Cannot find AdminHome.fxml");
                return;
            }

            Parent root = loader.load();
            Scene scene = new Scene(root, 1000, 700);
            AdminHomeController adminController = loader.getController();

            if (adminController != null && loginController != null) {
                if (loginController.getCurrentUser() != null && loginController.getClientSocket() != null) {
                    adminController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());
                }

                // Rút gọn callback
                adminController.setOnLogout(this::showLoginScreen);
            }

            primaryStage.setTitle("⚙️ Bảng điều khiển Admin");
            primaryStage.setScene(scene);
            primaryStage.show();

            LoggerUtil.info("✓ Admin panel displayed");
        } catch (Exception e) {
            LoggerUtil.error("❌ Error loading admin panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showHomeScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));

            if (loader.getLocation() == null) {
                LoggerUtil.error("❌ Cannot find home.fxml");
                return;
            }

            Scene scene = new Scene(loader.load(), 1000, 700);
            HomeScreenController homeController = loader.getController();

            if (homeController != null && loginController != null) {
                if (loginController.getCurrentUser() != null && loginController.getClientSocket() != null) {
                    homeController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());
                }

                // Rút gọn callback
                homeController.setOnLogout(this::showLoginScreen);
            }

            primaryStage.setTitle("🏪 Chợ Đấu giá");
            primaryStage.setScene(scene);
            primaryStage.show();

            LoggerUtil.info("✓ Home screen displayed");
        } catch (Exception e) {
            LoggerUtil.error("❌ Error loading home screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}