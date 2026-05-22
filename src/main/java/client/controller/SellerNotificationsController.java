package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.service.NotificationClient;
import client.service.NotificationClientService;
import client.service.NotificationClientService.NotificationAction;
import client.service.NotificationClientService.NotificationScope;
import client.ui.NotificationViewFactory;
import common.Message;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Notification;
import server.model.User;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SellerNotificationsController implements Initializable {

    private final NotificationClient notificationService;
    private final NotificationViewFactory viewFactory;

    public SellerNotificationsController() {
        this(new NotificationClientService());
    }

    SellerNotificationsController(NotificationClient notificationService) {
        this.notificationService = notificationService;
        this.viewFactory = new NotificationViewFactory(notificationService);
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
                    Platform.runLater(() -> showEmptyState("Khong co du lieu nguoi dung hoac ket noi server."));
                    return;
                }

                Message response = notificationService.fetchNotifications(socket, user);

                Platform.runLater(() -> {
                    if (response != null && response.getData() instanceof List<?> rawList) {
                        renderNotifications(rawList);
                    } else {
                        showEmptyState("Chua co thong bao nao.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error",
                        new RuntimeException("Loi load seller notifications: " + e.getMessage()));
                Platform.runLater(() -> showEmptyState("Khong tai duoc thong bao."));
            }
        });
    }

    private void renderNotifications(List<?> rawList) {
        notificationsContainer.getChildren().clear();
        timeLabelUpdaters.clear();

        for (Object item : rawList) {
            if (item instanceof Notification notification
                    && notificationService.isVisible(notification, NotificationScope.SELLER)) {
                notificationsContainer.getChildren().add(createNotificationRow(notification));
            }
        }

        if (notificationsContainer.getChildren().isEmpty()) {
            showEmptyState("Chua co thong bao nao.");
        }
    }

    private javafx.scene.Node createNotificationRow(Notification data) {
        return viewFactory.createNotificationRow(data, this::handleNotificationAction, timeLabelUpdaters::add);
    }

    private void markAllRead() {
        client.util.ClientTaskRunner.run(() -> {
            try {
                ClientSocket socket = resolveSocket();
                User user = resolveUser();
                if (socket == null || user == null) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi", "Mat ket noi toi server."));
                    return;
                }

                Message response = notificationService.markAllRead(socket, user, NotificationScope.SELLER);
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        loadNotifications();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Loi", "Khong the danh dau da doc.");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error",
                        new RuntimeException("Loi mark seller notifications read: " + e.getMessage()));
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Loi", "Khong the danh dau da doc."));
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
        notificationsContainer.getChildren().setAll(viewFactory.createStateLabel("Dang tai thong bao..."));
    }

    private void showEmptyState(String text) {
        notificationsContainer.getChildren().setAll(viewFactory.createStateLabel(text));
        timeLabelUpdaters.clear();
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

    private void showAlert(Alert.AlertType type, String title, String content) {
        client.util.DialogUtil.showAlert(type, title, null, content);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
