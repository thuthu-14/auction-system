// src/main/java/client/ClientMain.java
package client;

import client.controller.AdminPanelController;
import client.controller.HomeScreenController;
import client.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import util.LoggerUtil;
import java.io.IOException;


public class ClientMain extends Application {

    private static Stage primaryStage;
    private LoginController loginController;


    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

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
    private void showLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/logindemo.fxml"));
        Scene scene = new Scene(loader.load(), 400, 300);

        loginController = loader.getController();
        loginController.setOnAdminLoginSuccess(this::showAdminPanel);
        loginController.setOnLoginSuccess(this::showHomeScreen);



        primaryStage.setTitle("🏪 Hệ thống Đấu giá - Đăng nhập");
        primaryStage.setScene(scene);
        primaryStage.show();

        LoggerUtil.info("✓ Login screen displayed");
    }


    private void showAdminPanel() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/a.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);

            // ← THÊM: Set admin data to controller
            AdminPanelController adminController = loader.getController();
            adminController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());






            primaryStage.setTitle("⚙️ Bảng điều khiển Admin");
            primaryStage.setScene(scene);

            LoggerUtil.info("✓ Admin panel displayed");
        } catch (IOException e) {
            LoggerUtil.error("Error loading admin panel: " + e.getMessage());
        }
    }

    private void showHomeScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);

            // ← THÊM: Set user data to controller
            HomeScreenController homeController = loader.getController();
            homeController.setUserData(loginController.getCurrentUser(), loginController.getClientSocket());
            homeController.setOnLogout(() -> {
                try {
                    showLoginScreen();
                } catch (IOException e) {
                    LoggerUtil.error("Error returning to login screen: " + e.getMessage());
                }
            });




            primaryStage.setTitle("🏪 Chợ Đấu giá");
            primaryStage.setScene(scene);

            LoggerUtil.info("✓ Home screen displayed");
        } catch (IOException e) {
            LoggerUtil.error("Error loading home screen: " + e.getMessage());
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
