package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.service.NotificationClient;
import client.service.NotificationClientService;
import client.service.NotificationClientService.NotificationAction;
import client.service.NotificationClientService.NotificationPresentation;
import client.service.NotificationClientService.NotificationScope;
import common.Message;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Notification;
import server.model.User;
import util.LoggerUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SellerNotificationsController implements Initializable {

    private final NotificationClient notificationService;

    public SellerNotificationsController() {
        this(new NotificationClientService());
    }

    SellerNotificationsController(NotificationClient notificationService) {
        this.notificationService = notificationService;
    }

    @FXML private Button markAllReadBtn;
    @FXML private VBox notificationsContainer;

    private User currentUser;
    private ClientSocket clientSocket;
    private SellerHomeController sellerHomeController;
    private Timeline timeRefreshTimer;
    private final List<Runnable> timeLabelUpdaters = new ArrayList<>();

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        loadNotifications();
    }

    public void setSellerHomeController(SellerHomeController sellerHomeController) {
        this.sellerHomeController = sellerHomeController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (markAllReadBtn != null) {
            markAllReadBtn.setOnAction(event -> markAllRead());
        }

        startTimeRefreshTimer();
    }

    public void loadNotifications() {
        if (notificationsContainer == null) return;

        showLoadingState();

        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                User user = resolveUser();

                if (socket == null || user == null) {
                    Platform.runLater(() -> showEmptyState("Không có dữ liệu người dùng hoặc kết nối server."));
                    return;
                }

                Message response = notificationService.fetchNotifications(socket, user);

                Platform.runLater(() -> {
                    if (response != null && response.getData() instanceof List<?> rawList) {
                        renderNotifications(rawList);
                    } else {
                        showEmptyState("Chưa có thông báo nào.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi load seller notifications: " + e.getMessage())));
                Platform.runLater(() -> showEmptyState("Không tải được thông báo."));
            }
        });
    }

    private void renderNotifications(List<?> rawList) {
        notificationsContainer.getChildren().clear();
        timeLabelUpdaters.clear();

        for (Object item : rawList) {
            if (item instanceof Notification notification && notificationService.isVisible(notification, NotificationScope.SELLER)) {
                notificationsContainer.getChildren().add(createNotificationRow(notification));
            }
        }

        if (notificationsContainer.getChildren().isEmpty()) {
            showEmptyState("Chưa có thông báo nào.");
        }
    }

    private HBox createNotificationRow(Notification data) {
        HBox container = new HBox(20);
        container.setAlignment(Pos.TOP_LEFT);
        container.setStyle(getContainerStyle(data));

        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        iconBox.setPrefSize(60, 60);
        iconBox.setMinSize(60, 60);
        iconBox.setStyle(getIconBoxStyle(data));

        Label iconLabel = new Label(getIcon(data));
        iconLabel.setFont(Font.font(28));
        iconLabel.setStyle("-fx-text-fill: " + getIconColor(data) + ";");
        iconBox.getChildren().add(iconLabel);

        VBox contentBox = new VBox(5);
        contentBox.setMinWidth(0);
        contentBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        Label titleLabel = new Label(defaultText(data.getTitle(), "Thông báo"));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setWrapText(true);
        titleLabel.setMinWidth(0);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setStyle("-fx-text-fill: " + getTitleColor(data) + ";");

        Label descLabel = new Label(defaultText(data.getDescription(), ""));
        descLabel.setFont(Font.font("System", 14));
        descLabel.setWrapText(true);
        descLabel.setMinWidth(0);
        descLabel.setMaxWidth(Double.MAX_VALUE);
        descLabel.setStyle("-fx-text-fill: #4a5568;");

        contentBox.getChildren().addAll(titleLabel, descLabel);

        VBox actionBox = new VBox();
        actionBox.setAlignment(Pos.TOP_RIGHT);
        actionBox.setMinWidth(132);
        actionBox.setPrefWidth(132);
        actionBox.setMaxWidth(132);
        actionBox.setMinHeight(68);

        Label timeLabel = new Label(data.formatTimeAgo());
        timeLabel.setFont(Font.font("System", 12));
        timeLabel.setWrapText(true);
        timeLabel.setMaxWidth(132);
        timeLabel.setAlignment(Pos.CENTER_RIGHT);
        timeLabel.setStyle("-fx-text-fill: #9ca3af;");
        timeLabelUpdaters.add(() -> timeLabel.setText(data.formatTimeAgo()));

        Button actionBtn = new Button(defaultText(data.getButtonText(), "Xem chi tiết"));
        actionBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        actionBtn.setWrapText(true);
        actionBtn.setMaxWidth(132);
        actionBtn.setStyle(getButtonStyle(data));
        actionBtn.setOnAction(event -> handleNotificationAction(data));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        actionBox.getChildren().addAll(actionBtn, spacer, timeLabel);
        container.getChildren().addAll(iconBox, contentBox, actionBox);

        return container;
    }

    private void markAllRead() {
        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                User user = resolveUser();
                if (socket == null || user == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Mất kết nối tới server."));
                    return;
                }

                Message response = notificationService.markAllRead(socket, user, NotificationScope.SELLER);
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        loadNotifications();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đánh dấu đã đọc.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi mark seller notifications read: " + e.getMessage())));
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đánh dấu đã đọc."));
            }
        });
    }

    private void handleNotificationAction(Notification data) {
        if (sellerHomeController == null) {
            return;
        }

        NotificationAction action = notificationService.actionFor(data, NotificationScope.SELLER);
        switch (action) {
            case SELLER_MANAGE_AUCTIONS -> sellerHomeController.loadManageAuctionsView();
            case WALLET -> sellerHomeController.loadWalletView();
            case SELLER_DASHBOARD -> sellerHomeController.loadSellerDashboardView();
            default -> sellerHomeController.loadSellerDashboardView();
        }
    }

    private void showLoadingState() {
        notificationsContainer.getChildren().setAll(createStateLabel("Đang tải thông báo..."));
    }

    private void showEmptyState(String text) {
        notificationsContainer.getChildren().setAll(createStateLabel(text));
        timeLabelUpdaters.clear();
    }

    private Label createStateLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web("#a0aec0"));
        label.setFont(Font.font("System", 15));
        label.setStyle("-fx-text-fill: #a0aec0;");
        return label;
    }

    private ClientSocket resolveSocket() {
        if (clientSocket != null) return clientSocket;
        if (NavigationManager.getInstance().getClientSocket() != null) {
            return NavigationManager.getInstance().getClientSocket();
        }
        return client.network.ConnectionManager.getInstance().getClientSocket();
    }

    private User resolveUser() {
        if (currentUser != null) return currentUser;
        return NavigationManager.getInstance().getCurrentUser();
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

    private String getContainerStyle(Notification data) {
        return notificationService.containerStyle(data);
    }

    private String getIconBoxStyle(Notification data) {
        return presentation(data).iconBoxStyle();
    }

    private String getIcon(Notification data) {
        return presentation(data).icon();
    }

    private String getIconColor(Notification data) {
        return presentation(data).iconColor();
    }

    private String getTitleColor(Notification data) {
        return presentation(data).titleColor();
    }

    private String getButtonStyle(Notification data) {
        return presentation(data).buttonStyle();
    }

    private NotificationPresentation presentation(Notification data) {
        return notificationService.presentation(data);
    }
    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        client.util.DialogUtil.showAlert(type, title, null, content);
    }
}


