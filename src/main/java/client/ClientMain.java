package client;

import client.controller.LoginController;
import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import navigation.NavigationManager;
import util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;

public class ClientMain extends Application {

    private static Stage primaryStage;
    private LoginController loginController;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        ClientExceptionHandler.installGlobalHandlers();

        NavigationManager.getInstance().setMainStage(primaryStage);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        loadCustomFonts();
        loadWindowIcon();
        LoggerUtil.info("Auction system client started");
        showLoginScreen();

        stage.setOnCloseRequest(e -> {
            LoggerUtil.info("Application closed");
            System.exit(0);
        });
    }

    private void loadCustomFonts() {
        loadFont("/CSS/DarleySans-Regular.otf");
    }

    private void loadWindowIcon() {
        try (InputStream input = getClass().getResourceAsStream("/CSS/logo.png")) {
            if (input == null) {
                LoggerUtil.warn("Could not load window icon: /CSS/logo.png");
                return;
            }
            primaryStage.getIcons().add(new Image(input));
        } catch (IOException e) {
            LoggerUtil.warn("Could not load window icon: /CSS/logo.png - " + e.getMessage());
        }
    }

    private void loadFont(String resourcePath) {
        try (InputStream input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null || Font.loadFont(input, 12) == null) {
                LoggerUtil.warn("Could not load font: " + resourcePath);
            }
        } catch (IOException e) {
            LoggerUtil.warn("Could not load font: " + resourcePath + " - " + e.getMessage());
        }
    }

    private void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Scene scene = new Scene(loader.load(), 1300, 800);

            loginController = loader.getController();
            loginController.setOnAdminLoginSuccess(this::showAdminPanel);
            loginController.setOnLoginSuccess(this::showHomeScreen);

            primaryStage.setTitle("Hệ thống Đấu giá - Đăng nhập");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();

            LoggerUtil.info("Login screen displayed");
        } catch (IOException e) {
            ClientExceptionHandler.showError(ClientErrorType.NAVIGATION, "Error loading login screen", e);
        }
    }

    private void showAdminPanel() {
        if (loginController == null || loginController.getCurrentUser() == null) {
            ClientExceptionHandler.showError(
                    ClientErrorType.NAVIGATION,
                    "Error loading admin panel",
                    new IllegalStateException("Missing authenticated user")
            );
            return;
        }

        NavigationManager nav = NavigationManager.getInstance();
        nav.setMainStage(primaryStage);
        nav.setCurrentUser(loginController.getCurrentUser());
        nav.setClientSocket(loginController.getClientSocket());
        nav.goToAdminHome();
    }

    private void showHomeScreen() {
        if (loginController == null || loginController.getCurrentUser() == null) {
            ClientExceptionHandler.showError(
                    ClientErrorType.NAVIGATION,
                    "Error loading home screen",
                    new IllegalStateException("Missing authenticated user")
            );
            return;
        }

        NavigationManager nav = NavigationManager.getInstance();
        nav.setMainStage(primaryStage);
        nav.setCurrentUser(loginController.getCurrentUser());
        nav.setClientSocket(loginController.getClientSocket());
        nav.goToHome();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
