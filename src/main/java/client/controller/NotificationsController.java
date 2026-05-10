package client.controller;

import common.Message;
import common.MessageType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Notification; // Dùng class thật bạn vừa tạo
import util.LoggerUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class NotificationsController implements Initializable {

    @FXML private Button markAllReadBtn;
    @FXML private VBox notificationsContainer;

    private HomeScreenController homeController;
    private Timeline timeRefreshTimer;
    private final List<Runnable> timeLabelUpdaters = new ArrayList<>();

    public void setHomeController(HomeScreenController homeController) {
        this.homeController = homeController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (markAllReadBtn != null) {
            markAllReadBtn.setOnAction(event -> {
                sendSocketRequest(MessageType.MARK_NOTIFICATIONS_READ, "READ_ALL", "🔔 Đã cập nhật trạng thái đã đọc!");
                loadNotifications();
            });
        }
        startTimeRefreshTimer();
        loadNotifications();
    }

    /**
     * Gửi yêu cầu lên Server để lấy danh sách thông báo THẬT
     */
    public void loadNotifications() {
        if (notificationsContainer == null) return;
        notificationsContainer.getChildren().clear();
        timeLabelUpdaters.clear();

        new Thread(() -> {
            try {
                var socket = NavigationManager.getInstance().getClientSocket();
                var user = NavigationManager.getInstance().getCurrentUser();

                if (socket != null && user != null) {
                    // 1. Gửi lệnh lấy thông báo
                    Message request = new Message(MessageType.GET_NOTIFICATIONS, null, user.getUserId());

                    // 2. Nhận phản hồi
                    Message response = socket.sendAndReceive(request);
                    if (response != null && response.getData() instanceof List) {
                        List<Notification> realList = (List<Notification>) response.getData();

                        // 3. Hiển thị lên giao diện
                        Platform.runLater(() -> {
                            for (Notification noti : realList) {
                                notificationsContainer.getChildren().add(createNotificationRow(noti));
                            }
                        });
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("Lỗi load notifications: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Tạo giao diện dòng thông báo dựa trên Class Notification
     */
    private HBox createNotificationRow(Notification data) {
        HBox container = new HBox(20);
        container.setAlignment(Pos.CENTER_LEFT);

        // Icon Box
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setMinSize(60, 60);
        Label iconLabel = new Label();
        iconLabel.setFont(Font.font(28));

        // Content Box
        VBox contentBox = new VBox(5);
        HBox.setHgrow(contentBox, Priority.ALWAYS);
        Label titleLabel = new Label(data.getTitle()); // getTitle() từ class
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        Label descLabel = new Label(data.getDescription()); // getDescription() từ class
        descLabel.setFont(Font.font("System", 14));
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #4a5568;");
        contentBox.getChildren().addAll(titleLabel, descLabel);

        // Action Box
        VBox actionBox = new VBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Label timeLabel = new Label(data.formatTimeAgo());
        timeLabel.setFont(Font.font("System", 12));
        timeLabel.setStyle("-fx-text-fill: #9ca3af;");
        timeLabelUpdaters.add(() -> timeLabel.setText(data.formatTimeAgo()));
        Button actionBtn = new Button(data.getButtonText());
        actionBtn.setFont(Font.font("System", FontWeight.BOLD, 14));

        actionBox.getChildren().addAll(timeLabel, actionBtn);
        container.getChildren().addAll(iconBox, contentBox, actionBox);

        // Styling & Logic
        switch (data.getType()) {
            case "WIN" -> {
                container.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #fde047; -fx-border-radius: 15; -fx-border-width: 2; -fx-padding: 20;");
                iconBox.setStyle("-fx-background-color: #fef08a; -fx-background-radius: 30;");
                iconLabel.setText("🏆");
                actionBtn.setStyle("-fx-background-color: #facc15; -fx-text-fill: #1a1a1a; -fx-background-radius: 20; -fx-cursor: hand;");

                actionBtn.setOnAction(event -> NavigationManager.getInstance().navigateTo("/fxml/Wallet.fxml"));
            }
            case "OUTBID" -> {
                container.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #f3f4f6; -fx-border-radius: 15; -fx-border-width: 1; -fx-padding: 20;");
                iconBox.setStyle("-fx-background-color: #fee2e2; -fx-background-radius: 30;");
                iconLabel.setText("⚠");
                actionBtn.setStyle("-fx-background-color: #1a1a1a; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;");

                actionBtn.setOnAction(event -> showAlert(Alert.AlertType.WARNING, "Chú ý", "Đang quay lại phiên đấu giá..."));
            }
            case "SYSTEM" -> {
                container.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-border-color: #f3f4f6; -fx-border-radius: 15; -fx-border-width: 1; -fx-padding: 20;");
                iconBox.setStyle("-fx-background-color: #e0f2fe; -fx-background-radius: 30;");
                iconLabel.setText("✨");
                actionBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #4a5568; -fx-background-radius: 20; -fx-cursor: hand;");

                actionBtn.setOnAction(event -> { if(homeController != null) homeController.loadProfileView(); });
            }
        }

        iconBox.getChildren().add(iconLabel);
        return container;
    }

    private void startTimeRefreshTimer() {
        if (timeRefreshTimer != null) {
            timeRefreshTimer.stop();
        }

        timeRefreshTimer = new Timeline(new KeyFrame(Duration.seconds(30), event -> {
            for (Runnable updater : timeLabelUpdaters) {
                updater.run();
            }
        }));
        timeRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        timeRefreshTimer.play();
    }

    private void sendSocketRequest(MessageType type, Object data, String successPopup) {
        try {
            var socket = NavigationManager.getInstance().getClientSocket();
            var user = NavigationManager.getInstance().getCurrentUser();
            if (socket != null && user != null) {
                Message msg = new Message(type, data, user.getUserId());
                socket.sendMessage(msg);
                showAlert(Alert.AlertType.INFORMATION, "Thành công", successPopup);
            }
        } catch (Exception e) {
            LoggerUtil.error("Lỗi gửi request: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            client.util.DialogUtil.showAlert(type, title, null, content);
        });
    }
}
