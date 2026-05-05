package client.controller;

import client.network.ClientSocket;
import server.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import util.LoggerUtil;

public class SellerDashboardController {

    @FXML private VBox cardTaoPhien;
    @FXML private VBox cardQuanLyPhien;
    @FXML private VBox cardThongKe;
    @FXML private VBox cardThongBao;
    @FXML private VBox cardViTien;

    // ===== THÊM ĐỂ LINK SERVER =====
    private User currentUser;
    private ClientSocket clientSocket;

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent newNode = loader.load();

            // ===== BƠM DỮ LIỆU SERVER SANG MÀN HÌNH MỚI =====
            Object controller = loader.getController();
            if (controller != null) {
                if (controller instanceof WalletController) {
                    ((WalletController) controller).setUserData(currentUser, clientSocket);
                } else if (controller instanceof SellerManagementController) {
                    ((SellerManagementController) controller).refreshAuctions();
                }
                // (Nếu sau này các trang Thống kê, Thông báo cần Server, bạn thêm else if vào đây)
            }

            Pane contentArea = (Pane) cardTaoPhien.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(newNode);
            } else {
                LoggerUtil.error("Lỗi: Không tìm thấy fx:id='contentArea' ở trang chính!");
            }
        } catch (Exception e) {
            LoggerUtil.error("Không thể load file FXML: " + fxmlPath + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void onCardClick(MouseEvent event) {
        VBox source = (VBox) event.getSource();
        String id = source.getId();

        switch (id) {
            case "cardTaoPhien":
                LoggerUtil.info("Đang mở: Tạo phiên mới");
                loadView("/fxml/AddAuctionProduct.fxml");
                break;
            case "cardQuanLyPhien":
                LoggerUtil.info("Đang mở: Quản lý phiên");
                loadView("/fxml/SellerManageAuctions.fxml"); // Sửa lại tên file cho chuẩn với HomeController
                break;
            case "cardThongKe":
                LoggerUtil.info("Đang mở: Thống kê");
                loadView("/fxml/SellerStatistics.fxml");
                break;
            case "cardThongBao":
                LoggerUtil.info("Đang mở: Thông báo");
                loadView("/fxml/SellerNotifications.fxml");
                break;
            case "cardViTien":
                LoggerUtil.info("Đang mở: Ví tiền");
                loadView("/fxml/WalletView.fxml"); // Sửa lại tên file cho chuẩn với HomeController
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