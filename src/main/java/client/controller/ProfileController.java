package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import navigation.NavigationManager;

public class ProfileController {

    @FXML
    private VBox profileRoot;

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
