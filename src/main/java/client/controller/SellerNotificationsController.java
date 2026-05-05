package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import navigation.NavigationManager;
import server.model.User;
import util.LoggerUtil;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerNotificationsController implements Initializable {

    @FXML private Button markAllReadBtn;
    @FXML private Button shipOrderBtn;
    @FXML private Button viewAuctionBtn;
    @FXML private Button relistBtn;

    // TODO: Khi thiết kế xong FXML, bạn nên có một VBox hoặc ListView để hiển thị danh sách Thông báo
    // @FXML private ListView<Notification> notificationListView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (markAllReadBtn != null) markAllReadBtn.setOnAction(event -> handleMarkAllRead());
        if (shipOrderBtn != null) shipOrderBtn.setOnAction(event -> handleShipOrder());
        if (viewAuctionBtn != null) viewAuctionBtn.setOnAction(event -> handleViewAuction());
        if (relistBtn != null) relistBtn.setOnAction(event -> handleRelist());

        // Gọi Server lấy thông báo khi vừa load xong UI
        loadNotificationsFromServer();
    }

    private void loadNotificationsFromServer() {
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();

        if (currentUser == null || socket == null) return;

        new Thread(() -> {
            try {
                // Yêu cầu Server trả về danh sách thông báo
                Message request = new Message(MessageType.GET_NOTIFICATIONS, null, currentUser.getUsername());
                socket.sendMessage(request);

                Message response = socket.receiveMessage();

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        LoggerUtil.info("Tải danh sách thông báo thành công!");

                        // TODO: Sau này bạn lấy data từ response.getData() và nhét vào ListView ở đây
                        // List<Notification> list = (List<Notification>) response.getData();
                        // notificationListView.getItems().addAll(list);
                    } else {
                        LoggerUtil.error("Không thể tải thông báo: " + (response != null ? response.getMessage() : ""));
                    }
                });
            } catch (Exception e) {
                LoggerUtil.error("Lỗi kết nối mạng khi tải thông báo: " + e.getMessage());
            }
        }).start();
    }

    private void handleMarkAllRead() {
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();

        if (currentUser == null || socket == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mất kết nối tới Server!");
            return;
        }

        new Thread(() -> {
            try {
                // Gửi lệnh đánh dấu đã đọc lên Server
                Message request = new Message(MessageType.MARK_NOTIFICATIONS_READ, null, currentUser.getUsername());
                socket.sendMessage(request);

                Message response = socket.receiveMessage();

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        LoggerUtil.info("Đã đánh dấu đọc tất cả thông báo trên Server.");
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Toàn bộ thông báo đã được đánh dấu là đã đọc!");
                        // Load lại list nếu cần thiết
                        // loadNotificationsFromServer();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái thông báo.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Vui lòng thử lại sau."));
                LoggerUtil.error("Lỗi khi gửi lệnh mark-read: " + e.getMessage());
            }
        }).start();
    }

    private void handleShipOrder() {
        LoggerUtil.info("Đang mở màn hình quản lý giao hàng...");
        // TODO: Gọi NavigationManager để chuyển qua form giao hàng
    }

    private void handleViewAuction() {
        LoggerUtil.info("Đang mở trang chi tiết phiên đấu giá...");
        // TODO: Chuyển sang form chi tiết, hoặc mở dialog như bên SellerManagementController
    }

    private void handleRelist() {
        LoggerUtil.info("Đang chuyển sang form đăng lại sản phẩm...");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}