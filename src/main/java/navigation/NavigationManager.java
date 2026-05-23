package navigation;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.controller.DashboardController;
import client.controller.HomeScreenController;
import client.controller.AdminHomeController;
import client.controller.ProfileController;
import client.controller.SellerDashboardController;
import client.controller.SellerHomeController;
import client.controller.WalletController;
import client.network.ClientSocket;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import server.model.User;
import util.LoggerUtil;

import java.io.IOException;
import java.util.function.Consumer;

public class NavigationManager {

    private static NavigationManager instance;
    private Stage mainStage;
    private User currentUser;
    private ClientSocket clientSocket;

    private NavigationManager() {}

    public static NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public void setClientSocket(ClientSocket socket) {
        this.clientSocket = socket;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public ClientSocket getClientSocket() {
        return clientSocket;
    }

    public void navigateTo(String fxmlPath) {
        navigateTo(fxmlPath, null);
    }

    public void navigateTo(String fxmlPath, Consumer<Object> controllerSetup) {
        if (mainStage == null) {
            LoggerUtil.error("NavigationManager: mainStage is null");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Object controller = loader.getController();
            configureController(controller);
            if (controllerSetup != null) {
                controllerSetup.accept(controller);
            }

            Scene currentScene = mainStage.getScene();
            Scene scene = currentScene != null
                    ? new Scene(root, currentScene.getWidth(), currentScene.getHeight())
                    : new Scene(root, 1300, 800);
            mainStage.setScene(scene);
            mainStage.setFullScreenExitHint("");
            mainStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            mainStage.setMaximized(true);
            mainStage.show();
        } catch (IOException e) {
            ClientExceptionHandler.showError(ClientErrorType.NAVIGATION, "Navigation error [" + fxmlPath + "]", e);
        }
    }

    public Object loadContent(StackPane contentArea, String fxmlPath) {
        return loadContent(contentArea, fxmlPath, null);
    }

    public Object loadContent(StackPane contentArea, String fxmlPath, Consumer<Object> controllerSetup) {
        if (contentArea == null) {
            LoggerUtil.warn("NavigationManager: contentArea is null for " + fxmlPath);
            return null;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent node = loader.load();
            Object controller = loader.getController();
            configureController(controller);
            if (controllerSetup != null) {
                controllerSetup.accept(controller);
            }

            prepareContentNode(node);
            contentArea.getChildren().setAll(node);
            return controller;
        } catch (IOException e) {
            ClientExceptionHandler.showError(ClientErrorType.NAVIGATION, "Navigation content error [" + fxmlPath + "]", e);
            return null;
        }
    }

    private void configureController(Object controller) {
        if (controller instanceof HomeScreenController hsc) {
            hsc.setUserData(currentUser, clientSocket);
        } else if (controller instanceof AdminHomeController ahc) {
            ahc.setUserData(currentUser, clientSocket);
        } else if (controller instanceof DashboardController dc) {
            dc.setUserData(currentUser, clientSocket);
        } else if (controller instanceof WalletController wc) {
            wc.setUserData(currentUser, clientSocket);
        } else if (controller instanceof ProfileController pc) {
            pc.setUserData(currentUser);
        } else if (controller instanceof SellerHomeController shc) {
            shc.setUserData(currentUser, clientSocket);
        } else if (controller instanceof SellerDashboardController sdc) {
            sdc.setUserData(currentUser, clientSocket);
        }
    }

    private void prepareContentNode(Node node) {
        StackPane.setAlignment(node, Pos.TOP_LEFT);
        if (node instanceof Region region) {
            region.setMinWidth(0);
            region.setMinHeight(0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
        if (node instanceof ScrollPane scrollPane) {
            scrollPane.setFitToWidth(true);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
    }

    public void goToLogin() {
        setCurrentUser(null);
        setClientSocket(null);
        navigateTo("/fxml/Login.fxml");
    }

    public void goToHome() {
        navigateTo("/fxml/BidderView/BidderHome.fxml");
    }

    public void goToSellerHome() {
        navigateTo("/fxml/SellerView/SellerHome.fxml");
    }

    public void goToAdminHome() {
        navigateTo("/fxml/AdminView/AdminHome.fxml");
    }

    public void goToWallet() {
        navigateTo("/fxml/WalletView.fxml");
    }
}
