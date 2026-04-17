package io.auctionsystem.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.util.ResourceBundle;

public class WalletController implements Initializable {

    @FXML
    private Label balanceLabel;

    @FXML
    private ComboBox<String> timeFilter;

    @FXML
    private TextField searchField;

    @FXML
    private VBox addBankBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (timeFilter != null) {
            timeFilter.getItems().addAll("Tháng này", "Tháng trước", "3 tháng gần đây", "Tất cả thời gian");
            timeFilter.getSelectionModel().selectFirst();
        }
        loadWalletData();
    }

    private void loadWalletData() {
        balanceLabel.setText("0 đ");
    }

    @FXML
    private void handleDeposit() {
        System.out.println("Mở popup nạp tiền...");
    }

    @FXML
    private void handleWithdraw() {
        System.out.println("Mở popup rút tiền...");
    }


    @FXML
    private void handleAddBankAccount() {
        System.out.println("Mở form liên kết ngân hàng...");
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText();
        System.out.println("Đang tìm giao dịch: " + keyword);
    }

    @FXML
    private void filterAll() { /* Logic lọc tất cả */ }

    @FXML
    private void filterIn() { /* Logic lọc tiền vào */ }

    @FXML
    private void filterOut() { /* Logic lọc tiền ra */ }
}