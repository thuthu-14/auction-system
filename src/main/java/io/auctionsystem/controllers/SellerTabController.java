package io.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerTabController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("SellerTab loaded");
    }

    @FXML
    private void handleAddProduct(ActionEvent event) {
        System.out.println("Nhấn thêm sản phẩm");
    }
}