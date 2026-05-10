package client.controller;

import client.util.ResponsiveSceneUtil;
import client.util.StageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import navigation.NavigationManager;
import util.LoggerUtil;

public class ProfileController {

    @FXML
    private VBox profileRoot;

    @FXML
    private void handleLogout() {
        try {
            NavigationManager.getInstance().setCurrentUser(null);
            NavigationManager.getInstance().setClientSocket(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileRoot.getScene().getWindow();
            stage.setScene(ResponsiveSceneUtil.createScaledScene(root));
            StageUtil.showMaximized(stage);
        } catch (Exception e) {
            LoggerUtil.error("Loi khi dang xuat tu profile: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
