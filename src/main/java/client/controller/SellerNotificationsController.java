package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerNotificationsController implements Initializable {

    @FXML private Button markAllReadBtn;
    @FXML private Button shipOrderBtn;
    @FXML private Button viewAuctionBtn;
    @FXML private Button relistBtn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        if (markAllReadBtn != null) {
            markAllReadBtn.setOnAction(event -> handleMarkAllRead());
        }

        if (shipOrderBtn != null) {
            shipOrderBtn.setOnAction(event -> handleShipOrder());
        }

        if (viewAuctionBtn != null) {
            viewAuctionBtn.setOnAction(event -> handleViewAuction());
        }

        if (relistBtn != null) {
            relistBtn.setOnAction(event -> handleRelist());
        }
    }


    private void handleMarkAllRead() {
        System.out.println("Đã đánh dấu đọc tất cả thông báo.");
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Toàn bộ thông báo đã được đánh dấu là đã đọc!");
        // Thêm logic cập nhật xuống Database ở đây
    }

    private void handleShipOrder() {
        System.out.println("Đang mở màn hình quản lý giao hàng...");
    }

    private void handleViewAuction() {
        System.out.println("Đang mở trang chi tiết phiên đấu giá...");
    }

    private void handleRelist() {
        System.out.println("Đang chuyển sang form đăng lại sản phẩm...");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}