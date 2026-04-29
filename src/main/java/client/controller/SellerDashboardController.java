package client.controller;

import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class SellerDashboardController {

    @FXML private VBox cardTaoPhien;
    @FXML private VBox cardQuanLyPhien;
    @FXML private VBox cardThongKe;
    @FXML private VBox cardThongBao;
    @FXML private VBox cardViTien;

    // SellerHomeController inject vào đây sau khi load FXML
    private SellerHomeController homeController;

    public void setHomeController(SellerHomeController homeController) {
        this.homeController = homeController;
    }

    // Mỗi card map sang đúng HBox menu trong SellerHome.fxml
    // rồi gọi thẳng updateMenuSelection() của SellerHomeController
    @FXML
    private void onCardClick(MouseEvent event) {
        if (homeController == null) return;

        VBox card = (VBox) event.getSource();

        if (card == cardTaoPhien) {
            homeController.selectMenu(homeController.menuCreateAuctions);
        } else if (card == cardQuanLyPhien) {
            homeController.selectMenu(homeController.menuManageAuctions);
        } else if (card == cardThongKe) {
            homeController.selectMenu(homeController.menuAuctionStatistic);
        } else if (card == cardThongBao) {
            homeController.selectMenu(homeController.menuMsg);
        } else if (card == cardViTien) {
            homeController.selectMenu(homeController.menuPay);
        }
    }

    //hover effect
    @FXML
    private void onCardEnter(MouseEvent event) {
        VBox card = (VBox) event.getSource();
        card.getStyleClass().add("dashboard-card-hover");
    }

    @FXML
    private void onCardExit(MouseEvent event) {
        VBox card = (VBox) event.getSource();
        card.getStyleClass().remove("dashboard-card-hover");
    }
}
