package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class SellerTabController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    public void initialize() {
        titleLabel.setText("Seller Management");
        messageLabel.setText("Welcome to Seller Tab");
    }

    @FXML
    private void handleButtonClick() {
        messageLabel.setText("Button clicked!");
    }
}