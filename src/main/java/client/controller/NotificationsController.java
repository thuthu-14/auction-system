package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML
    private Button markAllReadBtn;

    @FXML
    private Button payNowBtn;

    @FXML
    private Button rebidBtn;

    @FXML
    private Button viewProfileBtn;
    private HomeScreenController homeController;

    public void setHomeController(HomeScreenController homeController) {
        this.homeController = homeController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        markAllReadBtn.setOnAction(event -> {
            System.out.println("🔔 Đã đánh dấu tất cả là đã đọc!");
        });

        payNowBtn.setOnAction(event -> {
            System.out.println("💰 Đang chuyển hướng đến trang Thanh toán...");
            showAlert(Alert.AlertType.INFORMATION, "Tuyệt vời", "Chuyển đến giỏ hàng để thanh toán MacBook Pro!");
        });

        rebidBtn.setOnAction(event -> {
            System.out.println("🔥 Đang quay lại phiên đấu giá iPhone 15 Pro Max...");
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Hãy đặt mức giá cao hơn 38.500.000 đ nhé!");
        });

        viewProfileBtn.setOnAction(event -> {
            System.out.println("👤 Đang yêu cầu HomeController mở trang Hồ sơ cá nhân...");
            if (homeController != null) {
                homeController.loadProfileView();
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
