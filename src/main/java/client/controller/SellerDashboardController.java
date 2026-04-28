package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class SellerDashboardController {

    @FXML private VBox cardTaoPhien;
    @FXML private VBox cardQuanLyPhien;
    @FXML private VBox cardThongKe;
    @FXML private VBox cardThongBao;
    @FXML private VBox cardViTien;

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newNode = loader.load();
            Pane contentArea = (Pane) cardTaoPhien.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(newNode);
            } else {
                System.err.println("Lỗi: Không tìm thấy fx:id='contentArea' ở trang chính!");
            }
        } catch (IOException e) {
            System.err.println("Không thể load file FXML: " + fxmlPath);
            e.printStackTrace();
        }
    }
    @FXML
    private void onCardClick(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        String id = source.getId();

        switch (id) {
            case "cardTaoPhien":
                System.out.println("Đang mở: Tạo phiên mới");
                loadView("/fxml/AddAuctionProduct.fxml");
                break;
            case "cardQuanLyPhien":
                System.out.println("Đang mở: Quản lý phiên");
                loadView("/fxml/SellerManageAuctions.fxml");
                break;
            case "cardThongKe":
                System.out.println("Đang mở: Thống kê");
                loadView("/fxml/SellerStatistics.fxml");
                break;
            case "cardThongBao":
                System.out.println("Đang mở: Thông báo");
                loadView("/fxml/SellerNotifications.fxml");
                break;
            case "cardViTien":
                System.out.println("Đang mở: Ví tiền");
                loadView("/fxml/SellerWallet.fxml");
                break;
        }
    }


    @FXML
    private void onCardEnter(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        source.setScaleX(1.02);
        source.setScaleY(1.02);
        source.setStyle(source.getStyle() + "; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 8);");
    }

    @FXML
    private void onCardExit(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        source.setScaleX(1.0);
        source.setScaleY(1.0);
    }

}