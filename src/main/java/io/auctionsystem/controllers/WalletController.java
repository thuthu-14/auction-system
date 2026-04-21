package io.auctionsystem.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ResourceBundle;

public class WalletController implements Initializable {

    @FXML private Label balanceLabel;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private VBox addBankBox;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (timeFilter != null) {
            timeFilter.getItems().addAll("Tháng này", "Tháng trước", "3 tháng gần đây", "Tất cả thời gian");
            timeFilter.getSelectionModel().selectFirst();
        }
        loadWalletData();
    }

    private void loadWalletData() {
        if (balanceLabel != null) balanceLabel.setText("0 đ");
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
    private void showAddBankPopup() {
        try {
            URL fxmlLocation = getClass().getResource("/io/auctionsystem/AddBankDialog.fxml");
            if (fxmlLocation == null) {
                System.out.println("LỖI: Không tìm thấy file AddBankDialog.fxml.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField != null ? searchField.getText() : "";
        System.out.println("Đang tìm giao dịch: " + keyword);
    }

    @FXML private void filterAll() { /* Logic lọc tất cả */ }
    @FXML private void filterIn() { /* Logic lọc tiền vào */ }
    @FXML private void filterOut() { /* Logic lọc tiền ra */ }
}