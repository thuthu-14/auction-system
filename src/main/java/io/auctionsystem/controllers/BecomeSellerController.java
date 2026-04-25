package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class BecomeSellerController {

    @FXML private TextField shopNameField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextArea descriptionArea;

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void handleBack() {
        if (homeController != null) {
            homeController.goBackToBidderHome();
        }
    }

    @FXML
    private void handleConfirmSeller() {
        String shopName = shopNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (shopName.isEmpty() || phone.isEmpty() || address.isEmpty() || description.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng nhập đầy đủ thông tin người bán.");
            alert.showAndWait();
            return;
        }

        if (homeController != null) {
            homeController.switchToSellerMode();
        }
    }
}